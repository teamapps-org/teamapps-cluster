package org.teamapps.cluster.model.cluster;

import org.teamapps.cluster.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Function;


public class ClusterTopicInfo extends Message {
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public final static Function<byte[], ClusterTopicInfo> DECODER_FUNCTION = bytes -> {
		try {
			return new ClusterTopicInfo(bytes);
		} catch (IOException e) {
			LOGGER.error("Error creating ClusterTopicInfo instance", e);
		}
		return null;
	};

	private final static MessageDecoder<ClusterTopicInfo> decoder = (dis, fileProvider) -> {
		try {
			return new ClusterTopicInfo(dis, fileProvider);
		} catch (IOException e) {
			LOGGER.error("Error creating ClusterTopicInfo instance", e);
		}
		return null;
	};

	public static MessageDecoder<ClusterTopicInfo> getMessageDecoder() {
		return decoder;
	}

    public final static int ROOT_FIELD_ID = 101007;

	public ClusterTopicInfo() {
		super(ClusterSchemaRegistry.SCHEMA.getFieldById(101007), new ArrayList<>());
	}

	public ClusterTopicInfo(ByteBuffer buf) {
		super(buf, ClusterSchemaRegistry.SCHEMA);
	}

	public ClusterTopicInfo(DataInputStream dis) throws IOException {
		super(dis, ClusterSchemaRegistry.SCHEMA);
	}

	public ClusterTopicInfo(DataInputStream dis, FileProvider fileProvider) throws IOException {
		super(dis, ClusterSchemaRegistry.SCHEMA, fileProvider, ClusterSchemaRegistry.REGISTRY);
	}

	public ClusterTopicInfo(byte[] bytes) throws IOException {
		super(bytes, ClusterSchemaRegistry.SCHEMA);
	}

	public ClusterTopicInfo(byte[] bytes, FileProvider fileProvider) throws IOException {
		super(bytes, ClusterSchemaRegistry.SCHEMA, fileProvider, ClusterSchemaRegistry.REGISTRY);
	}

	public String getTopicName() {
		return getStringValue("topicName");
	}

	public ClusterTopicInfo setTopicName(String value) {
		setPropertyValue("topicName", value);
		return this;
	}
	public String[] getNodeIds() {
		return getStringArrayValue("nodeIds");
	}

	public ClusterTopicInfo setNodeIds(String[] value) {
		setPropertyValue("nodeIds", value);
		return this;
	}
	public List<String> getNodeIdsAsList() {
		String[] arrayValue = getStringArrayValue("nodeIds");
		return arrayValue != null ? Arrays.asList(arrayValue) : Collections.emptyList();
	}

	public ClusterTopicInfo setNodeIds(List<String> value) {
		setPropertyValue("nodeIds", value != null ? value.toArray(String[]::new) : null);
		return this;
	}


}