package org.teamapps.cluster.model.cluster;

import org.teamapps.cluster.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Function;


public class ServiceClusterResponse extends Message {
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public final static Function<byte[], ServiceClusterResponse> DECODER_FUNCTION = bytes -> {
		try {
			return new ServiceClusterResponse(bytes);
		} catch (IOException e) {
			LOGGER.error("Error creating ServiceClusterResponse instance", e);
		}
		return null;
	};

	private final static MessageDecoder<ServiceClusterResponse> decoder = (dis, fileProvider) -> {
		try {
			return new ServiceClusterResponse(dis, fileProvider);
		} catch (IOException e) {
			LOGGER.error("Error creating ServiceClusterResponse instance", e);
		}
		return null;
	};

	public static MessageDecoder<ServiceClusterResponse> getMessageDecoder() {
		return decoder;
	}

    public final static int ROOT_FIELD_ID = 101023;

	public ServiceClusterResponse() {
		super(ClusterSchemaRegistry.SCHEMA.getFieldById(101023), new ArrayList<>());
	}

	public ServiceClusterResponse(ByteBuffer buf) {
		super(buf, ClusterSchemaRegistry.SCHEMA);
	}

	public ServiceClusterResponse(DataInputStream dis) throws IOException {
		super(dis, ClusterSchemaRegistry.SCHEMA);
	}

	public ServiceClusterResponse(DataInputStream dis, FileProvider fileProvider) throws IOException {
		super(dis, ClusterSchemaRegistry.SCHEMA, fileProvider, ClusterSchemaRegistry.REGISTRY);
	}

	public ServiceClusterResponse(byte[] bytes) throws IOException {
		super(bytes, ClusterSchemaRegistry.SCHEMA);
	}

	public ServiceClusterResponse(byte[] bytes, FileProvider fileProvider) throws IOException {
		super(bytes, ClusterSchemaRegistry.SCHEMA, fileProvider, ClusterSchemaRegistry.REGISTRY);
	}

	public long getRequestId() {
		return getLongValue("requestId");
	}

	public ServiceClusterResponse setRequestId(long value) {
		setPropertyValue("requestId", value);
		return this;
	}
	public byte[] getResponseData() {
		return getByteArrayValue("responseData");
	}

	public ServiceClusterResponse setResponseData(byte[] value) {
		setPropertyValue("responseData", value);
		return this;
	}
	public boolean getError() {
		return getBooleanValue("error");
	}

	public ServiceClusterResponse setError(boolean value) {
		setPropertyValue("error", value);
		return this;
	}
	public String getErrorMessage() {
		return getStringValue("errorMessage");
	}

	public ServiceClusterResponse setErrorMessage(String value) {
		setPropertyValue("errorMessage", value);
		return this;
	}


}