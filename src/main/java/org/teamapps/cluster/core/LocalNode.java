package org.teamapps.cluster.core;

public interface LocalNode extends Node {

	@Override
	default boolean isLocalNode() {
		return true;
	}

}
