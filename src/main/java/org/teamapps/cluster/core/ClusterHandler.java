package org.teamapps.cluster.core;

import org.teamapps.cluster.protocol.ClusterInfo;
import org.teamapps.protocol.schema.MessageObject;

public interface ClusterHandler {

	ClusterInfo getClusterInfo();

	void handleNodeConnected(RemoteNode node, ClusterInfo clusterInfo);

	void handleNodeDisconnected(RemoteNode node);

	void handleClusterUpdate();

	void handleMessage(MessageObject message, RemoteNode node);

	void handleTopicMessage(String topic, MessageObject message, RemoteNode node);

	MessageObject handleClusterServiceMethod(String service, String serviceMethod, MessageObject requestData);


}
