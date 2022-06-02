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
