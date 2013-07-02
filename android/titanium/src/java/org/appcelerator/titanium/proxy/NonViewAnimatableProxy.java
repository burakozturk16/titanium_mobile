package org.appcelerator.titanium.proxy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.appcelerator.kroll.KrollFunction;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.util.TiAnimationBuilder;
import org.appcelerator.titanium.util.TiAnimatorListener;
import org.appcelerator.titanium.util.TiAnimatorSet;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.view.TiAnimation;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
@Kroll.proxy
public class NonViewAnimatableProxy extends KrollProxy {
	private static final String TAG = "NonViewAnimatableProxy";
	private TiAnimatorSet pendingAnimation;
	protected Object pendingAnimationLock;
	
	public NonViewAnimatableProxy()
	{
		super();
		pendingAnimationLock = new Object();
	}
	@Kroll.method
	public void animate(Object arg, @Kroll.argument(optional=true) KrollFunction callback)
	{
		if (Build.VERSION.SDK_INT< TiC.API_LEVEL_HONEYCOMB) {
			Log.e(TAG, "animate can only work on API >= 11 ");
		}
		if (pendingAnimation != null) {
			//already running animation
			pendingAnimation.cancel();
			pendingAnimation = null;
		}
		synchronized (pendingAnimationLock) {
			pendingAnimation = new TiAnimatorSet();
			if (arg instanceof HashMap) {
				HashMap options = (HashMap) arg;
				pendingAnimation.setOptions(options);
			} else if (arg instanceof TiAnimation) {
				TiAnimation anim = (TiAnimation) arg;
				pendingAnimation.setAnimation(anim);
			} else {
				throw new IllegalArgumentException("Unhandled argument to animate: " + arg.getClass().getSimpleName());
			}

			if (callback != null) {
				pendingAnimation.setCallback(callback);
			}
			pendingAnimation.setProxy(this);
			HashMap options = pendingAnimation.getOptions();
			List<Animator> list = new ArrayList<Animator>();
			prepareAnimatorSet(pendingAnimation, list, options);
			AnimatorSet set = pendingAnimation.set();
			set.playTogether(list);
			set.start();
		}
	}
	
	protected void prepareAnimatorSet(TiAnimatorSet tiSet, List<Animator> list, HashMap options) {
		Log.d(TAG, "prepareAnimatorSet", Log.DEBUG_MODE);
		AnimatorSet set = tiSet.set();
		if (options.containsKey(TiC.PROPERTY_DELAY)) {
			set.setStartDelay(TiConvert.toInt(options, TiC.PROPERTY_DELAY));
		}

		if (options.containsKey(TiC.PROPERTY_DURATION)) {
			set.setDuration(TiConvert.toInt(options, TiC.PROPERTY_DURATION));
		}

		int repeat = 1;
		final boolean autoreverse = options
				.containsKey(TiC.PROPERTY_AUTOREVERSE) ? TiConvert.toBoolean(
				options, TiC.PROPERTY_AUTOREVERSE) : false;
		tiSet.setAutoreverse(autoreverse);
		if (options.containsKey(TiC.PROPERTY_REPEAT)) {
			repeat = TiConvert.toInt(options, TiC.PROPERTY_REPEAT);

			if (repeat == 0) {
				// A repeat of 0 is probably non-sensical. Titanium iOS
				// treats it as 1 and so should we.
				repeat = 1;
			}
		}
		set.addListener(new TiAnimatorListener(tiSet,
				options));
		
		if (autoreverse) {
			for (int i = 0; i < list.size(); i++) {
				ValueAnimator anim = (ValueAnimator) list.get(i);
				anim.setRepeatCount(1);
				anim.setRepeatMode(ValueAnimator.REVERSE);
			}
		} else if (repeat > 1) {
			int realRepeat = repeat - 1;
			for (int i = 0; i < list.size(); i++) {
				ValueAnimator anim = (ValueAnimator) list.get(i);
				anim.setRepeatCount(realRepeat);
				anim.setRepeatMode(ValueAnimator.RESTART);
			}
		}
		set.playTogether(list);
	}
	
	public void clearAnimation(TiAnimationBuilder builder)
	{
		synchronized(pendingAnimationLock) {
			if (pendingAnimation != null && pendingAnimation == builder) {
				pendingAnimation = null;
			}
		}
	}

	@Kroll.method
	public void cancelAllAnimations()
	{
		
		if (pendingAnimation != null) {
			pendingAnimation.cancel();
			pendingAnimation = null;
		}
	}
}
