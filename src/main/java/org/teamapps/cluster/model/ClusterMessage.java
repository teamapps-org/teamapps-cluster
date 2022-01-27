package org.teamapps.cluster.model;

import org.teamapps.cluster.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Function;


public class ClusterMessage extends Message {
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public final static Function<byte[], ClusterMessage> DECODER_FUNCTION = bytes -> {
		try {
			return new ClusterMessage(bytes);
		} catch (IOException e) {
			LOGGER.error("Error creating ClusterMessage instance", e);
		}
		return null;
	};

	private final static MessageDecoder<ClusterMessage> decoder = (dis, fileProvider) -> {
		try {
			return new ClusterMessage(dis, fileProvider);
		} catch (IOException e) {
			LOGGER.error("Error creating ClusterMessage instance", e);
		}
		return null;
	};

	public static MessageDecoder<ClusterMessage> getMessageDecoder() {
		return decoder;
	}

    public final static int ROOT_FIELD_ID = 100009;

	public ClusterMessage() {
		super(Schema.SCHEMA.getFieldById(100009), new ArrayList<>());
	}

	public ClusterMessage(ByteBuffer buf) {
		super(buf, Schema.SCHEMA);
	}

	public ClusterMessage(DataInputStream dis) throws IOException {
		super(dis, Schema.SCHEMA);
	}

	public ClusterMessage(DataInputStream dis, FileProvider fileProvider) throws IOException {
		super(dis, Schema.SCHEMA, fileProvider, Schema.REGISTRY);
	}

	public ClusterMessage(byte[] bytes) throws IOException {
		super(bytes, Schema.SCHEMA);
	}

	public ClusterMessage(byte[] bytes, FileProvider fileProvider) throws IOException {
		super(bytes, Schema.SCHEMA, fileProvider, Schema.REGISTRY);
	}

	public String getMemberId() {
		return getStringValue("memberId");
	}

	public ClusterMessage setMemberId(String value) {
		setPropertyValue("memberId", value);
		return this;
	}
	public String getClusterService() {
		return getStringValue("clusterService");
	}

	public ClusterMessage setClusterService(String value) {
		setPropertyValue("clusterService", value);
		return this;
	}
	public String getClusterMethod() {
		return getStringValue("clusterMethod");
	}

	public ClusterMessage setClusterMethod(String value) {
		setPropertyValue("clusterMethod", value);
		return this;
	}
	public byte[] getMessageData() {
		return getByteArrayValue("messageData");
	}

	public ClusterMessage setMessageData(byte[] value) {
		setPropertyValue("messageData", value);
		return this;
	}
	public boolean getError() {
		return getBooleanValue("error");
	}

	public ClusterMessage setError(boolean value) {
		setPropertyValue("error", value);
		return this;
	}
	public String getErrorMessage() {
		return getStringValue("errorMessage");
	}

	public ClusterMessage setErrorMessage(String value) {
		setPropertyValue("errorMessage", value);
		return this;
	}


}