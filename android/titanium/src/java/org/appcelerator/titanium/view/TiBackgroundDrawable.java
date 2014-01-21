/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package org.appcelerator.titanium.view;

import org.appcelerator.titanium.util.TiUIHelper;
import org.appcelerator.titanium.util.TiUIHelper.Shadow;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Path.Direction;
import android.graphics.Path.FillType;
import android.graphics.drawable.Drawable;
import android.util.SparseArray;
import android.util.StateSet;

public class TiBackgroundDrawable extends Drawable {
	static final int NOT_SET = -1;
	private int alpha = NOT_SET;
	
//	private RectF innerRect;
	private SparseArray<OneStateDrawable> drawables;
	private OneStateDrawable currentDrawable;
	private int defaultColor = Color.TRANSPARENT;
	private SparseArray<int[]> mStateSets;
	
	private RectF bounds = new RectF();
	private float[] radius = null;
	Path path = null;
	private float pathWidth = 0;
	private RectF mPadding;
	

	public TiBackgroundDrawable()
	{
		currentDrawable = null;
		mPadding = null;
		mStateSets = new SparseArray<int[]>();
		drawables = new SparseArray<OneStateDrawable>();
//		innerRect = new RectF();
	}
	
	private int keyOfStateSet(int[] stateSet) {
		int length = mStateSets.size();
		for(int i = 0; i < length; i++) {
			if (mStateSets.valueAt(i).equals(stateSet)) {
               return mStateSets.keyAt(i);
			}
		}
        return -1;
    }
	
	private int keyOfFirstMatchingStateSet(int[] stateSet) {
		int length = mStateSets.size();
		for(int i = 0; i < length; i++) {
		   if (StateSet.stateSetMatches(mStateSets.valueAt(i), stateSet)) {
               return mStateSets.keyAt(i);
           }
		}
        return -1;
    }
	
	private int keyOfBestMatchingStateSet(int[] stateSet) {
		int length = mStateSets.size();
		int bestSize = 0;
		int result = -1;
		for(int i = 0; i < length; i++) {
			int[] matchingStateSet = mStateSets.valueAt(i);
			if (StateSet.stateSetMatches(matchingStateSet, stateSet) && matchingStateSet.length > bestSize) {
			   bestSize = matchingStateSet.length;
			   result = mStateSets.keyAt(i);
			}
		}
        return result;
    }

	@Override
	public void draw(Canvas canvas)
	{
		canvas.save();
		
		if (currentDrawable != null) {
			currentDrawable.draw(canvas);
		}
		else if(defaultColor != Color.TRANSPARENT) {
			if (path != null){
				canvas.clipPath(path);
			}
			canvas.drawColor(defaultColor);
		}

		canvas.restore();
	}
	
	private float[] innerRadiusFromPadding(RectF outerRect, float padding)
	{
		float[] result = new float[8];
		for (int i = 0; i < result.length; i++) {
			result[i] = Math.max(radius[i] - padding, 0);
		}
		return result;
	}
	
	private void updatePath(){
		if (bounds.isEmpty()) return;
		path = null;
		RectF outerRect = TiUIHelper.insetRect(bounds, mPadding);
		if (radius != null) {
			path = new Path();
			path.setFillType(FillType.EVEN_ODD);
			path.addRoundRect(outerRect, radius, Direction.CW);
			if (pathWidth > 0) {
				float padding = 0;
				float maxPadding = 0;
				RectF innerRect = new RectF(); 
				maxPadding = Math.min(bounds.width() / 2, bounds.height() / 2);
				padding = Math.min(pathWidth, maxPadding);
				innerRect.set(outerRect.left + padding, outerRect.top + padding, outerRect.right - padding, outerRect.bottom - padding);
				path.addRoundRect(innerRect, innerRadiusFromPadding(outerRect, padding), Direction.CCW);
			}
		}
		else {
			if (pathWidth > 0) {
				path = new Path();
				path.setFillType(FillType.EVEN_ODD);
				path.addRect(outerRect, Direction.CW);
				int padding = 0;
				int maxPadding = 0;
				RectF innerRect = new RectF();
				maxPadding = (int) Math.min(bounds.width() / 2, bounds.height() / 2);
				padding = (int) Math.min(pathWidth, maxPadding);
				innerRect.set(outerRect.left + padding, outerRect.top + padding, outerRect.right - padding, outerRect.bottom - padding);
				path.addRect(innerRect, Direction.CCW);
			}
		}
	}
	public Path getPath(){
		return path;
	}

	@Override
	protected void onBoundsChange(Rect bounds)
	{
		this.bounds = new RectF(bounds);
		super.onBoundsChange(bounds);
		updatePath();
		int length = drawables.size();
		for(int i = 0; i < length; i++) {
		   Drawable drawable = drawables.valueAt(i);
		   drawable.setBounds(bounds);
		}
	}
	
