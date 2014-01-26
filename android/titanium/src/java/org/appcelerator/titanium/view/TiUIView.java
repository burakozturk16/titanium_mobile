/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package org.appcelerator.titanium.view;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollFunction;
import org.appcelerator.kroll.KrollPropertyChange;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.KrollProxyListener;
import org.appcelerator.kroll.common.AsyncResult;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.kroll.common.TiMessenger;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiDimension;
import org.appcelerator.titanium.animation.Ti2DMatrixEvaluator;
import org.appcelerator.titanium.animation.TiAnimatorSet;
import org.appcelerator.titanium.animation.TiViewAnimator;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.AffineTransform.DecomposedType;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiImageHelper;
import org.appcelerator.titanium.util.TiRect;
import org.appcelerator.titanium.util.TiUIHelper;
import org.appcelerator.titanium.util.TiUIHelper.Shadow;
import org.appcelerator.titanium.view.TiCompositeLayout.AnimationLayoutParams;
import org.appcelerator.titanium.view.TiCompositeLayout.LayoutParams;
import org.appcelerator.titanium.TiBlob;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.ArgbEvaluator;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.animation.PropertyValuesHolder;
import com.nineoldandroids.view.ViewHelper;
import com.nineoldandroids.view.animation.AnimatorProxy;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.Bitmap.Config;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.view.ViewCompat;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.SparseArray;
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
	
	private static final int MSG_FIRST_ID = 100;
	private static final int MSG_SET_BACKGROUND = MSG_FIRST_ID + 1;
	private static final int MSG_CLEAR_FOCUS = MSG_FIRST_ID + 2;

	protected View nativeView; // Native View object

	protected TiViewProxy proxy;
	protected TiViewProxy parent;
	protected ArrayList<TiUIView> children = new ArrayList<TiUIView>();

	protected LayoutParams layoutParams;
	protected TiBackgroundDrawable background;
	
	protected KrollDict additionalEventData;
	
	protected boolean touchPassThrough = false;
	protected boolean dispatchPressed = false;
	protected boolean reusing = false;
	
	private boolean clipChildren = true;

	protected MotionEvent lastUpEvent = null;
	protected MotionEvent lastDownEvent = null;

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
	public boolean hardwareAccEnabled = true;
	protected TiTouchDelegate mTouchDelegate;
	private RectF mBorderPadding;
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
	 */
	public void add(TiUIView child)
	{
		add(child, -1);
	}

	protected void add(TiUIView child, int childIndex)
	{
		if (child != null) {
			View cv = child.getOuterView();
			if (cv != null) {
				View nv = getParentViewForChild();
				if (nv instanceof ViewGroup) {
					if (cv.getParent() == null) {
						if (childIndex != -1) {
							((ViewGroup) nv).addView(cv, childIndex, child.getLayoutParams());
						} else {
							((ViewGroup) nv).addView(cv, child.getLayoutParams());
						}
					}
					children.add(child);
					child.parent = proxy;
					TiViewProxy childProxy = child.proxy;
					if (childProxy.hasProperty(TiC.PROPERTY_CLIP_CHILDREN)) {
						boolean value = TiConvert.toBoolean(childProxy.getProperty(TiC.PROPERTY_CLIP_CHILDREN));
						if (value == false) {
							((ViewGroup) nv).setClipChildren(false);
						}
					}
				}
			}
		}
	}

	private int findChildIndex(TiUIView child)
	{
		int idxChild = -1;
		if (child != null) {
			View cv = child.getOuterView();
			if (cv != null) {
				View nv = getNativeView();
				if (nv instanceof ViewGroup) {
					idxChild = ((ViewGroup) nv).indexOfChild(cv);

				}
			}
		}
		return idxChild;
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
	
	public void setTouchDelegate(TiTouchDelegate delegate) {
        mTouchDelegate = delegate;
    }

	/**
	 * @return the view's layout params.
	 * @module.api
	 */
	public LayoutParams getLayoutParams()
	{
		return layoutParams;
	}
	
	
	public Context getContext()
	{
		if (nativeView != null)
		{
			return nativeView.getContext();
		}
		return null;
	}
	
	
	//This handler callback is tied to the UI thread.
	public boolean handleMessage(Message msg)
	{
		switch(msg.what) {
			case MSG_SET_BACKGROUND : {
				applyCustomBackground();
				return true;
			}
			case MSG_CLEAR_FOCUS : {
				AsyncResult result = (AsyncResult) msg.obj;
				handleClearFocus((View) result.getArg());
				result.setResult(null); //Signal added.
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
	
	
	protected boolean isClickable(){
		return TiConvert.toBoolean(proxy.getProperty(TiC.PROPERTY_TOUCH_ENABLED), true);
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

		doSetClickable(getTouchView(), isClickable());
		nativeView.setOnFocusChangeListener(this);
		
		
		if (background != null)
		{
//			background.setNativeView(nativeView);
			if (TiApplication.isUIThread()) {
				applyCustomBackground();
			} else {
				handler.sendEmptyMessage(MSG_SET_BACKGROUND);
			}
		}
		nativeView.setTag(this);
		if (borderView != null)
		{
			addBorderView();
		}
		
		if (HONEYCOMB_OR_GREATER && hardwareAccEnabled == false) {
			disableHWAcceleration(getOuterView());
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
	
	public void resetAnimatedParams()
	{
		if (layoutParams instanceof AnimationLayoutParams) {
			((AnimationLayoutParams)layoutParams).animationFraction = 0.0f;
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
	}
	
	public void applyAnchorPoint(Object anchorPoint)
	{
		View view = getOuterView();
		if (view != null) {
			if (anchorPoint instanceof HashMap) {
				HashMap point = (HashMap) anchorPoint;
				layoutParams.anchorX = TiConvert.toFloat(point, TiC.PROPERTY_X);
				layoutParams.anchorY = TiConvert.toFloat(point, TiC.PROPERTY_Y);
			}
			else {
				layoutParams.anchorX = layoutParams.anchorY = 0.5f;
			}
			view.setLayoutParams(layoutParams);
			ViewParent viewParent = view.getParent();
			if (view.getVisibility() == View.VISIBLE && viewParent instanceof View) {
				((View) viewParent).postInvalidate();
			}
		}
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
	
	//for listview 
	public void setReusing(boolean value)
	{
		reusing = value;
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

	public void resort()
	{
		View v = getNativeView();
		if (v instanceof TiCompositeLayout) {
			((TiCompositeLayout) v).resort();
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
			if (newValue != null) {
				layoutParams.optionLeft = TiConvert.toTiDimension(TiConvert.toString(newValue), TiDimension.TYPE_LEFT);
			} else {
				layoutParams.optionLeft = null;
			}
			layoutNativeView();
		} else if (key.equals(TiC.PROPERTY_TOP)) {
			if (newValue != null) {
				layoutParams.optionTop = TiConvert.toTiDimension(TiConvert.toString(newValue), TiDimension.TYPE_TOP);
			} else {
				layoutParams.optionTop = null;
			}
			layoutNativeView();
		} else if (key.equals(TiC.PROPERTY_CENTER)) {
			TiConvert.updateLayoutCenter(newValue, layoutParams);
			layoutNativeView();
		} else if (key.equals(TiC.PROPERTY_RIGHT)) {
			if (newValue != null) {
				layoutParams.optionRight = TiConvert.toTiDimension(TiConvert.toString(newValue), TiDimension.TYPE_RIGHT);
			} else {
				layoutParams.optionRight = null;
			}
			layoutNativeView();
		} else if (key.equals(TiC.PROPERTY_BOTTOM)) {
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
			isFocusable = TiConvert.toBoolean(newValue, false);
			registerForKeyPress(nativeView, isFocusable);
		} else if (key.equals(TiC.PROPERTY_TOUCH_ENABLED)) {
			doSetClickable(TiConvert.toBoolean(newValue));
		} else if (key.equals(TiC.PROPERTY_VISIBLE)) {
			newValue = (newValue == null) ? false : newValue;
			this.setVisibility(TiConvert.toBoolean(newValue) ? View.VISIBLE : View.INVISIBLE);
		} else if (key.equals(TiC.PROPERTY_ENABLED)) {
			boolean oldEnabled = isEnabled;
			isEnabled = TiConvert.toBoolean(newValue, true);
			if (oldEnabled != isEnabled)
			{
				setEnabled(isEnabled, true);
			}
		} else if (key.equals(TiC.PROPERTY_EXCLUSIVE_TOUCH)) {
			exclusiveTouch = TiConvert.toBoolean(newValue);
		} else if (key.startsWith(TiC.PROPERTY_BACKGROUND_PADDING)) {
			Log.i(TAG, key + " not yet implemented.");
		} else if (key.equals(TiC.PROPERTY_BACKGROUND_COLOR)) {
				TiBackgroundDrawable bgdDrawable = getOrCreateBackground();
				int color = TiConvert.toColor(newValue);
				bgdDrawable.setColorForState(TiUIHelper.BACKGROUND_DEFAULT_STATE_1, color);
				bgdDrawable.setColorForState(TiUIHelper.BACKGROUND_DEFAULT_STATE_2, color);
			getOuterView().postInvalidate();
		} else if (key.equals(TiC.PROPERTY_BACKGROUND_SELECTED_COLOR)) {
			getOrCreateBackground().setColorForState(TiUIHelper.BACKGROUND_SELECTED_STATE, TiConvert.toColor(newValue));
			getOuterView().postInvalidate();
		} else if (key.equals(TiC.PROPERTY_BACKGROUND_FOCUSED_COLOR)) {
			getOrCreateBackground().setColorForState(TiUIHelper.BACKGROUND_FOCUSED_STATE, TiConvert.toColor(newValue));
			getOuterView().postInvalidate();
		} else if (key.equals(TiC.PROPERTY_BACKGROUND_DISABLED_COLOR)) {
			getOrCreateBackground().setColorForState(TiUIHelper.BACKGROUND_DISABLED_STATE, TiConvert.toColor(newValue));
			getOuterView().postInvalidate();
		} else if (key.equals(TiC.PROPERTY_BACKGROUND_SELECTED_GRADIENT)) {
			Drawable drawable =  TiUIHelper.buildGradientDrawable((KrollDict)newValue);
			getOrCreateBackground().setGradientDrawableForState(TiUIHelper.BACKGROUND_SELECTED_STATE, drawable);
			getOuterView().postInvalidate();
		} else if (key.equals(TiC.PROPERTY_BACKGROUND_FOCUSED_GRADIENT)) {
			Drawable drawable =  TiUIHelper.buildGradientDrawable((KrollDict)newValue);
			getOrCreateBackground().setGradientDrawableForState(TiUIHelper.BACKGROUND_FOCUSED_STATE, drawable);
			getOuterView().postInvalidate();
		} else if (key.equals(TiC.PROPERTY_BACKGROUND_DISABLED_GRADIENT)) {
			Drawable drawable =  TiUIHelper.buildGradientDrawable((KrollDict)newValue);
			getOrCreateBackground().setGradientDrawableForState(TiUIHelper.BACKGROUND_DISABLED_STATE, drawable);
			getOuterView().postInvalidate();
		} else if (key.equals(TiC.PROPERTY_BACKGROUND_GRADIENT)) {
			TiBackgroundDrawable bgdDrawable = getOrCreateBackground();
			Drawable drawable =  TiUIHelper.buildGradientDrawable((KrollDict)newValue);
			bgdDrawable.setGradientDrawableForState(TiUIHelper.BACKGROUND_DEFAULT_STATE_1, drawable);
			bgdDrawable.setGradientDrawableForState(TiUIHelper.BACKGROUND_DEFAULT_STATE_2, drawable);
			getOuterView().postInvalidate();
		} else if (key.equals(TiC.PROPERTY_BACKGROUND_IMAGE)) {
			boolean repeat = proxy.getProperties().optBoolean(TiC.PROPERTY_BACKGROUND_REPEAT, false);
			setBackgroundImageDrawable(newValue, repeat, new int[][]{TiUIHelper.BACKGROUND_DEFAULT_STATE_1, TiUIHelper.BACKGROUND_DEFAULT_STATE_2});
			getOuterView().postInvalidate();
		} else if (key.equals(TiC.PROPERTY_BACKGROUND_SELECTED_IMAGE)) {
			boolean repeat = proxy.getProperties().optBoolean(TiC.PROPERTY_BACKGROUND_REPEAT, false);
			setBackgroundImageDrawable(newValue, repeat, new int[][]{TiUIHelper.BACKGROUND_SELECTED_STATE});
			getOuterView().postInvalidate();
		} else if (key.equals(TiC.PROPERTY_BACKGROUND_FOCUSED_IMAGE)) {
			boolean repeat = proxy.getProperties().optBoolean(TiC.PROPERTY_BACKGROUND_REPEAT, false);
			setBackgroundImageDrawable(newValue, repeat, new int[][]{TiUIHelper.BACKGROUND_FOCUSED_STATE});
			getOuterView().postInvalidate();
		} else if (key.equals(TiC.PROPERTY_BACKGROUND_DISABLED_IMAGE)) {
			boolean repeat = proxy.getProperties().optBoolean(TiC.PROPERTY_BACKGROUND_REPEAT, false);
			setBackgroundImageDrawable(newValue, repeat, new int[][]{TiUIHelper.BACKGROUND_DISABLED_STATE});
			getOuterView().postInvalidate();
		} else if (key.equals(TiC.PROPERTY_BACKGROUND_REPEAT)) {
			if (background != null)
				background.setImageRepeat(TiConvert.toBoolean(newValue));
			getOuterView().postInvalidate();
		} else if (key.equals(TiC.PROPERTY_BACKGROUND_OPACITY)) {
			if (background != null)
				TiUIHelper.setDrawableOpacity(background, ViewHelper.getAlpha(getNativeView())*TiConvert.toFloat(newValue, 1f));
			getOuterView().postInvalidate();
		} else if (key.equals(TiC.PROPERTY_BORDER_COLOR)) {
			TiBorderWrapperView view = getOrCreateBorderView();
			TiBackgroundDrawable drawable = view.getBorderDrawable();
			int color = TiConvert.toColor(newValue);
			drawable.setColorForState(TiUIHelper.BACKGROUND_DEFAULT_STATE_1, color);
			drawable.setColorForState(TiUIHelper.BACKGROUND_DEFAULT_STATE_2, color);
			view.postInvalidate();
		} else if (key.equals(TiC.PROPERTY_BORDER_SELECTED_COLOR)) {
			TiBorderWrapperView view = getOrCreateBorderView();
			view.getBorderDrawable().setColorForState(TiUIHelper.BACKGROUND_SELECTED_STATE, TiConvert.toColor(newValue));
			view.postInvalidate();
		} else if (key.equals(TiC.PROPERTY_BORDER_FOCUSED_COLOR)) {
			TiBorderWrapperView view = getOrCreateBorderView();
			view.getBorderDrawable().setColorForState(TiUIHelper.BACKGROUND_FOCUSED_STATE, TiConvert.toColor(newValue));
			view.postInvalidate();
		} else if (key.equals(TiC.PROPERTY_BORDER_DISABLED_COLOR)) {
			TiBorderWrapperView view = getOrCreateBorderView();
			view.getBorderDrawable().setColorForState(TiUIHelper.BACKGROUND_DISABLED_STATE, TiConvert.toColor(newValue));
			view.postInvalidate();
		} else if (key.equals(TiC.PROPERTY_BORDER_SELECTED_GRADIENT)) {
			TiBorderWrapperView view = getOrCreateBorderView();
			Drawable drawable =  TiUIHelper.buildGradientDrawable((KrollDict)newValue);
			view.getBorderDrawable().setGradientDrawableForState(TiUIHelper.BACKGROUND_SELECTED_STATE, drawable);
			view.postInvalidate();
		} else if (key.equals(TiC.PROPERTY_BORDER_FOCUSED_GRADIENT)) {
			TiBorderWrapperView view = getOrCreateBorderView();
			Drawable drawable =  TiUIHelper.buildGradientDrawable((KrollDict)newValue);
			view.getBorderDrawable().setGradientDrawableForState(TiUIHelper.BACKGROUND_FOCUSED_STATE, drawable);
			view.postInvalidate();
		} else if (key.equals(TiC.PROPERTY_BORDER_DISABLED_GRADIENT)) {
			TiBorderWrapperView view = getOrCreateBorderView();
			Drawable drawable =  TiUIHelper.buildGradientDrawable((KrollDict)newValue);
			view.getBorderDrawable().setGradientDrawableForState(TiUIHelper.BACKGROUND_DISABLED_STATE, drawable);
			view.postInvalidate();
		} else if (key.equals(TiC.PROPERTY_BORDER_GRADIENT)) {
			TiBorderWrapperView view = getOrCreateBorderView();
			TiBackgroundDrawable bgdDrawable = view.getBorderDrawable();
			Drawable drawable =  TiUIHelper.buildGradientDrawable((KrollDict)newValue);
			bgdDrawable.setGradientDrawableForState(TiUIHelper.BACKGROUND_DEFAULT_STATE_1, drawable);
			bgdDrawable.setGradientDrawableForState(TiUIHelper.BACKGROUND_DEFAULT_STATE_2, drawable);
			view.postInvalidate();
		} else if (key.equals(TiC.PROPERTY_BORDER_RADIUS)) {
			setBorderRadius(newValue);
		} else if (key.equals(TiC.PROPERTY_BORDER_WIDTH)) {
			setBorderWidth(TiUIHelper.getRawSizeOrZero(newValue));
		} else if (key.equals(TiC.PROPERTY_BORDER_PADDING)) {
			mBorderPadding = TiConvert.toPaddingRect(newValue);
			if (borderView != null) {
				borderView.setBorderPadding(mBorderPadding);
			}
		} else if (key.equals(TiC.PROPERTY_VIEW_MASK)) {
			setViewMask(newValue);
		} else if (key.equals(TiC.PROPERTY_OPACITY)) {
			setOpacity(TiConvert.toFloat(newValue, 1f));
		} else if (key.equals(TiC.PROPERTY_SOFT_KEYBOARD_ON_FOCUS)) {
			Log.w(TAG, "Focus state changed to " + TiConvert.toString(newValue) + " not honored until next focus event.",
				Log.DEBUG_MODE);
		} else if (key.equals(TiC.PROPERTY_TRANSFORM)) {
			applyTransform((Ti2DMatrix)newValue);
		} else if (key.equals(TiC.PROPERTY_ANCHOR_POINT)) {
			applyAnchorPoint(newValue);
		} else if (key.equals(TiC.PROPERTY_KEEP_SCREEN_ON)) {
			if (nativeView != null) {
				nativeView.setKeepScreenOn(TiConvert.toBoolean(newValue));
			}

		} else if (key.indexOf("accessibility") == 0 && !key.equals(TiC.PROPERTY_ACCESSIBILITY_HIDDEN)) {
			applyContentDescription();

		} else if (key.equals(TiC.PROPERTY_ACCESSIBILITY_HIDDEN)) {
			applyAccessibilityHidden(newValue);

		} else if (key.equals(TiC.PROPERTY_TOUCH_PASSTHROUGH)) {
			touchPassThrough = TiConvert.toBoolean(newValue);
		} else if (key.equals(TiC.PROPERTY_DISPATCH_PRESSED)) {
			dispatchPressed = TiConvert.toBoolean(newValue);
		} else if (key.equals(TiC.PROPERTY_CLIP_CHILDREN)) {
			clipChildren = TiConvert.toBoolean(newValue);
			View parentViewForChild = getParentViewForChild();
			if (parentViewForChild instanceof ViewGroup) {
				((ViewGroup)parentViewForChild).setClipChildren(clipChildren);
			}
			if (borderView != null) {
				borderView.setClipChildren(clipChildren);
			}
			if (!clipChildren) {
				ViewGroup parent =  (ViewGroup)getOuterView().getParent();
				parent.setClipChildren(clipChildren);
			}
			
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
	
	protected void setEnabled(View view, boolean enabled, boolean focusable, boolean setChildren) {
		view.setEnabled(enabled);
		view.setFocusable(focusable);
		if (setChildren && view instanceof ViewGroup) {
			ViewGroup group = (ViewGroup) view;
			for (int i = 0; i < group.getChildCount(); i++) {
				View child = group.getChildAt(i);
				Object tag = child.getTag();
				if (tag != null && tag instanceof TiUIView) {
					((TiUIView) tag).setEnabled(enabled, setChildren);
				} else {
					setEnabled(child, enabled, focusable, setChildren);
				}
			}
		}
	}
	
	private boolean isEnabled = true;
	private boolean isFocusable = true;
	protected void setEnabled(boolean enabled, boolean setChildren){
        setEnabled(getOuterView(), enabled && isEnabled, enabled && isFocusable, setChildren);
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
			clipChildren = TiConvert.toBoolean(d, TiC.PROPERTY_CLIP_CHILDREN);
			View parentViewForChild = getParentViewForChild();
			if (parentViewForChild instanceof ViewGroup) {
				((ViewGroup) parentViewForChild).setClipChildren(clipChildren);
			}
		}
		
		if (d.containsKey(TiC.PROPERTY_FOCUSABLE)) {
			isFocusable = TiConvert.toBoolean(d, TiC.PROPERTY_FOCUSABLE);
		}

		if (d.containsKey(TiC.PROPERTY_TOUCH_PASSTHROUGH)) {
			touchPassThrough = TiConvert.toBoolean(d, TiC.PROPERTY_TOUCH_PASSTHROUGH);
		}

		if (d.containsKey(TiC.PROPERTY_EXCLUSIVE_TOUCH)) {
			exclusiveTouch = TiConvert.toBoolean(d, TiC.PROPERTY_EXCLUSIVE_TOUCH);
		}
		
		if (!(layoutParams instanceof AnimationLayoutParams) && TiConvert.fillLayout(d, layoutParams) && getOuterView() != null) {
			getOuterView().setLayoutParams(layoutParams);
		}
		
		registerForTouch();
		registerForKeyPress();
		
		if (d.containsKey(TiC.PROPERTY_BORDER_PADDING)) {
			mBorderPadding = TiConvert.toPaddingRect(d, TiC.PROPERTY_BORDER_PADDING);
		}
		
		boolean backgroundRepeat = d.optBoolean(TiC.PROPERTY_BACKGROUND_REPEAT, false);
		
		if (d.containsKey(TiC.PROPERTY_BACKGROUND_COLOR)) {
			TiBackgroundDrawable bgdDrawable = getOrCreateBackground();
			int color = TiConvert.toColor(d, TiC.PROPERTY_BACKGROUND_COLOR);
			bgdDrawable.setDefaultColor(color);
			bgdDrawable.setColorForState(TiUIHelper.BACKGROUND_DEFAULT_STATE_1, color);
			bgdDrawable.setColorForState(TiUIHelper.BACKGROUND_DEFAULT_STATE_2, color);
		}
		
		if (d.containsKey(TiC.PROPERTY_BACKGROUND_SELECTED_COLOR)) {
			TiBackgroundDrawable bgdDrawable = getOrCreateBackground();
			int color = TiConvert.toColor(d, TiC.PROPERTY_BACKGROUND_SELECTED_COLOR);
			bgdDrawable.setColorForState(TiUIHelper.BACKGROUND_SELECTED_STATE, color);
			bgdDrawable.setColorForState(TiUIHelper.BACKGROUND_FOCUSED_STATE, color);
		}
		if (d.containsKey(TiC.PROPERTY_BACKGROUND_FOCUSED_COLOR)) {
			getOrCreateBackground().setColorForState(TiUIHelper.BACKGROUND_FOCUSED_STATE, TiConvert.toColor(d, TiC.PROPERTY_BACKGROUND_FOCUSED_COLOR));
		} 
		if (d.containsKey(TiC.PROPERTY_BACKGROUND_DISABLED_COLOR)) {
			getOrCreateBackground().setColorForState(TiUIHelper.BACKGROUND_DISABLED_STATE, TiConvert.toColor(d, TiC.PROPERTY_BACKGROUND_DISABLED_COLOR));
		}
		if (d.containsKey(TiC.PROPERTY_BACKGROUND_IMAGE)) {
			setBackgroundImageDrawable(d.get(TiC.PROPERTY_BACKGROUND_IMAGE), backgroundRepeat, new int[][]{TiUIHelper.BACKGROUND_DEFAULT_STATE_1, TiUIHelper.BACKGROUND_DEFAULT_STATE_2});
		}
		if (d.containsKey(TiC.PROPERTY_BACKGROUND_SELECTED_IMAGE)) {
			setBackgroundImageDrawable(d.get(TiC.PROPERTY_BACKGROUND_SELECTED_IMAGE), backgroundRepeat, new int[][]{TiUIHelper.BACKGROUND_SELECTED_STATE, TiUIHelper.BACKGROUND_FOCUSED_STATE});
		}
		if (d.containsKey(TiC.PROPERTY_BACKGROUND_FOCUSED_IMAGE)) {
			setBackgroundImageDrawable(d.get(TiC.PROPERTY_BACKGROUND_FOCUSED_IMAGE), backgroundRepeat, new int[][]{TiUIHelper.BACKGROUND_FOCUSED_STATE});
		}
		if (d.containsKey(TiC.PROPERTY_BACKGROUND_DISABLED_IMAGE)) {
			setBackgroundImageDrawable(d.get(TiC.PROPERTY_BACKGROUND_DISABLED_IMAGE), backgroundRepeat, new int[][]{TiUIHelper.BACKGROUND_DISABLED_STATE});
		}
		if (d.containsKey(TiC.PROPERTY_BACKGROUND_SELECTED_GRADIENT)) {
			TiBackgroundDrawable bgdDrawable = getOrCreateBackground();
			Drawable drawable =  TiUIHelper.buildGradientDrawable(d.getKrollDict(TiC.PROPERTY_BACKGROUND_SELECTED_GRADIENT));
			bgdDrawable.setGradientDrawableForState(TiUIHelper.BACKGROUND_SELECTED_STATE, drawable);
			bgdDrawable.setGradientDrawableForState(TiUIHelper.BACKGROUND_FOCUSED_STATE, drawable);
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
			bgdDrawable.setGradientDrawableForState(TiUIHelper.BACKGROUND_DEFAULT_STATE_1, drawable);
			bgdDrawable.setGradientDrawableForState(TiUIHelper.BACKGROUND_DEFAULT_STATE_2, drawable);
		}
		
		if (d.containsKey(TiC.PROPERTY_BACKGROUND_SELECTED_INNERSHADOWS)) {
			TiBackgroundDrawable bgdDrawable = getOrCreateBackground();
			Shadow[] shadows =  TiConvert.toShadowArray((Object[]) d.get(TiC.PROPERTY_BACKGROUND_SELECTED_INNERSHADOWS));
			bgdDrawable.setInnerShadowsForState(TiUIHelper.BACKGROUND_SELECTED_STATE, shadows);
			bgdDrawable.setInnerShadowsForState(TiUIHelper.BACKGROUND_FOCUSED_STATE, shadows);
		}
		if (d.containsKey(TiC.PROPERTY_BACKGROUND_FOCUSED_INNERSHADOWS)) {
			getOrCreateBackground().setInnerShadowsForState(TiUIHelper.BACKGROUND_FOCUSED_STATE, TiConvert.toShadowArray((Object[]) d.get(TiC.PROPERTY_BACKGROUND_FOCUSED_INNERSHADOWS)));
		}
		if (d.containsKey(TiC.PROPERTY_BACKGROUND_DISABLED_INNERSHADOWS)) {
			getOrCreateBackground().setInnerShadowsForState(TiUIHelper.BACKGROUND_DISABLED_STATE, TiConvert.toShadowArray((Object[]) d.get(TiC.PROPERTY_BACKGROUND_DISABLED_INNERSHADOWS)));
		} 
		if (d.containsKey(TiC.PROPERTY_BACKGROUND_INNERSHADOWS)) {
			TiBackgroundDrawable bgdDrawable = getOrCreateBackground();
			Shadow[] shadows =  TiConvert.toShadowArray((Object[]) d.get(TiC.PROPERTY_BACKGROUND_INNERSHADOWS));
			bgdDrawable.setInnerShadowsForState(TiUIHelper.BACKGROUND_DEFAULT_STATE_1, shadows);
			bgdDrawable.setInnerShadowsForState(TiUIHelper.BACKGROUND_DEFAULT_STATE_2, shadows);
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
			TiBorderWrapperView view = getOrCreateBorderView();
			TiBackgroundDrawable drawable = view.getBorderDrawable();
			int color = TiConvert.toColor(d, TiC.PROPERTY_BORDER_COLOR);
			drawable.setDefaultColor(color);
			drawable.setColorForState(TiUIHelper.BACKGROUND_DEFAULT_STATE_1, color);
			drawable.setColorForState(TiUIHelper.BACKGROUND_DEFAULT_STATE_2, color);
		}
		
		if (d.containsKey(TiC.PROPERTY_BORDER_SELECTED_COLOR)) {
			TiBorderWrapperView view = getOrCreateBorderView();
			TiBackgroundDrawable drawable = view.getBorderDrawable();
			int color = TiConvert.toColor(d, TiC.PROPERTY_BORDER_SELECTED_COLOR); 
			drawable.setColorForState(TiUIHelper.BACKGROUND_SELECTED_STATE, color);
			drawable.setColorForState(TiUIHelper.BACKGROUND_FOCUSED_STATE, color);
		}
		if (d.containsKey(TiC.PROPERTY_BORDER_FOCUSED_COLOR)) {
			TiBorderWrapperView view = getOrCreateBorderView();
			view.getBorderDrawable().setColorForState(TiUIHelper.BACKGROUND_FOCUSED_STATE, TiConvert.toColor(d, TiC.PROPERTY_BORDER_FOCUSED_COLOR));
		} 
		if (d.containsKey(TiC.PROPERTY_BORDER_DISABLED_COLOR)) {
			TiBorderWrapperView view = getOrCreateBorderView();
			view.getBorderDrawable().setColorForState(TiUIHelper.BACKGROUND_DISABLED_STATE, TiConvert.toColor(d, TiC.PROPERTY_BORDER_DISABLED_COLOR));
		}
		if (d.containsKey(TiC.PROPERTY_BORDER_SELECTED_GRADIENT)) {
			TiBorderWrapperView view = getOrCreateBorderView();
			TiBackgroundDrawable bgdDrawable = view.getBorderDrawable();
			Drawable drawable =  TiUIHelper.buildGradientDrawable(d.getKrollDict(TiC.PROPERTY_BORDER_SELECTED_GRADIENT));
			bgdDrawable.setGradientDrawableForState(TiUIHelper.BACKGROUND_SELECTED_STATE, drawable);
			bgdDrawable.setGradientDrawableForState(TiUIHelper.BACKGROUND_FOCUSED_STATE, drawable);
		}
		if (d.containsKey(TiC.PROPERTY_BORDER_FOCUSED_GRADIENT)) {
			TiBorderWrapperView view = getOrCreateBorderView();
			Drawable drawable =  TiUIHelper.buildGradientDrawable(d.getKrollDict(TiC.PROPERTY_BORDER_FOCUSED_GRADIENT));
			view.getBorderDrawable().setGradientDrawableForState(TiUIHelper.BACKGROUND_FOCUSED_STATE, drawable);
		}
		if (d.containsKey(TiC.PROPERTY_BORDER_DISABLED_GRADIENT)) {
			TiBorderWrapperView view = getOrCreateBorderView();
			Drawable drawable =  TiUIHelper.buildGradientDrawable(d.getKrollDict(TiC.PROPERTY_BORDER_DISABLED_GRADIENT));
			view.getBorderDrawable().setGradientDrawableForState(TiUIHelper.BACKGROUND_DISABLED_STATE, drawable);
		} 
		if (d.containsKey(TiC.PROPERTY_BORDER_GRADIENT)) {
			TiBorderWrapperView view = getOrCreateBorderView();
			TiBackgroundDrawable bgdDrawable = view.getBorderDrawable();
			Drawable drawable =  TiUIHelper.buildGradientDrawable(d.getKrollDict(TiC.PROPERTY_BORDER_GRADIENT));
			bgdDrawable.setGradientDrawableForState(TiUIHelper.BACKGROUND_DEFAULT_STATE_1, drawable);
			bgdDrawable.setGradientDrawableForState(TiUIHelper.BACKGROUND_DEFAULT_STATE_2, drawable);
		}
		if (d.containsKey(TiC.PROPERTY_BORDER_RADIUS)) {
			setBorderRadius(d.get(TiC.PROPERTY_BORDER_RADIUS));
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
		if (d.containsKey(TiC.PROPERTY_DISPATCH_PRESSED)) {
			dispatchPressed = TiConvert.toBoolean(d, TiC.PROPERTY_DISPATCH_PRESSED, false);
		}
		
		if (d.containsKey(TiC.PROPERTY_ENABLED) && !nativeViewNull) {
			boolean oldValue = isEnabled;
			isEnabled = TiConvert.toBoolean(d, TiC.PROPERTY_ENABLED, true);
			if (oldValue != isEnabled)
			{
				setEnabled(isEnabled, true);
			}
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
		View view = getFocusView();
		if (view != null) {
			view.requestFocus();
		}
	}

	public boolean hasFocus()
	{
		View view = getFocusView();
		if (view != null) {
			return view.hasFocus();
		}
		return false;
	}
	
	protected void handleClearFocus(View view)
	{
		view.clearFocus();
		TiUIHelper.hideSoftKeyboard(view);
	}
	
	protected void clearFocus(View view)
	{
		if (TiApplication.isUIThread()) {
			handleClearFocus(view);
		}
		else {
			TiMessenger.sendBlockingMainMessage(proxy.getMainHandler().obtainMessage(MSG_CLEAR_FOCUS), view);
		}
	}

	/**
	 * Blurs the view.
	 */
	public boolean blur()
	{
		View view = getFocusView();
		if (view != null && view.hasFocus()) {
			clearFocus(view);
			return true;
		}
		return false;
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
			TiUIHelper.removeViewFromSuperView(getOuterView());
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
	
	public void propagateChildDrawableState(View child)
	{
		int[] state = child.getDrawableState();
		if (background != null)
		{
			background.setState(state);
		}
		if (borderView != null) {
			borderView.setDrawableState(state);
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
	public static void setBackgroundDrawable(View view, Drawable drawable) {
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
			
			if (alpha < 1.0)
				TiUIHelper.setDrawableOpacity(background, alpha);
			if (proxy.hasProperty(TiC.PROPERTY_BACKGROUND_REPEAT))
				background.setImageRepeat(TiConvert.toBoolean(proxy.getProperty(TiC.PROPERTY_BACKGROUND_REPEAT)));
			if (borderView != null) {
				background.setRadius(borderView.getRadius());
			}
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
		nativeView.setTag(null);
		borderView.setTag(this);
		borderView.addView(rootView, params);
		if (savedParent != null) {
			savedParent.addView(borderView, savedIndex,getLayoutParams());
		}
		
		if ((borderView.getRadius() != null || hardwareAccEnabled == false) && HONEYCOMB_OR_GREATER) {
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
			borderView = new TiBorderWrapperView(currentActivity, proxy);
			ViewHelper.setAlpha(borderView, ViewHelper.getAlpha(nativeView));
			borderView.setVisibility(this.visibility);

			if (proxy.hasProperty(TiC.PROPERTY_CLIP_CHILDREN)) {
				boolean value = TiConvert.toBoolean(proxy.getProperty(TiC.PROPERTY_CLIP_CHILDREN));
				borderView.setClipChildren(value);
			}
			
			if(mBorderPadding != null) borderView.setBorderPadding(mBorderPadding);
			addBorderView();
		}
		return borderView;
	}
	
	private void setBorderRadius(Object value){
		float[] result = null;
		if (value instanceof Object[]) {
			result = getBorderRadius(TiConvert.toFloatArray((Object[]) value));
		}
		else if (value instanceof Number) {
			result = getBorderRadius(TiConvert.toFloat(value, 0f));
		}
		if (background != null) {
			background.setRadius(result);
		}
		getOrCreateBorderView().setRadius(result);
		disableHWAcceleration();
	}
	private float[] getBorderRadius(float radius){
		float realRadius =  radius * TiDimension.getDisplayMetrics(proxy.getActivity()).density;
		float[] result = new float[8];
		Arrays.fill(result, realRadius);
		return result;
	}
	private float[] getBorderRadius(float[] radius){
		float factor = TiDimension.getDisplayMetrics(proxy.getActivity()).density;
		float[] result = null;
		if (radius.length == 4) {
			result = new float[8];
			for (int i = 0; i < radius.length; i++) {
				result[i*2] = result[i*2+1] = radius[i] * factor;
			}
		}
		else if (radius.length == 8) 
		{
			result = new float[8];
			if (radius.length == 4) {
				for (int i = 0; i < radius.length; i++) {
					result[i] = radius[i] * factor;
				}
			}
		}
		return result;
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
		DisplayMetrics metrics = TiDimension.getDisplayMetrics(getContext());
		double density = metrics.density;
		KrollDict data = new KrollDict();
		int[] coords = new int[2];
		getTouchView().getLocationInWindow(coords);

		final double rawx = e.getRawX();
		final double rawy = e.getRawY();
		final double x = (double) rawx - coords[0];
		final double y = (double) rawy - coords[1];
		data.put(TiC.EVENT_PROPERTY_X, x / density);
		data.put(TiC.EVENT_PROPERTY_Y, y / density);
		KrollDict globalPoint = new KrollDict();
		globalPoint.put(TiC.EVENT_PROPERTY_X, rawx / density);
		globalPoint.put(TiC.EVENT_PROPERTY_Y, rawy / density);
		data.put(TiC.EVENT_PROPERTY_GLOBALPOINT, globalPoint);
		data.put(TiC.EVENT_PROPERTY_SOURCE, proxy);
		return data;
	}

//	protected KrollDict dictFromEvent(KrollDict dictToCopy){
//		KrollDict data = new KrollDict();
//		if (dictToCopy.containsKey(TiC.EVENT_PROPERTY_X)){
//			data.put(TiC.EVENT_PROPERTY_X, dictToCopy.get(TiC.EVENT_PROPERTY_X));
//		} else {
//			data.put(TiC.EVENT_PROPERTY_X, (double)0);
//		}
//		if (dictToCopy.containsKey(TiC.EVENT_PROPERTY_Y)){
//			data.put(TiC.EVENT_PROPERTY_Y, dictToCopy.get(TiC.EVENT_PROPERTY_Y));
//		} else {
//			data.put(TiC.EVENT_PROPERTY_Y, (double)0);
//		}
//		data.put(TiC.EVENT_PROPERTY_SOURCE, proxy);
//		return data;
//	}

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
	
	public View getFocusView()
	{
		return nativeView;
	}

	public void registerForTouch()
	{
		if (allowRegisterForTouch()) {
			registerForTouch(getTouchView());
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
					if (hierarchyHasListener(TiC.EVENT_PINCH)) {
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
	
							return fireEventNoCheck(TiC.EVENT_PINCH, data);
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
				boolean hasDoubleTap = hierarchyHasListener(TiC.EVENT_DOUBLE_TAP);
				boolean hasDoubleClick = hierarchyHasListener(TiC.EVENT_DOUBLE_CLICK);
				
				if (hasDoubleTap || hasDoubleClick) {
					KrollDict event = dictFromEvent(e);
					if (hasDoubleTap) fireEventNoCheck(TiC.EVENT_DOUBLE_TAP, event);
					if (hasDoubleClick) fireEventNoCheck(TiC.EVENT_DOUBLE_CLICK, event);
					return true;
				}
				return false;
			}

			@Override
			public boolean onSingleTapConfirmed(MotionEvent e)
			{
				Log.d(TAG, "TAP, TAP, TAP on " + proxy, Log.DEBUG_MODE);
				if (hierarchyHasListener(TiC.EVENT_SINGLE_TAP)) {
					return fireEventNoCheck(TiC.EVENT_SINGLE_TAP, dictFromEvent(e));
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
				if (hierarchyHasListener(TiC.EVENT_SWIPE)) {
					KrollDict data = dictFromEvent(e2);
					if (Math.abs(velocityX) > Math.abs(velocityY)) {
						data.put(TiC.EVENT_PROPERTY_DIRECTION, velocityX > 0 ? "right" : "left");
					} else {
						data.put(TiC.EVENT_PROPERTY_DIRECTION, velocityY > 0 ? "down" : "up");
					}
					return fireEventNoCheck(TiC.EVENT_SWIPE, data);
				}
				return false;
			}

			@Override
			public void onLongPress(MotionEvent e)
			{
				Log.d(TAG, "LONGPRESS on " + proxy, Log.DEBUG_MODE);

				if (hierarchyHasListener(TiC.EVENT_LONGPRESS)) {
					fireEventNoCheck(TiC.EVENT_LONGPRESS, dictFromEvent(e));
				}
			}
		});
		
		touchable.setOnTouchListener(new OnTouchListener()
		{
			int pointersDown = 0;

			public boolean onTouch(View view, MotionEvent event)
			{
				if (mTouchDelegate != null) {
					mTouchDelegate.onTouchEvent(event, TiUIView.this);
				}
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
					lastUpEvent = event;
				}

				if (event.getAction() == MotionEvent.ACTION_DOWN ) {
					lastDownEvent = event;
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
						if (hierarchyHasListener(TiC.EVENT_TWOFINGERTAP)) {
							fireEventNoCheck(TiC.EVENT_TWOFINGERTAP, dictFromEvent(event));
						}
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
			if (hierarchyHasListener(motionEvent)) {
				fireEventNoCheck(motionEvent, dictFromEvent(event));
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
		
		registerForKeyPress(v, isFocusable);
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
					if (hierarchyHasListener(TiC.EVENT_KEY_PRESSED)) {
						KrollDict data = new KrollDict();
						data.put(TiC.EVENT_PROPERTY_KEYCODE, keyCode);
						fireEventNoCheck(TiC.EVENT_KEY_PRESSED, data);
					}

					switch (keyCode) {
						case KeyEvent.KEYCODE_ENTER:
						case KeyEvent.KEYCODE_DPAD_CENTER:
							if (hierarchyHasListener(TiC.EVENT_CLICK)) {
								fireEventNoCheck(TiC.EVENT_CLICK, null);
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
	public void setOpacity(float opacity)
	{
		if (opacity < 0 || opacity > 1) {
			Log.w(TAG, "Ignoring invalid value for opacity: " + opacity);
			return;
		}
		View view = getRootView();
		View parentForChildren = getParentViewForChild();
		ViewHelper.setAlpha(view, opacity);
		if (parentForChildren != view) {
			ViewHelper.setAlpha(parentForChildren, opacity);
		}
		if (borderView != null) {
			ViewHelper.setAlpha(borderView, opacity);
		}

	}
	
	public float getOpacity() {
		return ViewHelper.getAlpha(getRootView());
	}


	public void clearOpacity(View view)
	{
		if (background != null)
			background.clearColorFilter();
	}

	public TiBlob toImage(Number scale)
	{
		float scaleValue = scale.floatValue();
		Bitmap bitmap = TiUIHelper.viewToBitmap(proxy.getProperties(), getNativeView());
		if (scaleValue != 1.0f) {
			bitmap = TiImageHelper.imageScaled(bitmap, scaleValue);
		}
		return TiBlob.blobFromImage(bitmap);
	}

	protected View getTouchView()
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
	
	public boolean getTouchPassThrough() {
		return touchPassThrough;
	}
	
	public boolean getDispatchPressed() {
		return dispatchPressed;
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
		return fireEvent(eventName, data, true, true);
	}
	
	public boolean fireEventNoCheck(String eventName, KrollDict data) {
		return fireEvent(eventName, data, true, false);
	}
	
	public boolean fireEvent(String eventName, KrollDict data, boolean bubbles) {
		return fireEvent(eventName, data, bubbles, true);
	}
	
	public boolean hasListeners(String event, boolean checkParent) {
		return proxy.hasListeners(event, checkParent);
	}
	
	public boolean hasListeners(String event) {
		return hasListeners(event, false);
	}
	
	public boolean hierarchyHasListener(String event) {
		return proxy.hierarchyHasListener(event);
	}


	public boolean fireEvent(String eventName, KrollDict data, boolean bubbles, boolean checkListeners) {
		if (checkListeners && !hasListeners(eventName, bubbles))
		{
			return false;
		}
		if (data == null && additionalEventData != null) {
			data = new KrollDict(additionalEventData);
		} else if (additionalEventData != null) {
			data.putAll(additionalEventData);
		}
		return proxy.fireEvent(eventName, data, bubbles, false);
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
		if (HONEYCOMB_OR_GREATER) {
			view.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
		}
	}
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	protected void enableHWAcceleration(View view)
	{
		if (HONEYCOMB_OR_GREATER) {
			view.setLayerType(View.LAYER_TYPE_HARDWARE, null);
		}
	}

	protected void disableHWAcceleration()
	{
		if (hardwareAccEnabled == true) {
			disableHWAcceleration(getOuterView());
			hardwareAccEnabled = false;
		}
	}
	
	protected void enableHWAcceleration()
	{
		if (hardwareAccEnabled == false) {
			enableHWAcceleration(getOuterView());
			hardwareAccEnabled = true;
		}
	}
	
	public boolean hWAccelerationDisabled(){
		return !hardwareAccEnabled;
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
	}
	
	public Ti2DMatrix getTi2DMatrix() {
		return layoutParams.matrix;
	}
	
	public float getAnimatedRectFraction() {
		float result = 0.0f;
		if (layoutParams instanceof AnimationLayoutParams) {
			result = ((AnimationLayoutParams)layoutParams).animationFraction;
		}
		return result;
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
	public void prepareAnimatorSet(TiAnimatorSet tiSet, List<Animator> list, List<Animator> listReverse,
			HashMap options) {		
		
		View view = proxy.getOuterView();
		((TiViewAnimator)tiSet).setViewProxy(proxy);
		((TiViewAnimator)tiSet).setView(view);
		boolean needsReverse = listReverse != null;
		KrollDict properties = proxy.getProperties();

		if (options.containsKey(TiC.PROPERTY_OPACITY)) {
			show();
			list.add(ObjectAnimator.ofFloat(this, "opacity",
					TiConvert.toFloat(options, TiC.PROPERTY_OPACITY, 1.0f)));
			if (needsReverse) {
				listReverse.add(ObjectAnimator.ofFloat(this, "opacity",
						TiConvert.toFloat(properties, TiC.PROPERTY_OPACITY, 1.0f)));
			}
		}

		if (options.containsKey(TiC.PROPERTY_BACKGROUND_COLOR)) {
//			if (!proxy.hasProperty(TiC.PROPERTY_BACKGROUND_COLOR)) {
//				Log.w(TAG, "Cannot animate view without a backgroundColor. View doesn't have that property. Using #00000000");
//				getNativeView().setBackgroundColor(Color.argb(0, 0, 0, 0));
//			}
			ObjectAnimator anim = ObjectAnimator.ofInt(this, "tiBackgroundColor", TiConvert.toColor(options, TiC.PROPERTY_BACKGROUND_COLOR));
			 anim.setEvaluator(new ArgbEvaluator());
			list.add(anim);
			if (needsReverse) {
				anim = ObjectAnimator.ofInt(this, "tiBackgroundColor", TiConvert.toColor(properties, TiC.PROPERTY_BACKGROUND_COLOR));
				anim.setEvaluator(new ArgbEvaluator());
				listReverse.add(anim);
			}
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
			AnimationLayoutParams animParams;
			if (layoutParams instanceof AnimationLayoutParams) {
				animParams = (AnimationLayoutParams)layoutParams;
			}
			else {
				animParams = new AnimationLayoutParams(layoutParams);
				animParams.startRect = new Rect(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
				animParams.animationFraction = 0.0f;
			}
			//fillLayout will try to reset animationFraction, here we dont want that
			float oldAnimationFraction = animParams.animationFraction;
			TiConvert.fillLayout(options, animParams, false);
			animParams.animationFraction = oldAnimationFraction;
			
			setLayoutParams(animParams); //we need this because otherwise applying the matrix will override it :s
			view.setLayoutParams(animParams);
			ObjectAnimator anim = ObjectAnimator.ofFloat(this, "animatedRectFraction", 1.0f);
			list.add(anim);
			if (needsReverse) {
				listReverse.add(ObjectAnimator.ofFloat(this, "animatedRectFraction", 0.0f));
			}
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
				Ti2DMatrixEvaluator evaluator = new Ti2DMatrixEvaluator(view);
				ObjectAnimator anim = ObjectAnimator.ofObject(this, "ti2DMatrix", evaluator, matrix);
				list.add(anim);
				if (needsReverse) {
					matrix = (Ti2DMatrix) properties.get(TiC.PROPERTY_TRANSFORM);
					if (matrix != null && matrix.getClass().getSuperclass().equals(Ti2DMatrix.class))
					{
						matrix = new Ti2DMatrix(matrix); //case of _2DMatrixProxy
					}
					else if(matrix == null) {
						matrix = new Ti2DMatrix();
					}
					listReverse.add(ObjectAnimator.ofObject(this, "ti2DMatrix", evaluator, matrix));
				}
			}
			else {
				DecomposedType decompose = matrix.getAffineTransform(view).decompose();
				List<PropertyValuesHolder> propertiesList = new ArrayList<PropertyValuesHolder>();
				propertiesList.add(PropertyValuesHolder.ofFloat("translationX", (float)decompose.translateX));
				propertiesList.add(PropertyValuesHolder.ofFloat("translationY", (float)decompose.translateY));
				propertiesList.add(PropertyValuesHolder.ofFloat("rotation", (float)(decompose.angle*180/Math.PI)));
				propertiesList.add(PropertyValuesHolder.ofFloat("scaleX", (float)decompose.scaleX));
				propertiesList.add(PropertyValuesHolder.ofFloat("scaleY", (float)decompose.scaleY));
				list.add(ObjectAnimator.ofPropertyValuesHolder(AnimatorProxy.NEEDS_PROXY ?AnimatorProxy.wrap(view) : view,propertiesList.toArray(new PropertyValuesHolder[0])));
				if (needsReverse) {
					matrix = (Ti2DMatrix) properties.get(TiC.PROPERTY_TRANSFORM);
					decompose = matrix.getAffineTransform(view).decompose();
					propertiesList = new ArrayList<PropertyValuesHolder>();
					propertiesList.add(PropertyValuesHolder.ofFloat("translationX", (float)decompose.translateX));
					propertiesList.add(PropertyValuesHolder.ofFloat("translationY", (float)decompose.translateY));
					propertiesList.add(PropertyValuesHolder.ofFloat("rotation", (float)(decompose.angle*180/Math.PI)));
					propertiesList.add(PropertyValuesHolder.ofFloat("scaleX", (float)decompose.scaleX));
					propertiesList.add(PropertyValuesHolder.ofFloat("scaleY", (float)decompose.scaleY));
					listReverse.add(ObjectAnimator.ofPropertyValuesHolder(AnimatorProxy.NEEDS_PROXY ?AnimatorProxy.wrap(view) : view,propertiesList.toArray(new PropertyValuesHolder[0])));
				}
			}
		}
		view.postInvalidate();
	}
	
	@SuppressWarnings({"rawtypes", "unchecked"})
	private class BlurTask extends AsyncTask< Object, Void, Bitmap >
	{
		KrollProxy proxy;
		HashMap options;
		KrollFunction callback;
		String[] properties;
		@Override
		protected Bitmap doInBackground(Object... params)
		{
			Bitmap bitmap = (Bitmap)params[0];
			proxy = (TiViewProxy)params[1];
			Rect rect = (Rect)params[2];
			options = (HashMap)params[3];
			bitmap = TiImageHelper.imageCropped(bitmap, new TiRect(rect));
			if (options == null) {
				options = new KrollDict();
			}
			options.put("filters", new Object[]{TiImageHelper.FilterType.kFilterBoxBlur.ordinal()});

			if (options.containsKey("callback")) {
				callback = (KrollFunction) options.get("callback");
			}
			if (options.containsKey("properties")) {
				properties = TiConvert.toStringArray((Object[]) options.get("properties"));
			}
			bitmap = TiImageHelper.imageFiltered(bitmap, options);
			return bitmap;
		}
		/**
		 * Always invoked on UI thread.
		 */
		@Override
		protected void onPostExecute(Bitmap image)
		{
			TiBlob blob = TiBlob.blobFromImage(image);
			if (properties != null) {
				for (String prop : properties) {
					proxy.setPropertyAndFire(prop, blob);
				}
			}
			if (this.callback != null)  {
				KrollDict result = new KrollDict();
				if (image != null) {
					result.put("image", blob);
				}
				this.callback.callAsync(this.proxy.getKrollObject(), new Object[] { result });
			}
		}
	}
	
	public void blurBackground(HashMap args)
	{
		long startTime = System.currentTimeMillis();
		View outerView = getOuterView();
		TiViewProxy parentProxy = getParent();
		if (outerView != null && parentProxy != null) {	
			View parentView = parentProxy.getOuterView();
			if (parentView != null) {

				int width = parentView.getWidth();
				int height = parentView.getHeight();
		
				Bitmap bitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
				Canvas canvas = new Canvas(bitmap);
				float alpha = ViewHelper.getAlpha(outerView);
				ViewHelper.setAlpha(outerView, 0.0f);
				parentView.draw(canvas);
				ViewHelper.setAlpha(outerView, alpha);

				Rect rect = new Rect(outerView.getLeft(), outerView.getTop(), outerView.getRight(), outerView.getBottom());
				
				(new BlurTask()).execute(bitmap, this.proxy, rect, args);
				long endTime   = System.currentTimeMillis();
				long totalTime = endTime - startTime;
				System.out.println(totalTime);
			} 
		}
	}
}
