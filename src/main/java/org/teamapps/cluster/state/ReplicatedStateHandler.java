package org.teamapps.cluster.state;

import org.teamapps.protocol.schema.MessageObject;

public interface ReplicatedStateHandler {

	void handleStateUpdated(String stateId, MessageObject state);

	void handleEntryAdded(String list, MessageObject message);

	void handleEntryRemoved(String list, MessageObject message);

	void handleEntryUpdated(String list, MessageObject currentState, MessageObject previousState);

	void handleAllEntriesRemoved(String list);

	void handleFireAndForget(String list, MessageObject message);

	void handleStateMachineRemoved();
}
