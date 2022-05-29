package org.teamapps.cluster.core;

import org.teamapps.protocol.schema.MessageObject;

public class MessageQueueEntry {

	private final boolean resentOnError;
	private final MessageObject message;

	public MessageQueueEntry(boolean resentOnError, MessageObject message) {
		this.resentOnError = resentOnError;
		this.message = message;
	}

	public boolean isResentOnError() {
		return resentOnError;
	}

	public MessageObject getMessage() {
		return message;
	}
}
