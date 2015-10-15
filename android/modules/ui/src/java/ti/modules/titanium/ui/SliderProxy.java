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

import ti.modules.titanium.ui.widget.TiUISlider;
import android.app.Activity;

@Kroll.proxy(creatableInModule=UIModule.class, propertyAccessors = {
    TiC.PROPERTY_MIN, TiC.PROPERTY_MAX, "minRange",
	"maxRange", "thumbImage",
	"leftTrackImage","rightTrackImage",
	TiC.PROPERTY_VALUE
})
public class SliderProxy extends ViewProxy
{
	public SliderProxy()
	{
		super();
		//there seems to be a bug with the latest appcompat. Without a background the
        //switch is not showing
        defaultValues.put(TiC.PROPERTY_BACKGROUND_COLOR, "transparent");
	}

	public SliderProxy(TiContext tiContext)
	{
		this();
	}

	@Override
	public TiUIView createView(Activity activity)
	{
		return new TiUISlider(this);
	}

	@Override
	public String getApiName()
	{
		return "Ti.UI.Slider";
	}
}
