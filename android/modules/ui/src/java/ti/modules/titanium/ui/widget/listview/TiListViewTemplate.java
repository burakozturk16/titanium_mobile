/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

package ti.modules.titanium.ui.widget.listview;

import java.util.HashMap;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;

public class TiListViewTemplate {
	
	protected static final String TAG = "TiTemplate";

	protected HashMap<String, DataItem> dataItems;
	
	public static final String DEFAULT_TEMPLATE = "defaultTemplate";
	
	public static final String GENERATED_BINDING = "generatedBinding:";

	//Identifier for template, specified in ListView creation dict
	private String templateID;
	//Internal identifier for template, each template has a unique type
	private int templateType;
	
	protected DataItem rootItem;
	
	protected String itemID;
	//Properties of the template. 
	private KrollDict properties;
	
	public class DataItem {
		//binding id
		String bindId;
		KrollDict defaultProperties;

		public DataItem(String id) {
			bindId = id;
			defaultProperties = new KrollDict();
		}
		
		public String getBindingId() {
			return bindId;
		}
		public void setDefaultProperties(KrollDict d) {
			defaultProperties = d;
		}
		
		public KrollDict getDefaultProperties() {
			return defaultProperties;
		}

		
		public void release() {
		}
	}

	public TiListViewTemplate(String id, KrollDict properties) {
		//Init our binding hashmaps
		dataItems = new HashMap<String, DataItem>();

		//Set item id. Item binding is always "properties"
		itemID = TiC.PROPERTY_PROPERTIES;
		//Init vars.
		templateID = id;
		templateType = -1;
		if (properties != null) {
			this.properties = properties;
			processProperties(this.properties);
		} else {
			this.properties = new KrollDict();
		}
	}

	private void bindProxiesAndProperties(KrollDict properties, boolean isRootTemplate) {
		String id = null;
		Object props = null;
		DataItem item = null;


		//Get/generate random bind id
		if (isRootTemplate) {
			id = itemID;	
		} else if (properties.containsKey(TiC.PROPERTY_BIND_ID)) {
			id = TiConvert.toString(properties, TiC.PROPERTY_BIND_ID);
		} 

		if (id == null) return;

		if (isRootTemplate) {
			rootItem = item = new DataItem(TiC.PROPERTY_PROPERTIES);
		} else {
			item = new DataItem(id);
		}
		dataItems.put(id, item);

		if (properties.containsKey(TiC.PROPERTY_PROPERTIES)) {
			props = properties.get(TiC.PROPERTY_PROPERTIES);
		}
		
		if (props instanceof HashMap) {
			item.setDefaultProperties(new KrollDict((HashMap)props));
		}
	}

	private void processProperties(KrollDict properties) {
		bindProxiesAndProperties(properties, true);
		if (properties.containsKey(TiC.PROPERTY_CHILD_TEMPLATES)) {
			processChildProperties(properties.get(TiC.PROPERTY_CHILD_TEMPLATES));
		}

	}
	
	private void processChildProperties(Object childProperties) {
		if (childProperties instanceof Object[]) {
			Object[] propertiesArray = (Object[])childProperties;
			for (int i = 0; i < propertiesArray.length; i++) {
				HashMap<String, Object> properties = (HashMap<String, Object>) propertiesArray[i];
				//bind proxies and default properties
				bindProxiesAndProperties(new KrollDict(properties), false);
				//Recursively calls for all childTemplates
				if (properties.containsKey(TiC.PROPERTY_CHILD_TEMPLATES)) {
					processChildProperties(properties.get(TiC.PROPERTY_CHILD_TEMPLATES));
				}
			}
		}
	}

	public String getTemplateID() {
		return templateID;
	}

	public void setType(int type) {
		templateType = type;
	}
	
	public int getType() {
		return templateType;
	}

	/**
	 * Returns the bound view proxy if exists.
	 */
	public DataItem getDataItem(String binding) {
		return dataItems.get(binding);	
	}

	public DataItem getRootItem() {
		return rootItem;
	}
	
	public KrollDict getProperties() {
		return properties;
	}
	
	
	public ListItemProxy generateCellProxy(KrollDict data)
	{
		ListItemProxy proxy = (ListItemProxy) TiViewProxy.createTypeViewFromDict(properties, "Ti.UI.ListItem");
		proxy.setTemplate(this);
		return proxy;
	}

	public void updateOrMergeWithDefaultProperties(KrollDict data) {
		for (String binding: dataItems.keySet()) {
			DataItem dataItem = dataItems.get(binding);
			if (dataItem == null) continue;

			KrollDict defaultProps = dataItem.getDefaultProperties();
			if (defaultProps != null) {
				KrollDict props = data.containsKey(binding)?new KrollDict((HashMap)data.get(binding)):new KrollDict();
				//merge default properties with new properties and update data
				HashMap<String, Object> newData = ((HashMap<String, Object>)defaultProps.clone());
				newData.putAll(props);
				data.put(binding, newData);
			}
		}

	}
	
	public void release () {
		for (int i = 0; i < dataItems.size(); i++) {
			DataItem item = dataItems.get(i);
			if (item != null) {
				item.release();
			}
		}
		dataItems.clear();
		if (rootItem != null) {
			rootItem.release();
			rootItem = null;
		}
	}
	
	public KrollDict prepareDataDict(KrollDict dict)
	{
		KrollDict result = (KrollDict)dict.clone();
		return result;
	}
}
