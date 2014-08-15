package org.appcelerator.titanium;

import android.app.Service;
import android.os.Binder;

public class TiServiceBinder extends Binder {
	private Service service;
	public TiServiceBinder(Service service) {
		this.service = service;
	}
	public Service getService() {
		return this.service;
	}
}
