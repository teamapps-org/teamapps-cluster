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

import org.teamapps.message.protocol.file.FileDataReader;
import org.teamapps.message.protocol.file.FileDataWriter;
import org.teamapps.message.protocol.model.ModelRegistry;
import org.teamapps.message.protocol.utils.MessageUtils;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StateUpdate {

	private final String stateMachine;
	private long updateId;
	private final List<StateUpdateMessage> stateUpdateMessages;
	private final List<ReplicatedStateTransactionRule> transactionConditions;


	public StateUpdate(String stateMachine, StateUpdateMessage stateUpdateMessage) {
		this.stateMachine = stateMachine;
		this.stateUpdateMessages = Collections.singletonList(stateUpdateMessage);
		this.transactionConditions = null;
	}

	public StateUpdate(String stateMachine, List<StateUpdateMessage> stateUpdateMessages) {
		this.stateMachine = stateMachine;
		this.stateUpdateMessages = stateUpdateMessages;
		this.transactionConditions = null;
	}

	public StateUpdate(String stateMachine, List<StateUpdateMessage> stateUpdateMessages, List<ReplicatedStateTransactionRule> transactionConditions) {
		this.stateMachine = stateMachine;
		this.stateUpdateMessages = stateUpdateMessages;
		this.transactionConditions = transactionConditions;
	}

	public StateUpdate(DataInputStream dis, ModelRegistry modelRegistry, FileDataReader fileProvider) throws IOException {
		stateMachine =  MessageUtils.readString(dis);
		updateId = dis.readLong();
		int size = dis.readInt();
		stateUpdateMessages = new ArrayList<>();
		for (int i = 0; i < size; i++) {
			stateUpdateMessages.add(new StateUpdateMessage(dis, modelRegistry, fileProvider));
		}
		int rules = dis.readInt();
		if (rules > 0) {
			transactionConditions = new ArrayList<>();
			for (int i = 0; i < rules; i++) {
				//todo
			}
		} else {
			transactionConditions = null;
		}
	}

	public List<StateUpdateMessage> getUpdateMessages() {
		return stateUpdateMessages;
	}

	public List<ReplicatedStateTransactionRule> getTransactionConditions() {
		return transactionConditions;
	}

	public long getUpdateId() {
		return updateId;
	}

	public void setUpdateId(long updateId) {
		this.updateId = updateId;
	}

	public String getStateMachine() {
		return stateMachine;
	}

	public void write(DataOutputStream dos, FileDataWriter fileDataWriter) throws IOException {
		MessageUtils.writeString(dos, stateMachine);
		dos.writeLong(updateId);
		dos.writeInt(stateUpdateMessages.size());
		for (StateUpdateMessage stateUpdateMessage : stateUpdateMessages) {
			stateUpdateMessage.write(dos, fileDataWriter);
		}
		if (transactionConditions == null || transactionConditions.isEmpty()) {
			dos.writeInt(0);
		} else {
			dos.writeInt(transactionConditions.size());
			//todo write rules
		}
	}

	public byte[] toBytes(FileDataWriter fileDataWriter) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(bos);
		write(dos, fileDataWriter);
		dos.flush();
		return bos.toByteArray();
	}
}