	public void setRadius(float[] radius)
	{
		this.radius = radius;
		updatePath();
		invalidateSelf();
	}
	
	public void setPathWidth(float width)
	{
		this.pathWidth = width;
		updatePath();
		invalidateSelf();
	}
	public void setRadiusWidth(float[] radius, float width)
	{
		this.pathWidth = width;
		this.radius = radius;
		updatePath();
		invalidateSelf();
	}
	
	public void setPadding(RectF padding) {
		this.mPadding = padding;
		updatePath();
		invalidateSelf();
	}

	// @Override
	// protected boolean onLevelChange(int level)
	// {
	// 	return super.onLevelChange(level);
	// }
	
	// @Override
	// public boolean setState (int[] stateSet) {
	// 	return super.setState(stateSet);
	// }

	@Override
	protected boolean onStateChange(int[] stateSet) {
		
		super.onStateChange(stateSet);
//		setState(stateSet);
		int key = keyOfBestMatchingStateSet(stateSet);
        if (key < 0) {
        	key = keyOfBestMatchingStateSet(StateSet.WILD_CARD);
        }
		OneStateDrawable newdrawable = null;
		if (key != -1)
		{
			newdrawable = drawables.get(key);
		}
		
		if (newdrawable != currentDrawable)
		{
			currentDrawable = newdrawable;
			invalidateSelf();
			return true;
		}
		return false;
	}
	
	@Override
	public boolean isStateful()
	{
		return true;
	}
	
	private OneStateDrawable getOrCreateDrawableForState(int[] stateSet)
	{
		OneStateDrawable drawable;
		int key = keyOfStateSet(stateSet);
		if (key == -1)
		{
			key = mStateSets.size();
			mStateSets.append(key, stateSet);
			drawable = new OneStateDrawable(this);
			drawable.setAlpha(this.alpha);
			drawable.setDefaultColor(defaultColor);
			drawables.append(key, drawable);
		}
		else
		{
			drawable = drawables.get(key);
		}
		return drawable;
	}
	
	public int getColorForState(int[] stateSet)
	{
		int result = 0;
		int key = keyOfStateSet(stateSet);
		if (key != -1)
			result = drawables.get(key).getColor();
		return result;
	}
	
	public void setColorForState(int[] stateSet, int color)
	{
		getOrCreateDrawableForState(stateSet).setColor(color);
		onStateChange(getState());
	}

	
	public void setImageDrawableForState(int[] stateSet, Drawable drawable)
	{
		if (drawable != null) {
			drawable.setBounds(this.getBounds());
		}
		getOrCreateDrawableForState(stateSet).setBitmapDrawable(drawable);
		onStateChange(getState());
	}
	
	public void setGradientDrawableForState(int[] stateSet, Drawable drawable)
	{
		if (drawable != null) {
			drawable.setBounds(this.getBounds());
		}
		getOrCreateDrawableForState(stateSet).setGradientDrawable(drawable);
		onStateChange(getState());
	}
	
	public void setInnerShadowsForState(int[] stateSet, Shadow[] shadows)
	{
		getOrCreateDrawableForState(stateSet).setInnerShadows(shadows);
		onStateChange(getState());
	}
	
//	protected void setNativeView(View view)
//	{
//		int length = drawables.size();
//		for(int i = 0; i < length; i++) {
//			OneStateDrawable drawable = drawables.valueAt(i);
//			drawable.setNativeView(view);
//		}
//	}
	
	public void setImageRepeat(boolean repeat)
	{
		int length = drawables.size();
		for(int i = 0; i < length; i++) {
			OneStateDrawable drawable = drawables.valueAt(i);
			drawable.setImageRepeat(repeat);
		}
	}
	
//	public void invalidateDrawable(Drawable who) {
//		
//		int length = drawables.size();
//		for(int i = 0; i < length; i++) {
//			OneStateDrawable drawable = drawables.valueAt(i);
//			drawable.invalidateDrawable(who);
//		}
//
//	}

	public void releaseDelegate() {
		int length = drawables.size();
		for(int i = 0; i < length; i++) {
			OneStateDrawable drawable = drawables.valueAt(i);
			drawable.releaseDelegate();
		}
	}

	@Override
	public void setAlpha(int alpha)
	{
		if (alpha == this.alpha) return;
		this.alpha = alpha;
		int key = 0;
		int length = drawables.size();
		for(int i = 0; i < length; i++) {
		   key = drawables.keyAt(i);
		   Drawable drawable = drawables.get(key);
		   drawable.setAlpha(alpha);
		}
	}

	@Override
	public int getOpacity() {
		return 0;
	}

	@Override
	public void setColorFilter(ColorFilter cf) {
		
	}

	public void setDefaultColor(int defaultColor) {
		this.defaultColor = defaultColor;
		int length = drawables.size();
		for(int i = 0; i < length; i++) {
			OneStateDrawable drawable = drawables.valueAt(i);
			drawable.setDefaultColor(defaultColor);
		}
	}
}
