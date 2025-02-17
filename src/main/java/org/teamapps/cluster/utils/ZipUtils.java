/*-
 * ========================LICENSE_START=================================
 * TeamApps Cluster
 * ---
 * Copyright (C) 2021 - 2025 TeamApps.org
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
package org.teamapps.cluster.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipUtils {

	public static void createZipFromDirectory(File sourceDirectory, File zipFile) throws IOException {
		Path sourceFolderPath = sourceDirectory.toPath();
		ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile));
		Files.walkFileTree(sourceFolderPath, new SimpleFileVisitor<>() {
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				zos.putNextEntry(new ZipEntry(sourceFolderPath.relativize(file).toString()));
				Files.copy(file, zos);
				zos.closeEntry();
				return FileVisitResult.CONTINUE;
			}
		});
		zos.close();
	}

	public static void unzipToDirectory(File zipFile, File destDir) throws IOException {
		final byte[] buffer = new byte[1024];
		final ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
		ZipEntry zipEntry = zis.getNextEntry();
		while (zipEntry != null) {
			File destFile = new File(destDir, zipEntry.getName());
			String destDirPath = destDir.getCanonicalPath();
			String destFilePath = destFile.getCanonicalPath();
			if (!destFilePath.startsWith(destDirPath + File.separator)) {
				throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
			}
			if (zipEntry.isDirectory()) {
				if (!destFile.isDirectory() && !destFile.mkdirs()) {
					throw new IOException("Failed to create directory " + destFile);
				}
			} else {
				File parent = destFile.getParentFile();
				if (!parent.isDirectory() && !parent.mkdirs()) {
					throw new IOException("Failed to create directory " + parent);
				}
				final FileOutputStream fos = new FileOutputStream(destFile);
				int len;
				while ((len = zis.read(buffer)) > 0) {
					fos.write(buffer, 0, len);
				}
				fos.close();
			}
			zipEntry = zis.getNextEntry();
		}
		zis.closeEntry();
		zis.close();
	}

}
