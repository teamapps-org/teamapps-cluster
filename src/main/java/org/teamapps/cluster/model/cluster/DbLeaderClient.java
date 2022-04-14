package org.teamapps.cluster.model.cluster;

import org.teamapps.cluster.dto.*;
import org.teamapps.cluster.service.*;
import reactor.core.publisher.Mono;

import java.io.IOException;

public class DbLeaderClient extends AbstractClusterServiceClient {

    public DbLeaderClient(ServiceRegistry registry) {
        super(registry, "DbLeader");
    }

	public Mono<DbTransactionList> requestMissingTransactions(DbTransactionListRequest value) {
		return createClusterTask("requestMissingTransactions", value, DbTransactionList.getMessageDecoder());
	}



}