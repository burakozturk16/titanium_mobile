/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2012-2013 by Appcelerator, Inc. All Rights Reserved.
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
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiBaseActivity;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiFileHelper;
import org.appcelerator.titanium.util.TiRHelper;
import org.appcelerator.titanium.util.TiRHelper.ResourceNotFoundException;
import org.appcelerator.titanium.util.TiUIHelper;
import org.appcelerator.titanium.util.TiUrl;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Message;
import android.util.TypedValue;

@SuppressLint("InlinedApi")
@Kroll.proxy(propertyAccessors = {
		TiC.PROPERTY_ON_HOME_ICON_ITEM_SELECTED,
		TiC.PROPERTY_DISPLAY_HOME_AS_UP,
		TiC.PROPERTY_ICON
})

public class ActionBarProxy extends KrollProxy implements KrollProxyListener
{
	private static final int MSG_FIRST_ID = KrollProxy.MSG_LAST_ID + 1;
	private static final int MSG_DISPLAY_HOME_AS_UP = MSG_FIRST_ID + 100;
	private static final int MSG_SET_BACKGROUND_IMAGE = MSG_FIRST_ID + 101;
	private static final int MSG_SET_TITLE = MSG_FIRST_ID + 102;
	private static final int MSG_SHOW = MSG_FIRST_ID + 103;
	private static final int MSG_HIDE = MSG_FIRST_ID + 104;
	private static final int MSG_SET_LOGO = MSG_FIRST_ID + 105;
	private static final int MSG_SET_ICON = MSG_FIRST_ID + 106;
	private static final int MSG_SET_HOME_BUTTON_ENABLED = MSG_FIRST_ID + 107;
	private static final int MSG_SET_NAVIGATION_MODE = MSG_FIRST_ID + 108;
	private static final int MSG_SET_BACKGROUND_COLOR = MSG_FIRST_ID + 109;
	private static final int MSG_SET_BACKGROUND_GRADIENT = MSG_FIRST_ID + 110;
	private static final int MSG_RESET_BACKGROUND = MSG_FIRST_ID + 111;
	private static final int MSG_RESET_ICON = MSG_FIRST_ID + 112;
	private static final int MSG_SET_SUBTITLE = MSG_FIRST_ID + 113;

	private static final String SHOW_HOME_AS_UP = "showHomeAsUp";
	private static final String BACKGROUND_IMAGE = "backgroundImage";
	private static final String TITLE = "title";
	private static final String LOGO = "logo";
	private static final String ICON = "icon";
	private static final String NAVIGATION_MODE = "navigationMode";
	private static final String TAG = "ActionBarProxy";

	private ActionBar actionBar;
	private Drawable themeBackgroundDrawable;
	private Drawable themeIconDrawable;

	public ActionBarProxy(TiBaseActivity activity)
	{
		super();
		actionBar = activity.getSupportActionBar();
		themeIconDrawable = getActionBarIcon(activity);
		themeBackgroundDrawable = getActionBarBackground(activity);
		setModelListener(this, false);
		
	}
	
	protected static TypedArray obtainStyledAttrsFromThemeAttr(Context context,
            int[] styleAttrs) throws ResourceNotFoundException {
        // Need to get resource id of style pointed to from the theme attr
        TypedValue outValue = new TypedValue();
    	int resourceId = TiRHelper.getResource("com.actionbarsherlock.R$", "attr.actionBarStyle");
       context.getTheme().resolveAttribute(resourceId, outValue, true);
        final int styleResId =  outValue.resourceId;

        // Now return the values (from styleAttrs) from the style
        return context.obtainStyledAttributes(styleResId, styleAttrs);
    }
	
