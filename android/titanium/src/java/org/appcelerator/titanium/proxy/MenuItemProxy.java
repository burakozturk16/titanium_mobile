/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package org.appcelerator.titanium.proxy;

import java.util.List;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollPropertyChange;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.KrollProxyListener;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.AsyncResult;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.kroll.common.TiMessenger;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiFileHelper;
import org.appcelerator.titanium.util.TiUIHelper;
import org.appcelerator.titanium.util.TiUrl;
import org.appcelerator.titanium.view.TiUIView;

import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnActionExpandListener;

import android.graphics.drawable.Drawable;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;

@Kroll.proxy
public class MenuItemProxy extends KrollProxy implements KrollProxyListener
{
	private static final String TAG = "MenuItem";

	private MenuItem item;


	private static final int MSG_FIRST_ID = KrollProxy.MSG_LAST_ID + 1;
	
	private static final int MSG_GROUP_ID = MSG_FIRST_ID + 200;
	private static final int MSG_ITEM_ID = MSG_FIRST_ID + 201;
	private static final int MSG_ORDER = MSG_FIRST_ID + 202;
	private static final int MSG_TITLE = MSG_FIRST_ID + 203;
	private static final int MSG_TITLE_CONDENSED = MSG_FIRST_ID + 204;
	private static final int MSG_SUB_MENU = MSG_FIRST_ID + 205;
	private static final int MSG_CHECKED = MSG_FIRST_ID + 206;
	private static final int MSG_CHECKABLE = MSG_FIRST_ID + 207;
	private static final int MSG_ENABLED = MSG_FIRST_ID + 208;
	private static final int MSG_VISIBLE = MSG_FIRST_ID + 209;
	private static final int MSG_SET_CHECKED = MSG_FIRST_ID + 210;
	private static final int MSG_SET_CHECKABLE = MSG_FIRST_ID + 211;
	private static final int MSG_SET_ENABLED = MSG_FIRST_ID + 212;
	private static final int MSG_SET_VISIBLE = MSG_FIRST_ID + 213;
	private static final int MSG_SET_ICON = MSG_FIRST_ID + 214;
	private static final int MSG_SET_TITLE = MSG_FIRST_ID + 215;
	private static final int MSG_SET_TITLE_CONDENSED = MSG_FIRST_ID + 216;
	private static final int MSG_ACTION_VIEW_EXPANDED = MSG_FIRST_ID + 217;

	protected static final int MSG_LAST_ID = MSG_FIRST_ID + 1000;

	private final class ActionExpandListener implements OnActionExpandListener {
		public boolean onMenuItemActionCollapse(MenuItem item) {
			fireEvent(TiC.EVENT_COLLAPSE, null);
			return true;
		}

		public boolean onMenuItemActionExpand(MenuItem item) {
			fireEvent(TiC.EVENT_EXPAND, null);
			return true;
		}
	}
	
	private final class CompatActionExpandListener implements MenuItemCompat.OnActionExpandListener {
		public boolean onMenuItemActionCollapse(MenuItem item) {
			fireEvent(TiC.EVENT_COLLAPSE, null);
			return true;
		}

		public boolean onMenuItemActionExpand(MenuItem item) {
			fireEvent(TiC.EVENT_EXPAND, null);
			return true;
		}
	}

	protected MenuItemProxy(MenuItem item)
	{
		this.item = item;
		setModelListener(this, false);

		item.setOnActionExpandListener(new ActionExpandListener());
	}

	@Override
	public boolean handleMessage(Message msg) 
	{
		AsyncResult result = null;
		result = (AsyncResult) msg.obj;

		switch(msg.what) {
			case MSG_GROUP_ID: {
				result.setResult(item.getGroupId());
				return true;
			}
			case MSG_ITEM_ID: {
				result.setResult(item.getItemId());
				return true;
			}
			case MSG_ORDER: {
				result.setResult(item.getOrder());
				return true;
			}
			case MSG_TITLE: {
				result.setResult(item.getTitle());
				return true;
			}
			case MSG_TITLE_CONDENSED: {
				result.setResult(item.getTitleCondensed());
				return true;
			}
			case MSG_SUB_MENU: {
				result.setResult(item.hasSubMenu());
				return true;
			}
			case MSG_CHECKED: {
				result.setResult(item.isChecked());
				return true;
			}
			case MSG_CHECKABLE: {
				result.setResult(item.isCheckable());
				return true;
			}
			case MSG_ENABLED: {
				result.setResult(item.isEnabled());
				return true;
			}
			case MSG_VISIBLE: {
				result.setResult(item.isVisible());
				return true;
			}
			case MSG_SET_CHECKED: {
				item.setChecked((Boolean)result.getArg());
				result.setResult(this);
				return true;
			}
			case MSG_SET_CHECKABLE: {
				item.setCheckable((Boolean)result.getArg());
				result.setResult(this);
				return true;
			}
			case MSG_SET_ENABLED: {
				item.setEnabled((Boolean)result.getArg());
				result.setResult(this);
				return true;
			}
			case MSG_SET_VISIBLE: {
				item.setVisible((Boolean)result.getArg());
				result.setResult(this);
				return true;
			}
			case MSG_SET_ICON: {
				result.setResult(handleSetIcon(result.getArg()));
				return true;
			}
			case MSG_SET_TITLE: {
				item.setTitle((String)result.getArg());
				result.setResult(this);
				return true;
			}
			case MSG_SET_TITLE_CONDENSED: {
				item.setTitleCondensed((String)result.getArg());
				result.setResult(this);
				return true;
			}
			case MSG_ACTION_VIEW_EXPANDED: {
				result.setResult(item.isActionViewExpanded());
				return true;
			}
			
			default : {
				return super.handleMessage(msg);
			}
		}
	}

