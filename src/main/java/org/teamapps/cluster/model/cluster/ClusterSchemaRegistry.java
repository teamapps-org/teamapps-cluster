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
package org.teamapps.cluster.model.cluster;

import org.teamapps.cluster.dto.*;
import java.util.HashMap;
import java.util.Map;

public class ClusterSchemaRegistry implements MessageDecoderRegistry {

    public static MessageSchema SCHEMA = new MessageSchema(101, "clusterSchemaRegistry", "org.teamapps.cluster.model.cluster");
    public static MessageDecoderRegistry REGISTRY = new ClusterSchemaRegistry();
	private final static Map<Integer, MessageDecoder<? extends Message>> DECODERS = new HashMap<>();

    static {
		MessageField f1 = SCHEMA.addObject(1, "clusterNodeData", null);
		SCHEMA.addField(f1, 2, "nodeId", null, MessageFieldType.STRING, MessageFieldContentType.GENERIC, null);
		SCHEMA.addField(f1, 3, "host", null, MessageFieldType.STRING, MessageFieldContentType.GENERIC, null);
		SCHEMA.addField(f1, 4, "port", null, MessageFieldType.INT, MessageFieldContentType.GENERIC, null);
		SCHEMA.addField(f1, 5, "response", null, MessageFieldType.BOOLEAN, MessageFieldContentType.GENERIC, null);
		SCHEMA.addField(f1, 6, "availableServices", null, MessageFieldType.STRING_ARRAY, MessageFieldContentType.GENERIC, null);
		MessageField f7 = SCHEMA.addObject(7, "clusterTopicInfo", null);
		SCHEMA.addField(f7, 8, "topicName", null, MessageFieldType.STRING, MessageFieldContentType.GENERIC, null);
		SCHEMA.addField(f7, 9, "nodeIds", null, MessageFieldType.STRING_ARRAY, MessageFieldContentType.GENERIC, null);
		MessageField f10 = SCHEMA.addObject(10, "clusterTopicMessage", null);
		SCHEMA.addField(f10, 11, "topic", null, MessageFieldType.STRING, MessageFieldContentType.GENERIC, null);
		SCHEMA.addField(f10, 12, "data", null, MessageFieldType.BYTE_ARRAY, MessageFieldContentType.GENERIC, null);
		MessageField f13 = SCHEMA.addObject(13, "clusterNodeInfo", null);
		SCHEMA.addField(f13, 14, "response", null, MessageFieldType.BOOLEAN, MessageFieldContentType.GENERIC, null);
		SCHEMA.addSingleReference(f13, f1, "localNode", null);
		SCHEMA.addMultiReference(f13, f1, "knownRemoteNodes", null);
		SCHEMA.addMultiReference(f13, f7, "clusterTopics", null);
		MessageField f18 = SCHEMA.addObject(18, "serviceClusterRequest", null);
		SCHEMA.addField(f18, 19, "requestId", null, MessageFieldType.LONG, MessageFieldContentType.GENERIC, null);
		SCHEMA.addField(f18, 20, "serviceName", null, MessageFieldType.STRING, MessageFieldContentType.GENERIC, null);
		SCHEMA.addField(f18, 21, "method", null, MessageFieldType.STRING, MessageFieldContentType.GENERIC, null);
		SCHEMA.addField(f18, 22, "requestData", null, MessageFieldType.BYTE_ARRAY, MessageFieldContentType.GENERIC, null);
		MessageField f23 = SCHEMA.addObject(23, "serviceClusterResponse", null);
		SCHEMA.addField(f23, 24, "requestId", null, MessageFieldType.LONG, MessageFieldContentType.GENERIC, null);
		SCHEMA.addField(f23, 25, "responseData", null, MessageFieldType.BYTE_ARRAY, MessageFieldContentType.GENERIC, null);
		SCHEMA.addField(f23, 26, "error", null, MessageFieldType.BOOLEAN, MessageFieldContentType.GENERIC, null);
		SCHEMA.addField(f23, 27, "errorMessage", null, MessageFieldType.STRING, MessageFieldContentType.GENERIC, null);
		MessageField f28 = SCHEMA.addObject(28, "clusterFileTransfer", null);
		SCHEMA.addField(f28, 29, "fileId", null, MessageFieldType.STRING, MessageFieldContentType.GENERIC, null);
		SCHEMA.addField(f28, 30, "length", null, MessageFieldType.LONG, MessageFieldContentType.GENERIC, null);
		SCHEMA.addField(f28, 31, "messageIndex", null, MessageFieldType.INT, MessageFieldContentType.GENERIC, null);
		SCHEMA.addField(f28, 32, "data", null, MessageFieldType.BYTE_ARRAY, MessageFieldContentType.GENERIC, null);
		SCHEMA.addField(f28, 33, "initialMessage", null, MessageFieldType.BOOLEAN, MessageFieldContentType.GENERIC, null);
		SCHEMA.addField(f28, 34, "lastMessage", null, MessageFieldType.BOOLEAN, MessageFieldContentType.GENERIC, null);
		MessageField f35 = SCHEMA.addObject(35, "clusterFileTransferResponse", null);
		SCHEMA.addField(f35, 36, "fileId", null, MessageFieldType.STRING, MessageFieldContentType.GENERIC, null);
		SCHEMA.addField(f35, 37, "receivedData", null, MessageFieldType.LONG, MessageFieldContentType.GENERIC, null);
		MessageField f38 = SCHEMA.addObject(38, "keepAliveMessage", null);
		MessageField f39 = SCHEMA.addObject(39, "DbTransaction", null);
		SCHEMA.addField(f39, 40, "bytes", null, MessageFieldType.BYTE_ARRAY, MessageFieldContentType.GENERIC, null);
		MessageField f41 = SCHEMA.addObject(41, "DbTransactionRequest", null);
		SCHEMA.addField(f41, 42, "bytes", null, MessageFieldType.BYTE_ARRAY, MessageFieldContentType.GENERIC, null);
		MessageField f43 = SCHEMA.addObject(43, "DbTransactionListRequest", null);
		SCHEMA.addField(f43, 44, "lastKnownTransactionId", null, MessageFieldType.LONG, MessageFieldContentType.GENERIC, null);
		MessageField f45 = SCHEMA.addObject(45, "DbTransactionList", null);
		SCHEMA.addField(f45, 46, "lastKnownTransactionId", null, MessageFieldType.LONG, MessageFieldContentType.GENERIC, null);
		SCHEMA.addField(f45, 47, "transactionCount", null, MessageFieldType.LONG, MessageFieldContentType.GENERIC, null);
		SCHEMA.addField(f45, 48, "transactionsFile", null, MessageFieldType.FILE, MessageFieldContentType.GENERIC, null);

		DECODERS.put(101001, ClusterNodeData.getMessageDecoder());
		DECODERS.put(101007, ClusterTopicInfo.getMessageDecoder());
		DECODERS.put(101010, ClusterTopicMessage.getMessageDecoder());
		DECODERS.put(101013, ClusterNodeInfo.getMessageDecoder());
		DECODERS.put(101018, ServiceClusterRequest.getMessageDecoder());
		DECODERS.put(101023, ServiceClusterResponse.getMessageDecoder());
		DECODERS.put(101028, ClusterFileTransfer.getMessageDecoder());
		DECODERS.put(101035, ClusterFileTransferResponse.getMessageDecoder());
		DECODERS.put(101038, KeepAliveMessage.getMessageDecoder());
		DECODERS.put(101039, DbTransaction.getMessageDecoder());
		DECODERS.put(101041, DbTransactionRequest.getMessageDecoder());
		DECODERS.put(101043, DbTransactionListRequest.getMessageDecoder());
		DECODERS.put(101045, DbTransactionList.getMessageDecoder());

    }

	public MessageDecoder<? extends Message> getMessageDecoder(int id) {
		return DECODERS.get(id);
	}

	@Override
	public boolean containsDecoder(int id) {
		return DECODERS.containsKey(id);
	}

}
