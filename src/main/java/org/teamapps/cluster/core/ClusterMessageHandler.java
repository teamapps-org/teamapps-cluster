package org.teamapps.cluster.core;

import org.teamapps.protocol.schema.MessageObject;
import org.teamapps.protocol.schema.PojoObjectDecoder;

import java.util.concurrent.ExecutorService;

public class ClusterMessageHandler<MESSAGE extends MessageObject> {

	private final MessageHandler<MESSAGE> messageHandler;
	private final PojoObjectDecoder<MESSAGE> messageDecoder;

	public ClusterMessageHandler(MessageHandler<MESSAGE> messageHandler, PojoObjectDecoder<MESSAGE> messageDecoder) {
		this.messageHandler = messageHandler;
		this.messageDecoder = messageDecoder;
	}

	public void handleMessage(MessageObject message, String nodeId, ExecutorService executorService) {
		executorService.submit(() -> {
			MESSAGE remappedMessage = messageDecoder.remap(message);
			messageHandler.handleMessage(remappedMessage, nodeId);
		});
	}
}
