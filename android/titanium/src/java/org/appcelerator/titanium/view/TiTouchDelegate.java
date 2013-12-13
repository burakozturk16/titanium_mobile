package org.appcelerator.titanium.view;

import android.view.MotionEvent;

public interface TiTouchDelegate {
	public void onTouchEvent(MotionEvent event, TiUIView fromView);
}
