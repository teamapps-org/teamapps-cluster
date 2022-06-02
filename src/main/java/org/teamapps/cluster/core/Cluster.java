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

import org.teamapps.protocol.schema.ClusterServiceRegistry;
import org.teamapps.protocol.schema.MessageObject;
import org.teamapps.protocol.schema.ModelCollection;
import org.teamapps.protocol.schema.PojoObjectDecoder;

import java.io.IOException;
import java.util.List;

public interface Cluster extends ClusterServiceRegistry {

	static Cluster createCluster(String clusterSecret, String nodeId, HostAddress... knownNodes) {
		try {
			return new ClusterImpl(clusterSecret, nodeId, false, knownNodes);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	static Cluster createCluster(String clusterSecret, String nodeId, HostAddress externalAddress, boolean leader, HostAddress... knownNodes) {
		try {
			return new ClusterImpl(clusterSecret, nodeId, externalAddress, externalAddress, leader, knownNodes);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	static Cluster createCluster(String clusterSecret, String nodeId, HostAddress externalAddress, HostAddress bindToAddress, boolean leader, HostAddress... knownNodes) {
		try {
			return new ClusterImpl(clusterSecret, nodeId, externalAddress, bindToAddress, leader, knownNodes);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	void addModelCollection(ModelCollection modelCollection);

	LocalNode getLocalNode();

	List<RemoteNode> getRemoteNodes();

	void addRemoteNode(RemoteNode remoteNode);

	RemoteNode getRemoteNode(String nodeId);

	boolean isServiceAvailable(String serviceName);

	<MESSAGE extends MessageObject> void sendMessage(MESSAGE message, String nodeId);

	<MESSAGE extends MessageObject> void sendTopicMessage(String topic, MESSAGE message);

	<MESSAGE extends MessageObject> void registerMessageHandler(MessageHandler<MESSAGE> messageHandler, PojoObjectDecoder<MESSAGE> messageDecoder);

	<MESSAGE extends MessageObject> void registerTopicHandler(String topic, MessageHandler<MESSAGE> messageHandler, PojoObjectDecoder<MESSAGE> messageDecoder);

	void shutDown();

}
