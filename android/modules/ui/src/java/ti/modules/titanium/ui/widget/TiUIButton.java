/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.ui.widget;

import java.util.HashMap;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiUIHelper;
import org.appcelerator.titanium.view.TiDrawableReference;
import org.appcelerator.titanium.view.TiUIView;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.content.res.ColorStateList;

public class TiUIButton extends TiUIView
{
	private static final String TAG = "TiUIButton";
	
	private int defaultColor;

	private int shadowColor;
	private int shadowDx;
	private int shadowDy;
	private float shadowRadius;
	private Rect titlePadding;
	private Drawable imageDrawable;
	private int imageGravity;

	public TiUIButton(final TiViewProxy proxy)
	{
		super(proxy);
		shadowColor = 0;
		shadowRadius = 1;
		shadowDx = 0;
		shadowDy = 0;
		imageGravity = Gravity.LEFT;
		titlePadding = new Rect();
		titlePadding.left = 8;
		titlePadding.right = 8;
		Log.d(TAG, "Creating a button", Log.DEBUG_MODE);
		Button btn = new Button(proxy.getActivity());
		btn.setPadding(titlePadding.left, titlePadding.top, titlePadding.right, titlePadding.bottom);
		btn.setGravity(Gravity.CENTER);
		defaultColor = btn.getCurrentTextColor();
		setNativeView(btn, true);
	}

	private void setTextColors(int color, int selectedColor) {
		ColorStateList colorStateList = new ColorStateList(
			new int[][] {	new int [] {android.R.attr.state_pressed},
							new int [] {android.R.attr.state_focused},
							new int [] {}},
			new int[] {selectedColor, selectedColor, color}
		);
		((Button) getNativeView()).setTextColor(colorStateList);
	}

	private void updateImage(){
		Button btn = (Button) getNativeView();
		if (btn != null) {
			switch(imageGravity) {
				case Gravity.LEFT:
				default:
					btn.setCompoundDrawablesWithIntrinsicBounds(imageDrawable, null, null, null);
					break;
				case Gravity.TOP:
					btn.setCompoundDrawablesWithIntrinsicBounds(null, imageDrawable, null, null);
					break;
				case Gravity.RIGHT:
					btn.setCompoundDrawablesWithIntrinsicBounds(null, null, imageDrawable, null);
					break;
				case Gravity.BOTTOM:
					btn.setCompoundDrawablesWithIntrinsicBounds(null, null, null, imageDrawable);
					break;
			}
		}
	}

