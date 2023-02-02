/*-
 * ========================LICENSE_START=================================
 * TeamApps Cluster
 * ---
 * Copyright (C) 2021 - 2023 TeamApps.org
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
		MessageModelCollection modelCollection = new MessageModelCollection("clusterTestMessageProtocol", "org.teamapps.cluster.message.test.protocol", 1);

		MessageDefinition testMethodRequest = modelCollection.createModel("testMethodRequest", "test.testMethodRequest");
		MessageDefinition testMethodResult = modelCollection.createModel("testMethodResult", "test.testMethodResult");

		testMethodRequest.addInteger("executionDelaySeconds", 1);
		testMethodRequest.addBoolean("throwException", 2);
		testMethodRequest.addString("answerPayload", 3);
		testMethodRequest.addString("failIfNodeId", 4);

		testMethodResult.addString("answerPayload", 1);
		testMethodResult.addString("executingNode", 2);

		ServiceProtocol clusterTest = modelCollection.createService("clusterTest");
		clusterTest.addMethod("testServiceMethod", testMethodRequest, testMethodResult);

		return modelCollection;
	}
}
