/*-
 * ========================LICENSE_START=================================
 * TeamApps Cluster
 * ---
 * Copyright (C) 2021 - 2022 TeamApps.org
 * ---
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
package org.teamapps.cluster.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamapps.cluster.protocol.ClusterInfo;
import org.teamapps.cluster.protocol.ClusterModel;
import org.teamapps.cluster.service.Utils;
import org.teamapps.protocol.schema.*;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class ClusterImpl implements Cluster, ClusterHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private final String clusterSecret;
	private final File tempDir;
	private final ModelRegistry modelRegistry;
	private final LocalNode localNode;
	private final HostAddress bindToAddress;
	private List<RemoteNode> remoteNodes = new ArrayList<>();
	private final Map<String, RemoteNode> remoteNodeById = new ConcurrentHashMap<>();
	private boolean active = true;
	private final Map<String, AbstractClusterService> localServices = new ConcurrentHashMap<>();
	private final Map<String, List<ClusterMessageHandler<? extends MessageObject>>> messageHandlersByModelUuid = new ConcurrentHashMap<>();
	private final Map<String, List<ClusterMessageHandler<? extends MessageObject>>> messageHandlersByTopic = new ConcurrentHashMap<>();
	private Map<String, List<RemoteNode>> clusterServicesByName = new HashMap<>();
	private final ExecutorService executor = Executors.newFixedThreadPool(16);

	public ClusterImpl(String clusterSecret, String nodeId, boolean leader, HostAddress... knownNodes) throws IOException {
		this(clusterSecret, nodeId, null, null, leader, knownNodes);
	}

	public ClusterImpl(String clusterSecret, String nodeId, HostAddress externalAddress, boolean leader, HostAddress... knownNodes) throws IOException {
		this(clusterSecret, nodeId, externalAddress, externalAddress, leader, knownNodes);
	}

	public ClusterImpl(String clusterSecret, String nodeId, HostAddress externalAddress, HostAddress bindToAddress, boolean leader, HostAddress... knownNodes) throws IOException {
		this(clusterSecret, Files.createTempDirectory("temp").toFile(), nodeId, externalAddress, bindToAddress, leader, knownNodes);
	}

	public ClusterImpl(String clusterSecret, File tempDir, String nodeId, HostAddress externalAddress, HostAddress bindToAddress, boolean leader, HostAddress... knownNodes) {
		this.clusterSecret = clusterSecret;
		this.tempDir = tempDir;
		this.modelRegistry = ClusterModel.MODEL_COLLECTION.createRegistry();
		this.bindToAddress = bindToAddress;
		this.localNode = new LocalNodeImpl(nodeId, externalAddress, externalAddress != null && bindToAddress != null, leader);
		if (localNode.isExternallyReachable()) {
			startServerSocket();
		}
		if (knownNodes != null) {
			for (HostAddress hostAddress : knownNodes) {
				createRemoteNode(hostAddress);
			}
		}
		block();
	}

	public void block() {
		new Thread(() -> {
			while (active) {
				try {
					Thread.sleep(250);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		}).start();
	}

	private void createRemoteNode(HostAddress hostAddress) {
		if (!localNode.isExternallyReachable() || isOutboundConnection(localNode.getHostAddress(), hostAddress)) {
			remoteNodes.add(new RemoteNodeImpl(hostAddress, this, modelRegistry, tempDir, clusterSecret));
		}
	}

	private boolean isOutboundConnection(HostAddress localAddress, HostAddress remoteAddress) {
		return (localAddress.getHost() + localAddress.getPort()).compareTo(remoteAddress.getHost() + remoteAddress.getPort()) < 0;
	}


	private void startServerSocket() {
		String host = bindToAddress.getHost() != null ? bindToAddress.getHost() : "0.0.0.0";
		Thread thread = new Thread(() -> {
			try {
				ServerSocket serverSocket = new ServerSocket(bindToAddress.getPort(), 50, InetAddress.getByName(host));
				while (active) {
					try {
						Socket socket = serverSocket.accept();
						new RemoteNodeImpl(socket, this, modelRegistry, tempDir, clusterSecret);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		thread.setName("server-socket-" + host + "-" + bindToAddress.getPort());
		thread.setDaemon(true);
		thread.start();
	}

	@Override
	public ClusterInfo getClusterInfo() {
		return new ClusterInfo()
				.setLocalNode(localNode.createNodeInfo())
				.setRemoteNodes(remoteNodes.stream().map(Node::createNodeInfo)
						.collect(Collectors.toList()));
	}

	@Override
	public void handleNodeConnected(RemoteNode node, ClusterInfo clusterInfo) {
		ArrayList<RemoteNode> nodesCopy = new ArrayList<>(remoteNodes);
		RemoteNode remoteNode = remoteNodeById.get(node.getNodeId());
		if (remoteNode != null) {
			LOGGER.info("Existing node reconnected:" + node);
			node.recycleNode(remoteNode);
			remoteNodeById.put(node.getNodeId(), node);
			nodesCopy.remove(remoteNode);
			nodesCopy.add(node);
			remoteNodes = nodesCopy;
		} else {
			LOGGER.info("New node connected:" + node);
			remoteNodeById.put(node.getNodeId(), node);
			nodesCopy.add(node);
			remoteNodes = nodesCopy;
		}
		handleClusterUpdate();
	}

	@Override
	public void handleNodeDisconnected(RemoteNode node) {

	}

	@Override
	public void handleClusterUpdate() {
		Map<String, List<RemoteNode>> serviceMap = new HashMap<>();
		for (RemoteNode clusterNode : remoteNodes) {
			for (String service : clusterNode.getServices()) {
				serviceMap.putIfAbsent(service, new ArrayList<>());
				serviceMap.get(service).add(clusterNode);
			}
		}
		clusterServicesByName = serviceMap;
	}


	@Override
	public void addModelCollection(ModelCollection modelCollection) {
		modelRegistry.addModelCollection(modelCollection);
	}

	@Override
	public LocalNode getLocalNode() {
		return localNode;
	}

	@Override
	public List<RemoteNode> getRemoteNodes() {
		return remoteNodes;
	}

	@Override
	public void addRemoteNode(RemoteNode remoteNode) {

	}

	@Override
	public RemoteNode getRemoteNode(String nodeId) {
		return remoteNodes.stream()
				.filter(node -> node.getNodeId().equals(nodeId))
				.findFirst().orElse(null);
	}

	@Override
	public void registerService(AbstractClusterService clusterService) {
		localServices.put(clusterService.getServiceName(), clusterService);
		localNode.getServices().add(clusterService.getServiceName());
		sendMessageToAllNodes(getClusterInfo(), false);
	}

	@Override
	public void registerModelCollection(ModelCollection modelCollection) {
		modelRegistry.addModelCollection(modelCollection);
	}

	@Override
	public boolean isServiceAvailable(String serviceName) {
		List<RemoteNode> nodesWithService = clusterServicesByName.getOrDefault(serviceName, Collections.emptyList());
		return !nodesWithService.isEmpty();
	}

	public RemoteNode getRandomServiceProvider(String serviceName) {
		List<RemoteNode> nodesWithService = clusterServicesByName.getOrDefault(serviceName, Collections.emptyList()).stream().filter(RemoteNode::isConnected).collect(Collectors.toList());
		return Utils.randomListEntry(nodesWithService);
	}

	@Override
	public <REQUEST extends MessageObject, RESPONSE extends MessageObject> RESPONSE executeServiceMethod(String serviceName, String method, REQUEST request, PojoObjectDecoder<RESPONSE> responseDecoder) {
		RemoteNode serviceProvider = getRandomServiceProvider(serviceName);
		LOGGER.info("Execute cluster method: {}, {}", serviceName, method);
		if (serviceProvider == null) {
			LOGGER.info("Service method not executed: missing service provider: {}", serviceName);
			return null;
		} else {
			RESPONSE response = serviceProvider.executeServiceMethod(serviceName, method, request, responseDecoder);
			return response;
		}
	}

	private void sendMessageToAllNodes(MessageObject message, boolean resendOnError) {
		remoteNodes.forEach(node -> node.sendMessage(message, resendOnError));
	}

	@Override
	public void sendMessage(MessageObject message, String nodeId) {
		RemoteNode remoteNode = getRemoteNode(nodeId);
		if (remoteNode != null) {
			remoteNode.sendMessage(message, false);
		} else {
			LOGGER.info("Cannot send message to missing nose: {}", nodeId);
		}
	}

	@Override
	public void sendTopicMessage(String topic, MessageObject message) {

	}

	@Override
	public void handleMessage(MessageObject message, RemoteNode node) {
		String modelUuid = message.getModel().getModelUuid();
		List<ClusterMessageHandler<? extends MessageObject>> clusterMessageHandlers = messageHandlersByModelUuid.get(modelUuid);
		if (clusterMessageHandlers != null) {
			clusterMessageHandlers.forEach(handler -> handler.handleMessage(message, node.getNodeId(), executor));
		}
	}

	@Override
	public void handleTopicMessage(String topic, MessageObject message, RemoteNode node) {
		List<ClusterMessageHandler<? extends MessageObject>> clusterMessageHandlers = messageHandlersByTopic.get(topic);
		if (clusterMessageHandlers != null) {
			clusterMessageHandlers.forEach(handler -> handler.handleMessage(message, node.getNodeId(), executor));
		}
	}

	@Override
	public <MESSAGE extends MessageObject> void registerMessageHandler(MessageHandler<MESSAGE> messageHandler, PojoObjectDecoder<MESSAGE> messageDecoder) {
		messageHandlersByModelUuid.computeIfAbsent(messageDecoder.getMessageObjectUuid(), s -> new ArrayList<>()).add(new ClusterMessageHandler<>(messageHandler, messageDecoder));
	}

	@Override
	public <MESSAGE extends MessageObject> void registerTopicHandler(String topic, MessageHandler<MESSAGE> messageHandler, PojoObjectDecoder<MESSAGE> messageDecoder) {
		messageHandlersByTopic.computeIfAbsent(topic, s -> new ArrayList<>()).add(new ClusterMessageHandler<>(messageHandler, messageDecoder));
	}

	@Override
	public MessageObject handleClusterServiceMethod(String service, String serviceMethod, MessageObject requestData) {
		AbstractClusterService clusterService = localServices.get(service);
		if (clusterService != null) {
			return clusterService.handleMessage(serviceMethod, requestData);
		} else {
			LOGGER.warn("Cannot handle cluster service method: missing service: {}", service);
			return null;
		}
	}

	public void shutDown() {
		for (RemoteNode clusterNode : remoteNodes) {
			clusterNode.shutDown();
		}
	}

}
