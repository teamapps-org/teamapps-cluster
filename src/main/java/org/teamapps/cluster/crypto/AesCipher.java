package org.teamapps.cluster.crypto;

import org.teamapps.common.util.ExceptionUtil;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class AesCipher {

	private Cipher cipher;
	private SecretKeySpec secretKeySpec;
	private SecureRandom secureRandom;
	private static int BLOCK_SIZE_BYTES = 16;

	public AesCipher(String key) {
		this(ShaHash.createHashBytes(key));
	}

	public AesCipher(byte[] key) {
		secureRandom = new SecureRandom();
		secretKeySpec = new SecretKeySpec(key, "AES");
		try {
			cipher = Cipher.getInstance("AES/CTR/NoPadding");
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
			e.printStackTrace();
		}
	}

	public synchronized byte[] encrypt(byte[] data) throws Exception {
		byte[] iv = new byte[BLOCK_SIZE_BYTES];
		secureRandom.nextBytes(iv);
		cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new IvParameterSpec(iv));
		return combineArrays(iv, cipher.doFinal(data));
	}

	public byte[] encryptSave(byte[] data) {
		return ExceptionUtil.softenExceptions(() -> encrypt(data));
	}

	public byte[] decrypt(byte[] data) throws Exception {
		return decrypt(data, 0);
	}

	public byte[] decryptSave(byte[] data) {
		return ExceptionUtil.softenExceptions(() -> decrypt(data, 0));
	}

	public synchronized byte[] decrypt(byte[] data, int offset) throws Exception {
		byte[] iv = new byte[BLOCK_SIZE_BYTES];
		System.arraycopy(data, offset, iv, 0, BLOCK_SIZE_BYTES);
		cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new IvParameterSpec(iv));
		return cipher.doFinal(data, (BLOCK_SIZE_BYTES + offset), data.length - (BLOCK_SIZE_BYTES + offset));
	}

	public synchronized byte[] encrypt(long nonce, long id, byte[] data) throws Exception {
		byte[] iv = new byte[BLOCK_SIZE_BYTES];
		ByteBuffer buffer = ByteBuffer.wrap(iv);
		buffer.putLong(nonce);
		buffer.putLong(id);
		cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new IvParameterSpec(iv));
		return cipher.doFinal(data);
	}

	public synchronized byte[] decrypt(long nonce, long id, byte[] data) throws Exception {
		byte[] iv = new byte[BLOCK_SIZE_BYTES];
		ByteBuffer buffer = ByteBuffer.wrap(iv);
		buffer.putLong(nonce);
		buffer.putLong(id);
		cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new IvParameterSpec(iv));
		return cipher.doFinal(data);
	}

	public int encryptInt(long nonce, long id, int value) throws Exception {
		return cryptInt(nonce, id, value, Cipher.ENCRYPT_MODE);
	}

	public int decryptInt(long nonce, long id, int value) throws Exception {
		return cryptInt(nonce, id, value, Cipher.DECRYPT_MODE);
	}

	private synchronized int cryptInt(long nonce, long id, int value, int encryptMode) throws Exception {
		byte[] iv = new byte[BLOCK_SIZE_BYTES];
		ByteBuffer ivBuf = ByteBuffer.wrap(iv);
		ivBuf.putLong(nonce);
		ivBuf.putLong(id);
		byte[] data = new byte[4];
		ByteBuffer buf = ByteBuffer.wrap(data);
		buf.putInt(0, value);
		cipher.init(encryptMode, secretKeySpec, new IvParameterSpec(iv));
		byte[] bytes = cipher.doFinal(data);
		buf = ByteBuffer.wrap(bytes);
		return buf.getInt(0);
	}

	public static byte[] combineArrays(byte[] array1, byte[] array2) {
		byte[] joinedArray = new byte[array1.length + array2.length];
		System.arraycopy(array1, 0, joinedArray, 0, array1.length);
		System.arraycopy(array2, 0, joinedArray, array1.length, array2.length);
		return joinedArray;
	}
}
