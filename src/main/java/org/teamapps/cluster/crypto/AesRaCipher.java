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
package org.teamapps.cluster.crypto;

import org.apache.commons.codec.binary.Hex;
import org.teamapps.common.util.ExceptionUtil;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class AesRaCipher {

	private static int BLOCK_SIZE_BYTES = 16;

	private final Cipher cipher;
	private final SecretKey secretKeySpec;
	private final int ctrOffset;
	private final byte[] ivData;
	private final ByteBuffer ivBuffer;
	private final SecureRandom secureRandom;

	public AesRaCipher(String key) throws Exception {
		this(ShaHash.createHashBytes(key), null, 0);
	}

	public AesRaCipher(String key, String ivAsHex) throws Exception {
		this(ShaHash.createHashBytes(key), ivAsHex != null ? Hex.decodeHex(ivAsHex) : null, 0);
	}

	public AesRaCipher(String key, String ivAsHex, int ctrOffset) throws Exception {
		this(ShaHash.createHashBytes(key), ivAsHex != null ? Hex.decodeHex(ivAsHex) : null, ctrOffset);
	}

	public AesRaCipher(byte[] key, byte[] ivData, int ctrOffset) throws Exception {
		this.ctrOffset = ctrOffset;
		this.secretKeySpec = new SecretKeySpec(key, "AES");
		this.cipher = Cipher.getInstance("AES/CTR/NoPadding");
		this.secureRandom = new SecureRandom();
		if (ivData == null) {
			ivData = new byte[BLOCK_SIZE_BYTES];
			this.secureRandom.nextBytes(ivData);
		}
		this.ivBuffer = ByteBuffer.wrap(ivData);
		this.ivData = ivData;
	}

	private void init(int counter, boolean encrypt) throws Exception {
		ivBuffer.putInt(12, ctrOffset + counter);
		cipher.init(encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, secretKeySpec, new IvParameterSpec(ivData));
	}

	public synchronized byte[] encrypt(byte[] data, int counter) throws Exception {
		init(counter, true);
		return cipher.doFinal(data);
	}

	public byte[] encryptSave(byte[] data, int counter) {
		return ExceptionUtil.softenExceptions(() -> encrypt(data, counter));
	}

	public byte[] decrypt(byte[] data, int counter) throws Exception {
		return decrypt(data, 0, counter);
	}

	public byte[] decryptSave(byte[] data, int counter) {
		return ExceptionUtil.softenExceptions(() -> decrypt(data, counter));
	}

	public synchronized byte[] decrypt(byte[] data, int offset, int counter) throws Exception {
		init(counter, false);
		return cipher.doFinal(data, offset, data.length - offset);
	}


	public synchronized byte[] encryptInlinedIv(byte[] data) throws Exception {
		byte[] iv = new byte[BLOCK_SIZE_BYTES];
		secureRandom.nextBytes(iv);
		cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new IvParameterSpec(iv));
		return combineArrays(iv, cipher.doFinal(data));
	}

	public byte[] encryptSaveInlinedIv(byte[] data) {
		return ExceptionUtil.softenExceptions(() -> encryptInlinedIv(data));
	}

	public byte[] decryptInlinedIv(byte[] data) throws Exception {
		return decryptInlinedIv(data, 0);
	}

	public byte[] decryptSaveInlinedIv(byte[] data) {
		return ExceptionUtil.softenExceptions(() -> decryptInlinedIv(data, 0));
	}

	public synchronized byte[] decryptInlinedIv(byte[] data, int offset) throws Exception {
		byte[] iv = new byte[BLOCK_SIZE_BYTES];
		System.arraycopy(data, offset, iv, 0, BLOCK_SIZE_BYTES);
		cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new IvParameterSpec(iv));
		return cipher.doFinal(data, (BLOCK_SIZE_BYTES + offset), data.length - (BLOCK_SIZE_BYTES + offset));
	}

	public static byte[] combineArrays(byte[] array1, byte[] array2) {
		byte[] joinedArray = new byte[array1.length + array2.length];
		System.arraycopy(array1, 0, joinedArray, 0, array1.length);
		System.arraycopy(array2, 0, joinedArray, array1.length, array2.length);
		return joinedArray;
	}

	public synchronized byte[] encryptString(String value, int counter) throws Exception {
		if (value == null || value.isEmpty()) {
			return null;
		}
		init(counter, true);
		return cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
	}

	public synchronized String decryptString(byte[] data, int counter)throws Exception {
		if (data == null) {
			return null;
		}
		init(counter, false);
		byte[] bytes = cipher.doFinal(data);
		return new String(bytes, StandardCharsets.UTF_8);
	}

	public int encryptInt(int value, int counter) throws Exception {
		return cryptInt(value, counter, true);
	}

	public int decryptInt(int value, int counter) throws Exception {
		return cryptInt(value, counter, false);
	}

	private synchronized int cryptInt(int value, int counter, boolean encrypt) throws Exception {
		init(counter, encrypt);
		byte[] data = new byte[4];
		ByteBuffer buf = ByteBuffer.wrap(data);
		buf.putInt(0, value);
		byte[] bytes = cipher.doFinal(data);
		buf = ByteBuffer.wrap(bytes);
		return buf.getInt(0);
	}

	public long encryptLong(long value, int counter) throws Exception {
		return cryptLong(value, counter, true);
	}

	public long decryptLong(long value, int counter) throws Exception {
		return cryptLong(value, counter, false);
	}
	
	private synchronized long cryptLong(long value, int counter, boolean encrypt) throws Exception {
		init(counter, encrypt);
		byte[] data = new byte[8];
		ByteBuffer buf = ByteBuffer.wrap(data);
		buf.putLong(0, value);
		byte[] bytes = cipher.doFinal(data);
		buf = ByteBuffer.wrap(bytes);
		return buf.getLong(0);
	}

	public double encryptDouble(double value, int counter) throws Exception {
		return cryptDouble(value, counter, true);
	}

	public double decryptDouble(double value, int counter) throws Exception {
		return cryptDouble(value, counter, false);
	}

	private synchronized double cryptDouble(double value, int counter, boolean encrypt) throws Exception {
		init(counter, encrypt);
		byte[] data = new byte[8];
		ByteBuffer buf = ByteBuffer.wrap(data);
		buf.putDouble(0, value);
		byte[] bytes = cipher.doFinal(data);
		buf = ByteBuffer.wrap(bytes);
		return buf.getDouble(0);
	}

	public float encryptFloat(float value, int counter) throws Exception {
		return cryptFloat(value, counter, true);
	}

	public float decryptFloat(float value, int counter) throws Exception {
		return cryptFloat(value, counter, false);
	}

	private synchronized float cryptFloat(float value, int counter, boolean encrypt) throws Exception {
		init(counter, encrypt);
		byte[] data = new byte[4];
		ByteBuffer buf = ByteBuffer.wrap(data);
		buf.putFloat(0, value);
		byte[] bytes = cipher.doFinal(data);
		buf = ByteBuffer.wrap(bytes);
		return buf.getFloat(0);
	}



	public static void main(String[] args) throws Exception {
		AesRaCipher cipher = new AesRaCipher("this is the secret", "00000000111111112222222233333333");
		int v = cipher.encryptInt(3, 1);
		System.out.println(cipher.decryptInt(v, 1) + " -> " + v);
		long time = System.currentTimeMillis();
		int loops = 1_000_000;
		for (int i = 0; i < loops; i++) {
			int value = cipher.encryptInt(i, i);
			int decryptedInt = cipher.decryptInt(value, i);
			if (i != decryptedInt) {
				System.out.println("Error:" + i + " -> " + decryptedInt);
			}
		}
		System.out.println("TIME INT:" + (System.currentTimeMillis() - time));

		time = System.currentTimeMillis();
		for (int i = 0; i < loops; i++) {
			long value = cipher.encryptLong(i, i);
			long decryptedInt = cipher.decryptLong(value, i);
			if (i != decryptedInt) {
				System.out.println("Error:" + i + " -> " + decryptedInt);
			}
		}
		System.out.println("TIME LONG:" + (System.currentTimeMillis() - time));

		time = System.currentTimeMillis();
		for (int i = 0; i < loops; i++) {
			String value = "v" + i;
			byte[] data = cipher.encryptString(value, i);
			String s = cipher.decryptString(data, i);
			if (!value.equals(s)) {
				System.out.println("Error:" + value + " -> " + s);
			}
		}
		System.out.println("TIME STRING:" + (System.currentTimeMillis() - time));

		time = System.currentTimeMillis();
		for (int i = 0; i < Integer.MAX_VALUE; i++) {
			int value = cipher.encryptInt(i, i);
			if (value == 0) {
				System.out.println(i + " = 0");
			}

		}
		System.out.println("TIME TEST:" + (System.currentTimeMillis() - time));
	}


}
