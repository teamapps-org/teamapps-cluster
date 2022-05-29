package org.teamapps.cluster.core;

import org.teamapps.protocol.schema.MessageObject;
import org.teamapps.protocol.schema.PojoObjectDecoder;

public interface RemoteNode extends Node, ConnectionHandler {

	@Override
	default boolean isLocalNode() {
		return false;
	}

	boolean isConnected();

	boolean isOutbound();

	void sendMessage(MessageObject message, boolean resentOnError);

	<REQUEST extends MessageObject, RESPONSE extends MessageObject> RESPONSE executeServiceMethod(String service, String serviceMethod, REQUEST request, PojoObjectDecoder<RESPONSE> responseDecoder);
}
