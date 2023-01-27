package org.teamapps.cluster.core2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamapps.cluster.message.protocol.ClusterNodeData;
import org.teamapps.message.protocol.message.Message;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.TimeUnit;

public class ClusterNode {
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private final Cluster cluster;
	private final ClusterNodeData nodeData;
	private volatile ClusterConnection connection;
	private volatile int activeTasks;


	public ClusterNode(Cluster cluster, ClusterNodeData nodeData, ClusterConnection connection) {
		this.cluster = cluster;
		this.nodeData = nodeData;
		this.connection = connection;
		connection.setClusterNode(this);
		init();
		LOGGER.info("Cluster node [{}]: new peer node: {} [{}:{}]", cluster.getLocalNode().getNodeId(), nodeData.getNodeId(), nodeData.getHost(), nodeData.getPort());
	}

	private void init() {
		cluster.getScheduledExecutorService().scheduleWithFixedDelay(() -> {
			if (isConnected()) {
				/*
					send load status:
						available services (implicit with status)
						work queue
						cpu load
						...
				 */
			}
		}, 1, 1, TimeUnit.SECONDS);
		cluster.getScheduledExecutorService().scheduleWithFixedDelay(() -> {
			if (!isConnected()) {
				//try to reconnect...
			}
		}, 60, 60, TimeUnit.SECONDS);
	}

	public synchronized void writeMessage(Message message) {
		if (isConnected()) {
			connection.writeMessage(message);
		}
	}

	public void handleConnectionUpdate(ClusterConnection connection) {
		this.connection = connection;
		connection.setClusterNode(this);
		LOGGER.info("Cluster node [{}]: peer node reconnected: {} [{}:{}]", cluster.getLocalNode().getNodeId(), nodeData.getNodeId(), nodeData.getHost(), nodeData.getPort());
	}

	public synchronized void handleConnectionClosed() {
		this.connection = null;
		LOGGER.info("Cluster node [{}]: peer node disconnected: {} [{}:{}]", cluster.getLocalNode().getNodeId(), nodeData.getNodeId(), nodeData.getHost(), nodeData.getPort());
		cluster.handleDisconnect(this);
		if (!cluster.getScheduledExecutorService().isShutdown()) {
			cluster.getScheduledExecutorService().schedule(() -> {
				//try to reconnect
			}, 3, TimeUnit.SECONDS);
		}
	}

	public ClusterNodeData getNodeData() {
		return nodeData;
	}

	public ClusterConnection getConnection() {
		return connection;
	}

	public boolean isConnected() {
		return connection != null && connection.isConnected();
	}

	public void closeConnection() {
		if (isConnected()) {
			connection.close();
		}
	}

	public int getActiveTasks() {
		return activeTasks;
	}

	public void setActiveTasks(int activeTasks) {
		this.activeTasks = activeTasks;
	}

	public void addTask() {
		activeTasks++;
	}
}
