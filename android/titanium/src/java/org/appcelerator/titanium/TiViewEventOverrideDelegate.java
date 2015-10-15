package org.appcelerator.titanium;

import org.appcelerator.kroll.KrollProxy;

public interface TiViewEventOverrideDelegate {
    Object overrideEvent(Object data,  String type, KrollProxy proxy);

}
