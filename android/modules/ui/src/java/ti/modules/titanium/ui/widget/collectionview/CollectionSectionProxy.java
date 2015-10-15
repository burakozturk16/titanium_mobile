/**
 * Akylas
 * Copyright (c) 2014-2015 by Akylas. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

package ti.modules.titanium.ui.widget.collectionview;

import org.appcelerator.kroll.annotations.Kroll;
import ti.modules.titanium.ui.UIModule;
import ti.modules.titanium.ui.widget.abslistview.AbsListSectionProxy;
import android.annotation.SuppressLint;

@Kroll.proxy(creatableInModule = UIModule.class, propertyAccessors = {})
@SuppressWarnings({ "unchecked", "rawtypes" })
@SuppressLint("DefaultLocale")
public class CollectionSectionProxy extends AbsListSectionProxy {

	private static final String TAG = "CollectionSectionProxy";
    
	public CollectionSectionProxy() {
	    super();
	}

    @Override
    public String getApiName()
    {
        return "Ti.UI.CollectionSection";
    }
}
