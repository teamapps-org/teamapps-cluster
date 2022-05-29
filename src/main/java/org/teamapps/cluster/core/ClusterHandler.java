package org.teamapps.cluster.core;

import org.teamapps.cluster.protocol.ClusterInfo;

public interface ClusterHandler {

	ClusterInfo getClusterInfo();

	void handleNodeConnected(RemoteNode node, ClusterInfo clusterInfo);


}
