/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package org.appcelerator.titanium.util;

import java.util.HashMap;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiDimension;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.view.Ti2DMatrix;
import org.appcelerator.titanium.view.TiCompositeLayout;
import org.appcelerator.titanium.view.TiUIView;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.MessageQueue;
import android.util.FloatMath;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LinearInterpolator;
import android.view.animation.Transformation;
import android.view.animation.TranslateAnimation;

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
		anchorX = Ti2DMatrix.DEFAULT_ANCHOR_VALUE;
		anchorY = Ti2DMatrix.DEFAULT_ANCHOR_VALUE;
	}
	

	@Override
	protected void handleCancel() {
		super.handleCancel();
		if (view != null) {
			view.clearAnimation();
		}	
	}
	
	public void setViewProxy(TiViewProxy proxy) {
		setProxy(proxy);
		this.viewProxy = proxy;
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
	
	public void animateOnView(TiViewProxy proxy) {
		this.setViewProxy(proxy);
		this.view = viewProxy.getOuterView();
		// Pre-honeycomb, if one animation clobbers another you get a problem whereby the background of the
		// animated view's parent (or the grandparent) bleeds through.  It seems to improve if you cancel and clear
		// the older animation.  So here we cancel and clear, then re-queue the desired animation.
		if (Build.VERSION.SDK_INT < TiC.API_LEVEL_HONEYCOMB) {
			Animation currentAnimation = view.getAnimation();
			if (currentAnimation != null && currentAnimation.hasStarted() && !currentAnimation.hasEnded()) {
				// Cancel existing animation and
				// re-queue desired animation.
				currentAnimation.cancel();
				view.clearAnimation();
				proxy.handlePendingAnimation(true);
				return;
			}

		}		
		
		viewProxy.clearAnimation(this);
		AnimationSet as = this.render(proxy);

		// If a view is "visible" but not currently seen (such as because it's covered or
		// its position is currently set to be fully outside its parent's region),
		// then Android might not animate it immediately because by default it animates
		// "on first frame" and apparently "first frame" won't happen right away if the
		// view has no visible rectangle on screen.  In that case invalidate its parent, which will
		// kick off the pending animation.
		boolean invalidateParent = false;
		ViewParent viewParent = view.getParent();

		if (view.getVisibility() == View.VISIBLE && viewParent instanceof View) {
			int width = view.getWidth();
			int height = view.getHeight();

			if (width == 0 || height == 0) {
				// Could be animating from nothing to something
				invalidateParent = true;
			} else {
				Rect r = new Rect(0, 0, width, height);
				Point p = new Point(0, 0);
				invalidateParent = !(viewParent.getChildVisibleRect(view, r, p));
			}
		}

		Log.d(TAG, "starting animation: " + as, Log.DEBUG_MODE);
		view.startAnimation(as);

		if (invalidateParent) {
			((View) viewParent).postInvalidate();
		}
	}
//	
//	@Override
//	protected void applyCompletionProperties()
//	{
//		super.applyCompletionProperties();
////		((View)view.getParent()).postInvalidateDelayed(10);
//	}
//
//	@Override
//	protected void resetAnimationProperties()
//	{
//		super.resetAnimationProperties();
//		final View viewParent = (View) viewProxy.getOuterView().getParent();
//		(new Handler()).postDelayed(new Runnable() {
//		    public void run() {
//		    	viewParent.requestLayout();
//		    	viewParent.invalidate();
//
//		    }
//		},1000);
//	}
	
	public void simulateFinish(TiViewProxy proxy)
	{
		this.viewProxy = proxy;
		handleFinish();
		this.viewProxy = null;
	}

	public AnimationSet render(TiViewProxy viewProxy)
	{
		View view = viewProxy.getOuterView();
		ViewParent parent = view.getParent();
		int parentWidth = 0;
		int parentHeight = 0;

		if (parent instanceof ViewGroup) {
			ViewGroup group = (ViewGroup) parent;
			parentHeight = group.getMeasuredHeight();
			parentWidth = group.getMeasuredWidth();
		}

		return render(viewProxy, view.getLeft(), view.getTop(), view.getMeasuredWidth(),
			view.getMeasuredHeight(), parentWidth, parentHeight);
	}

//	public static TiMatrixAnimation createMatrixAnimation(TiUIView view, Ti2DMatrix matrix)
//	{
//		return new TiMatrixAnimation(view, matrix, Ti2DMatrix.DEFAULT_ANCHOR_VALUE, Ti2DMatrix.DEFAULT_ANCHOR_VALUE);
//	}

	public AnimationSet render(TiViewProxy viewProxy, int x, int y, int w, int h, int parentWidth,
		int parentHeight)
	{
		AnimationSet as = new AnimationSet(false);
		AnimationListener animationListener = new AnimationListener();
		as.setAnimationListener(animationListener);

		final TiUIView tiView = viewProxy.peekView();

		if (toOpacity != null) {
			// Determine which value to use for "from" value, in this order:
			// 1.)	If we previously performed an alpha animation on the view,
			//		use that as the from value.
			// 2.)	Else, if we have set an opacity property on the view, use
			//		that as the from value.
			// 3.)	Else, use 1.0f as the from value.

			float fromOpacity;
			float currentAnimatedAlpha =
					tiView == null ? Float.MIN_VALUE : tiView.getAnimatedAlpha();

			if (currentAnimatedAlpha != Float.MIN_VALUE) {
				// MIN_VALUE is used as a signal that no value has been set.
				fromOpacity = currentAnimatedAlpha;

			} else if (viewProxy.hasProperty(TiC.PROPERTY_OPACITY)) {
				fromOpacity = TiConvert.toFloat(viewProxy.getProperty(TiC.PROPERTY_OPACITY));

			} else {
				fromOpacity = 1.0f;
			}

			Animation animation = new AlphaAnimation(fromOpacity, toOpacity.floatValue());

			// Remember the toOpacity value for next time, since we no way of looking
			// up animated alpha values on the Android native view itself.
			if (tiView != null) {
				tiView.setAnimatedAlpha(toOpacity.floatValue());
			}

			applyOpacity = true; // Used in the animation listener
			addAnimation(as, animation);
//			animation.setAnimationListener(animationListener);

			if (viewProxy.hasProperty(TiC.PROPERTY_OPACITY) && toOpacity != null
				&& tiView != null) {
				// Initialize the opacity to 1 when we are going to change it in
				// the animation. If the opacity of the view was initialized to
				// 0, the animation doesn't work at all. If it was initialized to
				// something less than 1.0, then it "works" but doesn't give the
				// expected results. The reason seems to be partially explained
				// here:
				// http://stackoverflow.com/a/11387049/67842
				// Basically, the AlphaAnimation is transforming the
				// *existing* alpha value of the view. So to do what we want it
				// to do, we need to start with a base of 1. Surprisingly, this
				// does not seem to show a blip if the opacity was less than
				// 1.0 to begin with.
				tiView.setOpacity(1.0f);
			}
		}

		if (backgroundColor != null) {
			int fromBackgroundColor = 0;

			if (viewProxy.hasProperty(TiC.PROPERTY_BACKGROUND_COLOR)) {
				fromBackgroundColor = TiConvert.toColor(TiConvert.toString(viewProxy
					.getProperty(TiC.PROPERTY_BACKGROUND_COLOR)));
			} else {
				Log.w(TAG, "Cannot animate view without a backgroundColor. View doesn't have that property. Using #00000000");
				fromBackgroundColor = Color.argb(0, 0, 0, 0);
			}

			Animation a = new TiColorAnimation(tiView.getNativeView(), fromBackgroundColor, backgroundColor);
			addAnimation(as, a);
		}

		if (tdm != null) {
			Ti2DMatrix realTdm = tdm;
			Ti2DMatrix from = tiView.getLayoutParams().matrix;

			Animation anim;
			
			if (tiView.getLayoutParams().matrix != null) {
				realTdm = (new Ti2DMatrix(tiView.getLayoutParams().matrix)).invert().multiply(new Ti2DMatrix(tdm));
			}

			anim = new TiMatrixAnimation(tiView, realTdm, anchorX, anchorY);
			
			if (tiView.getLayoutParams().matrix != null) {
			 	((TiMatrixAnimation)anim).setFrom(from);
			}
			anim.setAnimationListener(new Animation.AnimationListener() {
				public void onAnimationStart(Animation animation) {}
				public void onAnimationRepeat(Animation animation) {}
				public void onAnimationEnd(Animation animation) {
						tiView.getLayoutParams().optionTransform = tdm;
				}
			});

			addAnimation(as, anim);

		}
		
		if (top != null || bottom != null || left != null || right != null || centerX != null || centerY != null) {
			TiDimension optionTop = null, optionBottom = null;
			TiDimension optionLeft = null, optionRight = null;
			TiDimension optionCenterX = null, optionCenterY = null;

			// Note that we're stringifying the values to make sure we
			// use the correct TiDimension constructor, except when
			// we know the values are expressed for certain in pixels.
			if (top != null) {
				optionTop = new TiDimension(top, TiDimension.TYPE_TOP);
			}

			if (bottom != null) {
				optionBottom = new TiDimension(bottom, TiDimension.TYPE_BOTTOM);
			}

			if (left != null) {
				optionLeft = new TiDimension(left, TiDimension.TYPE_LEFT);
			}

			if (right != null) {
				optionRight = new TiDimension(right, TiDimension.TYPE_RIGHT);
			}

			if (centerX != null) {
				optionCenterX = new TiDimension(centerX, TiDimension.TYPE_CENTER_X);
			}

			if (centerY != null) {
				optionCenterY = new TiDimension(centerY, TiDimension.TYPE_CENTER_Y);
			}

			int horizontal[] = new int[2];
			int vertical[] = new int[2];
			ViewParent parent = view.getParent();
			View parentView = null;

			if (parent instanceof View) {
				parentView = (View) parent;
			}

			TiCompositeLayout.computePosition(parentView, optionLeft, optionCenterX, optionRight, w, 0, parentWidth,
				horizontal);
			TiCompositeLayout.computePosition(parentView, optionTop, optionCenterY, optionBottom, h, 0, parentHeight,
				vertical);

			Animation animation = new TranslateAnimation(Animation.ABSOLUTE, 0, Animation.ABSOLUTE,
				horizontal[0] - x, Animation.ABSOLUTE, 0, Animation.ABSOLUTE, vertical[0] - y);

//			animation.setAnimationListener(animationListener);
			addAnimation(as, animation);

			// Will need to update layout params at end of animation
			// so that touch events will be recognized at new location,
			// and so that view will stay at new location after changes in
			// orientation. But if autoreversing to original layout, no
			// need to re-layout.
			relayoutChild = (autoreverse == null || !autoreverse.booleanValue());

			if (Log.isDebugModeEnabled()) {
				Log.d(TAG, "animate " + viewProxy + " relative to self: " + (horizontal[0] - x) + ", " + (vertical[0] - y),
					Log.DEBUG_MODE);
			}

		}

		if (tdm == null && (width != null || height != null)) {
			TiDimension optionWidth, optionHeight;

			if (width != null) {
				optionWidth = new TiDimension(width, TiDimension.TYPE_WIDTH);
			} else {
				optionWidth = new TiDimension(w, TiDimension.TYPE_WIDTH);
				optionWidth.setUnits(TypedValue.COMPLEX_UNIT_PX);
			}

			if (height != null) {
				optionHeight = new TiDimension(height, TiDimension.TYPE_HEIGHT);
			} else {
				optionHeight = new TiDimension(h, TiDimension.TYPE_HEIGHT);
				optionHeight.setUnits(TypedValue.COMPLEX_UNIT_PX);
			}
			
			ViewParent parent = view.getParent();
			View parentView = null;

			if (parent instanceof View) {
				parentView = (View) parent;
			}

			int toWidth = optionWidth.getAsPixels((parentView != null)?parentView:view);
			int toHeight = optionHeight.getAsPixels((parentView != null)?parentView:view);

			SizeAnimation sizeAnimation = new SizeAnimation(view, w, h, toWidth, toHeight);

			sizeAnimation.setInterpolator(new LinearInterpolator());
//			sizeAnimation.setAnimationListener(animationListener);
			addAnimation(as, sizeAnimation);
		}
		
		// Will need to update layout params at end of animation
		// so that touch events will be recognized within new
		// size rectangle, and so that new size will survive
		// any changes in orientation. But if autoreversing
		// to original layout, no need to re-layout.
		relayoutChild = (autoreverse == null || !autoreverse.booleanValue());

		// Set duration, repeatMode and fillAfter only after adding children.
		// The values are pushed down to the child animations.
		as.setFillAfter(true);

		if (duration != null) {
			as.setDuration(duration.longValue());
		}
		if (autoreverse != null && autoreverse.booleanValue()) {
			as.setRepeatMode(Animation.REVERSE);
		} else {
			as.setRepeatMode(Animation.RESTART);
		}

		// startOffset is relevant to the animation set and thus
		// not also set on the child animations.
		if (delay != null) {
			as.setStartOffset(delay.longValue());
		}

		return as;
	}

	protected class SizeAnimation extends Animation
	{
		protected View view;
		protected float fromWidth, fromHeight, toWidth, toHeight;
		protected static final String TAG = "TiSizeAnimation";

		public SizeAnimation(View view, float fromWidth, float fromHeight, float toWidth, float toHeight)
		{
			this.view = view;
			this.fromWidth = fromWidth;
			this.fromHeight = fromHeight;
			this.toWidth = toWidth;
			this.toHeight = toHeight;

			if (Log.isDebugModeEnabled()) {
				Log.d(TAG, "animate view from (" + fromWidth + "x" + fromHeight + ") to (" + toWidth + "x" + toHeight + ")",
					Log.DEBUG_MODE);
			}
		}

		@Override
		protected void applyTransformation(float interpolatedTime, Transformation transformation)
		{
			super.applyTransformation(interpolatedTime, transformation);

	        if (interpolatedTime < 1.0f) { //fixes infinite animation
				int width = 0;
				if (fromWidth == toWidth) {
					width = (int) fromWidth;
	
				} else {
					width = (int) FloatMath.floor(fromWidth + ((toWidth - fromWidth) * interpolatedTime));
				}
	
				int height = 0;
				if (fromHeight == toHeight) {
					height = (int) fromHeight;
	
				} else {
					height = (int) FloatMath.floor(fromHeight + ((toHeight - fromHeight) * interpolatedTime));
				}
	
				ViewGroup.LayoutParams params = view.getLayoutParams();
				params.width = width;
				params.height = height;
	
				if (params instanceof TiCompositeLayout.LayoutParams) {
					TiCompositeLayout.LayoutParams tiParams = (TiCompositeLayout.LayoutParams) params;
					tiParams.optionHeight = new TiDimension(height, TiDimension.TYPE_HEIGHT);
					tiParams.optionHeight.setUnits(TypedValue.COMPLEX_UNIT_PX);
					tiParams.optionWidth = new TiDimension(width, TiDimension.TYPE_WIDTH);
					tiParams.optionWidth.setUnits(TypedValue.COMPLEX_UNIT_PX);
				}
	
				view.requestLayout();
	        }
		}
		
		@Override
		public boolean willChangeBounds() {
		    return true;
		}
	}

	public static class TiMatrixAnimation extends Animation
	{
		protected TiUIView view;
		protected Ti2DMatrix matrix;
		protected Ti2DMatrix from;
		protected int childWidth, childHeight;
		protected float anchorX = -1, anchorY = -1;

		public boolean interpolate = true;

		public TiMatrixAnimation(TiUIView view, Ti2DMatrix matrix, float anchorX, float anchorY)
		{
			this.from = null;
			this.view = view;
			this.matrix = matrix;
			this.anchorX = anchorX;
			this.anchorY = anchorY;
			this.setFillAfter(true);
		}
		
		public void setFrom(Ti2DMatrix from)
		{
			this.from = from;
		}

		@Override
		public void initialize(int width, int height, int parentWidth, int parentHeight)
		{
			super.initialize(width, height, parentWidth, parentHeight);
			this.childWidth = width;
			this.childHeight = height;
		}

		@Override
		protected void applyTransformation(float interpolatedTime, Transformation transformation)
		{
			super.applyTransformation(interpolatedTime, transformation);
			View outerView = view.getOuterView();
			Matrix m;
			if (interpolate) {
				m = matrix.interpolate(outerView, interpolatedTime, childWidth, childHeight, anchorX, anchorY);
				if (from != null){
					Matrix mFrom = from.interpolate(outerView, 1, childWidth, childHeight, anchorX, anchorY);
					mFrom.preConcat(m);
					m = mFrom;
				}
			} else {
				m = getFinalMatrix(childWidth, childHeight);
			}
			
			view.applyTransform(new Ti2DMatrix(m));
		}

		public Matrix getFinalMatrix(int childWidth, int childHeight)
		{
			View outerView = view.getOuterView();
			return matrix.finalMatrixAfterInterpolation(outerView);
		}

		public void invalidateWithMatrix(View view)
		{
			int width = view.getWidth();
			int height = view.getHeight();
			Matrix m = getFinalMatrix(width, height);
			RectF rectF = new RectF(0, 0, width, height);
			m.mapRect(rectF);
			rectF.inset(-1.0f, -1.0f);
			Rect rect = new Rect();
			rectF.round(rect);

			if (view.getParent() instanceof ViewGroup) {
				int left = view.getLeft();
				int top = view.getTop();

				((ViewGroup) view.getParent()).invalidate(left + rect.left, top + rect.top, left + rect.width(),
					top + rect.height());
			}
		}
		
		@Override
		public boolean willChangeTransformationMatrix() {
		    return true;
		}
	}

	public static class TiColorAnimation extends Animation
	{
		View view;
		TransitionDrawable transitionDrawable;
		boolean reversing = false;
		int duration = 0;

		public TiColorAnimation(View view, int fromColor, int toColor)
		{
			this.view = view;

			ColorDrawable fromColorDrawable = new ColorDrawable(fromColor);
			ColorDrawable toColorDrawable = new ColorDrawable(toColor);
			transitionDrawable = new TransitionDrawable(new Drawable[] { fromColorDrawable, toColorDrawable });

			this.setAnimationListener(new android.view.animation.Animation.AnimationListener() {

				@SuppressWarnings("deprecation")
				public void onAnimationStart(Animation animation)
				{
					TiColorAnimation.this.view.setBackgroundDrawable(transitionDrawable);
					TiColorAnimation.this.duration = Long.valueOf(animation.getDuration()).intValue();
					transitionDrawable.startTransition(TiColorAnimation.this.duration);
				}

				public void onAnimationRepeat(Animation animation)
				{
					if (animation.getRepeatMode() == Animation.REVERSE) {
						reversing = !reversing;
					}
					if (reversing) {
						transitionDrawable.reverseTransition(TiColorAnimation.this.duration);
					} else {
						transitionDrawable.startTransition(TiColorAnimation.this.duration);
					}
				}

				public void onAnimationEnd(Animation animation)
				{
				}
			});
		}
	}

	protected class AnimationListener implements Animation.AnimationListener
	{
		public void onAnimationEnd(Animation a)
		{
			if (animating == false) return; //prevent double onEnd!
			animating = false;
			if (relayoutChild) {
				ViewGroup.LayoutParams params = view.getLayoutParams();
				if (autoreverse == false && params instanceof TiCompositeLayout.LayoutParams)
					TiConvert.fillLayout(options, (TiCompositeLayout.LayoutParams)params);
				view.setLayoutParams(params);
				relayoutChild = false;
			}

			if (applyOpacity && (autoreverse == null || !autoreverse.booleanValue())) {

				if (toOpacity.floatValue() == 0) {
					view.setVisibility(View.INVISIBLE);

				} else {
					if (view.getVisibility() == View.INVISIBLE) {
						view.setVisibility(View.VISIBLE);
					}
					if (viewProxy.peekView() != null) {
						viewProxy.peekView().setOpacity(toOpacity.floatValue());
					}
				}
				applyOpacity = false;
			}

			if (a instanceof AnimationSet) {
				handleFinish();
			}
		}
		
		public void onAnimationRepeat(Animation a)
		{
			if (animationProxy != null) {
				animationProxy.fireEvent(TiC.EVENT_REPEAT, null);
			}
		}

		public void onAnimationStart(Animation a)
		{
			animating = true;
			if (animationProxy != null) {
				animationProxy.fireEvent(TiC.EVENT_START, null);
			}
			if (applyOpacity) {
				// There is an android bug where animations still occur after
				// this method. We clear it from the view to
				// correct this.
				if (toOpacity.floatValue() > 0) {
					if (view.getVisibility() == View.INVISIBLE) {
						view.setVisibility(View.VISIBLE);
					}
				}
			}
		}
	}
}
