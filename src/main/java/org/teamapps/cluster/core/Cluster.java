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
