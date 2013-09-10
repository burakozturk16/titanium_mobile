/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package org.appcelerator.titanium.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Iterator;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollFunction;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.AnimatableProxy;
import org.appcelerator.titanium.view.TiAnimation;

import android.animation.ValueAnimator;
import android.os.Build;
import android.os.Looper;
import android.os.MessageQueue;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;

public class TiAnimator
{
	private static final String TAG = "TiAnimator";

	public Double delay = null;
	public Double duration = null;
	public int repeat = 1;
	public Boolean autoreverse = false;
	public Boolean restartFromBeginning = true;
	protected boolean animating;

	public TiAnimation animationProxy;
	protected KrollFunction callback;
	@SuppressWarnings("rawtypes")
	public HashMap options;
	protected AnimatableProxy proxy;

	public TiAnimator()
	{
		animating = false;
	}
	
	protected void handleCancel() {
		if (proxy != null) {
			proxy.animationFinished(this);
		}
		resetAnimationProperties();
	};
	
	public void cancel(){
		if (animating == false) return;
		Log.d(TAG, "cancel", Log.DEBUG_MODE);
		animating = false; //will prevent the call the handleFinish
		handleCancel();
	}
	
	public void cancelWithoutResetting(){
		if (animating == false) return;
		Log.d(TAG, "cancel", Log.DEBUG_MODE);
		animating = false; //will prevent the call the handleFinish
	}
	
	
	public void setOptions(HashMap options) {
		this.options = options;
	}
	
	public void setAnimation(TiAnimation animation) {
		this.animationProxy = animation;
		this.animationProxy.setAnimator(this);
	}
	
	public void setProxy(AnimatableProxy proxy) {
		this.proxy = proxy;
	}
	
	public HashMap getOptions() {
		return (this.animationProxy != null)?this.animationProxy.getProperties():this.options ;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public void applyOptions()
	{
		HashMap options = getOptions();
		
		if (options == null) {
			return;
		}

		if (options.containsKey(TiC.PROPERTY_DELAY)) {
			delay = TiConvert.toDouble(options, TiC.PROPERTY_DELAY);
		}

		if (options.containsKey(TiC.PROPERTY_DURATION)) {
			duration = TiConvert.toDouble(options, TiC.PROPERTY_DURATION);
		}
		if (options.containsKey(TiC.PROPERTY_REPEAT)) {
			repeat = TiConvert.toInt(options, TiC.PROPERTY_REPEAT);

			if (repeat == 0) {
				// A repeat of 0 is probably non-sensical. Titanium iOS
				// treats it as 1 and so should we.
				repeat = 1;
			}
		} else {
			repeat = 1; // Default as indicated in our documentation.
		}

		if (options.containsKey(TiC.PROPERTY_AUTOREVERSE)) {
			autoreverse = TiConvert.toBoolean(options, TiC.PROPERTY_AUTOREVERSE);
		}

		this.options = options;
	}
	
	public boolean animating() {
		return animating;
	}
	
	
	protected List<String> animationProperties(){
		return Arrays.asList(TiC.PROPERTY_DURATION, TiC.PROPERTY_DELAY, TiC.PROPERTY_AUTOREVERSE, TiC.PROPERTY_REPEAT);
	}

	@SuppressWarnings("rawtypes")
	protected void resetAnimationProperties()
	{
		if (this.options == null || proxy == null) {
			return;
		}

		Iterator it = this.options.entrySet().iterator();
		List<String>animationProperties = animationProperties();
		KrollDict resetProps = new KrollDict();
		while (it.hasNext()) {
			Map.Entry pairs = (Map.Entry)it.next();
			String key = (String)pairs.getKey();
			if (!animationProperties.contains(key)) {
				resetProps.put(key, proxy.getProperty(key));
			}
		}
		proxy.applyPropertiesInternal(resetProps, true);
	}
	
	protected void handleFinish()
	{
		if (proxy != null) {
			proxy.animationFinished(this);
		}
		if (autoreverse == true) {
			resetAnimationProperties();
		}
		else {
			applyCompletionProperties();
		}
		if (callback != null && proxy != null) {
			callback.callAsync(proxy.getKrollObject(), new Object[] { new KrollDict() });
		}

		if (this.animationProxy != null) {
			this.animationProxy.setAnimator(null);
			// In versions prior to Honeycomb, don't fire the event
			// until the message queue is empty. There appears to be
			// a bug in versions before Honeycomb where this
			// onAnimationEnd listener can be called even before the
			// animation is really complete.
			if (Build.VERSION.SDK_INT >= TiC.API_LEVEL_HONEYCOMB) {
				this.animationProxy.fireEvent(TiC.EVENT_COMPLETE, null);
			} else {
				Looper.myQueue().addIdleHandler(new MessageQueue.IdleHandler() {
					public boolean queueIdle()
					{
						animationProxy.fireEvent(TiC.EVENT_COMPLETE, null);
						return false;
					}
				});
			}
		}
	}

	@SuppressWarnings("rawtypes")
	protected void applyCompletionProperties()
	{
		HashMap options = getOptions();
		if (options == null || proxy == null || autoreverse == true) {
			return;
		}

		Iterator it = options.entrySet().iterator();
		List<String>animationProperties = animationProperties();	
		KrollDict resetProps = new KrollDict();
		while (it.hasNext()) {
			Map.Entry pairs = (Map.Entry)it.next();
			String key = (String)pairs.getKey();
			if (!animationProperties.contains(key)) {
				resetProps.put(key, pairs.getValue());
			}
		}
		proxy.applyPropertiesInternal(resetProps, true);
	}

	public void setCallback(KrollFunction callback)
	{
		this.callback = callback;
	}

	protected void addAnimation(AnimationSet animationSet, Animation animation)
	{
		// repeatCount is ignored at the AnimationSet level, so it needs to
		// be set for each child animation manually.

		// We need to reduce the repeat count by 1, since for native Android
		// 1 would mean repeating it once.
		int repeatCount = (repeat == ValueAnimator.INFINITE ? repeat : repeat - 1);

		// In Android (native), the repeat count includes reverses. So we
		// need to double-up and add one to the repeat count if we're reversing.
		if (autoreverse != null && autoreverse.booleanValue()) {
			repeatCount = repeatCount * 2 + 1;
		}

		animation.setRepeatCount(repeatCount);

		animationSet.addAnimation(animation);
	}
}
