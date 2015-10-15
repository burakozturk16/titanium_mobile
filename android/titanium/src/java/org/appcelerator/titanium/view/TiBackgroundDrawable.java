/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package org.appcelerator.titanium.view;

import java.lang.ref.WeakReference;

import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiUIHelper;
import org.appcelerator.titanium.util.TiUIHelper.Shadow;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
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
	
	public static interface TiBackgroundDrawableDelegate {
	    public void onTiBackgroundDrawablePathUpdated();
	}
	
	private WeakReference<TiBackgroundDrawableDelegate> mDelegate = null;
//	private RectF innerRect;
	private SparseArray<OneStateDrawable> drawables;
	private OneStateDrawable currentDrawable;
	private int defaultColor = Color.TRANSPARENT;
	private SparseArray<int[]> mStateSets;
	
	private RectF boundsF = new RectF();
	private Rect bounds = new Rect();
	private float[] radius = null;
	Path path = null;
	private float pathWidth = 0;
	private RectF mPadding;
	private Paint paint = new Paint();
	private final boolean isBorder;
	
	public TiBackgroundDrawable()
	{
        this(false);
	}
	
	public TiBackgroundDrawable(final boolean isBorder)
    {
        this.isBorder = isBorder;
        currentDrawable = null;
        mPadding = null;
        mStateSets = new SparseArray<int[]>();
        drawables = new SparseArray<OneStateDrawable>();
//      innerRect = new RectF();
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
	
//	private int keyOfFirstMatchingStateSet(int[] stateSet) {
//		int length = mStateSets.size();
//		for(int i = 0; i < length; i++) {
//		   if (StateSet.stateSetMatches(mStateSets.valueAt(i), stateSet)) {
//               return mStateSets.keyAt(i);
//           }
//		}
//        return -1;
//    }
	
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
				
                paint.setColor(defaultColor);
				canvas.drawPath(path, paint);
			}
			else {
				canvas.drawColor(defaultColor);
			}
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
	
//	private float[] insetRadius(float[] radius, float inset)
//	{
//		float[] result = new float[8];
//		for (int i = 0; i < result.length; i++) {
//			result[i] = radius[i] + inset;
//		}
//		return result;
//	}
	
	private void updatePath(){
		if (bounds.isEmpty()) return;
		path = null;
		RectF outerRect = TiUIHelper.insetRect(boundsF, mPadding);
		if (radius != null) {
		    path = new Path();
            path.setFillType(FillType.EVEN_ODD);
			if (pathWidth > 0) {
				path.addRoundRect(outerRect, radius, Direction.CW);
				float padding = 0;
				float maxPadding = 0;
				RectF innerRect = new RectF(); 
				maxPadding = Math.min(bounds.width() / 2, bounds.height() / 2);
				padding = Math.min(pathWidth, maxPadding);
				innerRect.set(outerRect.left + padding, outerRect.top + padding, outerRect.right - padding, outerRect.bottom - padding);
				path.addRoundRect(innerRect, innerRadiusFromPadding(outerRect, padding), Direction.CCW);
			}
			else if (!isBorder){
                
				//adjustment not see background under border because of antialias
				path.addRoundRect(TiUIHelper.insetRect(outerRect, 0.3f), radius, Direction.CW);
			}
		}
		else {
		    if (isBorder || pathWidth > 0) {
		        path = new Path();
                path.setFillType(FillType.EVEN_ODD);
		    }
			if (pathWidth > 0) {
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
		if (mDelegate != null) {
		    mDelegate.get().onTiBackgroundDrawablePathUpdated();
		}
	}
	public Path getPath(){
		return path;
	}
	
	public final Rect bounds()  {
        return this.bounds;
    }
	        

	@Override
	protected void onBoundsChange(Rect bounds)
	{
		this.boundsF.set(bounds);
		this.bounds.set(bounds);
        updatePath();
        int length = drawables.size();
        for(int i = 0; i < length; i++) {
           Drawable drawable = drawables.valueAt(i);
           drawable.setBounds(bounds);
        }
		super.onBoundsChange(bounds);
		
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
        	key = keyOfBestMatchingStateSet(TiUIHelper.BACKGROUND_DEFAULT_STATE_2);
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
			drawable.setBounds(bounds);
			drawables.append(key, drawable);
			
			int currentKey = keyOfBestMatchingStateSet(getState());
			if (currentKey == key) {
				currentDrawable = drawable;
			}
		}
		else
		{
			drawable = drawables.get(key);
		}
		return drawable;
	}
	
	public void removeDrawableForState(int[] stateSet)
    {
        int key = keyOfStateSet(stateSet);
        if (key != -1)
        {
            OneStateDrawable drawable = drawables.get(key);
            mStateSets.remove(key);
            drawables.remove(key);
            if (drawable == currentDrawable) {
                currentDrawable = null;
            }
        }
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
		invalidateSelf();
	}
	
	public void setColorForState(int[] stateSet, Object color)
    {
	    if (color == null) {
	        int key = keyOfStateSet(stateSet);
            if (key != -1) {
                OneStateDrawable drawable = drawables.get(key);
                if (drawable.hasOnlyColor()) {
                    mStateSets.remove(key);
                    drawables.remove(key);
                    if (drawable == currentDrawable) {
                        currentDrawable = null;
                    }
                } else {
                    drawables.get(key).setColor(TiConvert.toColor(color));             
                }
            }
	    } else {
	        getOrCreateDrawableForState(stateSet).setColor(TiConvert.toColor(color));
	    }
        invalidateSelf();
    }

	
	public void setImageDrawableForState(int[] stateSet, Drawable drawable)
	{
		if (drawable != null) {
			drawable.setBounds(this.getBounds());
	        getOrCreateDrawableForState(stateSet).setBitmapDrawable(drawable);
		} else {
		    int key = keyOfStateSet(stateSet);
	        if (key != -1) {
                drawables.get(key).setBitmapDrawable(drawable);	            
	        }
		}
		invalidateSelf();
	}
	
	public void setGradientDrawableForState(int[] stateSet, Drawable drawable)
	{
		if (drawable != null) {
			drawable.setBounds(this.getBounds());
			getOrCreateDrawableForState(stateSet).setGradientDrawable(drawable);
        } else {
            int key = keyOfStateSet(stateSet);
            if (key != -1) {
                drawables.get(key).setGradientDrawable(drawable);             
            }
		}
		invalidateSelf();
	}
	
	public void setInnerShadowsForState(int[] stateSet, Shadow[] shadows)
	{
		getOrCreateDrawableForState(stateSet).setInnerShadows(shadows);
		invalidateSelf();
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
	    if (currentDrawable != null) {
	        return currentDrawable.getOpacity();
//	        return 255;
	    }
		return PixelFormat.OPAQUE;
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
	
	public void setDelegate(TiBackgroundDrawableDelegate delegate) {
	    if (delegate != null) {
            this.mDelegate = new WeakReference<TiBackgroundDrawableDelegate>(delegate);
        }
        else {
            this.mDelegate = null;
        }
	}
}
