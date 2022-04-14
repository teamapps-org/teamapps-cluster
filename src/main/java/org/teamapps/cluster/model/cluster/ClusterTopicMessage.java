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


public class ClusterTopicMessage extends Message {
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public final static Function<byte[], ClusterTopicMessage> DECODER_FUNCTION = bytes -> {
		try {
			return new ClusterTopicMessage(bytes);
		} catch (IOException e) {
			LOGGER.error("Error creating ClusterTopicMessage instance", e);
		}
		return null;
	};

	private final static MessageDecoder<ClusterTopicMessage> decoder = (dis, fileProvider) -> {
		try {
			return new ClusterTopicMessage(dis, fileProvider);
		} catch (IOException e) {
			LOGGER.error("Error creating ClusterTopicMessage instance", e);
		}
		return null;
	};

	public static MessageDecoder<ClusterTopicMessage> getMessageDecoder() {
		return decoder;
	}

    public final static int ROOT_FIELD_ID = 101010;

	public ClusterTopicMessage() {
		super(ClusterSchemaRegistry.SCHEMA.getFieldById(101010), new ArrayList<>());
	}

	public ClusterTopicMessage(ByteBuffer buf) {
		super(buf, ClusterSchemaRegistry.SCHEMA);
	}

	public ClusterTopicMessage(DataInputStream dis) throws IOException {
		super(dis, ClusterSchemaRegistry.SCHEMA);
	}

	public ClusterTopicMessage(DataInputStream dis, FileProvider fileProvider) throws IOException {
		super(dis, ClusterSchemaRegistry.SCHEMA, fileProvider, ClusterSchemaRegistry.REGISTRY);
	}

	public ClusterTopicMessage(byte[] bytes) throws IOException {
		super(bytes, ClusterSchemaRegistry.SCHEMA);
	}

	public ClusterTopicMessage(byte[] bytes, FileProvider fileProvider) throws IOException {
		super(bytes, ClusterSchemaRegistry.SCHEMA, fileProvider, ClusterSchemaRegistry.REGISTRY);
	}

	public String getTopic() {
		return getStringValue("topic");
	}

	public ClusterTopicMessage setTopic(String value) {
		setPropertyValue("topic", value);
		return this;
	}
	public byte[] getData() {
		return getByteArrayValue("data");
	}

	public ClusterTopicMessage setData(byte[] value) {
		setPropertyValue("data", value);
		return this;
	}


}