	@Kroll.method @Kroll.getProperty
	public int getGroupId() {
		if (TiApplication.isUIThread()) {
			return item.getGroupId();
		}

		return (Integer) TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_GROUP_ID));
	}
	
	@Kroll.method @Kroll.getProperty
	public int getItemId() {
		if (TiApplication.isUIThread()) {
			return item.getItemId();
		}

		return (Integer) TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_ITEM_ID));
	}
	
	@Kroll.method @Kroll.getProperty
	public int getOrder() {
		if (TiApplication.isUIThread()) {
			return item.getOrder();
		}

		return (Integer) TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_ORDER));
	}
	
	@Kroll.method @Kroll.getProperty
	public String getTitle() {
		if (TiApplication.isUIThread()) {
			return (String) item.getTitle();
		}

		return (String) TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_TITLE));
	}
	
	@Kroll.method @Kroll.getProperty
	public String getTitleCondensed() {
		if (TiApplication.isUIThread()) {
			return (String) item.getTitleCondensed();
		}

		return (String) TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_TITLE_CONDENSED));
	}
	
	@Kroll.method
	public boolean hasSubMenu() {
		if (TiApplication.isUIThread()) {
			return item.hasSubMenu();
		}

		return (Boolean) TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_SUB_MENU));
	}
	
	@Kroll.method @Kroll.getProperty
	public boolean isChecked() {
		if (TiApplication.isUIThread()) {
			return item.isChecked();
		}

		return (Boolean) TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_CHECKED));
	}
	
	@Kroll.method @Kroll.getProperty
	public boolean isCheckable() {
		if (TiApplication.isUIThread()) {
			return item.isCheckable();
		}

		return (Boolean) TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_CHECKABLE));
	}
	
	@Kroll.method @Kroll.getProperty
	public boolean isEnabled() {
		if (TiApplication.isUIThread()) {
			return item.isEnabled();
		}

		return (Boolean) TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_ENABLED));
	}
	
	@Kroll.method @Kroll.getProperty
	public boolean isVisible() {
		if (TiApplication.isUIThread()) {
			return item.isVisible();
		}

		return (Boolean) TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_VISIBLE));
	}
	
	public MenuItemProxy setCheckable(boolean checkable) {
		if (TiApplication.isUIThread()) {
			item.setCheckable(checkable);
			return this;
		}

		return (MenuItemProxy) TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_SET_CHECKABLE), checkable);
	}
	
	@Kroll.method @Kroll.setProperty
	public MenuItemProxy setChecked(boolean checked) {
		if (TiApplication.isUIThread()) {
			item.setChecked(checked);
			return this;
		}

		return (MenuItemProxy) TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_SET_CHECKED), checked);
	}
	
	public MenuItemProxy setEnabled(boolean enabled) {
		if (TiApplication.isUIThread()) {
			item.setEnabled(enabled);
			return this;
		}

		return (MenuItemProxy) TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_SET_ENABLED), enabled);
	}
	
	private MenuItemProxy handleSetIcon(Object icon) 
	{
		if (icon != null) {
			if (icon instanceof String) {
				String iconPath = TiConvert.toString(icon);
				TiUrl iconUrl = new TiUrl(iconPath);
				if (iconPath != null) {
					TiFileHelper tfh = new TiFileHelper(TiApplication.getInstance());
					Drawable d = tfh.loadDrawable(iconUrl.resolve(), false);
					if (d != null) {
						item.setIcon(d);
					}
				}
			} else if (icon instanceof Number) {
				Drawable d = TiUIHelper.getResourceDrawable(TiConvert.toInt(icon));
				if (d != null) {
					item.setIcon(d);
				}
			}
		}
		return this;
	}
	public MenuItemProxy setIcon(Object icon) 
	{
		if (TiApplication.isUIThread()) {
			return handleSetIcon(icon);
		}

		return (MenuItemProxy) TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_SET_ICON), icon);	
	}
	
	public MenuItemProxy setTitle(String title) {
		if (TiApplication.isUIThread()) {
			item.setTitle(title);
			return this;
		}

		return (MenuItemProxy) TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_SET_TITLE), title);
	}
	
	public MenuItemProxy setTitleCondensed(String title) {
		if (TiApplication.isUIThread()) {
			item.setTitleCondensed(title);
			return this;
		}

		return (MenuItemProxy) TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_SET_TITLE_CONDENSED), title);
	}
	
	public MenuItemProxy setVisible(boolean visible) {
		if (TiApplication.isUIThread()) {
			item.setVisible(visible);
			return this;
		}

		return (MenuItemProxy) TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_SET_VISIBLE), visible);
	}

	public void setActionView(Object view)
	{
		if (view instanceof TiViewProxy) {
			final View v = ((TiViewProxy) view).getOrCreateView().getOuterView();
			TiMessenger.postOnMain(new Runnable() {
				public void run() {
					item.setActionView(v);
				}
			});
		} else {
			Log.w(TAG, "Invalid type for actionView", Log.DEBUG_MODE);
		}
	}

	public void setShowAsAction(final int flag) {
		TiMessenger.postOnMain(new Runnable() {
			public void run() {
				item.setShowAsAction(flag);
			}
		});
	}

	@Kroll.method
	public void collapseActionView() {
		TiMessenger.postOnMain(new Runnable() {
			public void run() {
				item.collapseActionView();
			}
		});
	}

	@Kroll.method
	public void expandActionView() {
		TiMessenger.postOnMain(new Runnable() {
			public void run() {
				item.expandActionView();
			}
		});
	}

	@Kroll.method @Kroll.getProperty
	public boolean isActionViewExpanded() {
		if (TiApplication.isUIThread()) {
			return item.isActionViewExpanded();
		}

		return (Boolean) TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_ACTION_VIEW_EXPANDED));
	}

	@Override
	public String getApiName()
	{
		return "Ti.Android.MenuItem";
	}

	@Override
	public void propertyChanged(String key, Object oldValue, Object newValue,
			KrollProxy proxy) {
		if (key.equals(TiC.PROPERTY_ACTION_VIEW)) {
			setActionView(newValue);
		}
		else if (key.equals(TiC.PROPERTY_CHECKABLE)) {
			setCheckable(TiConvert.toBoolean(newValue));
		}
		else if (key.equals(TiC.PROPERTY_CHECKED)) {
			setChecked(TiConvert.toBoolean(newValue));
		}
		else if (key.equals(TiC.PROPERTY_ENABLED)) {
			setEnabled(TiConvert.toBoolean(newValue));
		}
		else if (key.equals(TiC.PROPERTY_ICON)) {
			setIcon(newValue);
		}
		else if (key.equals(TiC.PROPERTY_SHOW_AS_ACTION)) {
			setShowAsAction(TiConvert.toInt(newValue));
		}
		else if (key.equals(TiC.PROPERTY_TITLE_CONDENSED)) {
			setTitleCondensed(TiConvert.toString(newValue));
		}
		else if (key.equals(TiC.PROPERTY_VISIBLE)) {
			setVisible(TiConvert.toBoolean(newValue));
		}
	}

	@Override
	public void processProperties(KrollDict d) {
		// TODO Auto-generated method stub
		if (d.containsKey(TiC.PROPERTY_ACTION_VIEW)) {
			setActionView(d.get(TiC.PROPERTY_ACTION_VIEW));
		}
		if (d.containsKey(TiC.PROPERTY_CHECKABLE)) {
			setCheckable(TiConvert.toBoolean(d, TiC.PROPERTY_CHECKABLE));
		}
		if (d.containsKey(TiC.PROPERTY_CHECKED)) {
			setChecked(TiConvert.toBoolean(d, TiC.PROPERTY_CHECKED));
		}
		if (d.containsKey(TiC.PROPERTY_ENABLED)) {
			setEnabled(TiConvert.toBoolean(d, TiC.PROPERTY_ENABLED));
		}
		if (d.containsKey(TiC.PROPERTY_ICON)) {
			setIcon(d.get(TiC.PROPERTY_ICON));
		}
		if (d.containsKey(TiC.PROPERTY_SHOW_AS_ACTION)) {
			setShowAsAction(TiConvert.toInt(d, TiC.PROPERTY_SHOW_AS_ACTION));
		}
		if (d.containsKey(TiC.PROPERTY_TITLE_CONDENSED)) {
			setTitleCondensed(TiConvert.toString(d, TiC.PROPERTY_TITLE_CONDENSED));
		}
		if (d.containsKey(TiC.PROPERTY_VISIBLE)) {
			setVisible(TiConvert.toBoolean(d, TiC.PROPERTY_VISIBLE));
		}
	}

	@Override
	public void propertiesChanged(List<KrollPropertyChange> changes,
			KrollProxy proxy) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void listenerAdded(String type, int count, KrollProxy proxy) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void listenerRemoved(String type, int count, KrollProxy proxy) {
		// TODO Auto-generated method stub
		
	}
}
