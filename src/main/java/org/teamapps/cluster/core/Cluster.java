package org.teamapps.cluster.core;

import org.teamapps.protocol.schema.MessageObject;
import org.teamapps.protocol.schema.ModelCollection;
import org.teamapps.protocol.schema.PojoObjectDecoder;

import java.util.List;

public interface Cluster extends ClusterHandler {

	void addModelCollection(ModelCollection modelCollection);

	LocalNode getLocalNode();

	List<RemoteNode> getRemoteNodes();

	void addRemoteNode(RemoteNode remoteNode);

	RemoteNode getRemoteNode(String nodeId);

	boolean isServiceAvailable(String serviceName);

	void registerService(String serviceName, ClusterService service);

	void removeService(String serviceName);

	void sendMessage(MessageObject message, String nodeId);

	void sendTopicMessage(String topic, MessageObject message);

	void handleMessage(MessageObject message, RemoteNode node);

	void handleTopicMessage(String topic, MessageObject message, RemoteNode node);

	<REQUEST extends MessageObject, RESPONSE extends MessageObject> RESPONSE executeClusterServiceMethod(String service, String serviceMethod, REQUEST request, PojoObjectDecoder<RESPONSE> responseDecoder);

	MessageObject handleClusterServiceMethod(String service, String serviceMethod, MessageObject requestData);

}
