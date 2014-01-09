/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package org.appcelerator.titanium.view;

import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiUIHelper;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.Path.FillType;
import android.graphics.drawable.shapes.PathShape;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;

/**
 * This class is a wrapper for Titanium Views with borders. Any view that specifies a border
 * related property will have a border wrapper view to maintain its border.
 */
public class TiBorderWrapperView extends MaskableView
{
	public static final int SOLID = 0;
	private static final String TAG = "TiBorderWrapperView";

//	private int color = Color.TRANSPARENT;
	private float[] radius = null;
	private float borderWidth = 0;
	private int alpha = -1;
	private RectF clipRect;
	private Path clipPath;
	private boolean clipChildren = true;
	private Rect mBorderPadding = null;
	private TiViewProxy proxy;
	private boolean mDrawableSizeChanged = false;
	
	protected TiBackgroundDrawable mDrawable;

	public TiBorderWrapperView(Context context, TiViewProxy proxy)
	{
		super(context);
		this.proxy = proxy;
		clipRect = new RectF();
		
		//we dont need it but this is a trick to get the mask to also mask the border
		setWillNotDraw(false);
	}
	
	protected TiBackgroundDrawable getOrCreateDrawable()
	{
		if (mDrawable == null)
		{
			mDrawable = new TiBackgroundDrawable();
			if (alpha < 1.0)
				mDrawable.setAlpha(Math.round(alpha * 255));
			if (proxy.hasProperty(TiC.PROPERTY_BACKGROUND_REPEAT))
				mDrawable.setImageRepeat(TiConvert.toBoolean(proxy.getProperty(TiC.PROPERTY_BACKGROUND_REPEAT)));
			mDrawable.setRadiusWidth(radius, borderWidth);
			mDrawable.setPadding(mBorderPadding);
			
			mDrawableSizeChanged = true;
			if (mDrawable.isStateful()) {
				mDrawable.setState(getDrawableState());
            }
			mDrawable.setVisible(getVisibility() == VISIBLE, false);
			invalidate();
		}
		return mDrawable;
	}
	
	protected void clipCanvas(Canvas canvas) {
		if (!this.clipChildren) return;
		if (radius != null) {
			// This still happens sometimes when hw accelerated so, catch and warn
			try {
				canvas.clipPath(clipPath);
			} catch (Exception e) {
				Log.w(TAG, "clipPath failed on canvas: " + e.getMessage(), Log.DEBUG_MODE);
			}
		} else {
			canvas.clipRect(clipRect);
		}
	}
	
	@Override
    public void setClipChildren(boolean clipChildren) {
		this.clipChildren = clipChildren;
		super.setClipChildren(clipChildren);
    }
	
//	@Override
//    public ViewParent invalidateChildInParent(final int[] location,final Rect dirty) {
//		ViewParent result = super.invalidateChildInParent(location,dirty);
//        return result;
//    }
	
	

	@Override
	protected void onSizeChanged (int w, int h, int oldw, int oldh) {
		mDrawableSizeChanged = true;
		updateBorderPath();
	}
	
