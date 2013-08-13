/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiAnimatorSet;
import org.appcelerator.titanium.view.TiAnimation;


import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.Animator.AnimatorListener;
import android.annotation.TargetApi;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.view.View;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
@Kroll.proxy(creatableInModule=UIModule.class, propertyAccessors={
	TiC.PROPERTY_DELAY,
	TiC.PROPERTY_DURATION,
	TiC.PROPERTY_REPEAT,
	TiC.PROPERTY_PLAY_MODE,
	TiC.PROPERTY_AUTOREVERSE
})
public class AnimationSetProxy extends KrollProxy implements Handler.Callback{
	private final Map<KrollProxy, TiViewProxy> mAnimations;
	private final List<KrollProxy> mAnimationsOrder;
	private AnimatorSet currentlyRunningSet;
	private boolean animating;
	private static final String TAG = AnimationSetProxy.class.getSimpleName();
	
	private static int FOR_NOTHING = 0;
	private static int FOR_PLAY_SEQ = 1;
	private static int FOR_PLAY_TOGETHER = 2;
	
	private static final int MSG_FIRST_ID = KrollProxy.MSG_LAST_ID + 1;

	private static final int MSG_START = MSG_FIRST_ID + 100;
	private static final int MSG_CANCEL = MSG_START + 1;
	
	@Override
	public boolean handleMessage(Message msg) {
		if (msg.what == MSG_START) {
			start((Integer) msg.obj);
			return true;
		} else if (msg.what == MSG_CANCEL) {
			cancel();
			return true;
		}
		return super.handleMessage(msg);
	}

	
	// Constructor
	public AnimationSetProxy() {
		super();
		mAnimations = new HashMap<KrollProxy, TiViewProxy>();
		mAnimationsOrder = new ArrayList<KrollProxy>();
		currentlyRunningSet = null;
		animating = false;
	}
	
	@Override
	public void release()
	{
		mAnimations.clear();
		mAnimationsOrder.clear();
		super.release();

	}
	
	@Kroll.method
	public void cancel() {
		if (currentlyRunningSet == null) return;
		if (!TiApplication.isUIThread()) {
			Message message = getMainHandler().obtainMessage(MSG_CANCEL);
			message.sendToTarget();
		}
		else {
			Log.d(TAG, "cancel", Log.DEBUG_MODE);
			currentlyRunningSet.cancel();
			currentlyRunningSet = null;
		}
	}

	public List<Animator> getSetList(int mode) {
		List<Animator> list = new ArrayList<Animator>();
		for (int i = 0; i < mAnimationsOrder.size(); i++) {
			KrollProxy proxy = mAnimationsOrder.get(i);
			if (proxy instanceof AnimationSetProxy) {
				AnimatorSet set = ((AnimationSetProxy)proxy).getSet(mode);
				if (set != null) {
					list.add(set);
				}
			} else if(proxy instanceof TiAnimation) {
				TiViewProxy viewProxy = mAnimations.get(proxy);
				if (viewProxy != null) {
					View view = viewProxy.getOuterView();
					if (view != null) {
						TiAnimatorSet tiSet = new TiAnimatorSet();
						tiSet.setViewProxy(viewProxy);
						tiSet.setAnimation((TiAnimation)proxy);
						tiSet.applyOptions();
						viewProxy.peekView().prepareAnimatorSet(tiSet);
						list.add(tiSet.set());
					}
					else {
						Log.e(TAG, "View is not realized, can't animate it with the set " + viewProxy);
					}
				} else {
					Log.e(TAG, "The animation was not added with a view, can't work " + proxy);
				}
			}
		}
		Log.d(TAG, "getSetList " + list.size(), Log.DEBUG_MODE);
		return list;
	}
	
