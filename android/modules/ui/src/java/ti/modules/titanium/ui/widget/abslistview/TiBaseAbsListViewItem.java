/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

package ti.modules.titanium.ui.widget.abslistview;

import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.titanium.view.TiCompositeLayout;
import org.appcelerator.titanium.view.TiUIView;

import android.content.Context;
import android.util.AttributeSet;

public class TiBaseAbsListViewItem extends TiCompositeLayout{

	
	public int sectionIndex = -1;
	public int itemIndex = -1;
	public TiBaseAbsListViewItem(Context context) {
		this(context, null);
	}
	
	public TiBaseAbsListViewItem(Context context, AttributeSet set) {
		super(context, set);
        setId(TiAbsListView.listContentId);
	}
	
	
	public void setCurrentItem(int sectionIndex, int itemIndex, AbsListSectionProxy sectionProxy)
	{
		this.sectionIndex = sectionIndex;
		this.itemIndex = itemIndex;
	}
	
	public int getItemIndex() {
	    return itemIndex;
	}
	public int getSectionIndex() {
        return sectionIndex;
    }
	
	public TiAbsListItem getListItem() {
        return (TiAbsListItem) getView();
    }
    
	public AbsListItemProxy getProxy() {
	    TiUIView view  = getView();
        if (view != null) {
            return (AbsListItemProxy)view.getProxy();
        }
        return null;
	}
    
	public KrollProxy getViewProxyFromBinding(String binding) {
		TiUIView view  = getView();
		if (view != null) {
			AbsListItemProxy proxy = (AbsListItemProxy)view.getProxy();
			if (proxy != null)
			{
				return proxy.getProxyFromBinding(binding);
			}
		}
		return null;
	}
}
