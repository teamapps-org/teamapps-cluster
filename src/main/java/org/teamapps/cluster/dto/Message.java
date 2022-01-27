package org.teamapps.cluster.dto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class Message {
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private final MessageField field;
	private Object value;

	public static Function<Message, byte[]> ENCODER = message -> {
		try {
			return message.toBytes();
		} catch (IOException e) {
			LOGGER.error("Error encoding message instance", e);
		}
		return null;
	};

	public static int getMessageFieldId(byte[] bytes) throws IOException {
		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(bytes));
		return dis.readInt();
	}

	public Message(MessageField field, Object value) {
		this.field = field;
		this.value = value;
	}

	public Message(ByteBuffer buf, MessageModel model) {
		this(buf, model, null);
	}

	public Message(ByteBuffer buf, MessageModel model, FileProvider fileProvider) {
		int id = buf.getInt();
		this.field = model.getFieldById(id);
		switch (field.getType()) {
			case OBJECT, OBJECT_MULTI_REFERENCE -> {
				List<Message> messages = new ArrayList<>();
				int messageCount = buf.getInt();
				for (int i = 0; i < messageCount; i++) {
					messages.add(new Message(buf, model));
				}
				value = messages;
			}
			case OBJECT_SINGLE_REFERENCE -> value = new Message(buf, model);
			case BOOLEAN -> value = MessageUtils.readBoolean(buf);
			case BYTE -> value = buf.get();
			case INT -> value = buf.getInt();
			case LONG -> value = buf.getLong();
			case FLOAT -> value = buf.getFloat();
			case DOUBLE -> value = buf.getDouble();
			case STRING -> value = MessageUtils.readString(buf);
			case BITSET -> value = MessageUtils.readBitSet(buf);
			case BYTE_ARRAY -> value = MessageUtils.readByteArray(buf);
			case INT_ARRAY -> value = MessageUtils.readIntArray(buf);
			case LONG_ARRAY -> value = MessageUtils.readLongArray(buf);
			case FLOAT_ARRAY -> value = MessageUtils.readFloatArray(buf);
			case DOUBLE_ARRAY -> value = MessageUtils.readDoubleArray(buf);
			case STRING_ARRAY -> value = MessageUtils.readStringArray(buf);
			case FILE -> value = MessageUtils.readFile(buf, fileProvider);
		}
	}

	public Message(DataInputStream dis, MessageModel model) throws IOException {
		this(dis, model, null);
	}

	public Message(DataInputStream dis, MessageModel model, FileProvider fileProvider) throws IOException {
		this(dis, model, fileProvider, null);
	}

	public Message(DataInputStream dis, MessageModel model, FileProvider fileProvider, MessageDecoderRegistry decoderRegistry) throws IOException {
		int id = dis.readInt();
		this.field = model.getFieldById(id);
		switch (field.getType()) {
			case OBJECT, OBJECT_MULTI_REFERENCE -> {
				if (decoderRegistry != null && field.getType() == MessageFieldType.OBJECT_MULTI_REFERENCE && decoderRegistry.containsDecoder(field.getReferencedFieldId())) {
					MessageDecoder<? extends Message> messageDecoder = decoderRegistry.getMessageDecoder(field.getReferencedFieldId());
					List<Message> messages = new ArrayList<>();
					int messageCount = dis.readInt();
					for (int i = 0; i < messageCount; i++) {
						messages.add(messageDecoder.decode(dis, fileProvider));
					}
					value = messages;
				} else {
					List<Message> messages = new ArrayList<>();
					int messageCount = dis.readInt();
					for (int i = 0; i < messageCount; i++) {
						messages.add(new Message(dis, model, fileProvider, decoderRegistry));
					}
					value = messages;
				}
			}
			case OBJECT_SINGLE_REFERENCE -> {
				if (decoderRegistry !=null && decoderRegistry.containsDecoder(field.getReferencedFieldId())) {
					MessageDecoder<? extends Message> messageDecoder = decoderRegistry.getMessageDecoder(field.getReferencedFieldId());
					value = messageDecoder.decode(dis, fileProvider);
				} else {
					value = new Message(dis, model, fileProvider);
				}
			}
			case BOOLEAN -> value = dis.readBoolean();
			case BYTE -> value = dis.readByte();
			case INT, ENUM -> value = dis.readInt();
			case LONG -> value = dis.readLong();
			case FLOAT -> value = dis.readFloat();
			case DOUBLE -> value = dis.readDouble();
			case STRING -> value = MessageUtils.readString(dis);
			case BITSET -> value = MessageUtils.readBitSet(dis);
			case BYTE_ARRAY -> value = MessageUtils.readByteArray(dis);
			case INT_ARRAY -> value = MessageUtils.readIntArray(dis);
			case LONG_ARRAY -> value = MessageUtils.readLongArray(dis);
			case FLOAT_ARRAY -> value = MessageUtils.readFloatArray(dis);
			case DOUBLE_ARRAY -> value = MessageUtils.readDoubleArray(dis);
			case STRING_ARRAY -> value = MessageUtils.readStringArray(dis);
			case FILE -> value = MessageUtils.readFile(dis, fileProvider);
		}
	}

	public Message(byte[] bytes, MessageModel model) throws IOException {
		this(new DataInputStream(new ByteArrayInputStream(bytes)), model);
	}

	public Message(byte[] bytes, MessageModel model, FileProvider fileProvider) throws IOException {
		this(new DataInputStream(new ByteArrayInputStream(bytes)), model, fileProvider);
	}

	public Message(byte[] bytes, MessageModel model, FileProvider fileProvider, MessageDecoderRegistry decoderRegistry) throws IOException {
		this(new DataInputStream(new ByteArrayInputStream(bytes)), model, fileProvider, decoderRegistry);
	}

	public void write(DataOutputStream dos) throws IOException {
		write(dos, null);
	}

	public void write(DataOutputStream dos, FileSink fileSink) throws IOException {
		dos.writeInt(field.getId());
		switch (field.getType()) {
			case OBJECT, OBJECT_MULTI_REFERENCE -> {
				List<Message> messages = getMessageObjectValue();
				if (messages == null) {
					dos.writeInt(0);
				} else {
					dos.writeInt(messages.size());
					for (Message message : messages) {
						message.write(dos, fileSink);
					}
				}
			}
			case OBJECT_SINGLE_REFERENCE -> getMessageObject().write(dos, fileSink);
			case BOOLEAN -> dos.writeBoolean(getBooleanValue());
			case BYTE -> dos.writeByte(getByteValue());
			case INT, ENUM -> dos.writeInt(getIntValue());
			case LONG -> dos.writeLong(getLongValue());
			case FLOAT -> dos.writeFloat(getFloatValue());
			case DOUBLE -> dos.writeDouble(getDoubleValue());
			case STRING -> MessageUtils.writeString(dos, getStringValue());
			case BITSET -> MessageUtils.writeBitSet(dos, getBitSetValue());
			case BYTE_ARRAY -> MessageUtils.writeByteArray(dos, getByteArrayValue());
			case INT_ARRAY -> MessageUtils.writeIntArray(dos, getIntArrayValue());
			case LONG_ARRAY -> MessageUtils.writeLongArray(dos, getLongArrayValue());
			case FLOAT_ARRAY -> MessageUtils.writeFloatArray(dos, getFloatArrayValue());
			case DOUBLE_ARRAY -> MessageUtils.writeDoubleArray(dos, getDoubleArrayValue());
			case STRING_ARRAY -> MessageUtils.writeStringArray(dos, getStringArrayValue());
			case FILE -> MessageUtils.writeFile(dos, getFileValue(), fileSink);
		}
	}

	public byte[] toBytes() throws IOException {
		return toBytes(null);
	}

	public byte[] toBytes(FileSink fileSink) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(bos);
		write(dos, fileSink);
		dos.close();
		return bos.toByteArray();
	}

	public MessageField getField() {
		return field;
	}

	public int getFieldId() {
		return field.getId();
	}

	private Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	public Message getMessageByFieldId(int id) {
		if (field.getId() == id) {
			return this;
		} else if (value != null && isObjectOrMultiReference()) {
			return getMessageObjectValue().stream()
					.filter(message -> message.getField().getId() == id)
					.findAny().orElse(null);
		} else if (value != null && isObject()) {
			Message message = getMessageObject();
			if (message.getField().getId() == id) {
				return message;
			}
		}
		return null;
	}

	public Message getMessageByFieldName(String name) {
		if (field.getName().equals(name)) {
			return this;
		} else if (value != null && isObjectOrMultiReference()) {
			return getMessageObjectValue().stream()
					.filter(message -> message.getField().getName().equals(name))
					.findAny().orElse(null);
		} else if (value != null && isObject()) {
			Message message = getMessageObject();
			if (message.getField().getName().equals(name)) {
				return message;
			}
		}
		return null;
	}

	public void setPropertyValue(String name, Object value) {
		Message message = getMessageByFieldName(name);
		if (message != null) {
			message.setValue(value);
		} else {
			MessageField field = getField().getByName(name);
			if (field != null) {
				message = new Message(field, value);
				addMessage(message);
			} else {
				throw new RuntimeException("Cannot find field with name:" + name);
			}
		}
	}

	public void addMultiReference(String name, Message value) {
		Message message = getMessageByFieldName(name);
		if (message != null && message.isMultiReference()) {
			message.addMessage(value);
		} else {
			MessageField field = getField().getByName(name);
			if (field != null && field.isMultiReference()) {
				message = new Message(field, new ArrayList<Message>());
				addMessage(message);
				message.addMessage(value);
			} else {
				throw new RuntimeException("Cannot find object field with name:" + name);
			}
		}
	}

	public void setSingleReference(String name, Message value) {
		Message message = getMessageByFieldName(name);
		if (message != null && message.isSingleReference()) {
			message.setValue(value);
		} else {
			MessageField field = getField().getByName(name);
			if (field != null && field.isSingleReference()) {
				message = new Message(field, value);
				addMessage(message);
			} else {
				throw new RuntimeException("Cannot find object field with name:" + name);
			}
		}
	}

	protected void addMessage(Message message) {
		if (!isObjectOrMultiReference()) {
			throw new RuntimeException("Cannot add message to wrong field:" + field + ", message:" + message);
		}
		getMessageObjectValue().add(message);
	}

	protected boolean isObject() {
		return field.isObject();
	}

	protected boolean isObjectOrMultiReference() {
		return field.isObjectOrMultiReference();
	}

	protected boolean isObjectReference() {
		return field.isObjectReference();
	}

	protected boolean isSingleReference() {
		return field.isSingleReference();
	}

	protected boolean isMultiReference() {
		return field.isMultiReference();
	}

	protected Message getMessageValue() {
		if (value == null) return null;
		return (Message) value;
	}

	protected List<Message> getMessageObjectValue() {
		if (value == null) return null;
		return (List<Message>) value;
	}

	protected <TYPE extends Message> List<TYPE> getMessageList() {
		return (List<TYPE>) value;
	}

	protected <TYPE extends Message> TYPE getMessageObject() {
		return (TYPE) value;
	}

	protected boolean getBooleanValue() {
		if (value == null) return false;
		return (boolean) value;
	}

	protected byte getByteValue() {
		if (value == null) return 0;
		return (byte) value;
	}

	protected int getIntValue() {
		if (value == null) return 0;
		return (int) value;
	}

	protected long getLongValue() {
		if (value == null) return 0;
		return (long) value;
	}

	protected float getFloatValue() {
		if (value == null) return 0;
		return (Float) value;
	}

	protected double getDoubleValue() {
		if (value == null) return 0;
		return (Double) value;
	}

	protected String getStringValue() {
		if (value == null) return null;
		return (String) value;
	}

	protected File getFileValue() {
		if (value == null) return null;
		return (File) value;
	}

	protected BitSet getBitSetValue() {
		if (value == null) return null;
		return (BitSet) value;
	}

	protected byte[] getByteArrayValue() {
		if (value == null) return null;
		return (byte[]) value;
	}

	protected int[] getIntArrayValue() {
		if (value == null) return null;
		return (int[]) value;
	}

	protected long[] getLongArrayValue() {
		if (value == null) return null;
		return (long[]) value;
	}

	protected float[] getFloatArrayValue() {
		if (value == null) return null;
		return (float[]) value;
	}

	protected double[] getDoubleArrayValue() {
		if (value == null) return null;
		return (double[]) value;
	}

	protected String[] getStringArrayValue() {
		if (value == null) return null;
		return (String[]) value;
	}


	public List<Message> getMessageObjectValue(String name) {
		Message message = getMessageByFieldName(name);
		if (message == null) return null;
		return message.getMessageObjectValue();
	}

	public <TYPE extends Message> TYPE getMessageObject(String name) {
		Message message = getMessageByFieldName(name);
		if (message == null) return null;
		return message.getMessageObject();
	}

	public <TYPE extends Message> List<TYPE> getMessageList(String name) {
		Message message = getMessageByFieldName(name);
		if (message == null) return Collections.emptyList();
		return message.getMessageList();
	}

	public boolean getBooleanValue(String name) {
		Message message = getMessageByFieldName(name);
		if (message == null) return false;
		return message.getBooleanValue();
	}

	public byte getByteValue(String name) {
		Message message = getMessageByFieldName(name);
		if (message == null) return 0;
		return message.getByteValue();
	}

	public int getIntValue(String name) {
		Message message = getMessageByFieldName(name);
		if (message == null) return 0;
		return message.getIntValue();
	}

	public long getLongValue(String name) {
		Message message = getMessageByFieldName(name);
		if (message == null) return 0;
		return message.getLongValue();
	}

	public float getFloatValue(String name) {
		Message message = getMessageByFieldName(name);
		if (message == null) return 0;
		return message.getFloatValue();
	}

	public double getDoubleValue(String name) {
		Message message = getMessageByFieldName(name);
		if (message == null) return 0;
		return message.getDoubleValue();
	}

	public String getStringValue(String name) {
		Message message = getMessageByFieldName(name);
		if (message == null) return null;
		return message.getStringValue();
	}

	public File getFileValue(String name) {
		Message message = getMessageByFieldName(name);
		if (message == null) return null;
		return message.getFileValue();
	}

	public BitSet getBitSetValue(String name) {
		Message message = getMessageByFieldName(name);
		if (message == null) return null;
		return message.getBitSetValue();
	}

	public byte[] getByteArrayValue(String name) {
		Message message = getMessageByFieldName(name);
		if (message == null) return null;
		return message.getByteArrayValue();
	}

	public int[] getIntArrayValue(String name) {
		Message message = getMessageByFieldName(name);
		if (message == null) return null;
		return message.getIntArrayValue();
	}

	public long[] getLongArrayValue(String name) {
		Message message = getMessageByFieldName(name);
		if (message == null) return null;
		return message.getLongArrayValue();
	}

	public float[] getFloatArrayValue(String name) {
		Message message = getMessageByFieldName(name);
		if (message == null) return null;
		return message.getFloatArrayValue();
	}

	public double[] getDoubleArrayValue(String name) {
		Message message = getMessageByFieldName(name);
		if (message == null) return null;
		return message.getDoubleArrayValue();
	}

	public String[] getStringArrayValue(String name) {
		Message message = getMessageByFieldName(name);
		if (message == null) return null;
		return message.getStringArrayValue();
	}

	public List<Message> getMessageObjectValue(int fieldId) {
		Message message = getMessageByFieldId(fieldId);
		if (message == null) return null;
		return message.getMessageObjectValue();
	}

	public boolean getBooleanValue(int fieldId) {
		Message message = getMessageByFieldId(fieldId);
		if (message == null) return false;
		return message.getBooleanValue();
	}

	public byte getByteValue(int fieldId) {
		Message message = getMessageByFieldId(fieldId);
		if (message == null) return 0;
		return message.getByteValue();
	}

	public int getIntValue(int fieldId) {
		Message message = getMessageByFieldId(fieldId);
		if (message == null) return 0;
		return message.getIntValue();
	}

	public long getLongValue(int fieldId) {
		Message message = getMessageByFieldId(fieldId);
		if (message == null) return 0;
		return message.getLongValue();
	}

	public float getFloatValue(int fieldId) {
		Message message = getMessageByFieldId(fieldId);
		if (message == null) return 0;
		return message.getFloatValue();
	}

	public double getDoubleValue(int fieldId) {
		Message message = getMessageByFieldId(fieldId);
		if (message == null) return 0;
		return message.getDoubleValue();
	}

	public String getStringValue(int fieldId) {
		Message message = getMessageByFieldId(fieldId);
		if (message == null) return null;
		return message.getStringValue();
	}

	public BitSet getBitSetValue(int fieldId) {
		Message message = getMessageByFieldId(fieldId);
		if (message == null) return null;
		return message.getBitSetValue();
	}

	public byte[] getByteArrayValue(int fieldId) {
		Message message = getMessageByFieldId(fieldId);
		if (message == null) return null;
		return message.getByteArrayValue();
	}

	public int[] getIntArrayValue(int fieldId) {
		Message message = getMessageByFieldId(fieldId);
		if (message == null) return null;
		return message.getIntArrayValue();
	}

	public long[] getLongArrayValue(int fieldId) {
		Message message = getMessageByFieldId(fieldId);
		if (message == null) return null;
		return message.getLongArrayValue();
	}

	public float[] getFloatArrayValue(int fieldId) {
		Message message = getMessageByFieldId(fieldId);
		if (message == null) return null;
		return message.getFloatArrayValue();
	}

	public double[] getDoubleArrayValue(int fieldId) {
		Message message = getMessageByFieldId(fieldId);
		if (message == null) return null;
		return message.getDoubleArrayValue();
	}

	public String[] getStringArrayValue(int fieldId) {
		Message message = getMessageByFieldId(fieldId);
		if (message == null) return null;
		return message.getStringArrayValue();
	}

	protected String explain(int level) {
		StringBuilder sb = new StringBuilder();
		sb.append("\t".repeat(level)).append(field.getName()).append(", ");
		if (field.getTitle() != null) {
			sb.append(field.getTitle()).append(", ");
		}
		sb.append(field.getId()).append(", ").append(field.getType());
		if (field.isObjectReference()) {
			sb.append(" -> ").append(field.getReferencedFieldId());
		}
		sb.append(field.getContentType() != MessageFieldContentType.GENERIC ? ", " + field.getContentType() : "");
		if (value != null && field.isObjectOrMultiReference()) {
			sb.append("\n");
			getMessageObjectValue().forEach(message -> sb.append(message.explain(level + 1)));
		} else if (value != null && field.isSingleReference()) {
			sb.append("\n");
			sb.append(getMessageValue().explain(level + 1));
		} else {
			sb.append(": ").append(value).append("\n");
		}
		return sb.toString();
	}

	@Override
	public String toString() {
		return explain(0);
	}
}
