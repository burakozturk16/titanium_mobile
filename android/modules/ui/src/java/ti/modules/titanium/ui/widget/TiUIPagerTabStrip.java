/**
 * Akylas
 * Copyright (c) 2014-2015 by Akylas. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.ui.widget;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiDimension;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiHtml;
import org.appcelerator.titanium.util.TiUIHelper;
import org.appcelerator.titanium.view.TiUIView;
import android.graphics.Color;
import android.graphics.RectF;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class TiUIPagerTabStrip extends TiUIView {
    private ArrayList<CharSequence> mStripTitles = new ArrayList<CharSequence>();
    private static final float DEFAULT_SHADOW_RADIUS = 0.5f;
    private float shadowRadius = DEFAULT_SHADOW_RADIUS;
    private float shadowX = 0f;
    private float shadowY = -1f; // to have the same value as ios
    private int shadowColor = Color.TRANSPARENT;
    private static final String TAG = "TiUIPagerTabStrip";
    private PagerTabStrip strip = null;
    private RectF textPadding = null;
    
    public TiUIPagerTabStrip(final TiViewProxy proxy)
    {
        super(proxy);
        useCustomLayoutParams = true;
//        this.parentProxy = parentProxy;
        strip = new PagerTabStrip(proxy.getActivity()) {
            @Override
            public void setLayoutParams(ViewGroup.LayoutParams params) {
                super.setLayoutParams(params);
            }
        };
        ViewPager.LayoutParams layoutParams = new ViewPager.LayoutParams();
        layoutParams.height = ViewPager.LayoutParams.WRAP_CONTENT;
        layoutParams.width = ViewPager.LayoutParams.MATCH_PARENT;
        layoutParams.gravity = Gravity.TOP;
        strip.setLayoutParams(layoutParams);
        setNativeView(strip);
    }
    
    public CharSequence getPageTitle(int position) {
        if (position < mStripTitles.size()) {
            return mStripTitles.get(position);
        }
        return null;
    }
    
    protected int fillLayout(String key, Object value, boolean withMatrix) {
        ViewPager.LayoutParams layoutParams = (ViewPager.LayoutParams) strip.getLayoutParams();
        switch (key) {
        case TiC.PROPERTY_WIDTH:
            if (value == null || value.equals(TiC.LAYOUT_FILL)) {
                layoutParams.width = ViewPager.LayoutParams.MATCH_PARENT;
            } else if (value.equals(TiC.SIZE_AUTO) || value.equals(TiC.LAYOUT_SIZE)) {
                layoutParams.width = ViewPager.LayoutParams.WRAP_CONTENT;
            } else  {
                layoutParams.width = TiConvert.toTiDimension(value, TiDimension.TYPE_WIDTH).getAsPixels((View) strip.getParent());
            }
            return TiUIView.TIFLAG_NEEDS_LAYOUT;
        case TiC.PROPERTY_HEIGHT:
            if (value == null || value.equals(TiC.LAYOUT_FILL)) {
                layoutParams.height = ViewPager.LayoutParams.MATCH_PARENT;
            } else if (value.equals(TiC.SIZE_AUTO) || value.equals(TiC.LAYOUT_SIZE)) {
                layoutParams.height = ViewPager.LayoutParams.WRAP_CONTENT;
            } else {
                layoutParams.height = TiConvert.toTiDimension(value, TiDimension.TYPE_HEIGHT).getAsPixels((View) strip.getParent());
            }
            return TiUIView.TIFLAG_NEEDS_LAYOUT;

        default:
            break;
        }
        return 0;
    }

    @Override
    protected int fillLayout(KrollDict d)
    {
        int updateFlags = 0;
        Iterator it = d.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            int result = fillLayout((String) entry.getKey(), entry.getValue(), true);
            if (result != 0) {
                updateFlags |= result;
                it.remove();
            }
        }
        if (updateFlags != 0) {
            strip.setLayoutParams(strip.getLayoutParams());
        }
        return updateFlags;
    }
    
    @Override
    public void propertySet(String key, Object newValue, Object oldValue,
            boolean changedProperty) {

        switch (key) {
        case TiC.PROPERTY_VERTICAL_ALIGN:
            layoutParams.gravity = TiUIHelper.getGravity(
                    TiConvert.toString(newValue), true);
            setLayoutParams(layoutParams);
            break;
        case TiC.PROPERTY_TITLES:
            Object[] values = (Object[]) newValue;
            mStripTitles.clear();
            for (int i = 0; i < values.length; i++) {
                mStripTitles.add(TiHtml.fromHtml(TiConvert.toString(values[i]),
                        false));
            }
            break;
        case TiC.PROPERTY_COLOR:
            strip.setTabIndicatorColor(TiConvert.toColor(newValue));
            break;
        case TiC.PROPERTY_INDICATOR_COLOR:
            strip.setTextColor(TiConvert.toColor(newValue));
            break;
        case TiC.PROPERTY_TEXT_ALIGN:
            for (int counter = 0; counter < strip.getChildCount(); counter++) {
                if (strip.getChildAt(counter) instanceof TextView) {
                    TiUIHelper.setAlignment(
                            (TextView) strip.getChildAt(counter),
                            TiConvert.toString(newValue), null);
                }
            }
            break;
        case TiC.PROPERTY_INCLUDE_FONT_PADDING:
            for (int counter = 0; counter < strip.getChildCount(); counter++) {
                if (strip.getChildAt(counter) instanceof TextView) {
                    ((TextView) strip.getChildAt(counter))
                            .setIncludeFontPadding(TiConvert.toBoolean(
                                    newValue, true));
                }
            }
            break;
        case TiC.PROPERTY_FONT:
            for (int counter = 0; counter < strip.getChildCount(); counter++) {
                if (strip.getChildAt(counter) instanceof TextView) {
                    TiUIHelper.styleText((TextView) strip.getChildAt(counter),
                            (HashMap) newValue);
                }
            }
            break;
        case TiC.PROPERTY_SHADOW_OFFSET:
            if (newValue instanceof HashMap) {
                HashMap dict = (HashMap) newValue;
                shadowX = TiUIHelper.getInPixels(dict, TiC.PROPERTY_X);
                shadowY = TiUIHelper.getInPixels(dict, TiC.PROPERTY_Y);
                for (int counter = 0; counter < strip.getChildCount(); counter++) {
                    if (strip.getChildAt(counter) instanceof TextView) {
                        ((TextView) strip.getChildAt(counter)).setShadowLayer(
                                shadowRadius, shadowX, shadowY, shadowColor);
                    }
                }
            }
            break;
        case TiC.PROPERTY_SHADOW_RADIUS:
            shadowRadius = TiConvert.toFloat(newValue, DEFAULT_SHADOW_RADIUS);
            for (int counter = 0; counter < strip.getChildCount(); counter++) {
                if (strip.getChildAt(counter) instanceof TextView) {
                    ((TextView) strip.getChildAt(counter)).setShadowLayer(
                            shadowRadius, shadowX, shadowY, shadowColor);
                }
            }
            break;
        case TiC.PROPERTY_SHADOW_COLOR:
            shadowColor = TiConvert.toColor(TiConvert.toString(newValue));
            for (int counter = 0; counter < strip.getChildCount(); counter++) {
                if (strip.getChildAt(counter) instanceof TextView) {
                    ((TextView) strip.getChildAt(counter)).setShadowLayer(
                            shadowRadius, shadowX, shadowY, shadowColor);
                }
            }
            break;
        case TiC.PROPERTY_TEXT_SPACING:
            strip.setTextSpacing(TiConvert.toTiDimension(newValue,
                    TiDimension.TYPE_WIDTH).getAsPixels(strip));
            break;
        case TiC.PROPERTY_NON_PRIMARY_ALPHA:
            strip.setNonPrimaryAlpha(TiConvert.toFloat(newValue, 0.5f));
            break;
        case TiC.PROPERTY_DRAW_FULL_UNDERLINE:
            strip.setDrawFullUnderline(TiConvert.toBoolean(newValue, true));
            break;
        case TiC.PROPERTY_PADDING:
            textPadding = TiConvert.toPaddingRect(newValue, textPadding);
            strip.setPadding((int) textPadding.left, (int) textPadding.top,
                    (int) textPadding.right, (int) textPadding.bottom);
        default:
            super.propertySet(key, newValue, oldValue, changedProperty);
            break;
        }
    }
}