	public AnimatorSet getSet(int mode) {
		Log.d(TAG, "getSet", Log.DEBUG_MODE);
		AnimatorSet fullSet = new AnimatorSet();
		List<Animator> list = getSetList(mode);
		
		int myMode = properties.optInt(TiC.PROPERTY_PLAY_MODE, FOR_NOTHING);
		if (myMode == FOR_NOTHING)
			myMode = mode;
		
		if (myMode == FOR_PLAY_SEQ)
			fullSet.playSequentially(list);
		else
			fullSet.playTogether(list);
		
		return fullSet;
	}
	
	private void start(int mode) {
		Log.d(TAG, "start " + mode, Log.DEBUG_MODE);
		cancel();
		currentlyRunningSet = getSet(mode);
		if (properties.containsKey(TiC.PROPERTY_DELAY)) {
			currentlyRunningSet.setStartDelay(properties.getInt(TiC.PROPERTY_DELAY));
		}
		if (properties.containsKey(TiC.PROPERTY_DURATION)) {
			currentlyRunningSet.setDuration(properties.getInt(TiC.PROPERTY_DURATION));
		}
		currentlyRunningSet.addListener(new AnimatorListener() {
			
			@Override
			public void onAnimationStart(Animator animation) {
				Log.d(TAG, "onAnimationStart", Log.DEBUG_MODE);
				animating = true;	
				fireEvent(TiC.EVENT_START, null);
			}
			
			@Override
			public void onAnimationRepeat(Animator animation) {
				Log.d(TAG, "onAnimationRepeat", Log.DEBUG_MODE);
				fireEvent(TiC.EVENT_REPEAT, null);
				
			}
			
			@Override
			public void onAnimationEnd(Animator animation) {
				Log.d(TAG, "onAnimationEnd", Log.DEBUG_MODE);
				if (animating == true) { //this will handle the cancel case
					animating = false;
					fireEvent(TiC.EVENT_COMPLETE, null);
				}
				
			}
			
			@Override
			public void onAnimationCancel(Animator animation) {
				animating = false;
				Log.d(TAG, "onAnimationCancel", Log.DEBUG_MODE);

			}
		});
		currentlyRunningSet.start();
	}
	
	@Kroll.getProperty
	public boolean animating() {
		return animating;
	}
	
	@Kroll.method
	public void startSequentially() {
		Log.d(TAG, "startSequentially", Log.DEBUG_MODE);
		if (!TiApplication.isUIThread()) {
			Message message = getMainHandler().obtainMessage(MSG_START, FOR_PLAY_SEQ);
			message.sendToTarget();
		} else {
			start(FOR_PLAY_SEQ);
		}
	}
	
	@Kroll.method
	public void startTogether() {
		if (!TiApplication.isUIThread()) {
			Message message = getMainHandler().obtainMessage(MSG_START, FOR_PLAY_TOGETHER);
			message.sendToTarget();
		} else {
			start(FOR_PLAY_TOGETHER);
		}
	}
	
	@Kroll.method
	public void start() {
		startSequentially();
	}

	@Kroll.method
	public void add(Object arg, @Kroll.argument(optional = true) Object obj) {
		if (arg instanceof AnimationSetProxy){
			mAnimations.put((KrollProxy) arg, null);
			mAnimationsOrder.add((KrollProxy) arg);
		} else if (arg instanceof TiAnimation){
			if (obj != null) {
				mAnimations.put((KrollProxy) arg, (TiViewProxy) obj);
				mAnimationsOrder.add((KrollProxy) arg);
			} else {
				Log.e(TAG, "when adding an Animation you must supply a view with it");
			}
		} else {
			Log.e(TAG, "AnimationSet can only add Animation or AnimationSet");
		}
	}
	
	@Kroll.method
	public void remove(Object arg) {
		if (arg instanceof AnimationSetProxy || arg instanceof TiAnimation){
			mAnimations.remove((KrollProxy) arg);
			mAnimationsOrder.remove((KrollProxy) arg);
		} else {
			Log.e(TAG, "AnimationSet can only remove Animation or AnimationSet");
		}
	}
}
