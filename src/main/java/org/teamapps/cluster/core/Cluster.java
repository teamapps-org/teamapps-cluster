/*-
 * ========================LICENSE_START=================================
 * TeamApps Cluster
 * ---
 * Copyright (C) 2021 - 2023 TeamApps.org
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

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamapps.cluster.message.protocol.*;
import org.teamapps.commons.event.Event;
import org.teamapps.commons.util.collections.ByKeyComparisonResult;
import org.teamapps.commons.util.collections.CollectionUtil;
import org.teamapps.configuration.Configuration;
import org.teamapps.message.protocol.message.Message;
import org.teamapps.message.protocol.model.ModelCollection;
import org.teamapps.message.protocol.model.PojoObjectDecoder;
import org.teamapps.message.protocol.service.AbstractClusterService;
import org.teamapps.message.protocol.service.ClusterServiceRegistry;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;


public class Cluster implements ClusterServiceRegistry {
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	public static final String CLUSTER_SERVICE = "clusterService";

	public final Event<ClusterNodeData> onLeaderAvailable = new Event<>();
	public final Event<List<ClusterNodeData>> onAvailableNodesChange = new Event<>();

	private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(2);
	private final ClusterNodeData localNode;
	private final Map<String, ClusterNode> clusterNodeMap = new ConcurrentHashMap<>();
	private final Map<String, AbstractClusterService> localServices = new ConcurrentHashMap<>();
	private final Map<String, List<ClusterNode>> nodesByServiceName = new HashMap<>();
	private final Map<ClusterNode, List<String>> servicesByNode = new HashMap<>();
	private final Map<Long, ClusterTask> pendingServiceRequestsMap = new ConcurrentHashMap<>();
	private final File tempDir;
	private ClusterConfig clusterConfig;
	private boolean active = true;
	private ClusterNodeData leaderNode;

	private ThreadPoolExecutor taskExecutor = new ThreadPoolExecutor(0, Integer.MAX_VALUE,
			60L, TimeUnit.SECONDS,
			new SynchronousQueue<>());
	private ServerSocket serverSocket;


	public static Cluster start() {
		Configuration configuration = Configuration.getConfiguration();
		Cluster cluster = start(configuration.getConfig(CLUSTER_SERVICE, ClusterConfig.getMessageDecoder()));
		configuration.addConfigUpdateListener(cluster::handleConfigUpdate, CLUSTER_SERVICE, ClusterConfig.getMessageDecoder());
		return cluster;
	}

	public static Cluster startServerMember(String clusterSecret, int port) {
		return start(new ClusterConfig().setClusterSecret(clusterSecret).setPort(port));
	}

	public static Cluster startClientMember(String clusterSecret, String host, int port) {
		return start(new ClusterConfig().setClusterSecret(clusterSecret).addPeerNodes(new ClusterNodeData().setHost(host).setPort(port)));
	}


	public static Cluster start(ClusterConfig clusterConfig) {
		return new Cluster(clusterConfig);
	}

	private Cluster(ClusterConfig clusterConfig) {
		this.clusterConfig = clusterConfig;
		localNode = new ClusterNodeData()
				.setNodeId(clusterConfig.getNodeId() != null && !clusterConfig.getNodeId().isBlank() ? clusterConfig.getNodeId() : UUID.randomUUID().toString())
				.setHost(clusterConfig.getHost())
				.setPort(clusterConfig.getPort())
				.setLeaderNode(clusterConfig.isLeaderNode());
		if (clusterConfig.isLeaderNode()) {
			leaderNode = localNode;
			onLeaderAvailable.fire(leaderNode);
		}
		tempDir = createTempDirSave();
		LOGGER.info("Cluster node [{}]: started {}", localNode.getNodeId(), clusterConfig.isLeaderNode() ? "as leader node" : "");
		startServerSocket(localNode);
		if (clusterConfig.getPeerNodes() != null) {
			clusterConfig.getPeerNodes().stream()
					.filter(node -> node.getPort() > 0)
					.filter(node -> node.getHost() != null)
					.forEach(this::connectNode);
		}
		Runtime.getRuntime().addShutdownHook(new Thread(this::shutDown));
	}

	private void startServerSocket(ClusterNodeData localNode) {
		if (localNode.getPort() <= 0) {
			return;
		}
		Thread thread = new Thread(() -> {
			try {
				serverSocket = new ServerSocket(localNode.getPort(), 50);
				while (active) {
					try {
						Socket socket = serverSocket.accept();
						new ClusterConnection(this, socket);
					} catch (IOException e) {
						LOGGER.info("Cluster node [{}]: error on server socket: {}", localNode.getNodeId(), e.getMessage());
					}
				}
			} catch (IOException e) {
				LOGGER.info("Cluster node [{}]: error opening server socket: {}", localNode.getNodeId(), e.getMessage(), e);
			}
		});
		thread.setName("server-socket-" + localNode.getHost() + "-" + localNode.getPort());
		thread.setDaemon(false);
		thread.start();
		LOGGER.info("Cluster node [{}]: network started, accepting connections on port: {}", localNode.getNodeId(), localNode.getPort());

	}

	protected synchronized ClusterConnectionResult handleConnectionRequest(ClusterConnectionRequest request, ClusterConnection connection) {
		ClusterNodeData remoteNode = request.getLocalNode();
		LOGGER.info("Cluster node [{}]: connection requested from: {}, {}", localNode.getNodeId(), request.getLocalNode().getNodeId(), request.getLocalNode().getHost());
		String[] nodeServices = request.getLocalServices();
		ClusterConnectionResult connectionResult = new ClusterConnectionResult().setLocalNode(localNode);
		if (request.getLeaderNode() != null) {
			if (leaderNode != null && !leaderNode.getNodeId().equals(request.getLeaderNode().getNodeId())) {
				LOGGER.error("Cluster node [{}]: error: connection requested denied from {}, {} - different leader node: {} vs {}", localNode.getNodeId(), request.getLocalNode().getNodeId(), request.getLocalNode().getHost(), leaderNode.getNodeId(), request.getLocalNode().getNodeId());
				return connectionResult
						.setAccepted(false);
			} else if (leaderNode == null) {
				leaderNode = remoteNode;
				sendLeaderNodeUpdateToPeers();
				onLeaderAvailable.fire(leaderNode);
				LOGGER.info("Cluster node [{}]: new leader node: {}", localNode.getNodeId(), request.getLeaderNode().getNodeId());
			}
		}
		ClusterNode clusterNode = clusterNodeMap.get(remoteNode.getNodeId());
		List<ClusterNodeData> knownPeers = new ArrayList<>(clusterNodeMap.values())
				.stream()
				.map(ClusterNode::getNodeData)
				.filter(nodeData -> !nodeData.getNodeId().equals(localNode.getNodeId()))
				.filter(nodeData -> nodeData.getPort() > 0)
				.toList();

		if (clusterNode == null || !clusterNode.isConnected()) {
			if (clusterNode == null) {
				clusterNode = new ClusterNode(this, remoteNode, connection);
				clusterNodeMap.put(remoteNode.getNodeId(), clusterNode);
			} else {
				clusterNode.handleConnectionUpdate(connection);
			}
			if (nodeServices != null) {
				updateClusterNodeServices(clusterNode, nodeServices);
			}
			if (remoteNode.getHost() != null && remoteNode.getPort() > 0) {
				sendMessageToPeerNodes(new ClusterNewPeerInfo().setNewPeer(remoteNode), remoteNode);
			}

			request.getKnownPeers().stream()
					.filter(peer -> peer.getHost() != null)
					.filter(peer -> peer.getPort() > 0)
					.filter(peer -> !clusterNodeMap.containsKey(peer.getNodeId()))
					.filter(peer -> !localNode.getNodeId().equals(peer.getNodeId()))
					.forEach(this::connectNode);

			handleAvailableClusterNodesChanged();

			return connectionResult
					.setAccepted(true)
					.setKnownPeers(knownPeers)
					.setLocalServices(localServices.isEmpty() ? null : localServices.keySet().toArray(new String[0]))
					.setKnownServices(nodeServices);
		} else {
			return connectionResult
					.setAccepted(false);
		}
	}

	protected synchronized void handleConnectionResult(ClusterConnectionResult result, ClusterNodeData remoteNode, ClusterConnection connection) {
		if (result.isAccepted()) {
			LOGGER.info("Cluster node [{}]: connection request accepted from: {}, {}", localNode.getNodeId(), result.getLocalNode().getNodeId(), result.getLocalNode().getHost());
			ClusterNode clusterNode = clusterNodeMap.get(remoteNode.getNodeId());
			if (result.getLeaderNode() != null) {
				if (leaderNode == null) {
					leaderNode = result.getLeaderNode();
					sendLeaderNodeUpdateToPeers();
					onLeaderAvailable.fire(leaderNode);
					LOGGER.info("Cluster node [{}]: new leader node: {}", localNode.getNodeId(), result.getLeaderNode().getNodeId());
				} else if (!leaderNode.getNodeId().equals(result.getLocalNode().getNodeId())) {
					LOGGER.error("Cluster node [{}]: error: connection result denied from {}, {} - different leader node: {} vs {}", localNode.getNodeId(), result.getLocalNode().getNodeId(), result.getLocalNode().getHost(), leaderNode.getNodeId(), result.getLocalNode().getNodeId());
					return;
				}
			}
			if (clusterNode == null) {
				clusterNode = new ClusterNode(this, remoteNode, connection);
				clusterNodeMap.put(remoteNode.getNodeId(), clusterNode);
				sendMessageToPeerNodes(new ClusterNewPeerInfo().setNewPeer(remoteNode), remoteNode);
			} else if (!clusterNode.isConnected()) {
				clusterNode.handleConnectionUpdate(connection);
			}
			result.getKnownPeers().stream()
					.filter(peer -> peer.getHost() != null)
					.filter(peer -> peer.getPort() > 0)
					.filter(peer -> !clusterNodeMap.containsKey(peer.getNodeId()))
					.filter(peer -> !localNode.getNodeId().equals(peer.getNodeId()))
					.forEach(this::connectNode);
			String[] nodeServices = result.getLocalServices();
			if (nodeServices != null) {
				updateClusterNodeServices(clusterNode, nodeServices);
			}
			if (!localServices.isEmpty()) {
				if (result.getKnownServices() == null || (result.getKnownServices().length != localServices.size())) {
					String[] services = localServices.keySet().toArray(new String[0]);
					ClusterAvailableServicesUpdate servicesUpdate = new ClusterAvailableServicesUpdate().setServices(services);
					sendMessageToPeerNodes(servicesUpdate);
				}
			}
			handleAvailableClusterNodesChanged();
		} else {
			LOGGER.info("Cluster node [{}]: connection request denied from: {}, {}", localNode.getNodeId(), result.getLocalNode().getNodeId(), result.getLocalNode().getHost());
		}
	}

	protected void handleServiceMethodExecutionRequest(ClusterServiceMethodRequest methodRequest, ClusterNode clusterNode) {
		LOGGER.info("Cluster node [{}]: handle service method request {}/{} from {}", localNode.getNodeId(), methodRequest.getServiceName(), methodRequest.getMethodName(), clusterNode.getNodeData().getNodeId());
		AbstractClusterService localService = localServices.get(methodRequest.getServiceName());
		ClusterServiceMethodResult methodResult = new ClusterServiceMethodResult()
				.setClusterTaskId(methodRequest.getClusterTaskId())
				.setServiceName(methodRequest.getServiceName())
				.setMethodName(methodRequest.getMethodName());
		if (localService != null) {
			taskExecutor.execute(() -> {
				LOGGER.info("Cluster node [{}]: execute task", localNode.getNodeId());
				try {
					Message message = localService.handleMessage(methodRequest.getMethodName(), methodRequest.getRequestMessage());
					methodResult.setResultMessage(message);
					clusterNode.writeMessage(methodResult);
				} catch (Throwable e) {
					e.printStackTrace();
					String stackTrace = ExceptionUtils.getStackTrace(e);
					methodResult
							.setError(true)
							.setErrorType(ClusterServiceMethodErrorType.SERVICE_EXCEPTION)
							.setErrorMessage(e.getMessage())
							.setErrorStackTrace(stackTrace);
					clusterNode.writeMessage(methodResult);
				}
			});
		} else {
			methodResult
					.setError(true)
					.setErrorType(ClusterServiceMethodErrorType.SERVICE_EXCEPTION)
					.setErrorMessage("Error: missing service:" + methodRequest.getMethodName());
			clusterNode.writeMessage(methodResult);
		}


	}

	protected void handleServiceMethodExecutionResult(ClusterServiceMethodResult methodResult, ClusterNode clusterNode) {
		LOGGER.info("Cluster node [{}]: handle service method result {}/{} from {}", localNode.getNodeId(), methodResult.getServiceName(), methodResult.getMethodName(), clusterNode.getNodeData().getNodeId());
		ClusterTask clusterTask = pendingServiceRequestsMap.get(methodResult.getClusterTaskId());
		if (clusterTask != null) {
			clusterTask.setError(methodResult.isError());
			clusterTask.setErrorType(methodResult.getErrorType());
			clusterTask.setErrorMessage(methodResult.getErrorMessage());
			clusterTask.setErrorStackTrace(methodResult.getErrorStackTrace());
			if (!methodResult.isError() && methodResult.getResultMessage() != null) {
				clusterTask.setFinished(true);
			}
			clusterTask.setResult(methodResult.getResultMessage());
		}
	}

	protected void handleClusterNewPeerInfo(ClusterNewPeerInfo newPeerInfo, ClusterNode clusterNode) {
		if (!clusterNodeMap.containsKey(newPeerInfo.getNewPeer().getNodeId())) {
			connectNode(newPeerInfo.getNewPeer());
		}
	}

	protected void handleClusterNewLeaderInfo(ClusterNewLeaderInfo newLeaderInfo, ClusterNode clusterNode) {
		if (leaderNode == null) {
			leaderNode = newLeaderInfo.getLeaderNode();
			sendLeaderNodeUpdateToPeers();
			onLeaderAvailable.fire(leaderNode);
			LOGGER.info("Cluster node [{}]: new leader node: {}", localNode.getNodeId(), newLeaderInfo.getLeaderNode().getNodeId());
		}
	}


	protected void handleClusterAvailableServicesUpdate(ClusterAvailableServicesUpdate availableServicesUpdate, ClusterNode clusterNode) {
		updateClusterNodeServices(clusterNode, availableServicesUpdate.getServices());
	}

	protected synchronized void handleDisconnect(ClusterNode clusterNode) {
		pendingServiceRequestsMap.values().stream()
				.filter(clusterTask -> Objects.equals(clusterTask.getProcessingNodeId(), clusterNode.getNodeData().getNodeId()))
				.forEach(this::executeClusterTask);
		handleAvailableClusterNodesChanged();
	}


	private void handleAvailableClusterNodesChanged() {
		List<ClusterNodeData> availableNodes = getAvailablePeerNodes();
		onAvailableNodesChange.fire(availableNodes);
	}

	private List<ClusterNodeData> getAvailablePeerNodes() {
		return clusterNodeMap.values().stream()
				.filter(ClusterNode::isConnected)
				.map(ClusterNode::getNodeData)
				.toList();
	}

	private void handleConfigUpdate(ClusterConfig config) {

	}

	private synchronized void updateClusterNodeServices(ClusterNode clusterNode, String[] servicesArray) {
		List<String> services = servicesArray == null ? Collections.emptyList() : Arrays.stream(servicesArray).toList();
		LOGGER.info("Cluster node [{}]: update peer node services for {} with services: {}", localNode.getNodeId(), clusterNode.getNodeData().getNodeId(), String.join(", ", services));

		List<String> previousServices = servicesByNode.get(clusterNode);
		if (previousServices != null) {
			ByKeyComparisonResult<String, String, String> keyComparator = CollectionUtil.compareByKey(previousServices, services, o -> o, o -> o);
			//remove services that don't exist anymore
			keyComparator.getAEntriesNotInB().forEach(service -> nodesByServiceName.get(service).remove(clusterNode));
			//add new services
			keyComparator.getBEntriesNotInA().forEach(service -> nodesByServiceName.computeIfAbsent(service, s -> new ArrayList<>()).add(clusterNode));
		} else {
			services.forEach(service -> nodesByServiceName.computeIfAbsent(service, s -> new ArrayList<>()).add(clusterNode));
		}
		servicesByNode.put(clusterNode, services);
	}

	protected void connectNode(ClusterNodeData peerNode) {
		List<ClusterNodeData> knownPeers = new ArrayList<>(clusterNodeMap.values())
				.stream()
				.map(ClusterNode::getNodeData)
				.filter(nodeData -> !nodeData.getNodeId().equals(localNode.getNodeId()))
				.filter(nodeData -> nodeData.getPort() > 0)
				.toList();
		String[] services = localServices.keySet().toArray(new String[0]);
		ClusterConnectionRequest clusterConnectionRequest = new ClusterConnectionRequest()
				.setLocalNode(localNode)
				.setLocalServices(services)
				.setLeaderNode(leaderNode)
				.setKnownPeers(knownPeers);
		new ClusterConnection(this, peerNode, clusterConnectionRequest);
	}

	private synchronized void sendMessageToPeerNodes(Message message, ClusterNodeData... excludingNodes) {
		Set<String> excludeSet = excludingNodes == null ? new HashSet<>() : Arrays.stream(excludingNodes).map(ClusterNodeData::getNodeId).collect(Collectors.toSet());
		List<ClusterNode> peerNodes = clusterNodeMap.values()
				.stream()
				.filter(node -> node.isConnected() && !excludeSet.contains(node.getNodeData().getNodeId()))
				.toList();
		LOGGER.info("Cluster node [{}]: send to peer nodes: {}, message: {}", localNode.getNodeId(), peerNodes.size(), message.getMessageDefUuid());
		peerNodes.forEach(node -> node.writeMessage(message));
	}


	private synchronized void sendLeaderNodeUpdateToPeers() {
		List<ClusterNode> peerNodes = clusterNodeMap.values()
				.stream()
				.toList();
		ClusterNewLeaderInfo clusterNewLeaderInfo = new ClusterNewLeaderInfo().setLeaderNode(leaderNode);
		peerNodes.forEach(node -> node.writeMessage(clusterNewLeaderInfo));
	}

	public void sendMessage(String nodeId, Message message) {
		ClusterNode clusterNode = clusterNodeMap.get(nodeId);
		if (clusterNode != null) {
			clusterNode.writeMessage(message);
		}
	}

	public void sendMessage(List<String> nodeIds, Message message) {
		for (String nodeId : nodeIds) {
			sendMessage(nodeId, message);
		}
	}

	@Override
	public void registerService(AbstractClusterService clusterService) {
		LOGGER.info("Cluster node [{}]: register local service: {}", localNode.getNodeId(), clusterService.getServiceName());
		String serviceName = clusterService.getServiceName();
		localServices.put(serviceName, clusterService);
		String[] services = localServices.keySet().toArray(new String[0]);
		ClusterAvailableServicesUpdate servicesUpdate = new ClusterAvailableServicesUpdate().setServices(services);
		sendMessageToPeerNodes(servicesUpdate);
	}

	@Override
	public void registerModelCollection(ModelCollection modelCollection) {

	}

	@Override
	public boolean isServiceAvailable(String serviceName) {
		return localServices.containsKey(serviceName);
	}

	@Override
	public <REQUEST extends Message, RESPONSE extends Message> RESPONSE executeServiceMethod(String serviceName, String method, REQUEST request, PojoObjectDecoder<RESPONSE> responseDecoder) {
		return executeServiceMethod(null, serviceName, method, request, responseDecoder);
	}

	@Override
	public <REQUEST extends Message, RESPONSE extends Message> RESPONSE executeServiceMethod(String clusterNodeId, String serviceName, String method, REQUEST request, PojoObjectDecoder<RESPONSE> responseDecoder) {
		LOGGER.info("Cluster node: {} - execute service method {}/{}" + (clusterNodeId != null ? ", on node {}" : ""), localNode.getNodeId(), serviceName, method, clusterNodeId);
		ClusterTask clusterTask = new ClusterTask(serviceName, method, request, clusterNodeId);
		pendingServiceRequestsMap.put(clusterTask.getTaskId(), clusterTask);
		while (!clusterTask.isFinished()) {
			clusterTask.startProcessing();
			executeClusterTask(clusterTask);
			clusterTask.waitForResult();
			if (clusterTask.isFinished()) {
				pendingServiceRequestsMap.remove(clusterTask.getTaskId());
				Message clusterTaskResult = clusterTask.getResult();
				return responseDecoder.remap(clusterTaskResult);
			} else if (clusterTask.isRetryLimitReached()) {
				LOGGER.warn("Cluster node [{}]: method execution {}/{} caused error '{}' with execution attempts: {}, retry limit reached - giving up!", localNode.getNodeId(), serviceName, method, clusterTask.getErrorMessage(), clusterTask.getExecutionAttempts());
				pendingServiceRequestsMap.remove(clusterTask.getTaskId());
				throw new RuntimeException("Error: execute cluster service method failed:" + serviceName + ", " + method);
			} else {
				LOGGER.warn("Cluster node [{}]: method execution {}/{} caused error '{}' with execution attempts: {}, will retry...", localNode.getNodeId(), serviceName, method, clusterTask.getErrorMessage(), clusterTask.getExecutionAttempts());
			}
		}
		throw new RuntimeException("Error: execute cluster service method failed:" + serviceName + ", " + method);
	}

	@Override
	public <MESSAGE extends Message> void executeServiceBroadcast(String serviceName, String method, MESSAGE message) {
		LOGGER.info("Cluster node: {} - execute service broadcast method {}/{}", localNode.getNodeId(), serviceName, method);
		List<ClusterNode> clusterNodes = nodesByServiceName.get(serviceName);
		if (clusterNodes != null) {
			ClusterServiceBroadcastMessage broadcastMessage = new ClusterServiceBroadcastMessage()
					.setServiceName(serviceName)
					.setMethodName(method)
					.setMessage(message);
			clusterNodes.forEach(node -> node.writeMessage(broadcastMessage));
		}
	}

	public void handleServiceBroadcastMessage(ClusterServiceBroadcastMessage broadcastMessage, ClusterNode clusterNode) {
		String serviceName = broadcastMessage.getServiceName();
		String method = broadcastMessage.getMethodName();
		LOGGER.info("Cluster node [{}]: handle broadcast message from {}: {}/{}", localNode.getNodeId(), clusterNode.getNodeData().getNodeId(), serviceName, method);
		AbstractClusterService clusterService = localServices.get(serviceName);
		if (clusterService != null) {
			taskExecutor.execute(() -> clusterService.handleMessage(method, broadcastMessage.getMessage()));
		}
	}


	private void executeClusterTask(ClusterTask clusterTask) {
		if (clusterTask.isRetryLimitReached()) {
			LOGGER.warn("Cluster node [{}]: Error: stop cluster task, too many retries; service: {}, method: {}", localNode.getNodeId(), clusterTask.getServiceName(), clusterTask.getMethod());
			clusterTask.setError(true);
			clusterTask.setErrorMessage("Error: too many retries");
			clusterTask.setResult(null);
			return;
		}
		clusterTask.addExecutionAttempt();

		AbstractClusterService localService = localServices.get(clusterTask.getServiceName());
		ClusterNode clusterNode;
		if (clusterTask.isFixedServiceNode()) {
			if (localNode.getNodeId().equals(clusterTask.getFixedServiceNodeId())) {
				clusterNode = null;
			} else {
				clusterNode = clusterNodeMap.get(clusterTask.getFixedServiceNodeId());
			}
		} else {
			clusterNode = getBestServiceNode(clusterTask.getServiceName());
		}
		if (localService != null && clusterNode != null) {
			if (getActiveTasks() <= clusterNode.getActiveTasks()) {
				runLocalClusterTask(localService, clusterTask);
			} else {
				runRemoteClusterTask(clusterNode, clusterTask);
			}
		} else if (localService != null) {
			runLocalClusterTask(localService, clusterTask);
		} else if (clusterNode != null) {
			runRemoteClusterTask(clusterNode, clusterTask);
		} else {
			LOGGER.warn("Cluster node [{}]: Error: no service available for cluster task; service: {}, method: {}", localNode.getNodeId(), clusterTask.getServiceName(), clusterTask.getMethod());
			clusterTask.setError(true);
			clusterTask.setErrorMessage("Error: no service available");
			clusterTask.setResult(null);
		}
	}

	private void runLocalClusterTask(AbstractClusterService localService, ClusterTask clusterTask) {
		taskExecutor.execute(() -> {
			try {
				Message message = localService.handleMessage(clusterTask.getMethod(), clusterTask.getRequest());
				clusterTask.setFinished(true);
				clusterTask.setResult(message);
			} catch (Throwable e) {
				String stackTrace = ExceptionUtils.getStackTrace(e);
				clusterTask.setError(true);
				clusterTask.setErrorMessage(e.getMessage());
				clusterTask.setErrorStackTrace(stackTrace);
				clusterTask.setResult(null);
				e.printStackTrace();
			}
		});
	}

	private void runRemoteClusterTask(ClusterNode clusterNode, ClusterTask clusterTask) {
		clusterNode.addTask();
		clusterTask.setProcessingNodeId(clusterNode.getNodeData().getNodeId());
		ClusterServiceMethodRequest clusterServiceMethodRequest = new ClusterServiceMethodRequest()
				.setServiceName(clusterTask.getServiceName())
				.setMethodName(clusterTask.getMethod())
				.setClusterTaskId(clusterTask.getTaskId())
				.setRequestMessage(clusterTask.getRequest());
		clusterNode.writeMessage(clusterServiceMethodRequest);
	}

	private synchronized ClusterNode getBestServiceNode(String serviceName) {
		List<ClusterNode> clusterNodes = nodesByServiceName.get(serviceName);
		if (clusterNodes == null) {
			return null;
		} else {
			List<ClusterNode> workloadSortedServices = clusterNodes.stream()
					.filter(ClusterNode::isConnected)
					.sorted(Comparator.comparingInt(ClusterNode::getActiveTasks))
					.toList();
			if (!workloadSortedServices.isEmpty()) {
				return workloadSortedServices.get(0);
			} else {
				return null;
			}
		}
	}

	protected ScheduledExecutorService getScheduledExecutorService() {
		return scheduledExecutorService;
	}

	public void shutDown() {
		try {
			LOGGER.info("Cluster node [{}]: shutdown cluster node", localNode.getNodeId());
			active = false;
			sendMessageToPeerNodes(new ClusterNodeShutDownInfo());
			clusterNodeMap.values().forEach(ClusterNode::closeConnection);
			scheduledExecutorService.shutdownNow();
			if (serverSocket != null) {
				serverSocket.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static File createTempDirSave() {
		try {
			return Files.createTempDirectory("temp").toFile();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public ClusterNodeData getLocalNode() {
		return localNode;
	}

	public ClusterConfig getClusterConfig() {
		return clusterConfig;
	}

	public File getTempDir() {
		return tempDir;
	}

	private int getActiveTasks() {
		return taskExecutor.getActiveCount() + taskExecutor.getQueue().size();
	}

	private long getCompletedTaskCount() {
		return taskExecutor.getCompletedTaskCount();
	}

	public List<ClusterNodeData> getPeerNodes(boolean connectedOnly) {
		return clusterNodeMap.values().stream()
				.filter(node -> !connectedOnly || node.isConnected())
				.map(ClusterNode::getNodeData)
				.collect(Collectors.toList());
	}

	public List<ClusterNode> getClusterNodes() {
		return new ArrayList<>(clusterNodeMap.values());
	}

	public boolean isConnected(ClusterNodeData clusterNodeData) {
		ClusterNode clusterNode = clusterNodeMap.get(clusterNodeData.getNodeId());
		return clusterNode != null && clusterNode.isConnected();
	}

	public synchronized List<String> getClusterNodeServices(ClusterNode clusterNode) {
		List<String> services = servicesByNode.get(clusterNode);
		return new ArrayList<>(services == null ? Collections.emptyList() : services);
	}

	public ClusterNodeData getLeaderNode() {
		return leaderNode;
	}

	public boolean isLeaderNode() {
		return leaderNode != null && leaderNode.getNodeId().equals(localNode.getNodeId());
	}
}
