/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.ui;

import java.util.HashMap;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.APIMap;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiContext;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.view.TiUIView;

import ti.modules.titanium.ui.widget.TiView;
import android.app.Activity;

@Kroll.proxy(creatableInModule = UIModule.class)
public class ViewProxy extends TiViewProxy {
	public ViewProxy() {
		super();
	}

	public ViewProxy(TiContext tiContext) {
		this();
	}

	@Override
	public TiUIView createView(Activity activity) {
		TiUIView view = new TiView(this);
		view.getLayoutParams().autoFillsHeight = true;
		view.getLayoutParams().autoFillsWidth = true;
		return view;
	}

	@Override
	public String getApiName() {
		return "Ti.UI.View";
	}
	
	public void handleCreationDict(KrollDict dict)
	{
		if (dict == null) {
			return;
		}
		if (dict.containsKey(TiC.PROPERTY_PROPERTIES) || dict.containsKey(TiC.PROPERTY_CHILD_TEMPLATES)) {
			super.handleCreationDict(dict.getKrollDict(TiC.PROPERTY_PROPERTIES));
		}
		else {
			super.handleCreationDict(dict);
		}
		if (dict.containsKey(TiC.PROPERTY_CHILD_TEMPLATES)) {
			initFromTemplate(dict, this);
		}
	}
	
	@SuppressWarnings("unchecked")
	protected void initFromTemplate(HashMap template_,
			KrollProxy rootProxy) {
		if (rootProxy != null
				&& template_.containsKey(TiC.PROPERTY_BIND_ID)) {
			rootProxy.setProperty(
					TiConvert.toString(template_, TiC.PROPERTY_BIND_ID),
					this);
		}
		if (template_.containsKey(TiC.PROPERTY_CHILD_TEMPLATES)) {
			Object childProperties = template_
					.get(TiC.PROPERTY_CHILD_TEMPLATES);
			if (childProperties instanceof Object[]) {
				Object[] propertiesArray = (Object[]) childProperties;
				for (int i = 0; i < propertiesArray.length; i++) {
					Object childDict = propertiesArray[i];
					if (childDict instanceof TiViewProxy) {
						this.add((TiViewProxy) childDict);
					} else {
						TiViewProxy childProxy = createViewFromTemplate(
								(HashMap) childDict, rootProxy);
						if (childProxy != null){
							this.updateKrollObjectProperties();
							this.add(childProxy);
						}
					}
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private TiViewProxy createViewFromTemplate(HashMap template_,
			KrollProxy rootProxy) {
		String type = TiConvert.toString(template_, TiC.PROPERTY_TYPE,
				getApiName());
		Object properties = (template_.containsKey(TiC.PROPERTY_PROPERTIES)) ? template_
				.get(TiC.PROPERTY_PROPERTIES) : template_;
		try {
			Class<? extends KrollProxy> cls = (Class<? extends KrollProxy>) Class
					.forName(APIMap.getProxyClass(type));
			ViewProxy proxy = (ViewProxy) KrollProxy.createProxy(cls, null,
					new Object[] { properties }, null);
			if (proxy == null)
				return null;
			proxy.initFromTemplate(template_, rootProxy);
			return proxy;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public void add(Object args, @Kroll.argument(optional = true) Object index) {
		if (args instanceof Object[]) {
			int i = -1; // no index by default
			if (index instanceof Number) {
				i = ((Number) index).intValue();
			}
			int arrayIndex = i;
			for (Object obj : (Object[]) args) {
				add(obj, Integer.valueOf(arrayIndex));
				if (arrayIndex != -1)
					arrayIndex++;
			}
			return;
		} else if (args instanceof HashMap) {
			TiViewProxy childProxy = createViewFromTemplate((HashMap) args,
					this);
			if (childProxy != null) {
				childProxy.updateKrollObjectProperties();
				add(childProxy);
			}
		} else {
			super.add(args, index);
		}
	}
}
