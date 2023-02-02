/*-
 * ========================LICENSE_START=================================
 * TeamApps Cluster
 * ---
 * Copyright (C) 2021 - 2023 TeamApps.org
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

public enum ChangeOperation {

	ADD(1),
	UPDATE(2),
	REMOVE(3),
	SET(4),
	REMOVE_ALL(5),
	SEND_AND_FORGET(6),

	;
	private final int id;

	ChangeOperation(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public static ChangeOperation getById(int id) {
		return switch (id) {
			case 1 -> ADD;
			case 2 -> UPDATE;
			case 3 -> REMOVE;
			case 4 -> SET;
			case 5 -> REMOVE_ALL;
			case 6 -> SEND_AND_FORGET;
			default -> null;
		};
	}
}
