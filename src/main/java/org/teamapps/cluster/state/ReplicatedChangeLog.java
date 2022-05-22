package org.teamapps.cluster.state;

public interface ReplicatedChangeLog {

	void handleStateMachineUpdate(StateUpdate update);
}
