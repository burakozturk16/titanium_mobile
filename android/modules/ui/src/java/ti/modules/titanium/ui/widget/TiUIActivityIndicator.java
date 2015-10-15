/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.ui.widget;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiUIHelper;
import org.appcelerator.titanium.view.TiUIView;

import android.app.Activity;
import android.support.v7.widget.AppCompatTextView;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

public class TiUIActivityIndicator extends TiUIView
{
	private static final String TAG = "TiUIActivityIndicator";

	protected int currentStyle;
	private AppCompatTextView label;
	private ProgressBar progress;
	private LinearLayout view;

	public static final int PLAIN = android.R.attr.progressBarStyleSmall;
	public static final int BIG = android.R.attr.progressBarStyleLarge;
	public static final int DARK = android.R.attr.progressBarStyleSmallInverse;
	public static final int BIG_DARK = android.R.attr.progressBarStyleLargeInverse;

	public TiUIActivityIndicator(TiViewProxy proxy)
	{
		super(proxy);
		Log.d(TAG, "Creating an activity indicator", Log.DEBUG_MODE);

		/*
		 * use getAppCurrentActivity over getActivity since technically the activity indicator
		 * should show up on top of the current activity when called - not just the
		 * activity it was created in
		 */
		final Activity activity = proxy.getActivity();

		if (activity == null) {
			Log.w(TAG, "Unable to create an activity indicator. Activity is null");
			return;
		}

		view = new LinearLayout(activity) {
			@Override
			public boolean dispatchTouchEvent(MotionEvent event) {
				if (touchPassThrough == true)
					return false;
				return super.dispatchTouchEvent(event);
			}
		};
		view.setOrientation(LinearLayout.HORIZONTAL);
		view.setGravity(Gravity.CENTER);

		label = new AppCompatTextView(activity);
		label.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
		label.setPadding(0, 0, 0, 0);
		label.setSingleLine(false);

		currentStyle = getStyle();
		progress = new ProgressBar(activity, null, currentStyle);

		view.addView(progress);
		view.addView(label);

		setNativeView(view);
	}
	
    @Override
	public void propertySet(String key, Object newValue, Object oldValue,
            boolean changedProperty) {
        switch (key) {
        case TiC.PROPERTY_STYLE:
            setStyle(TiConvert.toInt(newValue));
            break;
        case TiC.PROPERTY_FONT:
            TiUIHelper.styleText(label, TiConvert.toKrollDict(newValue));
            if (changedProperty) {
                label.requestLayout();
            }
            break;
        case TiC.PROPERTY_MESSAGE:
            label.setText(TiConvert.toString(newValue));
            if (changedProperty) {
                label.requestLayout();
            }
            break;
        case TiC.PROPERTY_COLOR:
            label.setTextColor(TiConvert.toColor(newValue));
            break;

        default:
            super.propertySet(key, newValue, oldValue, changedProperty);
            break;
        }
    }

	@Override
	public void processProperties(KrollDict d)
	{
		super.processProperties(d);
        if (view != null) {
            view.invalidate();
        }
	}

	protected int getStyle()
	{
		if (proxy.hasProperty(TiC.PROPERTY_STYLE)) {
			int style = TiConvert.toInt(proxy.getProperty(TiC.PROPERTY_STYLE));
			if (style != PLAIN && style != BIG && style != DARK && style != BIG_DARK) {
				Log.w(TAG, "Invalid value \"" + style + "\" for style.");
				return PLAIN;
			}
			return style;
		}
		return PLAIN;
	}

	protected void setStyle(int style)
	{
		if (style == currentStyle || view == null) {
			return;
		}
		if (style != PLAIN && style != BIG && style != DARK && style != BIG_DARK) {
			Log.w(TAG, "Invalid value \"" + style + "\" for style.");
			return;
		}

		view.removeAllViews();
		progress = new ProgressBar(proxy.getActivity(), null, style);
		currentStyle = style;
		view.addView(progress);
		view.addView(label);
		view.requestLayout();
	}
}
