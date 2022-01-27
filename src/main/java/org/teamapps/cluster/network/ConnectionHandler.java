package org.teamapps.cluster.network;

public interface ConnectionHandler {

	void handleMessage(byte[] bytes);

	void handleConnectionClosed();

}
