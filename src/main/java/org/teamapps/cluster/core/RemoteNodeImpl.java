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
import org.teamapps.cluster.protocol.NodeInfo;
import org.teamapps.protocol.schema.MessageObject;
import org.teamapps.protocol.schema.ModelRegistry;
import org.teamapps.protocol.schema.PojoObjectDecoder;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

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
	private AtomicLong serviceRequestIdGenerator = new AtomicLong();
	private Map<Long, CompletableFuture<MessageObject>> serviceRequestMap = new ConcurrentHashMap<>();
	private long sentBytes;
	private long receivedBytes;
	private long sentMessages;
	private long receivedMessages;
	private long establishedConnectionsCount;
	private long lastConnectedTimestamp;


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
		new NetworkConnection(socket, messageQueue, this, modelRegistry, tempDir, clusterSecret);
	}

	private void connect() {
		new NetworkConnection(getHostAddress(), messageQueue, this, modelRegistry, tempDir, clusterSecret);
	}

	private void startKeepAliveService() {
		scheduledExecutorService.scheduleAtFixedRate(() -> {
			if (isConnected() && System.currentTimeMillis() - connection.getLastMessageTimestamp() > 60_000) {
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
		updateClusterInfo(clusterInfo);
		lastConnectedTimestamp = System.currentTimeMillis();
		if (establishedConnectionsCount == 0) {
			startKeepAliveService();
		}
		establishedConnectionsCount++;
		this.retries = 0;
		this.connection = connection;
		clusterHandler.handleNodeConnected(this, clusterInfo);
		LOGGER.info("Remote connection established: {}, {}", getNodeId(), getHostAddress());
	}

	@Override
	public void handleClusterInfoUpdate(ClusterInfo clusterInfo) {
		updateClusterInfo(clusterInfo);
		clusterHandler.handleClusterUpdate();
	}

	private void updateClusterInfo(ClusterInfo clusterInfo) {
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
		if (connection != null) {
			receivedBytes += connection.getReceivedBytes();
			receivedMessages += connection.getReceivedMessages();
			sentBytes += connection.getSentBytes();
			sentMessages += connection.getSentMessages();
		}
		connection = null;
		retries++;
		clusterHandler.handleNodeDisconnected(this);
		if (outboundConnection && running) {
			scheduledExecutorService.schedule(this::connect, retries < 10 ? 3 : 15, TimeUnit.SECONDS);
		}
	}

	@Override
	public void handleMessage(MessageObject message) {
		clusterHandler.handleMessage(message, this);
	}

	@Override
	public void handleClusterExecutionRequest(String serviceName, String serviceMethod, MessageObject message, long requestId) {
		MessageObject result = clusterHandler.handleClusterServiceMethod(serviceName, serviceMethod, message);
		//todo handle exceptions, null messages...
		addMessageToSendQueue(new MessageQueueEntry(true, true, result, serviceName, serviceMethod, true, requestId));
	}

	@Override
	public void handleClusterExecutionResult(MessageObject message, long requestId) {
		CompletableFuture<MessageObject> completableFuture = serviceRequestMap.remove(requestId);
		if (completableFuture != null) {
			completableFuture.complete(message);
		}
	}

	@Override
	public void recycleNode(RemoteNode node) {
		this.messageQueue.reuseQueue(node.getMessageQueue());
	}

	@Override
	public boolean isConnected() {
		return connection != null && connection.isConnected();
	}

	@Override
	public boolean isOutbound() {
		return outboundConnection;
	}

	@Override
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
	public MessageQueue getMessageQueue() {
		return messageQueue;
	}

	@Override
	public long getSentBytes() {
		return sentBytes + (connection != null ? connection.getSentBytes() : 0);
	}

	@Override
	public long getReceivedBytes() {
		return receivedBytes + (connection != null ? connection.getReceivedBytes() : 0);
	}

	@Override
	public long getSentMessages() {
		return sentMessages + (connection != null ? connection.getSentMessages() : 0);
	}

	@Override
	public long getReceivedMessages() {
		return receivedMessages + (connection != null ? connection.getReceivedMessages() : 0);
	}

	@Override
	public long getReconnects() {
		return establishedConnectionsCount - 1;
	}

	@Override
	public long getConnectedSince() {
		return lastConnectedTimestamp;
	}

	@Override
	public void sendMessage(MessageObject message, boolean resendOnError) {
		if (isConnected() && message != null) {
			addMessageToSendQueue(new MessageQueueEntry(resendOnError, message));
		} else {
			LOGGER.warn("Cannot send message, connected: {}, message: {}", isConnected(), message != null ? message.getName() : "is NULL!");
		}
	}

	@Override
	public <REQUEST extends MessageObject, RESPONSE extends MessageObject> RESPONSE executeServiceMethod(String service, String serviceMethod, REQUEST request, PojoObjectDecoder<RESPONSE> responseDecoder) {
		if (isConnected() && request != null && responseDecoder != null) {
			long requestId = serviceRequestIdGenerator.incrementAndGet();
			CompletableFuture<MessageObject> completableFuture = new CompletableFuture<>();
			serviceRequestMap.put(requestId, completableFuture);
			addMessageToSendQueue(new MessageQueueEntry(true, true, request, service, serviceMethod, false, requestId));
			try {
				MessageObject response = completableFuture.get();
				return responseDecoder.remap(response);
			} catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException(e);
			}
		}
		return null;
	}

	private void addMessageToSendQueue(MessageQueueEntry entry) {
		if (!messageQueue.addMessage(entry)) {
			LOGGER.warn("Error message queue is full, dropping connection: {}, {}", getNodeId(), getHostAddress());
			if (connection != null) {
				connection.close();
			}
		}
	}

	@Override
	public String toString() {
		return getNodeId() + ", " + getHostAddress();
	}
}
