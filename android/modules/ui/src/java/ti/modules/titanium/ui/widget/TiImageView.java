/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.ui.widget;

import java.lang.ref.WeakReference;

import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.transition.Transition;
import org.appcelerator.titanium.util.TiUIHelper;
import org.appcelerator.titanium.view.MaskableView;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.Animator.AnimatorListener;
import com.trevorpage.tpsvg.SVGDrawable;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ZoomControls;

public class TiImageView extends MaskableView implements Handler.Callback, OnClickListener
{
	private static final String TAG = "TiImageView";

	private static final int CONTROL_TIMEOUT = 4000;
	private static final int MSG_HIDE_CONTROLS = 500;

	private Handler handler;

	private OnClickListener clickListener;

	private boolean enableScale;
	private boolean enableZoomControls;

	private GestureDetector gestureDetector;
	private ImageView imageView;
	private ZoomControls zoomControls;

	private float scaleFactor;
	private float scaleIncrement;
	private float scaleMin;
	private float scaleMax;
	
	private Matrix baseMatrix;
	private Matrix changeMatrix;

	private Boolean readyToLayout = false;
	private Boolean configured = false;
	private Drawable  queuedDrawable = null;
	private Bitmap  queuedBitmap = null;
	private Transition  queuedTransition = null;
	private boolean  inTransition = false;
	
	private ScaleType wantedScaleType = ScaleType.FIT_CENTER;
	
	// Flags to help determine whether width/height is defined, so we can scale appropriately
	private boolean viewWidthDefined;
	private boolean viewHeightDefined;

	private int orientation;
	
	private WeakReference<TiViewProxy> proxy;

	private ImageView oldImageView = null;

	public TiImageView(Context context) {
		super(context);

		final TiImageView me = this;

		handler = new Handler(Looper.getMainLooper(), this);

		enableZoomControls = false;
		scaleFactor = 1.0f;
		scaleIncrement = 0.1f;
		scaleMin = 1.0f;
		scaleMax = 5.0f;
		orientation = 0;
		configured = false;
		baseMatrix = new Matrix();
		changeMatrix = new Matrix();

		imageView = new ImageView(context) {
			@Override
			protected void onSizeChanged(int w, int h, int oldw, int oldh) {
				Drawable drawable  = imageView.getDrawable(); 
				
			    if (!(drawable instanceof SVGDrawable)) {
			        return;
			    } 
			    imageView.setImageDrawable(null); 
			    int vWidth = w - getPaddingLeft() - getPaddingRight();
			    int vHeight = h - getPaddingTop() - getPaddingBottom();
			    ((SVGDrawable)drawable).adjustToParentSize(vWidth, vHeight);
			    imageView.setImageDrawable(drawable);
			}
			
			@Override
			public void setScaleType (ScaleType scaleType) {
				super.setScaleType(scaleType);
			    Drawable drawable  = getDrawable(); 
				
			    if (!(drawable instanceof SVGDrawable)) {
			        return;
			    } 
			    setImageDrawable(null); 
			    ((SVGDrawable)drawable).setScaleType(scaleType);
			    int vWidth = getWidth() - getPaddingLeft() - getPaddingRight();
			    int vHeight = getHeight() - getPaddingTop() - getPaddingBottom();
			    ((SVGDrawable)drawable).adjustToParentSize(vWidth, vHeight);
			    setImageDrawable(drawable);
			} 
			
			@Override
			public void setImageDrawable(Drawable drawable) {
				if (!(drawable instanceof SVGDrawable)) {
					super.setImageDrawable(drawable);
					return;
				}
				if (getDrawable() == drawable) {
					return;
				}
				SVGDrawable svg = (SVGDrawable) drawable;
				svg.setScaleType(getScaleType());
				int vWidth = getWidth() - getPaddingLeft() - getPaddingRight();
				int vHeight = getHeight() - getPaddingTop()
						- getPaddingBottom();
				svg.adjustToParentSize(vWidth, vHeight);
				super.setImageDrawable(svg);
			}
		};
		addView(imageView, getImageLayoutParams());
		setEnableScale(true);

		gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener()
		{
			@Override
			public boolean onDown(MotionEvent e)
			{
				if (zoomControls.getVisibility() == View.VISIBLE) {
					super.onDown(e);
					return true;
				} else {
					onClick(me);
					return false;
				}
			}

			@Override
			public boolean onScroll(MotionEvent e1, MotionEvent e2, float dx, float dy)
			{
				boolean retValue = false;
				// Allow scrolling only if the image is zoomed in
				if (zoomControls.getVisibility() == View.VISIBLE && scaleFactor > 1) {
					// check if image scroll beyond its borders
					if (!checkImageScrollBeyondBorders(dx, dy)) {
						changeMatrix.postTranslate(-dx, -dy);
						imageView.setImageMatrix(getViewMatrix());
						requestLayout();
						scheduleControlTimeout();
						retValue = true;
					}
				}
				return retValue;
			}

			@Override
			public boolean onSingleTapConfirmed(MotionEvent e)
			{
				onClick(me);
				return super.onSingleTapConfirmed(e);
			}
		});
		gestureDetector.setIsLongpressEnabled(false);

