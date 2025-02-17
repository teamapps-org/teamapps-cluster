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

import java.util.List;

public interface ReplicatedState extends ReplicatedChangeLog {

	String getName();

	StateUpdateMessage prepareAddEntry(String list, String identifier, Message message);
	StateUpdateMessage prepareRemoveEntry(String list, String identifier);
	StateUpdateMessage prepareUpdateEntry(String list, String identifier, Message message);
	StateUpdateMessage prepareRemoveAllEntries(String list);
	StateUpdateMessage prepareSetState(String stateId, Message message);
	StateUpdateMessage prepareFireAndForget(String messageType, Message message);

	void executeStateMachineUpdate(StateUpdateMessage... updates);
	void executeStateMachineUpdate(StateUpdate update);

	void addEntry(String list, String identifier, Message message);
	void removeEntry(String list, String identifier);
	void updateEntry(String list, String identifier, Message message);
	void removeAllEntries(String list);
	void setProperty(String propertyId, Message message);
	void fireAndForget(String messageType, Message message);

	Message getEntry(String list, String identifier);
	List<Message> getEntries(String list);
	int getEntryCount(String list);
	List<String> getLists();
	Message getProperty(String stateId);



}
