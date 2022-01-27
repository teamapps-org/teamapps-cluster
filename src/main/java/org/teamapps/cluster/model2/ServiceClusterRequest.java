package org.teamapps.cluster.model2;

import org.teamapps.cluster.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Function;


public class ServiceClusterRequest extends Message {
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public final static Function<byte[], ServiceClusterRequest> DECODER_FUNCTION = bytes -> {
		try {
			return new ServiceClusterRequest(bytes);
		} catch (IOException e) {
			LOGGER.error("Error creating ServiceClusterRequest instance", e);
		}
		return null;
	};

	private final static MessageDecoder<ServiceClusterRequest> decoder = (dis, fileProvider) -> {
		try {
			return new ServiceClusterRequest(dis, fileProvider);
		} catch (IOException e) {
			LOGGER.error("Error creating ServiceClusterRequest instance", e);
		}
		return null;
	};

	public static MessageDecoder<ServiceClusterRequest> getMessageDecoder() {
		return decoder;
	}

    public final static int ROOT_FIELD_ID = 101011;

	public ServiceClusterRequest() {
		super(Schema2.SCHEMA.getFieldById(101011), new ArrayList<>());
	}

	public ServiceClusterRequest(ByteBuffer buf) {
		super(buf, Schema2.SCHEMA);
	}

	public ServiceClusterRequest(DataInputStream dis) throws IOException {
		super(dis, Schema2.SCHEMA);
	}

	public ServiceClusterRequest(DataInputStream dis, FileProvider fileProvider) throws IOException {
		super(dis, Schema2.SCHEMA, fileProvider, Schema2.REGISTRY);
	}

	public ServiceClusterRequest(byte[] bytes) throws IOException {
		super(bytes, Schema2.SCHEMA);
	}

	public ServiceClusterRequest(byte[] bytes, FileProvider fileProvider) throws IOException {
		super(bytes, Schema2.SCHEMA, fileProvider, Schema2.REGISTRY);
	}

	public long getRequestId() {
		return getLongValue("requestId");
	}

	public ServiceClusterRequest setRequestId(long value) {
		setPropertyValue("requestId", value);
		return this;
	}
	public String getServiceName() {
		return getStringValue("serviceName");
	}

	public ServiceClusterRequest setServiceName(String value) {
		setPropertyValue("serviceName", value);
		return this;
	}
	public String getMethod() {
		return getStringValue("method");
	}

	public ServiceClusterRequest setMethod(String value) {
		setPropertyValue("method", value);
		return this;
	}
	public byte[] getRequestData() {
		return getByteArrayValue("requestData");
	}

	public ServiceClusterRequest setRequestData(byte[] value) {
		setPropertyValue("requestData", value);
		return this;
	}


}