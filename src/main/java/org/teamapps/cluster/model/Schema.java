package org.teamapps.cluster.model;

import org.teamapps.cluster.dto.*;
import java.util.HashMap;
import java.util.Map;

public class Schema implements MessageDecoderRegistry {

    public static MessageSchema SCHEMA = new MessageSchema(100, "schema");
    public static MessageDecoderRegistry REGISTRY = new Schema();
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