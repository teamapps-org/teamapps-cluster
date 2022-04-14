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
package org.teamapps.cluster.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamapps.cluster.model.cluster.ClusterNodeData;

import java.lang.invoke.MethodHandles;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RemoteClusterNode extends ClusterNode implements ConnectionHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private static final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(3);

	private final ClusterNodeMessageHandler clusterNodeMessageHandler;
	private final boolean outgoing;
	private final NodeAddress nodeAddress;
	private Connection connection;
	private volatile boolean connected;
	private int retries;
	private long lastMessageTimestamp;
	private byte[] keepAliveMessage;
	private boolean running = true;

	private ArrayBlockingQueue<byte[]> sendMessageQueue = new ArrayBlockingQueue<>(100_000);

	public RemoteClusterNode(ClusterNodeMessageHandler clusterNodeMessageHandler, Socket socket) {
		this.clusterNodeMessageHandler = clusterNodeMessageHandler;
		this.outgoing = false;
		this.nodeAddress = new NodeAddress(socket.getInetAddress().getHostAddress(), socket.getPort());
		this.connection = new Connection(this, socket, nodeAddress);
		init();
	}

	public RemoteClusterNode(ClusterNodeMessageHandler clusterNodeMessageHandler, NodeAddress nodeAddress) {
		this.clusterNodeMessageHandler = clusterNodeMessageHandler;
		this.outgoing = true;
		this.nodeAddress = nodeAddress;
		createOutgoingConnection();
		init();
	}

	public void merge(RemoteClusterNode clusterNode) {
		if (!connected) {
			if (this.connection != null) {
				LOGGER.error("Error connection still exists!");
			}
			clusterNode.getConnection().setConnectionHandler(this);
			this.connection = clusterNode.getConnection();
		} else {
			LOGGER.error("Cannot merge cluster node, existing node is still connected:" + nodeAddress);
		}
	}

	private void init() {
		keepAliveMessage = clusterNodeMessageHandler.getKeepAliveMessage();
		scheduledExecutorService.scheduleAtFixedRate(() -> {
			if (connected && System.currentTimeMillis() - lastMessageTimestamp > 60_000) {
				sendKeepAlive();
			}
		}, 90, 90, TimeUnit.SECONDS);
		startSendQueueThread();
	}

	private void startSendQueueThread() {
		Thread thread = new Thread(() -> {
			byte[] unsentMessageBytes = null;
			while (running) {
				try {
					if (connected) {
						byte[] bytes = unsentMessageBytes != null ? unsentMessageBytes : sendMessageQueue.take();
						boolean success = connection.writeMessage(bytes);
						LOGGER.debug("Write async message to " + getNodeId() + ", len:" + bytes + ", success:" + success);
						if (success) {
							unsentMessageBytes = null;
							lastMessageTimestamp = System.currentTimeMillis();
						} else {
							unsentMessageBytes = bytes;
							Thread.sleep(1_000);
							LOGGER.error("Could not write message async");
						}
					} else {
						Thread.sleep(500);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
		thread.setDaemon(true);
		thread.setName("connection-async-writer-" + nodeAddress.getHost());
		thread.start();
	}

	private void sendKeepAlive() {
		sendMessage(keepAliveMessage);
	}

	private void createOutgoingConnection() {
		this.connection = new Connection(this, nodeAddress);
		this.connection.writeMessage(clusterNodeMessageHandler.createInitMessage());
	}

	public void sendMessage(byte[] bytes) {
		if (bytes != null && connection != null) {  //todo connected instead of connection != null!
			lastMessageTimestamp = System.currentTimeMillis();
			connection.writeMessage(bytes);
		}
	}

	public boolean sendMessageAsync(byte[] bytes) {
		return bytes != null && sendMessageQueue.offer(bytes);
	}


	@Override
	public void handleMessage(byte[] bytes) {
		lastMessageTimestamp = System.currentTimeMillis();
		clusterNodeMessageHandler.handleMessage(this, bytes);
	}

	@Override
	public void handleConnectionClosed() {
		LOGGER.info("Remote connection closed: {}, {}", outgoing, nodeAddress);
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

	public Connection getConnection() {
		return connection;
	}

	public void setClusterNodeData(ClusterNodeData nodeData) {
		if (getNodeId() == null) {
			setNodeId(nodeData.getNodeId());
		}
		setServices(nodeData.getAvailableServices() != null ? Arrays.asList(nodeData.getAvailableServices()) : Collections.emptyList());
	}

	public ClusterNodeData getClusterNodeData() {
		return new ClusterNodeData()
				.setNodeId(getNodeId())
				.setHost(getNodeAddress().getHost())
				.setPort(getNodeAddress().getPort())
				.setAvailableServices(getServices().toArray(new String[0]));
	}

	public void shutDown() {
		try {
			if (connection != null) {
				connection.closeConnection();
			}
			running = false;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public String toString() {
		return "RemoteClusterNode{ " + getNodeId() +
				", nodeAddress=" + nodeAddress +
				", outgoing=" + outgoing +
				", connected=" + connected +
				", retries=" + retries +
				", availableServices=" + getServices() +
				'}';
	}
}
