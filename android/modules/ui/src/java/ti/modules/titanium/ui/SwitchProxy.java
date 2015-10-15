/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.ui;

import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiContext;
import org.appcelerator.titanium.view.TiUIView;

import ti.modules.titanium.ui.android.AndroidModule;
import ti.modules.titanium.ui.widget.TiUISwitch;
import android.app.Activity;

@Kroll.proxy(creatableInModule=UIModule.class,
propertyAccessors = {
	TiC.PROPERTY_VALUE,
	TiC.PROPERTY_STYLE,
	TiC.PROPERTY_TITLE,
	TiC.PROPERTY_TITLE_ON,
	TiC.PROPERTY_TITLE_OFF,
	TiC.PROPERTY_COLOR,
	TiC.PROPERTY_FONT,
	TiC.PROPERTY_TEXT_ALIGN,
	TiC.PROPERTY_VERTICAL_ALIGN,
	TiC.PROPERTY_BACKGROUND_CHECKED_COLOR,
	TiC.PROPERTY_BACKGROUND_CHECKED_IMAGE,
	TiC.PROPERTY_BACKGROUND_CHECKED_GRADIENT
})
public class SwitchProxy extends ViewProxy
{
	public SwitchProxy()
	{
		super();
        defaultValues.put(TiC.PROPERTY_VALUE, false);
        defaultValues.put(TiC.PROPERTY_STYLE, AndroidModule.SWITCH_STYLE_SWITCH);
        
        //there seems to be a bug with the latest appcompat. Without a background the
        //switch is not showing
        defaultValues.put(TiC.PROPERTY_BACKGROUND_COLOR, "transparent");
	}

	public SwitchProxy(TiContext tiContext)
	{
		this();
	}

	@Override
	public TiUIView createView(Activity activity)
	{
		return new TiUISwitch(this);
	}

	@Override
	public String getApiName()
	{
		return "Ti.UI.Switch";
	}
}