	protected Drawable getActionBarBackground(Context context) {
        int[] android_styleable_ActionBar = {android.R.attr.background};
        TypedArray abStyle = null;
        
        try {
        	abStyle = obtainStyledAttrsFromThemeAttr(context, android_styleable_ActionBar);
            // background is the first attr in the array above so it's index is 0.
            return abStyle.getDrawable(0);
        } catch (ResourceNotFoundException e) {
			return null;
		} finally {
            if (abStyle != null) abStyle.recycle();
        }
    }

    protected int getActionBarSize(Context context) {
        int[] attrs = {	android.R.attr.actionBarSize};
        TypedArray values = context.getTheme().obtainStyledAttributes(attrs);
        try {
            return values.getDimensionPixelSize(0, 0);
        } finally {
            values.recycle();
        }
    }
	

	protected Drawable getActionBarIcon(Context context) {
        int[] android_styleable_ActionBar = {android.R.attr.icon};

        // Now get the action bar style values...
        TypedArray abStyle = null;
        try {
        	abStyle = obtainStyledAttrsFromThemeAttr(context, android_styleable_ActionBar);
       	int count = abStyle.getIndexCount();
        	if (count > 0) {
	            return abStyle.getDrawable(0);
        	}
        	return context.getApplicationInfo().loadIcon(context.getPackageManager()); 
        } catch (ResourceNotFoundException e) {
			return null;
		} finally {
            if (abStyle != null) abStyle.recycle();
        }
    }

	@Kroll.method @Kroll.setProperty
	private void setDisplayHomeAsUp(boolean showHomeAsUp)
	{
		if(TiApplication.isUIThread()) {
			handlesetDisplayHomeAsUp(showHomeAsUp);
		} else {
			Message message = getMainHandler().obtainMessage(MSG_DISPLAY_HOME_AS_UP, showHomeAsUp);
			message.getData().putBoolean(SHOW_HOME_AS_UP, showHomeAsUp);
			message.sendToTarget();
		}
	}

	@Kroll.method @Kroll.setProperty
	public void setNavigationMode(int navigationMode)
	{
		if (TiApplication.isUIThread()) {
			handlesetNavigationMode(navigationMode);
		} else {
			Message message = getMainHandler().obtainMessage(MSG_SET_NAVIGATION_MODE, navigationMode);
			message.getData().putInt(NAVIGATION_MODE, navigationMode);
			message.sendToTarget();
		}
	}

	public void setBackgroundImage(String url)
	{
		if (TiApplication.isUIThread()) {
			handleSetBackgroundImage(url);
		} else {
			Message message = getMainHandler().obtainMessage(MSG_SET_BACKGROUND_IMAGE, url);
			message.getData().putString(BACKGROUND_IMAGE, url);
			message.sendToTarget();
		}
	}
	
	public void setBackgroundColor(int color)
	{
		if (TiApplication.isUIThread()) {
			handleSetBackgroundColor(color);
		} else {
			getMainHandler().obtainMessage(MSG_SET_BACKGROUND_COLOR, Integer.valueOf(color)).sendToTarget();
		}
	}
	
	public void setBackgroundGradient(KrollDict gradient)
	{
		if (TiApplication.isUIThread()) {
			handleSetBackgroundGradient(gradient);
		} else {
			getMainHandler().obtainMessage(MSG_SET_BACKGROUND_GRADIENT, gradient).sendToTarget();
		}
	}

	@Kroll.method @Kroll.setProperty
	public void setTitle(String title)
	{
		if (TiApplication.isUIThread()) {
			handleSetTitle(title);
		} else {
			Message message = getMainHandler().obtainMessage(MSG_SET_TITLE, title);
			message.getData().putString(TITLE, title);
			message.sendToTarget();
		}
	}

	@Kroll.method @Kroll.getProperty
	public String getTitle()
	{
		if (actionBar == null) {
			return null;
		}
		return (String) actionBar.getTitle();
	}

