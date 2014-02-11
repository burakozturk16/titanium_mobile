/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

package ti.modules.titanium.ui.widget.listview;

import java.lang.ref.WeakReference;
import java.util.HashMap;

import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.view.TiUIView;

import ti.modules.titanium.ui.UIModule;
import android.app.Activity;

@Kroll.proxy(creatableInModule = UIModule.class)
public class ListItemProxy extends TiViewProxy
{
	protected WeakReference<TiViewProxy> listProxy;
	
	private HashMap<String, ViewItem> viewsMap;
	private ViewItem viewItem;
	
	public ListItemProxy()
	{
		viewsMap = new HashMap<String, ViewItem>();
	}
	
	@Override
	public void handleCreationDict(KrollDict options)
	{
		super.handleCreationDict(options);
	}

	public TiUIView createView(Activity activity)
	{
		return new TiListItem(this);
	}

	public void setListProxy(TiViewProxy list)
	{
		listProxy = new WeakReference<TiViewProxy>(list);
	}

	public TiViewProxy getListProxy()
	{
		if (listProxy != null) {
			return listProxy.get();
		}
		return null;
	}
	
	@Override
	public KrollProxy getParentForBubbling()
	{
		return getListProxy();
	}

	public boolean fireEvent(final String event, final Object data, boolean bubbles, boolean checkParent)
	{
		if (event.equals(TiC.EVENT_CLICK)) fireItemClick(event, data);
		return super.fireEvent(event, data, bubbles, checkParent);
	}

	private void fireItemClick(String event, Object data)
	{
		TiViewProxy listViewProxy = listProxy.get();
		if (listViewProxy != null) {
		if (listViewProxy != null && listViewProxy.hasListeners(TiC.EVENT_ITEM_CLICK)) {
			KrollDict eventData = new KrollDict((HashMap) data);
			Object source = eventData.get(TiC.EVENT_PROPERTY_SOURCE);
			if (source != null && !source.equals(this) && listProxy != null) {
					listViewProxy.fireEvent(TiC.EVENT_ITEM_CLICK, eventData, false, false);
				}
			}
		}
	}

	@Override
	public boolean hasListeners(String event, boolean bubbles)
	{
		// In order to fire the "itemclick" event when the children views are clicked,
		// the children views' "click" events must be fired and bubbled up. (TIMOB-14901)
		if (event.equals(TiC.EVENT_CLICK)) {
			return true;
		}
		return super.hasListeners(event, bubbles);
	}

	public void release()
	{
		super.release();
		if (listProxy != null) {
			listProxy = null;
		}
	}

	@Override
	public String getApiName()
	{
		return "Ti.UI.ListItem";
	}
	

	public TiViewProxy getViewProxyFromBinding(String binding) {
		ViewItem viewItem = viewsMap.get(binding);
		if (viewItem != null) {
			return viewItem.getViewProxy();
		}
		return null;
	}
	
	@Override
	protected void addBinding(String bindId, TiViewProxy bindingProxy)
	{
		super.addBinding(bindId, bindingProxy);
		ViewItem viewItem = new ViewItem(bindingProxy, bindingProxy.getProperties());
		viewsMap.put(bindId, viewItem);
	}
	
	public HashMap<String, ViewItem> getViewsMap() {
		return viewsMap;
	}
	
	public ViewItem getViewItem() {
		if (viewItem == null) {
			viewItem = new ViewItem(this, getProperties());
		}
		return viewItem;
	}
}
