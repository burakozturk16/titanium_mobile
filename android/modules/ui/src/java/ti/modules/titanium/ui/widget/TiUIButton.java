/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.ui.widget;

import java.io.FileNotFoundException;
import java.util.HashMap;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiUIHelper;
import org.appcelerator.titanium.view.TiDrawableReference;
import org.appcelerator.titanium.view.TiUINonViewGroupView;

import android.graphics.Rect;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.content.res.ColorStateList;

public class TiUIButton extends TiUINonViewGroupView
{
	private static final String TAG = "TiUIButton";
	private static final float DEFAULT_SHADOW_RADIUS = 0.5f;
	
	private int defaultColor, selectedColor, color, disabledColor;
	private float shadowRadius = DEFAULT_SHADOW_RADIUS;
	private float shadowX = 0f;
	private float shadowY = 0f;
	private int shadowColor = Color.TRANSPARENT;

	private Rect titlePadding;
	private Drawable imageDrawable;
	private int imageGravity;

	public TiUIButton(final TiViewProxy proxy)
	{
		super(proxy);
		imageGravity = Gravity.LEFT;
		titlePadding = new Rect();
		titlePadding.left = 8;
		titlePadding.right = 8;
		Log.d(TAG, "Creating a button", Log.DEBUG_MODE);
		Button btn = new Button(proxy.getActivity())
		{
			@Override
			protected void onLayout(boolean changed, int left, int top, int right, int bottom)
			{
				super.onLayout(changed, left, top, right, bottom);
				TiUIHelper.firePostLayoutEvent(TiUIButton.this);
			}

			@Override
			public void setPressed(boolean pressed) {
				super.setPressed(pressed);
				if (dispatchPressed == true && childrenHolder != null) {
					int count = childrenHolder.getChildCount();
					for (int i = 0; i < count; i++) {
			            final View child = childrenHolder.getChildAt(i);
			            child.setPressed(pressed);
			        }
				}
			}

			@Override
			public boolean dispatchTouchEvent(MotionEvent event) {
				if (touchPassThrough == true)
					return false;
				return super.dispatchTouchEvent(event);
			}

		};
		btn.setPadding(titlePadding.left, titlePadding.top, titlePadding.right, titlePadding.bottom);
		btn.setGravity(Gravity.CENTER);
		color = disabledColor = selectedColor = defaultColor = btn.getCurrentTextColor();
		setNativeView(btn);
	}
	
