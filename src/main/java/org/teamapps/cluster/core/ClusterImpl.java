package org.teamapps.cluster.core;

import org.teamapps.cluster.protocol.ClusterInfo;
import org.teamapps.cluster.protocol.ClusterModel;
import org.teamapps.protocol.schema.MessageObject;
import org.teamapps.protocol.schema.ModelCollection;
import org.teamapps.protocol.schema.ModelRegistry;
import org.teamapps.protocol.schema.PojoObjectDecoder;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ClusterImpl implements Cluster {

	private final String clusterSecret;
	private final File tempDir;
	private final ModelRegistry modelRegistry;
	private final LocalNode localNode;
	private final HostAddress bindToAddress;
	private List<RemoteNode> remoteNodes = new ArrayList<>();
	private final Map<String, RemoteNode> remoteNodeById = new ConcurrentHashMap<>();
	private boolean active = true;

	public ClusterImpl(String clusterSecret, String nodeId, boolean leader, HostAddress... knownNodes) throws IOException {
		this(clusterSecret, nodeId, leader, null, null, knownNodes);
	}

	public ClusterImpl(String clusterSecret, String nodeId, boolean leader, HostAddress externalAddress, HostAddress... knownNodes) throws IOException {
		this(clusterSecret, nodeId, leader, externalAddress, externalAddress, knownNodes);
	}

	public ClusterImpl(String clusterSecret, String nodeId, boolean leader, HostAddress externalAddress, HostAddress bindToAddress, HostAddress... knownNodes) throws IOException {
		this(clusterSecret, Files.createTempDirectory("temp" ).toFile(), nodeId, leader, externalAddress, bindToAddress, knownNodes);
	}

	public ClusterImpl(String clusterSecret, File tempDir, String nodeId, boolean leader, HostAddress externalAddress, HostAddress bindToAddress, HostAddress... knownNodes) {
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
		if (!remoteNodeById.containsKey(node.getNodeId())) {
			remoteNodeById.put(node.getNodeId(), node);
			ArrayList<RemoteNode> nodesCopy = new ArrayList<>(remoteNodes);
			nodesCopy.add(node);
			remoteNodes = nodesCopy;
		}
		//todo sync cluster info

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
	public boolean isServiceAvailable(String serviceName) {
		return false;
	}

	@Override
	public void registerService(String serviceName, ClusterService service) {

	}

	@Override
	public void removeService(String serviceName) {

	}

	@Override
	public void sendMessage(MessageObject message, String nodeId) {
		RemoteNode remoteNode = getRemoteNode(nodeId);
		if (remoteNode != null) {
			remoteNode.sendMessage(message, false);
		}
	}

	@Override
	public void sendTopicMessage(String topic, MessageObject message) {

	}

	@Override
	public void handleMessage(MessageObject message, RemoteNode node) {

	}

	@Override
	public void handleTopicMessage(String topic, MessageObject message, RemoteNode node) {

	}

	@Override
	public <REQUEST extends MessageObject, RESPONSE extends MessageObject> RESPONSE executeClusterServiceMethod(String service, String serviceMethod, REQUEST request, PojoObjectDecoder<RESPONSE> responseDecoder) {
		return null;
	}

	@Override
	public MessageObject handleClusterServiceMethod(String service, String serviceMethod, MessageObject requestData) {
		return null;
	}


}