		zoomControls = new ZoomControls(context);
		addView(zoomControls);
		zoomControls.setVisibility(View.GONE);
		zoomControls.setZoomSpeed(75);
		zoomControls.setOnZoomInClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				handleScaleUp();
			}
		});
		zoomControls.setOnZoomOutClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				handleScaleDown();
			}
		});

		super.setOnClickListener(this);
	}
	
	private ImageView cloneImageView(){
		ImageView newImageView = new ImageView(getContext()) {
			@Override
			protected void onSizeChanged(int w, int h, int oldw, int oldh) {
				Drawable drawable  = imageView.getDrawable(); 
				
			    if (!(drawable instanceof SVGDrawable)) {
			        return;
			    } 
			    imageView.setImageDrawable(null); 
			    int vWidth = w - getPaddingLeft() - getPaddingRight();
			    int vHeight = h - getPaddingTop() - getPaddingBottom();
			    ((SVGDrawable)drawable).adjustToParentSize(vWidth, vHeight);
			    imageView.setImageDrawable(drawable);
			}
			
			@Override
			public void setScaleType (ScaleType scaleType) {
				super.setScaleType(scaleType);
			    Drawable drawable  = getDrawable(); 
				
			    if (!(drawable instanceof SVGDrawable)) {
			        return;
			    } 
			    setImageDrawable(null); 
			    ((SVGDrawable)drawable).setScaleType(scaleType);
			    int vWidth = getWidth() - getPaddingLeft() - getPaddingRight();
			    int vHeight = getHeight() - getPaddingTop() - getPaddingBottom();
			    ((SVGDrawable)drawable).adjustToParentSize(vWidth, vHeight);
			    setImageDrawable(drawable);
			} 
			
			@Override
			public void setImageDrawable(Drawable drawable) {
				if (!(drawable instanceof SVGDrawable)) {
					super.setImageDrawable(drawable);
					return;
				}
				if (getDrawable() == drawable) {
					return;
				}
				SVGDrawable svg = (SVGDrawable) drawable;
				svg.setScaleType(getScaleType());
				int vWidth = getWidth() - getPaddingLeft() - getPaddingRight();
				int vHeight = getHeight() - getPaddingTop()
						- getPaddingBottom();
				svg.adjustToParentSize(vWidth, vHeight);
				super.setImageDrawable(svg);
			}
		};
		if(imageView != null) {
			newImageView.setImageMatrix(imageView.getImageMatrix());
			updateScaleTypeForImageView(newImageView);
		}
		return newImageView;
	}
	
	/**
	 * Constructs a new TiImageView object.
	 * @param context the associated context.
	 * @param proxy the associated proxy.
	 */
	public TiImageView(Context context, TiViewProxy proxy)
	{
		this(context);
		this.proxy = new WeakReference<TiViewProxy>(proxy);
	}

	public void setEnableScale(boolean enableScale)
	{
		this.enableScale = enableScale;
		updateScaleType();
	}

	public void setEnableZoomControls(boolean enableZoomControls)
	{
		this.enableZoomControls = enableZoomControls;
		updateScaleType();
	}

	public Drawable getImageDrawable() {
		return imageView.getDrawable();
	}
	
	/**
	 * Sets a Bitmap as the content of imageView
	 * @param bitmap The bitmap to set. If it is null, it will clear the previous image.
	 */
	public void setImageBitmap(Bitmap bitmap) {
			imageView.setImageBitmap(bitmap);
	}
	
	/**
	 * Sets a Bitmap as the content of imageView
	 * @param bitmap The bitmap to set. If it is null, it will clear the previous image.
	 */
	public void setImageBitmapWithTransition(Bitmap bitmap, Transition transition) {
		if (transition == null) {
			setImageBitmap(bitmap);
		}
		else {
			if (inTransition) {
				queuedTransition = transition;
				queuedBitmap = bitmap;
				return;
			}
			ImageView newImageView = cloneImageView();
			newImageView.setImageBitmap(bitmap);
			transitionToImageView(newImageView, transition);
		}
	}
	
	/**
	 * Sets a Bitmap as the content of imageView
	 * @param bitmap The bitmap to set. If it is null, it will clear the previous image.
	 */
	public void setImageDrawable(Drawable drawable) {
		imageView.setImageDrawable(drawable);
	}
	
	
	private void onTransitionEnd() {
		inTransition = false;
		if (queuedBitmap != null) {
			setImageBitmapWithTransition(queuedBitmap, queuedTransition);
			queuedTransition = null;
			queuedBitmap = null;
		}
		else if (queuedDrawable != null) {
			setImageDrawableWithTransition(queuedDrawable, queuedTransition);
			queuedTransition = null;
			queuedDrawable = null;
		}
	}
	
	private ViewGroup.LayoutParams getImageLayoutParams() {
		ViewGroup.LayoutParams params  = new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		return params;
	}
	
	/**
	 * Sets a Bitmap as the content of imageView
	 * @param bitmap The bitmap to set. If it is null, it will clear the previous image.
	 */
	public void transitionToImageView(ImageView newImageView, Transition transition) {
		inTransition = true;
		oldImageView = imageView;
		imageView = newImageView;
		newImageView.setVisibility(View.GONE);
		
		TiUIHelper.addView(this, newImageView, (oldImageView != null)?oldImageView.getLayoutParams():getImageLayoutParams());
		transition.setTargets(this, newImageView, oldImageView);

		AnimatorSet set = transition.getSet(new AnimatorListener() {
			public void onAnimationEnd(Animator arg0) {	
					removeView(oldImageView);
					oldImageView = null;
					onTransitionEnd();
			}

			public void onAnimationCancel(Animator arg0) {
					removeView(oldImageView);
					oldImageView = null;
					onTransitionEnd();
			}

			public void onAnimationRepeat(Animator arg0) {
			}

			public void onAnimationStart(Animator arg0) {
			}
		});
		set.start();
		newImageView.setVisibility(View.VISIBLE);
	}
	
	/**
	 * Sets a Bitmap as the content of imageView
	 * @param bitmap The bitmap to set. If it is null, it will clear the previous image.
	 */
	public void setImageDrawableWithTransition(Drawable drawable, Transition transition) {
		if (transition == null) {
			setImageDrawable(drawable);
		}
		else {
			if (inTransition) {
				queuedTransition = transition;
				queuedDrawable = drawable;
				return;
			}
			ImageView newImageView = cloneImageView();
			newImageView.setImageDrawable(drawable);
			transitionToImageView(newImageView, transition);
		}
	}

	public void setOnClickListener(OnClickListener clickListener)
	{
		this.clickListener = clickListener;
	}

	public boolean handleMessage(Message msg)
	{
		switch (msg.what) {
			case MSG_HIDE_CONTROLS: {
				handleHideControls();
				return true;
			}
		}
		return false;
	}

	public void onClick(View view)
	{
		boolean sendClick = true;
		if (enableZoomControls) {
			if (zoomControls.getVisibility() != View.VISIBLE) {
				sendClick = false;
				manageControls();
				zoomControls.setVisibility(View.VISIBLE);
			}
			scheduleControlTimeout();
		}
		if (sendClick && clickListener != null) {
			clickListener.onClick(view);
		}
	}

	private void handleScaleUp()
	{
		if (scaleFactor < scaleMax) {
			onViewChanged(scaleIncrement);
		}
	}

	private void handleScaleDown()
	{
		if (scaleFactor > scaleMin) {
			onViewChanged(-scaleIncrement);
		}
	}

	private void handleHideControls()
	{
		zoomControls.setVisibility(View.GONE);
	}

	private void manageControls()
	{
		if (scaleFactor == scaleMax) {
			zoomControls.setIsZoomInEnabled(false);
		} else {
			zoomControls.setIsZoomInEnabled(true);
		}

		if (scaleFactor == scaleMin) {
			zoomControls.setIsZoomOutEnabled(false);
		} else {
			zoomControls.setIsZoomOutEnabled(true);
		}
	}

	private void onViewChanged(float dscale)
	{
		updateChangeMatrix(dscale);
		manageControls();
		requestLayout();
		scheduleControlTimeout();
	}

	private boolean computeBaseMatrix()
	{
		if (imageView.getScaleType() != ScaleType.MATRIX) return false;
		Drawable d = imageView.getDrawable();
		baseMatrix.reset();

		if (d != null) {
			// The base matrix is the matrix that displays the entire image bitmap.
			// It orients the image when orientation is set and scales in X and Y independently, 
			// so that src matches dst exactly.
			// This may change the aspect ratio of the src.
			Rect r = new Rect();
			getDrawingRect(r);
			int intrinsicWidth = d.getIntrinsicWidth();
			int intrinsicHeight = d.getIntrinsicHeight();
			int dwidth = intrinsicWidth;
			int dheight = intrinsicHeight;

			if (orientation > 0) {
				baseMatrix.postRotate(orientation);
				if (orientation == 90 || orientation == 270) {
					dwidth = intrinsicHeight;
					dheight = intrinsicWidth;
				}
			}

			float vwidth = getWidth() - getPaddingLeft() - getPaddingRight();
			float vheight = getHeight() - getPaddingTop() - getPaddingBottom();

			RectF dRectF = null;
			RectF vRectF = new RectF(0, 0, vwidth, vheight);
			if (orientation == 0) {
				dRectF = new RectF(0, 0, dwidth, dheight);
			} else if (orientation == 90) {
				dRectF = new RectF(-dwidth, 0, 0, dheight);
			} else if (orientation == 180) {
				dRectF = new RectF(-dwidth, -dheight, 0, 0);
			} else if (orientation == 270) {
				dRectF = new RectF(0, -dheight, dwidth, 0);
			} else {
				Log.e(TAG, "Invalid value for orientation. Cannot compute the base matrix for the image.");
				return false;
			}

			Matrix m = new Matrix();
			Matrix.ScaleToFit scaleType;
			if (viewWidthDefined && viewHeightDefined) {
				scaleType = Matrix.ScaleToFit.FILL;
			} else {
				scaleType = Matrix.ScaleToFit.CENTER;
			}
			m.setRectToRect(dRectF, vRectF, scaleType);
			baseMatrix.postConcat(m);
		}
		return true;
	}

	private void updateChangeMatrix(float dscale)
	{
		changeMatrix.reset();
		scaleFactor += dscale;
		scaleFactor = Math.max(scaleFactor, scaleMin);
		scaleFactor = Math.min(scaleFactor, scaleMax);
		changeMatrix.postScale(scaleFactor, scaleFactor, getWidth() / 2, getHeight() / 2);
	}

	private Matrix getViewMatrix()
	{
		Matrix m = new Matrix(baseMatrix);
		m.postConcat(changeMatrix);
		return m;
	}

	private void scheduleControlTimeout()
	{
		handler.removeMessages(MSG_HIDE_CONTROLS);
		handler.sendEmptyMessageDelayed(MSG_HIDE_CONTROLS, CONTROL_TIMEOUT);
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev)
	{
		boolean handled = false;
		if (enableZoomControls) {
			if (zoomControls.getVisibility() == View.VISIBLE) {
				zoomControls.onTouchEvent(ev);
			}
			handled = gestureDetector.onTouchEvent(ev);
		}
		if (!handled) {
			handled = super.onTouchEvent(ev);
		}
		return handled;
	}
	
	private float getImageRatio(){
		float ratio = 0;
		Drawable drawable = getImageDrawable();
		if (drawable instanceof TiAnimationDrawable) {
			TiAnimationDrawable animDrawable = (TiAnimationDrawable)drawable;
			if (animDrawable.getNumberOfFrames() >0) {
				drawable = animDrawable.getFrame(0);
			}
		}
		if (drawable instanceof BitmapDrawable) {
			Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
			if (bitmap != null && bitmap.getHeight() > 0)
				return (float)bitmap.getWidth() /  (float)bitmap.getHeight();
		}
		float height = drawable.getIntrinsicHeight();
		if (height > 0) {
			ratio = (float)drawable.getIntrinsicWidth() /  (float)drawable.getIntrinsicHeight(); 
		}
		return ratio;
	}
	

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		int maxWidth = 0;
		int maxHeight = 0;

