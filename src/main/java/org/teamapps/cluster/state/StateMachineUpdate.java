package org.teamapps.cluster.state;

import org.teamapps.protocol.file.FileProvider;
import org.teamapps.protocol.file.FileSink;
import org.teamapps.protocol.message.MessageUtils;
import org.teamapps.protocol.schema.ModelRegistry;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StateMachineUpdate {

	private final String stateMachine;
	private long updateId;
	private final List<StateMachineUpdateMessage> stateMachineUpdateMessages;


	public StateMachineUpdate(String stateMachine, StateMachineUpdateMessage stateMachineUpdateMessage) {
		this.stateMachine = stateMachine;
		this.updateId = updateId;
		this.stateMachineUpdateMessages = Collections.singletonList(stateMachineUpdateMessage);
	}

	public StateMachineUpdate(String stateMachine, List<StateMachineUpdateMessage> stateMachineUpdateMessages) {
		this.stateMachine = stateMachine;
		this.updateId = updateId;
		this.stateMachineUpdateMessages = stateMachineUpdateMessages;
	}

	public StateMachineUpdate(DataInputStream dis, ModelRegistry modelRegistry, FileProvider fileProvider) throws IOException {
		stateMachine =  MessageUtils.readString(dis);
		updateId = dis.readLong();
		int size = dis.readInt();
		stateMachineUpdateMessages = new ArrayList<>();
		for (int i = 0; i < size; i++) {
			stateMachineUpdateMessages.add(new StateMachineUpdateMessage(dis, modelRegistry, fileProvider));
		}
	}

	public List<StateMachineUpdateMessage> getUpdateMessages() {
		return stateMachineUpdateMessages;
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

	public void write(DataOutputStream dos, FileSink fileSink) throws IOException {
		MessageUtils.writeString(dos, stateMachine);
		dos.writeLong(updateId);
		dos.writeInt(stateMachineUpdateMessages.size());
		for (StateMachineUpdateMessage stateMachineUpdateMessage : stateMachineUpdateMessages) {
			stateMachineUpdateMessage.write(dos, fileSink);
		}
	}

	public byte[] toBytes(FileSink fileSink) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(bos);
		write(dos, fileSink);
		dos.flush();
		return bos.toByteArray();
	}
}
