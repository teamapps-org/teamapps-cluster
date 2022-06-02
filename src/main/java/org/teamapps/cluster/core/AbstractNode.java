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

import org.teamapps.cluster.protocol.NodeInfo;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractNode implements Node {

	private String nodeId;
	private HostAddress hostAddress;
	private boolean externallyReachable;
	private boolean leader;
	private List<String> services = new ArrayList<>();

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
				.setHost(hostAddress != null ? hostAddress.getHost() : null)
				.setPort(hostAddress != null ? hostAddress.getPort() : 0)
				.setServices(services != null ? services.toArray(new String[0]) : null);
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
