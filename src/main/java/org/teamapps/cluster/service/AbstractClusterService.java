package org.teamapps.cluster.service;

import org.teamapps.cluster.dto.FileProvider;
import org.teamapps.cluster.dto.FileSink;

import java.io.IOException;

public abstract class AbstractClusterService {

	private final TeamAppsCluster cluster;
	private final String serviceName;

	public AbstractClusterService(TeamAppsCluster cluster, String serviceName) {
		this.cluster = cluster;
		this.serviceName = serviceName;
		cluster.registerService(this);
	}

	public String getServiceName() {
		return serviceName;
	}

	public abstract byte[] handleMessage(String method, byte[] bytes, FileProvider fileProvider, FileSink fileSink) throws IOException;
}
