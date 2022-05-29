package org.teamapps.cluster.core;

public class LocalNodeImpl extends AbstractNode implements LocalNode {

	public LocalNodeImpl() {
	}

	public LocalNodeImpl(String nodeId, HostAddress hostAddress, boolean externallyReachable, boolean leader) {
		super(nodeId, hostAddress, externallyReachable, leader);
	}
}
