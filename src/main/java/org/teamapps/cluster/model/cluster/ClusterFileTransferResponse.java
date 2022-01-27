package org.teamapps.cluster.model.cluster;

import org.teamapps.cluster.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Function;


public class ClusterFileTransferResponse extends Message {
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public final static Function<byte[], ClusterFileTransferResponse> DECODER_FUNCTION = bytes -> {
		try {
			return new ClusterFileTransferResponse(bytes);
		} catch (IOException e) {
			LOGGER.error("Error creating ClusterFileTransferResponse instance", e);
		}
		return null;
	};

	private final static MessageDecoder<ClusterFileTransferResponse> decoder = (dis, fileProvider) -> {
		try {
			return new ClusterFileTransferResponse(dis, fileProvider);
		} catch (IOException e) {
			LOGGER.error("Error creating ClusterFileTransferResponse instance", e);
		}
		return null;
	};

	public static MessageDecoder<ClusterFileTransferResponse> getMessageDecoder() {
		return decoder;
	}

    public final static int ROOT_FIELD_ID = 101027;

	public ClusterFileTransferResponse() {
		super(ClusterSchemaRegistry.SCHEMA.getFieldById(101027), new ArrayList<>());
	}

	public ClusterFileTransferResponse(ByteBuffer buf) {
		super(buf, ClusterSchemaRegistry.SCHEMA);
	}

	public ClusterFileTransferResponse(DataInputStream dis) throws IOException {
		super(dis, ClusterSchemaRegistry.SCHEMA);
	}

	public ClusterFileTransferResponse(DataInputStream dis, FileProvider fileProvider) throws IOException {
		super(dis, ClusterSchemaRegistry.SCHEMA, fileProvider, ClusterSchemaRegistry.REGISTRY);
	}

	public ClusterFileTransferResponse(byte[] bytes) throws IOException {
		super(bytes, ClusterSchemaRegistry.SCHEMA);
	}

	public ClusterFileTransferResponse(byte[] bytes, FileProvider fileProvider) throws IOException {
		super(bytes, ClusterSchemaRegistry.SCHEMA, fileProvider, ClusterSchemaRegistry.REGISTRY);
	}

	public String getFileId() {
		return getStringValue("fileId");
	}

	public ClusterFileTransferResponse setFileId(String value) {
		setPropertyValue("fileId", value);
		return this;
	}
	public long getReceivedData() {
		return getLongValue("receivedData");
	}

	public ClusterFileTransferResponse setReceivedData(long value) {
		setPropertyValue("receivedData", value);
		return this;
	}


}