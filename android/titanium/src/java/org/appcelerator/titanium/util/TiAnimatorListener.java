package org.appcelerator.titanium.util;

import java.util.HashMap;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.view.TiAnimation;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.Animator.AnimatorListener;

import android.view.View;

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
	
	public TiAnimatorListener(TiAnimatorSet tiSet, HashMap options) {
		super();
		this.tiSet = tiSet;
		this.animationProxy = tiSet.animationProxy;
		this.options = options;
	}

	public TiAnimatorListener(View view, TiAnimation aproxy,
			HashMap options) {
		super();
		this.tiSet = tiSet;
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
		Log.d(TAG, "onAnimationEnd", Log.DEBUG_MODE);
		if (tiSet == null || tiSet.getAnimating() == false) return;//prevent double onEnd!
		tiSet.setAnimating(false);
		tiSet.handleFinish(); //will fire the EVENT_COMPLETE
	}

	public void onAnimationStart(Animator animation) {
		if (tiSet != null) tiSet.setAnimating(true);
		Log.d(TAG, "onAnimationStart", Log.DEBUG_MODE);
		if (this.animationProxy != null) {
			this.animationProxy.fireEvent(TiC.EVENT_START, null);
		}
	}
	
	public void onAnimationCancel(Animator animation) {
		Log.d(TAG, "onAnimationCancel", Log.DEBUG_MODE);
		if (tiSet == null || tiSet.getAnimating() == false) return;//prevent double onEnd!
		tiSet.setAnimating(false);
		tiSet.handleCancel(); //will fire the EVENT_COMPLETE

	}
	
	public void onAnimationRepeat(Animator animation) {
		Log.d(TAG, "onAnimationRepeat", Log.DEBUG_MODE);
		if (this.animationProxy != null) {
			this.animationProxy.fireEvent(TiC.EVENT_REPEAT, null);
		}
	}

}