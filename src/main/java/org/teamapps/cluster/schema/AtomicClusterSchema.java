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
package org.teamapps.cluster.schema;

import org.teamapps.cluster.dto.MessageField;
import org.teamapps.cluster.dto.MessageSchema;
import org.teamapps.cluster.dto.PojoBuilder;

import java.io.File;
import java.io.IOException;

public class AtomicClusterSchema {

	public static void main(String[] args) throws IOException {
		createSchema();
	}

	private static void createSchema() throws IOException {
		MessageSchema schema = new MessageSchema(100, "atomixSchemaRegistry", "org.teamapps.cluster.model.atomix");

		MessageField fileTransfer = schema.addObject("fileTransfer");
		schema.addTextField(fileTransfer, "fileId");
		schema.addLongField(fileTransfer, "length");
		schema.addByteArrayField(fileTransfer, "data");
		schema.addBooleanField(fileTransfer, "finished");

		MessageField fileTransferResponse = schema.addObject("fileTransferResponse");
		schema.addLongField(fileTransferResponse, "receivedData");
		schema.addBooleanField(fileTransferResponse, "finished");

		MessageField clusterMessage = schema.addObject("clusterMessage");
		schema.addTextField(clusterMessage, "memberId");
		schema.addTextField(clusterMessage, "clusterService");
		schema.addTextField(clusterMessage, "clusterMethod");
		schema.addByteArrayField(clusterMessage, "messageData");
		schema.addBooleanField(clusterMessage, "error");
		schema.addTextField(clusterMessage, "errorMessage");

		System.out.println(schema);
		PojoBuilder.createPojos(schema, new File("./src/main/java"));
	}
}
