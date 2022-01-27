package org.teamapps.cluster.service;

import org.teamapps.cluster.dto.FileProvider;
import org.teamapps.cluster.dto.FileSink;

import java.io.IOException;

public abstract class AbstractClusterService {

	private final ServiceRegistry serviceRegistry;
	private final String serviceName;

	public AbstractClusterService(ServiceRegistry serviceRegistry, String serviceName) {
		this.serviceRegistry = serviceRegistry;
		this.serviceName = serviceName;
		serviceRegistry.registerService(this);
	}

	public String getServiceName() {
		return serviceName;
	}

	public abstract byte[] handleMessage(String method, byte[] bytes, FileProvider fileProvider, FileSink fileSink) throws IOException;
}
