package org.teamapps.cluster.core;

import org.teamapps.cluster.protocol.NodeInfo;

import java.util.List;

public abstract class AbstractNode implements Node {

	private String nodeId;
	private HostAddress hostAddress;
	private boolean externallyReachable;
	private boolean leader;
	private List<String> services;

	public AbstractNode() {
	}

	public AbstractNode(HostAddress hostAddress) {
		this.hostAddress = hostAddress;
		this.externallyReachable = true;
	}

	public AbstractNode(String nodeId, HostAddress hostAddress, boolean externallyReachable, boolean leader) {
		this.nodeId = nodeId;
		this.hostAddress = hostAddress;
		this.externallyReachable = externallyReachable;
		this.leader = leader;
	}

	@Override
	public String getNodeId() {
		return nodeId;
	}

	@Override
	public boolean isExternallyReachable() {
		return externallyReachable;
	}

	@Override
	public HostAddress getHostAddress() {
		return hostAddress;
	}

	@Override
	public List<String> getServices() {
		return services;
	}

	@Override
	public boolean isLeader() {
		return leader;
	}

	@Override
	public NodeInfo createNodeInfo() {
		return new NodeInfo()
				.setNodeId(nodeId)
				.setLeader(leader)
				.setHost(hostAddress.getHost())
				.setPort(hostAddress.getPort())
				.setServices(services.toArray(new String[0]));
	}

	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	public void setHostAddress(HostAddress hostAddress) {
		this.hostAddress = hostAddress;
	}

	public void setExternallyReachable(boolean externallyReachable) {
		this.externallyReachable = externallyReachable;
	}

	public void setLeader(boolean leader) {
		this.leader = leader;
	}

	public void setServices(List<String> services) {
		this.services = services;
	}
}
