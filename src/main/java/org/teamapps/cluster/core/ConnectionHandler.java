package org.teamapps.cluster.core;

import org.teamapps.cluster.protocol.ClusterInfo;
import org.teamapps.protocol.schema.MessageObject;

public interface ConnectionHandler {

	ClusterInfo getClusterInfo();

	void handleConnectionEstablished(Connection connection, ClusterInfo clusterInfo);

	void handleClusterInfoUpdate(ClusterInfo clusterInfo);

	void handleConnectionClosed();

	void handleMessage(MessageObject message);
}
