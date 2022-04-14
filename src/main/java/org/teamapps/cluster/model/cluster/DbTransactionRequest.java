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


public class DbTransactionRequest extends Message {
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public final static Function<byte[], DbTransactionRequest> DECODER_FUNCTION = bytes -> {
		try {
			return new DbTransactionRequest(bytes);
		} catch (IOException e) {
			LOGGER.error("Error creating DbTransactionRequest instance", e);
		}
		return null;
	};

	private final static MessageDecoder<DbTransactionRequest> decoder = (dis, fileProvider) -> {
		try {
			return new DbTransactionRequest(dis, fileProvider);
		} catch (IOException e) {
			LOGGER.error("Error creating DbTransactionRequest instance", e);
		}
		return null;
	};

	public static MessageDecoder<DbTransactionRequest> getMessageDecoder() {
		return decoder;
	}

    public final static int ROOT_FIELD_ID = 101041;

	public DbTransactionRequest() {
		super(ClusterSchemaRegistry.SCHEMA.getFieldById(101041), new ArrayList<>());
	}

	public DbTransactionRequest(ByteBuffer buf) {
		super(buf, ClusterSchemaRegistry.SCHEMA);
	}

	public DbTransactionRequest(DataInputStream dis) throws IOException {
		super(dis, ClusterSchemaRegistry.SCHEMA);
	}

	public DbTransactionRequest(DataInputStream dis, FileProvider fileProvider) throws IOException {
		super(dis, ClusterSchemaRegistry.SCHEMA, fileProvider, ClusterSchemaRegistry.REGISTRY);
	}

	public DbTransactionRequest(byte[] bytes) throws IOException {
		super(bytes, ClusterSchemaRegistry.SCHEMA);
	}

	public DbTransactionRequest(byte[] bytes, FileProvider fileProvider) throws IOException {
		super(bytes, ClusterSchemaRegistry.SCHEMA, fileProvider, ClusterSchemaRegistry.REGISTRY);
	}

	public byte[] getBytes() {
		return getByteArrayValue("bytes");
	}

	public DbTransactionRequest setBytes(byte[] value) {
		setPropertyValue("bytes", value);
		return this;
	}


}
