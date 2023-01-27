package org.teamapps.cluster.core2;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamapps.cluster.message.protocol.*;
import org.teamapps.commons.collections.CollectionsByKeyComparator;
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
	private final Map<String, List<ClusterNode>> nodesByServiceName = new HashMap<>();
	private final Map<ClusterNode, List<String>> servicesByNode = new HashMap<>();
	private final Map<Long, ClusterTask> pendingServiceRequestsMap = new ConcurrentHashMap<>();
	private final File tempDir;
	private ClusterConfig clusterConfig;
	private boolean active = true;

	private ThreadPoolExecutor taskExecutor = new ThreadPoolExecutor(0, 128,
			60L, TimeUnit.SECONDS,
			new ArrayBlockingQueue<>(10_000));

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
				.setPort(clusterConfig.getPort());
		tempDir = createTempDirSave();
		LOGGER.info("Cluster node [{}]: started", localNode.getNodeId());
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
		String[] nodeServices = request.getLocalServices();
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
			if (nodeServices != null) {
				updateClusterNodeServices(clusterNode, nodeServices);
			}
			return connectionResult
					.setAccepted(true)
					.setKnownPeers(knownPeers)
					.setLocalServices(localServices.isEmpty() ? null : localServices.keySet().toArray(new String[0]))
					.setKnownServices(nodeServices);
		} else if (!clusterNode.isConnected()) {
			clusterNode.handleConnectionUpdate(connection);
			if (nodeServices != null) {
				updateClusterNodeServices(clusterNode, nodeServices);
			}
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
		ClusterTask clusterTask = pendingServiceRequestsMap.remove(methodResult.getClusterTaskId());
		if (clusterTask != null) {
			clusterTask.setError(methodResult.isError());
			clusterTask.setErrorType(methodResult.getErrorType());
			clusterTask.setErrorMessage(methodResult.getErrorMessage());
			clusterTask.setErrorStackTrace(methodResult.getErrorStackTrace());
			clusterTask.setResult(methodResult.getResultMessage());
		}
	}

	protected void handleClusterNewPeerInfo(ClusterNewPeerInfo newPeerInfo, ClusterNode clusterNode) {
		//todo implement!
	}



	protected void handleClusterAvailableServicesUpdate(ClusterAvailableServicesUpdate availableServicesUpdate, ClusterNode clusterNode) {
		updateClusterNodeServices(clusterNode, availableServicesUpdate.getServices());
	}

	protected synchronized void handleDisconnect(ClusterNode clusterNode) {
		pendingServiceRequestsMap.values().stream()
				.filter(clusterTask -> Objects.equals(clusterTask.getProcessingNodeId(), clusterNode.getNodeData().getNodeId()))
				.forEach(this::executeClusterTask);
	}

	private void handleConfigUpdate(ClusterConfig config) {

	}

	private synchronized void updateClusterNodeServices(ClusterNode clusterNode, String[] servicesArray) {
		List<String> services = servicesArray == null ? Collections.emptyList() : Arrays.stream(servicesArray).toList();
		LOGGER.info("Cluster node [{}]: update peer node services for {} with services: {}", localNode.getNodeId(), clusterNode.getNodeData().getNodeId(), String.join(", ", services));

		List<String> previousServices = servicesByNode.get(clusterNode);
		if (previousServices != null) {
			CollectionsByKeyComparator<String, String> keyComparator = new CollectionsByKeyComparator<>(previousServices, services, o -> o, o -> o);
			//remove services that don't exist anymore
			keyComparator.getAEntriesNotInB().forEach(service -> nodesByServiceName.get(service).remove(clusterNode));
			//add new services
			keyComparator.getBEntriesNotInA().forEach(service -> nodesByServiceName.computeIfAbsent(service, s -> new ArrayList<>()).add(clusterNode));
		} else {
			services.forEach(service -> nodesByServiceName.computeIfAbsent(service, s -> new ArrayList<>()).add(clusterNode));
		}
		servicesByNode.put(clusterNode, services);
	}

	private void connectNode(ClusterNodeData peerNode) {
		new ClusterConnection(this, peerNode, new ArrayList<>(localServices.keySet()));
	}

	private synchronized void sendMessageToPeerNodes(Message message) {
		LOGGER.info("Cluster node [{}]: send to peer nodes: {}, message: {}", localNode.getNodeId(), clusterNodeMap.size(), message.getMessageDefUuid());
		clusterNodeMap.values().forEach(node -> node.writeMessage(message));
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
		LOGGER.info("Cluster node: {} - execute service method {}/{}", localNode.getNodeId(), serviceName, method);
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
			LOGGER.warn("Cluster node [{}]: Error: stop cluster task, too many retries; service: {}, method: {}", localNode.getNodeId(), clusterTask.getServiceName(), clusterTask.getMethod());
			clusterTask.setError(true);
			clusterTask.setErrorMessage("Error: too many retries");
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
				clusterTask.setResult(message);
			} catch (Throwable e) {
				clusterTask.setResult(null);
				//todo set error message
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
			if (!clusterNodes.isEmpty()) {
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
			active = false;
			sendMessageToPeerNodes(new ClusterNodeShutDownInfo());
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

	public synchronized List<String> getClusterNodeServices(ClusterNode clusterNode) {
		List<String> services = servicesByNode.get(clusterNode);
		return new ArrayList<>(services == null ? Collections.emptyList() : services);
	}
}
