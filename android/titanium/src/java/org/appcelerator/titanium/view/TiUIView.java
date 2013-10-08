/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package org.appcelerator.titanium.view;

import java.lang.ref.WeakReference;
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
import org.appcelerator.titanium.util.AffineTransform.DecomposedType;
import org.appcelerator.titanium.util.Ti2DMatrixEvaluator;
import org.appcelerator.titanium.util.TiAnimatorListener;
import org.appcelerator.titanium.util.TiAnimatorSet;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiUIHelper;
import org.appcelerator.titanium.util.TiViewAnimator;
import org.appcelerator.titanium.view.TiCompositeLayout.AnimationLayoutParams;
import org.appcelerator.titanium.view.TiCompositeLayout.LayoutParams;
import org.appcelerator.titanium.TiBlob;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ArgbEvaluator;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.animation.PropertyValuesHolder;
import com.nineoldandroids.animation.ValueAnimator;
import com.nineoldandroids.view.animation.AnimatorProxy;

import android.annotation.TargetApi;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.view.ViewCompat;
import android.text.TextUtils;
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
import android.view.animation.AccelerateDecelerateInterpolator;
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
	protected TiBackgroundDrawable background;
	
	protected KrollDict additionalEventData;

	private float animatedAlpha = Float.MIN_VALUE; // i.e., no animated alpha.

	protected KrollDict lastUpEvent = new KrollDict(2);
	protected KrollDict lastDownEvent = new KrollDict(2);

	// In the case of heavy-weight windows, the "nativeView" is null,
	// so this holds a reference to the view which is used for touching,
	// i.e., the view passed to registerForTouch.
	private WeakReference<View> touchView = null;


	private boolean zIndexChanged = false;
	protected TiBorderWrapperView borderView;
	// For twofingertap detection
	private boolean didScale = false;

	//to maintain sync visibility between borderview and view. Default is visible
	private int visibility = View.VISIBLE;
	
	protected GestureDetector detector = null;

	protected Handler handler;
	
	protected boolean exclusiveTouch = false;
	public boolean hardwareAccSupported = true;
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
		return nativeView;
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
			} else {
				handler.sendEmptyMessage(MSG_SET_BACKGROUND);
			}
		}
		
		if (borderView != null)
		{
			addBorderView();
		}
		
		if (HONEYCOMB_OR_GREATER && hardwareAccSupported == false) {
			disableHWAcceleration();
		}
		applyAccessibilityProperties();
	}

	protected void setLayoutParams(LayoutParams layoutParams)
	{
		this.layoutParams = layoutParams;
	}
	
	public void cleanAnimatedParams()
	{
		if (layoutParams instanceof AnimationLayoutParams) {
			//we remove any animated params...
			layoutParams = new TiCompositeLayout.LayoutParams(layoutParams);
			if (getOuterView() != null)
				getOuterView().setLayoutParams(layoutParams);
		}
	}

	public void listenerAdded(String type, int count, KrollProxy proxy) {
	}

	public void listenerRemoved(String type, int count, KrollProxy proxy){
	}

	public float[] getPreTranslationValue(float[] points)
	{
		View view = getOuterView();
		if (view != null && layoutParams.matrix != null) {
			Matrix m = layoutParams.matrix.getMatrix(view);
			// Get the translation values
			float[] values = new float[9];
			m.getValues(values);
			points[0] = points[0] - values[2];
			points[1] = points[1] - values[5];
		}
		return points;
	}

	@SuppressLint("NewApi")
	public void applyTransform(Ti2DMatrix timatrix)
	{
		View view = getOuterView();
		if (view != null) {
			layoutParams.matrix = timatrix;
			view.setLayoutParams(layoutParams);
			ViewParent viewParent = view.getParent();
			if (view.getVisibility() == View.VISIBLE && viewParent instanceof View) {
				((View) viewParent).postInvalidate();
			}
		}
//		View parent = (proxy.getParent() != null)?proxy.getParent().getParentViewForChild():null;
//		if (parent instanceof FreeLayout) {
			// layoutParams.matrix = timatrix;
			// outerView.setLayoutParams(layoutParams);
//		}
//		else if (HONEYCOMB_OR_GREATER) {
//			if (timatrix != null) {
//				DecomposedType decompose = timatrix.getAffineTransform(outerView, true).decompose();
//				outerView.setTranslationX((float)decompose.translateX);
//				outerView.setTranslationY((float)decompose.translateY);
//				outerView.setRotation((float)(decompose.angle*180/Math.PI));
//				outerView.setScaleX((float)decompose.scaleX);
//				outerView.setScaleY((float)decompose.scaleY);
//			}
//			else {
//				outerView.setTranslationX(0);
//				outerView.setTranslationY(0);
//				outerView.setRotation(0);
//				outerView.setScaleX(1);
//				outerView.setScaleY(1);
//			}
//		}
	}

	public void forceLayoutNativeView(boolean informParent)
	{
		layoutNativeView(informParent);
	}

	protected void layoutNativeView()
	{
		layoutNativeView(false);
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
				View v = uiv.getNativeView();
				if (v.getVisibility() == View.INVISIBLE || v.getVisibility() == View.GONE) {
					//if we have a parent which is hidden, we are hidden, so no need to layout
					return;
				}
			}
		}
		if (nativeView != null) {
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
			View childHolder = getParentViewForChild();
			if (childHolder != null) {
				childHolder.requestLayout();
			}
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
			View parentViewForChild = getParentViewForChild();
			if (parentViewForChild instanceof TiCompositeLayout) {
				((TiCompositeLayout)parentViewForChild).setLayoutArrangement(layout);
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
				((TiCompositeLayout) getParentViewForChild()).setEnableHorizontalWrap(TiConvert.toBoolean(newValue,true));
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
			layoutNativeView(true);
		} else if (key.equals(TiC.PROPERTY_FOCUSABLE) && newValue != null) {
			registerForKeyPress(nativeView, TiConvert.toBoolean(newValue, false));
		} else if (key.equals(TiC.PROPERTY_TOUCH_ENABLED)) {
			doSetClickable(TiConvert.toBoolean(newValue));
		} else if (key.equals(TiC.PROPERTY_VISIBLE)) {
			newValue = (newValue == null) ? false : newValue;
			this.setVisibility(TiConvert.toBoolean(newValue) ? View.VISIBLE : View.INVISIBLE);
		} else if (key.equals(TiC.PROPERTY_ENABLED)) {
			nativeView.setEnabled(TiConvert.toBoolean(newValue));
		} else if (key.equals(TiC.PROPERTY_EXCLUSIVE_TOUCH)) {
			exclusiveTouch = TiConvert.toBoolean(newValue);
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
			Drawable drawable =  TiUIHelper.buildGradientDrawable((KrollDict)newValue);
			getOrCreateBackground().setGradientDrawableForState(TiUIHelper.BACKGROUND_SELECTED_STATE, drawable);
		} else if (key.equals(TiC.PROPERTY_BACKGROUND_FOCUSED_GRADIENT)) {
			Drawable drawable =  TiUIHelper.buildGradientDrawable((KrollDict)newValue);
			getOrCreateBackground().setGradientDrawableForState(TiUIHelper.BACKGROUND_FOCUSED_STATE, drawable);
		} else if (key.equals(TiC.PROPERTY_BACKGROUND_DISABLED_GRADIENT)) {
			Drawable drawable =  TiUIHelper.buildGradientDrawable((KrollDict)newValue);
			getOrCreateBackground().setGradientDrawableForState(TiUIHelper.BACKGROUND_DISABLED_STATE, drawable);
		} else if (key.equals(TiC.PROPERTY_BACKGROUND_GRADIENT)) {
			TiBackgroundDrawable bgdDrawable = getOrCreateBackground();
			Drawable drawable =  TiUIHelper.buildGradientDrawable((KrollDict)newValue);
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
			setBorderWidth(TiUIHelper.getRawSizeOrZero(newValue));
		} else if (key.equals(TiC.PROPERTY_VIEW_MASK)) {
			setViewMask(newValue);
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
	
	protected void updateLayoutForChildren(KrollDict d) {
		View viewForLayout = getParentViewForChild();
		
		if (viewForLayout instanceof TiCompositeLayout) {
			TiCompositeLayout tiLayout = (TiCompositeLayout)viewForLayout;
			if (d.containsKey(TiC.PROPERTY_LAYOUT)) {
				String layout = TiConvert.toString(d, TiC.PROPERTY_LAYOUT);
				tiLayout.setLayoutArrangement(layout);
			}

			if (d.containsKey(TiC.PROPERTY_HORIZONTAL_WRAP)) {
				tiLayout.setEnableHorizontalWrap(TiConvert.toBoolean(d,TiC.PROPERTY_HORIZONTAL_WRAP,true));
			}			
		}
	}

	public void processProperties(KrollDict d)
	{
		boolean nativeViewNull = false;
		
		
		if (nativeView == null) {
			nativeViewNull = true;
			Log.d(TAG, "Nativeview is null", Log.DEBUG_MODE);
		}
			
		updateLayoutForChildren(d);

		if (d.containsKey(TiC.PROPERTY_CLIP_CHILDREN)) {
			View rootView = getRootView();
			if (rootView instanceof ViewGroup) {
				((ViewGroup) rootView).setClipToPadding(TiConvert.toBoolean(d, TiC.PROPERTY_CLIP_CHILDREN));				
			}
		}

		if (d.containsKey(TiC.PROPERTY_TOUCH_PASSTHROUGH) && (nativeView instanceof TiCompositeLayout)) {
			((TiCompositeLayout)nativeView).setTouchPassThrough(TiConvert.toBoolean(d, TiC.PROPERTY_TOUCH_PASSTHROUGH));
		}

		if (d.containsKey(TiC.PROPERTY_EXCLUSIVE_TOUCH)) {
			exclusiveTouch = TiConvert.toBoolean(d, TiC.PROPERTY_EXCLUSIVE_TOUCH);
		}
		
		if (TiConvert.fillLayout(d, layoutParams) && getOuterView() != null) {
			getOuterView().setLayoutParams(layoutParams);
//			getOuterView().requestLayout();
		}
		
		registerForTouch();
		registerForKeyPress();
		
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
			Drawable drawable =  TiUIHelper.buildGradientDrawable(d.getKrollDict(TiC.PROPERTY_BACKGROUND_SELECTED_GRADIENT));
			getOrCreateBackground().setGradientDrawableForState(TiUIHelper.BACKGROUND_SELECTED_STATE, drawable);
		}
		if (d.containsKey(TiC.PROPERTY_BACKGROUND_FOCUSED_GRADIENT)) {
			Drawable drawable =  TiUIHelper.buildGradientDrawable(d.getKrollDict(TiC.PROPERTY_BACKGROUND_FOCUSED_GRADIENT));
			getOrCreateBackground().setGradientDrawableForState(TiUIHelper.BACKGROUND_FOCUSED_STATE, drawable);
		}
		if (d.containsKey(TiC.PROPERTY_BACKGROUND_DISABLED_GRADIENT)) {
			Drawable drawable =  TiUIHelper.buildGradientDrawable(d.getKrollDict(TiC.PROPERTY_BACKGROUND_DISABLED_GRADIENT));
			getOrCreateBackground().setGradientDrawableForState(TiUIHelper.BACKGROUND_DISABLED_STATE, drawable);
		} 
		if (d.containsKey(TiC.PROPERTY_BACKGROUND_GRADIENT)) {
			TiBackgroundDrawable bgdDrawable = getOrCreateBackground();
			Drawable drawable =  TiUIHelper.buildGradientDrawable(d.getKrollDict(TiC.PROPERTY_BACKGROUND_GRADIENT));
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
			setBorderWidth(TiUIHelper.getRawSizeOrZero(d, TiC.PROPERTY_BORDER_WIDTH));
		} 
		if (d.containsKey(TiC.PROPERTY_VIEW_MASK)) {
			setViewMask(d.get(TiC.PROPERTY_VIEW_MASK));
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
		View nv = getRootView();
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
		proxy.setProperty(TiC.PROPERTY_VISIBLE, (visibility == View.VISIBLE));

		View view = getOuterView();
		if (view != null) {
			view.clearAnimation();
			view.setVisibility(this.visibility);
		}
		
		view = getRootView();
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
	
	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
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
		View view = getNativeView();
		if (view != null) {
			Drawable currentDrawable = view.getBackground();
			if (currentDrawable != null) {
				currentDrawable.setCallback(null);
				if (currentDrawable instanceof TiBackgroundDrawable) {
					((TiBackgroundDrawable) currentDrawable).releaseDelegate();
				}
				setBackgroundDrawable(view, null);
			}
			setBackgroundDrawable(view, background);
		}
	}
	
	private void addBorderView(){
		View rootView = getRootView();
		// Create new layout params for the child view since we just want the
		// wrapper to control the layout
		LayoutParams params = new LayoutParams();
		params.height = ViewGroup.LayoutParams.MATCH_PARENT;
		params.width = ViewGroup.LayoutParams.MATCH_PARENT;
		// If the view already has a parent, we need to detach it from the parent
		// and add the borderView to the parent as the child
		ViewGroup savedParent = null;
		int savedIndex = 0;
		if (rootView.getParent() != null) {
			ViewParent nativeParent = rootView.getParent();
			if (nativeParent instanceof ViewGroup) {
				savedParent = (ViewGroup) nativeParent;
				savedIndex = savedParent.indexOfChild(rootView);
				savedParent.removeView(rootView);
			}
		}
		borderView.addView(rootView, params);
		if (savedParent != null) {
			savedParent.addView(borderView, savedIndex,getLayoutParams());
		}
		
		if ((borderView.getRadius() > 0f || hardwareAccSupported == false) && HONEYCOMB_OR_GREATER) {
			disableHWAcceleration(borderView);
		}
	}
	
	protected TiBorderWrapperView getOrCreateBorderView()
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
	}
	
	private void setBorderRadius(float radius){
		float realRadius = (new TiDimension(Float.toString(radius), TiDimension.TYPE_WIDTH)).getAsPixels(nativeView);
		getOrCreateBorderView().setRadius(realRadius);
		disableHWAcceleration();
	}
	
	private void setBorderWidth(float width){
//		float realWidth = (new TiDimension(Float.toString(width), TiDimension.TYPE_WIDTH)).getAsPixels(nativeView);
		getOrCreateBorderView().setBorderWidth(width);
	}
	
	private void setViewMask(Object mask){
		Bitmap bitmap = null;
		if (mask instanceof TiBlob) {
			bitmap = ((TiBlob)mask).getImage();
		}
		else {
			BitmapDrawable drawable = ((BitmapDrawable) TiUIHelper.buildImageDrawable(TiConvert.toString(mask), false, proxy));
			if (drawable != null) {
				bitmap = drawable.getBitmap();
			}
		}
		
		disableHWAcceleration();
		getOrCreateBorderView().setMask(bitmap);
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
		return borderView == null ? nativeView : borderView;
	}

	public View getRootView()
	{
		return nativeView;
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
				if (exclusiveTouch) {
					ViewGroup parent =  (ViewGroup)view.getParent();
					if(parent != null) {
						switch (event.getAction()) {
					    case MotionEvent.ACTION_MOVE: 
					        parent.requestDisallowInterceptTouchEvent(true);
					        break;
					    case MotionEvent.ACTION_UP:
					    case MotionEvent.ACTION_CANCEL:
					    	parent.requestDisallowInterceptTouchEvent(false);
					        break;
					    }
					}
					
				}
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
			if (touchView == null || touchView.get() != touchable) registerTouchEvents(touchable);

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
		View view = getRootView();
		if (view != null) {
			if (HONEYCOMB_OR_GREATER) {
				setAlpha(view, opacity);
			} else {
				setOpacity(view, opacity);
			}
			view.postInvalidate();
		}
	}
	
	public float getOpacity() {
		if (proxy.hasProperty(TiC.PROPERTY_OPACITY))
			return TiConvert.toFloat(proxy.getProperty(TiC.PROPERTY_OPACITY));
		return 1;
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
		if (nativeView instanceof TiCompositeLayout) {
			TiCompositeLayout layout = (TiCompositeLayout) nativeView;
			layout.setAlphaCompat(opacity);
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

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	protected void disableHWAcceleration(View view)
	{
		view.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
	}
	

	protected void disableHWAcceleration()
	{
		if (HONEYCOMB_OR_GREATER && hardwareAccSupported == true) {
			disableHWAcceleration(getOuterView());
			hardwareAccSupported = false;
		}
	}
	
	public boolean hWAccelerationDisabled(){
		return !hardwareAccSupported;
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
		ViewGroup.LayoutParams params = getOuterView().getLayoutParams();
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
		ViewGroup.LayoutParams params = getOuterView().getLayoutParams();
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
		ViewGroup.LayoutParams params = getOuterView().getLayoutParams();
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
		ViewGroup.LayoutParams params = getOuterView().getLayoutParams();
		if (params instanceof TiCompositeLayout.LayoutParams) {
			TiCompositeLayout.LayoutParams tiParams = (TiCompositeLayout.LayoutParams) params;
			tiParams.optionBottom = new TiDimension(val, TiDimension.TYPE_BOTTOM, TypedValue.COMPLEX_UNIT_PX);
		}
		view.setLayoutParams(params);
	}
	public int getTiBottom() {
		View view =  getOuterView();
		View parent =  (View)getOuterView().getParent();
		if (parent != null) {
			return parent.getMeasuredHeight() - view.getBottom();
		}
		return 0;
	}
	public void setTiLeft(int val) {
		View view =  getOuterView();
		ViewGroup.LayoutParams params = getOuterView().getLayoutParams();
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
		ViewGroup.LayoutParams params = getOuterView().getLayoutParams();
		if (params instanceof TiCompositeLayout.LayoutParams) {
			TiCompositeLayout.LayoutParams tiParams = (TiCompositeLayout.LayoutParams) params;
			tiParams.optionRight = new TiDimension(val, TiDimension.TYPE_RIGHT, TypedValue.COMPLEX_UNIT_PX);
		}
		view.setLayoutParams(params);
	}
	public int getTiRight() {
		View view =  getOuterView();
		View parent =  (View)getOuterView().getParent();
		if (parent != null) {
			return parent.getMeasuredWidth() - view.getRight();
		}
		return 0;
	}
	public void setTiCenterX(int val) {
		View view =  getOuterView();
		ViewGroup.LayoutParams params = getOuterView().getLayoutParams();
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
		ViewGroup.LayoutParams params = getOuterView().getLayoutParams();
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
//		return TiColorHelper.parseColor(TiConvert.toString(proxy.getProperty(TiC.PROPERTY_BACKGROUND_COLOR)));
		if (background == null)
		{
			return Color.TRANSPARENT;
		}
		return background.getColorForState(TiUIHelper.BACKGROUND_DEFAULT_STATE_1);
	}

	public void setTi2DMatrix(Ti2DMatrix matrix) {
		applyTransform(matrix);
		View outerView =  getOuterView();
		ViewParent viewParent = outerView.getParent();
		if (outerView.getVisibility() == View.VISIBLE && viewParent instanceof View) {
			((View) viewParent).postInvalidate();
		}
	}
	
	public Ti2DMatrix getTi2DMatrix() {
		return layoutParams.matrix;
	}
	
	public float getAnimatedRectFraction() {
		if (layoutParams instanceof AnimationLayoutParams)
			return ((AnimationLayoutParams)layoutParams).animationFraction;
		return 0.0f;
	}
	
	public void setAnimatedRectFraction(float fraction) {
		if (layoutParams instanceof AnimationLayoutParams) {
			((AnimationLayoutParams)layoutParams).animationFraction = fraction;
			View outerView =  getOuterView();
			outerView.setLayoutParams(layoutParams);
			ViewParent viewParent = outerView.getParent();
			if (outerView.getVisibility() == View.VISIBLE && viewParent instanceof View) {
				((View) viewParent).postInvalidate();
			}
		}
	}

	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void prepareAnimatorSet(TiAnimatorSet tiSet, List<Animator> list,
			HashMap options) {
		AnimatorSet set = tiSet.set();
		
		
		View view = proxy.getOuterView();
		((TiViewAnimator)tiSet).setViewProxy(proxy);
		((TiViewAnimator)tiSet).setView(view);

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

		if (parent instanceof View) {
			parentView = (View) parent;
		}
		
		if (options.containsKey(TiC.PROPERTY_WIDTH) ||
				options.containsKey(TiC.PROPERTY_HEIGHT) ||
				options.containsKey(TiC.PROPERTY_TOP) ||
				options.containsKey(TiC.PROPERTY_BOTTOM) ||
				options.containsKey(TiC.PROPERTY_LEFT) ||
				options.containsKey(TiC.PROPERTY_RIGHT) ||
				options.containsKey(TiC.PROPERTY_CENTER)) {
			
			AnimationLayoutParams animParams = new AnimationLayoutParams(layoutParams);
			animParams.startRect = new Rect(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
			animParams.animationFraction = 0.0f;
			TiConvert.fillLayout(options, animParams, false);
			
			setLayoutParams(animParams); //we need this because otherwise applying the matrix will override it :s
			view.setLayoutParams(animParams);
			ObjectAnimator anim = ObjectAnimator.ofFloat(this, "animatedRectFraction", 1.0f);
			list.add(anim);
		}
		
		if (options.containsKey(TiC.PROPERTY_TRANSFORM)) {
			Ti2DMatrix matrix = (Ti2DMatrix) options.get(TiC.PROPERTY_TRANSFORM);
			if (matrix != null && matrix.getClass().getSuperclass().equals(Ti2DMatrix.class))
			{
				matrix = new Ti2DMatrix(matrix); //case of _2DMatrixProxy
			}
			else if(matrix == null) {
				matrix = new Ti2DMatrix();
			}
			
			if (parentView instanceof FreeLayout) {
				ObjectAnimator anim = ObjectAnimator.ofObject(this, "ti2DMatrix", new Ti2DMatrixEvaluator(view), matrix);
				list.add(anim);
			}
			else {
				DecomposedType decompose = matrix.getAffineTransform(view, true).decompose();
				List<PropertyValuesHolder> propertiesList = new ArrayList<PropertyValuesHolder>();
				propertiesList.add(PropertyValuesHolder.ofFloat("translationX", (float)decompose.translateX));
				propertiesList.add(PropertyValuesHolder.ofFloat("translationY", (float)decompose.translateY));
				propertiesList.add(PropertyValuesHolder.ofFloat("rotation", (float)(decompose.angle*180/Math.PI)));
				propertiesList.add(PropertyValuesHolder.ofFloat("scaleX", (float)decompose.scaleX));
				propertiesList.add(PropertyValuesHolder.ofFloat("scaleY", (float)decompose.scaleY));
				list.add(ObjectAnimator.ofPropertyValuesHolder(AnimatorProxy.NEEDS_PROXY ?AnimatorProxy.wrap(view) : view,propertiesList.toArray(new PropertyValuesHolder[0])));
			}
		}

//		anim.setTarget(AnimatorProxy.NEEDS_PROXY ?AnimatorProxy.wrap(view) : view);
//		anim.setInterpolator(new AccelerateDecelerateInterpolator());
//		list.add(anim);
		view.postInvalidate();

	}
}
