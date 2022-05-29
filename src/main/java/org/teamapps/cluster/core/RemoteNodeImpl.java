package org.teamapps.cluster.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamapps.cluster.protocol.ClusterInfo;
import org.teamapps.cluster.protocol.NodeInfo;
import org.teamapps.protocol.schema.MessageObject;
import org.teamapps.protocol.schema.ModelRegistry;
import org.teamapps.protocol.schema.PojoObjectDecoder;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RemoteNodeImpl extends AbstractNode implements RemoteNode {
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private static final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(2);

	private final ClusterHandler clusterHandler;
	private final ModelRegistry modelRegistry;
	private File tempDir;
	private String clusterSecret;
	private final boolean outboundConnection;
	private final MessageQueue messageQueue = new MessageQueue();
	private Connection connection;
	private int retries;
	private boolean running = true;
	private ClusterInfo lastClusterInfo;

	public RemoteNodeImpl(HostAddress hostAddress, ClusterHandler clusterHandler, ModelRegistry modelRegistry, File tempDir, String clusterSecret) {
		super(hostAddress);
		this.clusterHandler = clusterHandler;
		this.modelRegistry = modelRegistry;
		this.tempDir = tempDir;
		this.clusterSecret = clusterSecret;
		this.outboundConnection = true;
		connect();
	}

	public RemoteNodeImpl(Socket socket, ClusterHandler clusterHandler, ModelRegistry modelRegistry, File tempDir, String clusterSecret) {
		this.clusterHandler = clusterHandler;
		this.modelRegistry = modelRegistry;
		this.outboundConnection = false;
		new NetworkConnection(socket, this, modelRegistry, tempDir, clusterSecret);
	}

	private void connect() {
		new NetworkConnection(getHostAddress(), messageQueue, this, modelRegistry, tempDir, clusterSecret);
	}

	private void startKeepAliveService() {
		scheduledExecutorService.scheduleAtFixedRate(() -> {
			if (isConnected() && System.currentTimeMillis() - connection.lastMessageTimestamp() > 60_000) {
				connection.sendKeepAlive();
			}
		}, 90, 90, TimeUnit.SECONDS);
	}

	@Override
	public ClusterInfo getClusterInfo() {
		return clusterHandler.getClusterInfo();
	}

	@Override
	public void handleConnectionEstablished(Connection connection, ClusterInfo clusterInfo) {
		LOGGER.info("Remote connection established: {}, {}", getNodeId(), getHostAddress());
		this.retries = 0;
		this.connection = connection;
		handleClusterInfoUpdate(clusterInfo);
		clusterHandler.handleNodeConnected(this, clusterInfo);
		startKeepAliveService();
	}

	@Override
	public void handleClusterInfoUpdate(ClusterInfo clusterInfo) {
		NodeInfo localNode = clusterInfo.getLocalNode();
		setNodeId(localNode.getNodeId());
		setHostAddress(new HostAddress(localNode.getHost(), localNode.getPort()));
		setLeader(localNode.isLeader());
		setServices(localNode.getServices() != null ? Arrays.asList(localNode.getServices()) : Collections.emptyList());
		lastClusterInfo = clusterInfo;
	}

	@Override
	public void handleConnectionClosed() {
		LOGGER.info("Remote connection closed: {}, {}", getNodeId(), getHostAddress());
		connection = null;
		retries++;
		if (outboundConnection && running) {
			scheduledExecutorService.schedule(this::connect, retries < 10 ? 3 : 15, TimeUnit.SECONDS);
		}
	}

	@Override
	public void handleMessage(MessageObject message) {

	}

	@Override
	public boolean isConnected() {
		return connection != null && connection.isConnected();
	}

	@Override
	public boolean isOutbound() {
		return outboundConnection;
	}

	public void shutDown() {
		try {
			if (connection != null) {
				connection.close();
			}
			running = false;
			scheduledExecutorService.shutdownNow();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void sendMessage(MessageObject message, boolean resentOnError) {
		if (isConnected()) {
			if (!messageQueue.addMessage(message, resentOnError)) {
				if (connection != null) {
					connection.close();
				}
			}
		}
	}

	@Override
	public <REQUEST extends MessageObject, RESPONSE extends MessageObject> RESPONSE executeServiceMethod(String service, String serviceMethod, REQUEST request, PojoObjectDecoder<RESPONSE> responseDecoder) {
		return null;
	}
}