	@Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int parentLeft = 0;
		int parentRight = r - l;
		int parentTop = 0;
		int parentBottom = b - t;
        for (int i = 0; i < getChildCount(); i++) {
            final View child = getChildAt(i);
            child.layout(parentLeft, parentTop, parentRight, parentBottom);
        }
    }
	
	@Override
	protected void drawableStateChanged() {
        super.drawableStateChanged();
        if (mDrawable != null && mDrawable.isStateful()) {
        	mDrawable.setState(getDrawableState());
        }
    }
	@Override
	public void childDrawableStateChanged(View child) {
		if (child != proxy.getFocusView()) return;
		if (mDrawable != null && mDrawable.isStateful()) {
        	mDrawable.setState(child.getDrawableState());
        	invalidate();
        }
	}
	
	public void setDrawableState(int[] state) {
		if (mDrawable != null && mDrawable.isStateful()) {
        	mDrawable.setState(state);
        	invalidate();
        }
	}
	@Override
	public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (mDrawable != null) mDrawable.setVisible(visibility == VISIBLE, false);
    }
	
	@Override
	protected void dispatchDraw(Canvas canvas)
	{
		if (mBorderPadding != null)
		{
			canvas.save();
			clipCanvas(canvas);
			super.dispatchDraw(canvas);
			drawBorder(canvas);
			canvas.restore();
		}
		else  {
			canvas.save();
			clipCanvas(canvas);
			super.dispatchDraw(canvas);
			canvas.restore();
			drawBorder(canvas);
		}
	}
	
	private float[] clipRadiusFromPadding(RectF outerRect)
	{
		float[] result = new float[8];
		for (int i = 0; i < result.length; i++) {
			result[i] = radius[i] + 1;
		}
		return result;
	}

	private void updateBorderPath()
	{
		Rect bounds = new Rect();
		getDrawingRect(bounds);
		if (bounds.isEmpty()) return;
		
		RectF outerRect = new RectF();
		outerRect.set(bounds);

		// cap padding to current bounds
		if (radius !=null) {
			clipPath = new Path();
			clipPath.setFillType(FillType.EVEN_ODD);
			clipPath.addRoundRect(outerRect, (borderWidth> 1)?clipRadiusFromPadding(outerRect):radius, Direction.CW);
			
		} else {
			clipPath = null;
			clipRect.set(outerRect);
		}
		invalidate();
	}

	private void drawBorder(Canvas canvas)
	{
		if (mDrawable != null) {
			if (mDrawableSizeChanged) {
				mDrawable.setBounds(0, 0,  getWidth(), getHeight());
                mDrawableSizeChanged = false; 
            }
			mDrawable.draw(canvas);
		}
	}
	
	public TiBackgroundDrawable getBorderDrawable()
	{
		TiBackgroundDrawable drawable = getOrCreateDrawable();
		if (this.borderWidth == 0) {
			this.borderWidth = TiUIHelper.getRawSize(1, null);
			if (drawable != null) 
			{
				drawable.setPathWidth(borderWidth);
			}
		}
		return drawable;
	}

	public void setColor(int color)
	{
		TiBackgroundDrawable bgdDrawable = getOrCreateDrawable();
		bgdDrawable.setDefaultColor(color);
		bgdDrawable.setColorForState(TiUIHelper.BACKGROUND_DEFAULT_STATE_1, color);
		bgdDrawable.setColorForState(TiUIHelper.BACKGROUND_DEFAULT_STATE_2, color);
//		this.color = color;
		if (this.borderWidth == 0) {
			this.borderWidth = TiUIHelper.getRawSize(1, null);
			if (mDrawable != null) 
			{
				mDrawable.setPathWidth(borderWidth);
			}
		}
		postInvalidate();
	}

	public void setRadius(float[] radius)
	{
		this.radius = radius;
		if (mDrawable != null) 
		{
			mDrawable.setRadius(radius);
		}
		updateBorderPath();
		postInvalidate();
	}

	public float[] getRadius()
	{
		return this.radius;
	}

	public void setBorderWidth(float borderWidth)
	{
		this.borderWidth = borderWidth;
		getOrCreateDrawable().setPathWidth(borderWidth);
		postInvalidate();
	}

	public void setBorderAlpha(int alpha)
	{
		this.alpha = alpha;
		if (mDrawable != null) {
			mDrawable.setAlpha(alpha);
		}
		postInvalidate();
	}
	
	@Override
	public void dispatchSetPressed(boolean pressed) {
		int count = getChildCount();
		for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            child.setPressed(pressed);
        }
	}

	public void setBorderPadding(Rect borderPadding) {
		mBorderPadding  = borderPadding;
		if (mDrawable != null) 
		{
			mDrawable.setPadding(mBorderPadding);
		}
//		updateBorderPath();
		postInvalidate();
	};
}
