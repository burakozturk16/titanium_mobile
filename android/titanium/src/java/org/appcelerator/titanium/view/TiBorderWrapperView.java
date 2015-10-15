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
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.Path.FillType;
import android.graphics.drawable.Drawable;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

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
	private float borderWidth = -1;
	private int borderAlpha = -1;
	private RectF clipRect;
	private Path clipPath;
	private boolean clipChildren = true;
	private boolean antiAlias = false;
	private RectF mBorderPadding = null;
	private TiViewProxy proxy;
//	private boolean mDrawableSizeChanged = false;
	
	private View borderDrawableHoldingView = null;
	
	protected TiBackgroundDrawable mDrawable;

	public TiBorderWrapperView(Context context, TiViewProxy proxy)
	{
		super(context);
		this.proxy = proxy;
		this.setAddStatesFromChildren(true);
		clipRect = new RectF();
		
		//we dont need it but this is a trick to get the mask to also mask the border
		setWillNotDraw(false);
	}
	
	protected TiBackgroundDrawable getOrCreateDrawable()
	{
		if (mDrawable == null)
		{
			borderDrawableHoldingView = new FrameLayout(getContext()){
				@Override
				public boolean dispatchTouchEvent(MotionEvent event) {
					return false;
				}
			};
//			if (this.borderWidth == -1) {
//				this.borderWidth = TiUIHelper.getRawSize(1, null);
//			}
			mDrawable = new TiBackgroundDrawable(true);
			if (borderAlpha < 1.0)
				mDrawable.setAlpha(Math.round(borderAlpha * 255));
			if (proxy.hasProperty(TiC.PROPERTY_BACKGROUND_REPEAT))
				mDrawable.setImageRepeat(TiConvert.toBoolean(proxy.getProperty(TiC.PROPERTY_BACKGROUND_REPEAT)));
			mDrawable.setRadius(radius);
			mDrawable.setPadding(mBorderPadding);
			
//			mDrawableSizeChanged = true;
			
//			mDrawable.setVisible(getVisibility() == VISIBLE, false);
			TiUIView.setBackgroundDrawable(borderDrawableHoldingView, mDrawable);
			
			borderDrawableHoldingView.setEnabled(isEnabled());
            if (mDrawable.isStateful()) {
                mDrawable.setState(getDrawableState());
            }
			addView(borderDrawableHoldingView, new TiCompositeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
			invalidate();
			
		}
		return mDrawable;
	}
	
	protected void clipCanvas(Canvas canvas) {
		if (!this.clipChildren || this.antiAlias) return;
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
	
	public void setAntiAlias(boolean antialias) {
        this.antiAlias = antialias;
    }
    
    

	@Override
	protected void onSizeChanged (int w, int h, int oldw, int oldh) {
//		mDrawableSizeChanged = true;
		updateBorderPath();
	}
	
	@Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
	    //we must call super onlayout not to break anything
	    super.onLayout(changed, l, t, r, b);
		if (changed) updateBorderPath();
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
        invalidate();
    }
	@Override
	public void childDrawableStateChanged(View child) {
	    super.childDrawableStateChanged(child);
//		if (child != proxy.getFocusView()) return;
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
//	@Override
//	public void setVisibility(int visibility) {
//        super.setVisibility(visibility);
//        if (mDrawable != null) mDrawable.setVisible(visibility == VISIBLE, false);
//    }

	
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

//        if (TiC.LOLLIPOP_OR_GREATER) {
//            invalidateOutline();
//        }
	}

	private void drawBorder(Canvas canvas)
	{
//		if (mDrawable != null) {
//			if (mDrawableSizeChanged) {
//				mDrawable.setBounds(0, 0,  getWidth(), getHeight());
//                mDrawableSizeChanged = false; 
//            }
//			mDrawable.draw(canvas);
//		}
	}
	
	public TiBackgroundDrawable getBorderDrawable()
	{		
		return getOrCreateDrawable();
	}
	
	@Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        super.addView(child, index, params);
        if (borderDrawableHoldingView != null) {
        	borderDrawableHoldingView.bringToFront();
        }
    }
	
	private void onColorSet() {
	    if (this.borderWidth == -1) {
            this.borderWidth = TiUIHelper.getRawSize(1, null);
            if (mDrawable != null) 
            {
                mDrawable.setPathWidth(borderWidth);
            }
	    }
	}

	public void setColor(int color)
	{
		TiBackgroundDrawable bgdDrawable = getOrCreateDrawable();
		bgdDrawable.setDefaultColor(color);
		bgdDrawable.setColorForState(TiUIHelper.BACKGROUND_DEFAULT_STATE_1, color);
		bgdDrawable.setColorForState(TiUIHelper.BACKGROUND_DEFAULT_STATE_2, color);
		onColorSet();
		postInvalidate();
	}
	
	public void setColorForState(int[] stateSet, int color)
    {
	    getOrCreateDrawable().setColorForState(stateSet, color);
        onColorSet();
        postInvalidate();
    }
	
	public void setImageDrawableForState(int[] stateSet, Drawable drawable)
    {
	    getOrCreateDrawable().setImageDrawableForState(stateSet, drawable);
        onColorSet();
        postInvalidate();
    }
    
    public void setGradientDrawableForState(int[] stateSet, Drawable drawable)
    {
        getOrCreateDrawable().setGradientDrawableForState(stateSet, drawable);
        onColorSet();
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
		this.borderAlpha = alpha;
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
	
	public void setBorderPadding(RectF mBorderPadding) {
		this.mBorderPadding  = mBorderPadding;
		if (mDrawable != null) 
		{
			mDrawable.setPadding(mBorderPadding);
		}
		postInvalidate();
	};
}
