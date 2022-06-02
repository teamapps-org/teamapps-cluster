/*-
 * ========================LICENSE_START=================================
 * TeamApps Cluster
 * ---
 * Copyright (C) 2021 - 2022 TeamApps.org
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