//		if (DBG) {
		int w = MeasureSpec.getSize(widthMeasureSpec);
		int wm = MeasureSpec.getMode(widthMeasureSpec);
		int h = MeasureSpec.getSize(heightMeasureSpec);
		int hm = MeasureSpec.getMode(heightMeasureSpec);

//			Log.i(TAG, "w: " + w + " wm: " + wm + " h: " + h + " hm: " + hm);
//		}

		// TODO padding and margins

		measureChild(imageView, widthMeasureSpec, heightMeasureSpec);
		int measuredWidth = imageView.getMeasuredWidth();
		int measuredHeight = imageView.getMeasuredHeight();
		
		
		if (measuredWidth > 0 && measuredHeight > 0) {
			if(hm == MeasureSpec.EXACTLY && (wm == MeasureSpec.AT_MOST || wm == MeasureSpec.UNSPECIFIED)) { 
				maxHeight = Math.max(h, Math.max(maxHeight, measuredHeight));
				float ratio =  getImageRatio();
				maxWidth = (int) Math.floor(maxHeight * ratio);
			}
			else if(wm == MeasureSpec.EXACTLY && (hm == MeasureSpec.AT_MOST || hm == MeasureSpec.UNSPECIFIED)) { 
				maxWidth = Math.max(w, Math.max(maxWidth, measuredWidth));
				float ratio =  getImageRatio();
				if (ratio > 0)
					maxHeight = (int) Math.floor(maxWidth / ratio);
			}
			else {
				maxWidth = Math.max(maxWidth, measuredWidth);
				maxHeight = Math.max(maxHeight, measuredHeight);
			}
		}

		
		// Allow for zoom controls.
		if (enableZoomControls) {
			measureChild(zoomControls, widthMeasureSpec, heightMeasureSpec);
			maxWidth = Math.max(maxWidth, zoomControls.getMeasuredWidth());
			maxHeight = Math.max(maxHeight, zoomControls.getMeasuredHeight());
		}

		setMeasuredDimension(resolveSize(maxWidth, widthMeasureSpec), resolveSize(maxHeight, heightMeasureSpec));
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom)
	{
		if (computeBaseMatrix()) imageView.setImageMatrix(getViewMatrix());

		int parentLeft = 0;
		int parentRight = right - left;
		int parentTop = 0;
		int parentBottom = bottom - top;

		imageView.layout(parentLeft, parentTop, parentRight, parentBottom);
		if (oldImageView != null) {
			//make sure the old image view remains in place
			int centerX = (parentRight - parentLeft)/2;
			int centerY = (parentBottom - parentTop)/2;
			int w = oldImageView.getWidth()/2;
			int h = oldImageView.getHeight()/2;
			oldImageView.layout(centerX-w, centerY-h, centerX+w, centerY+h);
		}
		if (enableZoomControls && zoomControls.getVisibility() == View.VISIBLE) {
			int zoomWidth = zoomControls.getMeasuredWidth();
			int zoomHeight = zoomControls.getMeasuredHeight();
			zoomControls.layout(parentRight - zoomWidth, parentBottom - zoomHeight, parentRight, parentBottom);
		}
	}

	public void setColorFilter(ColorFilter filter)
	{
		imageView.setColorFilter(filter);
	}
	
	

	private void updateScaleType()
	{
		if (!configured) return;
		updateScaleTypeForImageView(imageView);
		if (readyToLayout) requestLayout();
	}
	
	private void updateScaleTypeForImageView(ImageView view)
	{
		if (orientation > 0 || enableZoomControls) {
			view.setScaleType(ScaleType.MATRIX);
			view.setAdjustViewBounds(false);
		} else {
			if (viewWidthDefined && viewHeightDefined) {
				view.setAdjustViewBounds(false);
				view.setScaleType(wantedScaleType);
			}
			else if(!enableScale) {
				view.setAdjustViewBounds(false);
				view.setScaleType(ScaleType.CENTER);
			} else {
				view.setAdjustViewBounds(true);
				view.setScaleType(ScaleType.FIT_CENTER);
			}
		}
	}
	
	public void setWantedScaleType(ScaleType type) {
		wantedScaleType = type;
		updateScaleType();
	}
	
	public ScaleType getScaleType() {
		return imageView.getScaleType();
	}
	
	public void setWidthDefined(boolean defined)
	{
		viewWidthDefined = defined;
		updateScaleType();
	}
	
	public boolean getWidthDefined()
	{
		return viewWidthDefined;
	}

	public void setHeightDefined(boolean defined)
	{
		viewHeightDefined = defined;
		updateScaleType();
	}
	
	public boolean getHeightDefined()
	{
		return viewHeightDefined;
	}

	public void setOrientation(int orientation)
	{
		this.orientation = orientation;
		updateScaleType();
	}
	
	private boolean checkImageScrollBeyondBorders(float dx, float dy)
	{
		float[] matrixValues = new float[9];
		Matrix m = new Matrix(changeMatrix);
		// Apply the translation
		m.postTranslate(-dx, -dy);
		m.getValues(matrixValues);
		// Image can move only the extra width or height that is available
		// after scaling from the original width or height
		float scaledAdditionalHeight = imageView.getHeight() * (matrixValues[4] - 1);
		float scaledAdditionalWidth = imageView.getWidth() * (matrixValues[0] - 1);
		if (matrixValues[5] > -scaledAdditionalHeight && matrixValues[5] < 0 && matrixValues[2] > -scaledAdditionalWidth
			&& matrixValues[2] < 0) {
			return false;
		}
		return true;
	}
	
	public void setReadyToLayout(boolean ready)
	{
		readyToLayout = ready;
		if (readyToLayout) requestLayout();
	}
	
	public void setConfigured(boolean configured)
	{
		this.configured = configured;
		
		if (configured)
		{
			updateScaleType();
			readyToLayout = true;
		}
	}
}
