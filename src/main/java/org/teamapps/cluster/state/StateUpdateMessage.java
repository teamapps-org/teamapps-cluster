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
import org.teamapps.message.protocol.message.Message;
import org.teamapps.message.protocol.model.ModelRegistry;
import org.teamapps.message.protocol.utils.MessageUtils;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class StateUpdateMessage {

	private final String subStateId;
	private final ChangeOperation operation;
	private final String identifier;
	private final Message message;


	public StateUpdateMessage(String subStateId, ChangeOperation operation, String identifier, Message message) {
		this.subStateId = subStateId;
		this.operation = operation;
		this.identifier = identifier;
		this.message = message;
	}

	public StateUpdateMessage(DataInputStream dis, ModelRegistry modelRegistry, FileDataReader fileDataReader) throws IOException {
		subStateId = MessageUtils.readString(dis);
		operation = ChangeOperation.getById(dis.readUnsignedByte());
		identifier = MessageUtils.readString(dis);
		message = new Message(dis, modelRegistry, fileDataReader, null);
	}

	public String getSubStateId() {
		return subStateId;
	}

	public ChangeOperation getOperation() {
		return operation;
	}

	public String getIdentifier() {
		return identifier;
	}

	public Message getMessage() {
		return message;
	}

	public void write(DataOutputStream dos, FileDataWriter fileDataWriter) throws IOException {
		MessageUtils.writeString(dos, subStateId);
		dos.writeByte(operation.getId());
		MessageUtils.writeString(dos, identifier);
		message.write(dos, fileDataWriter);
	}

	public byte[] toBytes(FileDataWriter fileDataWriter) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(bos);
		write(dos, fileDataWriter);
		dos.flush();
		return bos.toByteArray();
	}
}
