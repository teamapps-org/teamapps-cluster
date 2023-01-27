package org.teamapps.cluster.core2;

import org.teamapps.cluster.message.protocol.ClusterServiceMethodErrorType;
import org.teamapps.message.protocol.message.Message;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class ClusterTask {

	private static final AtomicLong taskIdGenerator = new AtomicLong();

	private final String serviceName;
	private final String method;
	private final Message request;
	private final CompletableFuture<Void> completableFuture;
	private final long started;
	private final long taskId;
	private String processingNodeId;
	private int executionAttempts;
	private Message result;
	private volatile boolean error;
	private ClusterServiceMethodErrorType errorType;
	private String errorMessage;
	private String errorStackTrace;

	public ClusterTask(String serviceName, String method, Message request) {
		this.serviceName = serviceName;
		this.method = method;
		this.request = request;
		this.completableFuture = new CompletableFuture<>();
		this.started = System.currentTimeMillis();
		this.taskId = taskIdGenerator.incrementAndGet();
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

	public int getExecutionAttempts() {
		return executionAttempts;
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

	public void addExecutionAttempt() {
		executionAttempts++;
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
}
