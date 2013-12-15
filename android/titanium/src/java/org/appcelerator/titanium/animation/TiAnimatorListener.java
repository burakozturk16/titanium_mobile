package org.appcelerator.titanium.animation;

import java.util.HashMap;
import java.util.List;

import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.TiViewProxy;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.Animator.AnimatorListener;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ValueAnimator;

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
	
	private void restartAnimatorSet() {
		//start repeat without reverse
		if (this.animationProxy != null) {
			this.animationProxy.fireEvent(TiC.EVENT_REPEAT, null);
		}
		tiSet.currentRepeatCount = tiSet.currentRepeatCount - 1;
		tiSet.resetReverseSet();
		tiSet.set().start();
	}
	
	public void onAnimationEnd(Animator animation) {
		Log.d(TAG, "onAnimationEnd", Log.DEBUG_MODE);
		if (tiSet == null || tiSet.getAnimating() == false) return;//prevent double onEnd!
		AnimatorSet set = tiSet.set();
		AnimatorSet reverseSet = tiSet.reverseSet();
		int repeatCount = tiSet.repeatCount;
		int currentRepeatCount = tiSet.currentRepeatCount;
		if (reverseSet != null) {
			//the case where we have a reverse
			if (animation == set) {
//				set.setupEndValues();
				tiSet.resetSet();
				reverseSet.start();
				return;
			}
			else if (animation == reverseSet) {
				if (repeatCount < 0 || currentRepeatCount > 0) {
					//start repeat without reverse
					if (this.animationProxy != null) {
						this.animationProxy.fireEvent(TiC.EVENT_REPEAT, null);
					}
					if (currentRepeatCount > 0) tiSet.currentRepeatCount = tiSet.currentRepeatCount - 1;
//					reverseSet.setupEndValues();
					tiSet.resetReverseSet();
					set.start();
					return;
				}
			}
		}
		else if (repeatCount < 0 || currentRepeatCount > 0) {
			//start repeat without reverse
			if (this.animationProxy != null) {
				this.animationProxy.fireEvent(TiC.EVENT_REPEAT, null);
			}
			if (currentRepeatCount > 0) tiSet.currentRepeatCount = tiSet.currentRepeatCount - 1;
			tiSet.resetSet();
			tiSet.restartFromBeginning();
			tiSet.set().start();
			return;
		}
		tiSet.setAnimating(false);
		tiSet.handleFinish(); //will fire the EVENT_COMPLETE
	}

	public void onAnimationStart(Animator animation) {
		if (tiSet != null && ( animation == tiSet.reverseSet() || tiSet.getAnimating() == true)) return;
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
		if (animation == tiSet.reverseSet()) return;
		Log.d(TAG, "onAnimationRepeat", Log.DEBUG_MODE);
		if (this.animationProxy != null) {
			this.animationProxy.fireEvent(TiC.EVENT_REPEAT, null);
		}
	}

}