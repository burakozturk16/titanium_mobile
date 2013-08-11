/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package org.appcelerator.titanium.view;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollPropertyChange;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.KrollProxyListener;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.kroll.common.TiMessenger;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiDimension;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiAnimationBuilder;
import org.appcelerator.titanium.util.TiAnimationBuilder.TiMatrixAnimation;
import org.appcelerator.titanium.util.Ti2DMatrixEvaluator;
import org.appcelerator.titanium.util.TiAnimatorListener;
import org.appcelerator.titanium.util.TiAnimatorSet;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiUIHelper;
import org.appcelerator.titanium.view.TiCompositeLayout.LayoutParams;
import org.appcelerator.titanium.TiBlob;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.view.ViewCompat;
import android.text.TextUtils;
import android.util.Pair;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;

/**
 * This class is for Titanium View implementations, that correspond with TiViewProxy. 
 * A TiUIView is responsible for creating and maintaining a native Android View instance.
 */
public abstract class TiUIView
	implements KrollProxyListener, OnFocusChangeListener, Handler.Callback
{

	private static final boolean HONEYCOMB_OR_GREATER = (Build.VERSION.SDK_INT >= 11);
	private static final boolean JELLY_BEAN_OR_GREATER = (Build.VERSION.SDK_INT >= 16);
	private static final int LAYER_TYPE_SOFTWARE = 1;
	private static final String TAG = "TiUIView";

	private static AtomicInteger idGenerator;

	// When distinguishing twofingertap and pinch events, minimum motion (in pixels) 
	// to qualify as a scale event. 
	private static final float SCALE_THRESHOLD = 6.0f;

	public static final int SOFT_KEYBOARD_DEFAULT_ON_FOCUS = 0;
	public static final int SOFT_KEYBOARD_HIDE_ON_FOCUS = 1;
	public static final int SOFT_KEYBOARD_SHOW_ON_FOCUS = 2;
	
	private static final int MSG_SET_BACKGROUND = 100;

	protected View nativeView; // Native View object

	protected TiViewProxy proxy;
	protected TiViewProxy parent;
	protected ArrayList<TiUIView> children = new ArrayList<TiUIView>();

	protected LayoutParams layoutParams;
	protected TiAnimationBuilder animBuilder;
	protected TiBackgroundDrawable background;
	
	protected KrollDict additionalEventData;

	// Since Android doesn't have a property to check to indicate
	// the current animated x/y scale (from a scale animation), we track it here
	// so if another scale animation is done we can gleen the fromX and fromY values
	// rather than starting the next animation always from scale 1.0f (i.e., normal scale).
	// This gives us parity with iPhone for scale animations that use the 2-argument variant
	// of Ti2DMatrix.scale().
	private Pair<Float, Float> animatedScaleValues = Pair.create(Float.valueOf(1f), Float.valueOf(1f)); // default = full size (1f)

	private float animatedAlpha = Float.MIN_VALUE; // i.e., no animated alpha.

	protected KrollDict lastUpEvent = new KrollDict(2);
	protected KrollDict lastDownEvent = new KrollDict(2);

	// In the case of heavy-weight windows, the "nativeView" is null,
	// so this holds a reference to the view which is used for touching,
	// i.e., the view passed to registerForTouch.
	private WeakReference<View> touchView = null;

	private Method mSetLayerTypeMethod = null; // Honeycomb, for turning off hw acceleration.

	private boolean zIndexChanged = false;
	private TiBorderWrapperView borderView;
	// For twofingertap detection
	private boolean didScale = false;

	//to maintain sync visibility between borderview and view. Default is visible
	private int visibility = View.VISIBLE;
	
	protected GestureDetector detector = null;

	protected Handler handler;
	
	/**
	 * Constructs a TiUIView object with the associated proxy.
	 * @param proxy the associated proxy.
	 * @module.api
	 */
	public TiUIView(TiViewProxy proxy)
	{
		if (idGenerator == null) {
			idGenerator = new AtomicInteger(0);
		}
		this.proxy = proxy;
		this.layoutParams = new TiCompositeLayout.LayoutParams();
		handler = new Handler(Looper.getMainLooper(), this);
	}

	/**
	 * Adds a child view into the ViewGroup.
	 * @param child the view to be added.
	 * @param index z-index of the view to be added.
	 */
	public void add(TiUIView child, int index)
	{
		if (child != null) {
			View cv = child.getOuterView();
			if (cv != null) {
				View nv = getParentViewForChild();
				if (nv instanceof ViewGroup) {
					if (index >= 0) {
						if (cv.getParent() == null) {
							((ViewGroup) nv).addView(cv, index, child.getLayoutParams());
						}
						children.add(index, child);
					}
					else {
						if (cv.getParent() == null) {
						((ViewGroup) nv).addView(cv, child.getLayoutParams());
						}
						children.add(child);
					}
					
					child.parent = proxy;
				}
			}
		}
	}

	/**
	 * Adds a child view into the ViewGroup.
	 * @param child the view to be added.
	 */
	public void add(TiUIView child)
	{
		add(child, -1);
	}

	/**
	 * Removes the child view from the ViewGroup, if child exists.
	 * @param child the view to be removed.
	 */
	public void remove(TiUIView child)
	{
		if (child != null) {
			View cv = child.getOuterView();
			if (cv != null) {
				View nv = getParentViewForChild();
				if (nv instanceof ViewGroup) {
					((ViewGroup) nv).removeView(cv);
					children.remove(child);
					child.parent = null;
				}
			}
		}
	}
	
	public void setAdditionalEventData(KrollDict dict) {
		additionalEventData = dict;
	}
	
	public KrollDict getAdditionalEventData() {
		return additionalEventData;
	}

	/**
	 * @return list of views added.
	 */
	public List<TiUIView> getChildren()
	{
		return children;
	}

	/**
	 * @return the view proxy.
	 * @module.api
	 */
	public TiViewProxy getProxy()
	{
		return proxy;
	}

	/**
	 * Sets the view proxy.
	 * @param proxy the proxy to set.
	 * @module.api
	 */
	public void setProxy(TiViewProxy proxy)
	{
		this.proxy = proxy;
	}

	public TiViewProxy getParent()
	{
		return parent;
	}

	public void setParent(TiViewProxy parent)
	{
		this.parent = parent;
	}

	/**
	 * @return the view's layout params.
	 * @module.api
	 */
	public LayoutParams getLayoutParams()
	{
		return layoutParams;
	}
	
	
	
	//This handler callback is tied to the UI thread.
	public boolean handleMessage(Message msg)
	{
		switch(msg.what) {
			case MSG_SET_BACKGROUND : {
				applyCustomBackground();
				return true;
			}
		}
		return false;
	}

	/**
	 * @return the Android native view.
	 * @module.api
	 */
	public View getNativeView()
	{
		return nativeView;
	}

	public View getParentViewForChild()
	{
		return getNativeView();
	}

	/**
	 * Sets the nativeView to view.
	 * @param view the view to set
	 * @module.api
	 */
	protected void setNativeView(View view)
	{
		if (view.getId() == View.NO_ID) {
			view.setId(idGenerator.incrementAndGet());
		}
		
		if (borderView != null)
		{
			borderView.removeView(nativeView);
		}
		
		this.nativeView = view;
		boolean clickable = true;
		
		if (proxy.hasProperty(TiC.PROPERTY_TOUCH_ENABLED)) {
			clickable = TiConvert.toBoolean(proxy.getProperty(TiC.PROPERTY_TOUCH_ENABLED), true);
		}
		doSetClickable(nativeView, clickable);
		nativeView.setOnFocusChangeListener(this);
		
		
		if (background != null)
		{
			background.setNativeView(nativeView);
			if (TiApplication.isUIThread()) {
				applyCustomBackground();
				addBorderView();
			} else {
				handler.sendEmptyMessage(MSG_SET_BACKGROUND);
			}

		}
		
		applyAccessibilityProperties();
	}

	protected void setLayoutParams(LayoutParams layoutParams)
	{
		this.layoutParams = layoutParams;
	}

	public void listenerAdded(String type, int count, KrollProxy proxy) {
	}

	public void listenerRemoved(String type, int count, KrollProxy proxy){
	}

//	private boolean hasBorder(KrollDict d)
//	{
//		return d.containsKeyAndNotNull(TiC.PROPERTY_BORDER_COLOR) 
//			|| d.containsKeyAndNotNull(TiC.PROPERTY_BORDER_RADIUS)
//			|| d.containsKeyAndNotNull(TiC.PROPERTY_BORDER_WIDTH);
//	}

	private double computeAngle(float[] v1, float[] v2)
	{
		return (180.0 / Math.PI * Math.atan2(v1[0] - v2[0], v2[1] - v1[1]));
	}

	public float[] getPreTranslationValue(float[] points)
	{
		View view = getOuterView();
		if (view != null && layoutParams.optionTransform != null) {
			TiMatrixAnimation matrixAnimation = animBuilder.createMatrixAnimation(view, layoutParams.optionTransform);
			int width = view.getWidth();
			int height = view.getHeight();
			Matrix m = matrixAnimation.getFinalMatrix(width, height);
			// Get the translation values
			float[] values = new float[9];
			m.getValues(values);
			points[0] = points[0] - values[2];
			points[1] = points[1] - values[5];
		}
		return points;
	}

	protected void applyTransform(Ti2DMatrix timatrix)
	{
		layoutParams.optionTransform = timatrix;
		if (animBuilder == null) {
			animBuilder = new TiAnimationBuilder();
		}

		View outerView = getOuterView();
		if (outerView != null) {
			if (timatrix != null) {
				TiMatrixAnimation matrixAnimation = animBuilder.createMatrixAnimation(outerView, timatrix);
				matrixAnimation.interpolate = false;
				matrixAnimation.setDuration(0);
				outerView.startAnimation(matrixAnimation);
			} else {
				outerView.clearAnimation();
			}
		}
	}

	public void forceLayoutNativeView(boolean informParent)
	{
		layoutNativeView(informParent);
	}

	protected void layoutNativeView()
	{
		if (!this.proxy.isLayoutStarted()) {
			layoutNativeView(false);
		}
	}
	
	protected void redrawNativeView() {
		if (nativeView != null)
			nativeView.postInvalidate();
	}

	protected void layoutNativeView(boolean informParent)
	{
		if (parent != null) {
			TiUIView uiv = parent.peekView();
			if (uiv != null) {
				View v = uiv.getOuterView();
				if (v.getVisibility() == View.INVISIBLE || v.getVisibility() == View.GONE) {
					//if we have a parent which is hidden, we are hidden, so no need to layout
					return;
				}
			}
		}
		if (nativeView != null) {
			Animation a = nativeView.getAnimation();
			if (a != null && a instanceof TiMatrixAnimation) {
				TiMatrixAnimation matrixAnimation = (TiMatrixAnimation) a;
				matrixAnimation.invalidateWithMatrix(nativeView);
			}
			if (informParent) {				
				if (parent != null) {
					TiUIView uiv = parent.peekView();
					if (uiv != null) {
						View v = uiv.getParentViewForChild();
						if (v instanceof TiCompositeLayout) {
							((TiCompositeLayout) v).resort();
						}
					}
				}
			}
			getParentViewForChild().requestLayout();
		}
	}

	public boolean iszIndexChanged()
	{
		return zIndexChanged;
	}

	public void setzIndexChanged(boolean zIndexChanged)
	{
		this.zIndexChanged = zIndexChanged;
	}

	public void propertyChanged(String key, Object oldValue, Object newValue, KrollProxy proxy)
	{
		if (key.equals(TiC.PROPERTY_LAYOUT)) {
			String layout = TiConvert.toString(newValue);
			if (nativeView instanceof TiCompositeLayout) {
				((TiCompositeLayout)nativeView).setLayoutArrangement(layout);
			}
		} else if (key.equals(TiC.PROPERTY_LEFT)) {
			resetPostAnimationValues();
			if (newValue != null) {
				layoutParams.optionLeft = TiConvert.toTiDimension(TiConvert.toString(newValue), TiDimension.TYPE_LEFT);
			} else {
				layoutParams.optionLeft = null;
			}
			layoutNativeView();
		} else if (key.equals(TiC.PROPERTY_TOP)) {
			resetPostAnimationValues();
			if (newValue != null) {
				layoutParams.optionTop = TiConvert.toTiDimension(TiConvert.toString(newValue), TiDimension.TYPE_TOP);
			} else {
				layoutParams.optionTop = null;
			}
			layoutNativeView();
		} else if (key.equals(TiC.PROPERTY_CENTER)) {
			resetPostAnimationValues();
			TiConvert.updateLayoutCenter(newValue, layoutParams);
			layoutNativeView();
		} else if (key.equals(TiC.PROPERTY_RIGHT)) {
			resetPostAnimationValues();
			if (newValue != null) {
				layoutParams.optionRight = TiConvert.toTiDimension(TiConvert.toString(newValue), TiDimension.TYPE_RIGHT);
			} else {
				layoutParams.optionRight = null;
			}
			layoutNativeView();
		} else if (key.equals(TiC.PROPERTY_BOTTOM)) {
			resetPostAnimationValues();
			if (newValue != null) {
				layoutParams.optionBottom = TiConvert.toTiDimension(TiConvert.toString(newValue), TiDimension.TYPE_BOTTOM);
			} else {
				layoutParams.optionBottom = null;
			}
			layoutNativeView();
		} else if (key.equals(TiC.PROPERTY_SIZE)) {
			if (newValue instanceof HashMap) {
				@SuppressWarnings("unchecked")
				HashMap<String, Object> d = (HashMap<String, Object>) newValue;
				propertyChanged(TiC.PROPERTY_WIDTH, oldValue, d.get(TiC.PROPERTY_WIDTH), proxy);
				propertyChanged(TiC.PROPERTY_HEIGHT, oldValue, d.get(TiC.PROPERTY_HEIGHT), proxy);
			}else if (newValue != null){
				Log.w(TAG, "Unsupported property type ("+(newValue.getClass().getSimpleName())+") for key: " + key+". Must be an object/dictionary");
			}
		} else if (key.equals(TiC.PROPERTY_HEIGHT)) {
			resetPostAnimationValues();
			if (newValue != null) {
				layoutParams.optionHeight = null;
				layoutParams.sizeOrFillHeightEnabled = true;
				if (newValue.equals(TiC.LAYOUT_SIZE)) {
					layoutParams.autoFillsHeight = false;
				} else if (newValue.equals(TiC.LAYOUT_FILL)) {
					layoutParams.autoFillsHeight = true;
				} else if (!newValue.equals(TiC.SIZE_AUTO)) {
					layoutParams.optionHeight = TiConvert.toTiDimension(TiConvert.toString(newValue),
						TiDimension.TYPE_HEIGHT);
					layoutParams.sizeOrFillHeightEnabled = false;
				}
			} else {
				layoutParams.optionHeight = null;
			}
			layoutNativeView();
		} else if (key.equals(TiC.PROPERTY_HORIZONTAL_WRAP)) {
			if (nativeView instanceof TiCompositeLayout) {
				((TiCompositeLayout) nativeView).setEnableHorizontalWrap(TiConvert.toBoolean(newValue,true));
			}
			layoutNativeView();
		} else if (key.equals(TiC.PROPERTY_WIDTH)) {
			resetPostAnimationValues();
			if (newValue != null) {
				layoutParams.optionWidth = null;
				layoutParams.sizeOrFillWidthEnabled = true;
				if (newValue.equals(TiC.LAYOUT_SIZE)) {
					layoutParams.autoFillsWidth = false;
				} else if (newValue.equals(TiC.LAYOUT_FILL)) {
					layoutParams.autoFillsWidth = true;
				} else if (!newValue.equals(TiC.SIZE_AUTO)) {
					layoutParams.optionWidth = TiConvert.toTiDimension(TiConvert.toString(newValue), TiDimension.TYPE_WIDTH);
					layoutParams.sizeOrFillWidthEnabled = false;
				}
			} else {
				layoutParams.optionWidth = null;
			}
			layoutNativeView();
		} else if (key.equals(TiC.PROPERTY_ZINDEX)) {
			if (newValue != null) {
				layoutParams.optionZIndex = TiConvert.toInt(newValue);
			} else {
				layoutParams.optionZIndex = 0;
			}
			if (!this.proxy.isLayoutStarted()) {
				layoutNativeView(true);
			} else {
				setzIndexChanged(true);
			}
		} else if (key.equals(TiC.PROPERTY_FOCUSABLE) && newValue != null) {
			registerForKeyPress(nativeView, TiConvert.toBoolean(newValue, false));
		} else if (key.equals(TiC.PROPERTY_TOUCH_ENABLED)) {
			doSetClickable(TiConvert.toBoolean(newValue));
		} else if (key.equals(TiC.PROPERTY_VISIBLE)) {
			newValue = (newValue == null) ? false : newValue;
			this.setVisibility(TiConvert.toBoolean(newValue) ? View.VISIBLE : View.INVISIBLE);
		} else if (key.equals(TiC.PROPERTY_ENABLED)) {
			nativeView.setEnabled(TiConvert.toBoolean(newValue));
		} else if (key.startsWith(TiC.PROPERTY_BACKGROUND_PADDING)) {
			Log.i(TAG, key + " not yet implemented.");
		} else if (key.equals(TiC.PROPERTY_BACKGROUND_COLOR)) {
				TiBackgroundDrawable bgdDrawable = getOrCreateBackground();
				ColorDrawable colorDrawable = TiUIHelper.buildColorDrawable(TiConvert.toString(newValue));		
				bgdDrawable.setColorDrawableForState(TiUIHelper.BACKGROUND_DEFAULT_STATE_1, colorDrawable);
				bgdDrawable.setColorDrawableForState(TiUIHelper.BACKGROUND_DEFAULT_STATE_2, colorDrawable);
		} else if (key.equals(TiC.PROPERTY_BACKGROUND_SELECTED_COLOR)) {
			ColorDrawable colorDrawable = TiUIHelper.buildColorDrawable(TiConvert.toString(newValue));		
			getOrCreateBackground().setColorDrawableForState(TiUIHelper.BACKGROUND_SELECTED_STATE, colorDrawable);
		} else if (key.equals(TiC.PROPERTY_BACKGROUND_FOCUSED_COLOR)) {
			ColorDrawable colorDrawable = TiUIHelper.buildColorDrawable(TiConvert.toString(newValue));		
			getOrCreateBackground().setColorDrawableForState(TiUIHelper.BACKGROUND_FOCUSED_STATE, colorDrawable);
		} else if (key.equals(TiC.PROPERTY_BACKGROUND_DISABLED_COLOR)) {
			ColorDrawable colorDrawable = TiUIHelper.buildColorDrawable(TiConvert.toString(newValue));		
			getOrCreateBackground().setColorDrawableForState(TiUIHelper.BACKGROUND_DISABLED_STATE, colorDrawable);
		} else if (key.equals(TiC.PROPERTY_BACKGROUND_IMAGE)) {
			boolean repeat = proxy.getProperties().optBoolean(TiC.PROPERTY_BACKGROUND_REPEAT, false);
			setBackgroundImageDrawable(newValue, repeat, new int[][]{TiUIHelper.BACKGROUND_DEFAULT_STATE_1, TiUIHelper.BACKGROUND_DEFAULT_STATE_2});
		} else if (key.equals(TiC.PROPERTY_BACKGROUND_SELECTED_IMAGE)) {
			boolean repeat = proxy.getProperties().optBoolean(TiC.PROPERTY_BACKGROUND_REPEAT, false);
			setBackgroundImageDrawable(newValue, repeat, new int[][]{TiUIHelper.BACKGROUND_SELECTED_STATE});
		} else if (key.equals(TiC.PROPERTY_BACKGROUND_FOCUSED_IMAGE)) {
			boolean repeat = proxy.getProperties().optBoolean(TiC.PROPERTY_BACKGROUND_REPEAT, false);
			setBackgroundImageDrawable(newValue, repeat, new int[][]{TiUIHelper.BACKGROUND_FOCUSED_STATE});
		} else if (key.equals(TiC.PROPERTY_BACKGROUND_DISABLED_IMAGE)) {
			boolean repeat = proxy.getProperties().optBoolean(TiC.PROPERTY_BACKGROUND_REPEAT, false);
			setBackgroundImageDrawable(newValue, repeat, new int[][]{TiUIHelper.BACKGROUND_DISABLED_STATE});
		} else if (key.equals(TiC.PROPERTY_BACKGROUND_SELECTED_GRADIENT)) {
			Drawable drawable =  TiUIHelper.buildGradientDrawable(nativeView, (KrollDict)newValue);
			getOrCreateBackground().setGradientDrawableForState(TiUIHelper.BACKGROUND_SELECTED_STATE, drawable);
		} else if (key.equals(TiC.PROPERTY_BACKGROUND_FOCUSED_GRADIENT)) {
			Drawable drawable =  TiUIHelper.buildGradientDrawable(nativeView, (KrollDict)newValue);
			getOrCreateBackground().setGradientDrawableForState(TiUIHelper.BACKGROUND_FOCUSED_STATE, drawable);
		} else if (key.equals(TiC.PROPERTY_BACKGROUND_DISABLED_GRADIENT)) {
			Drawable drawable =  TiUIHelper.buildGradientDrawable(nativeView, (KrollDict)newValue);
			getOrCreateBackground().setGradientDrawableForState(TiUIHelper.BACKGROUND_DISABLED_STATE, drawable);
		} else if (key.equals(TiC.PROPERTY_BACKGROUND_GRADIENT)) {
			TiBackgroundDrawable bgdDrawable = getOrCreateBackground();
			Drawable drawable =  TiUIHelper.buildGradientDrawable(nativeView, (KrollDict)newValue);
			bgdDrawable.setGradientDrawableForState(TiUIHelper.BACKGROUND_DEFAULT_STATE_1, drawable);
			bgdDrawable.setGradientDrawableForState(TiUIHelper.BACKGROUND_DEFAULT_STATE_2, drawable);
		} else if (key.equals(TiC.PROPERTY_BACKGROUND_REPEAT)) {
			if (background != null)
				background.setImageRepeat(TiConvert.toBoolean(newValue));
		} else if (key.equals(TiC.PROPERTY_BACKGROUND_OPACITY)) {
			if (background != null)
				TiUIHelper.setDrawableOpacity(background, TiConvert.toFloat(newValue, 1f));
		} else if (key.equals(TiC.PROPERTY_BORDER_COLOR)) {
			setBorderColor(TiConvert.toString(newValue));
		} else if (key.equals(TiC.PROPERTY_BORDER_RADIUS)) {
			setBorderRadius(TiConvert.toFloat(newValue, 0f));
		} else if (key.equals(TiC.PROPERTY_BORDER_WIDTH)) {
			setBorderWidth(TiConvert.toFloat(newValue, 0f));
		} else if (key.equals(TiC.PROPERTY_OPACITY)) {
			setOpacity(TiConvert.toFloat(newValue, 1f));
		} else if (key.equals(TiC.PROPERTY_SOFT_KEYBOARD_ON_FOCUS)) {
			Log.w(TAG, "Focus state changed to " + TiConvert.toString(newValue) + " not honored until next focus event.",
				Log.DEBUG_MODE);
		} else if (key.equals(TiC.PROPERTY_TRANSFORM)) {
			applyTransform((Ti2DMatrix)newValue);
		} else if (key.equals(TiC.PROPERTY_KEEP_SCREEN_ON)) {
			if (nativeView != null) {
				nativeView.setKeepScreenOn(TiConvert.toBoolean(newValue));
			}

		} else if (key.indexOf("accessibility") == 0 && !key.equals(TiC.PROPERTY_ACCESSIBILITY_HIDDEN)) {
			applyContentDescription();

		} else if (key.equals(TiC.PROPERTY_ACCESSIBILITY_HIDDEN)) {
			applyAccessibilityHidden(newValue);

		} else if (key.equals(TiC.PROPERTY_TOUCH_PASSTHROUGH)) {
			if (nativeView instanceof TiCompositeLayout) {
				((TiCompositeLayout) nativeView).setTouchPassThrough(TiConvert.toBoolean(newValue));
			}
		} else if (key.equals(TiC.PROPERTY_CLIP_CHILDREN)) {
//			if (nativeView instanceof TiCompositeLayout) {
//				((TiCompositeLayout) nativeView).setClipToPadding(TiConvert.toBoolean(newValue));
//			}
		} else if (Log.isDebugModeEnabled()) {
			Log.d(TAG, "Unhandled property key: " + key, Log.DEBUG_MODE);
		}
	}
	
	private void setBackgroundImageDrawable(Object object, boolean backgroundRepeat, int[][] states) {
		TiBackgroundDrawable bgdDrawable = getOrCreateBackground();
		Drawable drawable = null;
		if (object instanceof TiBlob) {
			drawable = TiUIHelper.buildImageDrawable(nativeView.getContext(), ((TiBlob)object).getImage(), backgroundRepeat, proxy);
		}
		else {
			drawable = TiUIHelper.buildImageDrawable(TiConvert.toString(object), backgroundRepeat, proxy);
		}
		for (int i = 0; i < states.length; i++) {
			bgdDrawable.setImageDrawableForState(states[i], drawable);			
		}
	}

	public void processProperties(KrollDict d)
	{
		boolean nativeViewNull = false;
		View viewForLayout = getParentViewForChild();
		
		if (nativeView == null) {
			nativeViewNull = true;
			Log.d(TAG, "Nativeview is null", Log.DEBUG_MODE);
		}
		
		if (viewForLayout instanceof TiCompositeLayout) {
			TiCompositeLayout tiLayout = (TiCompositeLayout)viewForLayout;
			if (d.containsKey(TiC.PROPERTY_LAYOUT)) {
				String layout = TiConvert.toString(d, TiC.PROPERTY_LAYOUT);
				tiLayout.setLayoutArrangement(layout);
			}
			

			if (d.containsKey(TiC.PROPERTY_HORIZONTAL_WRAP)) {
				tiLayout.setEnableHorizontalWrap(TiConvert.toBoolean(d,TiC.PROPERTY_HORIZONTAL_WRAP,true));
			}

			if (d.containsKey(TiC.PROPERTY_TOUCH_PASSTHROUGH)) {
				tiLayout.setTouchPassThrough(TiConvert.toBoolean(d, TiC.PROPERTY_TOUCH_PASSTHROUGH));
			}
			if (d.containsKey(TiC.PROPERTY_CLIP_CHILDREN)) {
//				if (nativeView instanceof ViewGroup) {
//					((TiCompositeLayout) nativeView).setClipToPadding(TiConvert.toBoolean(d, TiC.PROPERTY_CLIP_CHILDREN));				((ViewGroup) nativeView).setClipToPadding(false);
//				}
			}
		}
		
		if (TiConvert.fillLayout(d, layoutParams) && viewForLayout != null) {
			viewForLayout.requestLayout();
		}
		
		
		boolean backgroundRepeat = d.optBoolean(TiC.PROPERTY_BACKGROUND_REPEAT, false);
		
		if (d.containsKey(TiC.PROPERTY_OPACITY)) {
			setOpacity(TiConvert.toFloat(d, TiC.PROPERTY_OPACITY, 1f));
		}
		
		if (d.containsKey(TiC.PROPERTY_BACKGROUND_COLOR)) {
			TiBackgroundDrawable bgdDrawable = getOrCreateBackground();
			ColorDrawable colorDrawable = TiUIHelper.buildColorDrawable(TiConvert.toString(d, TiC.PROPERTY_BACKGROUND_COLOR));		
			bgdDrawable.setColorDrawableForState(TiUIHelper.BACKGROUND_DEFAULT_STATE_1, colorDrawable);
			bgdDrawable.setColorDrawableForState(TiUIHelper.BACKGROUND_DEFAULT_STATE_2, colorDrawable);
		}
		
		if (d.containsKey(TiC.PROPERTY_BACKGROUND_SELECTED_COLOR)) {
			ColorDrawable colorDrawable = TiUIHelper.buildColorDrawable(TiConvert.toString(d, TiC.PROPERTY_BACKGROUND_SELECTED_COLOR));		
			getOrCreateBackground().setColorDrawableForState(TiUIHelper.BACKGROUND_SELECTED_STATE, colorDrawable);
		}
		if (d.containsKey(TiC.PROPERTY_BACKGROUND_FOCUSED_COLOR)) {
			ColorDrawable colorDrawable = TiUIHelper.buildColorDrawable(TiConvert.toString(d, TiC.PROPERTY_BACKGROUND_FOCUSED_COLOR));		
			getOrCreateBackground().setColorDrawableForState(TiUIHelper.BACKGROUND_FOCUSED_STATE, colorDrawable);
		} 
		if (d.containsKey(TiC.PROPERTY_BACKGROUND_DISABLED_COLOR)) {
			ColorDrawable colorDrawable = TiUIHelper.buildColorDrawable(TiConvert.toString(d, TiC.PROPERTY_BACKGROUND_DISABLED_COLOR));		
			getOrCreateBackground().setColorDrawableForState(TiUIHelper.BACKGROUND_DISABLED_STATE, colorDrawable);
		}
		if (d.containsKey(TiC.PROPERTY_BACKGROUND_IMAGE)) {
			setBackgroundImageDrawable(d.get(TiC.PROPERTY_BACKGROUND_IMAGE), backgroundRepeat, new int[][]{TiUIHelper.BACKGROUND_DEFAULT_STATE_1, TiUIHelper.BACKGROUND_DEFAULT_STATE_2});
		}
		if (d.containsKey(TiC.PROPERTY_BACKGROUND_SELECTED_IMAGE)) {
			setBackgroundImageDrawable(d.get(TiC.PROPERTY_BACKGROUND_SELECTED_IMAGE), backgroundRepeat, new int[][]{TiUIHelper.BACKGROUND_SELECTED_STATE});
		}
		if (d.containsKey(TiC.PROPERTY_BACKGROUND_FOCUSED_IMAGE)) {
			setBackgroundImageDrawable(d.get(TiC.PROPERTY_BACKGROUND_FOCUSED_IMAGE), backgroundRepeat, new int[][]{TiUIHelper.BACKGROUND_FOCUSED_STATE});
		}
		if (d.containsKey(TiC.PROPERTY_BACKGROUND_DISABLED_IMAGE)) {
			setBackgroundImageDrawable(d.get(TiC.PROPERTY_BACKGROUND_DISABLED_IMAGE), backgroundRepeat, new int[][]{TiUIHelper.BACKGROUND_DISABLED_STATE});
		}
		if (d.containsKey(TiC.PROPERTY_BACKGROUND_SELECTED_GRADIENT)) {
			Drawable drawable =  TiUIHelper.buildGradientDrawable(nativeView, d.getKrollDict(TiC.PROPERTY_BACKGROUND_SELECTED_GRADIENT));
			getOrCreateBackground().setGradientDrawableForState(TiUIHelper.BACKGROUND_SELECTED_STATE, drawable);
		}
		if (d.containsKey(TiC.PROPERTY_BACKGROUND_FOCUSED_GRADIENT)) {
			Drawable drawable =  TiUIHelper.buildGradientDrawable(nativeView, d.getKrollDict(TiC.PROPERTY_BACKGROUND_FOCUSED_GRADIENT));
			getOrCreateBackground().setGradientDrawableForState(TiUIHelper.BACKGROUND_FOCUSED_STATE, drawable);
		}
		if (d.containsKey(TiC.PROPERTY_BACKGROUND_DISABLED_GRADIENT)) {
			Drawable drawable =  TiUIHelper.buildGradientDrawable(nativeView, d.getKrollDict(TiC.PROPERTY_BACKGROUND_DISABLED_GRADIENT));
			getOrCreateBackground().setGradientDrawableForState(TiUIHelper.BACKGROUND_DISABLED_STATE, drawable);
		} 
		if (d.containsKey(TiC.PROPERTY_BACKGROUND_GRADIENT)) {
			TiBackgroundDrawable bgdDrawable = getOrCreateBackground();
			Drawable drawable =  TiUIHelper.buildGradientDrawable(nativeView, d.getKrollDict(TiC.PROPERTY_BACKGROUND_GRADIENT));
//			bgdDrawable.setGradientDrawableForState(TiUIHelper.BACKGROUND_DEFAULT_STATE, drawable);
			bgdDrawable.setGradientDrawableForState(TiUIHelper.BACKGROUND_DEFAULT_STATE_1, drawable);
			bgdDrawable.setGradientDrawableForState(TiUIHelper.BACKGROUND_DEFAULT_STATE_2, drawable);
		}

		//no need to have it here, will be set when necessary
		// if (d.containsKey(TiC.PROPERTY_BACKGROUND_OPACITY)) {
		// 	if(background != null)
		// 		TiUIHelper.setDrawableOpacity(background, TiConvert.toFloat(d, TiC.PROPERTY_BACKGROUND_OPACITY, 1f));
		// } 
		
		if (d.containsKey(TiC.PROPERTY_OPACITY)) {
			setOpacity(TiConvert.toFloat(d, TiC.PROPERTY_OPACITY, 1f));
		}

		if (d.containsKey(TiC.PROPERTY_BORDER_COLOR)) {
			setBorderColor(TiConvert.toString(d, TiC.PROPERTY_BORDER_COLOR));
		}
		if (d.containsKey(TiC.PROPERTY_BORDER_RADIUS)) {
			setBorderRadius(TiConvert.toFloat(d, TiC.PROPERTY_BORDER_RADIUS, 0f));
		}
		if (d.containsKey(TiC.PROPERTY_BORDER_WIDTH)) {
			setBorderWidth(TiConvert.toFloat(d, TiC.PROPERTY_BORDER_WIDTH, 0f));
		} 

		if (d.containsKey(TiC.PROPERTY_VISIBLE) && !nativeViewNull) {
			this.setVisibility(TiConvert.toBoolean(d, TiC.PROPERTY_VISIBLE, true) ? View.VISIBLE : View.INVISIBLE);
		}
		if (d.containsKey(TiC.PROPERTY_ENABLED) && !nativeViewNull) {
			nativeView.setEnabled(TiConvert.toBoolean(d, TiC.PROPERTY_ENABLED, true));
		}

		if (d.containsKey(TiC.PROPERTY_TRANSFORM)) {
			Ti2DMatrix matrix = (Ti2DMatrix) d.get(TiC.PROPERTY_TRANSFORM);
			applyTransform(matrix);
		}
		
		if (d.containsKey(TiC.PROPERTY_KEEP_SCREEN_ON) && !nativeViewNull) {
			nativeView.setKeepScreenOn(TiConvert.toBoolean(d, TiC.PROPERTY_KEEP_SCREEN_ON, false));
			
		}

		if (d.containsKey(TiC.PROPERTY_ACCESSIBILITY_HINT) || d.containsKey(TiC.PROPERTY_ACCESSIBILITY_LABEL)
				|| d.containsKey(TiC.PROPERTY_ACCESSIBILITY_VALUE) || d.containsKey(TiC.PROPERTY_ACCESSIBILITY_HIDDEN)) {
			applyAccessibilityProperties();
		}
	}

	// TODO dead code? @Override
	public void propertiesChanged(List<KrollPropertyChange> changes, KrollProxy proxy)
	{
		for (KrollPropertyChange change : changes) {
			propertyChanged(change.getName(), change.getOldValue(), change.getNewValue(), proxy);
		}
	}

	public void onFocusChange(final View v, boolean hasFocus)
	{
		if (hasFocus) {
			TiMessenger.postOnMain(new Runnable() {
				public void run() {
					TiUIHelper.requestSoftInputChange(proxy, v);
				}
			});
			fireEvent(TiC.EVENT_FOCUS, getFocusEventObject(hasFocus));
		} else {
			fireEvent(TiC.EVENT_BLUR, getFocusEventObject(hasFocus));
		}
	}

	protected KrollDict getFocusEventObject(boolean hasFocus)
	{
		return null;
	}

	protected InputMethodManager getIMM()
	{
		InputMethodManager imm = null;
		imm = (InputMethodManager) TiApplication.getInstance().getSystemService(Context.INPUT_METHOD_SERVICE);
		return imm;
	}

	/**
	 * Focuses the view.
	 */
	public void focus()
	{
		if (nativeView != null) {
			nativeView.requestFocus();
		}
	}

	public boolean hasFocus()
	{
		if (nativeView != null) {
			return nativeView.hasFocus();
		}
		return false;
	}

	/**
	 * Blurs the view.
	 */
	public void blur()
	{
		if (nativeView != null && nativeView.hasFocus()) {
			nativeView.clearFocus();
			TiMessenger.postOnMain(new Runnable() {
				public void run() {
					TiUIHelper.showSoftKeyboard(nativeView, false);
				}
			});
		}
	}

	public void release()
	{
		if (Log.isDebugModeEnabled()) {
			Log.d(TAG, "Releasing: " + this, Log.DEBUG_MODE);
		}
		proxy.cancelAllAnimations();
		View nv = getNativeView();
		if (nv != null) {
			if (nv instanceof ViewGroup) {
				ViewGroup vg = (ViewGroup) nv;
				if (Log.isDebugModeEnabled()) {
					Log.d(TAG, "Group has: " + vg.getChildCount(), Log.DEBUG_MODE);
				}
				if (!(vg instanceof AdapterView<?>)) {
					vg.removeAllViews();
				}
			}
			Drawable d = nv.getBackground();
			if (d != null) {
				setBackgroundDrawable(nv, null);
				d.setCallback(null);
				if (d instanceof TiBackgroundDrawable) {
					((TiBackgroundDrawable)d).releaseDelegate();
				}
				d = null;
			}
			nativeView = null;
			borderView = null;
			if (proxy != null) {
				proxy.setModelListener(null);
			}
		}
	}

	public void setVisibility(int visibility)
	{
		if (this.visibility != visibility)
			forceLayoutNativeView(true);
		this.visibility = visibility;
		View view = getOuterView();
		proxy.setProperty(TiC.PROPERTY_VISIBLE, (visibility == View.VISIBLE));
		if (view != null) {
			view.clearAnimation();
			view.setVisibility(this.visibility);
		}
	}

	/**
	 * Shows the view, changing the view's visibility to View.VISIBLE.
	 */
	public void show()
	{
		this.setVisibility(View.VISIBLE);
		if (getOuterView() == null) {
			Log.w(TAG, "Attempt to show null native control", Log.DEBUG_MODE);
		}
	}

	/**
	 * Hides the view, changing the view's visibility to View.INVISIBLE.
	 */
	public void hide()
	{
		this.setVisibility(View.INVISIBLE);
		if (getOuterView() == null) {
			Log.w(TAG, "Attempt to hide null native control", Log.DEBUG_MODE);
		}
	}

	protected TiBackgroundDrawable getOrCreateBackground()
	{
		if (background == null)
		{
			applyCustomBackground();
		}
		return background;
	}
	
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private void setBackgroundDrawable(View view, Drawable drawable) {
		if(JELLY_BEAN_OR_GREATER) {
			view.setBackground(drawable);
		} else {
			view.setBackgroundDrawable(drawable);
		}
	}

	protected void applyCustomBackground()
	{
		if (background == null) {
			background = new TiBackgroundDrawable();
				float alpha = 1.0f;
			if (!HONEYCOMB_OR_GREATER) {
				if (proxy.hasProperty(TiC.PROPERTY_OPACITY))
					alpha *= TiConvert.toFloat(proxy.getProperty(TiC.PROPERTY_OPACITY));
			}
			if (proxy.hasProperty(TiC.PROPERTY_BACKGROUND_OPACITY))
				alpha *= TiConvert.toFloat(proxy.getProperty(TiC.PROPERTY_BACKGROUND_OPACITY));
			
			if (alpha < 255)
				background.setAlpha(Math.round(alpha * 255));
			if (proxy.hasProperty(TiC.PROPERTY_BACKGROUND_REPEAT))
				background.setImageRepeat(TiConvert.toBoolean(proxy.getProperty(TiC.PROPERTY_BACKGROUND_REPEAT)));
		}
		if (nativeView != null) {
			Drawable currentDrawable = nativeView.getBackground();
			if (currentDrawable != null) {
				currentDrawable.setCallback(null);
				if (currentDrawable instanceof TiBackgroundDrawable) {
					((TiBackgroundDrawable) currentDrawable).releaseDelegate();
				}
				setBackgroundDrawable(nativeView, null);
			}
			setBackgroundDrawable(nativeView, background);
		}
	}
	
	private void addBorderView(){
		// Create new layout params for the child view since we just want the
		// wrapper to control the layout
		LayoutParams params = new LayoutParams();
		params.height = android.widget.FrameLayout.LayoutParams.MATCH_PARENT;
		params.width = android.widget.FrameLayout.LayoutParams.MATCH_PARENT;
		// If the view already has a parent, we need to detach it from the parent
		// and add the borderView to the parent as the child
		ViewGroup savedParent = null;
		android.view.ViewGroup.LayoutParams savedLayoutParams = null;
		if (nativeView.getParent() != null) {
			ViewParent nativeParent = getOuterView().getParent();
			if (nativeParent instanceof ViewGroup) {
				savedParent = (ViewGroup) nativeParent;
				savedLayoutParams = savedParent.getLayoutParams();
				savedParent.removeView(nativeView);
			}
		}
		borderView.addView(nativeView, params);
		if (savedParent != null) {
			savedParent.addView(getOuterView(), savedLayoutParams);
		}
		
		if (borderView.getRadius() > 0f && HONEYCOMB_OR_GREATER) {
			disableHWAcceleration();
		}
	}
	
	private TiBorderWrapperView getOrCreateBorderView()
	{
		if (borderView == null) {
			Activity currentActivity = proxy.getActivity();
			if (currentActivity == null) {
				currentActivity = TiApplication.getAppCurrentActivity();
			}
			borderView = new TiBorderWrapperView(currentActivity);
			
			borderView.setVisibility(this.visibility);
			if (proxy.hasProperty(TiC.PROPERTY_OPACITY))
				borderView.setBorderAlpha(Math.round(TiConvert.toFloat(proxy.getProperty(TiC.PROPERTY_OPACITY)) * 255));
			
			
			if (proxy.hasProperty(TiC.PROPERTY_BACKGROUND_COLOR))
				borderView.setColor(TiConvert.toColor(TiConvert.toString(proxy.getProperty(TiC.PROPERTY_BACKGROUND_COLOR))));
			
			addBorderView();
		}
		return borderView;
	}
	
	private void setBorderColor(String color){
		if (color == null) return;
		getOrCreateBorderView().setColor(TiConvert.toColor(color));
		borderView.invalidate();
	}
	
	private void setBorderRadius(float radius){
		float realRadius = (new TiDimension(Float.toString(radius), TiDimension.TYPE_WIDTH)).getAsPixels(nativeView);
		getOrCreateBorderView().setRadius(realRadius);
		if (radius > 0f && HONEYCOMB_OR_GREATER) {
			disableHWAcceleration();
		}
		borderView.postInvalidate();
	}
	
	private void setBorderWidth(float width){
		float realWidth = (new TiDimension(Float.toString(width), TiDimension.TYPE_WIDTH)).getAsPixels(nativeView);
		getOrCreateBorderView().setBorderWidth(width);
		borderView.invalidate();
	}

	protected static SparseArray<String> motionEvents = new SparseArray<String>();
	static
	{
		motionEvents.put(MotionEvent.ACTION_DOWN, TiC.EVENT_TOUCH_START);
		motionEvents.put(MotionEvent.ACTION_UP, TiC.EVENT_TOUCH_END);
		motionEvents.put(MotionEvent.ACTION_MOVE, TiC.EVENT_TOUCH_MOVE);
		motionEvents.put(MotionEvent.ACTION_CANCEL, TiC.EVENT_TOUCH_CANCEL);
	}

	protected KrollDict dictFromEvent(MotionEvent e)
	{
		KrollDict data = new KrollDict();
		data.put(TiC.EVENT_PROPERTY_X, (double)e.getX());
		data.put(TiC.EVENT_PROPERTY_Y, (double)e.getY());
		KrollDict globalPoint = new KrollDict();
		globalPoint.put(TiC.EVENT_PROPERTY_X, (double)e.getRawX());
		globalPoint.put(TiC.EVENT_PROPERTY_Y, (double)e.getRawY());
		data.put(TiC.EVENT_PROPERTY_GLOBALPOINT, globalPoint);
		data.put(TiC.EVENT_PROPERTY_SOURCE, proxy);
		return data;
	}

	protected KrollDict dictFromEvent(KrollDict dictToCopy){
		KrollDict data = new KrollDict();
		if (dictToCopy.containsKey(TiC.EVENT_PROPERTY_X)){
			data.put(TiC.EVENT_PROPERTY_X, dictToCopy.get(TiC.EVENT_PROPERTY_X));
		} else {
			data.put(TiC.EVENT_PROPERTY_X, (double)0);
		}
		if (dictToCopy.containsKey(TiC.EVENT_PROPERTY_Y)){
			data.put(TiC.EVENT_PROPERTY_Y, dictToCopy.get(TiC.EVENT_PROPERTY_Y));
		} else {
			data.put(TiC.EVENT_PROPERTY_Y, (double)0);
		}
		data.put(TiC.EVENT_PROPERTY_SOURCE, proxy);
		return data;
	}

	protected boolean allowRegisterForTouch()
	{
		return true;
	}

	/**
	 * @module.api
	 */
	protected boolean allowRegisterForKeyPress()
	{
		return true;
	}

	public View getOuterView()
	{
		return borderView == null ? getNativeView() : borderView;
	}

	public void registerForTouch()
	{
		if (allowRegisterForTouch()) {
			registerForTouch(getNativeView());
		}
	}

	protected void registerTouchEvents(final View touchable)
	{

		touchView = new WeakReference<View>(touchable);

		final ScaleGestureDetector scaleDetector = new ScaleGestureDetector(touchable.getContext(),
			new SimpleOnScaleGestureListener()
			{
				// protect from divide by zero errors
				long minTimeDelta = 1;
				float minStartSpan = 1.0f;
				float startSpan;

				@Override
				public boolean onScale(ScaleGestureDetector sgd)
				{
					if (proxy.hierarchyHasListener(TiC.EVENT_PINCH)) {
						float timeDelta = sgd.getTimeDelta() == 0 ? minTimeDelta : sgd.getTimeDelta();

						// Suppress scale events (and allow for possible two-finger tap events)
						// until we've moved at least a few pixels. Without this check, two-finger 
						// taps are very hard to register on some older devices.
						if (!didScale) {
							if (Math.abs(sgd.getCurrentSpan() - startSpan) > SCALE_THRESHOLD) {
								didScale = true;
							} 
						}

						if (didScale) {
							KrollDict data = new KrollDict();
							data.put(TiC.EVENT_PROPERTY_SCALE, sgd.getCurrentSpan() / startSpan);
							data.put(TiC.EVENT_PROPERTY_VELOCITY, (sgd.getScaleFactor() - 1.0f) / timeDelta * 1000);
							data.put(TiC.EVENT_PROPERTY_SOURCE, proxy);
	
							return fireEvent(TiC.EVENT_PINCH, data);
						}
					}
					return false;
				}

				@Override
				public boolean onScaleBegin(ScaleGestureDetector sgd)
				{
					startSpan = sgd.getCurrentSpan() == 0 ? minStartSpan : sgd.getCurrentSpan();
					return true;
				}
			});

		detector = new GestureDetector(touchable.getContext(), new SimpleOnGestureListener()
		{
			@Override
			public boolean onDoubleTap(MotionEvent e)
			{
				if (proxy.hierarchyHasListener(TiC.EVENT_DOUBLE_TAP) || proxy.hierarchyHasListener(TiC.EVENT_DOUBLE_CLICK)) {
					boolean handledTap = fireEvent(TiC.EVENT_DOUBLE_TAP, dictFromEvent(e));
					boolean handledClick = fireEvent(TiC.EVENT_DOUBLE_CLICK, dictFromEvent(e));
					return handledTap || handledClick;
				}
				return false;
			}

			@Override
			public boolean onSingleTapConfirmed(MotionEvent e)
			{
				Log.d(TAG, "TAP, TAP, TAP on " + proxy, Log.DEBUG_MODE);
				if (proxy.hierarchyHasListener(TiC.EVENT_SINGLE_TAP)) {
					return fireEvent(TiC.EVENT_SINGLE_TAP, dictFromEvent(e));
					// Moved click handling to the onTouch listener, because a single tap is not the
					// same as a click. A single tap is a quick tap only, whereas clicks can be held
					// before lifting.
					// boolean handledClick = proxy.fireEvent(TiC.EVENT_CLICK, dictFromEvent(event));
					// Note: this return value is irrelevant in our case. We "want" to use it
					// in onTouch below, when we call detector.onTouchEvent(event); But, in fact,
					// onSingleTapConfirmed is *not* called in the course of onTouchEvent. It's
					// called via Handler in GestureDetector. <-- See its Java source.
					// return handledTap;// || handledClick;
				}
				return false;
			}

			@Override
			public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
			{
				Log.d(TAG, "SWIPE on " + proxy, Log.DEBUG_MODE);
				if (proxy.hierarchyHasListener(TiC.EVENT_SWIPE)) {
					KrollDict data = dictFromEvent(e2);
					if (Math.abs(velocityX) > Math.abs(velocityY)) {
						data.put(TiC.EVENT_PROPERTY_DIRECTION, velocityX > 0 ? "right" : "left");
					} else {
						data.put(TiC.EVENT_PROPERTY_DIRECTION, velocityY > 0 ? "down" : "up");
					}
					return fireEvent(TiC.EVENT_SWIPE, data);
				}
				return false;
			}

			@Override
			public void onLongPress(MotionEvent e)
			{
				Log.d(TAG, "LONGPRESS on " + proxy, Log.DEBUG_MODE);

				if (proxy.hierarchyHasListener(TiC.EVENT_LONGPRESS)) {
					fireEvent(TiC.EVENT_LONGPRESS, dictFromEvent(e));
				}
			}
		});
		
		touchable.setOnTouchListener(new OnTouchListener()
		{
			int pointersDown = 0;

			public boolean onTouch(View view, MotionEvent event)
			{
				if (event.getAction() == MotionEvent.ACTION_UP) {
					lastUpEvent.put(TiC.EVENT_PROPERTY_X, (double) event.getX());
					lastUpEvent.put(TiC.EVENT_PROPERTY_Y, (double) event.getY());
				}

				if (event.getAction() == MotionEvent.ACTION_DOWN ) {
					lastDownEvent.put(TiC.EVENT_PROPERTY_X, (double) event.getX());
					lastDownEvent.put(TiC.EVENT_PROPERTY_Y, (double) event.getY());
				}

				scaleDetector.onTouchEvent(event);
				if (scaleDetector.isInProgress()) {
					pointersDown = 0;
					return true;
				}

				boolean handled = detector.onTouchEvent(event);
				if (handled) {
					pointersDown = 0;
					return true;
				}

				if (event.getActionMasked() == MotionEvent.ACTION_POINTER_UP) {
					if (didScale) {
						didScale = false;
						pointersDown = 0;
					} else {
						pointersDown++;
					}
				} else if (event.getAction() == MotionEvent.ACTION_UP) {
					if (pointersDown == 1) {
						fireEvent(TiC.EVENT_TWOFINGERTAP, dictFromEvent(event));
						pointersDown = 0;
						return true;
					}
					pointersDown = 0;
				}


				handleTouchEvent(event);

				// Inside View.java, dispatchTouchEvent() does not call onTouchEvent() if this listener returns true. As
				// a result, click and other motion events do not occur on the native Android side. To prevent this, we
				// always return false and let Android generate click and other motion events.
				return false;
			}
		});
		
	}
	
	protected void handleTouchEvent(MotionEvent event) {
		String motionEvent = motionEvents.get(event.getAction());
		if (motionEvent != null) {
			if (proxy.hierarchyHasListener(motionEvent)) {
				fireEvent(motionEvent, dictFromEvent(event));
			}
		}
	}

	protected void registerForTouch(final View touchable)
	{
		if (touchable == null) {
			return;
		}
		
		boolean clickable = proxy.getProperties().optBoolean(TiC.PROPERTY_TOUCH_ENABLED, true);

		if (clickable) {
			registerTouchEvents(touchable);

			// Previously, we used the single tap handling above to fire our click event. It doesn't
			// work: a single tap is not the same as a click. A click can be held for a while before
			// lifting the finger; a single-tap is only generated from a quick tap (which will also cause
			// a click.) We wanted to do it in single-tap handling presumably because the singletap
			// listener gets a MotionEvent, which gives us the information we want to provide to our
			// users in our click event, whereas Android's standard OnClickListener does _not_ contain
			// that info. However, an "up" seems to always occur before the click listener gets invoked,
			// so we store the last up event's x,y coordinates (see onTouch above) and use them here.
			// Note: AdapterView throws an exception if you try to put a click listener on it.
			doSetClickable(touchable);
		}
	}


	public void registerForKeyPress()
	{
		if (allowRegisterForKeyPress()) {
			registerForKeyPress(getNativeView());
		}
	}

	protected void registerForKeyPress(final View v)
	{
		if (v == null) {
			return;
		}

		Object focusable = proxy.getProperty(TiC.PROPERTY_FOCUSABLE);
		if (focusable != null) {
			registerForKeyPress(v, TiConvert.toBoolean(focusable, false));
		}
	}

	protected void registerForKeyPress(final View v, boolean focusable)
	{
		if (v == null) {
			return;
		}

		v.setFocusable(focusable);

		// The listener for the "keypressed" event is only triggered when the view has focus. So we only register the
		// "keypressed" event when the view is focusable.
		if (focusable) {
			registerForKeyPressEvents(v);
		} else {
			v.setOnKeyListener(null);
		}
	}

	/**
	 * Registers a callback to be invoked when a hardware key is pressed in this view.
	 *
	 * @param v The view to have the key listener to attach to.
	 */
	protected void registerForKeyPressEvents(final View v)
	{
		if (v == null) {
			return;
		}

		v.setOnKeyListener(new OnKeyListener()
		{
			public boolean onKey(View view, int keyCode, KeyEvent event)
			{
				if (event.getAction() == KeyEvent.ACTION_UP) {
					KrollDict data = new KrollDict();
					data.put(TiC.EVENT_PROPERTY_KEYCODE, keyCode);
					fireEvent(TiC.EVENT_KEY_PRESSED, data);

					switch (keyCode) {
						case KeyEvent.KEYCODE_ENTER:
						case KeyEvent.KEYCODE_DPAD_CENTER:
							if (proxy.hasListeners(TiC.EVENT_CLICK)) {
								fireEvent(TiC.EVENT_CLICK, null);
								return true;
							}
					}
				}
				return false;
			}
		});
	}


	/**
	 * Sets the nativeView's opacity.
	 * @param opacity the opacity to set.
	 */
	@SuppressLint("NewApi")
	public void setOpacity(float opacity)
	{
		if (opacity < 0 || opacity > 1) {
			Log.w(TAG, "Ignoring invalid value for opacity: " + opacity);
			return;
		}
		if (borderView != null) {
			borderView.setBorderAlpha(Math.round(opacity * 255));
			borderView.postInvalidate();
		}
		if (nativeView != null) {
			if (HONEYCOMB_OR_GREATER) {
				setAlpha(nativeView, opacity);
			} else {
				setOpacity(nativeView, opacity);
			}
			nativeView.postInvalidate();
		}
	}
	
	public float getOpacity() {
		return TiConvert.toFloat(proxy.getProperty(TiC.PROPERTY_OPACITY));
	}

	/**
	 * Sets the view's alpha (Honeycomb or later).
	 * @param view The native view object
	 * @param alpha The new alpha value
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	protected void setAlpha(View view, float alpha)
	{
		view.setAlpha(alpha);
	}

	/**
	 * Sets the view's opacity (pre-Honeycomb).
	 * @param view the view object.
	 * @param opacity the opacity to set.
	 */
	protected void setOpacity(View view, float opacity)
	{
		if (view != null) {
			TiUIHelper.setDrawableOpacity(view.getBackground(), opacity);
			if (opacity == 1) {
				clearOpacity(view);
			}
		}
	}

	public void clearOpacity(View view)
	{
		if (background != null)
			background.clearColorFilter();
	}

	public TiBlob toImage(Number scale)
	{
		return TiUIHelper.viewToImage(proxy.getProperties(), getNativeView(), scale.floatValue());
	}

	private View getTouchView()
	{
		if (nativeView != null) {
			return nativeView;
		} else {
			if (touchView != null) {
				return touchView.get();
			}
		}
		return null;
	}

	protected void doSetClickable(View view, boolean clickable)
	{
		if (view == null) {
			return;
		}
		if (!clickable) {
			view.setOnClickListener(null); // This will set clickable to true in the view, so make sure it stays here so the next line turns it off.
			view.setClickable(false);
			view.setOnLongClickListener(null);
			view.setLongClickable(false);
		} else if ( ! (view instanceof AdapterView) ){
			// n.b.: AdapterView throws if click listener set.
			// n.b.: setting onclicklistener automatically sets clickable to true.
			setOnClickListener(view);
			setOnLongClickListener(view);
		}
	}

	private void doSetClickable(boolean clickable)
	{
		doSetClickable(getTouchView(), clickable);
	}

	/*
	 * Used just to setup the click listener if applicable.
	 */
	private void doSetClickable(View view)
	{
		if (view == null) {
			return;
		}
		doSetClickable(view, view.isClickable());
	}

	/**
	 * Can be overriden by inheriting views for special click handling.  For example,
	 * the Facebook module's login button view needs special click handling.
	 */
	protected void setOnClickListener(View view)
	{
		
		view.setOnClickListener(new OnClickListener()
		{
			public void onClick(View view)
			{
				fireEvent(TiC.EVENT_CLICK, dictFromEvent(lastUpEvent));
			}
		});
	}
	
	public boolean fireEvent(String eventName, KrollDict data) {
		return fireEvent(eventName, data, true);
	}

	public boolean fireEvent(String eventName, KrollDict data, boolean bubbles) {
		if (data == null && additionalEventData != null) {
			data = new KrollDict(additionalEventData);
		} else if (additionalEventData != null) {
			data.putAll(additionalEventData);
		}
		return proxy.fireEvent(eventName, data, bubbles);
	}

	protected void setOnLongClickListener(View view)
	{
		view.setOnLongClickListener(new OnLongClickListener()
		{
			public boolean onLongClick(View view)
			{
				return fireEvent(TiC.EVENT_LONGCLICK, dictFromEvent(lastDownEvent));
			}
		});
	}

	protected void disableHWAcceleration()
	{
		View  view = getOuterView();
		if (view == null) {
			return;
		}
		Log.d(TAG, "Disabling hardware acceleration for instance of " + view.getClass().getSimpleName(),
			Log.DEBUG_MODE);
		if (mSetLayerTypeMethod == null) {
			try {
				Class<? extends View> c = view.getClass();
				mSetLayerTypeMethod = c.getMethod("setLayerType", int.class, Paint.class);
			} catch (SecurityException e) {
				Log.e(TAG, "SecurityException trying to get View.setLayerType to disable hardware acceleration.", e,
					Log.DEBUG_MODE);
			} catch (NoSuchMethodException e) {
				Log.e(TAG, "NoSuchMethodException trying to get View.setLayerType to disable hardware acceleration.", e,
					Log.DEBUG_MODE);
			}
		}

		if (mSetLayerTypeMethod == null) {
			return;
		}
		try {
			mSetLayerTypeMethod.invoke(view, LAYER_TYPE_SOFTWARE, null);
		} catch (IllegalArgumentException e) {
			Log.e(TAG, e.getMessage(), e);
		} catch (IllegalAccessException e) {
			Log.e(TAG, e.getMessage(), e);
		} catch (InvocationTargetException e) {
			Log.e(TAG, e.getMessage(), e);
		}
	}
	
	public boolean hWAccelerationDisabled(){
		return (mSetLayerTypeMethod != null);
	}

	/**
	 * Set the animated alpha values, since Android provides no property for looking it up.
	 */
	public void setAnimatedAlpha(float alpha)
	{
		animatedAlpha = alpha;
	}

	/**
	 * Retrieve the animated alpha value, which we store here since Android provides no property
	 * for looking it up.
	 */
	public float getAnimatedAlpha()
	{
		return animatedAlpha;
	}

	/**
	 * "Forget" the values we save after scale and rotation and alpha animations.
	 */
	private void resetPostAnimationValues()
	{
		animatedAlpha = Float.MIN_VALUE; // we use min val to signal no val.
	}

	private void applyContentDescription()
	{
		if (proxy == null || nativeView == null) {
			return;
		}
		String contentDescription = composeContentDescription();
		if (contentDescription != null) {
			nativeView.setContentDescription(contentDescription);
		}
	}

	/**
	 * Our view proxy supports three properties to match iOS regarding
	 * the text that is read aloud (or otherwise communicated) by the
	 * assistive technology: accessibilityLabel, accessibilityHint
	 * and accessibilityValue.
	 *
	 * We combine these to create the single Android property contentDescription.
	 * (e.g., View.setContentDescription(...));
	 */
	protected String composeContentDescription()
	{
		if (proxy == null) {
			return null;
		}

		final String punctuationPattern = "^.*\\p{Punct}\\s*$";
		StringBuilder buffer = new StringBuilder();

		KrollDict properties = proxy.getProperties();
		String label, hint, value;
		label = TiConvert.toString(properties.get(TiC.PROPERTY_ACCESSIBILITY_LABEL));
		hint = TiConvert.toString(properties.get(TiC.PROPERTY_ACCESSIBILITY_HINT));
		value = TiConvert.toString(properties.get(TiC.PROPERTY_ACCESSIBILITY_VALUE));

		if (!TextUtils.isEmpty(label)) {
			buffer.append(label);
			if (!label.matches(punctuationPattern)) {
				buffer.append(".");
			}
		}

		if (!TextUtils.isEmpty(value)) {
			if (buffer.length() > 0) {
				buffer.append(" ");
			}
			buffer.append(value);
			if (!value.matches(punctuationPattern)) {
				buffer.append(".");
			}
		}

		if (!TextUtils.isEmpty(hint)) {
			if (buffer.length() > 0) {
				buffer.append(" ");
			}
			buffer.append(hint);
			if (!hint.matches(punctuationPattern)) {
				buffer.append(".");
			}
		}

		return buffer.toString();
	}

	private void applyAccessibilityProperties()
	{
		if (nativeView != null) {
			applyContentDescription();
			applyAccessibilityHidden();
		}

	}

	private void applyAccessibilityHidden()
	{
		if (nativeView == null || proxy == null) {
			return;
		}

		applyAccessibilityHidden(proxy.getProperty(TiC.PROPERTY_ACCESSIBILITY_HIDDEN));
	}

	private void applyAccessibilityHidden(Object hiddenPropertyValue)
	{
		if (nativeView == null) {
			return;
		}

		int importanceMode = ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO;

		if (hiddenPropertyValue != null && TiConvert.toBoolean(hiddenPropertyValue, false)) {
				importanceMode = ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO;
		}

		ViewCompat.setImportantForAccessibility(nativeView, importanceMode);
	}
	
	public void setTiWidth(int val) {
		View view =  getOuterView();
		ViewGroup.LayoutParams params = getNativeView().getLayoutParams();
		params.height = val;
		if (params instanceof TiCompositeLayout.LayoutParams) {
			TiCompositeLayout.LayoutParams tiParams = (TiCompositeLayout.LayoutParams) params;
			tiParams.optionWidth = new TiDimension(val, TiDimension.TYPE_WIDTH, TypedValue.COMPLEX_UNIT_PX);
		}
		view.setLayoutParams(params);
	}
	public int getTiWidth() {
		View view =  getOuterView();
		return view.getMeasuredWidth();
	}
	public void setTiHeight(int val) {
		View view =  getOuterView();
		ViewGroup.LayoutParams params = getNativeView().getLayoutParams();
		params.height = val;
		if (params instanceof TiCompositeLayout.LayoutParams) {
			TiCompositeLayout.LayoutParams tiParams = (TiCompositeLayout.LayoutParams) params;
			tiParams.optionHeight = new TiDimension(val, TiDimension.TYPE_HEIGHT, TypedValue.COMPLEX_UNIT_PX);
		}
		view.setLayoutParams(params);
	}
	public int getTiHeight() {
		View view =  getOuterView();
		return view.getMeasuredHeight();
	}
	public void setTiTop(int val) {
		View view =  getOuterView();
		ViewGroup.LayoutParams params = getNativeView().getLayoutParams();
		if (params instanceof TiCompositeLayout.LayoutParams) {
			TiCompositeLayout.LayoutParams tiParams = (TiCompositeLayout.LayoutParams) params;
			tiParams.optionTop = new TiDimension(val, TiDimension.TYPE_TOP, TypedValue.COMPLEX_UNIT_PX);
		}
		view.setLayoutParams(params);
	}
	public int getTiTop() {
		View view =  getOuterView();
		return view.getTop();
	}
	public void setTiBottom(int val) {
		View view =  getOuterView();
		ViewGroup.LayoutParams params = getNativeView().getLayoutParams();
		if (params instanceof TiCompositeLayout.LayoutParams) {
			TiCompositeLayout.LayoutParams tiParams = (TiCompositeLayout.LayoutParams) params;
			tiParams.optionBottom = new TiDimension(val, TiDimension.TYPE_BOTTOM, TypedValue.COMPLEX_UNIT_PX);
		}
		view.setLayoutParams(params);
	}
	public int getTiBottom() {
		View view =  getOuterView();
		return view.getBottom();
	}
	public void setTiLeft(int val) {
		View view =  getOuterView();
		ViewGroup.LayoutParams params = getNativeView().getLayoutParams();
		if (params instanceof TiCompositeLayout.LayoutParams) {
			TiCompositeLayout.LayoutParams tiParams = (TiCompositeLayout.LayoutParams) params;
			tiParams.optionLeft = new TiDimension(val, TiDimension.TYPE_LEFT, TypedValue.COMPLEX_UNIT_PX);
		}
		view.setLayoutParams(params);
	}
	public int getTiLeft() {
		View view =  getOuterView();
		return view.getLeft();
	}
	public void setTiRight(int val) {
		View view =  getOuterView();
		ViewGroup.LayoutParams params = getNativeView().getLayoutParams();
		if (params instanceof TiCompositeLayout.LayoutParams) {
			TiCompositeLayout.LayoutParams tiParams = (TiCompositeLayout.LayoutParams) params;
			tiParams.optionRight = new TiDimension(val, TiDimension.TYPE_RIGHT, TypedValue.COMPLEX_UNIT_PX);
		}
		view.setLayoutParams(params);
	}
	public int getTiRight() {
		View view =  getOuterView();
		return view.getRight();
	}
	public void setTiCenterX(int val) {
		View view =  getOuterView();
		ViewGroup.LayoutParams params = getNativeView().getLayoutParams();
		if (params instanceof TiCompositeLayout.LayoutParams) {
			TiCompositeLayout.LayoutParams tiParams = (TiCompositeLayout.LayoutParams) params;
			tiParams.optionCenterX = new TiDimension(val, TiDimension.TYPE_CENTER_X, TypedValue.COMPLEX_UNIT_PX);
		}
		view.setLayoutParams(params);
	}
	public int getTiCenterX() {
		View view =  getOuterView();
		return (view.getLeft() + view.getMeasuredWidth()) / 2;
	}
	
	public void setTiCenterY(int val) {
		View view = getOuterView();
		ViewGroup.LayoutParams params = getNativeView().getLayoutParams();
		if (params instanceof TiCompositeLayout.LayoutParams) {
			TiCompositeLayout.LayoutParams tiParams = (TiCompositeLayout.LayoutParams) params;
			tiParams.optionCenterY = new TiDimension(val, TiDimension.TYPE_CENTER_Y, TypedValue.COMPLEX_UNIT_PX);
		}
		view.setLayoutParams(params);
	}
	public int getTiCenterY() {
		View view =  getOuterView();
		return (view.getTop() + view.getMeasuredHeight()) / 2;
	}
	
	public void setTiBackgroundColor(int color) {
		int currentColor = getTiBackgroundColor();
		if (currentColor != color) {
			TiBackgroundDrawable bgdDrawable = getOrCreateBackground();
			bgdDrawable.setColorForState(TiUIHelper.BACKGROUND_DEFAULT_STATE_1, color);
			bgdDrawable.setColorForState(TiUIHelper.BACKGROUND_DEFAULT_STATE_2, color);
			bgdDrawable.invalidateSelf();
		}
	}
	
	public int getTiBackgroundColor() {
		if (background == null)
		{
			return Color.TRANSPARENT;
		}
		return background.getColorForState(TiUIHelper.BACKGROUND_DEFAULT_STATE_1);
	}
	
	private class FakeMatrixAnimation extends Animation {
		Matrix matrix;
		
		public FakeMatrixAnimation(Matrix matrix){
			this.setDuration(0);
			this.setFillAfter(true);
			this.matrix = matrix;
		}
		
		@Override
		protected void applyTransformation(float interpolatedTime,
				Transformation t) {
			super.applyTransformation(interpolatedTime, t);
			t.getMatrix().set(matrix);
		}
	}
	
	public void setTi2DMatrix(Ti2DMatrix matrix) {
		
		View outerView =  getOuterView();
		outerView.clearAnimation();
		FakeMatrixAnimation matrixAnimation = new FakeMatrixAnimation(matrix.getTransformMatrix());
		outerView.startAnimation(matrixAnimation);
		ViewParent viewParent = outerView.getParent();
		if (outerView.getVisibility() == View.VISIBLE && viewParent instanceof View) {
			((View) viewParent).postInvalidate();
		}
	}
	public Ti2DMatrix getTi2DMatrix() {
		return layoutParams.optionTransform;
	}
	
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private ValueAnimator tiDimensionAnimator(TiDimension[] dimensions, View view, View parentView, TiAnimation proxy, HashMap options) 
	{
		PropertyValuesHolder[] arrayOfPropertyValuesHolder = new PropertyValuesHolder[dimensions.length];
		for (int i = 0; i < dimensions.length; i++) {
			TiDimension dimension = dimensions[i];
			int to = dimension.getAsPixels((parentView != null) ? parentView : view);
			PropertyValuesHolder anim = PropertyValuesHolder.ofInt(dimension.getAnimatedProperty() , to);
			arrayOfPropertyValuesHolder[i] = anim;
		}		
		
		TiAnimatorListener listener = new TiAnimatorListener(view, proxy, options) {
			public void onAnimationStart(Animator animation) {}
			public void onAnimationEnd(Animator animation) {
				
				TiMessenger.postOnMain(new Runnable() {
					public void run() {
						ViewGroup.LayoutParams params = view.getLayoutParams();
						if (params instanceof TiCompositeLayout.LayoutParams) {
							if (viewProxy != null) {
								TiConvert
										.fillLayout(
												viewProxy.getProperties(),
												(TiCompositeLayout.LayoutParams) params);
							} else if (options != null) {
								TiConvert
										.fillLayout(
												options,
												(TiCompositeLayout.LayoutParams) params);
							}
						}
						view.setLayoutParams(params);
					}
				});
			}
		};
		ObjectAnimator localObjectAnimator = ObjectAnimator.ofPropertyValuesHolder(this, arrayOfPropertyValuesHolder);
		localObjectAnimator.addListener(listener);
		return localObjectAnimator;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public void prepareAnimatorSet(TiAnimatorSet tiSet) {
		AnimatorSet set = tiSet.set();
		final View view = proxy.viewToAnimate();

		List<Animator> list = new ArrayList<Animator>();
		HashMap options = tiSet.getOptions();

		if (options.containsKey(TiC.PROPERTY_DELAY)) {
			set.setStartDelay(TiConvert.toInt(options, TiC.PROPERTY_DELAY));
		}

		if (options.containsKey(TiC.PROPERTY_DURATION)) {
			set.setDuration(TiConvert.toInt(options, TiC.PROPERTY_DURATION));
		}

		int repeat = 1;
		final boolean autoreverse = options
				.containsKey(TiC.PROPERTY_AUTOREVERSE) ? TiConvert.toBoolean(
				options, TiC.PROPERTY_AUTOREVERSE) : false;
		tiSet.setAutoreverse(autoreverse);
		if (options.containsKey(TiC.PROPERTY_REPEAT)) {
			repeat = TiConvert.toInt(options, TiC.PROPERTY_REPEAT);

			if (repeat == 0) {
				// A repeat of 0 is probably non-sensical. Titanium iOS
				// treats it as 1 and so should we.
				repeat = 1;
			}
		}
		set.addListener(new TiAnimatorListener(this.proxy, tiSet,
				options));

		if (options.containsKey(TiC.PROPERTY_OPACITY)) {
			show();
			ObjectAnimator anim = ObjectAnimator.ofFloat(this, "opacity",
					TiConvert.toFloat(options, TiC.PROPERTY_OPACITY));
			list.add(anim);
		}

		if (options.containsKey(TiC.PROPERTY_BACKGROUND_COLOR)) {
//			if (!proxy.hasProperty(TiC.PROPERTY_BACKGROUND_COLOR)) {
//				Log.w(TAG, "Cannot animate view without a backgroundColor. View doesn't have that property. Using #00000000");
//				getNativeView().setBackgroundColor(Color.argb(0, 0, 0, 0));
//			}
			ObjectAnimator anim = ObjectAnimator.ofInt(this, "tiBackgroundColor", TiConvert.toColor(options, TiC.PROPERTY_BACKGROUND_COLOR));
			 anim.setEvaluator(new ArgbEvaluator());
			list.add(anim);
		}
		
		ViewParent parent = view.getParent();
		View parentView = null;
		int parentWidth = 0;
		int parentHeight = 0;

		if (parent instanceof ViewGroup) {
			ViewGroup group = (ViewGroup) parent;
			parentHeight = group.getMeasuredHeight();
			parentWidth = group.getMeasuredWidth();
		}
		if (parent instanceof View) {
			parentView = (View) parent;
		}
		List<TiDimension> dimensions = new ArrayList<TiDimension>();
		if (options.containsKey(TiC.PROPERTY_WIDTH)) {
			String value = TiConvert.toString(options, TiC.PROPERTY_WIDTH);
			if (value != null)
				dimensions.add(new TiDimension(value, TiDimension.TYPE_WIDTH));
			else
				dimensions.add(new TiDimension(view.getMeasuredWidth(), TiDimension.TYPE_WIDTH));
		}
		if (options.containsKey(TiC.PROPERTY_HEIGHT)) {
			String value = TiConvert.toString(options, TiC.PROPERTY_HEIGHT);
			if (value != null)
				dimensions.add(new TiDimension(value, TiDimension.TYPE_HEIGHT));
			else
				dimensions.add(new TiDimension(view.getMeasuredHeight(), TiDimension.TYPE_HEIGHT));
		}
		if (options.containsKey(TiC.PROPERTY_TOP)) {
			String value = TiConvert.toString(options, TiC.PROPERTY_TOP);
			if (value != null)
				dimensions.add(new TiDimension(value, TiDimension.TYPE_TOP));
			else
				dimensions.add(new TiDimension(((parentHeight - view.getMeasuredHeight())/2), TiDimension.TYPE_TOP));
		}
		if (options.containsKey(TiC.PROPERTY_BOTTOM)) {
			String value = TiConvert.toString(options, TiC.PROPERTY_BOTTOM);
			if (value != null)
				dimensions.add(new TiDimension(value, TiDimension.TYPE_BOTTOM));
			else
				dimensions.add(new TiDimension(((parentHeight + view.getMeasuredHeight())/2), TiDimension.TYPE_BOTTOM));
		}
		if (options.containsKey(TiC.PROPERTY_LEFT)) {
			String value = TiConvert.toString(options, TiC.PROPERTY_LEFT);
			if (value != null)
				dimensions.add(new TiDimension(value, TiDimension.TYPE_LEFT));
			else
				dimensions.add(new TiDimension(((parentWidth - view.getMeasuredWidth())/2), TiDimension.TYPE_LEFT));
		}
		if (options.containsKey(TiC.PROPERTY_RIGHT)) {
			String value = TiConvert.toString(options, TiC.PROPERTY_RIGHT);
			if (value != null)
				dimensions.add(new TiDimension(value, TiDimension.TYPE_RIGHT));
			else
				dimensions.add(new TiDimension(((parentWidth + view.getMeasuredWidth())/2), TiDimension.TYPE_RIGHT));
		}
		if (options.containsKey(TiC.PROPERTY_CENTER)) {
			HashMap center = (HashMap) options.get(TiC.PROPERTY_CENTER);
			String value = (center == null)?null:TiConvert.toString(center.get(TiC.PROPERTY_X));
			if (value != null)
				dimensions.add(new TiDimension(value, TiDimension.TYPE_CENTER_X));
			else
				dimensions.add(new TiDimension((parentWidth/2), TiDimension.TYPE_CENTER_X));
			value = (center == null)?null:TiConvert.toString(center.get(TiC.PROPERTY_Y));
			if (value != null)
				dimensions.add(new TiDimension(value, TiDimension.TYPE_CENTER_Y));
			else
				dimensions.add(new TiDimension((parentHeight/2), TiDimension.TYPE_CENTER_Y));
		}
	
		TiDimension[] dimsArray = {};
		ValueAnimator layoutAnim = tiDimensionAnimator(dimensions.toArray(dimsArray), view, parentView, tiSet.animationProxy, options);
		list.add(layoutAnim);
		
		
		
		if (options.containsKey(TiC.PROPERTY_TRANSFORM)) {
			float anchorX = Ti2DMatrix.DEFAULT_ANCHOR_VALUE;
			float anchorY = Ti2DMatrix.DEFAULT_ANCHOR_VALUE;
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
			
			
			Ti2DMatrix tdm = (Ti2DMatrix) options.get(TiC.PROPERTY_TRANSFORM);
			if (layoutParams.optionTransform != null) {
				tdm = layoutParams.optionTransform.invert().multiply(tdm);
			}
			ObjectAnimator anim = ObjectAnimator.ofObject(this, "ti2DMatrix", new Ti2DMatrixEvaluator(view, anchorX, anchorY), tdm);
			anim.addListener(new TiAnimatorListener(this.proxy, tiSet, options) {
				public void onAnimationStart(Animator animation) {}
				public void onAnimationEnd(Animator animation) {
					layoutParams.optionTransform = (Ti2DMatrix) options.get(TiC.PROPERTY_TRANSFORM);
				}
			});
			list.add(anim);
		}


		if (autoreverse) {
			for (int i = 0; i < list.size(); i++) {
				ValueAnimator anim = (ValueAnimator) list.get(i);
				anim.setRepeatCount(1);
				anim.setRepeatMode(ValueAnimator.REVERSE);
			}
		} else if (repeat > 1) {
			int realRepeat = repeat - 1;
			for (int i = 0; i < list.size(); i++) {
				ValueAnimator anim = (ValueAnimator) list.get(i);
				anim.setRepeatCount(realRepeat);
				anim.setRepeatMode(ValueAnimator.RESTART);
			}
		}
		set.playTogether(list);
	}
}
