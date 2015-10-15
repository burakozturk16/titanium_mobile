/**
 * Akylas
 * Copyright (c) 2014-2015 by Akylas. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.ui;

import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.TiContext;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.view.TiUIView;

import ti.modules.titanium.ui.widget.TiUIPagerTabStrip;
import android.app.Activity;

@Kroll.proxy()
public class PagerTabStripProxy extends TiViewProxy
{
    public PagerTabStripProxy()
    {
        super();
    }

    public PagerTabStripProxy(TiContext tiContext)
    {
        this();
    }

    @Override
    public TiUIView createView(Activity activity)
    {
        return new TiUIPagerTabStrip(this);
    }

    @Override
    public String getApiName()
    {
        return "PageTabStrip";
    }
}
