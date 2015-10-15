/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package org.appcelerator.titanium.animation;

import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollFunction;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.AnimatableProxy;
import org.appcelerator.titanium.util.TiConvert;

import android.os.Looper;
import android.os.MessageQueue;
import android.view.animation.Interpolator;

@SuppressWarnings({"unchecked", "rawtypes"})
public class TiAnimator
{
	private static final String TAG = "TiAnimator";

	public Double delay = null;
    private Double duration = null;
    private Double reverseDuration = null;
	public int repeat = 1;
	public Boolean autoreverse = false;
	public Boolean restartFromBeginning = false;
	public Boolean cancelRunningAnimations = false;
    private Interpolator curve = null;
    private Interpolator reverseCurve = null;
	protected boolean animating;
	protected boolean cancelled = false;

	public TiAnimation animationProxy;
	protected KrollFunction callback;
	public HashMap options;
	protected AnimatableProxy proxy;

	public TiAnimator()
	{
		animating = false;
		cancelled = false;
	}
	
	protected void handleCancel() {
        cancelled = true;
//		if (proxy != null) {
//			proxy.animationFinished(this);
//		}
//		resetAnimationProperties();
//		proxy.afterAnimationReset();
	};
	
	public void cancel(){
		if (animating == false || cancelled == true) return;
        cancelled = true;
		Log.d(TAG, "cancel", Log.DEBUG_MODE);
		handleCancel();
	}
	
	public void cancelWithoutResetting(){
		if (animating == false) return;
		Log.d(TAG, "cancelWithoutResetting", Log.DEBUG_MODE);
		animating = false; //will prevent the call the handleFinish
	}
	
	
	public void setOptions(HashMap options) {
		this.options = (HashMap) options.clone();
		applyOptions();
	}
	
	public void setAnimation(TiAnimation animation) {
		this.animationProxy = animation;
		this.animationProxy.setAnimator(this);
		this.setOptions(animation.getClonedProperties());
	}
	
	public void setProxy(AnimatableProxy proxy) {
		this.proxy = proxy;
	}
	
	public HashMap getOptions() {
	    return this.options;
//		return (this.animationProxy != null)?this.animationProxy.getProperties():this.options ;
	}

	protected void applyOptions()
	{
//		HashMap options = getOptions();
		
		if (options == null) {
			return;
		}

		if (options.containsKey(TiC.PROPERTY_DELAY)) {
			delay = TiConvert.toDouble(options, TiC.PROPERTY_DELAY);
			options.remove(TiC.PROPERTY_DELAY);
		}

		if (options.containsKey(TiC.PROPERTY_DURATION)) {
			duration = TiConvert.toDouble(options, TiC.PROPERTY_DURATION);
            options.remove(TiC.PROPERTY_DURATION);
		}
		if (options.containsKey(TiC.PROPERTY_REVERSE_DURATION)) {
            reverseDuration = TiConvert.toDouble(options, TiC.PROPERTY_REVERSE_DURATION);
            options.remove(TiC.PROPERTY_REVERSE_DURATION);
        }
		if (options.containsKey(TiC.PROPERTY_REPEAT)) {
			repeat = TiConvert.toInt(options, TiC.PROPERTY_REPEAT);

			if (repeat == 0) {
				// A repeat of 0 is probably non-sensical. Titanium iOS
				// treats it as 1 and so should we.
				repeat = 1;
			}
            options.remove(TiC.PROPERTY_REPEAT);
		} else {
			repeat = 1; // Default as indicated in our documentation.
		}

		if (options.containsKey(TiC.PROPERTY_AUTOREVERSE)) {
			autoreverse = TiConvert.toBoolean(options, TiC.PROPERTY_AUTOREVERSE);
            options.remove(TiC.PROPERTY_AUTOREVERSE);
		}
		
		if (options.containsKey(TiC.PROPERTY_RESTART_FROM_BEGINNING)) {
			restartFromBeginning = TiConvert.toBoolean(options, TiC.PROPERTY_RESTART_FROM_BEGINNING);
            options.remove(TiC.PROPERTY_RESTART_FROM_BEGINNING);
		}
		
		if (options.containsKey(TiC.PROPERTY_CANCEL_RUNNING_ANIMATIONS)) {
			cancelRunningAnimations = TiConvert.toBoolean(options, TiC.PROPERTY_CANCEL_RUNNING_ANIMATIONS);
            options.remove(TiC.PROPERTY_CANCEL_RUNNING_ANIMATIONS);
		}
		if (options.containsKey(TiC.PROPERTY_CURVE)) {
			Object value = options.get(TiC.PROPERTY_CURVE);
			if (value instanceof Number) {
				curve = TiInterpolator.getInterpolator(TiConvert.toInt(value), duration);
			}
			
			else if (value instanceof Object[]) {
				double[] values = TiConvert.toDoubleArray((Object[]) value);
				if (values.length == 4) {
					curve =new CubicBezierInterpolator(values[0], values[1], values[2], values[3]);
				}
			}
            options.remove(TiC.PROPERTY_CURVE);
		}
		if (options.containsKey(TiC.PROPERTY_REVERSE_CURVE)) {
            Object value = options.get(TiC.PROPERTY_REVERSE_CURVE);
            if (value instanceof Number) {
                reverseCurve = TiInterpolator.getInterpolator(TiConvert.toInt(value), duration);
            }
            
            else if (value instanceof Object[]) {
                double[] values = TiConvert.toDoubleArray((Object[]) value);
                if (values.length == 4) {
                    reverseCurve =new CubicBezierInterpolator(values[0], values[1], values[2], values[3]);
                }
            }
            options.remove(TiC.PROPERTY_REVERSE_CURVE);
        }

//		this.options = options;
	}
	
