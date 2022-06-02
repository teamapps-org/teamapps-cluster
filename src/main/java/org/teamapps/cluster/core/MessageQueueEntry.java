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
package org.teamapps.cluster.core;

import org.teamapps.protocol.schema.MessageObject;

public class MessageQueueEntry {

	private final long timestamp;
	private final boolean resendOnError;
	private final boolean serviceExecution;
	private final MessageObject message;
	private String serviceName;
	private String serviceMethod;
	private long requestId;
	private boolean serviceResponse;

	public MessageQueueEntry(boolean resendOnError, MessageObject message) {
		this.timestamp = System.currentTimeMillis();
		this.resendOnError = resendOnError;
		this.serviceExecution = false;
		this.message = message;
	}

	public MessageQueueEntry(boolean resendOnError, boolean serviceExecution, MessageObject message, String serviceName, String serviceMethod, boolean serviceResponse, long requestId) {
		this.timestamp = System.currentTimeMillis();
		this.resendOnError = resendOnError;
		this.serviceExecution = serviceExecution;
		this.message = message;
		this.serviceName = serviceName;
		this.serviceMethod = serviceMethod;
		this.serviceResponse = serviceResponse;
		this.requestId = requestId;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public boolean isResendOnError() {
		return resendOnError;
	}

	public MessageObject getMessage() {
		return message;
	}

	public boolean isServiceExecution() {
		return serviceExecution;
	}

	public String getServiceName() {
		return serviceName;
	}

	public String getServiceMethod() {
		return serviceMethod;
	}

	public boolean isServiceResponse() {
		return serviceResponse;
	}

	public long getRequestId() {
		return requestId;
	}
}
