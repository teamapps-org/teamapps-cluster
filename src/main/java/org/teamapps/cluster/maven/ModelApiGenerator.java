package org.teamapps.cluster.maven;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamapps.cluster.dto.MessageModelSchemaProvider;
import org.teamapps.cluster.dto.MessageSchema;
import org.teamapps.cluster.dto.PojoBuilder;

import java.io.File;
import java.lang.invoke.MethodHandles;

public class ModelApiGenerator {
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());


	public static void main(String[] args) throws Exception{
		if (args == null || args.length < 2) {
			LOGGER.error("Error: missing argument(s). Mandatory arguments are: modelClassName targetPath");
			System.exit(1);
		}
		String schemaClassName = args[0];
		String targetPath = args[1];

		Class<?> schemaClass = Class.forName(schemaClassName);
		MessageModelSchemaProvider schemaInfoProvider = (MessageModelSchemaProvider) schemaClass.getConstructor().newInstance();
		MessageSchema schema = schemaInfoProvider.getSchema();

		File basePath = new File(targetPath);
		if (!basePath.getParentFile().exists() && basePath.getParentFile().getParentFile().exists()) {
			basePath.getParentFile().mkdir();
		}
		basePath.mkdir();
		System.out.println("Path: " + basePath.getPath());
		System.out.println("Exists:" + basePath.exists());
		PojoBuilder.createPojos(schema, new File(targetPath));
	}
}
