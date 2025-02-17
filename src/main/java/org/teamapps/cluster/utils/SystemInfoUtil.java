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

import org.teamapps.cluster.message.protocol.ClusterNodeSystemInfo;
import org.teamapps.commons.formatter.FileSizeFormatter;
import oshi.SystemInfo;
import oshi.hardware.*;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SystemInfoUtil {


	public static ClusterNodeSystemInfo createNodeInfo() {
		SystemInfo si = new SystemInfo();
		HardwareAbstractionLayer hardware = si.getHardware();
		CentralProcessor processor = hardware.getProcessor();
		return new ClusterNodeSystemInfo()
				.setDetailedInfo(getSystemInfoMessage())
				.setCpus(processor.getPhysicalPackageCount())
				.setCores(processor.getPhysicalProcessorCount())
				.setThreads(processor.getLogicalProcessorCount())
				.setMemorySize(hardware.getMemory().getTotal());
	}

	public static String getSystemInfoMessage() {
		StringBuilder sb = new StringBuilder();
		SystemInfo si = new SystemInfo();
		HardwareAbstractionLayer hardware = si.getHardware();
		OperatingSystem operatingSystem = si.getOperatingSystem();

		CentralProcessor processor = hardware.getProcessor();
		addLine(sb, 0, "Processor", null);
		addLine(sb, 1, "Identifier", processor.getProcessorIdentifier());
		addLine(sb, 1, "Processor-ID", processor.getProcessorIdentifier().getProcessorID());
		addLine(sb, 1, "CPUs", processor.getPhysicalPackageCount());
		addLine(sb, 1, "Cores", processor.getPhysicalProcessorCount());
		addLine(sb, 1, "Threads", processor.getLogicalProcessorCount());

		GlobalMemory memory = hardware.getMemory();
		addLine(sb, 0, "Memory", null);
		addLine(sb, 1, "Total",  FileSizeFormatter.humanReadableByteCount(memory.getTotal(), false, 1));
		addLine(sb, 1, "Available",  FileSizeFormatter.humanReadableByteCount(memory.getAvailable(), false, 1));

		ComputerSystem computerSystem = hardware.getComputerSystem();
		addLine(sb, 0, "System", null);
		addLine(sb, 1, "Manufacturer",  computerSystem.getManufacturer());
		addLine(sb, 1, "Hardware-ID",  computerSystem.getHardwareUUID());
		addLine(sb, 1, "Serial-No",  computerSystem.getSerialNumber());
		addLine(sb, 1, "Firmware",  computerSystem.getFirmware());

		List<NetworkIF> networkIFs = hardware.getNetworkIFs();
		addLine(sb, 0, "Network", null);
		for (int i = 0; i < networkIFs.size(); i++) {
			NetworkIF networkIF = networkIFs.get(i);
			addLine(sb, 1, "Network card " + (i + 1),  null);
			addLine(sb, 2, "Name", networkIF.getName());
			addLine(sb, 2, "Display name", networkIF.getDisplayName());
			addLine(sb, 2, "IPv4", networkIF.getIPv4addr());
			addLine(sb, 2, "IPv6", networkIF.getIPv6addr());
			addLine(sb, 2, "Mac", networkIF.getMacaddr());
		}

		List<HWDiskStore> diskStores = hardware.getDiskStores();
		addLine(sb, 0, "Storage", null);
		for (int i = 0; i < diskStores.size(); i++) {
			HWDiskStore store = diskStores.get(i);
			addLine(sb, 1, "Hard disc " + (i + 1),  null);
			addLine(sb, 2, "Size", FileSizeFormatter.humanReadableByteCount(store.getSize(), false, 1));
			addLine(sb, 2, "Model", store.getModel());
			addLine(sb, 2, "Name", store.getName());
			addLine(sb, 2, "Serial", store.getSerial());
		}

		OperatingSystem.OSVersionInfo versionInfo = operatingSystem.getVersionInfo();
		addLine(sb, 0, "OS", null);
		addLine(sb, 1, "Manufacturer",  operatingSystem.getManufacturer());
		addLine(sb, 1, "Family",  operatingSystem.getFamily());
		addLine(sb, 1, "Version",  versionInfo.getVersion());
		addLine(sb, 1, "Code name",  versionInfo.getCodeName());
		addLine(sb, 1, "Build",  versionInfo.getBuildNumber());
		addLine(sb, 1, "System boot", Instant.ofEpochSecond(operatingSystem.getSystemBootTime()));


		FileSystem fileSystem = operatingSystem.getFileSystem();
		List<OSFileStore> fileStores = fileSystem.getFileStores();
		addLine(sb, 0, "File system", null);
		addLine(sb, 1, "Max file descriptors per process",  fileSystem.getMaxFileDescriptorsPerProcess());
		addLine(sb, 1, "Max file descriptors",  fileSystem.getMaxFileDescriptors());
		addLine(sb, 1, "Open file descriptors",  fileSystem.getOpenFileDescriptors());

		for (int i = 0; i < fileStores.size(); i++) {
			OSFileStore store = fileStores.get(i);
			addLine(sb, 1, "File store " + (i + 1),  null);
			addLine(sb, 2, "Volume", store.getVolume());
			addLine(sb, 2, "Logical volume", store.getLogicalVolume());
			addLine(sb, 2, "Mount", store.getMount());
			addLine(sb, 2, "Label", store.getLabel());
			addLine(sb, 2, "Description", store.getDescription());
			addLine(sb, 2, "Total space", FileSizeFormatter.humanReadableByteCount(store.getTotalSpace(), false, 1));
			addLine(sb, 2, "Free space", FileSizeFormatter.humanReadableByteCount(store.getFreeSpace(), false, 1));
			addLine(sb, 2, "UUID", store.getUUID());
			addLine(sb, 2, "Type", store.getType());
		}

		return sb.toString();
	}

	private static void addLine(StringBuilder sb, int tabs, String key, Object value) {
		for (int i = 0; i < tabs; i++) {
			sb.append("\t");
		}
		sb.append(key).append(": ");
		if (value != null) {
			if (value instanceof String[]) {
				String[] arr = (String[]) value;
				sb.append(Arrays.stream(arr).collect(Collectors.joining(", ")));
			} else {
				sb.append(value.toString());
			}
		}
		sb.append("\n");
	}
}
