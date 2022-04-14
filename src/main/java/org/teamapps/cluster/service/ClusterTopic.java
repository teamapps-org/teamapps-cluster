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
package org.teamapps.cluster.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamapps.cluster.crypto.AesCipher;
import org.teamapps.cluster.model.cluster.ClusterTopicInfo;
import org.teamapps.cluster.model.cluster.ClusterTopicMessage;
import org.teamapps.cluster.network.RemoteClusterNode;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class ClusterTopic {
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private final String topicName;
	private final AesCipher aesCipher;
	private final Set<String> registeredMemberNodeIds = new HashSet<>();
	private final List<RemoteClusterNode> members = new ArrayList<>();
	private final Set<String> availableMembers = new HashSet<>();
	private Consumer<ClusterTopicMessage> messageConsumer;

	public ClusterTopic(String topicName, AesCipher aesCipher) {
		this.topicName = topicName;
		this.aesCipher = aesCipher;
	}

	public ClusterTopic(String topicName, AesCipher aesCipher, String localNodeId) {
		this.topicName = topicName;
		this.aesCipher = aesCipher;
		registeredMemberNodeIds.add(localNodeId);
	}

	public synchronized boolean isRegistered(String nodeId) {
		return registeredMemberNodeIds.contains(nodeId);
	}

	public synchronized boolean isAvailableMember(String nodeId) {
		return availableMembers.contains(nodeId);
	}

	public synchronized void addRegisteredMember(String nodeId) {
		registeredMemberNodeIds.add(nodeId);
	}

	public synchronized void sendMessageAsync(byte[] data) throws Exception {
		LOGGER.debug("Add topic message: {}, length: {}", topicName, +data.length);
		ClusterTopicMessage message = new ClusterTopicMessage().setTopic(topicName).setData(data);
		byte[] messageBytes = aesCipher.encrypt(message.toBytes());
		for (RemoteClusterNode member : members) {
			member.sendMessageAsync(messageBytes);
		}
	}

	public synchronized void addMember(RemoteClusterNode member) {
		if (!availableMembers.contains(member.getNodeId())) {
			members.add(member);
			availableMembers.add(member.getNodeId());
		}
	}

	public synchronized void removeMember(RemoteClusterNode member) {
		members.remove(member);
		availableMembers.remove(member.getNodeId());
		registeredMemberNodeIds.remove(member.getNodeId());
	}

	public synchronized ClusterTopicInfo createTopicInfo() {
		ClusterTopicInfo topicInfo = new ClusterTopicInfo();
		topicInfo.setTopicName(topicName);
		topicInfo.setNodeIds(new ArrayList<>(registeredMemberNodeIds));
		return topicInfo;
	}

	public void setMessageConsumer(Consumer<ClusterTopicMessage> messageConsumer) {
		this.messageConsumer = messageConsumer;
	}

	public void handleMessage(ClusterTopicMessage message) {
		LOGGER.debug("Receive message on topic: {}, size: {}, consumer: {}", topicName, message.getData().length, (messageConsumer != null));
		if (messageConsumer != null) {
			messageConsumer.accept(message);
		}
	}
}
