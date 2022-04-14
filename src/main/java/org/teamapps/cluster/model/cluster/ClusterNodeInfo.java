/*-
 * ========================LICENSE_START=================================
 * TeamApps Cluster
 * ---
 * Copyright (C) 2021 - 2022 TeamApps.org
 * ---
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
package org.teamapps.cluster.model.cluster;

import org.teamapps.cluster.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Function;


public class ClusterNodeInfo extends Message {
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public final static Function<byte[], ClusterNodeInfo> DECODER_FUNCTION = bytes -> {
		try {
			return new ClusterNodeInfo(bytes);
		} catch (IOException e) {
			LOGGER.error("Error creating ClusterNodeInfo instance", e);
		}
		return null;
	};

	private final static MessageDecoder<ClusterNodeInfo> decoder = (dis, fileProvider) -> {
		try {
			return new ClusterNodeInfo(dis, fileProvider);
		} catch (IOException e) {
			LOGGER.error("Error creating ClusterNodeInfo instance", e);
		}
		return null;
	};

	public static MessageDecoder<ClusterNodeInfo> getMessageDecoder() {
		return decoder;
	}

    public final static int ROOT_FIELD_ID = 101013;

	public ClusterNodeInfo() {
		super(ClusterSchemaRegistry.SCHEMA.getFieldById(101013), new ArrayList<>());
	}

	public ClusterNodeInfo(ByteBuffer buf) {
		super(buf, ClusterSchemaRegistry.SCHEMA);
	}

	public ClusterNodeInfo(DataInputStream dis) throws IOException {
		super(dis, ClusterSchemaRegistry.SCHEMA);
	}

	public ClusterNodeInfo(DataInputStream dis, FileProvider fileProvider) throws IOException {
		super(dis, ClusterSchemaRegistry.SCHEMA, fileProvider, ClusterSchemaRegistry.REGISTRY);
	}

	public ClusterNodeInfo(byte[] bytes) throws IOException {
		super(bytes, ClusterSchemaRegistry.SCHEMA);
	}

	public ClusterNodeInfo(byte[] bytes, FileProvider fileProvider) throws IOException {
		super(bytes, ClusterSchemaRegistry.SCHEMA, fileProvider, ClusterSchemaRegistry.REGISTRY);
	}

	public boolean getResponse() {
		return getBooleanValue("response");
	}

	public ClusterNodeInfo setResponse(boolean value) {
		setPropertyValue("response", value);
		return this;
	}
	public ClusterNodeData getLocalNode() {
		return getMessageObject("localNode");
	}

	public ClusterNodeInfo setLocalNode(ClusterNodeData value) {
		setPropertyValue("localNode", value);
		return this;
	}
	public List<ClusterNodeData> getKnownRemoteNodes() {
		return getMessageList("knownRemoteNodes");
	}

	public ClusterNodeInfo setKnownRemoteNodes(List<ClusterNodeData> value) {
		setPropertyValue("knownRemoteNodes", value);
		return this;
	}

	public ClusterNodeInfo addKnownRemoteNodes(ClusterNodeData value) {
		addMultiReference("knownRemoteNodes", value);
		return this;
	}
	public List<ClusterTopicInfo> getClusterTopics() {
		return getMessageList("clusterTopics");
	}

	public ClusterNodeInfo setClusterTopics(List<ClusterTopicInfo> value) {
		setPropertyValue("clusterTopics", value);
		return this;
	}

	public ClusterNodeInfo addClusterTopics(ClusterTopicInfo value) {
		addMultiReference("clusterTopics", value);
		return this;
	}


}
