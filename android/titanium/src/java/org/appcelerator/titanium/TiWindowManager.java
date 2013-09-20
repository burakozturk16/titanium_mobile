package org.appcelerator.titanium;

import org.appcelerator.titanium.proxy.TiWindowProxy;

public interface TiWindowManager {
	public boolean handleClose(TiWindowProxy proxy, Object arg);
	public boolean handleOpen(TiWindowProxy proxy, Object arg);
}
