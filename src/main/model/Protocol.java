/*-
 * ========================LICENSE_START=================================
 * TeamApps Protocol Schema
 * ---
 * Copyright (C) 2022 TeamApps.org
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
		clusterMessageFilePart.addProperty("fileId", 1, PropertyType.STRING);
		clusterMessageFilePart.addProperty("totalLength", 2, PropertyType.LONG);
		clusterMessageFilePart.addProperty("initialMessage", 3, PropertyType.BOOLEAN);
		clusterMessageFilePart.addProperty("lastMessage", 4, PropertyType.BOOLEAN);
		clusterMessageFilePart.addProperty("data", 5, PropertyType.BYTE_ARRAY);



		return modelCollection;
	}
}
