/*-
 * ========================LICENSE_START=================================
 * TeamApps Cluster
 * ---
 * Copyright (C) 2021 - 2025 TeamApps.org
 * ---
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
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
		MessageDefinition clusterServiceBroadcastMessage = modelCollection.createModel("clusterServiceBroadcastMessage", "cluster.clusterServiceBroadcastMessage");
		MessageDefinition clusterLoadInfo = modelCollection.createModel("clusterLoadInfo", "cluster.clusterLoadInfo");

		MessageDefinition clusterNodeSystemInfo = modelCollection.createModel("clusterNodeSystemInfo", "cluster.clusterNodeSystemInfo");
		MessageDefinition clusterNewLeaderInfo = modelCollection.createModel("clusterNewLeaderInfo", "cluster.clusterNewLeaderInfo");
		EnumDefinition clusterServiceMethodErrorType = modelCollection.createEnum("clusterServiceMethodErrorType", "NETWORK_ERROR", "SERVICE_EXCEPTION");

		clusterLoadInfo.addInteger("load", 1);

		clusterNodeSystemInfo.addString("detailedInfo", 1);
		clusterNodeSystemInfo.addInteger("cpus", 2);
		clusterNodeSystemInfo.addInteger("cores", 3);
		clusterNodeSystemInfo.addInteger("threads", 4);
		clusterNodeSystemInfo.addLong("memorySize", 5);

		clusterNewLeaderInfo.addSingleReference("leaderNode", clusterNodeData, 1);

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

		clusterServiceBroadcastMessage.addString("serviceName", 1);
		clusterServiceBroadcastMessage.addString("methodName", 2);
		clusterServiceBroadcastMessage.addGenericMessage("message",3);

		clusterConnectionRequest.addSingleReference("localNode", clusterNodeData, 1);
		clusterConnectionRequest.addStringArray("localServices", 2);
		clusterConnectionRequest.addSingleReference("leaderNode", clusterNodeData, 3);
		clusterConnectionRequest.addMultiReference("knownPeers", clusterNodeData, 4);


		clusterConnectionResult.addSingleReference("localNode", clusterNodeData, 1);
		clusterConnectionResult.addBoolean("accepted", 2);
		clusterConnectionResult.addSingleReference("leaderNode", clusterNodeData, 3);
		clusterConnectionResult.addMultiReference("knownPeers", clusterNodeData, 4);
		clusterConnectionResult.addStringArray("localServices", 5);
		clusterConnectionResult.addStringArray("knownServices", 6);


		clusterInfo.addSingleReference("localNode", clusterNodeData, 1);
		clusterInfo.addMultiReference("peerNodes", clusterNodeData, 2);

		clusterConfig.addString("clusterSecret", 1);
		clusterConfig.addString("nodeId", 2);
		clusterConfig.addString("host", 3);
		clusterConfig.addInteger("port", 4);
		clusterConfig.addBoolean("leaderNode", 5);
		clusterConfig.addMultiReference("peerNodes", clusterNodeData, 6);

		clusterNodeData.addString("nodeId", 1);
		clusterNodeData.addString("host", 2);
		clusterNodeData.addInteger("port", 3);
		clusterNodeData.addBoolean("leaderNode", 4);

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
