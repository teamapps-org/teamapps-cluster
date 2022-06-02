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
