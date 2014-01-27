/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

package ti.modules.titanium.ui.widget.listview;

import java.util.HashMap;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.titanium.view.TiCompositeLayout;
import org.appcelerator.titanium.view.TiUIView;

import ti.modules.titanium.ui.widget.TiUILabel;

import android.content.Context;
import android.util.AttributeSet;

public class TiBaseListViewItem extends TiCompositeLayout{

	private HashMap<String, ViewItem> viewsMap;
	private ViewItem viewItem;
	private TiListItem item = null;
	private int sectionIndex = -1;
	private int itemIndex = -1;
	public TiBaseListViewItem(Context context) {
		super(context);
		viewsMap = new HashMap<String, ViewItem>();
	}
	
	public TiBaseListViewItem(Context context, AttributeSet set) {
		super(context, set);
		setId(TiListView.listContentId);
		viewsMap = new HashMap<String, ViewItem>();
		viewItem = new ViewItem(null, new KrollDict());
	}
	
	public HashMap<String, ViewItem> getViewsMap() {
		return viewsMap;
	}
	
	public ViewItem getViewItem() {
		return viewItem;
	}

	public void bindView(String binding, ViewItem view) {
		viewsMap.put(binding, view);
	}
	
	public TiUIView getViewFromBinding(String binding) {
		ViewItem viewItem = viewsMap.get(binding);
		if (viewItem != null) {
			return viewItem.getView();
		}
		return null;
	}
	
	public void setListItem(TiListItem item)
	{
		this.item = item;
	}
	
	public TiListItem getListItem()
	{
		return item;
	}
	
	public void setCurrentItem(int sectionIndex, int itemIndex)
	{
		this.sectionIndex = sectionIndex;
		this.itemIndex = itemIndex;
	}
	
	public boolean isItemAtIndex(int sectionIndex, int itemIndex)
	{
		return this.sectionIndex == sectionIndex && this.itemIndex == itemIndex;
	}
}
