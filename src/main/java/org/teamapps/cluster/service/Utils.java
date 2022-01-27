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
