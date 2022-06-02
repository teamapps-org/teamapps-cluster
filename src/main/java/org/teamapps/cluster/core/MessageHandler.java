package org.teamapps.cluster.core;

import org.teamapps.protocol.schema.MessageObject;

public interface MessageHandler<MESSAGE extends MessageObject> {

	void handleMessage(MESSAGE message, String nodeId);
}
