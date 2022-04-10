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
package org.teamapps.cluster.dto;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MessageField {

	private final int parentFieldId;
	private final int id;
	private final String name;
	private String title;
	private final MessageFieldType type;
	private final MessageFieldContentType contentType;
	private final String specificType;
	private final int referencedFieldId;
	private boolean deprecated;
	private String[] enumValues;
	private final List<MessageField> fields = new ArrayList<>();

	public MessageField(int parentFieldId, int id, String name, String title, MessageFieldType type, MessageFieldContentType contentType, String specificType, int referencedFieldId) {
		this.parentFieldId = parentFieldId;
		this.id = id;
		this.name = name;
		this.title = title;
		this.type = type;
		this.contentType = contentType;
		this.specificType = specificType;
		this.referencedFieldId = referencedFieldId;
	}

	public MessageField(DataInputStream dis) throws IOException {
		parentFieldId = dis.readInt();
		id = dis.readInt();
		name = MessageUtils.readString(dis);
		title = MessageUtils.readString(dis);
		type = MessageFieldType.getById(MessageUtils.readByteAsInt(dis));
		contentType = MessageFieldContentType.getById(MessageUtils.readByteAsInt(dis));
		specificType = MessageUtils.readString(dis);
		referencedFieldId = dis.readInt();
		deprecated = dis.readBoolean();
		int fieldCount = dis.readInt();
		for (int i = 0; i < fieldCount; i++) {
			fields.add(new MessageField(dis));
		}
	}


	public void write(DataOutputStream dos) throws IOException {
		dos.writeInt(parentFieldId);
		dos.writeInt(id);
		MessageUtils.writeString(dos, name);
		MessageUtils.writeString(dos, title);
		MessageUtils.writeIntAsByte(dos, type.getId());
		MessageUtils.writeIntAsByte(dos, contentType.getId());
		MessageUtils.writeString(dos, specificType);
		dos.writeInt(referencedFieldId);
		dos.writeBoolean(deprecated);
		dos.writeInt(fields.size());
		for (MessageField field : fields) {
			field.write(dos);
		}
	}

	public byte[] toBytes() throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(bos);
		write(dos);
		dos.close();
		return bos.toByteArray();
	}

	public MessageField getById(int id) {
		if (this.id == id) {
			return this;
		}
		return fields.stream()
				.filter(field -> field.getId() == id)
				.findAny().orElse(null);
	}

	public MessageField getByName(String name) {
		if (this.name.equals(name)) {
			return this;
		}
		return fields.stream()
				.filter(field -> field.getName().equals(name))
				.findAny().orElse(null);
	}


	public boolean isObject() {
		return type == MessageFieldType.OBJECT;
	}

	public boolean isObjectOrMultiReference() {
		return type == MessageFieldType.OBJECT || type == MessageFieldType.OBJECT_MULTI_REFERENCE;
	}

	public boolean isObjectReference() {
		return type == MessageFieldType.OBJECT_SINGLE_REFERENCE || type == MessageFieldType.OBJECT_MULTI_REFERENCE;
	}

	public boolean isSingleReference() {
		return type == MessageFieldType.OBJECT_SINGLE_REFERENCE;
	}

	public boolean isMultiReference() {
		return type == MessageFieldType.OBJECT_MULTI_REFERENCE;
	}

	protected void addField(MessageField field) {
		fields.add(field);
	}

	public int getParentFieldId() {
		return parentFieldId;
	}

	public MessageField getParent(MessageModel model) {
		return model.getFieldById(parentFieldId);
	}

	public int getReferencedFieldId() {
		return referencedFieldId;
	}

	public MessageField getReferencedField(MessageModel model) {
		return model.getFieldById(referencedFieldId);
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getTitle() {
		return title;
	}

	public MessageField setTitle(String title) {
		this.title = title;
		return this;
	}

	public MessageFieldType getType() {
		return type;
	}

	public MessageFieldContentType getContentType() {
		return contentType;
	}

	public String getSpecificType() {
		return specificType;
	}

	public boolean isDeprecated() {
		return deprecated;
	}

	public void setDeprecated(boolean deprecated) {
		this.deprecated = deprecated;
	}

	public List<MessageField> getFields() {
		return fields;
	}

	protected String explain(int level) {
		StringBuilder sb = new StringBuilder();
		sb.append("\t".repeat(level))
				.append(name).append(", ")
				.append(title).append(", ")
				.append(id).append(", ")
				.append(type).append(contentType != null ? ", " + contentType : "").append("\n");
		getFields().forEach(field -> sb.append(field.explain(level + 1)));
		return sb.toString();
	}

	@Override
	public String toString() {
		return explain(0);
	}
}
