package org.teamapps.cluster.core2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamapps.cluster.message.protocol.*;
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
	private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(2);
	private final ClusterNodeData localNode;
	private final Map<String, ClusterNode> clusterNodeMap = new ConcurrentHashMap<>();
	private final Map<String, AbstractClusterService> localServices = new ConcurrentHashMap<>();
	private final Map<String, List<ClusterNode>> remoteServices = new ConcurrentHashMap<>();
	private final Map<Long, ClusterTask> pendingServiceRequestsMap = new ConcurrentHashMap<>();
	private final File tempDir;
	private ClusterConfig clusterConfig;
	private boolean active = true;

	private ThreadPoolExecutor taskExecutor = new ThreadPoolExecutor(0, 128,
			60L, TimeUnit.SECONDS,
			new ArrayBlockingQueue<>(10_000));
	;

	public static Cluster start() {
		Configuration configuration = Configuration.getConfiguration();
		Cluster cluster = start(configuration.getConfig(CLUSTER_SERVICE, ClusterConfig.getMessageDecoder()));
		configuration.addConfigUpdateListener(cluster::handleConfigUpdate, CLUSTER_SERVICE, ClusterConfig.getMessageDecoder());
		return cluster;
	}

	public static Cluster start(ClusterConfig clusterConfig) {
		return new Cluster(clusterConfig);
	}

	private Cluster(ClusterConfig clusterConfig) {
		this.clusterConfig = clusterConfig;
		localNode = new ClusterNodeData()
				.setNodeId(clusterConfig.getNodeId() != null && !clusterConfig.getNodeId().isBlank() ? clusterConfig.getNodeId() : UUID.randomUUID().toString())
				.setHost(clusterConfig.getHost())
				.setPort(clusterConfig.getPort());
		tempDir = createTempDirSave();
		LOGGER.info("Cluster started, node-id: {}", localNode.getNodeId());
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
				ServerSocket serverSocket = new ServerSocket(localNode.getPort(), 50);
				while (active) {
					try {
						Socket socket = serverSocket.accept();
						new ClusterConnection(this, socket);
					} catch (IOException e) {
						LOGGER.info("Error on server socket:" + e.getMessage());
					}
				}
			} catch (IOException e) {
				LOGGER.info("Error opening server socket:" + e.getMessage(), e);
			}
		});
		thread.setName("server-socket-" + localNode.getHost() + "-" + localNode.getPort());
		thread.setDaemon(false);
		thread.start();
		LOGGER.info("Cluster network started, accepting connections on port: {}", localNode.getPort());

	}

	public synchronized ClusterConnectionResult handleConnectionRequest(ClusterNodeData remoteNode, ClusterConnection connection) {
		ClusterConnectionResult connectionResult = new ClusterConnectionResult().setLocalNode(localNode);
		ClusterNode clusterNode = clusterNodeMap.get(remoteNode.getNodeId());
		List<ClusterNode> existingPeerNodes = new ArrayList<>(clusterNodeMap.values())
				.stream()
				.filter(node -> !node.getNodeData().getNodeId().equals(localNode.getNodeId()))
				.toList();
		List<ClusterNodeData> knownPeers = existingPeerNodes.stream()
				.map(ClusterNode::getNodeData)
				.collect(Collectors.toList());
		if (clusterNode == null) {
			clusterNode = new ClusterNode(this, remoteNode, connection);
			clusterNodeMap.put(remoteNode.getNodeId(), clusterNode);
			if (remoteNode.getHost() != null && remoteNode.getPort() > 0) {
				ClusterNewPeerInfo clusterNewPeerInfo = new ClusterNewPeerInfo()
						.setNewPeer(remoteNode);
				existingPeerNodes.forEach(node -> node.writeMessage(clusterNewPeerInfo));
			}
			return connectionResult
					.setAccepted(true)
					.setKnownPeers(knownPeers);
		} else if (!clusterNode.isConnected()) {
			clusterNode.handleConnectionUpdate(connection);
			return connectionResult
					.setAccepted(true)
					.setKnownPeers(knownPeers);
		} else {
			return connectionResult
					.setAccepted(false);
		}
	}

	public synchronized void handleConnectionResult(ClusterConnectionResult result, ClusterNodeData remoteNode, ClusterConnection connection) {
		if (result.isAccepted()) {
			LOGGER.info("Connection request accepted:" + result.getLocalNode().getNodeId() + ", " + result.getLocalNode().getHost());
			ClusterNode clusterNode = clusterNodeMap.get(remoteNode.getNodeId());
			if (clusterNode == null) {
				clusterNode = new ClusterNode(this, remoteNode, connection);
				clusterNodeMap.put(remoteNode.getNodeId(), clusterNode);
			} else if (!clusterNode.isConnected()) {
				clusterNode.handleConnectionUpdate(connection);
			}
			result.getKnownPeers().stream()
					.filter(peer -> peer.getHost() != null)
					.filter(peer -> peer.getPort() > 0)
					.filter(peer -> !clusterNodeMap.containsKey(peer.getNodeId()))
					.filter(peer -> !localNode.getNodeId().equals(peer.getNodeId()))
					.forEach(this::connectNode);
		} else {
			LOGGER.info("Connection request denied:" + result.getLocalNode().getNodeId() + ", " + result.getLocalNode().getHost());
		}
	}

	public synchronized void handleDisconnect(ClusterNode clusterNode) {
		pendingServiceRequestsMap.values().stream()
				.filter(clusterTask -> Objects.equals(clusterTask.getProcessingNodeId(), clusterNode.getNodeData().getNodeId()))
				.forEach(clusterTask -> {

					executeClusterTask(clusterTask);
				});
	}

	private void connectNode(ClusterNodeData peerNode) {
		new ClusterConnection(this, peerNode);
	}

	private synchronized void sendMessageToAllNodes(Message message) {
		clusterNodeMap.values().forEach(node -> node.writeMessage(message));
	}


	public void handleConfigUpdate(ClusterConfig config) {

	}

	@Override
	public void registerService(AbstractClusterService clusterService) {
		String serviceName = clusterService.getServiceName();
		localServices.put(serviceName, clusterService);
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
		ClusterTask clusterTask = new ClusterTask(serviceName, method, request);
		pendingServiceRequestsMap.put(clusterTask.getTaskId(), clusterTask);
		executeClusterTask(clusterTask);
		clusterTask.waitForResult();
		pendingServiceRequestsMap.remove(clusterTask.getTaskId());
		Message clusterTaskResult = clusterTask.getResult();
		if (clusterTaskResult == null) {
			throw new RuntimeException("Error: execute cluster service method failed:" + serviceName + ", " + method);
		} else {
			return responseDecoder.remap(clusterTaskResult);
		}
	}

	private void executeClusterTask(ClusterTask clusterTask) {
		clusterTask.addExecutionAttempt();
		if (clusterTask.getExecutionAttempts() > 3) {
			LOGGER.warn("Error: stop cluster task, too many retries; service: {}, method: {}", clusterTask.getServiceName(), clusterTask.getMethod());
			clusterTask.setResult(null);
			return;
		}

		AbstractClusterService localService = localServices.get(clusterTask.getServiceName());
		ClusterNode clusterNode = getBestServiceNode(clusterTask.getServiceName());
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
			LOGGER.warn("Error: no service available for cluster task; service: {}, method: {}", clusterTask.getServiceName(), clusterTask.getMethod());
			clusterTask.setResult(null);
		}
	}

	private void runRemoteClusterTask(ClusterNode clusterNode, ClusterTask clusterTask) {
		clusterTask.setProcessingNodeId(clusterNode.getNodeData().getNodeId());
		ClusterServiceMethodRequest clusterServiceMethodRequest = new ClusterServiceMethodRequest()
				.setServiceName(clusterTask.getServiceName())
				.setMethodName(clusterTask.getMethod())
				.setClusterTaskId(clusterTask.getTaskId())
				.setRequestMessage(clusterTask.getRequest());
		clusterNode.writeMessage(clusterServiceMethodRequest);
	}

	private void runLocalClusterTask(AbstractClusterService localService, ClusterTask clusterTask) {
		taskExecutor.execute(() -> {
			try {
				Message message = localService.handleMessage(clusterTask.getMethod(), clusterTask.getRequest());
				clusterTask.setResult(message);
			} catch (Throwable e) {
				clusterTask.setResult(null);
				//todo set error message
				e.printStackTrace();
			}
		});
	}

	private ClusterNode getBestServiceNode(String serviceName) {
		List<ClusterNode> clusterNodes = remoteServices.get(serviceName);
		if (clusterNodes == null) {
			return null;
		} else {
			List<ClusterNode> workloadSortedServices = clusterNodes.stream()
					.filter(ClusterNode::isConnected)
					.sorted(Comparator.comparingInt(ClusterNode::getActiveTasks))
					.toList();
			if (!clusterNodes.isEmpty()) {
				return workloadSortedServices.get(0);
			} else {
				return null;
			}
		}
	}

	public ScheduledExecutorService getScheduledExecutorService() {
		return scheduledExecutorService;
	}

	public void shutDown() {
		try {
			active = false;
			sendMessageToAllNodes(new ClusterNodeShutDownInfo());
			clusterNodeMap.values().forEach(ClusterNode::closeConnection);
			scheduledExecutorService.shutdownNow();
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
}
