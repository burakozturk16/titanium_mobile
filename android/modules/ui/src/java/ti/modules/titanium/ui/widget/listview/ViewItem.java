/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

package ti.modules.titanium.ui.widget.listview;

import java.util.HashMap;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.titanium.proxy.TiViewProxy;

public class ViewItem {
	TiViewProxy viewProxy;
	KrollDict initialProperties;
	KrollDict currentProperties;
	KrollDict diffProperties;
	
	public ViewItem(TiViewProxy viewProxy, KrollDict props) {
		initialProperties = new KrollDict((HashMap<String, Object>)props.clone());
		this.viewProxy = viewProxy;
		diffProperties = new KrollDict();
		currentProperties = new KrollDict();
	}
	
	public TiViewProxy getViewProxy() {
		return viewProxy;
	}
	
	/**
	 * This method compares applied properties of a view and our data model to
	 * generate a new set of properties we need to set. It is crucial for scrolling performance. 
	 * @param properties The properties from our data model
	 * @return The difference set of properties to set
	 */
	public KrollDict generateDiffProperties(KrollDict properties) {
		diffProperties.clear();

		for (String appliedProp : currentProperties.keySet()) {
			if (!properties.containsKey(appliedProp)) {
				applyProperty(appliedProp, initialProperties.get(appliedProp));
			}
		}
		
		for (String property : properties.keySet()) {
			Object value = properties.get(property);

			Object existingVal = currentProperties.get(property);			
			if (existingVal == null || value == null || !existingVal.equals(value)) {
				applyProperty(property, value);
			}
		}
		return diffProperties;
		
	}
	
	private void applyProperty(String key, Object value) {
		diffProperties.put(key, value);
		currentProperties.put(key, value);
	}
	
	
}