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



		System.out.println(schema);
		PojoBuilder.createPojos(schema, new File("./src/main/java"));
	}
}
