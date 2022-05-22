package org.teamapps.cluster.state;

public interface ReplicatedStateRegistry {

	String getLocalNodeId();

	ReplicatedState getStateMachine(String name, boolean persisted);

	void removeStateMachine(String name);
}
