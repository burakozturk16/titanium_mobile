package org.appcelerator.titanium.proxy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.appcelerator.kroll.KrollFunction;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.animation.TiAnimation;
import org.appcelerator.titanium.animation.TiAnimator;
import org.appcelerator.titanium.animation.TiAnimatorListener;
import org.appcelerator.titanium.animation.TiAnimatorSet;
import org.appcelerator.titanium.animation.TiInterpolator;

import android.view.animation.Interpolator;

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
			tiSet.needsToRestartFromBeginning = (runningAnimations.size() > 0);
			if (tiSet.cancelRunningAnimations) {
				for (int i = 0; i < runningAnimations.size(); i++) {
					runningAnimations.get(i).cancelWithoutResetting();
				}
				runningAnimations.clear();
			}
			else if (pendingAnimation.animationProxy != null){
				for (int i = 0; i < runningAnimations.size(); i++) {
					TiAnimator anim = runningAnimations.get(i);
					if (anim.animationProxy == pendingAnimation.animationProxy) {
						anim.cancelWithoutResetting();
						runningAnimations.remove(anim);
						break;
					}
				}
				runningAnimations.clear();
			}
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
		tiSet.aboutToBePrepared();
		AnimatorSet set = tiSet.set();
		HashMap options = tiSet.getOptions();

		TiAnimatorListener listener = new TiAnimatorListener(tiSet,
				options);		
		
		List<Animator> list = new ArrayList<Animator>();
		List<Animator> listReverse = tiSet.autoreverse?new ArrayList<Animator>():null;
		
		prepareAnimatorSet(tiSet, list, listReverse, (HashMap) options.clone());
		
		int repeatCount = (tiSet.repeat == ValueAnimator.INFINITE ? tiSet.repeat : tiSet.repeat - 1);
		tiSet.setRepeatCount(repeatCount);
		Interpolator interpolator = tiSet.curve;

		for (int i = 0; i < list.size(); i++) {
			ValueAnimator anim = (ValueAnimator) list.get(i);
			if (tiSet.delay != null)
				anim.setStartDelay(tiSet.delay.longValue());
			if (tiSet.duration != null)
				anim.setDuration(tiSet.duration.longValue());
			if (interpolator != null)
				anim.setInterpolator(interpolator);
		}
		set.playTogether(list);
		
		//reverse set
		if (listReverse != null) {
			AnimatorSet reverseSet = tiSet.getOrCreateReverseSet();
			Interpolator reverseInterpolator = (interpolator != null)?new TiInterpolator.ReverseInterpolator(interpolator):null;
			for (int i = 0; i < listReverse.size(); i++) {
				ValueAnimator anim = (ValueAnimator) listReverse.get(i);
				//no startdelay for the reversed animation
				if (tiSet.duration != null)
					anim.setDuration(tiSet.duration.longValue());
				if (reverseInterpolator != null)
					anim.setInterpolator(reverseInterpolator);
			}
			reverseSet.playTogether(listReverse);
		}
		///
		
		tiSet.setListener(listener);
		tiSet.createClonableSets(); //create clonable after adding listener so that it is cloned too
	}

	protected void prepareAnimatorSet(TiAnimatorSet tiSet, List<Animator> list, List<Animator> listReverse,
			HashMap options) {
		Log.d(TAG, "prepareAnimatorSet", Log.DEBUG_MODE);
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
	
	public void afterAnimationReset()
	{
		
	}
}
