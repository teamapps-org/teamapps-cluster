package org.teamapps.cluster.state;

import org.teamapps.protocol.schema.MessageObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractReplicatedState implements ReplicatedState {

	private final String name;
	private final ReplicatedStateHandler handler;
	private Map<String, List<MessageObject>> messageMap = new ConcurrentHashMap<>();
	private Map<String, Map<String, MessageObject>> messageById = new ConcurrentHashMap<>();
	private Map<String, MessageObject> propertyById = new ConcurrentHashMap<>();

	public AbstractReplicatedState(String name, ReplicatedStateHandler handler) {
		this.name = name;
		this.handler = handler;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void handleStateMachineUpdate(StateUpdate update) {
		List<ReplicatedStateTransactionRule> transactionConditions = update.getTransactionConditions();
		if (transactionConditions != null) {
			for (ReplicatedStateTransactionRule condition : transactionConditions) {
				final String subStateId = condition.getSubStateId();
				switch (condition.getCompareRule()) {
					case CONTAINS -> {
						if (getEntry(subStateId, condition.getIdentifier()) == null) {
							return;
						}
					}
					case CONTAINS_NOT -> {
						if (getEntry(subStateId, condition.getIdentifier()) != null) {
							return;
						}
					}
					case LIST_SIZE_EQUALS -> {
						if (condition.getValue() != getEntryCount(subStateId)) {
							return;
						}
					}
					case LIST_SIZE_GREATER -> {
						if (condition.getValue() <= getEntryCount(subStateId)) {
							return;
						}
					}
					case LIST_SIZE_SMALLER -> {
						if (condition.getValue() >= getEntryCount(subStateId)) {
							return;
						}
					}
				}
			}
		}
		for (StateUpdateMessage stateUpdateMessage : update.getUpdateMessages()) {
			handleUpdate(stateUpdateMessage);
		}
	}

	private void handleUpdate(StateUpdateMessage stateUpdateMessage) {
		ChangeOperation operation = stateUpdateMessage.getOperation();
		MessageObject message = stateUpdateMessage.getMessage();
		String subStateId = stateUpdateMessage.getSubStateId();
		String identifier = stateUpdateMessage.getIdentifier();
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
				propertyById.put(subStateId, message);
				handler.handleStateUpdated(subStateId, message);
			}
			case REMOVE_ALL -> {
				messageById.put(subStateId, new ConcurrentHashMap<>());
				messageMap.put(subStateId, new ArrayList<>());
				handler.handleAllEntriesRemoved(subStateId);
			}
			case SEND_AND_FORGET -> {
				handler.handleFireAndForget(subStateId, message);
			}
		}
	}

	private void sendStateMachineUpdate(StateUpdateMessage stateUpdateMessage) {
		sendStateMachineUpdate(new StateUpdate(name, stateUpdateMessage));
	}

	public abstract void sendStateMachineUpdate(StateUpdate update);

	@Override
	public MessageObject getEntry(String list, String identifier) {
		return messageById.computeIfAbsent(list, s -> new ConcurrentHashMap<>()).get(identifier);
	}

	@Override
	public List<MessageObject> getEntries(String list) {
		return messageMap.get(list);
	}

	@Override
	public int getEntryCount(String list) {
		List<MessageObject> entries = getEntries(list);
		return entries != null ? entries.size() : 0;
	}

	@Override
	public List<String> getLists() {
		return new ArrayList<>(messageMap.keySet());
	}

	@Override
	public MessageObject getProperty(String stateId) {
		return propertyById.get(stateId);
	}

	@Override
	public StateUpdateMessage prepareAddEntry(String list, String identifier, MessageObject message) {
		return new StateUpdateMessage(list, ChangeOperation.ADD, identifier, message);
	}

	@Override
	public StateUpdateMessage prepareRemoveEntry(String list, String identifier) {
		return new StateUpdateMessage(list, ChangeOperation.REMOVE, identifier, null);
	}


	@Override
	public StateUpdateMessage prepareUpdateEntry(String list, String identifier, MessageObject message) {
		return new StateUpdateMessage(list, ChangeOperation.UPDATE, identifier, message);
	}

	@Override
	public StateUpdateMessage prepareRemoveAllEntries(String list) {
		return new StateUpdateMessage(list, ChangeOperation.REMOVE_ALL, null, null);
	}

	@Override
	public StateUpdateMessage prepareSetState(String stateId, MessageObject message) {
		return new StateUpdateMessage(stateId, ChangeOperation.SET, null, message);
	}

	@Override
	public StateUpdateMessage prepareFireAndForget(String messageType, MessageObject message) {
		return new StateUpdateMessage(messageType, ChangeOperation.SEND_AND_FORGET, null, message);
	}

	@Override
	public void executeStateMachineUpdate(StateUpdateMessage... updates) {
		sendStateMachineUpdate(new StateUpdate(name, Arrays.asList(updates)));
	}

	@Override
	public void executeStateMachineUpdate(StateUpdate update) {
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
	public void setProperty(String stateId, MessageObject message) {
		executeStateMachineUpdate(prepareSetState(stateId, message));
	}

	@Override
	public void fireAndForget(String messageType, MessageObject message) {
		executeStateMachineUpdate(prepareFireAndForget(messageType, message));
	}
}
