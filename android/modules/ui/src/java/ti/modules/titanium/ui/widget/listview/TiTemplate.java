package ti.modules.titanium.ui.widget.listview;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;

public class TiTemplate {
	
	private static final String TAG = "TiTemplate";

	protected HashMap<String, DataItem> dataItems;
	
	public static final String DEFAULT_TEMPLATE = "defaultTemplate";
	
	public static final String GENERATED_BINDING = "@#$_+_#$#^%^~:";

	//Identifier for template, specified in ListView creation dict
	private String templateID;
	//Internal identifier for template, each template has a unique type
	private int templateType;
	
	protected DataItem rootItem;
	
	protected String itemID;
	//Properties of the template. 
	private KrollDict properties;
	
	public class DataItem {
		//proxy for the item
		TiViewProxy vProxy;
		//binding id
		String bindId;
		DataItem parent;
		ArrayList<DataItem> children;
		KrollDict defaultProperties;

		public DataItem(TiViewProxy proxy, String id, DataItem parent) {
			vProxy = proxy;
			bindId = id;
			this.parent = parent;
			setProxyParent();
			children = new ArrayList<DataItem>();
			defaultProperties = new KrollDict();
		}
		
		private void setProxyParent() {
			if (vProxy != null && parent != null) {
				TiViewProxy parentProxy = parent.getViewProxy();
				if (parentProxy != null) {
					vProxy.setParent(parentProxy);
				}
			}
		}
		
		public TiViewProxy getViewProxy() {
			return vProxy;
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

		public DataItem getParent() {
			return parent;
		}
		
		public ArrayList<DataItem> getChildren() {
			return children;
		}
		
		public void addChild(DataItem child) {
			children.add(child);
		}
	}

	public TiTemplate() {
		
	}

	public TiTemplate(String id, KrollDict properties) {
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

	private DataItem bindProxiesAndProperties(KrollDict properties, boolean isRootTemplate, DataItem parent) {
		Object proxy = null;
		String id = null;
		Object props = null;
		DataItem item = null;
		if (properties.containsKey(TiC.PROPERTY_TYPE)) {
			proxy = properties.get(TiC.PROPERTY_TYPE);
		}

		//Get/generate random bind id
		if (isRootTemplate) {
			id = itemID;	
		} else if (!isRootTemplate && properties.containsKey(TiC.PROPERTY_BIND_ID)) {
			id = TiConvert.toString(properties, TiC.PROPERTY_BIND_ID);
		} else {
			id = GENERATED_BINDING + Math.random();
		}
		

		if (proxy instanceof TiViewProxy) {
			TiViewProxy viewProxy = (TiViewProxy) proxy;
			if (isRootTemplate) {
				rootItem = item = new DataItem(viewProxy, TiC.PROPERTY_PROPERTIES, null);
			} else {
				item = new DataItem(viewProxy, id, parent);
				parent.addChild(item);
			}
			dataItems.put(id, item);
		}

		if (properties.containsKey(TiC.PROPERTY_PROPERTIES)) {
			props = properties.get(TiC.PROPERTY_PROPERTIES);
		}
		
		if (props instanceof HashMap) {
			item.setDefaultProperties(new KrollDict((HashMap)props));
		}

		return item;
	}

	private void processProperties(KrollDict properties) {
		bindProxiesAndProperties(properties, true, null);
		if (properties.containsKey(TiC.PROPERTY_CHILD_TEMPLATES)) {
			processChildProperties(properties.get(TiC.PROPERTY_CHILD_TEMPLATES), rootItem);
		}

	}
	
	private void processChildProperties(Object childProperties, DataItem parent) {
		if (childProperties instanceof Object[]) {
			Object[] propertiesArray = (Object[])childProperties;
			for (int i = 0; i < propertiesArray.length; i++) {
				HashMap<String, Object> properties = (HashMap<String, Object>) propertiesArray[i];
				//bind proxies and default properties
				DataItem item = bindProxiesAndProperties(new KrollDict(properties), false, parent);
				//Recursively calls for all childTemplates
				if (properties.containsKey(TiC.PROPERTY_CHILD_TEMPLATES)) {
					if(item == null) {
						Log.e(TAG, "ITEM SHOULDN'NT BE NULL");
					}
					processChildProperties(properties.get(TiC.PROPERTY_CHILD_TEMPLATES), item);
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
	
	public String getItemID() {
		return itemID;
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

	/**
	 * 
	 * @param data
	 */
	public void updateDefaultProperties(KrollDict data) {
		
		for (String binding: data.keySet()) {
			DataItem dataItem = dataItems.get(binding);
			if (dataItem == null) continue;
			
			KrollDict defaultProps = dataItem.getDefaultProperties();
			KrollDict props = new KrollDict((HashMap)data.get(binding));
			if (defaultProps != null) {
				//update default properties
				modifyDefaultProperties(defaultProps, props);
			}
		}
		
	}
	
	public void mergeWithDefaultProperties(KrollDict data) {
		for (String binding: data.keySet()) {
			DataItem dataItem = dataItems.get(binding);
			if (dataItem == null) continue;

			KrollDict defaultProps = dataItem.getDefaultProperties();
			KrollDict props = new KrollDict((HashMap)data.get(binding));
			if (defaultProps != null) {
				//merge default properties with new properties and update data
				HashMap<String, Object> newData = ((HashMap<String, Object>)defaultProps.clone());
				newData.putAll(props);
				data.put(binding, newData);
			}
		}
	}
	
	public void modifyDefaultProperties(KrollDict existingProperties, KrollDict newProperties) {
		Set<String> existingKeys = existingProperties.keySet();
		for (String key:  newProperties.keySet()) {
			if (!existingKeys.contains(key)) {
				existingProperties.put(key, null);
			}
		}

	}
}
