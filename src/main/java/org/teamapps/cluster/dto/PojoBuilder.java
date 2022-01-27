package org.teamapps.cluster.dto;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class PojoBuilder {

	public static void createPojos(MessageSchema schema, File directory) throws IOException {
		File dir = directory;
		String namespace = schema.getNamespace();
		for (String name : namespace.split("\\.")) {
			dir = new File(dir, name);
			dir.mkdir();
		}
		System.out.println("Create source in path: " + dir.getPath());
		File f = dir;
		createServiceClasses(schema, dir);
		createSchemaPojo(schema, dir);
		schema.getFields().stream()
				.filter(field -> field.getType() == MessageFieldType.OBJECT)
				.forEach(field -> createMessagePojoSave(schema, field, f));
	}

	private static void createServiceClasses(MessageSchema schema, File directory) throws IOException {
		for (ServiceSchema serviceSchema : schema.getServiceSchemas()) {
			String tpl = readTemplate("service.tpl");
			tpl = setValue(tpl, "package", schema.getNamespace());
			String type = "Abstract" + firstUpperCase(serviceSchema.getServiceName());
			tpl = setValue(tpl, "type", type);
			tpl = setValue(tpl, "serviceName", serviceSchema.getServiceName());

			StringBuilder data = new StringBuilder();
			StringBuilder cases = new StringBuilder();
			for (ServiceMethod method : serviceSchema.getServiceMethods()) {
				data.append(getTabs(1)).append("public abstract ")
						.append(firstUpperCase(method.getOutputMessage().getName()))
						.append(" ").append(method.getMethodName()).append("(").append(firstUpperCase(method.getInputMessage().getName())).append(" value);\n\n");
				cases.append(getTabs(3)).append("case \"").append(method.getMethodName()).append("\" -> {\n");
				cases.append(getTabs(4)).append("return ").append(method.getMethodName()).append("( new ").append(firstUpperCase(method.getInputMessage().getName())).append("(bytes, fileProvider)).toBytes(fileSink);\n");
				cases.append(getTabs(3)).append("}\n");
			}
			tpl = setValue(tpl, "methods", data.toString());
			tpl = setValue(tpl, "cases", cases.toString());

			File file = new File(directory, type + ".java");
			Files.writeString(file.toPath(), tpl);

			type = firstUpperCase(serviceSchema.getServiceName()) + "Client";
			tpl = readTemplate("serviceClient.tpl");
			tpl = setValue(tpl, "package", schema.getNamespace());
			tpl = setValue(tpl, "type", type);
			tpl = setValue(tpl, "serviceName", serviceSchema.getServiceName());

			data = new StringBuilder();
			for (ServiceMethod method : serviceSchema.getServiceMethods()) {
				data.append(getTabs(1)).append("public Mono<").append(firstUpperCase(method.getOutputMessage().getName()))
						.append("> ").append(method.getMethodName()).append("(").append(firstUpperCase(method.getInputMessage().getName())).append(" value) {\n");
				data.append(getTabs(2)).append("return createClusterTask(\"").append(method.getMethodName()).append("\", value, ").append(firstUpperCase(method.getOutputMessage().getName())).append(".getMessageDecoder());\n");
				data.append(getTabs(1)).append("}\n\n");
			}
			tpl = setValue(tpl, "methods", data.toString());

			file = new File(directory, type + ".java");
			Files.writeString(file.toPath(), tpl);
		}

	}

	private static void createSchemaPojo(MessageSchema schema, File directory) throws IOException {
		String tpl = readTemplate("schema.tpl");
		tpl = setValue(tpl, "package", schema.getNamespace());
		tpl = setValue(tpl, "type", firstUpperCase(schema.getName()));
		tpl = setValue(tpl, "id", "" + schema.getSchemaId());
		tpl = setValue(tpl, "name", schema.getName());

		StringBuilder data = new StringBuilder();
		StringBuilder registry = new StringBuilder();

		for (MessageField field : schema.getFields()) {
			int id = field.getId() - schema.getSchemaIdPrefix();
			int parentId = (field.getParentFieldId() - schema.getSchemaIdPrefix());
			int referenceId = field.getReferencedFieldId() - schema.getSchemaIdPrefix();
			String title = field.getTitle() != null ? "\"" + field.getTitle() + "\"" : "null";
			if (field.isObject()) {
				data.append(getTabs(2)).append("MessageField f").append(id).append(" = SCHEMA.addObject(" + id + ", \"" + field.getName() + "\", " + title + ");\n");
				registry.append(getTabs(2)).append("DECODERS.put(").append(field.getId()).append(", ").append(firstUpperCase(field.getName())).append(".getMessageDecoder());\n");
			} else if (field.isSingleReference()) {
				data.append(getTabs(2)).append("SCHEMA.addSingleReference(f").append(parentId).append(", f").append(referenceId).append(", \"" + field.getName() + "\", " + title + ");\n");
			} else if (field.isMultiReference()) {
				data.append(getTabs(2)).append("SCHEMA.addMultiReference(f").append(parentId).append(", f").append(referenceId).append(", \"" + field.getName() + "\", " + title + ");\n");
			} else {
				data.append(getTabs(2)).append("SCHEMA.addField(f" + parentId + ", " + id + ", \"" + field.getName() + "\", " + title + ", MessageFieldType." + field.getType().name() + ", MessageFieldContentType." + field.getContentType().name() + ", null);\n");
			}
		}

		tpl = setValue(tpl, "data", data.toString());
		tpl = setValue(tpl, "registry", registry.toString());

		File file = new File(directory, firstUpperCase(schema.getName()) + ".java");
		Files.writeString(file.toPath(), tpl);
	}

	private static void createMessagePojoSave(MessageSchema schema, MessageField field, File directory) {
		try {
			createMessagePojo(schema, field, directory);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void createMessagePojo(MessageSchema schema, MessageField objectField, File directory) throws IOException {
		String tpl = readTemplate("pojo.tpl");
		tpl = setValue(tpl, "package", schema.getNamespace());
		tpl = setValue(tpl, "type", firstUpperCase(objectField.getName()));
		tpl = setValue(tpl, "schema", firstUpperCase(schema.getName()));
		tpl = setValue(tpl, "fieldId", "" + objectField.getId());
		StringBuilder data = new StringBuilder();

		for (MessageField field : objectField.getFields()) {
			data.append(getTabs(1)).append("public ").append(getReturnType(field, schema)).append(" get").append(firstUpperCase(field.getName())).append("() {\n");
			data.append(getTabs(2)).append("return ").append(getGetterMethod(field)).append("(\"").append(field.getName()).append("\");\n");
			data.append(getTabs(1)).append("}\n");

			data.append("\n");
			data.append(getTabs(1)).append("public " + firstUpperCase(objectField.getName()) + " set").append(firstUpperCase(field.getName())).append("(").append(getReturnType(field, schema)).append(" value) {\n");
			data.append(getTabs(2)).append("setPropertyValue(\"").append(field.getName()).append("\", value);\n");
			data.append(getTabs(2)).append("return this;\n");
			data.append(getTabs(1)).append("}\n");
			if (field.isMultiReference()) {
				data.append("\n");
				data.append(getTabs(1)).append("public " + firstUpperCase(objectField.getName()) + " add").append(firstUpperCase(field.getName())).append("(").append(firstUpperCase(field.getReferencedField(schema).getName())).append(" value) {\n");
				data.append(getTabs(2)).append("addMultiReference(\"").append(field.getName()).append("\", value);\n");
				data.append(getTabs(2)).append("return this;\n");
				data.append(getTabs(1)).append("}\n");
			}
		}

		tpl = setValue(tpl, "methods", data.toString());
		File file = new File(directory, firstUpperCase(objectField.getName()) + ".java");
		Files.writeString(file.toPath(), tpl);
		System.out.println("Write pojo:" + file.getPath());
	}

	private static String readTemplate(String name) throws IOException {
		InputStream inputStream = PojoBuilder.class.getResourceAsStream("/templates/" + name);
		return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
	}

	private static String setValue(String template, String name, String value) {
		return template.replace("{" + name + "}", value);
	}

	private static String firstUpperCase(String value) {
		return value.substring(0, 1).toUpperCase() + value.substring(1);
	}

	private static String getTabs(int count) {
		return "\t".repeat(count);
	}

	private static String getReturnType(MessageField field, MessageSchema schema) {
		return switch (field.getType()) {
			case OBJECT -> firstUpperCase(field.getName());
			case OBJECT_SINGLE_REFERENCE -> firstUpperCase(field.getReferencedField(schema).getName());
			case OBJECT_MULTI_REFERENCE -> "List<" + firstUpperCase(field.getReferencedField(schema).getName()) + ">";
			case BOOLEAN -> "boolean";
			case BYTE -> "byte";
			case INT -> "int";
			case LONG -> "long";
			case FLOAT -> "float";
			case DOUBLE -> "double";
			case STRING -> "String";
			case BITSET -> "BitSet";
			case BYTE_ARRAY -> "byte[]";
			case INT_ARRAY -> "int[]";
			case LONG_ARRAY -> "long[]";
			case FLOAT_ARRAY -> "float[]";
			case DOUBLE_ARRAY -> "double[]";
			case STRING_ARRAY -> "String[]";
			case FILE -> "File";
			case ENUM -> firstUpperCase(field.getName());
		};
	}

	private static String getGetterMethod(MessageField field) {
		return switch (field.getType()) {
			case OBJECT -> "getMessageObject";
			case OBJECT_SINGLE_REFERENCE -> "getMessageObject";
			case OBJECT_MULTI_REFERENCE -> "getMessageList";
			case BOOLEAN -> "getBooleanValue";
			case BYTE -> "getByteValue";
			case INT -> "getIntValue";
			case LONG -> "getLongValue";
			case FLOAT -> "getFloatValue";
			case DOUBLE -> "getDoubleValue";
			case STRING -> "getStringValue";
			case BITSET -> "getBitSetValue";
			case BYTE_ARRAY -> "getByteArrayValue";
			case INT_ARRAY -> "getIntArrayValue";
			case LONG_ARRAY -> "getLongArrayValue";
			case FLOAT_ARRAY -> "getFloatArrayValue";
			case DOUBLE_ARRAY -> "getDoubleArrayValue";
			case STRING_ARRAY -> "getStringArrayValue";
			case FILE -> "getFileValue";
			case ENUM -> "getIntValue";
		};
	}


}
