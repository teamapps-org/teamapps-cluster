package org.teamapps.cluster.model.cluster;

import org.teamapps.cluster.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Function;


public class KeepAliveMessage extends Message {
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public final static Function<byte[], KeepAliveMessage> DECODER_FUNCTION = bytes -> {
		try {
			return new KeepAliveMessage(bytes);
		} catch (IOException e) {
			LOGGER.error("Error creating KeepAliveMessage instance", e);
		}
		return null;
	};

	private final static MessageDecoder<KeepAliveMessage> decoder = (dis, fileProvider) -> {
		try {
			return new KeepAliveMessage(dis, fileProvider);
		} catch (IOException e) {
			LOGGER.error("Error creating KeepAliveMessage instance", e);
		}
		return null;
	};

	public static MessageDecoder<KeepAliveMessage> getMessageDecoder() {
		return decoder;
	}

    public final static int ROOT_FIELD_ID = 101030;

	public KeepAliveMessage() {
		super(ClusterSchemaRegistry.SCHEMA.getFieldById(101030), new ArrayList<>());
	}

	public KeepAliveMessage(ByteBuffer buf) {
		super(buf, ClusterSchemaRegistry.SCHEMA);
	}

	public KeepAliveMessage(DataInputStream dis) throws IOException {
		super(dis, ClusterSchemaRegistry.SCHEMA);
	}

	public KeepAliveMessage(DataInputStream dis, FileProvider fileProvider) throws IOException {
		super(dis, ClusterSchemaRegistry.SCHEMA, fileProvider, ClusterSchemaRegistry.REGISTRY);
	}

	public KeepAliveMessage(byte[] bytes) throws IOException {
		super(bytes, ClusterSchemaRegistry.SCHEMA);
	}

	public KeepAliveMessage(byte[] bytes, FileProvider fileProvider) throws IOException {
		super(bytes, ClusterSchemaRegistry.SCHEMA, fileProvider, ClusterSchemaRegistry.REGISTRY);
	}



}