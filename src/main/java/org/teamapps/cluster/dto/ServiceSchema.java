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