	@Kroll.method @Kroll.setProperty
	public void setSubtitle(String subTitle)
	{
		if (TiApplication.isUIThread()) {
			handleSetSubTitle(subTitle);
		} else {
			Message message = getMainHandler().obtainMessage(MSG_SET_SUBTITLE, subTitle);
			message.getData().putString(TiC.PROPERTY_SUBTITLE, subTitle);
			message.sendToTarget();
		}
	}
	
	@Kroll.method @Kroll.getProperty
	public String getSubtitle()
	{
		if (actionBar == null) {
			return null;
		}
		return (String) actionBar.getSubtitle();
	}

	public int getNavigationMode()
	{
		if (actionBar == null) {
			return 0;
		}
		return (int) actionBar.getNavigationMode();
	}

	@Kroll.method
	public void show()
	{
		if (TiApplication.isUIThread()) {
			handleShow();
		} else {
			getMainHandler().obtainMessage(MSG_SHOW).sendToTarget();
		}
	}

	@Kroll.method
	public void hide()
	{
		if (TiApplication.isUIThread()) {
			handleHide();
		} else {
			getMainHandler().obtainMessage(MSG_HIDE).sendToTarget();
		}
	}

	public void setLogo(String url)
	{
		if (TiApplication.isUIThread()) {
			handleSetLogo(url);
		} else {
			Message message = getMainHandler().obtainMessage(MSG_SET_LOGO, url);
			message.getData().putString(LOGO, url);
			message.sendToTarget();
		}
	}

	public void setIcon(Object value)
	{
		if (Build.VERSION.SDK_INT >= TiC.API_LEVEL_ICE_CREAM_SANDWICH) {
			if (TiApplication.isUIThread()) {
				handleSetIcon(value);
			} else {
				getMainHandler().obtainMessage(MSG_SET_ICON, value).sendToTarget();
			}
		}
	}

	private void handleSetIcon(Object value)
	{
		if (actionBar == null) {
			Log.w(TAG, "ActionBar is not enabled");
			return;
		}
		Drawable icon = null;
		if (value instanceof String) {
			 icon = getDrawableFromUrl(TiConvert.toString(value));
		}
		else {
			icon = TiUIHelper.getResourceDrawable(TiConvert.toInt(value));
		}
		if (icon != null) {
			actionBar.setIcon(icon);
		} 
	}
	
	private void handleSetTitle(String title)
	{
		if (actionBar != null) {
			actionBar.setTitle(title);
		} else {
			Log.w(TAG, "ActionBar is not enabled");
		}
	}

	private void handleSetSubTitle(String subTitle)
	{
		if (actionBar != null) {
			actionBar.setDisplayShowTitleEnabled(true);
			actionBar.setSubtitle(subTitle);
		} else {
			Log.w(TAG, "ActionBar is not enabled");
		}
	}
	
	private void handleShow()
	{
		if (actionBar != null) {
			actionBar.show();
		} else {
			Log.w(TAG, "ActionBar is not enabled");
		}
	}

	private void handleHide()
	{
		if (actionBar != null) {
			actionBar.hide();
		} else {
			Log.w(TAG, "ActionBar is not enabled");
		}
	}

	private void handleSetBackgroundImage(String url)
	{
		if (actionBar == null) {
			Log.w(TAG, "ActionBar is not enabled");
			return;
		}

		Drawable backgroundImage = getDrawableFromUrl(url);
		//This is a workaround due to https://code.google.com/p/styled-action-bar/issues/detail?id=3. [TIMOB-12148]
		if (backgroundImage != null) {
			actionBar.setDisplayShowTitleEnabled(false);
			actionBar.setDisplayShowTitleEnabled(true);
			actionBar.setBackgroundDrawable(backgroundImage);
		}
	}
	
	private void handleSetBackgroundColor(int color)
	{
		if (actionBar == null) {
			Log.w(TAG, "ActionBar is not enabled");
			return;
		}
		actionBar.setBackgroundDrawable(new ColorDrawable(color));
	}
	
