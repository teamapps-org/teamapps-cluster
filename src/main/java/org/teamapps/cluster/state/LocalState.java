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

public class LocalState extends AbstractReplicatedState {

	public LocalState(String name) {
		super(name);
	}

	public LocalState(String name, ReplicatedStateHandler handler) {
		super(name, handler);
	}

	@Override
	public void sendStateMachineUpdate(StateUpdate update) {
		handleStateMachineUpdate(update);
	}
}