	@Override
	public void processProperties(KrollDict d)
	{
		super.processProperties(d);

		Button btn = (Button) getNativeView();
		if (d.containsKey(TiC.PROPERTY_IMAGE)) {
			Object value = d.get(TiC.PROPERTY_IMAGE);
			TiDrawableReference drawableRef = TiDrawableReference.fromObject(proxy.getActivity(), value);

			if (drawableRef != null) {
				imageDrawable = drawableRef.getDrawable();
			}
			else {
				imageDrawable = null;
			}
		}
		if (d.containsKey(TiC.PROPERTY_TITLE)) {
			btn.setText(d.getString(TiC.PROPERTY_TITLE));
		}
		if (d.containsKey(TiC.PROPERTY_COLOR) || d.containsKey(TiC.PROPERTY_SELECTED_COLOR)) {
			int color = d.optColor(TiC.PROPERTY_COLOR, defaultColor);
			int selectedColor = d.optColor(TiC.PROPERTY_SELECTED_COLOR, color);
			setTextColors(color, selectedColor);
		}
		if (d.containsKey(TiC.PROPERTY_FONT)) {
			TiUIHelper.styleText(btn, d.getKrollDict(TiC.PROPERTY_FONT));
		}
		if (d.containsKey(TiC.PROPERTY_TEXT_ALIGN)) {
			String textAlign = d.getString(TiC.PROPERTY_TEXT_ALIGN);
			TiUIHelper.setAlignment(btn, textAlign, null);
		}
		if (d.containsKey(TiC.PROPERTY_VERTICAL_ALIGN)) {
			String verticalAlign = d.getString(TiC.PROPERTY_VERTICAL_ALIGN);
			TiUIHelper.setAlignment(btn, null, verticalAlign);
		}
		if (d.containsKey(TiC.PROPERTY_TITLE_PADDING)) {
			KrollDict dict = d.getKrollDict(TiC.PROPERTY_TITLE_PADDING);
			if (dict.containsKey(TiC.PROPERTY_LEFT)) {
				titlePadding.left = (int) TiUIHelper.getRawSizeOrZero(dict, TiC.PROPERTY_LEFT);
			}
			if (dict.containsKey(TiC.PROPERTY_RIGHT)) {
				titlePadding.right =  (int) TiUIHelper.getRawSizeOrZero(dict, TiC.PROPERTY_RIGHT);
			}
			if (dict.containsKey(TiC.PROPERTY_TOP)) {
				titlePadding.top =  (int) TiUIHelper.getRawSizeOrZero(dict, TiC.PROPERTY_TOP);
			}
			if (dict.containsKey(TiC.PROPERTY_BOTTOM)) {
				titlePadding.bottom =  (int) TiUIHelper.getRawSizeOrZero(dict, TiC.PROPERTY_BOTTOM);
			}
			btn.setPadding(titlePadding.left, titlePadding.top, titlePadding.right, titlePadding.bottom);
		}
		if (d.containsKey(TiC.PROPERTY_IMAGE_ANCHOR)) {
			imageGravity = TiUIHelper.getGravity(d.getString(TiC.PROPERTY_IMAGE_ANCHOR), false);
		}
		if (d.containsKey(TiC.PROPERTY_SHADOW_COLOR)) {
			shadowColor = TiConvert.toColor(d, TiC.PROPERTY_SHADOW_COLOR);
			btn.setShadowLayer(shadowRadius, shadowDx, shadowDy, shadowColor);
		}
		if (d.containsKey(TiC.PROPERTY_SHADOW_RADIUS)) {
			shadowRadius = TiConvert.toFloat(d, TiC.PROPERTY_SHADOW_RADIUS);
			btn.setShadowLayer(shadowRadius, shadowDx, shadowDy, shadowColor);
		}
		if (d.containsKey(TiC.PROPERTY_SHADOW_OFFSET)) {
			KrollDict value = d.getKrollDict(TiC.PROPERTY_SHADOW_OFFSET);
			shadowDx = value.getInt(TiC.PROPERTY_X);
			shadowDy = value.getInt(TiC.PROPERTY_Y);
			btn.setShadowLayer(shadowRadius, shadowDx, shadowDy, shadowColor);
		}
		if (d.containsKey(TiC.PROPERTY_WORD_WRAP)) {
			btn.setSingleLine(!TiConvert.toBoolean(d, TiC.PROPERTY_WORD_WRAP));
		}
		if (d.containsKey(TiC.PROPERTY_SELECTED)) {
			btn.setPressed(TiConvert.toBoolean(d, TiC.PROPERTY_SELECTED));
		}
		updateImage();
		btn.invalidate();
	}

