/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package org.appcelerator.titanium.view;

import java.util.Arrays;

import org.appcelerator.kroll.common.Log;

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
	private float radius = 0;
	private float borderWidth = 0;
	private int alpha = -1;
	private RectF innerRect;
	private Path innerPath;
	private Path borderPath;
	private Paint paint;
	private boolean clipChildren = true;
	
	

	public TiBorderWrapperView(Context context)
	{
		super(context);
		innerRect = new RectF();
		paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		setWillNotDraw(false);
		updateBorderPath();
	}
	
	protected void clipCanvas(Canvas canvas) {
		if (!this.clipChildren) return;
		if (radius > 0) {
			// This still happens sometimes when hw accelerated so, catch and warn
			try {
				canvas.clipPath(innerPath);
			} catch (Exception e) {
				Log.w(TAG, "clipPath failed on canvas: " + e.getMessage(), Log.DEBUG_MODE);
			}
		} else {
			canvas.clipRect(innerRect);
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
		drawBorder(canvas);
 	 	super.onDraw(canvas);
 		clipCanvas(canvas);
	}

	private void updateBorderPath()
	{
		Rect bounds = new Rect();
		getDrawingRect(bounds);
		if (bounds.isEmpty()) return;
		
		RectF outerRect = new RectF();
		outerRect.set(bounds);

		int padding = 0;
		int maxPadding = 0;
		// cap padding to current bounds
		maxPadding = (int) Math.min(outerRect.right / 2, outerRect.bottom / 2);
		padding = (int) Math.min(borderWidth, maxPadding);
		innerRect.set(bounds.left + padding, bounds.top + padding, bounds.right - padding, bounds.bottom - padding);

		if (radius > 0) {
			float outerRadii[] = new float[8];
			Arrays.fill(outerRadii, radius);
			borderPath = new Path();
			borderPath.addRoundRect(outerRect, outerRadii, Direction.CW);
			borderPath.setFillType(FillType.EVEN_ODD);
			innerPath = new Path();
			innerPath.setFillType(FillType.EVEN_ODD);
			if (radius - padding > 0) {
				float innerRadii[] = new float[8];
				Arrays.fill(innerRadii, radius - padding);
				borderPath.addRoundRect(innerRect, innerRadii, Direction.CCW);
				innerPath.addRoundRect(innerRect, innerRadii, Direction.CW);
			} else {
				borderPath.addRect(innerRect, Direction.CCW);
				innerPath.addRect(innerRect, Direction.CW);
			}
		} else {
			borderPath = new Path();
			borderPath.addRect(outerRect, Direction.CW);
			borderPath.addRect(innerRect, Direction.CCW);
			borderPath.setFillType(FillType.EVEN_ODD);
		}
		invalidate();
	}

	private void drawBorder(Canvas canvas)
	{
		if (borderWidth != 0) {
			paint.setColor(color);
			if (alpha > -1) {
				paint.setAlpha(alpha);
			}
			canvas.drawPath(borderPath, paint);
		}
	}

	public void setColor(int color)
	{
		this.color = color;
		postInvalidate();
	}

	public void setRadius(float radius)
	{
		this.radius = radius;
		updateBorderPath();
		postInvalidate();
	}

	public float getRadius()
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
	};

}
