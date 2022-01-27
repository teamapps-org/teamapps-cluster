package org.teamapps.cluster.dto;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;

public class MessageUtils {

	public static File readFile(DataInputStream dis, FileProvider fileProvider) throws IOException {
		String fileId = readString(dis);
		if (fileProvider == null) {
			return null;
		}
		return fileProvider.getFile(fileId);
	}

	public static File readFile(ByteBuffer buffer, FileProvider fileProvider) {
		String fileId = readString(buffer);
		if (fileProvider == null) {
			return null;
		}
		return fileProvider.getFile(fileId);
	}

	public static void writeFile(DataOutputStream dos, File file, FileSink fileSink) throws IOException {
		if (fileSink == null || file == null || !file.exists() || file.length() == 0) {
			writeString(dos, null);
			return;
		}
		String fileId = fileSink.handleFile(file);
		writeString(dos, fileId);
	}

	public static void writeFile(ByteBuffer buffer, File file, FileSink fileSink) throws IOException {
		if (fileSink == null || file == null) {
			writeString(buffer, null);
			return;
		}
		String fileId = fileSink.handleFile(file);
		writeString(buffer, fileId);
	}

	public static void writeIntAsByte(DataOutputStream dos, int value) throws IOException {
		dos.writeByte(value);
	}

	public static int readByteAsInt(DataInputStream dis) throws IOException {
		return dis.readByte();
	}

	public static void writeString(DataOutputStream dos, String value) throws IOException {
		if (value != null && !value.isEmpty()) {
			byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
			dos.writeInt(bytes.length);
			dos.write(bytes);
		} else {
			dos.writeInt(0);
		}
	}

	public static void writeString(ByteBuffer buffer, String value) {
		if (value != null && !value.isEmpty()) {
			byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
			buffer.putInt(bytes.length);
			buffer.put(bytes);
		} else {
			buffer.putInt(0);
		}
	}

	public static String readString(DataInputStream dis) throws IOException {
		int length = dis.readInt();
		if (length == 0) {
			return null;
		} else {
			byte[] bytes = new byte[length];
			dis.readFully(bytes);
			return new String(bytes, StandardCharsets.UTF_8);
		}
	}

	public static String readString(ByteBuffer buf) {
		int length = buf.getInt();
		if (length == 0) {
			return null;
		} else {
			byte[] bytes = new byte[length];
			buf.get(bytes);
			return new String(bytes, StandardCharsets.UTF_8);
		}
	}

	public static void writeByteArray(DataOutputStream dos, byte[] bytes) throws IOException {
		if (bytes == null) {
			dos.writeInt(0);
		} else {
			dos.writeInt(bytes.length);
			dos.write(bytes);
		}
	}

	public static void writeByteArray(ByteBuffer buf, byte[] bytes) {
		if (bytes == null) {
			buf.putInt(0);
		} else {
			buf.putInt(bytes.length);
			buf.put(bytes);
		}
	}

	public static byte[] readByteArray(DataInputStream dis) throws IOException {
		int length = dis.readInt();
		if (length == 0) {
			return null;
		}
		byte[] bytes = new byte[length];
		dis.readFully(bytes);
		return bytes;
	}

	public static byte[] readByteArray(ByteBuffer buf) {
		int length = buf.getInt();
		if (length == 0) {
			return null;
		}
		byte[] bytes = new byte[length];
		buf.get(bytes);
		return bytes;
	}

	public static void writeBitSet(DataOutputStream dos, BitSet bitSet) throws IOException {
		if (bitSet == null) {
			dos.writeInt(0);
		} else {
			dos.writeInt(bitSet.cardinality());
			for (int id = bitSet.nextSetBit(0); id >= 0; id = bitSet.nextSetBit(id + 1)) {
				dos.writeInt(id);
			}
		}
	}

	public static BitSet readBitSet(DataInputStream dis) throws IOException {
		int length = dis.readInt();
		if (length == 0) {
			return null;
		}
		BitSet bitSet = new BitSet();
		int size = dis.readInt();
		for (int i = 0; i < size; i++) {
			bitSet.set(dis.readInt());
		}
		return bitSet;
	}

	public static BitSet readBitSet(ByteBuffer buf) {
		int length = buf.getInt();
		if (length == 0) {
			return null;
		}
		BitSet bitSet = new BitSet();
		int size = buf.getInt();
		for (int i = 0; i < size; i++) {
			bitSet.set(buf.getInt());
		}
		return bitSet;
	}

	public static void writeIntArray(DataOutputStream dos, int[] intArray) throws IOException {
		if (intArray == null || intArray.length == 0) {
			dos.writeInt(0);
		} else {
			dos.writeInt(intArray.length);
			for (int value : intArray) {
				dos.writeInt(value);
			}
		}
	}

