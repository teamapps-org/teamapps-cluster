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

    public final static int ROOT_FIELD_ID = 101028;

	public ClusterFileTransfer() {
		super(ClusterSchemaRegistry.SCHEMA.getFieldById(101028), new ArrayList<>());
	}

	public ClusterFileTransfer(ByteBuffer buf) {
		super(buf, ClusterSchemaRegistry.SCHEMA);
	}

	public ClusterFileTransfer(DataInputStream dis) throws IOException {
		super(dis, ClusterSchemaRegistry.SCHEMA);
	}

	public ClusterFileTransfer(DataInputStream dis, FileProvider fileProvider) throws IOException {
		super(dis, ClusterSchemaRegistry.SCHEMA, fileProvider, ClusterSchemaRegistry.REGISTRY);
	}

	public ClusterFileTransfer(byte[] bytes) throws IOException {
		super(bytes, ClusterSchemaRegistry.SCHEMA);
	}

	public ClusterFileTransfer(byte[] bytes, FileProvider fileProvider) throws IOException {
		super(bytes, ClusterSchemaRegistry.SCHEMA, fileProvider, ClusterSchemaRegistry.REGISTRY);
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
	public int getMessageIndex() {
		return getIntValue("messageIndex");
	}

	public ClusterFileTransfer setMessageIndex(int value) {
		setPropertyValue("messageIndex", value);
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
