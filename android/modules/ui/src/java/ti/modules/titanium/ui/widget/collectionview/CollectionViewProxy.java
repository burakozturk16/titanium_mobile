/**
 * Akylas
 * Copyright (c) 2014-2015 by Akylas. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

package ti.modules.titanium.ui.widget.collectionview;

import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.view.TiUIView;
import org.appcelerator.titanium.view.TiCompositeLayout.LayoutParams;

import ti.modules.titanium.ui.UIModule;
import ti.modules.titanium.ui.widget.abslistview.AbsListViewProxy;
import android.app.Activity;

@Kroll.proxy(creatableInModule = UIModule.class)
public class CollectionViewProxy extends AbsListViewProxy {

	private static final String TAG = "CollectionViewProxy";
	
	   private static final int MSG_FIRST_ID = AbsListViewProxy.MSG_LAST_ID + 1;
	
	public CollectionViewProxy() {
		super();
	}

    @Override
	public TiUIView createView(Activity activity) {
        TiUIView view = new TiCollectionView(this, activity);
	    LayoutParams params = view.getLayoutParams();
        params.sizeOrFillWidthEnabled = true;
        params.sizeOrFillHeightEnabled = true;
        params.autoFillsHeight = true;
        params.autoFillsHeight = true;
        params.autoFillsWidth = true;
		return view;
	}

    @Override
    public String getApiName()
    {
        return "Ti.UI.CollectionView";
    }
}
