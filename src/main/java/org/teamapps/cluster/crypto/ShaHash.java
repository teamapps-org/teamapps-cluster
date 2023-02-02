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
