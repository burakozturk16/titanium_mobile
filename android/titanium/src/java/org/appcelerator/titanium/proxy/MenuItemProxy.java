/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package org.appcelerator.titanium.proxy;

import java.util.HashMap;

import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.kroll.common.TiMessenger;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiFileHelper;
import org.appcelerator.titanium.util.TiUIHelper;
import org.appcelerator.titanium.util.TiUrl;

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.MenuItem;
import android.view.MenuItem.OnActionExpandListener;
import android.view.View;

@Kroll.proxy(propertyAccessors={
	TiC.PROPERTY_VISIBLE,
	TiC.PROPERTY_ENABLED,
	TiC.PROPERTY_CHECKED,
	TiC.PROPERTY_CHECKABLE,
	TiC.PROPERTY_TITLE,
	TiC.PROPERTY_TITLE_CONDENSED,
	TiC.PROPERTY_ICON,
	TiC.PROPERTY_ACTION_VIEW,
	TiC.PROPERTY_SHOW_AS_ACTION
})
public class MenuItemProxy extends KrollProxy
{
	private static final String TAG = "MenuItem";

	private MenuItem item;

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

	protected MenuItemProxy(MenuItem item)
	{
		this.item = item;

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			item.setOnActionExpandListener(new ActionExpandListener());
		}
	}

	@Override
	public void handleCreationDict(KrollDict options)
	{
		super.handleCreationDict(options);
		if (options.containsKey(TiC.PROPERTY_VISIBLE)) {
			item.setVisible(TiConvert.toBoolean(options, TiC.PROPERTY_VISIBLE));
		}
		if (options.containsKey(TiC.PROPERTY_ENABLED)) {
			item.setEnabled(TiConvert.toBoolean(options, TiC.PROPERTY_ENABLED));
		}
		if (options.containsKey(TiC.PROPERTY_CHECKED)) {
			item.setChecked(TiConvert.toBoolean(options, TiC.PROPERTY_CHECKED));
		}
		if (options.containsKey(TiC.PROPERTY_CHECKABLE)) {
			item.setCheckable(TiConvert.toBoolean(options, TiC.PROPERTY_CHECKABLE));
		}
		if (options.containsKey(TiC.PROPERTY_TITLE)) {
			item.setTitle(TiConvert.toString(options, TiC.PROPERTY_TITLE));
		}
		if (options.containsKey(TiC.PROPERTY_TITLE_CONDENSED)) {
			item.setTitleCondensed(TiConvert.toString(options, TiC.PROPERTY_TITLE_CONDENSED));
		}
		if (options.containsKey(TiC.PROPERTY_ICON)) {
			item.setIcon(TiUIHelper.getResourceDrawable(options.get(TiC.PROPERTY_ICON)));
		}
		if (options.containsKey(TiC.PROPERTY_ACTION_VIEW)) {
			setActionView(options.get(TiC.PROPERTY_ACTION_VIEW));
		}
		if (options.containsKey(TiC.PROPERTY_SHOW_AS_ACTION)) {
			setShowAsAction(TiConvert.toInt(options, TiC.PROPERTY_SHOW_AS_ACTION));
		}
	}

	public void onPropertyChanged(String name, Object value){
		final String fname = name;
		final Object fvalue = value;
		TiMessenger.postOnMain(new Runnable() {
			public void run() {
				handlePropertyChange(fname, fvalue);
			}
		});
	}

	private void handlePropertyChange(String name, Object value) {
		if (name.equals(TiC.PROPERTY_VISIBLE)) {
			item.setVisible(TiConvert.toBoolean(value));
		}
		else if (name.equals(TiC.PROPERTY_CHECKED)) {
			item.setChecked(TiConvert.toBoolean(value));
		}
		else if (name.equals(TiC.PROPERTY_CHECKABLE)) {
			item.setCheckable(TiConvert.toBoolean(value));
		}
		else if (name.equals(TiC.PROPERTY_ENABLED)) {
			item.setEnabled(TiConvert.toBoolean(value));
		}
		else if (name.equals(TiC.PROPERTY_TITLE)) {
			item.setTitle(value.toString());
		}
		else if (name.equals(TiC.PROPERTY_TITLE_CONDENSED)) {
			item.setTitleCondensed(value.toString());
		}
		else if (name.equals(TiC.PROPERTY_ACTION_VIEW)) {
			setActionView(value);
		}
		else if (name.equals(TiC.PROPERTY_SHOW_AS_ACTION)) {
			setShowAsAction(TiConvert.toInt(value));
		}
		else if (name.equals(TiC.PROPERTY_ICON)) {
			item.setIcon(value != null ? TiUIHelper.getResourceDrawable(value) : null);
		}
		else
		{
			super.onPropertyChanged(name, value);
		}
	}

	@Kroll.method @Kroll.getProperty
	public int getGroupId() {
		return item.getGroupId();
	}
	
	@Kroll.method @Kroll.getProperty
	public int getItemId() {
		return item.getItemId();
	}
	
	@Kroll.method @Kroll.getProperty
	public int getOrder() {
		return item.getOrder();
	}
	
	@Kroll.method
	public boolean hasSubMenu() {
		return item.hasSubMenu();
	}

	private void setActionView(Object view)
	{
		if (view instanceof TiViewProxy) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
				View v = ((TiViewProxy) view).getOrCreateView().getNativeView();
				item.setActionView(v);

			} else {
				Log.i(TAG, "Action bar is not available on this device. Ignoring actionView property.", Log.DEBUG_MODE);
			}
		} else {
			Log.w(TAG, "Invalid type for actionView", Log.DEBUG_MODE);
		}
	}

	private void setShowAsAction(int flag) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			item.setShowAsAction(flag);

		} else {
			Log.i(TAG, "Action bar unsupported by this device. Ignoring showAsAction property.", Log.DEBUG_MODE);
		}
	}

	@Kroll.method
	public void collapseActionView() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			TiMessenger.postOnMain(new Runnable() {
				public void run() {
					item.collapseActionView();
				}
			});

		} else {
			Log.i(TAG, "This device does not support collapsing action views. No operation performed.", Log.DEBUG_MODE);
		}
	}

	@Kroll.method
	public void expandActionView() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			TiMessenger.postOnMain(new Runnable() {
				public void run() {
					item.expandActionView();
				}
			});

		} else {
			Log.i(TAG, "This device does not support expanding action views. No operation performed.", Log.DEBUG_MODE);
		}
	}

	@Kroll.method @Kroll.getProperty
	public boolean isActionViewExpanded() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			return item.isActionViewExpanded();
		}

		// If this system does not support expandable action views, we will
		// always return false since the menu item can never "expand".
		return false;
	}
}
