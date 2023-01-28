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
		MessageDefinition clusterAvailableServicesUpdate = modelCollection.createModel("clusterAvailableServicesUpdate", "cluster.clusterAvailableServicesUpdate");
		MessageDefinition clusterConnectionRequest = modelCollection.createModel("clusterConnectionRequest", "cluster.clusterConnectionRequest");
		MessageDefinition clusterConnectionResult = modelCollection.createModel("clusterConnectionResult", "cluster.clusterConnectionResult");
		MessageDefinition clusterServiceMethodRequest = modelCollection.createModel("clusterServiceMethodRequest", "cluster.clusterServiceMethodRequest");
		MessageDefinition clusterServiceMethodResult = modelCollection.createModel("clusterServiceMethodResult", "cluster.clusterServiceMethodResult");
		MessageDefinition clusterNodeSystemInfo = modelCollection.createModel("clusterNodeSystemInfo", "cluster.clusterNodeSystemInfo");
		EnumDefinition clusterServiceMethodErrorType = modelCollection.createEnum("clusterServiceMethodErrorType", "NETWORK_ERROR", "SERVICE_EXCEPTION");

		clusterNodeSystemInfo.addString("detailedInfo", 1);
		clusterNodeSystemInfo.addInteger("cpus", 2);
		clusterNodeSystemInfo.addInteger("cores", 3);
		clusterNodeSystemInfo.addInteger("threads", 4);
		clusterNodeSystemInfo.addLong("memorySize", 5);


		clusterNewPeerInfo.addSingleReference("newPeer", clusterNodeData, 1);

		clusterAvailableServicesUpdate.addStringArray("services", 1);

		clusterServiceMethodRequest.addLong("clusterTaskId", 1);
		clusterServiceMethodRequest.addString("serviceName", 2);
		clusterServiceMethodRequest.addString("methodName", 3);
		clusterServiceMethodRequest.addGenericMessage("requestMessage", 4);

		clusterServiceMethodResult.addLong("clusterTaskId", 1);
		clusterServiceMethodResult.addString("serviceName", 2);
		clusterServiceMethodResult.addString("methodName", 3);
		clusterServiceMethodResult.addGenericMessage("resultMessage", 4);
		clusterServiceMethodResult.addBoolean("error", 5);
		clusterServiceMethodResult.addEnum("errorType", clusterServiceMethodErrorType, 6);
		clusterServiceMethodResult.addString("errorMessage", 7);
		clusterServiceMethodResult.addString("errorStackTrace", 8);

		clusterConnectionRequest.addSingleReference("localNode", clusterNodeData, 1);
		clusterConnectionRequest.addStringArray("localServices", 2);

		clusterConnectionResult.addSingleReference("localNode", clusterNodeData, 1);
		clusterConnectionResult.addBoolean("accepted", 2);
		clusterConnectionResult.addMultiReference("knownPeers", clusterNodeData, 3);
		clusterConnectionResult.addStringArray("localServices", 4);
		clusterConnectionResult.addStringArray("knownServices", 5);


		clusterInfo.addSingleReference("localNode", clusterNodeData, 1);
		clusterInfo.addMultiReference("peerNodes", clusterNodeData, 2);

		clusterConfig.addString("clusterSecret", 1);
		clusterConfig.addString("nodeId", 2);
		clusterConfig.addString("host", 3);
		clusterConfig.addInteger("port", 4);
		clusterConfig.addMultiReference("peerNodes", clusterNodeData, 5);

		clusterNodeData.addString("nodeId", 1);
		clusterNodeData.addString("host", 2);
		clusterNodeData.addInteger("port", 3);

		clusterMessageFilePart.addString("fileId", 1);
		clusterMessageFilePart.addLong("totalLength", 2);
		clusterMessageFilePart.addBoolean("initialMessage", 3);
		clusterMessageFilePart.addBoolean("lastMessage", 4);
		clusterMessageFilePart.addByteArray("data", 5);

		ServiceProtocol systemInfo = modelCollection.createService("systemInfo");

		ServiceProtocol clusterService = modelCollection.createService("clusterInfoService");
		clusterService.addMethod("getHostInfo", clusterNodeData, clusterNodeData);

		return modelCollection;
	}
}
