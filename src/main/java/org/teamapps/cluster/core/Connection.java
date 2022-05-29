package org.teamapps.cluster.core;

import org.teamapps.protocol.file.FileProvider;
import org.teamapps.protocol.file.FileSink;

public interface Connection extends FileProvider, FileSink {

	boolean isConnected();

	void close();

	long lastMessageTimestamp();

	long sendBytes();

	long receivedBytes();

	void sendKeepAlive();

}
