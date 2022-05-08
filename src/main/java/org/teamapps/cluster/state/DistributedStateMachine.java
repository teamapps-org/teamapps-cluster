package org.teamapps.cluster.state;

import org.teamapps.protocol.schema.MessageObject;

import java.util.List;

public interface DistributedStateMachine extends DistributedChangeLog {

	String getName();

	StateMachineUpdateMessage prepareAddEntry(String list, String identifier, MessageObject message);
	StateMachineUpdateMessage prepareRemoveEntry(String list, String identifier);
	StateMachineUpdateMessage prepareUpdateEntry(String list, String identifier, MessageObject message);
	StateMachineUpdateMessage prepareRemoveAllEntries(String list);
	StateMachineUpdateMessage prepareSetState(String stateId, MessageObject message);
	StateMachineUpdateMessage prepareSendAndForget(String messageType, MessageObject message);

	void executeStateMachineUpdate(StateMachineUpdateMessage... updates);
	void executeStateMachineUpdate(StateMachineUpdate update);

	void addEntry(String list, String identifier, MessageObject message);
	void removeEntry(String list, String identifier);
	void updateEntry(String list, String identifier, MessageObject message);
	void removeAllEntries(String list);
	void setState(String stateId, MessageObject message);
	void sendAndForget(String messageType, MessageObject message);

	MessageObject getEntry(String list, String identifier);
	List<MessageObject> getEntries(String list);
	List<String> getLists();
	MessageObject getState(String stateId);



}
