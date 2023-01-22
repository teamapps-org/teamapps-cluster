import org.teamapps.message.protocol.message.MessageDefinition;
import org.teamapps.message.protocol.message.MessageModelCollection;
import org.teamapps.message.protocol.model.EnumDefinition;
import org.teamapps.message.protocol.model.ModelCollection;
import org.teamapps.message.protocol.model.ModelCollectionProvider;
import org.teamapps.message.protocol.service.ServiceProtocol;

public class MessageProtocol implements ModelCollectionProvider {
	@Override
	public ModelCollection getModelCollection() {
		MessageModelCollection modelCollection = new MessageModelCollection("clusterMessageProtocol", "org.teamapps.cluster.message.protocol", 1);

		MessageDefinition clusterConfig = modelCollection.createModel("clusterConfig", "cluster.clusterConfig");
		MessageDefinition clusterNodeData = modelCollection.createModel("clusterNodeData", "cluster.clusterNodeData");
		MessageDefinition clusterMessageFilePart = modelCollection.createModel("clusterMessageFilePart", "cluster.clusterMessageFilePart");
		MessageDefinition clusterInfo = modelCollection.createModel("clusterInfo", "cluster.clusterInfo");
		MessageDefinition clusterNewPeerInfo = modelCollection.createModel("clusterNewPeerInfo", "cluster.clusterNewPeerInfo");
		MessageDefinition clusterNodeShutDownInfo = modelCollection.createModel("clusterNodeShutDownInfo", "cluster.clusterNodeShutDownInfo");

		MessageDefinition clusterConnectionRequest = modelCollection.createModel("clusterConnectionRequest", "cluster.clusterConnectionRequest");
		MessageDefinition clusterConnectionResult = modelCollection.createModel("clusterConnectionResult", "cluster.clusterConnectionResult");

		MessageDefinition clusterServiceMethodRequest = modelCollection.createModel("clusterServiceMethodRequest", "cluster.clusterServiceMethodRequest");
		MessageDefinition clusterServiceMethodResult = modelCollection.createModel("clusterServiceMethodResult", "cluster.clusterServiceMethodResult");
		EnumDefinition clusterServiceMethodErrorType = modelCollection.createEnum("clusterServiceMethodErrorType", "NETWORK_ERROR", "SERVICE_EXCEPTION");

		clusterNewPeerInfo.addSingleReference("newPeer", clusterNodeData, 1);

		clusterServiceMethodRequest.addLongAttribute("clusterTaskId", 1);
		clusterServiceMethodRequest.addStringAttribute("serviceName", 2);
		clusterServiceMethodRequest.addStringAttribute("methodName", 3);
		clusterServiceMethodRequest.addGenericMessageAttribute("requestMessage", 4);

		clusterServiceMethodResult.addStringAttribute("clusterTaskId", 1);
		clusterServiceMethodResult.addStringAttribute("serviceName", 2);
		clusterServiceMethodResult.addStringAttribute("methodName", 3);
		clusterServiceMethodResult.addGenericMessageAttribute("resultMessage", 4);
		clusterServiceMethodResult.addBooleanAttribute("error", 5);
		clusterServiceMethodResult.addEnum("errorType", clusterServiceMethodErrorType, 6);
		clusterServiceMethodResult.addStringAttribute("errorMessage", 7);
		clusterServiceMethodResult.addStringAttribute("errorStackTrace", 8);

		clusterConnectionRequest.addSingleReference("localNode", clusterNodeData, 1);

		clusterConnectionResult.addSingleReference("localNode", clusterNodeData, 1);
		clusterConnectionResult.addBooleanAttribute("accepted", 2);
		clusterConnectionResult.addMultiReference("knownPeers", clusterNodeData, 3);


		clusterInfo.addSingleReference("localNode", clusterNodeData, 1);
		clusterInfo.addMultiReference("peerNodes", clusterNodeData, 2);

		clusterConfig.addStringAttribute("clusterSecret", 1);
		clusterConfig.addStringAttribute("nodeId", 2);
		clusterConfig.addStringAttribute("host", 3);
		clusterConfig.addIntAttribute("port", 4);
		clusterConfig.addMultiReference("peerNodes", clusterNodeData, 5);

		clusterNodeData.addStringAttribute("nodeId", 1);
		clusterNodeData.addStringAttribute("host", 2);
		clusterNodeData.addIntAttribute("port", 3);

		clusterMessageFilePart.addStringAttribute("fileId", 1);
		clusterMessageFilePart.addLongAttribute("totalLength", 2);
		clusterMessageFilePart.addBooleanAttribute("initialMessage", 3);
		clusterMessageFilePart.addBooleanAttribute("lastMessage", 4);
		clusterMessageFilePart.addByteArrayAttribute("data", 5);

		ServiceProtocol clusterService = modelCollection.createService("clusterInfoService");
		clusterService.addMethod("getHostInfo", clusterNodeData, clusterNodeData);

		return modelCollection;
	}
}
