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
		// if (properties instanceof Object[]) {
		// 	Object[] propertiesArray = (Object[])properties;
		// 	for (int i = 0; i < propertiesArray.length; i++) {
		// 		prepareProperties(propertiesArray[i]);
		// 	}
		// }
		// else {
		// 	HashMap kProperties = (HashMap)properties;
		// 	if (kProperties.containsKey(TiC.PROPERTY_PROPERTIES)) {
		// 		Object props = kProperties.get(TiC.PROPERTY_PROPERTIES);
		// 		if (props instanceof HashMap) {
		// 			kProperties.put(TiC.PROPERTY_PROPERTIES, new KrollDict((HashMap)props));
		// 		}
		// 	}
		// 	if (kProperties.containsKey(TiC.PROPERTY_CHILD_TEMPLATES)) {
		// 		prepareProperties(kProperties.get(TiC.PROPERTY_CHILD_TEMPLATES));
		// 	}
		// }
	}

	@SuppressWarnings("unchecked")
	private TiViewProxy bindProxiesAndProperties(HashMap proxyProperties, TiViewProxy parent, TiViewProxy root, HashMap rootArguments) {
		TiViewProxy proxy = null;
		Object props = null;
		String bindId = TiConvert.toString(proxyProperties.get(TiC.PROPERTY_BIND_ID));
		boolean hasProperties = proxyProperties.containsKey(TiC.PROPERTY_PROPERTIES);
		HashMap realProxyProperties = new HashMap(proxyProperties);
		if (hasProperties == false) {
			realProxyProperties.put(TiC.PROPERTY_PROPERTIES, new HashMap());
		}
		HashMap realProperties = (HashMap)realProxyProperties.get(TiC.PROPERTY_PROPERTIES);
		HashMap data = (HashMap)rootArguments.get(TiC.PROPERTY_DATA);

		if (root == null && rootArguments != null && rootArguments.containsKey(TiC.PROPERTY_PROPERTIES)) {
			//we accept root properties additions from rootArguments, but we must only do it for the root!
			realProperties.putAll((HashMap)rootArguments.get(TiC.PROPERTY_PROPERTIES));
		}

		if (bindId != null && data != null && data.containsKey(bindId)) {
			//we complementary data for that object comming from external properties
			HashMap bindingProxyProperties = (HashMap)data.get(bindId);
			if (bindingProxyProperties.containsKey(TiC.PROPERTY_PROPERTIES))
				realProperties.putAll((HashMap)bindingProxyProperties.get(TiC.PROPERTY_PROPERTIES));
			//only used for subtemplate which can also have bindings
			if (bindingProxyProperties.containsKey(TiC.PROPERTY_DATA))
				realProxyProperties.put(TiC.PROPERTY_DATA, bindingProxyProperties.get(TiC.PROPERTY_DATA));
		}
		if (proxyProperties.containsKey(TiC.PROPERTY_TI_PROXY)) {
			proxy = (TiViewProxy) proxyProperties.get(TiC.PROPERTY_TI_PROXY);
		}
		else if(proxyProperties.containsKey(TiC.PROPERTY_TEMPLATE)) {
			proxy = UIModule.internalCreateViewFromTemplate(TiConvert.toString(proxyProperties.get(TiC.PROPERTY_TEMPLATE)), realProxyProperties);
		}
		else if (proxyProperties.containsKey(TiC.PROPERTY_TI_CLASS)) {
			try {
				Class<? extends KrollProxy> clazz = (Class<? extends KrollProxy>) Class.forName(TiConvert.toString(proxyProperties.get(TiC.PROPERTY_TI_CLASS)));
				proxy = (TiViewProxy) KrollProxy.createProxy(clazz, null, new Object[]{realProperties}, null);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				proxy = null;
			}
		}
		if (root != null) {
			if (bindId != null) {
				if (proxy == null) {
					//we still create a binding, this can be usefull for JS custom views
					root.setBinding(bindId, realProperties); 
				}
				else {
					root.setBinding(bindId, proxy);
				}
			}
		}

		if (proxy == null) return null;
		if (root == null) {
			root = proxy;
		}

		if (parent != null) {
			parent.add(proxy);
		}
		
		Object childTemplates = proxyProperties.get(TiC.PROPERTY_CHILD_TEMPLATES);
		if (childTemplates instanceof Object[]) {
			Object[] propertiesArray = (Object[])childTemplates;
			for (int i = 0; i < propertiesArray.length; i++) {
				HashMap childProperties = (HashMap)propertiesArray[i];
				//bind proxies and default properties
				bindProxiesAndProperties(childProperties, proxy, root, rootArguments);
			}
		}
		
		return proxy;
	}

	public TiViewProxy buildProxy(HashMap rootArguments) {
		TiViewProxy result = bindProxiesAndProperties(this.properties, null, null, rootArguments);
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
