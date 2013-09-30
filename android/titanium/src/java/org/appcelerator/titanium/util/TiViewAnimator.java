/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package org.appcelerator.titanium.util;

import java.util.HashMap;

import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.TiViewProxy;
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
	
	
	private void cleanupView() {
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

		if (options.containsKey(TiC.PROPERTY_TRANSFORM)) {
			tdm = (Ti2DMatrix) options.get(TiC.PROPERTY_TRANSFORM);
		}
	
		if (options.containsKey(TiC.PROPERTY_OPACITY)) {
			toOpacity = TiConvert.toDouble(options, TiC.PROPERTY_OPACITY);
		}

		if (options.containsKey(TiC.PROPERTY_TOP)) {
			top = TiConvert.toString(options, TiC.PROPERTY_TOP);
		}

		if (options.containsKey(TiC.PROPERTY_BOTTOM)) {
			bottom = TiConvert.toString(options, TiC.PROPERTY_BOTTOM);
		}

		if (options.containsKey(TiC.PROPERTY_LEFT)) {
			left = TiConvert.toString(options, TiC.PROPERTY_LEFT);
		}

		if (options.containsKey(TiC.PROPERTY_RIGHT)) {
			right = TiConvert.toString(options, TiC.PROPERTY_RIGHT);
		}

		if (options.containsKey(TiC.PROPERTY_CENTER)) {
			Object centerPoint = options.get(TiC.PROPERTY_CENTER);
			if (centerPoint instanceof HashMap) {
				HashMap center = (HashMap) centerPoint;
				centerX = TiConvert.toString(center, TiC.PROPERTY_X);
				centerY = TiConvert.toString(center, TiC.PROPERTY_Y);

			} else {
				Log.e(TAG, "Invalid argument type for center property. Ignoring");
			}
		}

		if (options.containsKey(TiC.PROPERTY_WIDTH)) {
			width = TiConvert.toString(options, TiC.PROPERTY_WIDTH);
		}

		if (options.containsKey(TiC.PROPERTY_HEIGHT)) {
			height = TiConvert.toString(options, TiC.PROPERTY_HEIGHT);
		}

		if (options.containsKey(TiC.PROPERTY_BACKGROUND_COLOR)) {
			backgroundColor = TiConvert.toColor(options, TiC.PROPERTY_BACKGROUND_COLOR);
		}

		this.options = options;
	}
	
	public void simulateFinish(TiViewProxy proxy)
	{
		this.viewProxy = proxy;
		handleFinish();
		this.viewProxy = null;
	}

}
