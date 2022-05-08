package org.teamapps.cluster.state;

public class LocalStateMachine extends AbstractDistributedStateMachine {

	public LocalStateMachine(String name, DistributedStateMachineHandler handler) {
		super(name, handler);
	}

	@Override
	public void sendStateMachineUpdate(StateMachineUpdate update) {
		handleStateMachineUpdate(update);
	}
}
