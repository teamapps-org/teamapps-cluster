package org.teamapps.cluster.model2;

import org.teamapps.cluster.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Function;


public class ClusterNodeData extends Message {
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public final static Function<byte[], ClusterNodeData> DECODER_FUNCTION = bytes -> {
		try {
			return new ClusterNodeData(bytes);
		} catch (IOException e) {
			LOGGER.error("Error creating ClusterNodeData instance", e);
		}
		return null;
	};

	private final static MessageDecoder<ClusterNodeData> decoder = (dis, fileProvider) -> {
		try {
			return new ClusterNodeData(dis, fileProvider);
		} catch (IOException e) {
			LOGGER.error("Error creating ClusterNodeData instance", e);
		}
		return null;
	};

	public static MessageDecoder<ClusterNodeData> getMessageDecoder() {
		return decoder;
	}

    public final static int ROOT_FIELD_ID = 101001;

	public ClusterNodeData() {
		super(Schema2.SCHEMA.getFieldById(101001), new ArrayList<>());
	}

	public ClusterNodeData(ByteBuffer buf) {
		super(buf, Schema2.SCHEMA);
	}

	public ClusterNodeData(DataInputStream dis) throws IOException {
		super(dis, Schema2.SCHEMA);
	}

	public ClusterNodeData(DataInputStream dis, FileProvider fileProvider) throws IOException {
		super(dis, Schema2.SCHEMA, fileProvider, Schema2.REGISTRY);
	}

	public ClusterNodeData(byte[] bytes) throws IOException {
		super(bytes, Schema2.SCHEMA);
	}

	public ClusterNodeData(byte[] bytes, FileProvider fileProvider) throws IOException {
		super(bytes, Schema2.SCHEMA, fileProvider, Schema2.REGISTRY);
	}

	public String getNodeId() {
		return getStringValue("nodeId");
	}

	public ClusterNodeData setNodeId(String value) {
		setPropertyValue("nodeId", value);
		return this;
	}
	public String getHost() {
		return getStringValue("host");
	}

	public ClusterNodeData setHost(String value) {
		setPropertyValue("host", value);
		return this;
	}
	public int getPort() {
		return getIntValue("port");
	}

	public ClusterNodeData setPort(int value) {
		setPropertyValue("port", value);
		return this;
	}
	public boolean getResponse() {
		return getBooleanValue("response");
	}

	public ClusterNodeData setResponse(boolean value) {
		setPropertyValue("response", value);
		return this;
	}
	public String[] getAvailableServices() {
		return getStringArrayValue("availableServices");
	}

	public ClusterNodeData setAvailableServices(String[] value) {
		setPropertyValue("availableServices", value);
		return this;
	}


}