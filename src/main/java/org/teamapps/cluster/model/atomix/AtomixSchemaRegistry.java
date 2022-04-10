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
package org.teamapps.cluster.model.atomix;

import org.teamapps.cluster.dto.*;
import java.util.HashMap;
import java.util.Map;

public class AtomixSchemaRegistry implements MessageDecoderRegistry {

    public static MessageSchema SCHEMA = new MessageSchema(100, "atomixSchemaRegistry", "org.teamapps.cluster.model.atomix");
    public static MessageDecoderRegistry REGISTRY = new AtomixSchemaRegistry();
	private final static Map<Integer, MessageDecoder<? extends Message>> DECODERS = new HashMap<>();

    static {
		MessageField f1 = SCHEMA.addObject(1, "fileTransfer", null);
		SCHEMA.addField(f1, 2, "fileId", null, MessageFieldType.STRING, MessageFieldContentType.GENERIC, null);
		SCHEMA.addField(f1, 3, "length", null, MessageFieldType.LONG, MessageFieldContentType.GENERIC, null);
		SCHEMA.addField(f1, 4, "data", null, MessageFieldType.BYTE_ARRAY, MessageFieldContentType.GENERIC, null);
		SCHEMA.addField(f1, 5, "finished", null, MessageFieldType.BOOLEAN, MessageFieldContentType.GENERIC, null);
		MessageField f6 = SCHEMA.addObject(6, "fileTransferResponse", null);
		SCHEMA.addField(f6, 7, "receivedData", null, MessageFieldType.LONG, MessageFieldContentType.GENERIC, null);
		SCHEMA.addField(f6, 8, "finished", null, MessageFieldType.BOOLEAN, MessageFieldContentType.GENERIC, null);
		MessageField f9 = SCHEMA.addObject(9, "clusterMessage", null);
		SCHEMA.addField(f9, 10, "memberId", null, MessageFieldType.STRING, MessageFieldContentType.GENERIC, null);
		SCHEMA.addField(f9, 11, "clusterService", null, MessageFieldType.STRING, MessageFieldContentType.GENERIC, null);
		SCHEMA.addField(f9, 12, "clusterMethod", null, MessageFieldType.STRING, MessageFieldContentType.GENERIC, null);
		SCHEMA.addField(f9, 13, "messageData", null, MessageFieldType.BYTE_ARRAY, MessageFieldContentType.GENERIC, null);
		SCHEMA.addField(f9, 14, "error", null, MessageFieldType.BOOLEAN, MessageFieldContentType.GENERIC, null);
		SCHEMA.addField(f9, 15, "errorMessage", null, MessageFieldType.STRING, MessageFieldContentType.GENERIC, null);

		DECODERS.put(100001, FileTransfer.getMessageDecoder());
		DECODERS.put(100006, FileTransferResponse.getMessageDecoder());
		DECODERS.put(100009, ClusterMessage.getMessageDecoder());

    }

	public MessageDecoder<? extends Message> getMessageDecoder(int id) {
		return DECODERS.get(id);
	}

	@Override
	public boolean containsDecoder(int id) {
		return DECODERS.containsKey(id);
	}

}
