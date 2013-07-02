package org.appcelerator.titanium.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.view.TiAnimation;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.annotation.TargetApi;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.view.View;
import android.view.ViewParent;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
@SuppressWarnings("rawtypes")
public class TiAnimatorListener implements AnimatorListener {
	private static final String TAG = "TiAnimatorListener";
	protected TiAnimatorSet tiSet;
	protected TiViewProxy viewProxy;
	protected TiAnimation animationProxy;
	protected View view;
	protected HashMap options;
	
	public TiAnimatorListener(TiViewProxy proxy,
			TiAnimatorSet tiSet, HashMap options) {
		super();
		this.viewProxy = proxy;
		this.tiSet = tiSet;
		this.animationProxy = tiSet.animationProxy;
		this.options = options;
	}

	public TiAnimatorListener(View view, TiAnimation aproxy,
			HashMap options) {
		super();
		this.view = view;
		this.animationProxy = aproxy;
		this.options = options;
	}

	public TiAnimatorListener(View view) {
		super();
		this.view = view;
	}

	public TiAnimatorListener() {
		super();
	}

	
	public void onAnimationEnd(Animator animation) {
		if (tiSet == null || tiSet.getAnimating() == false) return;//prevent double onEnd!
		tiSet.setAnimating(false);
		tiSet.handleFinish(); //will fire the EVENT_COMPLETE
	}

	public void onAnimationStart(Animator animation) {
		if (tiSet != null) tiSet.setAnimating(true);
		if (this.animationProxy != null) {
			this.animationProxy.fireEvent(TiC.EVENT_START, null);
		}
	}
	
	public void onAnimationCancel(Animator animation) {
		if (tiSet != null) {
			tiSet.setAnimating(false);
			tiSet.resetAnimationProperties();
		}

	}
	
	public void onAnimationRepeat(Animator animation) {
		if (this.animationProxy != null) {
			this.animationProxy.fireEvent(TiC.EVENT_REPEAT, null);
		}
	}

}