/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

package ti.modules.titanium.ui.widget.listview;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.view.TiUIView;

import ti.modules.titanium.ui.UIModule;
import android.app.Activity;

@Kroll.proxy(creatableInModule = UIModule.class)
public class ListItemProxy extends TiViewProxy implements KrollProxy.SetPropertyChangeListener
{
    protected WeakReference<TiViewProxy> listProxy;
	
	private HashMap<String, ProxyListItem> bindingsMap;
	private List<KrollProxy> nonBindingProxies;
	private ProxyListItem listItem;
	
	public int sectionIndex = -1;
    public int itemIndex = -1;
    protected WeakReference<ListSectionProxy> sectionProxy;
	
	public void setCurrentItem(int sectionIndex, int itemIndex, ListSectionProxy sectionProxy)
    {
        this.sectionIndex = sectionIndex;
        this.itemIndex = itemIndex;
        this.sectionProxy = new WeakReference<ListSectionProxy>(sectionProxy);
    }
	
	public ListItemProxy()
	{
		bindingsMap = new HashMap<String, ProxyListItem>();
		nonBindingProxies = new ArrayList();
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
		bindingsMap.clear();
		nonBindingProxies.clear();
		if (listProxy != null) {
			listProxy = null;
		}
		if (sectionProxy != null) {
		    sectionProxy = null;
        }
	}

	@Override
	public String getApiName()
	{
		return "Ti.UI.ListItem";
	}
	

	public KrollProxy getProxyFromBinding(String binding) {
		ProxyListItem viewItem = bindingsMap.get(binding);
		if (viewItem != null) {
			return viewItem.getProxy();
		}
		return null;
	}
	
	@Override
    public void addBinding(String bindId, KrollProxy arg)
	{
		super.addBinding(bindId, arg);
		KrollProxy bindingProxy = null;
        if (arg instanceof KrollProxy)
            bindingProxy = (KrollProxy) arg;
        if (bindingProxy == null) {
            return;
        }
		if (bindId != null) {
			ProxyListItem viewItem = new ProxyListItem(bindingProxy, bindingProxy.getProperties());
			bindingsMap.put(bindId, viewItem);
		}
		else {
			nonBindingProxies.add(bindingProxy);
		}
		
	}
	
	public HashMap<String, ProxyListItem> getBindings() {
		return bindingsMap;
	}
	
	public List<KrollProxy> getNonBindedProxies() {
		return nonBindingProxies;
	}
	
	public ProxyListItem getListItem() {
		if (listItem == null) {
			listItem = new ProxyListItem(this, getProperties());
		}
		return listItem;
	}

    @Override
    public void onSetProperty(KrollProxy proxy, String name, Object value) {
        for (Map.Entry<String, ProxyListItem> entry : bindingsMap.entrySet()) {
            String key = entry.getKey();
            ProxyListItem item = entry.getValue();
            if (item.getProxy() == proxy) {
                item.setCurrentProperty(name, value);
                if (sectionProxy != null) {
                    sectionProxy.get().updateItemAt(itemIndex, key, name, value);
                }
                return;
            }
        }
    }
	
	
}