	private void handleSetBackgroundGradient(KrollDict gradDict)
	{
		if (actionBar == null) {
			Log.w(TAG, "ActionBar is not enabled");
			return;
		}
		Drawable drawable =  TiUIHelper.buildGradientDrawable(gradDict);

		actionBar.setBackgroundDrawable(drawable);
	}
	
	private void activateHomeButton(boolean value)
	{
		if (actionBar == null) {
			Log.w(TAG, "ActionBar is not enabled");
			return;
		}

		// If we have a listener on the home icon item, then enable the home button (we need to do this for ICS and
		// above)
		if (TiApplication.isUIThread()) {
			actionBar.setHomeButtonEnabled(value);
		} else {
			getMainHandler().obtainMessage(MSG_SET_HOME_BUTTON_ENABLED, Boolean.valueOf(value)).sendToTarget();
		}
	}


	private void handlesetDisplayHomeAsUp(boolean showHomeAsUp)
	{
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(showHomeAsUp);
		} else {
			Log.w(TAG, "ActionBar is not enabled");
		}
	}

	private void handlesetNavigationMode(int navigationMode)
	{
		actionBar.setNavigationMode(navigationMode);
	}

	private void handleSetLogo(String url)
	{
		if (actionBar == null) {
			Log.w(TAG, "ActionBar is not enabled");
			return;
		}

		Drawable logo = getDrawableFromUrl(url);
		if (logo != null) {
			actionBar.setLogo(logo);
		}
	}

	private Drawable getDrawableFromUrl(String url)
	{
		TiUrl imageUrl = new TiUrl((String) url);
		TiFileHelper tfh = new TiFileHelper(TiApplication.getInstance());
		return tfh.loadDrawable(imageUrl.resolve(), false);
	}

	@Override
	public boolean handleMessage(Message msg)
	{
		switch (msg.what) {
			case MSG_DISPLAY_HOME_AS_UP:
				handlesetDisplayHomeAsUp(msg.getData().getBoolean(SHOW_HOME_AS_UP));
				return true;
			case MSG_SET_NAVIGATION_MODE:
				handlesetNavigationMode(msg.getData().getInt(NAVIGATION_MODE));
				return true;
			case MSG_SET_BACKGROUND_COLOR:
				handleSetBackgroundColor((Integer)msg.obj);
				return true;
			case MSG_SET_BACKGROUND_IMAGE:
				handleSetBackgroundImage(msg.getData().getString(BACKGROUND_IMAGE));
				return true;
			case MSG_SET_BACKGROUND_GRADIENT:
				handleSetBackgroundGradient((KrollDict) (msg.obj));
				return true;
			case MSG_SET_TITLE:
				handleSetTitle(msg.getData().getString(TITLE));
				return true;
			case MSG_SET_SUBTITLE:
				handleSetSubTitle(msg.getData().getString(TiC.PROPERTY_SUBTITLE));
				return true;

			case MSG_SHOW:
				handleShow();
				return true;
			case MSG_HIDE:
				handleHide();
				return true;
			case MSG_RESET_BACKGROUND:
				actionBar.setBackgroundDrawable(themeBackgroundDrawable);
				return true;
			case MSG_RESET_ICON:
				actionBar.setIcon(themeIconDrawable);
				return true;
			case MSG_SET_LOGO:
				handleSetLogo(msg.getData().getString(LOGO));
				return true;
			case MSG_SET_ICON:
				handleSetIcon(msg.obj);
				return true;
			case MSG_SET_HOME_BUTTON_ENABLED:
				actionBar.setHomeButtonEnabled((Boolean)msg.obj);
				return true;
		}
		return super.handleMessage(msg);
	}

	public void processProperties(KrollDict properties) {
		if (actionBar == null) return;
		if (!properties.containsKey(TiC.PROPERTY_BACKGROUND_COLOR) && 
				!properties.containsKey(TiC.PROPERTY_BACKGROUND_IMAGE) &&  
				!properties.containsKey(TiC.PROPERTY_BACKGROUND_GRADIENT) )
		{
			if (TiApplication.isUIThread()) {
				actionBar.setBackgroundDrawable(themeBackgroundDrawable);
			} else {
				getMainHandler().obtainMessage(MSG_RESET_BACKGROUND).sendToTarget();
			}
		}
		activateHomeButton(properties.get(TiC.PROPERTY_ON_HOME_ICON_ITEM_SELECTED) != null);
		
		setDisplayHomeAsUp(properties.optBoolean(TiC.PROPERTY_DISPLAY_HOME_AS_UP, false));
		if (properties.containsKey(TiC.PROPERTY_BACKGROUND_IMAGE)) {
			setBackgroundImage(properties.getString(TiC.PROPERTY_BACKGROUND_IMAGE));
		}
		if (properties.containsKey(TiC.PROPERTY_BACKGROUND_COLOR)) {
			setBackgroundColor(properties.getColor(TiC.PROPERTY_BACKGROUND_COLOR));
		}
		if (properties.containsKey(TiC.PROPERTY_BACKGROUND_GRADIENT)) {
			setBackgroundGradient(properties.getKrollDict(TiC.PROPERTY_BACKGROUND_GRADIENT));
		}
		if (properties.containsKey(TiC.PROPERTY_LOGO)) {
			setLogo(properties.getString(TiC.PROPERTY_LOGO));
		}
		Object obj= properties.get(TiC.PROPERTY_ICON);
		if (obj != null) {
			if (obj instanceof String) {
				setIcon((String)obj);
			}
			else if (obj instanceof Number){
				setIcon(TiConvert.toInt(obj));
			}
		}
		else {
			if (TiApplication.isUIThread()) {
				actionBar.setIcon(themeIconDrawable);
			} else {
				getMainHandler().obtainMessage(MSG_RESET_ICON).sendToTarget();
			}
		}
	}
	
	public void propertyChanged(String key, Object oldValue, Object newValue,
			KrollProxy proxy) {		
		if (key.equals(TiC.PROPERTY_BACKGROUND_COLOR)) {
			setBackgroundColor(TiConvert.toColor((String) newValue));
		} else if (key.equals(TiC.PROPERTY_BACKGROUND_IMAGE)) {
			setBackgroundImage(TiConvert.toString(newValue));
		} else if (key.equals(TiC.PROPERTY_BACKGROUND_GRADIENT)) {
				setBackgroundGradient((KrollDict)newValue);
		} else if (key.equals(TiC.PROPERTY_LOGO)) {
			setLogo(TiConvert.toString(newValue));
		} else if (key.equals(TiC.PROPERTY_DISPLAY_HOME_AS_UP)) {
			setDisplayHomeAsUp(TiConvert.toBoolean(newValue, false));
		} else if (key.equals(TiC.PROPERTY_ON_HOME_ICON_ITEM_SELECTED)) {
			activateHomeButton((newValue != null));
		} else if (key.equals(TiC.PROPERTY_ICON)) {
			if (newValue != null) {
				if (newValue instanceof String) {
					setIcon((String)newValue);
				}
				else if (newValue instanceof Number){
					setIcon(TiConvert.toInt(newValue));
				}
			}
			else {
				if (TiApplication.isUIThread()) {
					actionBar.setIcon(themeIconDrawable);
				} else {
					getMainHandler().obtainMessage(MSG_RESET_ICON).sendToTarget();
				}
			}
		}
	}

	public void propertiesChanged(List<KrollPropertyChange> changes,
			KrollProxy proxy) {
	}

	public void listenerAdded(String type, int count, KrollProxy proxy) {		
	}

	public void listenerRemoved(String type, int count, KrollProxy proxy) {		
	}
	@Override
	public String getApiName()
	{
		return "Ti.Android.ActionBar";
	}
}
