package org.appcelerator.titanium.proxy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.appcelerator.kroll.KrollFunction;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.util.TiAnimator;
import org.appcelerator.titanium.util.TiAnimatorListener;
import org.appcelerator.titanium.util.TiAnimatorSet;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.view.TiAnimation;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.os.Build;
import android.view.View;

@SuppressLint("NewApi")
@Kroll.proxy
public class AnimatableProxy extends KrollProxy {
	private static final String TAG = "AnimatableProxy";
	protected TiAnimator pendingAnimation;
	protected Object pendingAnimationLock;

	public AnimatableProxy() {
		super();
		pendingAnimationLock = new Object();
	}

	protected void handlePendingAnimation() {
		pendingAnimation.setProxy(this);
		pendingAnimation.applyOptions();
		TiAnimatorSet tiSet = (TiAnimatorSet) pendingAnimation;
		prepareAnimatorSet(tiSet);
		tiSet.set().start();
	}

	public AnimatorSet getAnimatorSetForAnimation(Object arg) {
		if (Build.VERSION.SDK_INT < TiC.API_LEVEL_HONEYCOMB) {
			Log.e(TAG, "animate can only work on API >= 11 ");
			return null;
		}
		TiAnimator tiAnimator = createAnimator();
		if (!(tiAnimator instanceof TiAnimatorSet)) {
			Log.e(TAG, "must be a TiAnimatorSet");
			return null;
		}
		if (arg instanceof HashMap) {
			HashMap options = (HashMap) arg;
			tiAnimator.setOptions(options);
		} else if (arg instanceof TiAnimation) {
			TiAnimation anim = (TiAnimation) arg;
			tiAnimator.setAnimation(anim);
		} else {
			throw new IllegalArgumentException(
					"Unhandled argument to animate: "
							+ arg.getClass().getSimpleName());
		}
		tiAnimator.setProxy(this);
		tiAnimator.applyOptions();
		
		prepareAnimatorSet((TiAnimatorSet) tiAnimator);

		return ((TiAnimatorSet) tiAnimator).set();
	}

	protected TiAnimator createAnimator() {
		return new TiAnimatorSet();
	}

	@Kroll.method
	public void animate(Object arg,
			@Kroll.argument(optional = true) KrollFunction callback) {
		// if (Build.VERSION.SDK_INT< TiC.API_LEVEL_HONEYCOMB) {
		// Log.e(TAG, "animate can only work on API >= 11 ");
		// }
		if (pendingAnimation != null) {
			// already running animation
			pendingAnimation.cancel();
			pendingAnimation = null;
		}
		synchronized (pendingAnimationLock) {
			pendingAnimation = createAnimator();
			if (arg instanceof HashMap) {
				HashMap options = (HashMap) arg;
				pendingAnimation.setOptions(options);
			} else if (arg instanceof TiAnimation) {
				TiAnimation anim = (TiAnimation) arg;
				pendingAnimation.setAnimation(anim);
			} else {
				throw new IllegalArgumentException(
						"Unhandled argument to animate: "
								+ arg.getClass().getSimpleName());
			}

			if (callback != null) {
				pendingAnimation.setCallback(callback);
			}

			handlePendingAnimation();
		}
	}
	
	protected void prepareAnimatorSet(TiAnimatorSet tiSet) {
		AnimatorSet set = tiSet.set();
		HashMap options = tiSet.getOptions();
		if (tiSet.delay != null)
			set.setStartDelay(tiSet.delay.longValue());
		if (tiSet.duration != null)
			set.setDuration(tiSet.duration.longValue());

		set.addListener(new TiAnimatorListener(tiSet,
				options));
		
		List<Animator> list = new ArrayList<Animator>();
		
		prepareAnimatorSet(tiSet, list, options);
		
		if (tiSet.autoreverse) {
			for (int i = 0; i < list.size(); i++) {
				ValueAnimator anim = (ValueAnimator) list.get(i);
				anim.setRepeatCount(1);
				anim.setRepeatMode(ValueAnimator.REVERSE);
			}
		} else if (tiSet.repeat > 1) {
			int realRepeat = (int) (tiSet.repeat - 1);
			for (int i = 0; i < list.size(); i++) {
				ValueAnimator anim = (ValueAnimator) list.get(i);
				anim.setRepeatCount(realRepeat);
				anim.setRepeatMode(ValueAnimator.RESTART);
			}
		}		
		set.playTogether(list);
	}

	protected void prepareAnimatorSet(TiAnimatorSet tiSet, List<Animator> list,
			HashMap options) {
		Log.d(TAG, "prepareAnimatorSet", Log.DEBUG_MODE);
		AnimatorSet set = tiSet.set();
		if (tiSet.delay != null)
			set.setStartDelay(tiSet.delay.longValue());
		if (tiSet.duration != null)
			set.setDuration(tiSet.duration.longValue());

		set.addListener(new TiAnimatorListener(tiSet, options));

	}

	public void clearAnimation(TiAnimator builder) {
		synchronized (pendingAnimationLock) {
			if (pendingAnimation != null && pendingAnimation == builder) {
				pendingAnimation = null;
			}
		}
	}

	@Kroll.method
	public void cancelAllAnimations() {

		if (pendingAnimation != null) {
			pendingAnimation.cancel();
			pendingAnimation = null;
		}
	}
}
