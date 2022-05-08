package org.teamapps.cluster.state;

import org.teamapps.protocol.file.FileProvider;
import org.teamapps.protocol.file.FileSink;
import org.teamapps.protocol.message.MessageUtils;
import org.teamapps.protocol.schema.MessageObject;
import org.teamapps.protocol.schema.ModelRegistry;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class StateMachineUpdateMessage {

	private final String subStateId;
	private final ChangeOperation operation;
	private final String identifier;
	private final MessageObject message;


	public StateMachineUpdateMessage(String subStateId, ChangeOperation operation, String identifier, MessageObject message) {
		this.subStateId = subStateId;
		this.operation = operation;
		this.identifier = identifier;
		this.message = message;
	}

	public StateMachineUpdateMessage(DataInputStream dis, ModelRegistry modelRegistry, FileProvider fileProvider) throws IOException {
		subStateId = MessageUtils.readString(dis);
		operation = ChangeOperation.getById(dis.readUnsignedByte());
		identifier = MessageUtils.readString(dis);
		message = new MessageObject(dis, modelRegistry, fileProvider, null);
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

	public MessageObject getMessage() {
		return message;
	}

	public void write(DataOutputStream dos, FileSink fileSink) throws IOException {
		MessageUtils.writeString(dos, subStateId);
		dos.writeByte(operation.getId());
		MessageUtils.writeString(dos, identifier);
		message.write(dos, fileSink);
	}

	public byte[] toBytes(FileSink fileSink) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(bos);
		write(dos, fileSink);
		dos.flush();
		return bos.toByteArray();
	}
}
