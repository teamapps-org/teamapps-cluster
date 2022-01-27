package org.teamapps.cluster.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamapps.cluster.model2.ClusterNodeData;

import java.lang.invoke.MethodHandles;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RemoteClusterNode extends ClusterNode implements ConnectionHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private static ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(3);

	private final ClusterNodeMessageHandler clusterNodeMessageHandler;
	private final boolean outgoing;
	private final NodeAddress nodeAddress;
	private Connection connection;
	private volatile boolean connected;
	private int retries;
	private List<String> availableServices = new ArrayList<>();

	public RemoteClusterNode(ClusterNodeMessageHandler clusterNodeMessageHandler, Socket socket) {
		this.clusterNodeMessageHandler = clusterNodeMessageHandler;
		this.outgoing = false;
		this.nodeAddress = new NodeAddress(socket.getInetAddress().getHostAddress(), socket.getPort());
		this.connection = new Connection(this, socket, nodeAddress);
	}

	public RemoteClusterNode(ClusterNodeMessageHandler clusterNodeMessageHandler, NodeAddress nodeAddress) {
		this.clusterNodeMessageHandler = clusterNodeMessageHandler;
		this.outgoing = true;
		this.nodeAddress = nodeAddress;
		createOutgoingConnection();
	}

	private void createOutgoingConnection() {
		this.connection = new Connection(this, nodeAddress);
		this.connection.writeMessage(clusterNodeMessageHandler.createInitMessage());
	}

	public void sendMessage(byte[] bytes) {
		if (bytes != null && connection != null) {
			connection.writeMessage(bytes);
		}
	}

	@Override
	public void handleMessage(byte[] bytes) {
		clusterNodeMessageHandler.handleMessage(this, bytes);
	}

	@Override
	public void handleConnectionClosed() {
		LOGGER.info("Remote connection closed: {}, {}",outgoing, nodeAddress);
		connected = false;
		connection = null;
		retries++;
		if (outgoing) {
			scheduledExecutorService.schedule(this::createOutgoingConnection, retries < 10 ? 3 : 15, TimeUnit.SECONDS);
		}
	}

	public boolean isOutgoing() {
		return outgoing;
	}

	public boolean isConnected() {
		return connected;
	}

	public void setConnected(boolean connected) {
		this.connected = connected;
		retries = 0;
	}

	public NodeAddress getNodeAddress() {
		return nodeAddress;
	}

	public List<String> getAvailableServices() {
		return availableServices;
	}

	public void setClusterNodeData(ClusterNodeData nodeData) {
		if (getNodeId() == null) {
			setNodeId(nodeData.getNodeId());
		}
		if (nodeData.getAvailableServices() != null) {
			availableServices = Arrays.asList(nodeData.getAvailableServices());
		} else {
			availableServices = Collections.emptyList();
		}
	}

	public ClusterNodeData getClusterNodeData() {
		return new ClusterNodeData()
				.setNodeId(getNodeId())
				.setHost(getNodeAddress().getHost())
				.setPort(getNodeAddress().getPort())
				.setAvailableServices(getAvailableServices().toArray(new String[0]));
	}

	@Override
	public String toString() {
		return "RemoteClusterNode{ " + getNodeId() +
				", nodeAddress=" + nodeAddress +
				", outgoing=" + outgoing +
				", connected=" + connected +
				", retries=" + retries +
				", availableServices=" + availableServices +
				'}';
	}
}
