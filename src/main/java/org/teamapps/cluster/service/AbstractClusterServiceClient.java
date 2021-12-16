package org.teamapps.cluster.service;

import org.teamapps.cluster.dto.Message;
import org.teamapps.cluster.dto.MessageDecoder;
import reactor.core.publisher.Mono;

public abstract class AbstractClusterServiceClient {

	private final TeamAppsCluster cluster;
	private final String serviceName;

	public AbstractClusterServiceClient(TeamAppsCluster cluster, String serviceName) {
		this.cluster = cluster;
		this.serviceName = serviceName;
	}

	protected <REQUEST extends Message, RESPONSE extends Message> Mono<RESPONSE> createClusterTask(String method, REQUEST request, MessageDecoder<RESPONSE> responseDecoder) {
		return cluster.createServiceTask(serviceName, method, request, responseDecoder);
	}

}
