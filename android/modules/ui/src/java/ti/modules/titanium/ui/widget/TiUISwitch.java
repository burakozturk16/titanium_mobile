/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.ui.widget;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiUIHelper;
import org.appcelerator.titanium.view.TiUIView;

import ti.modules.titanium.ui.android.AndroidModule;
import android.annotation.SuppressLint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ToggleButton;
import android.widget.Switch;

@SuppressLint("NewApi")
public class TiUISwitch extends TiUIView
	implements OnCheckedChangeListener
{
	private static final String TAG = "TiUISwitch";
	
	private boolean oldValue = false;
	private int style = AndroidModule.SWITCH_STYLE_TOGGLEBUTTON;
	private static final boolean ICE_CREAM_OR_GREATER = (Build.VERSION.SDK_INT >= 14);
	
	public TiUISwitch(TiViewProxy proxy) {
		super(proxy);
		Log.d(TAG, "Creating a switch", Log.DEBUG_MODE);

		propertyChanged(TiC.PROPERTY_STYLE, null, proxy.getProperty(TiC.PROPERTY_STYLE), proxy);
	}

	@Override
	public void processProperties(KrollDict d)
	{
		super.processProperties(d);

		if (d.containsKey(TiC.PROPERTY_STYLE)) {
			setStyle(TiConvert.toInt(d.get(TiC.PROPERTY_STYLE), style));
		}

		if (d.containsKey(TiC.PROPERTY_VALUE)) {
			oldValue = TiConvert.toBoolean(d, TiC.PROPERTY_VALUE);
		}

		View nativeView = getNativeView();
		if (nativeView != null) {
			updateButton((CompoundButton)nativeView, d);
		}
	}
	
	private void updateToggleButton(ToggleButton cb, KrollDict d) {
		if (cb == null) return;
		if (d.containsKey(TiC.PROPERTY_TITLE_OFF)) {
			cb.setTextOff(TiConvert.toString(d, TiC.PROPERTY_TITLE_OFF));
		}
		if (d.containsKey(TiC.PROPERTY_TITLE_ON) ) {
			cb.setTextOn(TiConvert.toString(d, TiC.PROPERTY_TITLE_ON));
		}
	}

	private void updateSwitchButton(Switch cb, KrollDict d) {
		if (cb == null) return;
		Log.d(TAG, "updateSwitchButton" + d.toString(), Log.DEBUG_MODE);
		if (d.containsKey(TiC.PROPERTY_TITLE_OFF)) {
			cb.setTextOff(TiConvert.toString(d, TiC.PROPERTY_TITLE_OFF));
		}
		if (d.containsKey(TiC.PROPERTY_TITLE_ON) ) {
			cb.setTextOn(TiConvert.toString(d, TiC.PROPERTY_TITLE_ON));
		}
	}

	protected void updateButton(CompoundButton cb, KrollDict d) {
		boolean backgroundRepeat = d.optBoolean(TiC.PROPERTY_BACKGROUND_REPEAT, false);

		if (d.containsKey(TiC.PROPERTY_TITLE)) {
			cb.setText(TiConvert.toString(d, TiC.PROPERTY_TITLE));
		}
		if (d.containsKey(TiC.PROPERTY_VALUE)) {
		
			cb.setChecked(TiConvert.toBoolean(d, TiC.PROPERTY_VALUE));
		}
		if (d.containsKey(TiC.PROPERTY_COLOR)) {
			cb.setTextColor(TiConvert.toColor(d, TiC.PROPERTY_COLOR));
		}
		if (d.containsKey(TiC.PROPERTY_FONT)) {
			TiUIHelper.styleText(cb, d.getKrollDict(TiC.PROPERTY_FONT));
		}
		if (d.containsKey(TiC.PROPERTY_TEXT_ALIGN)) {
			String textAlign = d.getString(TiC.PROPERTY_TEXT_ALIGN);
			TiUIHelper.setAlignment(cb, textAlign, null);
		}
		if (d.containsKey(TiC.PROPERTY_VERTICAL_ALIGN)) {
			String verticalAlign = d.getString(TiC.PROPERTY_VERTICAL_ALIGN);
			TiUIHelper.setAlignment(cb, null, verticalAlign);
		}
		if (d.containsKey(TiC.PROPERTY_BACKGROUND_CHECKED_COLOR)) {
			getOrCreateBackground().setColorForState(TiUIHelper.BACKGROUND_CHECKED_STATE, TiConvert.toColor(d, TiC.PROPERTY_BACKGROUND_CHECKED_COLOR));
		}
		if (d.containsKey(TiC.PROPERTY_BACKGROUND_CHECKED_IMAGE)) {
			Drawable drawable =  TiUIHelper.buildImageDrawable(TiConvert.toString(d, TiC.PROPERTY_BACKGROUND_CHECKED_IMAGE), backgroundRepeat, proxy);
			getOrCreateBackground().setImageDrawableForState(TiUIHelper.BACKGROUND_CHECKED_STATE, drawable);
		}
		if (d.containsKey(TiC.PROPERTY_BACKGROUND_CHECKED_GRADIENT)) {
			Drawable drawable =  TiUIHelper.buildGradientDrawable(d.getKrollDict(TiC.PROPERTY_BACKGROUND_CHECKED_GRADIENT));
			getOrCreateBackground().setGradientDrawableForState(TiUIHelper.BACKGROUND_CHECKED_STATE, drawable);
		}
		if (cb instanceof ToggleButton) {
			updateToggleButton((ToggleButton) cb, d);
		}
		else if (cb instanceof Switch) {
			updateSwitchButton((Switch) cb, d);
		}
		cb.invalidate();
	}

	private boolean propertyChangedToggleButton(ToggleButton cb, String key, Object oldValue, Object newValue, KrollProxy proxy) {
		if (cb == null) return false;
		if (key.equals(TiC.PROPERTY_TITLE_OFF)) {
			cb.setTextOff((String)  newValue);
		}
		else if (key.equals(TiC.PROPERTY_TITLE_ON)) {
			cb.setTextOn((String)  newValue);
		} else {
			return false;
		}
		return true;
	}

	private boolean propertyChangedSwitchButton(Switch cb, String key, Object oldValue, Object newValue, KrollProxy proxy) {
		if (cb == null) return false;
		if (key.equals(TiC.PROPERTY_TITLE_OFF)) {
			cb.setTextOff((String)  newValue);
		}
		else if (key.equals(TiC.PROPERTY_TITLE_ON)) {
			cb.setTextOn((String)  newValue);
		} else {
			return false;
		}
		return true;
	}

	@Override
	public void propertyChanged(String key, Object oldValue, Object newValue, KrollProxy proxy)
	{
		if (Log.isDebugModeEnabled()) {
			Log.d(TAG, "Property: " + key + " old: " + oldValue + " new: " + newValue, Log.DEBUG_MODE);
		}
		
		CompoundButton cb = (CompoundButton) getNativeView();
		if (key.equals(TiC.PROPERTY_STYLE) && newValue != null) {
			setStyle(TiConvert.toInt(newValue));
		} else if (key.equals(TiC.PROPERTY_TITLE)) {
			cb.setText((String) newValue);
		} else if (key.equals(TiC.PROPERTY_VALUE)) {
			cb.setChecked(TiConvert.toBoolean(newValue));
		} else if (key.equals(TiC.PROPERTY_COLOR)) {
			cb.setTextColor(TiConvert.toColor(TiConvert.toString(newValue)));
		} else if (key.equals(TiC.PROPERTY_FONT)) {
			TiUIHelper.styleText(cb, (KrollDict) newValue);
		} else if (key.equals(TiC.PROPERTY_TEXT_ALIGN)) {
			TiUIHelper.setAlignment(cb, TiConvert.toString(newValue), null);
			cb.requestLayout();
		} else if (key.equals(TiC.PROPERTY_VERTICAL_ALIGN)) {
			TiUIHelper.setAlignment(cb, null, TiConvert.toString(newValue));
			cb.requestLayout();
		} else if (key.equals(TiC.PROPERTY_BACKGROUND_CHECKED_COLOR)) {
			ColorDrawable drawable = TiUIHelper.buildColorDrawable(TiConvert.toString(newValue));		
			getOrCreateBackground().setImageDrawableForState(TiUIHelper.BACKGROUND_CHECKED_STATE, drawable);
		} else if (key.equals(TiC.PROPERTY_BACKGROUND_CHECKED_IMAGE)) {
			boolean repeat = proxy.getProperties().optBoolean(TiC.PROPERTY_BACKGROUND_REPEAT, false);
			Drawable drawable =  TiUIHelper.buildImageDrawable(TiConvert.toString(newValue), repeat, proxy);
			getOrCreateBackground().setImageDrawableForState(TiUIHelper.BACKGROUND_CHECKED_STATE, drawable);
		} else if (key.equals(TiC.PROPERTY_BACKGROUND_CHECKED_GRADIENT)) {
			Drawable drawable =  TiUIHelper.buildGradientDrawable((KrollDict)newValue);
			getOrCreateBackground().setGradientDrawableForState(TiUIHelper.BACKGROUND_CHECKED_STATE, drawable);
		} else if (cb instanceof ToggleButton) {
			propertyChangedToggleButton((ToggleButton) cb, key, oldValue, newValue, proxy);
		} else if (cb instanceof Switch) {
			propertyChangedSwitchButton((Switch) cb, key, oldValue, newValue, proxy);
		} else {
			super.propertyChanged(key, oldValue, newValue, proxy);
		}
	}

	@Override
	public void onCheckedChanged(CompoundButton btn, boolean value) {

		proxy.setProperty(TiC.PROPERTY_VALUE, value);
		//if user triggered change, we fire it.
		if (oldValue != value) {
			oldValue = value;
			if (hasListeners(TiC.EVENT_CHANGE)) {
				KrollDict data = new KrollDict();
				data.put(TiC.PROPERTY_VALUE, value);
				fireEvent(TiC.EVENT_CHANGE, data, false, false);
			}
		}
	}
	
	protected void setStyle(int style)
	{
		CompoundButton currentButton = (CompoundButton) getNativeView();
		CompoundButton button = null;
		if  (!ICE_CREAM_OR_GREATER && style == AndroidModule.SWITCH_STYLE_SWITCH) {
			style = AndroidModule.SWITCH_STYLE_TOGGLEBUTTON;
		}
		this.style = style;
		

		switch (style) {
			case AndroidModule.SWITCH_STYLE_CHECKBOX:
				if (!(currentButton instanceof CheckBox)) {
					button = new CheckBox(proxy.getActivity())
					{
						@Override
						protected void onLayout(boolean changed, int left, int top, int right, int bottom)
						{
							super.onLayout(changed, left, top, right, bottom);
							TiUIHelper.firePostLayoutEvent(TiUISwitch.this);
						}
						
						@Override
						public boolean dispatchTouchEvent(MotionEvent event) {
							if (touchPassThrough == true)
								return false;
							return super.dispatchTouchEvent(event);
						}
					};
				}
				break;

			case AndroidModule.SWITCH_STYLE_TOGGLEBUTTON:
				if (!(currentButton instanceof ToggleButton)) {
					button = new ToggleButton(proxy.getActivity())
					{
						@Override
						protected void onLayout(boolean changed, int left, int top, int right, int bottom)
						{
							super.onLayout(changed, left, top, right, bottom);
							TiUIHelper.firePostLayoutEvent(TiUISwitch.this);
						}
						
						@Override
						public boolean dispatchTouchEvent(MotionEvent event) {
							if (touchPassThrough == true)
								return false;
							return super.dispatchTouchEvent(event);
						}
					};
				}
				break;

			case AndroidModule.SWITCH_STYLE_SWITCH:
				if (!(currentButton instanceof Switch)) {
					button = new Switch(proxy.getActivity())
					{
						@Override
						protected void onLayout(boolean changed, int left, int top, int right, int bottom)
						{
							super.onLayout(changed, left, top, right, bottom);
							TiUIHelper.firePostLayoutEvent(TiUISwitch.this);
						}
						
						@Override
						public boolean dispatchTouchEvent(MotionEvent event) {
							if (touchPassThrough == true)
								return false;
							return super.dispatchTouchEvent(event);
						}
					};
				}
				break;

			default:
				return;
		}

		if (button != null) {
			setNativeView(button);
			updateButton(button, proxy.getProperties());
			button.setOnCheckedChangeListener(this);
		}
	}
}