	public static int[] readIntArray(DataInputStream dis) throws IOException {
		int length = dis.readInt();
		if (length == 0) {
			return null;
		}
		int[] intArray = new int[length];
		for (int i = 0; i < length; i++) {
			intArray[i] = dis.readInt();
		}
		return intArray;
	}

	public static int[] readIntArray(ByteBuffer buf) {
		int length = buf.getInt();
		if (length == 0) {
			return null;
		}
		int[] intArray = new int[length];
		for (int i = 0; i < length; i++) {
			intArray[i] = buf.getInt();
		}
		return intArray;
	}

	public static void writeLongArray(DataOutputStream dos, long[] longArray) throws IOException {
		if (longArray == null || longArray.length == 0) {
			dos.writeInt(0);
		} else {
			dos.writeLong(longArray.length);
			for (long value : longArray) {
				dos.writeLong(value);
			}
		}
	}

	public static long[] readLongArray(DataInputStream dis) throws IOException {
		int length = dis.readInt();
		if (length == 0) {
			return null;
		}
		long[] longArray = new long[length];
		for (int i = 0; i < length; i++) {
			longArray[i] = dis.readLong();
		}
		return longArray;
	}

	public static long[] readLongArray(ByteBuffer buf) {
		int length = buf.getInt();
		if (length == 0) {
			return null;
		}
		long[] longArray = new long[length];
		for (int i = 0; i < length; i++) {
			longArray[i] = buf.getLong();
		}
		return longArray;
	}


	public static void writeFloatArray(DataOutputStream dos, float[] floatArray) throws IOException {
		if (floatArray == null || floatArray.length == 0) {
			dos.writeInt(0);
		} else {
			dos.writeFloat(floatArray.length);
			for (float value : floatArray) {
				dos.writeFloat(value);
			}
		}
	}

	public static float[] readFloatArray(DataInputStream dis) throws IOException {
		int length = dis.readInt();
		if (length == 0) {
			return null;
		}
		float[] floatArray = new float[length];
		for (int i = 0; i < length; i++) {
			floatArray[i] = dis.readFloat();
		}
		return floatArray;
	}

	public static float[] readFloatArray(ByteBuffer buf) {
		int length = buf.getInt();
		if (length == 0) {
			return null;
		}
		float[] floatArray = new float[length];
		for (int i = 0; i < length; i++) {
			floatArray[i] = buf.getFloat();
		}
		return floatArray;
	}

	public static void writeDoubleArray(DataOutputStream dos, double[] doubleArray) throws IOException {
		if (doubleArray == null || doubleArray.length == 0) {
			dos.writeInt(0);
		} else {
			dos.writeDouble(doubleArray.length);
			for (double value : doubleArray) {
				dos.writeDouble(value);
			}
		}
	}

	public static double[] readDoubleArray(DataInputStream dis) throws IOException {
		int length = dis.readInt();
		if (length == 0) {
			return null;
		}
		double[] doubleArray = new double[length];
		for (int i = 0; i < length; i++) {
			doubleArray[i] = dis.readDouble();
		}
		return doubleArray;
	}

	public static double[] readDoubleArray(ByteBuffer buf) {
		int length = buf.getInt();
		if (length == 0) {
			return null;
		}
		double[] doubleArray = new double[length];
		for (int i = 0; i < length; i++) {
			doubleArray[i] = buf.getDouble();
		}
		return doubleArray;
	}

	public static void writeStringArray(DataOutputStream dos, String[] stringArray) throws IOException {
		if (stringArray == null || stringArray.length == 0) {
			dos.writeInt(0);
		} else {
			dos.writeInt(stringArray.length);
			for (String value : stringArray) {
				writeString(dos, value);
			}
		}
	}

	public static String[] readStringArray(DataInputStream dis) throws IOException {
		int length = dis.readInt();
		if (length == 0) {
			return null;
		}
		String[] stringArray = new String[length];
		for (int i = 0; i < length; i++) {
			stringArray[i] = readString(dis);
		}
		return stringArray;
	}

	public static String[] readStringArray(ByteBuffer buf) {
		int length = buf.getInt();
		if (length == 0) {
			return null;
		}
		String[] stringArray = new String[length];
		for (int i = 0; i < length; i++) {
			stringArray[i] = readString(buf);
		}
		return stringArray;
	}

	public static void writeBoolean(ByteBuffer buffer, boolean value) {
		buffer.put((byte) (value ? 1 : 0));
	}

	public static boolean readBoolean(ByteBuffer buf) {
		return buf.get() == 1;
	}

	public static void writeShort(ByteBuffer buffer, int value) {
		buffer.putShort((short) value);
	}

	public static int readShort(ByteBuffer buffer) {
		return buffer.getShort();
	}
}
