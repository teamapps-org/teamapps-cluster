package org.teamapps.cluster.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ShaHash {

	public static String createHash(String data) {
		final MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("SHA3-256");
			return HexUtil.bytesToHex(digest.digest(data.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String createHash(byte[] data) {
		return HexUtil.bytesToHex(createHashBytes(data));
	}

	public static byte[] createHashBytes(String data) {
		return createHashBytes(data.getBytes(StandardCharsets.UTF_8));
	}

	public static byte[] createHashBytes(byte[] data) {
		final MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("SHA3-256");
			return digest.digest(data);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return null;
	}

}
