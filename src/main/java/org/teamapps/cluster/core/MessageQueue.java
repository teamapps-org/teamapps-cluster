package org.teamapps.cluster.core;

import org.teamapps.protocol.schema.MessageObject;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MessageQueue {

	private ArrayBlockingQueue<MessageQueueEntry> messageQueue = new ArrayBlockingQueue<>(100_000);

	private MessageQueueEntry lastEntry;

	public void reuseQueue(MessageQueue queue) {
		List<MessageQueueEntry> entries = queue.getEntries(false);
		if (messageQueue.remainingCapacity() > entries.size()) {
			entries.forEach(e -> messageQueue.offer(e));
		}
	}

	public boolean addMessage(MessageQueueEntry entry) {
		return messageQueue.offer(entry);
	}

	public boolean addMessage(MessageObject message, boolean resendOnError) {
		MessageQueueEntry queueEntry = new MessageQueueEntry(resendOnError, message);
		return messageQueue.offer(queueEntry);
	}

	public MessageQueueEntry getNext() {
		if (lastEntry != null) {
			return lastEntry;
		} else {
			while (true) {
				try {
					lastEntry = messageQueue.poll(1_000, TimeUnit.MILLISECONDS);
					if (lastEntry != null) {
						return lastEntry;
					}
				} catch (InterruptedException ignore) { }
			}
		}
	}

	public void commitLastMessage() {
		lastEntry = null;
	}

	public void recycleQueue() {
		if (lastEntry != null && !lastEntry.isResendOnError()) {
			lastEntry = null;
		}
		messageQueue.removeIf(entry -> !entry.isResendOnError());
	}

	public List<MessageQueueEntry> getEntries(boolean includeAll) {
		return messageQueue.stream()
				.filter(e -> e.isResendOnError() || includeAll)
				.collect(Collectors.toList());
	}

}
