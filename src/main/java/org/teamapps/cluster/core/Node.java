package org.teamapps.cluster.core;

import org.teamapps.cluster.protocol.NodeInfo;
import org.teamapps.protocol.schema.MessageObject;

import java.util.List;

public interface Node {

	boolean isLocalNode();

	boolean isExternallyReachable();

	HostAddress getHostAddress();

	String getNodeId();

	List<String> getServices();

	boolean isLeader();

	NodeInfo createNodeInfo();


}
