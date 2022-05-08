package org.teamapps.cluster.state;

public class LeaderStateMachine extends AbstractDistributedStateMachine{



	public LeaderStateMachine(String name, DistributedStateMachineHandler handler) {
		super(name, handler);
	}

	@Override
	public void sendStateMachineUpdate(StateMachineUpdate update) {

		//todo persist to logindex (in memory or on disc)
		//todo sent to cluster

		handleStateMachineUpdate(update);
	}
}
