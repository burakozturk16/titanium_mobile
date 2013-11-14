/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.ui;

import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiContext;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.view.TiUIView;

import ti.modules.titanium.ui.widget.TiUIProgressBar;
import android.app.Activity;

@Kroll.proxy(creatableInModule=UIModule.class, propertyAccessors = {
	"min", "max",
	TiC.PROPERTY_VALUE,
	TiC.PROPERTY_MESSAGE
})
public class ProgressBarProxy extends TiViewProxy
{
	public ProgressBarProxy()
	{
		super();
	}

	public ProgressBarProxy(TiContext tiContext)
	{
		this();
	}

	@Override
	public TiUIView createView(Activity activity)
	{
		return new TiUIProgressBar(this);
	}

	@Override
	public String getApiName()
	{
		return "Ti.UI.ProgressBar";
	}
}
