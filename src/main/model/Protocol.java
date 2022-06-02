/*-
 * ========================LICENSE_START=================================
 * TeamApps Cluster
 * ---
 * Copyright (C) 2021 - 2022 TeamApps.org
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
import org.teamapps.protocol.schema.*;

public class Protocol implements ModelCollectionProvider {
	@Override
	public ModelCollection getModelCollection() {
		MessageModelCollection modelCollection = new MessageModelCollection("ClusterModel", "org.teamapps.cluster.protocol", 1);

		ObjectPropertyDefinition clusterMessageFilePart = modelCollection.createModel("clusterMessageFilePart", "#tac.cmfp");
		clusterMessageFilePart.addStringProperty("fileId", 1);
		clusterMessageFilePart.addLongProperty("totalLength", 2);
		clusterMessageFilePart.addBooleanProperty("initialMessage", 3);
		clusterMessageFilePart.addBooleanProperty("lastMessage", 4);
		clusterMessageFilePart.addByteArrayProperty("data", 5);

		ObjectPropertyDefinition nodeInfo = modelCollection.createModel("nodeInfo", "#tac.ni");
		nodeInfo.addStringProperty("nodeId", 1);
		nodeInfo.addBooleanProperty("leader", 2);
		nodeInfo.addStringProperty("host", 3);
		nodeInfo.addIntProperty("port", 4);
		nodeInfo.addBooleanProperty("reachable", 5);
		nodeInfo.addStringArrayProperty("services", 6);

		ObjectPropertyDefinition clusterInfo = modelCollection.createModel("clusterInfo", "#tac.ci");
		clusterInfo.addBooleanProperty("initialMessage", 1);
		clusterInfo.addBooleanProperty("response", 2);
		clusterInfo.addSingleReference("localNode", 3, nodeInfo);
		clusterInfo.addMultiReference("remoteNodes", 4, nodeInfo);

		ObjectPropertyDefinition clusterMethodExecution = modelCollection.createModel("clusterMethodExecution", "#tac.cme");
		clusterMethodExecution.addBooleanProperty("response", 1);
		clusterMethodExecution.addLongProperty("requestId", 2);
		clusterMethodExecution.addStringProperty("serviceName", 3);
		clusterMethodExecution.addStringProperty("serviceMethod", 4);
		clusterMethodExecution.addByteArrayProperty("data", 5);

		return modelCollection;
	}
}
