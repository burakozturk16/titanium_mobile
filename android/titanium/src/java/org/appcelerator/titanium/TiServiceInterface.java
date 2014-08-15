package org.appcelerator.titanium;

import org.appcelerator.titanium.proxy.ServiceProxy;

public interface TiServiceInterface {

	void start(ServiceProxy vpnServiceProxy);

	void unbindProxy(ServiceProxy vpnServiceProxy);
	public int nextServiceInstanceId();
	
	void stop();

}
