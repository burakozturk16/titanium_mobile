/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package org.appcelerator.titanium.view;

import java.util.Arrays;

import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.util.TiUIHelper;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.Path.FillType;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;
import android.view.ViewParent;

/**
 * This class is a wrapper for Titanium Views with borders. Any view that specifies a border
 * related property will have a border wrapper view to maintain its border.
 */
public class TiBorderWrapperView extends MaskableView
{
	public static final int SOLID = 0;
	private static final String TAG = "TiBorderWrapperView";

	private int color = Color.TRANSPARENT;
	private float[] radius = null;
	private float borderWidth = 0;
	private int alpha = -1;
	private RectF clipRect;
	private Path clipPath;
	private Path borderPath;
	private Path borderClipPath;
	private Paint paint;
	private boolean clipChildren = true;
	private Rect mBorderPadding;
	
	

	public TiBorderWrapperView(Context context)
	{
		super(context);
		clipRect = new RectF();
		paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		setWillNotDraw(false);
//		updateBorderPath();
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
	
	@Override
    public ViewParent invalidateChildInParent(final int[] location,final Rect dirty) {
		ViewParent result = super.invalidateChildInParent(location,dirty);
        return result;
    }
	
	

	@Override
	protected void onSizeChanged (int w, int h, int oldw, int oldh) {
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
	protected void onDraw(Canvas canvas)
	{
 	 	super.onDraw(canvas);
	}
	
	@Override
	protected void dispatchDraw(Canvas canvas)
	{
		canvas.save();
		clipCanvas(canvas);
		super.dispatchDraw(canvas);
		canvas.restore();
		drawBorder(canvas);
	}
	
	private static void insetRect(RectF source, Rect inset) {
		source.set(source.left + inset.left, 
				source.top + inset.top, 
				source.right - inset.right, 
				source.bottom - inset.bottom);
	}
	
	private float[] innerRadiusFromPadding(RectF outerRect)
	{
		int maxPadding = (int) Math.min(outerRect.right / 2, outerRect.bottom / 2);
		int padding = (int) Math.min(borderWidth, maxPadding);
		float[] result = new float[8];
		for (int i = 0; i < result.length; i++) {
			result[i] = Math.max(radius[i] - padding, 0);
		}
		return result;
	}
	
	private float[] clipRadiusFromPadding(RectF outerRect)
	{
		int maxPadding = (int) Math.min(outerRect.right / 2, outerRect.bottom / 2);
		int padding = (int) Math.min(borderWidth, maxPadding);
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
		
		RectF innerRect = new RectF();
		RectF outerRect = new RectF();
		RectF outerRectForDrawing = new RectF();
		RectF innerRectForDrawing = new RectF();
		outerRect.set(bounds);

		int padding = 0;
		int maxPadding = 0;
		// cap padding to current bounds
		maxPadding = (int) Math.min(outerRect.right / 2, outerRect.bottom / 2);
		padding = (int) Math.min(borderWidth, maxPadding);
		innerRect.set(bounds.left + padding, bounds.top + padding, bounds.right - padding, bounds.bottom - padding);
		innerRectForDrawing.set(innerRect);
		outerRectForDrawing.set(outerRect);
		if (mBorderPadding != null) {
			insetRect(innerRectForDrawing, mBorderPadding);
			insetRect(outerRectForDrawing, mBorderPadding);
		}
		if (radius !=null) {
			clipPath = new Path();
			clipPath.setFillType(FillType.EVEN_ODD);
			borderPath = new Path();
			borderClipPath = new Path();
			borderClipPath.setFillType(FillType.EVEN_ODD); 
			borderPath.addRoundRect(outerRectForDrawing, radius, Direction.CW);
			borderPath.setFillType(FillType.EVEN_ODD);
			clipPath.addRoundRect(outerRect, clipRadiusFromPadding(outerRect), Direction.CW);
			borderClipPath.addRoundRect(outerRect, radius, Direction.CW);
			borderPath.addRoundRect(innerRectForDrawing, innerRadiusFromPadding(outerRect), Direction.CCW);
		} else {
			clipPath = null;
			borderPath = new Path();
			borderPath.addRect(outerRectForDrawing, Direction.CW);
			borderPath.addRect(innerRectForDrawing, Direction.CCW);
			borderPath.setFillType(FillType.EVEN_ODD);
			clipRect.set(outerRect);
		}
		invalidate();
	}

	private void drawBorder(Canvas canvas)
	{
		canvas.save();
		if (borderWidth != 0) {
			paint.setColor(color);
			if (alpha > -1) {
				paint.setAlpha(alpha);
			}
			if(borderClipPath != null) {
				canvas.clipPath(borderClipPath);
			}
			canvas.drawPath(borderPath, paint);
		}
		canvas.restore();
	}

	public void setColor(int color)
	{
		this.color = color;
		if (this.borderWidth == 0) {
			this.borderWidth = TiUIHelper.getRawSize(1, null);
		}
		postInvalidate();
	}

	public void setRadius(float[] radius)
	{
		this.radius = radius;
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
		updateBorderPath();
		postInvalidate();
	}

	public void setBorderAlpha(int alpha)
	{
		this.alpha = alpha;
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
		updateBorderPath();
		postInvalidate();
	};

}
