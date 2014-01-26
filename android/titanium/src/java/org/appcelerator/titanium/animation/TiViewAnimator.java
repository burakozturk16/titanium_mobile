/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package org.appcelerator.titanium.animation;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.view.Ti2DMatrix;
import android.annotation.SuppressLint;
import android.view.View;

@SuppressWarnings({"unchecked", "rawtypes"})
@SuppressLint({"FloatMath"})
public class TiViewAnimator extends TiAnimatorSet
{
	private static final String TAG = "TiViewAnimator";

	protected float anchorX;
	protected float anchorY;
	protected Ti2DMatrix tdm = null;
	protected Double toOpacity = null;
	protected String top = null, bottom = null, left = null, right = null;
	protected String centerX = null, centerY = null;
	protected String width = null, height = null;
	protected Integer backgroundColor = null;

	public boolean relayoutChild = false, applyOpacity = false;
	protected View view;
	protected TiViewProxy viewProxy;

	public TiViewAnimator()
	{
		super();
	}
	
	static List<String> kAnimationResetProperties = Arrays.asList(
			TiC.PROPERTY_DURATION, TiC.PROPERTY_DELAY,
			TiC.PROPERTY_AUTOREVERSE, TiC.PROPERTY_REPEAT,
			TiC.PROPERTY_RESTART_FROM_BEGINNING,
			TiC.PROPERTY_CANCEL_RUNNING_ANIMATIONS,
			TiC.PROPERTY_WIDTH, TiC.PROPERTY_HEIGHT,
			TiC.PROPERTY_LEFT, TiC.PROPERTY_RIGHT,
			TiC.PROPERTY_TOP,
			TiC.PROPERTY_BOTTOM);
	
	@Override
	protected List<String> animationResetProperties() {
		return kAnimationResetProperties;
	}
	
	
	public void cleanupView() {
//		if (view != null) {
//			view.clearAnimation();
//		}
		if (viewProxy != null && viewProxy.peekView() != null) {
			viewProxy.peekView().cleanAnimatedParams();
		}
	}

	@Override
	protected void handleCancel() {
		super.handleCancel();
		cleanupView();
	}
	
	@Override
	public void cancelWithoutResetting(){
		super.cancelWithoutResetting();
//		cleanupView();
	}
	
	@Override
	public void restartFromBeginning(){
		super.restartFromBeginning();
		if (viewProxy != null && viewProxy.peekView() != null) {
			viewProxy.peekView().resetAnimatedParams();
		}
	}
	
	
	@Override
	protected void handleFinish() {
		cleanupView();
		super.handleFinish();
		
	}
	
	public void setViewProxy(TiViewProxy proxy) {
		setProxy(proxy);
		this.viewProxy = proxy;
	}
	
	public void setView(View view) {
		this.view = view;
	}

	
	public HashMap getOptions() {
		return (this.animationProxy != null)?this.animationProxy.getProperties():this.options ;
	}

	@Override
	public void applyOptions()
	{
		super.applyOptions();
		HashMap options = getOptions();
		
		if (options == null) {
			return;
		}

		if (options.containsKey(TiC.PROPERTY_ANCHOR_POINT)) {
			Object anchorPoint = options.get(TiC.PROPERTY_ANCHOR_POINT);
			if (anchorPoint instanceof HashMap) {
				HashMap point = (HashMap) anchorPoint;
				anchorX = TiConvert.toFloat(point, TiC.PROPERTY_X);
				anchorY = TiConvert.toFloat(point, TiC.PROPERTY_Y);
			} else {
				Log.e(TAG, "Invalid argument type for anchorPoint property. Ignoring");
			}
		}
		this.options = options;
	}
	
	public void simulateFinish(TiViewProxy proxy)
	{
		this.proxy = this.viewProxy = proxy;
		handleFinish();
		this.proxy = this.viewProxy = null;
	}
}
