/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.ui.widget;

import java.util.HashMap;

import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiHtml;
import org.appcelerator.titanium.util.TiUIHelper;
import org.appcelerator.titanium.view.TiDrawableReference;
import org.appcelerator.titanium.view.TiUINonViewGroupView;

import android.graphics.Color;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.AppCompatButton;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.Button;
import android.content.res.ColorStateList;

public class TiUIButton extends TiUINonViewGroupView
{
	private static final String TAG = "TiUIButton";
	private static final float DEFAULT_SHADOW_RADIUS = 0.5f;
	
	
    protected static final int TIFLAG_NEEDS_COLORS               = 0x00000001;
    protected static final int TIFLAG_NEEDS_TEXT                 = 0x00000002;
    protected static final int TIFLAG_NEEDS_TEXT_HTML            = 0x00000004;
    protected static final int TIFLAG_NEEDS_SHADOW               = 0x00000008;
    protected static final int TIFLAG_NEEDS_IMAGE                = 0x00000010;

	private int selectedColor, color, disabledColor;
	private float shadowRadius = DEFAULT_SHADOW_RADIUS;
	private float shadowX = 0f;
	private float shadowY = 0f;
	private int shadowColor = Color.TRANSPARENT;
    private String text = null;

	private RectF titlePadding;
	private Drawable imageDrawable;
	private int imageGravity;
    protected RectF padding = null;

