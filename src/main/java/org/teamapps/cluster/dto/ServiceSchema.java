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
package org.teamapps.cluster.dto;

import java.util.ArrayList;
import java.util.List;

public class ServiceSchema {

	private final String serviceName;
	private final List<ServiceMethod> serviceMethods = new ArrayList<>();

	public ServiceSchema(String serviceName) {
		this.serviceName = serviceName;
	}

	public ServiceSchema addMethod(ServiceMethod method) {
		serviceMethods.add(method);
		return this;
	}

	public ServiceSchema addMethod(String methodName, MessageField inputMessage, MessageField outputMessage) {
		return addMethod(new ServiceMethod(methodName, inputMessage, outputMessage));
	}

	public String getServiceName() {
		return serviceName;
	}

	public List<ServiceMethod> getServiceMethods() {
		return serviceMethods;
	}
}
