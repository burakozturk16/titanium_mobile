package org.appcelerator.titanium.util;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiC;

import android.view.MotionEvent;
import android.view.View;

import com.nineoldandroids.view.ViewHelper;

public class TiViewHelper {

    private static int getWidthForView(View view) {
        int width = view.getWidth();
        if (width == 0) { // a cheat for NavigationWindowProxy where animation
                          // will start before layout
            View parent = (View) view.getParent();
            if (parent != null) {
                width = parent.getWidth();
            }
        }
        return width;
    }

    private static int getHeightForView(View view) {
        int height = view.getHeight();
        if (height == 0) { // a cheat for NavigationWindowProxy where animation
                           // will start before layout
            View parent = (View) view.getParent();
            if (parent != null) {
                height = parent.getHeight();
            }
        }
        return height;
    }

    public static void setTranslationRelativeX(View view, float val) {
        ViewHelper.setTranslationX(view, getWidthForView(view) * val);

    }

    public static float getTranslationRelativeX(View view) {
        return (ViewHelper.getTranslationX(view) / getWidthForView(view));
    }

    public static void setTranslationRelativeY(View view, float val) {
        ViewHelper.setTranslationY(view, getHeightForView(view) * val);
    }

    public static float getTranslationRelativeY(View view) {
        return (ViewHelper.getTranslationY(view) / getHeightForView(view));
    }

    public static void setPivotFloatX(View view, float val) {
        ViewHelper.setPivotX(view, getWidthForView(view) * val);
    }

    public static void setPivotFloat(View view, float valx, float valy) {
        ViewHelper.setPivotX(view, getWidthForView(view) * valx);
        ViewHelper.setPivotY(view, getHeightForView(view) * valy);
    }

    public static float getPivotFloatX(View view) {
        return (ViewHelper.getPivotX(view) / getWidthForView(view));
    }

    public static void setPivotFloatY(View view, float val) {
        ViewHelper.setPivotX(view, getHeightForView(view) * val);
    }

    public static float getPivotFloatY(View view) {
        return (ViewHelper.getPivotY(view) / getHeightForView(view));
    }

    public static void setScale(View view, float valx, float valy) {
        ViewHelper.setScaleX(view, valx);
        ViewHelper.setScaleY(view, valy);
    }

    public static void setScale(View view, float val) {
        ViewHelper.setScaleX(view, val);
        ViewHelper.setScaleY(view, val);
    }

    public static void resetValues(View view) {

        setPivotFloat(view, 0.5f, 0.5f);
        ViewHelper.setTranslationX(view, 0.0f);
        ViewHelper.setTranslationY(view, 0.0f);
        ViewHelper.setScaleX(view, 1.0f);
        ViewHelper.setScaleY(view, 1.0f);
        ViewHelper.setRotation(view, 0.0f);
        ViewHelper.setRotationX(view, 0.0f);
        ViewHelper.setRotationY(view, 0.0f);
        ViewHelper.setAlpha(view, 1.0f);
    }

    public static KrollDict dictFromMotionEvent(final View view, final MotionEvent e) {
        KrollDict data = new KrollDict();
        if (e != null) {
            double density = TiApplication.getAppDensity();
            int[] coords = new int[2];
            view.getLocationInWindow(coords);

            final double rawx = e.getRawX() / density;
            final double rawy = e.getRawY() / density;
            final double x = (double) rawx - coords[0] / density;
            final double y = (double) rawy - coords[1] / density;
            data.put(TiC.EVENT_PROPERTY_X, x);
            data.put(TiC.EVENT_PROPERTY_Y, y);
            KrollDict globalPoint = new KrollDict();
            globalPoint.put(TiC.EVENT_PROPERTY_X, rawx);
            globalPoint.put(TiC.EVENT_PROPERTY_Y, rawy);
            data.put(TiC.EVENT_PROPERTY_GLOBALPOINT, globalPoint);
        }
//        data.put(TiC.EVENT_PROPERTY_SOURCE, proxy);
        return data;
    }
    
    public static KrollDict dictFromMotionEventCoords(final View view, final float focusX, final float focusY) {
        KrollDict data = new KrollDict();
//        if (e != null) {
            double density = TiApplication.getAppDensity();
            int[] coords = new int[2];
            view.getLocationInWindow(coords);

            
            final double x = (double) focusX / density;
            final double y = (double) focusY / density;
            final double rawx = x  + coords[0] / density;
            final double rawy = y  + coords[1] / density;
            data.put(TiC.EVENT_PROPERTY_X, x);
            data.put(TiC.EVENT_PROPERTY_Y, y);
            KrollDict globalPoint = new KrollDict();
            globalPoint.put(TiC.EVENT_PROPERTY_X, rawx);
            globalPoint.put(TiC.EVENT_PROPERTY_Y, rawy);
            data.put(TiC.EVENT_PROPERTY_GLOBALPOINT, globalPoint);
//        }
//        data.put(TiC.EVENT_PROPERTY_SOURCE, proxy);
        return data;
    }
}
