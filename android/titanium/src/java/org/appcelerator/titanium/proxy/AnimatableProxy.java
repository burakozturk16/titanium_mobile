package org.appcelerator.titanium.proxy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.appcelerator.kroll.KrollFunction;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.util.TiAnimator;
import org.appcelerator.titanium.util.TiAnimatorListener;
import org.appcelerator.titanium.util.TiAnimatorSet;
import org.appcelerator.titanium.view.TiAnimation;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ValueAnimator;

@SuppressWarnings({"rawtypes"})
@Kroll.proxy
public class AnimatableProxy extends KrollProxy {
	private static final String TAG = "AnimatableProxy";
	protected ArrayList<TiAnimator> pendingAnimations;
	protected ArrayList<TiAnimator> runningAnimations;
	protected Object pendingAnimationLock;
	protected Object runningAnimationsLock;

	public AnimatableProxy() {
		super();
		pendingAnimations = new ArrayList<TiAnimator>();
		runningAnimations = new ArrayList<TiAnimator>();
		pendingAnimationLock = new Object();
		runningAnimationsLock = new Object();
	}

	protected void handlePendingAnimation() {
		TiAnimator pendingAnimation;
		synchronized (pendingAnimationLock) {
			if (pendingAnimations.size() == 0) {
				return;
			}
			pendingAnimation = pendingAnimations.remove(0);
		}
		pendingAnimation.setProxy(this);
		pendingAnimation.applyOptions();
		TiAnimatorSet tiSet = (TiAnimatorSet) pendingAnimation;
		
		synchronized (runningAnimationsLock) {
			runningAnimations.add(pendingAnimation);
		}
		prepareAnimatorSet(tiSet);
		tiSet.set().start();
	}

	public AnimatorSet getAnimatorSetForAnimation(Object arg) {
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

//		if (pendingAnimation != null) {
//			// already running animation
//			pendingAnimation.cancelWithoutResetting();
//			pendingAnimation = null;
//		}
		synchronized (pendingAnimationLock) {
			TiAnimator pendingAnimation = createAnimator();
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
			pendingAnimations.add(pendingAnimation);

		}
		handlePendingAnimation();
	}
	
	protected void prepareAnimatorSet(TiAnimatorSet tiSet) {
		AnimatorSet set = tiSet.set();
		HashMap options = tiSet.getOptions();
//		if (tiSet.delay != null)
//			set.setStartDelay(tiSet.delay.longValue());
//		if (tiSet.duration != null)
//			set.setDuration(tiSet.duration.longValue());

		set.addListener(new TiAnimatorListener(tiSet,
				options));
		
		List<Animator> list = new ArrayList<Animator>();
		
		prepareAnimatorSet(tiSet, list, options);
		
		int style = tiSet.autoreverse?ValueAnimator.REVERSE:ValueAnimator.RESTART;
		int repeatCount = (tiSet.repeat == ValueAnimator.INFINITE ? tiSet.repeat : tiSet.repeat - 1);
		if (tiSet.autoreverse) {
			repeatCount = repeatCount * 2 + 1;
		}
			
		for (int i = 0; i < list.size(); i++) {
			ValueAnimator anim = (ValueAnimator) list.get(i);
			if (tiSet.delay != null)
				anim.setStartDelay(tiSet.delay.longValue());
			if (tiSet.duration != null)
				anim.setDuration(tiSet.duration.longValue());
			anim.setRepeatCount(repeatCount);
			anim.setRepeatMode(style);
		}
		set.playTogether(list);
	}

	protected void prepareAnimatorSet(TiAnimatorSet tiSet, List<Animator> list,
			HashMap options) {
		Log.d(TAG, "prepareAnimatorSet", Log.DEBUG_MODE);
//		AnimatorSet set = tiSet.set();
//		if (tiSet.delay != null)
//			set.setStartDelay(tiSet.delay.longValue());
//		if (tiSet.duration != null)
//			set.setDuration(tiSet.duration.longValue());
//		set.addListener(new TiAnimatorListener(tiSet, options));

	}
	
	public void animationFinished(TiAnimator animation) {
		synchronized (runningAnimationsLock) {
			runningAnimations.remove(animation);
		}
	}

	public void clearAnimation(TiAnimator builder)
	{
		synchronized(pendingAnimationLock) {
			pendingAnimations.remove(builder);
		}
	}

	@Kroll.method
	public void cancelAllAnimations() {
		synchronized (runningAnimationsLock) {
			for (int i = 0; i < runningAnimations.size(); i++) {
				runningAnimations.get(i).cancel();
			}
			runningAnimations.clear();
		}
	}
}
