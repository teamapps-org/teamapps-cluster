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
