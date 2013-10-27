/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.ui;

import java.util.HashMap;

import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.TiContext;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.view.TiUIView;

import ti.modules.titanium.ui.widget.TiView;
import android.app.Activity;

@Kroll.proxy(creatableInModule=UIModule.class)
public class ViewProxy extends TiViewProxy
{
	public ViewProxy()
	{
		super();
	}

	public ViewProxy(TiContext tiContext)
	{
		this();
	}

	@Override
	public TiUIView createView(Activity activity)
	{
		TiUIView view = new TiView(this);
		view.getLayoutParams().autoFillsHeight = true;
		view.getLayoutParams().autoFillsWidth = true;
		return view;
	}
	
	@Override
	public void add(Object args, @Kroll.argument(optional = true) Object index)
	{
		TiViewProxy child = null;
		if (args instanceof TiViewProxy)
			child = (TiViewProxy) args;
		else if (args instanceof HashMap) {
			child = (ViewProxy) KrollProxy.createProxy(ViewProxy.class, null, new Object[] { args }, null);
		}
		super.add(child, index);
	}

	@Override
	public String getApiName()
	{
		return "Ti.UI.View";
	}
}
