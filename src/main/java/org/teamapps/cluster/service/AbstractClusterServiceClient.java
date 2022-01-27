package org.teamapps.cluster.service;

import org.teamapps.cluster.dto.Message;
import org.teamapps.cluster.dto.MessageDecoder;
import reactor.core.publisher.Mono;

public abstract class AbstractClusterServiceClient {

	private final ServiceRegistry serviceRegistry;
	private final String serviceName;

	public AbstractClusterServiceClient(ServiceRegistry serviceRegistry, String serviceName) {
		this.serviceRegistry = serviceRegistry;
		this.serviceName = serviceName;
	}

	protected <REQUEST extends Message, RESPONSE extends Message> Mono<RESPONSE> createClusterTask(String method, REQUEST request, MessageDecoder<RESPONSE> responseDecoder) {
		return serviceRegistry.createServiceTask(serviceName, method, request, responseDecoder);
	}

	public boolean isAvailable() {
		return serviceRegistry.isServiceAvailable(serviceName);
	}

}
