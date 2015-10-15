/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.ui.widget;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiUIHelper;
import org.appcelerator.titanium.view.TiBackgroundDrawable;
import org.appcelerator.titanium.view.TiUIView;

import ti.modules.titanium.ui.android.AndroidModule;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.SwitchCompat;
import android.view.MotionEvent;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ToggleButton;

public class TiUISwitch extends TiUIView
	implements OnCheckedChangeListener
{
	private static final String TAG = "TiUISwitch";
	
	private boolean ignoreChangeEvent = false;
	private int style = AndroidModule.SWITCH_STYLE_SWITCH;
	
	public TiUISwitch(TiViewProxy proxy) {
		super(proxy);
	}
	
	private void propertySetSwitch(String key, Object newValue, Object oldValue,
            boolean changedProperty) {
	    switch (key) {
        case TiC.PROPERTY_TITLE_OFF:
            getSwitch().setTextOff(TiConvert.toString(newValue));
            break;
        case TiC.PROPERTY_TITLE_ON:
            getSwitch().setTextOn(TiConvert.toString(newValue));
            break;
        default:
            propertySetCompound(key, newValue, oldValue, changedProperty);
            break;
        }
	}
	
	private void propertySetToggle(String key, Object newValue, Object oldValue,
            boolean changedProperty) {
        switch (key) {
        case TiC.PROPERTY_TITLE_OFF:
            getToggle().setTextOff(TiConvert.toString(newValue));
            break;
        case TiC.PROPERTY_TITLE_ON:
            getToggle().setTextOn(TiConvert.toString(newValue));
            break;
        default:
            propertySetCompound(key, newValue, oldValue, changedProperty);
            break;
        }
    }
	
	private void propertySetCompound(String key, Object newValue, Object oldValue,
            boolean changedProperty) {
	    if (key.startsWith(TiC.PROPERTY_BACKGROUND_PREFIX)) {
            TiBackgroundDrawable bgdDrawable = getOrCreateBackground();
            switch (key) {
            case TiC.PROPERTY_BACKGROUND_CHECKED_COLOR:
                bgdDrawable.setColorForState(
                        TiUIHelper.BACKGROUND_CHECKED_STATE,
                        TiConvert.toColor(newValue));
                break;
            case TiC.PROPERTY_BACKGROUND_CHECKED_GRADIENT:
            {
                Drawable drawable = TiUIHelper.buildGradientDrawable(TiConvert
                        .toKrollDict(newValue));
                bgdDrawable.setGradientDrawableForState(
                        TiUIHelper.BACKGROUND_CHECKED_STATE, drawable);
                break;
            }
            case TiC.PROPERTY_BACKGROUND_CHECKED_IMAGE:
            {
                boolean repeat = proxy.getProperties().optBoolean(
                        TiC.PROPERTY_BACKGROUND_REPEAT, false);
                setBackgroundImageDrawable(newValue, repeat,
                        new int[][] { TiUIHelper.BACKGROUND_CHECKED_STATE });
                break;
            }
            default:
                super.propertySet(key, newValue, oldValue, changedProperty);
                break;
            }
            if (changedProperty)
                bgdDrawable.invalidateSelf();
            return;
        }
        switch (key) {
        case TiC.PROPERTY_STYLE:
            setStyle(TiConvert.toInt(newValue, style));
            break;
        case TiC.PROPERTY_VALUE:
            ignoreChangeEvent = true;
            getButton().setChecked(TiConvert.toBoolean(newValue, false));
            break;
        case TiC.PROPERTY_TITLE:
            getButton().setText(TiConvert.toString(newValue));
            break;
        case TiC.PROPERTY_COLOR:
            getButton().setTextColor(TiConvert.toColor(newValue));
            break;
        case TiC.PROPERTY_FONT:
            TiUIHelper.styleText(getButton(), TiConvert.toKrollDict(TiC.PROPERTY_FONT));
            break;
        case TiC.PROPERTY_TEXT_ALIGN:
            TiUIHelper.setAlignment(getButton(), TiConvert.toString(newValue), null);
            break;
        case TiC.PROPERTY_VERTICAL_ALIGN:
            TiUIHelper.setAlignment(getButton(), null, TiConvert.toString(newValue));
            break;
        default:
            super.propertySet(key, newValue, oldValue, changedProperty);
            break;
        }
    }
	
	@Override
    public void propertySet(String key, Object newValue, Object oldValue,
            boolean changedProperty) {
	    if (isToggle()) {
	        propertySetToggle(key, newValue, oldValue, changedProperty);
        } else if (isSwitch()) {
            propertySetSwitch(key, newValue, oldValue, changedProperty);
        } else {
            propertySetCompound(key, newValue, oldValue, changedProperty);
        }
    }
    
	
    @Override
	protected void aboutToProcessProperties(KrollDict d) {
      //make sure style is handled first to create the button
        if (d.containsKey(TiC.PROPERTY_STYLE)) {
            propertySet(TiC.PROPERTY_STYLE, d.get(TiC.PROPERTY_STYLE), style, false);
            d.remove(TiC.PROPERTY_STYLE);
        }
        super.aboutToProcessProperties(d);
    }
    
    @Override
    protected void didProcessProperties() {
        super.didProcessProperties();
        getButton().invalidate();
        ignoreChangeEvent = false;
    }
	
	private CompoundButton getButton() {
	       return (CompoundButton) getNativeView();
	}
	
	private SwitchCompat getSwitch() {
        return (SwitchCompat) getNativeView();
	}
	
	private ToggleButton getToggle() {
        return (ToggleButton) getNativeView();
    }
	
	private boolean isSwitch() {
	    return style == AndroidModule.SWITCH_STYLE_SWITCH;
	}
	
	private boolean isToggle() {
        return style == AndroidModule.SWITCH_STYLE_TOGGLEBUTTON;
    }

	@Override
	public void onCheckedChanged(CompoundButton btn, boolean value) {
	    
	    if (!ignoreChangeEvent) {
	        proxy.setProperty(TiC.PROPERTY_VALUE, value);
            if (hasListeners(TiC.EVENT_CHANGE)) {
                KrollDict data = new KrollDict();
                data.put(TiC.PROPERTY_VALUE, value);
                fireEvent(TiC.EVENT_CHANGE, data, false, false);
            }
	    }
        ignoreChangeEvent = false;
	}
	
	protected void setStyle(int style)
	{
		CompoundButton currentButton = (CompoundButton) getNativeView();
        if (this.style == style && currentButton != null) {
            return;
        }
		CompoundButton button = null;
		this.style = style;

		switch (style) {
			case AndroidModule.SWITCH_STYLE_CHECKBOX:
				if (!(currentButton instanceof AppCompatCheckBox)) {
					button = new AppCompatCheckBox(proxy.getActivity())
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
				if (!(currentButton instanceof SwitchCompat)) {
					button = new SwitchCompat(proxy.getActivity())
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

		if (button != null && currentButton != button) {
			setNativeView(button);
			if (currentButton != null) {
	            currentButton.setOnCheckedChangeListener(null);
	            currentButton = null;
	            //we need to reprocess properties
	            processProperties(proxy.getProperties());
			}
            button.setOnCheckedChangeListener(this);
		}
	}
}
