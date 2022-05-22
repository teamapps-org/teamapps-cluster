package org.teamapps.cluster.state;

public class LocalState extends AbstractReplicatedState {

	public LocalState(String name, ReplicatedStateHandler handler) {
		super(name, handler);
	}

	@Override
	public void sendStateMachineUpdate(StateUpdate update) {
		handleStateMachineUpdate(update);
	}
}
