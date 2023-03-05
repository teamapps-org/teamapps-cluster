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

	private long sentBytes;
	private long receivedBytes;
	private long sentMessages;
	private long receivedMessages;

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
				cluster.connectNode(nodeData);
			} else {
				cluster.sendLoadInfoMessage();
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
		if (this.connection != null) {
			this.receivedBytes += connection.getReceivedBytes();
			this.sentBytes += connection.getSentBytes();
			this.receivedMessages += connection.getReceivedMessages();
			this.sentMessages += connection.getSentMessages();
		}
		this.connection = null;
		LOGGER.info("Cluster node [{}]: peer node disconnected: {} [{}:{}]", cluster.getLocalNode().getNodeId(), nodeData.getNodeId(), nodeData.getHost(), nodeData.getPort());
		cluster.handleDisconnect(this);
		Runnable reconnect = () -> {
			if (!cluster.isConnected(nodeData)) {
				cluster.connectNode(nodeData);
			}
		};
		if (nodeData.getPort() > 0 && nodeData.getHost() != null && !cluster.getScheduledExecutorService().isShutdown()) {
			cluster.getScheduledExecutorService().schedule(reconnect, 100, TimeUnit.MILLISECONDS);
			cluster.getScheduledExecutorService().schedule(reconnect, 1, TimeUnit.SECONDS);
			cluster.getScheduledExecutorService().schedule(reconnect, 3, TimeUnit.SECONDS);
			cluster.getScheduledExecutorService().schedule(reconnect, 15, TimeUnit.SECONDS);
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

	public long getSentBytes() {
		return connection != null ? connection.getSentBytes() + sentBytes : sentBytes;
	}

	public long getReceivedBytes() {
		return connection != null ? connection.getReceivedBytes() + receivedBytes : receivedBytes;
	}

	public long getSentMessages() {
		return connection != null ? connection.getSentMessages() + sentMessages : sentMessages;
	}

	public long getReceivedMessages() {
		return connection != null ? connection.getReceivedMessages() + receivedMessages : receivedMessages;
	}
}
