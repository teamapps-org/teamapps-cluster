package org.teamapps.cluster.state;

import org.teamapps.protocol.schema.MessageObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractDistributedStateMachine implements DistributedStateMachine {

	private final String name;
	private final DistributedStateMachineHandler handler;
	private Map<String, List<MessageObject>> messageMap = new ConcurrentHashMap<>();
	private Map<String, Map<String, MessageObject>> messageById = new ConcurrentHashMap<>();
	private Map<String, MessageObject> stateById = new ConcurrentHashMap<>();

	public AbstractDistributedStateMachine(String name, DistributedStateMachineHandler handler) {
		this.name = name;
		this.handler = handler;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void handleStateMachineUpdate(StateMachineUpdate update) {
		for (StateMachineUpdateMessage stateMachineUpdateMessage : update.getUpdateMessages()) {
			handleUpdate(stateMachineUpdateMessage);
		}
	}

	private void handleUpdate(StateMachineUpdateMessage stateMachineUpdateMessage) {
		ChangeOperation operation = stateMachineUpdateMessage.getOperation();
		MessageObject message = stateMachineUpdateMessage.getMessage();
		String subStateId = stateMachineUpdateMessage.getSubStateId();
		String identifier = stateMachineUpdateMessage.getIdentifier();
		switch (operation) {
			case ADD -> {
				messageMap.computeIfAbsent(subStateId, s -> new ArrayList<>()).add(message);
				messageById.computeIfAbsent(subStateId, s -> new ConcurrentHashMap<>()).put(identifier, message);
				handler.handleEntryAdded(subStateId, message);
			}
			case UPDATE -> {
				MessageObject previousEntry = messageById.computeIfAbsent(subStateId, s -> new ConcurrentHashMap<>()).remove(identifier);
				if (previousEntry != null) {
					messageById.get(subStateId).put(identifier, message);
					messageMap.computeIfAbsent(subStateId, s -> new ArrayList<>()).remove(previousEntry);
					messageMap.computeIfAbsent(subStateId, s -> new ArrayList<>()).add(message);
					handler.handleEntryUpdated(subStateId, message, previousEntry);
				} else {
					messageById.get(subStateId).put(identifier, message);
					messageMap.computeIfAbsent(subStateId, s -> new ArrayList<>()).add(message);
				}
			}
			case REMOVE -> {
				MessageObject messageObject = messageById.computeIfAbsent(subStateId, s -> new ConcurrentHashMap<>()).remove(identifier);
				if (messageObject != null) {
					messageMap.get(subStateId).remove(messageObject);
					handler.handleEntryRemoved(subStateId, messageObject);
				}
			}
			case SET -> {
				stateById.put(subStateId, message);
				handler.handleStateUpdated(subStateId, message);
			}
			case REMOVE_ALL -> {
				messageById.put(subStateId, new ConcurrentHashMap<>());
				messageMap.put(subStateId, new ArrayList<>());
				handler.handleAllEntriesRemoved(subStateId);
			}
		}
	}

	private void sendStateMachineUpdate(StateMachineUpdateMessage stateMachineUpdateMessage) {
		sendStateMachineUpdate(new StateMachineUpdate(name, stateMachineUpdateMessage));
	}

	public abstract void sendStateMachineUpdate(StateMachineUpdate update);

	@Override
	public MessageObject getEntry(String list, String identifier) {
		return messageById.computeIfAbsent(list, s -> new ConcurrentHashMap<>()).get(identifier);
	}

	@Override
	public List<MessageObject> getEntries(String list) {
		return messageMap.get(list);
	}

	@Override
	public List<String> getLists() {
		return new ArrayList<>(messageMap.keySet());
	}

	@Override
	public MessageObject getState(String stateId) {
		return stateById.get(stateId);
	}

	@Override
	public StateMachineUpdateMessage prepareAddEntry(String list, String identifier, MessageObject message) {
		return new StateMachineUpdateMessage(list, ChangeOperation.ADD, identifier, message);
	}

	@Override
	public StateMachineUpdateMessage prepareRemoveEntry(String list, String identifier) {
		return new StateMachineUpdateMessage(list, ChangeOperation.REMOVE, identifier, null);
	}


	@Override
	public StateMachineUpdateMessage prepareUpdateEntry(String list, String identifier, MessageObject message) {
		return new StateMachineUpdateMessage(list, ChangeOperation.UPDATE, identifier, message);
	}

	@Override
	public StateMachineUpdateMessage prepareRemoveAllEntries(String list) {
		return new StateMachineUpdateMessage(list, ChangeOperation.REMOVE_ALL, null, null);
	}

	@Override
	public StateMachineUpdateMessage prepareSetState(String stateId, MessageObject message) {
		return new StateMachineUpdateMessage(stateId, ChangeOperation.SET, null, message);
	}

	@Override
	public StateMachineUpdateMessage prepareSendAndForget(String messageType, MessageObject message) {
		return new StateMachineUpdateMessage(messageType, ChangeOperation.SEND_AND_FORGET, null, message);
	}

	@Override
	public void executeStateMachineUpdate(StateMachineUpdateMessage... updates) {
		sendStateMachineUpdate(new StateMachineUpdate(name, Arrays.asList(updates)));
	}

	@Override
	public void executeStateMachineUpdate(StateMachineUpdate update) {
		sendStateMachineUpdate(update);
	}

	@Override
	public void addEntry(String list, String identifier, MessageObject message) {
		executeStateMachineUpdate(prepareAddEntry(list, identifier, message));
	}

	@Override
	public void removeEntry(String list, String identifier) {
		executeStateMachineUpdate(prepareRemoveEntry(list, identifier));
	}

	@Override
	public void updateEntry(String list, String identifier, MessageObject message) {
		executeStateMachineUpdate(prepareUpdateEntry(list, identifier, message));
	}

	@Override
	public void removeAllEntries(String list) {
		executeStateMachineUpdate(prepareRemoveAllEntries(list));
	}

	@Override
	public void setState(String stateId, MessageObject message) {
		executeStateMachineUpdate(prepareSetState(stateId, message));
	}

	@Override
	public void sendAndForget(String messageType, MessageObject message) {
		executeStateMachineUpdate(prepareSendAndForget(messageType, message));
	}
}
