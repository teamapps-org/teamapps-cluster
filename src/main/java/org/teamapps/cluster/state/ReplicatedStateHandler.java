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

import org.teamapps.message.protocol.message.Message;

public interface ReplicatedStateHandler {

	void handleStateUpdated(String stateId, Message state);

	void handleEntryAdded(String list, Message message);

	void handleEntryRemoved(String list, Message message);

	void handleEntryUpdated(String list, Message currentState, Message previousState);

	void handleAllEntriesRemoved(String list);

	void handleFireAndForget(String list, Message message);

	void handleStateMachineRemoved();
}
