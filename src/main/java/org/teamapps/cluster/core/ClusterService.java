package org.teamapps.cluster.core;

import org.teamapps.protocol.schema.MessageObject;
import org.teamapps.protocol.schema.PojoObjectDecoder;

public interface ClusterService {

	<REQUEST extends MessageObject, RESPONSE extends MessageObject> RESPONSE createServiceTask(String serviceMethod, REQUEST request, PojoObjectDecoder<RESPONSE> responseDecoder);
}