	private void updateTextColors() {
		int[][] states = new int[][] {
			TiUIHelper.BACKGROUND_DISABLED_STATE, // disabled
			TiUIHelper.BACKGROUND_SELECTED_STATE, // pressed
			TiUIHelper.BACKGROUND_FOCUSED_STATE,  // pressed
			TiUIHelper.BACKGROUND_CHECKED_STATE,  // pressed
			new int [] {android.R.attr.state_pressed},  // pressed
			new int [] {android.R.attr.state_focused},  // pressed
			new int [] {}
		};

		ColorStateList colorStateList = new ColorStateList(
			states,
			new int[] {disabledColor, selectedColor, selectedColor, selectedColor, selectedColor, selectedColor, color}
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

		boolean needShadow = false;

		Button btn = (Button) getNativeView();
		if (d.containsKey(TiC.PROPERTY_IMAGE)) {
			Object value = d.get(TiC.PROPERTY_IMAGE);
			TiDrawableReference drawableRef = TiDrawableReference.fromObject(proxy.getActivity(), value);

			if (drawableRef != null) {
				try {
					imageDrawable = drawableRef.getDrawable();
				} catch (FileNotFoundException e) {
					imageDrawable = null;
				}
			}
			else {
				imageDrawable = null;
			}
		}
		if (d.containsKey(TiC.PROPERTY_TITLE)) {
			btn.setText(d.getString(TiC.PROPERTY_TITLE));
		}
		
		boolean needsColors = false;
		if(d.containsKey(TiC.PROPERTY_COLOR)) {
			needsColors = true;
			color = d.optColor(TiC.PROPERTY_COLOR, this.color);
		}
		if(d.containsKey(TiC.PROPERTY_SELECTED_COLOR)) {
			needsColors = true;
			selectedColor = d.optColor(TiC.PROPERTY_SELECTED_COLOR, this.selectedColor);
		}
		if(d.containsKey(TiC.PROPERTY_DISABLED_COLOR)) {
			needsColors = true;
			disabledColor = d.optColor(TiC.PROPERTY_COLOR, this.disabledColor);
		}
		if (needsColors) {
			updateTextColors();
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
			titlePadding = TiConvert.toPaddingRect(d, TiC.PROPERTY_TITLE_PADDING);
			btn.setPadding(titlePadding.left, titlePadding.top, titlePadding.right, titlePadding.bottom);
		}
		if (d.containsKey(TiC.PROPERTY_IMAGE_ANCHOR)) {
			imageGravity = TiUIHelper.getGravity(d.getString(TiC.PROPERTY_IMAGE_ANCHOR), false);
		}
		if (d.containsKey(TiC.PROPERTY_WORD_WRAP)) {
			btn.setSingleLine(!TiConvert.toBoolean(d, TiC.PROPERTY_WORD_WRAP));
		}
		if (d.containsKey(TiC.PROPERTY_SELECTED)) {
			btn.setPressed(TiConvert.toBoolean(d, TiC.PROPERTY_SELECTED));
		}
		updateImage();
		if (d.containsKey(TiC.PROPERTY_SHADOW_OFFSET)) {
			Object value = d.get(TiC.PROPERTY_SHADOW_OFFSET);
			if (value instanceof HashMap) {
				needShadow = true;
				HashMap dict = (HashMap) value;
				shadowX = TiConvert.toFloat(dict.get(TiC.PROPERTY_X), 0);
				shadowY = TiConvert.toFloat(dict.get(TiC.PROPERTY_Y), 0);
			}
		}
		if (d.containsKey(TiC.PROPERTY_SHADOW_RADIUS)) {
			needShadow = true;
			shadowRadius = TiConvert.toFloat(d.get(TiC.PROPERTY_SHADOW_RADIUS), DEFAULT_SHADOW_RADIUS);
		}
		if (d.containsKey(TiC.PROPERTY_SHADOW_COLOR)) {
			needShadow = true;
			shadowColor = TiConvert.toColor(d, TiC.PROPERTY_SHADOW_COLOR);
		}
		if (needShadow) {
			btn.setShadowLayer(shadowRadius, shadowX, shadowY, shadowColor);
		}
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
			this.color = TiConvert.toColor(newValue);
			updateTextColors();
		} else if (key.equals(TiC.PROPERTY_SELECTED_COLOR)) {
			this.selectedColor = TiConvert.toColor(newValue);
			updateTextColors();
		} else if (key.equals(TiC.PROPERTY_DISABLED_COLOR)) {
			this.disabledColor = TiConvert.toColor(newValue);
			updateTextColors();
		} else if (key.equals(TiC.PROPERTY_FONT)) {
			TiUIHelper.styleText(btn, (HashMap) newValue);
		} else if (key.equals(TiC.PROPERTY_TEXT_ALIGN)) {
			TiUIHelper.setAlignment(btn, TiConvert.toString(newValue), null);
			btn.requestLayout();
		} else if (key.equals(TiC.PROPERTY_VERTICAL_ALIGN)) {
			TiUIHelper.setAlignment(btn, null, TiConvert.toString(newValue));
			btn.requestLayout();
		} else if (key.equals(TiC.PROPERTY_TITLE_PADDING)) {
			titlePadding = TiConvert.toPaddingRect(newValue);
			btn.setPadding(titlePadding.left, titlePadding.top, titlePadding.right, titlePadding.bottom);
			btn.requestLayout();
		} else if (key.equals(TiC.PROPERTY_WORD_WRAP)) {
			btn.setSingleLine(!TiConvert.toBoolean(newValue));
			btn.requestLayout();
		} else if (key.equals(TiC.PROPERTY_SELECTED)) {
			btn.setPressed(TiConvert.toBoolean(newValue));
		} else if (key.equals(TiC.PROPERTY_IMAGE)) {
			TiDrawableReference drawableRef = TiDrawableReference.fromObject(proxy.getActivity(), newValue);

			if (drawableRef != null) {
				try {
					imageDrawable = drawableRef.getDrawable();
				} catch (FileNotFoundException e) {
					imageDrawable = null;
				}
			}
			else {
				imageDrawable = null;
			}
			updateImage();
		} else if (key.equals(TiC.PROPERTY_IMAGE_ANCHOR)) {
			imageGravity = TiUIHelper.getGravity(TiConvert.toString(newValue), false);
			updateImage();
		} else if (key.equals(TiC.PROPERTY_SHADOW_OFFSET)) {
			if (newValue instanceof HashMap) {
				HashMap dict = (HashMap) newValue;
				shadowX = TiConvert.toFloat(dict.get(TiC.PROPERTY_X), 0);
				shadowY = TiConvert.toFloat(dict.get(TiC.PROPERTY_Y), 0);
				btn.setShadowLayer(shadowRadius, shadowX, shadowY, shadowColor);
			}
		} else if (key.equals(TiC.PROPERTY_SHADOW_RADIUS)) {
			shadowRadius = TiConvert.toFloat(newValue, DEFAULT_SHADOW_RADIUS);
			btn.setShadowLayer(shadowRadius, shadowX, shadowY, shadowColor);
		} else if (key.equals(TiC.PROPERTY_SHADOW_COLOR)) {
			shadowColor = TiConvert.toColor(TiConvert.toString(newValue));
			btn.setShadowLayer(shadowRadius, shadowX, shadowY, shadowColor);
		} else {
			super.propertyChanged(key, oldValue, newValue, proxy);
		}
	}
}
