package org.teamapps.cluster.state;

public interface DistributedChangeLog {

	void handleStateMachineUpdate(StateMachineUpdate update);
}
