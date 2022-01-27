package org.teamapps.cluster.model;

import org.teamapps.cluster.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Function;


public class FileTransferResponse extends Message {
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public final static Function<byte[], FileTransferResponse> DECODER_FUNCTION = bytes -> {
		try {
			return new FileTransferResponse(bytes);
		} catch (IOException e) {
			LOGGER.error("Error creating FileTransferResponse instance", e);
		}
		return null;
	};

	private final static MessageDecoder<FileTransferResponse> decoder = (dis, fileProvider) -> {
		try {
			return new FileTransferResponse(dis, fileProvider);
		} catch (IOException e) {
			LOGGER.error("Error creating FileTransferResponse instance", e);
		}
		return null;
	};

	public static MessageDecoder<FileTransferResponse> getMessageDecoder() {
		return decoder;
	}

    public final static int ROOT_FIELD_ID = 100006;

	public FileTransferResponse() {
		super(Schema.SCHEMA.getFieldById(100006), new ArrayList<>());
	}

	public FileTransferResponse(ByteBuffer buf) {
		super(buf, Schema.SCHEMA);
	}

	public FileTransferResponse(DataInputStream dis) throws IOException {
		super(dis, Schema.SCHEMA);
	}

	public FileTransferResponse(DataInputStream dis, FileProvider fileProvider) throws IOException {
		super(dis, Schema.SCHEMA, fileProvider, Schema.REGISTRY);
	}

	public FileTransferResponse(byte[] bytes) throws IOException {
		super(bytes, Schema.SCHEMA);
	}

	public FileTransferResponse(byte[] bytes, FileProvider fileProvider) throws IOException {
		super(bytes, Schema.SCHEMA, fileProvider, Schema.REGISTRY);
	}

	public long getReceivedData() {
		return getLongValue("receivedData");
	}

	public FileTransferResponse setReceivedData(long value) {
		setPropertyValue("receivedData", value);
		return this;
	}
	public boolean getFinished() {
		return getBooleanValue("finished");
	}

	public FileTransferResponse setFinished(boolean value) {
		setPropertyValue("finished", value);
		return this;
	}


}