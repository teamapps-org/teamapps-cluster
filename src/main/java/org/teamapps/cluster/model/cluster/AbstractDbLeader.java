package org.teamapps.cluster.model.cluster;

import org.teamapps.cluster.dto.*;
import org.teamapps.cluster.service.*;
import java.io.IOException;

public abstract class AbstractDbLeader extends AbstractClusterService {

    public AbstractDbLeader(ServiceRegistry registry) {
        super(registry, "DbLeader");
    }

	public abstract DbTransactionList requestMissingTransactions(DbTransactionListRequest value);



	@Override
	public byte[] handleMessage(String method, byte[] bytes, FileProvider fileProvider, FileSink fileSink) throws IOException {
		switch (method) {
			case "requestMissingTransactions" -> {
				return requestMissingTransactions( new DbTransactionListRequest(bytes, fileProvider)).toBytes(fileSink);
			}

		}
		return null;
	}

}