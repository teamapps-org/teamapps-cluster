package org.teamapps.cluster.network;

import java.util.UUID;

public class LocalClusterNode extends ClusterNode{
	private final int port;


	public LocalClusterNode(int port) {
		this.port = port;
		setNodeId(UUID.randomUUID().toString());
	}

	public int getPort() {
		return port;
	}
}