	public boolean animating() {
		return animating;
	}
	
//	static List<String> kAnimationProperties = Arrays.asList(
//			TiC.PROPERTY_DURATION, TiC.PROPERTY_DELAY,
//			TiC.PROPERTY_AUTOREVERSE, TiC.PROPERTY_REPEAT,
//			TiC.PROPERTY_RESTART_FROM_BEGINNING,
//			TiC.PROPERTY_CANCEL_RUNNING_ANIMATIONS,
//			TiC.PROPERTY_CURVE);
//
//	protected List<String> animationProperties() {
//		return kAnimationProperties;
//	}
//	
//	protected List<String> animationResetProperties() {
//		return kAnimationProperties;
//	}
	
	public HashMap getToOptions() {
	    if (this.options.containsKey("to")) {
	        return (HashMap) this.options.get("to");
	    } else if  (this.options.containsKey("from")) {
	        KrollDict toProps = new KrollDict();
	        KrollDict properties = proxy.getProperties();
	        Iterator it = ((HashMap) this.options.get("from")).entrySet().iterator();        
	        while (it.hasNext()) {
	            Map.Entry pairs = (Map.Entry)it.next();
	            String key = (String)pairs.getKey();
	            toProps.put(key, properties.get(key));
	        }
	        return toProps;
	    }
	    return this.options;
	}
	
	public HashMap getFromOptions() {
        if (this.options.containsKey("from")) {
            return (HashMap) this.options.get("from");
        }
        return proxy.getProperties();
    }

	public void resetAnimationProperties()
	{
		applyResetProperties();
        proxy.afterAnimationReset();
	}
	
	protected void handleFinish()
	{		
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
			if (TiC.HONEYCOMB_OR_GREATER) {
				this.animationProxy.fireEvent(TiC.EVENT_COMPLETE);
			} else {
				Looper.myQueue().addIdleHandler(new MessageQueue.IdleHandler() {
					public boolean queueIdle()
					{
						animationProxy.fireEvent(TiC.EVENT_COMPLETE);
						return false;
					}
				});
			}
		}
		if (proxy != null) {
			proxy.animationFinished(this);
		}
	}

	protected void applyCompletionProperties()
	{
		if (options == null || proxy == null || autoreverse == true) {
			return;
		}
        HashMap toProps = getToOptions();
        proxy.applyPropertiesInternal(toProps, true);
	}
	
	protected void applyResetProperties()
	{
	    if (this.options == null || proxy == null) {
            return;
        }
        HashMap toProps = getToOptions();
        HashMap fromProps = getFromOptions();

        Iterator it = toProps.entrySet().iterator();        
        KrollDict resetProps = new KrollDict();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry)it.next();
            String key = (String)pairs.getKey();
            resetProps.put(key, fromProps.get(key));
        }
        proxy.applyPropertiesInternal(resetProps, true, true);
	}

	public void setCallback(KrollFunction callback)
	{
		this.callback = callback;
	}
	
	public void restartFromBeginning(){
		applyResetProperties();
		proxy.afterAnimationReset();
	}

//	protected void addAnimation(AnimationSet animationSet, Animation animation)
//	{
//		// repeatCount is ignored at the AnimationSet level, so it needs to
//		// be set for each child animation manually.
//
//		// We need to reduce the repeat count by 1, since for native Android
//		// 1 would mean repeating it once.
//		int repeatCount = (repeat == ValueAnimator.INFINITE ? repeat : repeat - 1);
//
//		// In Android (native), the repeat count includes reverses. So we
//		// need to double-up and add one to the repeat count if we're reversing.
//		if (autoreverse != null && autoreverse.booleanValue()) {
//			repeatCount = repeatCount * 2 + 1;
//		}
//
//		animation.setRepeatCount(repeatCount);
//
//		animationSet.addAnimation(animation);
//	}
	
	public Interpolator getCurve() {
	    return curve;
	}
	public Interpolator getReverseCurve() {
	    if (reverseCurve != null) {
	        return reverseCurve;
	    } else if (curve != null) {
            return new TiInterpolator.ReverseInterpolator(curve);
        }
        return null;
    }
	
	public Double getDuration() {
        return duration;
    }
    public Double getReverseDuration() {
        if (reverseDuration != null) {
            return reverseDuration;
        }
        return duration;
    }
}
