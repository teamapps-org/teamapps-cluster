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


public class DbTransactionList extends Message {
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public final static Function<byte[], DbTransactionList> DECODER_FUNCTION = bytes -> {
		try {
			return new DbTransactionList(bytes);
		} catch (IOException e) {
			LOGGER.error("Error creating DbTransactionList instance", e);
		}
		return null;
	};

	private final static MessageDecoder<DbTransactionList> decoder = (dis, fileProvider) -> {
		try {
			return new DbTransactionList(dis, fileProvider);
		} catch (IOException e) {
			LOGGER.error("Error creating DbTransactionList instance", e);
		}
		return null;
	};

	public static MessageDecoder<DbTransactionList> getMessageDecoder() {
		return decoder;
	}

    public final static int ROOT_FIELD_ID = 101045;

	public DbTransactionList() {
		super(ClusterSchemaRegistry.SCHEMA.getFieldById(101045), new ArrayList<>());
	}

	public DbTransactionList(ByteBuffer buf) {
		super(buf, ClusterSchemaRegistry.SCHEMA);
	}

	public DbTransactionList(DataInputStream dis) throws IOException {
		super(dis, ClusterSchemaRegistry.SCHEMA);
	}

	public DbTransactionList(DataInputStream dis, FileProvider fileProvider) throws IOException {
		super(dis, ClusterSchemaRegistry.SCHEMA, fileProvider, ClusterSchemaRegistry.REGISTRY);
	}

	public DbTransactionList(byte[] bytes) throws IOException {
		super(bytes, ClusterSchemaRegistry.SCHEMA);
	}

	public DbTransactionList(byte[] bytes, FileProvider fileProvider) throws IOException {
		super(bytes, ClusterSchemaRegistry.SCHEMA, fileProvider, ClusterSchemaRegistry.REGISTRY);
	}

	public long getLastKnownTransactionId() {
		return getLongValue("lastKnownTransactionId");
	}

	public DbTransactionList setLastKnownTransactionId(long value) {
		setPropertyValue("lastKnownTransactionId", value);
		return this;
	}
	public long getTransactionCount() {
		return getLongValue("transactionCount");
	}

	public DbTransactionList setTransactionCount(long value) {
		setPropertyValue("transactionCount", value);
		return this;
	}
	public File getTransactionsFile() {
		return getFileValue("transactionsFile");
	}

	public DbTransactionList setTransactionsFile(File value) {
		setPropertyValue("transactionsFile", value);
		return this;
	}


}
