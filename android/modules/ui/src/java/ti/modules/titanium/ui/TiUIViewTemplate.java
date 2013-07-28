package ti.modules.titanium.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;

public class TiUIViewTemplate {
	
	protected static final String TAG = "TiTemplate";
		
	public static final String GENERATED_BINDING = "generatedBinding:";

	//Identifier for template, specified in ListView creation dict
	private String templateID;
			
	//Properties of the template. 
	private KrollDict properties;
	
	public TiUIViewTemplate(String id, KrollDict properties) {
		templateID = id;
		if (properties != null) {
			this.properties = properties;
			prepareProperties(this.properties);
		}
	
	}
	
	@SuppressWarnings("unchecked")
	private void prepareProperties(Object properties) {
		if (properties instanceof Object[]) {
			Object[] propertiesArray = (Object[])properties;
			for (int i = 0; i < propertiesArray.length; i++) {
				prepareProperties(propertiesArray[i]);
			}
		}
		else {
			HashMap kProperties = (HashMap)properties;
			if (kProperties.containsKey(TiC.PROPERTY_PROPERTIES)) {
				Object props = kProperties.get(TiC.PROPERTY_PROPERTIES);
				if (props instanceof HashMap) {
					kProperties.put(TiC.PROPERTY_PROPERTIES, new KrollDict((HashMap)props));
				}
			}
			if (kProperties.containsKey(TiC.PROPERTY_CHILD_TEMPLATES)) {
				prepareProperties(kProperties.get(TiC.PROPERTY_CHILD_TEMPLATES));
			}
		}
	}

	@SuppressWarnings("unchecked")
	private TiViewProxy bindProxiesAndProperties(HashMap properties, TiViewProxy parent, TiViewProxy root) {
		TiViewProxy proxy = null;
		Object props = null;
		if (properties.containsKey(TiC.PROPERTY_TI_PROXY)) {
			proxy = (TiViewProxy) properties.get(TiC.PROPERTY_TI_PROXY);
		}
		else if(properties.containsKey(TiC.PROPERTY_TEMPLATE)) {
			proxy = UIModule.internalCreateViewFromTemplate(TiConvert.toString(properties.get(TiC.PROPERTY_TEMPLATE)));
		}
		else if (properties.containsKey(TiC.PROPERTY_TI_CLASS)) {
			KrollDict proxyProperties = null;
			if (properties.containsKey(TiC.PROPERTY_PROPERTIES)) {
				proxyProperties = (KrollDict) properties.get(TiC.PROPERTY_PROPERTIES);
			}
			try {
				Class<? extends KrollProxy> clazz = (Class<? extends KrollProxy>) Class.forName(TiConvert.toString(properties.get(TiC.PROPERTY_TI_CLASS)));
				proxy = (TiViewProxy) KrollProxy.createProxy(clazz, null, new Object[]{proxyProperties}, null);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				proxy = null;
			}
		}
		
		if (proxy == null) return null;
		if (root == null) {
			root = proxy;
		}

		if (properties.containsKey(TiC.PROPERTY_BIND_ID)) {
			String id = TiConvert.toString(properties, TiC.PROPERTY_BIND_ID);
			root.setBinding(id, proxy); 
		}
		if (parent != null) {
			parent.add(proxy);
		}
		
		if (properties.containsKey(TiC.PROPERTY_CHILD_TEMPLATES)) {
			Object childTemplates = properties.get(TiC.PROPERTY_CHILD_TEMPLATES);
			if (childTemplates instanceof Object[]) {
				Object[] propertiesArray = (Object[])childTemplates;
				for (int i = 0; i < propertiesArray.length; i++) {
					HashMap<String, Object> childProperties = (HashMap<String, Object>) propertiesArray[i];
					//bind proxies and default properties
					bindProxiesAndProperties(childProperties, proxy, root);
				}
			}
		}
		
		return proxy;
	}

	public TiViewProxy buildProxy() {
		TiViewProxy result = bindProxiesAndProperties(properties, null, null);
		return result;

	}

	public String getTemplateID() {
		return templateID;
	}
	
	public KrollDict getProperties() {
		return properties;
	}

	public void release () {
	}
}