	public TiUIButton(final TiViewProxy proxy)
	{
		super(proxy);
		imageGravity = Gravity.LEFT;
		titlePadding = new RectF();
		titlePadding.left = 8;
		titlePadding.right = 8;
		Log.d(TAG, "Creating a button", Log.DEBUG_MODE);
		AppCompatButton btn = new AppCompatButton(proxy.getActivity())
		{
			@Override
			protected void onLayout(boolean changed, int left, int top, int right, int bottom)
			{
				super.onLayout(changed, left, top, right, bottom);
				TiUIHelper.firePostLayoutEvent(TiUIButton.this);
			}

			@Override
	        public void dispatchSetPressed(boolean pressed) {
	            if (propagateSetPressed(this, pressed)) {
	                super.dispatchSetPressed(pressed);
	            }
	        }

			@Override
			public boolean dispatchTouchEvent(MotionEvent event) {
	            if (touchPassThrough(getParentViewForChild(), event)) return false;
				return super.dispatchTouchEvent(event);
			}

		};
		TiUIHelper.setPadding(btn, titlePadding);
		btn.setGravity(Gravity.CENTER);
		color = disabledColor = selectedColor = btn.getCurrentTextColor();
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
	
	private Button getButton() {
	    return (Button) getNativeView();
	}
	
	@Override
    public void propertySet(String key, Object newValue, Object oldValue,
            boolean changedProperty) {
        switch (key) {
        case TiC.PROPERTY_IMAGE:
            TiDrawableReference drawableRef = TiDrawableReference.fromObject(proxy.getActivity(), newValue);
            if (drawableRef != null) {
                imageDrawable = drawableRef.getDrawable();
            }
            else {
                imageDrawable = null;
            }
            mProcessUpdateFlags |= TIFLAG_NEEDS_COLORS;
            break;
        case TiC.PROPERTY_COLOR:
            color = selectedColor = disabledColor = color = TiConvert.toColor(newValue, this.color);
            mProcessUpdateFlags |= TIFLAG_NEEDS_COLORS;
            break;
        case TiC.PROPERTY_SELECTED_COLOR:
            selectedColor = TiConvert.toColor(newValue, this.selectedColor);
            mProcessUpdateFlags |= TIFLAG_NEEDS_COLORS;
            break;
        case TiC.PROPERTY_DISABLED_COLOR:
            disabledColor = TiConvert.toColor(newValue, this.disabledColor);
            mProcessUpdateFlags |= TIFLAG_NEEDS_COLORS;
            break;
        case TiC.PROPERTY_FONT:
            TiUIHelper.styleText(getButton(), TiConvert.toKrollDict(newValue));
            setNeedsLayout();
            break;
        case TiC.PROPERTY_TEXT_ALIGN:
            TiUIHelper.setAlignment(getButton(), TiConvert.toString(newValue), null);
            setNeedsLayout();
            break;
        case TiC.PROPERTY_VERTICAL_ALIGN:
            TiUIHelper.setAlignment(getButton(), null, TiConvert.toString(newValue));
            setNeedsLayout();
            break;
        case TiC.PROPERTY_TITLE_PADDING:
            padding = TiConvert.toPaddingRect(newValue, padding);
            TiUIHelper.setPadding(getButton(), padding);
            setNeedsLayout();
            break;
        case TiC.PROPERTY_AUTOCAPITALIZATION:
            if (TiC.ICS_OR_GREATER) {
                getButton().setAllCaps(TiConvert.toBoolean(newValue));
            }
            break;
        case TiC.PROPERTY_IMAGE_ANCHOR:
            imageGravity = TiUIHelper.getGravity(TiConvert.toString(newValue), false);
            mProcessUpdateFlags |= TIFLAG_NEEDS_IMAGE;
            break;
        case TiC.PROPERTY_WORD_WRAP:
            getButton().setSingleLine(!TiConvert.toBoolean(newValue));
            setNeedsLayout();
            break;
        case TiC.PROPERTY_SELECTED:
            getButton().setPressed(TiConvert.toBoolean(newValue));
            break;
        case TiC.PROPERTY_SHADOW_OFFSET:
            if (newValue instanceof HashMap) {
                HashMap dict = (HashMap) newValue;
                shadowX = TiUIHelper.getInPixels(dict.get(TiC.PROPERTY_X));
                shadowY = TiUIHelper.getInPixels(dict.get(TiC.PROPERTY_Y));
            }
            else {
                shadowX = 0f;
                shadowY = 0f;
            }
            mProcessUpdateFlags |= TIFLAG_NEEDS_SHADOW;
            break;
        case TiC.PROPERTY_SHADOW_RADIUS:
            shadowRadius = TiConvert.toFloat(newValue, DEFAULT_SHADOW_RADIUS);
            mProcessUpdateFlags |= TIFLAG_NEEDS_SHADOW;
            break;
        case TiC.PROPERTY_SHADOW_COLOR:
            shadowColor = TiConvert.toColor(newValue);
            mProcessUpdateFlags |= TIFLAG_NEEDS_SHADOW;
            break;
        case TiC.PROPERTY_HTML:
            text = TiConvert.toString(newValue);
            mProcessUpdateFlags |= TIFLAG_NEEDS_TEXT | TIFLAG_NEEDS_TEXT_HTML;
            break;
        case TiC.PROPERTY_TEXT:
        case TiC.PROPERTY_TITLE:
            if ((mProcessUpdateFlags & TIFLAG_NEEDS_TEXT) == 0) {
                text = TiConvert.toString(newValue);
                mProcessUpdateFlags |= TIFLAG_NEEDS_TEXT;
            }
            break;
        default:
            super.propertySet(key, newValue, oldValue, changedProperty);
            break;
        }
    }
	
	@Override
    protected void didProcessProperties() {
        if ((mProcessUpdateFlags & TIFLAG_NEEDS_COLORS) != 0) {
            updateTextColors();
            mProcessUpdateFlags &= ~TIFLAG_NEEDS_COLORS;
        }
        if ((mProcessUpdateFlags & TIFLAG_NEEDS_SHADOW) != 0) {
            getButton().setShadowLayer(shadowRadius, shadowX, shadowY, shadowColor);
            mProcessUpdateFlags &= ~TIFLAG_NEEDS_SHADOW;
        }
        if ((mProcessUpdateFlags & TIFLAG_NEEDS_IMAGE) != 0) {
            updateImage();
            mProcessUpdateFlags &= ~TIFLAG_NEEDS_IMAGE;
        }
        if ((mProcessUpdateFlags & TIFLAG_NEEDS_TEXT) != 0) {
            if ((mProcessUpdateFlags & TIFLAG_NEEDS_TEXT_HTML) != 0) {
                getButton().setText(fromHtml(text));
            } else {
                getButton().setText(text);
            }
            mProcessUpdateFlags &= ~TIFLAG_NEEDS_TEXT;
            mProcessUpdateFlags &= ~TIFLAG_NEEDS_TEXT_HTML;
        }
        super.didProcessProperties();
//        getButton().invalidate();
    }


	private Spanned fromHtml(String str)
    {
        SpannableStringBuilder htmlText = new SpannableStringBuilder(TiHtml.fromHtml(str, false));
        return htmlText;
    }


}
