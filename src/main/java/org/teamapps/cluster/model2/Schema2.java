package org.teamapps.cluster.model2;

import org.teamapps.cluster.dto.*;
import java.util.HashMap;
import java.util.Map;

public class Schema2 implements MessageDecoderRegistry {

    public static MessageSchema SCHEMA = new MessageSchema(101, "schema2", "org.teamapps.cluster.model2");
    public static MessageDecoderRegistry REGISTRY = new Schema2();
	private final static Map<Integer, MessageDecoder<? extends Message>> DECODERS = new HashMap<>();

    static {
		MessageField f1 = SCHEMA.addObject(1, "clusterNodeData", null);
		SCHEMA.addField(f1, 2, "nodeId", null, MessageFieldType.STRING, MessageFieldContentType.GENERIC, null);
		SCHEMA.addField(f1, 3, "host", null, MessageFieldType.STRING, MessageFieldContentType.GENERIC, null);
		SCHEMA.addField(f1, 4, "port", null, MessageFieldType.INT, MessageFieldContentType.GENERIC, null);
		SCHEMA.addField(f1, 5, "response", null, MessageFieldType.BOOLEAN, MessageFieldContentType.GENERIC, null);
		SCHEMA.addField(f1, 6, "availableServices", null, MessageFieldType.STRING_ARRAY, MessageFieldContentType.GENERIC, null);
		MessageField f7 = SCHEMA.addObject(7, "clusterNodeInfo", null);
		SCHEMA.addField(f7, 8, "response", null, MessageFieldType.BOOLEAN, MessageFieldContentType.GENERIC, null);
		SCHEMA.addSingleReference(f7, f1, "localNode", null);
		SCHEMA.addMultiReference(f7, f1, "knownRemoteNodes", null);
		MessageField f11 = SCHEMA.addObject(11, "serviceClusterRequest", null);
		SCHEMA.addField(f11, 12, "requestId", null, MessageFieldType.LONG, MessageFieldContentType.GENERIC, null);
		SCHEMA.addField(f11, 13, "serviceName", null, MessageFieldType.STRING, MessageFieldContentType.GENERIC, null);
		SCHEMA.addField(f11, 14, "method", null, MessageFieldType.STRING, MessageFieldContentType.GENERIC, null);
		SCHEMA.addField(f11, 15, "requestData", null, MessageFieldType.BYTE_ARRAY, MessageFieldContentType.GENERIC, null);
		MessageField f16 = SCHEMA.addObject(16, "serviceClusterResponse", null);
		SCHEMA.addField(f16, 17, "requestId", null, MessageFieldType.LONG, MessageFieldContentType.GENERIC, null);
		SCHEMA.addField(f16, 18, "responseData", null, MessageFieldType.BYTE_ARRAY, MessageFieldContentType.GENERIC, null);
		SCHEMA.addField(f16, 19, "error", null, MessageFieldType.BOOLEAN, MessageFieldContentType.GENERIC, null);
		SCHEMA.addField(f16, 20, "errorMessage", null, MessageFieldType.STRING, MessageFieldContentType.GENERIC, null);
		MessageField f21 = SCHEMA.addObject(21, "clusterFileTransfer", null);
		SCHEMA.addField(f21, 22, "fileId", null, MessageFieldType.STRING, MessageFieldContentType.GENERIC, null);
		SCHEMA.addField(f21, 23, "length", null, MessageFieldType.LONG, MessageFieldContentType.GENERIC, null);
		SCHEMA.addField(f21, 24, "data", null, MessageFieldType.BYTE_ARRAY, MessageFieldContentType.GENERIC, null);
		SCHEMA.addField(f21, 25, "initialMessage", null, MessageFieldType.BOOLEAN, MessageFieldContentType.GENERIC, null);
		SCHEMA.addField(f21, 26, "lastMessage", null, MessageFieldType.BOOLEAN, MessageFieldContentType.GENERIC, null);
		MessageField f27 = SCHEMA.addObject(27, "clusterFileTransferResponse", null);
		SCHEMA.addField(f27, 28, "fileId", null, MessageFieldType.STRING, MessageFieldContentType.GENERIC, null);
		SCHEMA.addField(f27, 29, "receivedData", null, MessageFieldType.LONG, MessageFieldContentType.GENERIC, null);

		DECODERS.put(101001, ClusterNodeData.getMessageDecoder());
		DECODERS.put(101007, ClusterNodeInfo.getMessageDecoder());
		DECODERS.put(101011, ServiceClusterRequest.getMessageDecoder());
		DECODERS.put(101016, ServiceClusterResponse.getMessageDecoder());
		DECODERS.put(101021, ClusterFileTransfer.getMessageDecoder());
		DECODERS.put(101027, ClusterFileTransferResponse.getMessageDecoder());

    }

	public MessageDecoder<? extends Message> getMessageDecoder(int id) {
		return DECODERS.get(id);
	}

	@Override
	public boolean containsDecoder(int id) {
		return DECODERS.containsKey(id);
	}

}