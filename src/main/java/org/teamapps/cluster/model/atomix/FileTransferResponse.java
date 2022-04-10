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
package org.teamapps.cluster.model.atomix;

import org.teamapps.cluster.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Function;


public class FileTransferResponse extends Message {
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public final static Function<byte[], FileTransferResponse> DECODER_FUNCTION = bytes -> {
		try {
			return new FileTransferResponse(bytes);
		} catch (IOException e) {
			LOGGER.error("Error creating FileTransferResponse instance", e);
		}
		return null;
	};

	private final static MessageDecoder<FileTransferResponse> decoder = (dis, fileProvider) -> {
		try {
			return new FileTransferResponse(dis, fileProvider);
		} catch (IOException e) {
			LOGGER.error("Error creating FileTransferResponse instance", e);
		}
		return null;
	};

	public static MessageDecoder<FileTransferResponse> getMessageDecoder() {
		return decoder;
	}

    public final static int ROOT_FIELD_ID = 100006;

	public FileTransferResponse() {
		super(AtomixSchemaRegistry.SCHEMA.getFieldById(100006), new ArrayList<>());
	}

	public FileTransferResponse(ByteBuffer buf) {
		super(buf, AtomixSchemaRegistry.SCHEMA);
	}

	public FileTransferResponse(DataInputStream dis) throws IOException {
		super(dis, AtomixSchemaRegistry.SCHEMA);
	}

	public FileTransferResponse(DataInputStream dis, FileProvider fileProvider) throws IOException {
		super(dis, AtomixSchemaRegistry.SCHEMA, fileProvider, AtomixSchemaRegistry.REGISTRY);
	}

	public FileTransferResponse(byte[] bytes) throws IOException {
		super(bytes, AtomixSchemaRegistry.SCHEMA);
	}

	public FileTransferResponse(byte[] bytes, FileProvider fileProvider) throws IOException {
		super(bytes, AtomixSchemaRegistry.SCHEMA, fileProvider, AtomixSchemaRegistry.REGISTRY);
	}

	public long getReceivedData() {
		return getLongValue("receivedData");
	}

	public FileTransferResponse setReceivedData(long value) {
		setPropertyValue("receivedData", value);
		return this;
	}
	public boolean getFinished() {
		return getBooleanValue("finished");
	}

	public FileTransferResponse setFinished(boolean value) {
		setPropertyValue("finished", value);
		return this;
	}


}
