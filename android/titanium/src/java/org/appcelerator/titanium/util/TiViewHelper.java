package org.appcelerator.titanium.util;

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

	public static void setTranslationFloatX(View view, float val) {
		ViewHelper.setTranslationX(view, getWidthForView(view) * val);

	}

	public static float getTranslationFloatX(View view) {
	return (ViewHelper.getTranslationX(view)/getWidthForView(view));
}

	public static void setTranslationFloatY(View view, float val) {
		ViewHelper.setTranslationY(view, getHeightForView(view) * val);
	}

	public static float getTranslationFloatY(View view) {
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
	}
}
