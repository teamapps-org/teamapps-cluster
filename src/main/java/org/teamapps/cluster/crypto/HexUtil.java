package org.teamapps.cluster.crypto;

public class HexUtil {

	public static final char[] HEX_ARRAY = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

	public static String bytesToHex(byte[] bytes) {
		char[] chars = new char[bytes.length * 2];
		int v;
		for (int j = 0; j < bytes.length; j++) {
			v = bytes[j] & 0xFF;
			chars[j * 2] = HEX_ARRAY[v >>> 4];
			chars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
		}
		return new String(chars);
	}

}
