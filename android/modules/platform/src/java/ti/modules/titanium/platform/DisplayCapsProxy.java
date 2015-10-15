/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.platform;

import java.lang.ref.SoftReference;
import java.lang.reflect.Method;

import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiContext;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

@Kroll.proxy(parentModule=PlatformModule.class)
public class DisplayCapsProxy extends KrollProxy
{
	private DisplayMetrics dm = null;
	private boolean sizeSet = false;
	private SoftReference<Display> softDisplay;
	int realWidth;
    int realHeight;

	public DisplayCapsProxy()
	{
		super();
	}

	public DisplayCapsProxy(TiContext tiContext)
	{
		this();
	}
	
	public static Point getRealSize(Display display) {
        Point outPoint = new Point();
        Method mGetRawH;
        try {
            mGetRawH = Display.class.getMethod("getRawHeight");
            Method mGetRawW = Display.class.getMethod("getRawWidth");
            outPoint.x = (Integer) mGetRawW.invoke(display);
            outPoint.y = (Integer) mGetRawH.invoke(display);
            return outPoint;
        } catch (Throwable e) {
            return null;
        }
    }

    public static Point getSize(Display display) {
        if (Build.VERSION.SDK_INT >= 17) {
            Point outPoint = new Point();
            DisplayMetrics metrics = new DisplayMetrics();
            display.getRealMetrics(metrics);
            outPoint.x = metrics.widthPixels;
            outPoint.y = metrics.heightPixels;
            return outPoint;
        }
        if (Build.VERSION.SDK_INT >= 14) {
            Point outPoint = getRealSize(display);
            if (outPoint != null)
                return outPoint;
        }
        Point outPoint = new Point();
        if (Build.VERSION.SDK_INT >= 13) {
            display.getSize(outPoint);
        } else {
            outPoint.x = display.getWidth();
            outPoint.y = display.getHeight();
        }
        return outPoint;
    }
	
	@SuppressWarnings("deprecation")
    private void querySize() {
	    if (sizeSet == false) {
            DisplayMetrics displaymetrics = new DisplayMetrics();
            getDisplay().getMetrics(displaymetrics);
            int orientation = TiApplication.getAppRootOrCurrentActivity().getResources().getConfiguration().orientation;
            Point size = getSize(getDisplay());
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                realWidth = size.x;
                realHeight = size.y;
            } else {
                realWidth = size.y;
                realHeight = size.x;
            }
	        sizeSet = true;
	    }
	}

	private Display getDisplay() {
		if (softDisplay == null || softDisplay.get() == null) {
		    Activity activity = TiApplication.getAppRootOrCurrentActivity();
		    if (activity != null) {
		     // we only need the window manager so it doesn't matter if the root or current activity is used
	            // for accessing it
	            softDisplay = new SoftReference<Display>(activity.getWindowManager().getDefaultDisplay());
		    } else {
		        
	            softDisplay = new SoftReference<Display>(((WindowManager)TiApplication.getAppSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay());
		    }
			
		}
		return softDisplay.get();
	}
	
	private DisplayMetrics getDisplayMetrics() {
	    if (dm == null) {
	        dm = new DisplayMetrics();
	        getDisplay().getMetrics(dm);
	    }
	    return dm;
	}

	@Kroll.getProperty @Kroll.method
	public int getPlatformWidth() {
	    querySize();
	    return realWidth;
	}

	@Kroll.getProperty @Kroll.method
	public int getPlatformHeight() {
	    querySize();
        return realHeight;
	}

	@Kroll.getProperty @Kroll.method
	public String getDensity() {
		return TiApplication.getAppDensityString();
	}

	@Kroll.getProperty @Kroll.method
	public float getDpi() {
		return getDisplayMetrics().densityDpi;
	}

	@Kroll.getProperty @Kroll.method
	public float getXdpi() {
		return getDisplayMetrics().xdpi;
	}

	@Kroll.getProperty @Kroll.method
	public float getYdpi() {
		return getDisplayMetrics().ydpi;
	}

	@Kroll.getProperty @Kroll.method
	public float getLogicalDensityFactor() {
        return TiApplication.getAppDensity();
	}

	@Override
	public String getApiName()
	{
		return "Ti.Platform.DisplayCaps";
	}
}
