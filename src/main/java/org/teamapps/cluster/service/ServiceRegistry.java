package org.teamapps.cluster.service;

import org.teamapps.cluster.dto.Message;
import org.teamapps.cluster.dto.MessageDecoder;
import reactor.core.publisher.Mono;

public interface ServiceRegistry {

	void registerService(AbstractClusterService clusterService);

	boolean isServiceAvailable(String serviceName);

	<REQUEST extends Message, RESPONSE extends Message> Mono<RESPONSE> createServiceTask(String serviceName, String method, REQUEST request, MessageDecoder<RESPONSE> responseDecoder);
}
