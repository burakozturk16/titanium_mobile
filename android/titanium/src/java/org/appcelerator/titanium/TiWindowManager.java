package org.appcelerator.titanium;

import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.titanium.proxy.TiWindowProxy;

public interface TiWindowManager {
	public boolean handleClose(TiWindowProxy proxy, Object arg);
	public boolean handleOpen(TiWindowProxy proxy, Object arg);
	public boolean shouldExitOnClose();
	public void updateOrientationModes();
	public void onWindowActivityCreated();
	public KrollProxy getParentForBubbling(TiWindowProxy proxy);
}
