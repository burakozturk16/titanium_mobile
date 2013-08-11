/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.ui.widget;

import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.view.TiCompositeLayout;
import org.appcelerator.titanium.view.TiUIView;

import android.view.View;

public class TiView extends TiUIView
{

	public TiView(TiViewProxy proxy) {
		super(proxy);
		setNativeView(new TiCompositeLayout(proxy.getActivity(), proxy));
	}

	@Override
	protected void setOpacity(View view, float opacity)
	{
		super.setOpacity(view, opacity);
		TiCompositeLayout layout = (TiCompositeLayout) nativeView;
		layout.setAlphaCompat(opacity);
	}

}
