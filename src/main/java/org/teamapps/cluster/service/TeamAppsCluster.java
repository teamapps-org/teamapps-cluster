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
package org.teamapps.cluster.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamapps.cluster.crypto.AesCipher;
import org.teamapps.cluster.dto.FileProvider;
import org.teamapps.cluster.dto.Message;
import org.teamapps.cluster.dto.MessageDecoder;
import org.teamapps.cluster.dto.MessageField;
import org.teamapps.cluster.model.cluster.*;
import org.teamapps.cluster.network.ClusterNodeMessageHandler;
import org.teamapps.cluster.network.LocalClusterNode;
import org.teamapps.cluster.network.NodeAddress;
import org.teamapps.cluster.network.RemoteClusterNode;
import org.teamapps.cluster.utils.ExceptionUtil;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class TeamAppsCluster extends Thread implements ClusterNodeMessageHandler, FileProvider, ServiceRegistry {
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());


	private final String clusterSecret;
	private final LocalClusterNode localNode;
	private final AesCipher aesCipher;
	private int retryMaxAttempts = 3;
	private Duration retryBackoffDuration = Duration.ofSeconds(3);

	private Consumer<NodeAddress> onNodeConnectionListener;

//	private final ExecutorService executor = new ThreadPoolExecutor(1, 32,
//			180L, TimeUnit.SECONDS,
//			new LinkedBlockingQueue<>());

	private final ExecutorService executor = Executors.newFixedThreadPool(16);

	private volatile boolean running = true;

	private final Map<String, RemoteClusterNode> remoteNodes = new ConcurrentHashMap<>();
	private final Map<String, AbstractClusterService> localServices = new ConcurrentHashMap<>();
	private Map<String, List<RemoteClusterNode>> clusterServices = new HashMap<>();
	private final Map<Long, CompletableFuture<ServiceClusterResponse>> serviceResponseFutureMap = new ConcurrentHashMap<>();
	private final Map<String, CompletableFuture<ClusterFileTransferResponse>> fileTransferFutureMap = new ConcurrentHashMap<>();
	private final AtomicLong requestIdGenerator = new AtomicLong();
	private final Map<String, ClusterTopic> clusterTopics = new ConcurrentHashMap<>();

	private final File fileTransferPath;
	private final Map<String, File> fileTransferMap = Collections.synchronizedMap(new LinkedHashMap<>() {
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, File> eldest) {
			File file = eldest.getValue();
			boolean oldFile = System.currentTimeMillis() - file.lastModified() > 86_400_000;
			boolean remove = oldFile || size() > 100_000;
			if (remove) {
				eldest.getValue().delete();
			}
			return remove;
		}
	});

	public TeamAppsCluster(String clusterSecret, int localPort, NodeAddress... knownNodes) {
		this(clusterSecret, null, null, localPort, null, knownNodes);
	}

	public TeamAppsCluster(String clusterSecret, String localNodeId, Consumer<NodeAddress> onNodeConnectionListener, int localPort, NodeAddress... knownNodes) {
		this(clusterSecret, localNodeId, onNodeConnectionListener, localPort, null, knownNodes);
	}

	public TeamAppsCluster(String clusterSecret, String localNodeId, Consumer<NodeAddress> onNodeConnectionListener, int localPort, File tempDir, NodeAddress... knownNodes) {
		super("cluster-server-socket");
		this.clusterSecret = clusterSecret;
		this.aesCipher = new AesCipher(clusterSecret);
		this.localNode = new LocalClusterNode(localNodeId, localPort);
		this.fileTransferPath = tempDir != null ? tempDir : Utils.createTempDir();
		this.onNodeConnectionListener = onNodeConnectionListener;
		start();
		connectNodes(knownNodes);
	}

	public ClusterTopic createTopic(String topicName, Consumer<ClusterTopicMessage> messageConsumer) {
		ClusterTopic clusterTopic = clusterTopics.computeIfAbsent(topicName, s -> new ClusterTopic(topicName, aesCipher, localNode.getNodeId()));
		clusterTopic.setMessageConsumer(messageConsumer);
		sendClusterInfo();
		return clusterTopic;
	}

	public void shutDown() {
		for (RemoteClusterNode clusterNode : remoteNodes.values()) {
			clusterNode.shutDown();
		}
		executor.shutdownNow();
	}

	private void connectNodes(NodeAddress[] knownNodes) {
		if (knownNodes == null) {
			return;
		}
		for (NodeAddress nodeAddress : knownNodes) {
			RemoteClusterNode remoteClusterNode = new RemoteClusterNode(this, nodeAddress);
		}
	}

	public void registerService(AbstractClusterService clusterService) {
		localServices.put(clusterService.getServiceName(), clusterService);
		List<RemoteClusterNode> connectedNodes = remoteNodes.values().stream().filter(RemoteClusterNode::isConnected).collect(Collectors.toList());
		sendNodeUpdate(connectedNodes);
	}

	public boolean isServiceAvailable(String serviceName) {
		List<RemoteClusterNode> nodesWithService = clusterServices.getOrDefault(serviceName, Collections.emptyList());
		return !nodesWithService.isEmpty();
	}

	public RemoteClusterNode getRandomServiceProvider(String serviceName) {
		List<RemoteClusterNode> nodesWithService = clusterServices.getOrDefault(serviceName, Collections.emptyList()).stream().filter(RemoteClusterNode::isConnected).collect(Collectors.toList());
		return Utils.randomListEntry(nodesWithService);
	}


	@Override
	public void run() {
		try {
			ServerSocket serverSocket = new ServerSocket(localNode.getPort());
			while (running) {
				try {
					Socket socket = serverSocket.accept();
					new RemoteClusterNode(this, socket);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void sendMessage(Message message, RemoteClusterNode clusterNode) throws Exception {
		//message files must have been handled before!
		LOGGER.info("Send message:" + message);
		byte[] messageBytes = aesCipher.encrypt(message.toBytes());
		clusterNode.sendMessage(messageBytes);
	}

	private synchronized void handleClusterNodeInfo(ClusterNodeInfo clusterNodeInfo, RemoteClusterNode clusterNode) throws Exception {
		clusterNode.setClusterNodeData(clusterNodeInfo.getLocalNode());
		RemoteClusterNode existingNode = remoteNodes.get(clusterNode.getNodeId());
		if (existingNode != null && existingNode != clusterNode) {
			existingNode.merge(clusterNode);
			clusterNode = existingNode;
		}
		for (ClusterNodeData knownRemoteNode : clusterNodeInfo.getKnownRemoteNodes()) {
			if (!remoteNodes.containsKey(knownRemoteNode.getNodeId()) && !knownRemoteNode.getNodeId().equals(localNode.getNodeId())) {
				RemoteClusterNode remoteClusterNode = new RemoteClusterNode(this, new NodeAddress(knownRemoteNode.getHost(), knownRemoteNode.getPort()));
			}
		}
		for (ClusterTopicInfo clusterTopicInfo : clusterNodeInfo.getClusterTopics()) {
			ClusterTopic clusterTopic = clusterTopics.get(clusterTopicInfo.getTopicName());
			if (clusterTopic == null) {
				clusterTopic = new ClusterTopic(clusterTopicInfo.getTopicName(), aesCipher);
			}
			for (String nodeId : clusterTopicInfo.getNodeIdsAsList()) {
				if (!clusterTopic.isRegistered(nodeId)) {
					clusterTopic.addRegisteredMember(nodeId);
				}
				if (!clusterTopic.isAvailableMember(nodeId)) {
					RemoteClusterNode remoteClusterNode = remoteNodes.get(nodeId);
					if (remoteClusterNode != null) {
						clusterTopic.addMember(remoteClusterNode);
					}
				}
			}
		}

		boolean sendClusterInfo = !clusterNodeInfo.getResponse();
		if (!remoteNodes.containsKey(clusterNode.getNodeId())) {
			clusterNode.setConnected(true);
			sendClusterInfo = true;
			LOGGER.info("New cluster node: {}", clusterNode);
			remoteNodes.put(clusterNode.getNodeId(), clusterNode);
			for (ClusterTopic clusterTopic : clusterTopics.values()) {
				if (clusterTopic.isRegistered(clusterNode.getNodeId()) && !clusterTopic.isAvailableMember(clusterNode.getNodeId())) {
					clusterTopic.addMember(clusterNode);
				}
			}
		} else if (!clusterNode.isConnected()) {
			clusterNode.setConnected(true);
			LOGGER.info("Reconnected cluster node: {}", clusterNode);
		}
		//todo set map for each node which node it knows - send if unknown
		if (sendClusterInfo) {
			clusterNode.sendMessage(createInfoMessage(true));
			List<RemoteClusterNode> connectedNodes = remoteNodes.values().stream().filter(RemoteClusterNode::isConnected).collect(Collectors.toList());
			sendNodeUpdate(connectedNodes);
		}
		recreateClusterServiceMap();
		if (onNodeConnectionListener != null) {
			onNodeConnectionListener.accept(clusterNode.getNodeAddress());
		}
	}

	private void recreateClusterServiceMap() {
		Map<String, List<RemoteClusterNode>> serviceMap = new HashMap<>();
		for (RemoteClusterNode clusterNode : remoteNodes.values()) {
			for (String service : clusterNode.getServices()) {
				serviceMap.putIfAbsent(service, new ArrayList<>());
				serviceMap.get(service).add(clusterNode);
			}
		}
		clusterServices = serviceMap;
	}

	private void sendNodeUpdate(List<RemoteClusterNode> nodes) {
		byte[] infoMessage = createInfoMessage(true);
		for (RemoteClusterNode node : nodes) {
			node.sendMessage(infoMessage);
		}
	}

	private void sendClusterInfo() {
		byte[] infoMessage = createInfoMessage(true);
		for (RemoteClusterNode remoteClusterNode : remoteNodes.values()) {
			remoteClusterNode.sendMessageAsync(infoMessage);
		}
	}

	@Override
	public void handleMessage(RemoteClusterNode clusterNode, byte[] bytes) {
		try {
			byte[] data = aesCipher.decrypt(bytes);
			int messageRootFieldId = Message.getMessageFieldId(data);
			MessageField messageField = ClusterSchemaRegistry.SCHEMA.getFieldById(messageRootFieldId);
			LOGGER.debug("Handle message: id: {}, field: {}, size: {}, node: {}", messageRootFieldId, messageField, bytes.length, clusterNode);
			if (messageRootFieldId == ClusterFileTransfer.ROOT_FIELD_ID) {
				handleFileTransfer(new ClusterFileTransfer(data, this), clusterNode);
			} else if (messageRootFieldId == ClusterTopicMessage.ROOT_FIELD_ID) {
				ClusterTopicMessage clusterTopicMessage = new ClusterTopicMessage(data, this);
				ClusterTopic clusterTopic = clusterTopics.get(clusterTopicMessage.getTopic());
				if (clusterTopic != null) {
					clusterTopic.handleMessage(clusterTopicMessage);
				} else {
					LOGGER.error("Topic message but no handler: {}", clusterTopicMessage.getTopic());
				}
			} else {
				executor.submit(() -> {
					try {
						switch (messageRootFieldId) {
							case ClusterNodeInfo.ROOT_FIELD_ID ->
									handleClusterNodeInfo(new ClusterNodeInfo(data, this), clusterNode);
							case ClusterFileTransferResponse.ROOT_FIELD_ID ->
									handleFileTransferResponse(new ClusterFileTransferResponse(data, this), clusterNode);
							case ServiceClusterRequest.ROOT_FIELD_ID ->
									handleServiceClusterRequest(new ServiceClusterRequest(data, this), clusterNode);
							case ServiceClusterResponse.ROOT_FIELD_ID ->
									handleServiceClusterResponse(new ServiceClusterResponse(data, this), clusterNode);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				});
			}
		} catch (Exception e) {
			e.printStackTrace();
		}


	}

	public <REQUEST extends Message, RESPONSE extends Message> Mono<RESPONSE> createServiceTask(String serviceName, String method, REQUEST request, MessageDecoder<RESPONSE> responseDecoder) {
		long requestId = requestIdGenerator.incrementAndGet();
		Mono<RESPONSE> mono = Mono.<CompletableFuture<ServiceClusterResponse>>create(monoSink -> {
					RemoteClusterNode clusterNode = getRandomServiceProvider(serviceName);
					if (clusterNode == null) {
						LOGGER.warn("No cluster member available for service: {}, method: {}, with request: {}", serviceName, method, request);
						monoSink.error(new Exception("Error: no cluster member available!"));
						return;
					}
					LOGGER.debug("Create cluster task for member: {}", clusterNode);
					AtomicBoolean disposed = new AtomicBoolean();
					monoSink.onDispose(() -> disposed.set(true));
					try {
						byte[] messageBytes = request.toBytes(file -> ExceptionUtil.softenExceptions(() -> sendFile(file, clusterNode, disposed)));
						if (disposed.get()) {
							return;
						}
						ServiceClusterRequest serviceClusterRequest = new ServiceClusterRequest().setRequestId(requestId).setServiceName(serviceName).setMethod(method).setRequestData(messageBytes);
						CompletableFuture<ServiceClusterResponse> completableFuture = new CompletableFuture<>();
						serviceResponseFutureMap.put(requestId, completableFuture);
						sendMessage(serviceClusterRequest, clusterNode);
						monoSink.success(completableFuture);
					} catch (Exception e) {
						monoSink.error(e);
					}
				})
				.flatMap(Mono::fromFuture)
				.map(response -> responseDecoder.decode(response.getResponseData(), this))
				.subscribeOn(Schedulers.boundedElastic())
				.retryWhen(Retry.backoff(retryMaxAttempts, retryBackoffDuration));
		return mono.timeout(Duration.ofMinutes(5)).doAfterTerminate(() -> serviceResponseFutureMap.remove(requestId));
	}

	private void handleServiceClusterRequest(ServiceClusterRequest request, RemoteClusterNode clusterNode) throws Exception {
		LOGGER.debug("Handle cluster request: {}, node: {}", request, clusterNode);
		AbstractClusterService clusterService = localServices.get(request.getServiceName());
		ServiceClusterResponse response = new ServiceClusterResponse().setRequestId(request.getRequestId());
		if (clusterService != null) {
			byte[] message = clusterService.handleMessage(request.getMethod(), request.getRequestData(), this, file -> ExceptionUtil.softenExceptions(() -> sendFile(file, clusterNode, null)));
			response.setResponseData(message);
		} else {
			LOGGER.error("Could not find requested service {}", request.getServiceName());
			response.setError(true).setErrorMessage("could not find requested service: " + request.getServiceName());
		}
		sendMessage(response, clusterNode);
	}

	private void handleServiceClusterResponse(ServiceClusterResponse serviceClusterResponse, RemoteClusterNode clusterNode) {
		LOGGER.debug("Handle cluster response: {}, node: {}", serviceClusterResponse, clusterNode);
		CompletableFuture<ServiceClusterResponse> completableFuture = serviceResponseFutureMap.get(serviceClusterResponse.getRequestId());
		if (completableFuture != null) {
			completableFuture.complete(serviceClusterResponse);
		}
	}

	@Override
	public byte[] createInitMessage() {
		return createInfoMessage(false);
	}

	@Override
	public byte[] getKeepAliveMessage() {
		try {
			return aesCipher.encrypt(new KeepAliveMessage().toBytes());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private byte[] createInfoMessage(boolean response) {
		try {
			ClusterNodeInfo nodeInfo = new ClusterNodeInfo();
			nodeInfo.setResponse(response);
			nodeInfo.setLocalNode(
					new ClusterNodeData()
							.setNodeId(this.localNode.getNodeId())
							.setAvailableServices(localServices.keySet().toArray(new String[0]))
			);
			for (RemoteClusterNode remoteNode : remoteNodes.values()) {
				if (remoteNode.isOutgoing() && remoteNode.isConnected()) {
					nodeInfo.addKnownRemoteNodes(remoteNode.getClusterNodeData());
				}
			}
			for (ClusterTopic clusterTopic : clusterTopics.values()) {
				nodeInfo.addClusterTopics(clusterTopic.createTopicInfo());
			}

			return aesCipher.encrypt(nodeInfo.toBytes());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public File getFile(String fileId) {
		return fileTransferMap.get(fileId);
	}

	private File getTransferFile(String fileId) {
		return new File(fileTransferPath, fileId + ".tmp");
	}

	private String sendFile(File file, RemoteClusterNode clusterNode, AtomicBoolean disposed) throws Exception {
		LOGGER.info("Send file: {}, node: {}", file.getName(), clusterNode);
		String fileId = UUID.randomUUID().toString().replace("-", ".");
		BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
		byte[] buf = new byte[10_000];
		int read;
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		boolean initialMessage = true;
		int messageIndex = 0;
		while ((read = bis.read(buf)) >= 0) {
			bos.write(buf, 0, read);
			if (bos.size() >= 10_000_000) {
				byte[] bytes = bos.toByteArray();
				ClusterFileTransfer fileTransfer = new ClusterFileTransfer()
						.setFileId(fileId)
						.setData(bytes)
						.setLength(bytes.length)
						.setMessageIndex(messageIndex++)
						.setInitialMessage(initialMessage);
				//todo if this is last message - lastMessage must be set!
				initialMessage = false;
				bos.reset();
				if (disposed != null && disposed.get()) {
					return null;
				}
				sendMessage(fileTransfer, clusterNode);
			}
		}
		ClusterFileTransfer fileTransfer = new ClusterFileTransfer()
				.setFileId(fileId)
				.setData(bos.toByteArray())
				.setMessageIndex(messageIndex)
				.setLastMessage(true);
		if (disposed != null && disposed.get()) {
			return null;
		}
		CompletableFuture<ClusterFileTransferResponse> completableFuture = new CompletableFuture<>();
		fileTransferFutureMap.put(fileId, completableFuture);
		sendMessage(fileTransfer, clusterNode);
		ClusterFileTransferResponse fileTransferResponse = completableFuture.get(120, TimeUnit.SECONDS);
		if (fileTransferResponse.getReceivedData() == file.length()) {
			return fileId;
		} else {
			throw new Exception("Error sending file transfer, expected: " + file.length() + ", actual received: " + fileTransferResponse.getReceivedData());
		}
	}

	private void handleFileTransfer(ClusterFileTransfer fileTransfer, RemoteClusterNode clusterNode) throws Exception {
		LOGGER.info("Handle file transfer: {}, length: {}, last: {}, messageIndex: {}, node: {}", fileTransfer.getFileId(), fileTransfer.getLength(), fileTransfer.getLastMessage(), fileTransfer.getMessageIndex(), clusterNode);
		if (fileTransfer.getData().length != fileTransfer.getLength()) {
			LOGGER.error("Wrong length of file transfer, expected: {}, actual: {}", fileTransfer.getLength(), fileTransfer.getData().length);
		}
		long length = appendFileTransferData(fileTransfer.getFileId(), fileTransfer.getData(), fileTransfer.getInitialMessage());
		if (fileTransfer.getLastMessage()) {
			File file = getTransferFile(fileTransfer.getFileId());
			fileTransferMap.put(fileTransfer.getFileId(), file);
			sendMessage(new ClusterFileTransferResponse().setReceivedData(length).setFileId(fileTransfer.getFileId()), clusterNode);
		}
	}

	private long appendFileTransferData(String fileId, byte[] bytes, boolean initialData) throws IOException {
		File file = getTransferFile(fileId);
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file, !initialData), 32_000);
		bos.write(bytes);
		bos.close();
		return file.length();
	}

	private void handleFileTransferResponse(ClusterFileTransferResponse fileTransferResponse, RemoteClusterNode clusterNode) {
		CompletableFuture<ClusterFileTransferResponse> completableFuture = fileTransferFutureMap.remove(fileTransferResponse.getFileId());
		if (completableFuture != null) {
			completableFuture.complete(fileTransferResponse);
		}
	}
}
