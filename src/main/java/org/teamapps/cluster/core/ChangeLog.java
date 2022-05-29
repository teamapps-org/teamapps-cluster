package org.teamapps.cluster.core;

import org.teamapps.protocol.schema.MessageObject;

public interface ChangeLog {

	String getName();



	void sendChangeLogMessage(MessageObject message);

	void handleChangeLogMessage(MessageObject message);
}
