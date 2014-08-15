/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

package ti.modules.titanium.ui.widget.listview;

import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.view.TiCompositeLayout;
import org.appcelerator.titanium.view.TiUIView;

import android.content.Context;
import android.util.AttributeSet;

public class TiBaseListViewItem extends TiCompositeLayout{

	
	public int sectionIndex = -1;
	public int itemIndex = -1;
	public TiBaseListViewItem(Context context) {
		super(context);
	}
	
	public TiBaseListViewItem(Context context, AttributeSet set) {
		super(context, set);
		setId(TiListView.listContentId);
	}
	
	
	public void setCurrentItem(int sectionIndex, int itemIndex, ListSectionProxy sectionProxy)
	{
		this.sectionIndex = sectionIndex;
		this.itemIndex = itemIndex;
		((ListItemProxy) getView().getProxy()).setCurrentItem(sectionIndex, itemIndex, sectionProxy);
	}
	
	public int getItemIndex() {
	    return itemIndex;
	}
	
	public KrollProxy getViewProxyFromBinding(String binding) {
		TiUIView view  = getView();
		if (view != null) {
			ListItemProxy proxy = (ListItemProxy)view.getProxy();
			if (proxy != null)
			{
				return proxy.getProxyFromBinding(binding);
			}
		}
		return null;
	}
}
