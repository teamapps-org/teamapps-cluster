package org.teamapps.cluster.network;

public interface ClusterNodeMessageHandler {

	void handleMessage(RemoteClusterNode clusterNode, byte[] message);

	byte[] createInitMessage();

}
