package org.teamapps.cluster.state;

public interface DistributedStateMachineRegistry {

	DistributedStateMachine getStateMachine(String name, boolean persisted);

	void removeStateMachine(String name);
}
