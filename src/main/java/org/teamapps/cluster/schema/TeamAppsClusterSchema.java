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

public class TeamAppsClusterSchema {

	public static void main(String[] args) throws IOException {
		createSchema();
	}

	private static void createSchema() throws IOException {
		MessageSchema schema = new MessageSchema(101, "clusterSchemaRegistry", "org.teamapps.cluster.model.cluster");

		MessageField clusterNodeData = schema.addObject("clusterNodeData");
		schema.addTextField(clusterNodeData, "nodeId");
		schema.addTextField(clusterNodeData, "host");
		schema.addIntField(clusterNodeData, "port");
		schema.addBooleanField(clusterNodeData,  "response");
		schema.addStringArrayField(clusterNodeData, "availableServices");

		MessageField clusterNodeInfo = schema.addObject("clusterNodeInfo");
		schema.addBooleanField(clusterNodeInfo, "response");
		schema.addSingleReference(clusterNodeInfo, clusterNodeData, "localNode");
		schema.addMultiReference(clusterNodeInfo, clusterNodeData, "knownRemoteNodes");


		MessageField clusterRequest = schema.addObject("serviceClusterRequest");
		schema.addLongField(clusterRequest, "requestId");
		schema.addTextField(clusterRequest, "serviceName");
		schema.addTextField(clusterRequest, "method");
		schema.addByteArrayField(clusterRequest, "requestData");

		MessageField clusterResponse = schema.addObject("serviceClusterResponse");
		schema.addLongField(clusterResponse, "requestId");
		schema.addByteArrayField(clusterResponse, "responseData");
		schema.addBooleanField(clusterResponse, "error");
		schema.addTextField(clusterResponse, "errorMessage");


		MessageField clusterFileTransfer = schema.addObject("clusterFileTransfer");
		schema.addTextField(clusterFileTransfer, "fileId");
		schema.addLongField(clusterFileTransfer, "length");
		schema.addByteArrayField(clusterFileTransfer, "data");
		schema.addBooleanField(clusterFileTransfer, "initialMessage");
		schema.addBooleanField(clusterFileTransfer, "lastMessage");

		MessageField clusterFileTransferResponse = schema.addObject("clusterFileTransferResponse");
		schema.addTextField(clusterFileTransferResponse, "fileId");
		schema.addLongField(clusterFileTransferResponse, "receivedData");

		MessageField keepAliveMessage = schema.addObject("keepAliveMessage");

		System.out.println(schema);
		PojoBuilder.createPojos(schema, new File("./src/main/java"));
	}
}
