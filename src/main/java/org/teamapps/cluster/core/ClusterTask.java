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
package org.teamapps.cluster.core;

import org.teamapps.cluster.message.protocol.ClusterServiceMethodErrorType;
import org.teamapps.message.protocol.message.Message;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class ClusterTask {

	private static final AtomicLong taskIdGenerator = new AtomicLong();

	private final String fixedServiceNodeId;
	private final String serviceName;
	private final String method;
	private final Message request;
	private final long started;
	private final long taskId;
	private int maxExecutionAttempts = 3;
	private CompletableFuture<Void> completableFuture;
	private String processingNodeId;
	private int executionAttempts;
	private Message result;
	private volatile boolean error;
	private volatile boolean finished;
	private ClusterServiceMethodErrorType errorType;
	private String errorMessage;
	private String errorStackTrace;

	public ClusterTask(String serviceName, String method, Message request) {
		this(serviceName, method, request, null);
	}

	public ClusterTask(String serviceName, String method, Message request, String fixedServiceNodeId) {
		this.serviceName = serviceName;
		this.method = method;
		this.request = request;
		this.fixedServiceNodeId = fixedServiceNodeId;
		this.started = System.currentTimeMillis();
		this.taskId = taskIdGenerator.incrementAndGet();
		if (isFixedServiceNode()) {
			maxExecutionAttempts = 2;
		}
	}

	public boolean isFixedServiceNode() {
		return fixedServiceNodeId != null;
	}

	public String getFixedServiceNodeId() {
		return fixedServiceNodeId;
	}

	public void startProcessing() {
		this.completableFuture = new CompletableFuture<>();
	}

	public Message getRequest() {
		return request;
	}

	public long getStarted() {
		return started;
	}

	public long getTaskId() {
		return taskId;
	}

	public void addExecutionAttempt() {
		executionAttempts++;
	}

	public int getExecutionAttempts() {
		return executionAttempts;
	}

	public boolean isRetryLimitReached() {
		return executionAttempts >= maxExecutionAttempts;
	}

	public String getServiceName() {
		return serviceName;
	}

	public String getMethod() {
		return method;
	}

	public String getProcessingNodeId() {
		return processingNodeId;
	}

	public void setProcessingNodeId(String processingNodeId) {
		this.processingNodeId = processingNodeId;
	}

	public Message getResult() {
		return result;
	}

	public void setResult(Message result) {
		this.result = result;
		completableFuture.complete(null);
	}

	public void waitForResult() {
		try {
			completableFuture.get(90, TimeUnit.MINUTES);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public boolean isError() {
		return error;
	}

	public void setError(boolean error) {
		this.error = error;
	}

	public ClusterServiceMethodErrorType getErrorType() {
		return errorType;
	}

	public void setErrorType(ClusterServiceMethodErrorType errorType) {
		this.errorType = errorType;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public String getErrorStackTrace() {
		return errorStackTrace;
	}

	public void setErrorStackTrace(String errorStackTrace) {
		this.errorStackTrace = errorStackTrace;
	}

	public boolean isFinished() {
		return finished;
	}

	public void setFinished(boolean finished) {
		this.finished = finished;
	}
}
