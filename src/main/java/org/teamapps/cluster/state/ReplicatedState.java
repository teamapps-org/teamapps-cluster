package org.teamapps.cluster.state;

import org.teamapps.protocol.schema.MessageObject;

import java.util.List;

public interface ReplicatedState extends ReplicatedChangeLog {

	String getName();

	StateUpdateMessage prepareAddEntry(String list, String identifier, MessageObject message);
	StateUpdateMessage prepareRemoveEntry(String list, String identifier);
	StateUpdateMessage prepareUpdateEntry(String list, String identifier, MessageObject message);
	StateUpdateMessage prepareRemoveAllEntries(String list);
	StateUpdateMessage prepareSetState(String stateId, MessageObject message);
	StateUpdateMessage prepareFireAndForget(String messageType, MessageObject message);

	void executeStateMachineUpdate(StateUpdateMessage... updates);
	void executeStateMachineUpdate(StateUpdate update);

	void addEntry(String list, String identifier, MessageObject message);
	void removeEntry(String list, String identifier);
	void updateEntry(String list, String identifier, MessageObject message);
	void removeAllEntries(String list);
	void setProperty(String propertyId, MessageObject message);
	void fireAndForget(String messageType, MessageObject message);

	MessageObject getEntry(String list, String identifier);
	List<MessageObject> getEntries(String list);
	int getEntryCount(String list);
	List<String> getLists();
	MessageObject getProperty(String stateId);



}
