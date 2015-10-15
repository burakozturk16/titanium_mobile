/**
 * Akylas
 * Copyright (c) 2014-2015 by Akylas. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

package ti.modules.titanium.ui.widget.collectionview;

import org.appcelerator.kroll.annotations.Kroll;

import ti.modules.titanium.ui.UIModule;
import ti.modules.titanium.ui.widget.abslistview.AbsListItemProxy;

@Kroll.proxy(creatableInModule = UIModule.class)
public class CollectionItemProxy extends AbsListItemProxy
{
	public CollectionItemProxy()
	{
	    super();
	}

    @Override
    public String getApiName()
    {
        return "Ti.UI.CollectionItem";
    }
}
