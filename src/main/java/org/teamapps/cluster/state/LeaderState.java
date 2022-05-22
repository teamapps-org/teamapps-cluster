package org.teamapps.cluster.state;

public class LeaderState extends AbstractReplicatedState {


	public LeaderState(String name, ReplicatedStateHandler handler) {
		super(name, handler);
	}

	@Override
	public void sendStateMachineUpdate(StateUpdate update) {

		//todo persist to logindex (in memory or on disc)
		//todo sent to cluster

		handleStateMachineUpdate(update);
	}
}
