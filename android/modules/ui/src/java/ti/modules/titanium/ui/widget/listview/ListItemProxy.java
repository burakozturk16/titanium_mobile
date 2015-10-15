/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

package ti.modules.titanium.ui.widget.listview;

import org.appcelerator.kroll.annotations.Kroll;

import ti.modules.titanium.ui.UIModule;
import ti.modules.titanium.ui.widget.abslistview.AbsListItemProxy;

@Kroll.proxy(creatableInModule = UIModule.class)
public class ListItemProxy extends AbsListItemProxy
{
	public ListItemProxy()
	{
	    super();
	}
	

    @Override
    public String getApiName()
    {
        return "Ti.UI.ListItem";
    }
    
}
