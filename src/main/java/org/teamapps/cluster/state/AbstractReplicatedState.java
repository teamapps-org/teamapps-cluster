/*-
 * ========================LICENSE_START=================================
 * TeamApps Cluster
 * ---
 * Copyright (C) 2021 - 2025 TeamApps.org
 * ---
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
package org.teamapps.cluster.state;


import org.teamapps.message.protocol.message.Message;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractReplicatedState implements ReplicatedState {

	private final String name;
	private ReplicatedStateHandler handler;
	private Map<String, List<Message>> messageMap = new ConcurrentHashMap<>();
	private Map<String, Map<String, Message>> messageById = new ConcurrentHashMap<>();
	private Map<String, Message> propertyById = new ConcurrentHashMap<>();

	public AbstractReplicatedState(String name) {
		this.name = name;
	}

	public AbstractReplicatedState(String name, ReplicatedStateHandler handler) {
		this.name = name;
		this.handler = handler;
	}

	@Override
	public String getName() {
		return name;
	}

	public void setHandler(ReplicatedStateHandler handler) {
		this.handler = handler;
	}

	public ReplicatedStateHandler getHandler() {
		return handler;
	}

	@Override
	public synchronized void handleStateMachineUpdate(StateUpdate update) {
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
		Message message = stateUpdateMessage.getMessage();
		String subStateId = stateUpdateMessage.getSubStateId();
		String identifier = stateUpdateMessage.getIdentifier();
		switch (operation) {
			case ADD -> {
				messageMap.computeIfAbsent(subStateId, s -> new ArrayList<>()).add(message);
				messageById.computeIfAbsent(subStateId, s -> new ConcurrentHashMap<>()).put(identifier, message);
				handler.handleEntryAdded(subStateId, message);
			}
			case UPDATE -> {
				Message previousEntry = messageById.computeIfAbsent(subStateId, s -> new ConcurrentHashMap<>()).remove(identifier);
				if (previousEntry != null) {
					messageById.get(subStateId).put(identifier, message);
					messageMap.computeIfAbsent(subStateId, s -> new ArrayList<>()).remove(previousEntry);
					messageMap.computeIfAbsent(subStateId, s -> new ArrayList<>()).add(message);
					handler.handleEntryUpdated(subStateId, message, previousEntry);
				} else {
					messageById.get(subStateId).put(identifier, message);
					messageMap.computeIfAbsent(subStateId, s -> new ArrayList<>()).add(message);
					handler.handleEntryUpdated(subStateId, message, null);
				}
			}
			case REMOVE -> {
				Message messageObject = messageById.computeIfAbsent(subStateId, s -> new ConcurrentHashMap<>()).remove(identifier);
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
	public Message getEntry(String list, String identifier) {
		return messageById.computeIfAbsent(list, s -> new ConcurrentHashMap<>()).get(identifier);
	}

	@Override
	public List<Message> getEntries(String list) {
		List<Message> messages = messageMap.get(list);
		return messages == null ? null : new ArrayList<>(messages);
	}

	@Override
	public int getEntryCount(String list) {
		List<Message> entries = getEntries(list);
		return entries != null ? entries.size() : 0;
	}

	@Override
	public List<String> getLists() {
		return new ArrayList<>(messageMap.keySet());
	}

	@Override
	public Message getProperty(String stateId) {
		return propertyById.get(stateId);
	}

	@Override
	public StateUpdateMessage prepareAddEntry(String list, String identifier, Message message) {
		return new StateUpdateMessage(list, ChangeOperation.ADD, identifier, message);
	}

	@Override
	public StateUpdateMessage prepareRemoveEntry(String list, String identifier) {
		return new StateUpdateMessage(list, ChangeOperation.REMOVE, identifier, null);
	}


	@Override
	public StateUpdateMessage prepareUpdateEntry(String list, String identifier, Message message) {
		return new StateUpdateMessage(list, ChangeOperation.UPDATE, identifier, message);
	}

	@Override
	public StateUpdateMessage prepareRemoveAllEntries(String list) {
		return new StateUpdateMessage(list, ChangeOperation.REMOVE_ALL, null, null);
	}

	@Override
	public StateUpdateMessage prepareSetState(String stateId, Message message) {
		return new StateUpdateMessage(stateId, ChangeOperation.SET, null, message);
	}

	@Override
	public StateUpdateMessage prepareFireAndForget(String messageType, Message message) {
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
	public void addEntry(String list, String identifier, Message message) {
		executeStateMachineUpdate(prepareAddEntry(list, identifier, message));
	}

	@Override
	public void removeEntry(String list, String identifier) {
		executeStateMachineUpdate(prepareRemoveEntry(list, identifier));
	}

	@Override
	public void updateEntry(String list, String identifier, Message message) {
		executeStateMachineUpdate(prepareUpdateEntry(list, identifier, message));
	}

	@Override
	public void removeAllEntries(String list) {
		executeStateMachineUpdate(prepareRemoveAllEntries(list));
	}

	@Override
	public void setProperty(String stateId, Message message) {
		executeStateMachineUpdate(prepareSetState(stateId, message));
	}

	@Override
	public void fireAndForget(String messageType, Message message) {
		executeStateMachineUpdate(prepareFireAndForget(messageType, message));
	}
}
