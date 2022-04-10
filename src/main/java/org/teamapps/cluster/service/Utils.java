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
package org.teamapps.cluster.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class Utils {

	public static <T> T randomListEntry(List<T> list) {
		if (list == null || list.isEmpty()) {
			return null;
		}
		int id = randomValue(0, list.size());
		return list.get(id);
	}

	public static int randomValue(int minInclusive, int maxExclusive) {
		int value = -1;
		while (value < minInclusive || value >= maxExclusive) {
			value = (int) (maxExclusive * Math.random()) + minInclusive;
		}
		return value;
	}

	public static File createTempDir() {
		try {
			return Files.createTempFile("temp", "temp").getParent().toFile();
		} catch (IOException e) {
			throw new RuntimeException("Unable to create temp dir!");
		}
	}
}
