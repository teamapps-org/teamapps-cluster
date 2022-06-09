/*-
 * ========================LICENSE_START=================================
 * TeamApps Cluster
 * ---
 * Copyright (C) 2021 - 2022 TeamApps.org
 * ---
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
package org.teamapps.cluster.core;

import org.teamapps.protocol.schema.MessageObject;
import org.teamapps.protocol.schema.PojoObjectDecoder;

public interface RemoteNode extends Node, ConnectionHandler {

	@Override
	default boolean isLocalNode() {
		return false;
	}

	void recycleNode(RemoteNode node);

	boolean isConnected();

	boolean isOutbound();

	void sendMessage(MessageObject message, boolean resendOnError);

	<REQUEST extends MessageObject, RESPONSE extends MessageObject> RESPONSE executeServiceMethod(String service, String serviceMethod, REQUEST request, PojoObjectDecoder<RESPONSE> responseDecoder);

	void shutDown();

	MessageQueue getMessageQueue();

	long getSentBytes();

	long getReceivedBytes();

	long getSentMessages();

	long getReceivedMessages();

	long getReconnects();

	long getConnectedSince();
}