	@Override
	public void propertyChanged(String key, Object oldValue, Object newValue, KrollProxy proxy)
	{
		if (Log.isDebugModeEnabled()) {
			Log.d(TAG, "Property: " + key + " old: " + oldValue + " new: " + newValue, Log.DEBUG_MODE);
		}
		Button btn = (Button) getNativeView();
		if (key.equals(TiC.PROPERTY_TITLE)) {
			btn.setText((String) newValue);
		} else if (key.equals(TiC.PROPERTY_COLOR)) {
			int color = TiConvert.toColor(TiConvert.toString(newValue));
			int selectedColor = proxy.getProperties().optColor(TiC.PROPERTY_SELECTED_COLOR, color);
			setTextColors(color, selectedColor);
		} else if (key.equals(TiC.PROPERTY_SELECTED_COLOR)) {
			btn.setTextColor(TiConvert.toColor(TiConvert.toString(newValue)));
			int selectedColor = TiConvert.toColor(TiConvert.toString(newValue));
			int color = proxy.getProperties().optColor(TiC.PROPERTY_COLOR, selectedColor);
			setTextColors(color, selectedColor);
		} else if (key.equals(TiC.PROPERTY_FONT)) {
			TiUIHelper.styleText(btn, (HashMap) newValue);
		} else if (key.equals(TiC.PROPERTY_TEXT_ALIGN)) {
			TiUIHelper.setAlignment(btn, TiConvert.toString(newValue), null);
			btn.requestLayout();
		} else if (key.equals(TiC.PROPERTY_VERTICAL_ALIGN)) {
			TiUIHelper.setAlignment(btn, null, TiConvert.toString(newValue));
			btn.requestLayout();
		} else if (key.equals(TiC.PROPERTY_TITLE_PADDING)) {
			KrollDict dict = (KrollDict) newValue;
			if (dict.containsKey(TiC.PROPERTY_LEFT)) {
				titlePadding.left = (int) TiUIHelper.getRawSizeOrZero(dict, TiC.PROPERTY_LEFT);
			}
			if (dict.containsKey(TiC.PROPERTY_RIGHT)) {
				titlePadding.right =  (int) TiUIHelper.getRawSizeOrZero(dict, TiC.PROPERTY_RIGHT);
			}
			if (dict.containsKey(TiC.PROPERTY_TOP)) {
				titlePadding.top =  (int) TiUIHelper.getRawSizeOrZero(dict, TiC.PROPERTY_TOP);
			}
			if (dict.containsKey(TiC.PROPERTY_BOTTOM)) {
				titlePadding.bottom =  (int) TiUIHelper.getRawSizeOrZero(dict, TiC.PROPERTY_BOTTOM);
			}
			btn.setPadding(titlePadding.left, titlePadding.top, titlePadding.right, titlePadding.bottom);
			btn.requestLayout();
		} else if (key.equals(TiC.PROPERTY_SHADOW_COLOR)) {
			shadowColor = TiConvert.toColor((String) newValue);
			btn.setShadowLayer(shadowRadius, shadowDx, shadowDy, shadowColor);
		} else if (key.equals(TiC.PROPERTY_SHADOW_RADIUS)) {
			shadowRadius = TiConvert.toFloat(newValue);
			btn.setShadowLayer(shadowRadius, shadowDx, shadowDy, shadowColor);
		} else if (key.equals(TiC.PROPERTY_SHADOW_OFFSET)) {
			KrollDict dict = (KrollDict) newValue;
			shadowDx = TiConvert.toInt(dict.get(TiC.PROPERTY_X));
			shadowDy = TiConvert.toInt(dict.get(TiC.PROPERTY_Y));
			btn.setShadowLayer(shadowRadius, shadowDx, shadowDy, shadowColor);
			btn.requestLayout();
		} else if (key.equals(TiC.PROPERTY_WORD_WRAP)) {
			btn.setSingleLine(!TiConvert.toBoolean(newValue));
			btn.requestLayout();
		} else if (key.equals(TiC.PROPERTY_SELECTED)) {
			btn.setPressed(TiConvert.toBoolean(newValue));
		} else if (key.equals(TiC.PROPERTY_IMAGE)) {
			TiDrawableReference drawableRef = TiDrawableReference.fromObject(proxy.getActivity(), newValue);

			if (drawableRef != null) {
				imageDrawable = drawableRef.getDrawable();
			}
			else {
				imageDrawable = null;
			}
			updateImage();
		} else if (key.equals(TiC.PROPERTY_IMAGE_ANCHOR)) {
			imageGravity = TiUIHelper.getGravity(TiConvert.toString(newValue), false);
			updateImage();
		} else {
			super.propertyChanged(key, oldValue, newValue, proxy);
		}
	}

	public void setOpacityForButton(float opacity)
	{
		if (opacity < 0 || opacity > 1) {
			Log.w(TAG, "Ignoring invalid value for opacity: " + opacity);
			return;
		}
		View view = getNativeView();
		if (view != null) {
			TiUIHelper.setPaintOpacity(((Button) view).getPaint(), opacity);
			Drawable[] drawables = ((Button) view).getCompoundDrawables();
			if (drawables != null) {
				for (int i = 0; i < drawables.length; i++) {
					TiUIHelper.setDrawableOpacity(drawables[i], opacity);
				}
			}
		}
	}

	public void clearOpacityForButton()
	{
		View view = getNativeView();
		if (view != null) {
			((Button) view).getPaint().setColorFilter(null);
			Drawable[] drawables = ((Button) view).getCompoundDrawables();
			if (drawables != null) {
				for (int i = 0; i < drawables.length; i++) {
					Drawable d = drawables[i];
					if (d != null) {
						d.clearColorFilter();
					}
				}
			}
		}
	}

	@Override
	protected void setOpacity(View view, float opacity)
	{
		setOpacityForButton(opacity);
		super.setOpacity(view, opacity);
	}

	@Override
	public void clearOpacity(View view)
	{
		clearOpacityForButton();
		super.clearOpacity(view);
	}
}
