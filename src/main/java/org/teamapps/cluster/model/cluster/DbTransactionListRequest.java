package org.teamapps.cluster.model.cluster;

import org.teamapps.cluster.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Function;


public class DbTransactionListRequest extends Message {
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public final static Function<byte[], DbTransactionListRequest> DECODER_FUNCTION = bytes -> {
		try {
			return new DbTransactionListRequest(bytes);
		} catch (IOException e) {
			LOGGER.error("Error creating DbTransactionListRequest instance", e);
		}
		return null;
	};

	private final static MessageDecoder<DbTransactionListRequest> decoder = (dis, fileProvider) -> {
		try {
			return new DbTransactionListRequest(dis, fileProvider);
		} catch (IOException e) {
			LOGGER.error("Error creating DbTransactionListRequest instance", e);
		}
		return null;
	};

	public static MessageDecoder<DbTransactionListRequest> getMessageDecoder() {
		return decoder;
	}

    public final static int ROOT_FIELD_ID = 101043;

	public DbTransactionListRequest() {
		super(ClusterSchemaRegistry.SCHEMA.getFieldById(101043), new ArrayList<>());
	}

	public DbTransactionListRequest(ByteBuffer buf) {
		super(buf, ClusterSchemaRegistry.SCHEMA);
	}

	public DbTransactionListRequest(DataInputStream dis) throws IOException {
		super(dis, ClusterSchemaRegistry.SCHEMA);
	}

	public DbTransactionListRequest(DataInputStream dis, FileProvider fileProvider) throws IOException {
		super(dis, ClusterSchemaRegistry.SCHEMA, fileProvider, ClusterSchemaRegistry.REGISTRY);
	}

	public DbTransactionListRequest(byte[] bytes) throws IOException {
		super(bytes, ClusterSchemaRegistry.SCHEMA);
	}

	public DbTransactionListRequest(byte[] bytes, FileProvider fileProvider) throws IOException {
		super(bytes, ClusterSchemaRegistry.SCHEMA, fileProvider, ClusterSchemaRegistry.REGISTRY);
	}

	public long getLastKnownTransactionId() {
		return getLongValue("lastKnownTransactionId");
	}

	public DbTransactionListRequest setLastKnownTransactionId(long value) {
		setPropertyValue("lastKnownTransactionId", value);
		return this;
	}


}