package org.teamapps.cluster.network;

import java.util.ArrayList;
import java.util.List;

public class ClusterNode {

	private String nodeId;
	private List<String> services = new ArrayList<>();

	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	public String getNodeId() {
		return nodeId;
	}

	public List<String> getServices() {
		return services;
	}

	public void setServices(List<String> services) {
		this.services = services;
	}
}
