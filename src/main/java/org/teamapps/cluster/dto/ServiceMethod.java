package org.teamapps.cluster.dto;

public class ServiceMethod {

	private final String methodName;
	private final MessageField inputMessage;
	private final MessageField outputMessage;

	public ServiceMethod(String methodName, MessageField inputMessage, MessageField outputMessage) {
		this.methodName = methodName;
		this.inputMessage = inputMessage;
		this.outputMessage = outputMessage;
	}

	public MessageField getInputMessage() {
		return inputMessage;
	}

	public MessageField getOutputMessage() {
		return outputMessage;
	}

	public String getMethodName() {
		return methodName;
	}
}
