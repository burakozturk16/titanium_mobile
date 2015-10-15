/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

package ti.modules.titanium.ui.widget.abslistview;

import java.util.HashMap;
import java.util.Iterator;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollProxy;

public class ProxyAbsListItem {
	KrollProxy proxy;
	KrollDict initialProperties;
	KrollDict currentProperties;
	KrollDict diffProperties;
	
	public ProxyAbsListItem(KrollProxy proxy, KrollDict props) {
		initialProperties = (KrollDict)props.clone();
		this.proxy = proxy;
		diffProperties = new KrollDict();
		currentProperties = new KrollDict();
	}
	
	public KrollProxy getProxy() {
		return proxy;
	}
	
	/**
	 * This method compares applied properties of a view and our data model to
	 * generate a new set of properties we need to set. It is crucial for scrolling performance. 
	 * @param properties The properties from our data model
	 * @return The difference set of properties to set
	 */
	public KrollDict generateDiffProperties(HashMap properties) {
		diffProperties.clear();
		Iterator<String> it = currentProperties.keySet().iterator();
		while (it.hasNext())
		{
			String appliedProp = it.next();
			if (properties == null || !properties.containsKey(appliedProp)) {
				applyProperty(appliedProp, initialProperties.get(appliedProp), it);
			}
		}
		if (properties != null) { 
			it = properties.keySet().iterator();
			while (it.hasNext())
			{
				String property = it.next();
				Object value = properties.get(property);
				Object existingVal = currentProperties.get(property);			
				if (existingVal != value && (existingVal == null || value == null || !existingVal.equals(value))) {
					applyProperty(property, value, it);
				}
			}
		}
		return diffProperties;
		
	}
	
	private void applyProperty(String key, Object value, Iterator<String> it) {
		diffProperties.put(key, value);
		if (value == null)
			it.remove();
		else
			currentProperties.put(key, value);
	}
	
	public void setCurrentProperty(String key, Object value) {
	    currentProperties.put(key, value);
    }

	public boolean containsKey(String key) {
		return initialProperties.containsKey(key);
	}
	
	
}