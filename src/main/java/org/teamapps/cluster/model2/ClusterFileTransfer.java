package org.teamapps.cluster.model2;

import org.teamapps.cluster.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Function;


public class ClusterFileTransfer extends Message {
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public final static Function<byte[], ClusterFileTransfer> DECODER_FUNCTION = bytes -> {
		try {
			return new ClusterFileTransfer(bytes);
		} catch (IOException e) {
			LOGGER.error("Error creating ClusterFileTransfer instance", e);
		}
		return null;
	};

	private final static MessageDecoder<ClusterFileTransfer> decoder = (dis, fileProvider) -> {
		try {
			return new ClusterFileTransfer(dis, fileProvider);
		} catch (IOException e) {
			LOGGER.error("Error creating ClusterFileTransfer instance", e);
		}
		return null;
	};

	public static MessageDecoder<ClusterFileTransfer> getMessageDecoder() {
		return decoder;
	}

    public final static int ROOT_FIELD_ID = 101021;

	public ClusterFileTransfer() {
		super(Schema2.SCHEMA.getFieldById(101021), new ArrayList<>());
	}

	public ClusterFileTransfer(ByteBuffer buf) {
		super(buf, Schema2.SCHEMA);
	}

	public ClusterFileTransfer(DataInputStream dis) throws IOException {
		super(dis, Schema2.SCHEMA);
	}

	public ClusterFileTransfer(DataInputStream dis, FileProvider fileProvider) throws IOException {
		super(dis, Schema2.SCHEMA, fileProvider, Schema2.REGISTRY);
	}

	public ClusterFileTransfer(byte[] bytes) throws IOException {
		super(bytes, Schema2.SCHEMA);
	}

	public ClusterFileTransfer(byte[] bytes, FileProvider fileProvider) throws IOException {
		super(bytes, Schema2.SCHEMA, fileProvider, Schema2.REGISTRY);
	}

	public String getFileId() {
		return getStringValue("fileId");
	}

	public ClusterFileTransfer setFileId(String value) {
		setPropertyValue("fileId", value);
		return this;
	}
	public long getLength() {
		return getLongValue("length");
	}

	public ClusterFileTransfer setLength(long value) {
		setPropertyValue("length", value);
		return this;
	}
	public byte[] getData() {
		return getByteArrayValue("data");
	}

	public ClusterFileTransfer setData(byte[] value) {
		setPropertyValue("data", value);
		return this;
	}
	public boolean getInitialMessage() {
		return getBooleanValue("initialMessage");
	}

	public ClusterFileTransfer setInitialMessage(boolean value) {
		setPropertyValue("initialMessage", value);
		return this;
	}
	public boolean getLastMessage() {
		return getBooleanValue("lastMessage");
	}

	public ClusterFileTransfer setLastMessage(boolean value) {
		setPropertyValue("lastMessage", value);
		return this;
	}


}