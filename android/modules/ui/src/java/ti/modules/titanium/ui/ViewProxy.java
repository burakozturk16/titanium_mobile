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
import org.appcelerator.titanium.view.TiCompositeLayout.LayoutParams;
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
		LayoutParams params = view.getLayoutParams();
		params.sizeOrFillWidthEnabled = true;
		params.sizeOrFillHeightEnabled = true;
		params.autoFillsHeight = true;
		params.autoFillsWidth = true;
		return view; 
	}

	@Override
	public String getApiName()
	{
		return "Ti.UI.View";
	}
}
