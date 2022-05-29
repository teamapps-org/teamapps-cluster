package org.teamapps.cluster.core;

import org.teamapps.protocol.schema.MessageObject;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

public class MessageQueue {

	private ArrayBlockingQueue<MessageQueueEntry> messageQueue = new ArrayBlockingQueue<>(100_000);

	private MessageQueueEntry lastEntry;

	public boolean addMessage(MessageObject message, boolean resentOnError) {
		MessageQueueEntry queueEntry = new MessageQueueEntry(resentOnError, message);
		return messageQueue.offer(queueEntry);
	}

	public MessageObject getNext() {
		if (lastEntry != null) {
			return lastEntry.getMessage();
		} else {
			while (true) {
				try {
					messageQueue.poll(1_000, TimeUnit.MILLISECONDS);
					lastEntry = messageQueue.take();
					return lastEntry.getMessage();
				} catch (InterruptedException ignore) { }
			}
		}
	}

	public void commitLastMessage() {
		lastEntry = null;
	}

	public void recycleQueue() {
		if (lastEntry != null && !lastEntry.isResentOnError()) {
			lastEntry = null;
		}
		messageQueue.removeIf(entry -> !entry.isResentOnError());
	}

}
