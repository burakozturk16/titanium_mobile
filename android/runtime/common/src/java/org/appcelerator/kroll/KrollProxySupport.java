/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package org.appcelerator.kroll;

/**
 * This interface represents the various APIs needed by KrollRuntime
 * on a specific Proxy instance (and hides KrollProxy)
 */
public interface KrollProxySupport
{
	/**
	 * Implementing classes should use this method to modify an existing property.
	 * @param name the property name.
	 * @param value the replacing value.
	 */
	public void onPropertyChanged(String name, Object value);

	public void onPropertiesChanged(Object[][] changes);

	/**
	 * Implementing classes should return the corresponding KrollObject associated with it.
	 * @return the KrollObject object.
	 */
	public KrollObject getKrollObject();
	
	/**
	 * Implementing classes should set its KrollObject to object.
	 * @param object the KrollObject to be set.
	 */
	public void setKrollObject(KrollObject object);

	public Object getIndexedProperty(int index);

	public void setIndexedProperty(int index, Object value);

	/**
	 * Implementing classes should use this method when an eventListener is added or removed from event
	 * @param event the event whose eventListener has been added or removed.
	 * @param hasListeners If this is true, the eventListener has been added to event, and vice versa.
	 */
	public void onHasListenersChanged(String event, boolean hasListeners);
	
	
	public void onEventFired(String event, Object data);
}
