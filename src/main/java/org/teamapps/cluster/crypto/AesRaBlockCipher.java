/*-
 * ========================LICENSE_START=================================
 * TeamApps Cluster
 * ---
 * Copyright (C) 2021 - 2023 TeamApps.org
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

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;

public class AesRaBlockCipher {
	private static int BLOCK_SIZE_BYTES = 16;

	private final Cipher cipher;
	private final SecretKey secretKeySpec;
	private final SecureRandom secureRandom;

	private final int ctrOffset;
	private final byte[] ivData;
	private final ByteBuffer ivBuffer;

	public AesRaBlockCipher(String key) throws Exception {
		this(ShaHash.createHashBytes(key), null, 0);
	}

	public AesRaBlockCipher(String key, String ivAsHex) throws Exception {
		this(ShaHash.createHashBytes(key), ivAsHex != null ? Hex.decodeHex(ivAsHex) : null, 0);
	}

	public AesRaBlockCipher(String key, String ivAsHex, int ctrOffset) throws Exception {
		this(ShaHash.createHashBytes(key), ivAsHex != null ? Hex.decodeHex(ivAsHex) : null, ctrOffset);
	}

	public AesRaBlockCipher(byte[] key, byte[] ivData, int ctrOffset) throws Exception {
		this.ctrOffset = ctrOffset;
		this.secureRandom = new SecureRandom();
		this.secretKeySpec = new SecretKeySpec(key, "AES");
		this.cipher = Cipher.getInstance("AES/CTR/NoPadding");
		if (ivData == null) {
			ivData = new byte[BLOCK_SIZE_BYTES];
			secureRandom.nextBytes(ivData);
		}
		this.ivBuffer = ByteBuffer.wrap(ivData);
		this.ivData = ivData;
	}

	private void initCypher(int pos, int dataLength, boolean encrypt) throws Exception {
		int counter = getCounter(pos, dataLength);
		ivBuffer.putInt(12, ctrOffset + counter);
		cipher.init(encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, secretKeySpec, new IvParameterSpec(ivData));
	}

	private static int getCounter(int pos, int dataLength) {
		return pos / (BLOCK_SIZE_BYTES / dataLength);
	}

	private static int getBlockPos(int pos, int dataLength) {
		return pos % (BLOCK_SIZE_BYTES / dataLength);
	}

	public synchronized byte[] encryptInteger(int pos, int value, byte[] blockData) throws Exception {
		int dataLength = 4;
		boolean emptyBlock = Arrays.equals(blockData, new byte[blockData.length]);
		byte[] block = blockData;
		if (!emptyBlock) {
			initCypher(pos, dataLength, false);
			block = cipher.doFinal(blockData);
		}
		ByteBuffer buffer = ByteBuffer.wrap(block);
		int blockPos = getBlockPos(pos, dataLength);
		buffer.putInt(blockPos * dataLength, value);
		initCypher(pos, dataLength, true);
		return cipher.doFinal(block);
	}

	public synchronized int decryptInteger(int pos, byte[] blockData) throws Exception {
		int dataLength = 4;
		boolean emptyBlock = Arrays.equals(blockData, new byte[blockData.length]);
		if (emptyBlock) {
			return 0;
		}
		initCypher(pos, dataLength, false);
		int blockPos = getBlockPos(pos, dataLength);
		byte[] bytes = cipher.doFinal(blockData);
		ByteBuffer buf = ByteBuffer.wrap(bytes);
		return buf.getInt(blockPos * dataLength);
	}

	public static byte[] getBlockData(int pos, int dataLength, byte[] data) {
		int counter = getCounter(pos, dataLength);
		int offset = counter * BLOCK_SIZE_BYTES;
		return Arrays.copyOfRange(data, offset, offset + BLOCK_SIZE_BYTES);
	}

	private static void mergeBlockData(int pos, int dataLength, byte[] data, byte[] blockData) {
		int counter = getCounter(pos, dataLength);
		int offset = counter * BLOCK_SIZE_BYTES;
		System.arraycopy(blockData, 0, data, offset, blockData.length);
	}


	public static void main(String[] args) throws Exception {
		byte[] data = new byte[4_0_000_000];
		AesRaBlockCipher cipher = new AesRaBlockCipher("the key");

		long time = System.currentTimeMillis();
		for (int i = 0; i < 10_000_000; i += 1) {
			byte[] blockData = getBlockData(i, 4, data);
			byte[] encryptedBlockData = cipher.encryptInteger(i, i, blockData);
			mergeBlockData(i, 4, data, encryptedBlockData);
		}
		System.out.println("TIME ENC:" + (System.currentTimeMillis() - time));
		time = System.currentTimeMillis();
		for (int i = 0; i < 10_000_000; i++) {
			int value = cipher.decryptInteger(i, getBlockData(i, 4, data));
			//System.out.println(i + " -> " + value);
		}
		System.out.println("TIME DEC:" + (System.currentTimeMillis() - time));


	}


}
