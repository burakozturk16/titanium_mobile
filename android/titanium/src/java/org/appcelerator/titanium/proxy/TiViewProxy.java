/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package org.appcelerator.titanium.proxy;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollEventFunction;
import org.appcelerator.kroll.KrollFunction;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.KrollRuntime;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.APIMap;
import org.appcelerator.kroll.common.AsyncResult;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.kroll.common.TiMessenger;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiBaseActivity;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiDimension;
import org.appcelerator.titanium.animation.TiAnimator;
import org.appcelerator.titanium.animation.TiAnimatorSet;
import org.appcelerator.titanium.animation.TiViewAnimator;
import org.appcelerator.titanium.transition.Transition;
import org.appcelerator.titanium.transition.TransitionHelper;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiUIHelper;
import org.appcelerator.titanium.view.TiUIView;
import org.appcelerator.titanium.TiBlob;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.Animator.AnimatorListener;

import android.util.DisplayMetrics;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.AsyncTask;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ViewSwitcher;

/**
 * The parent class of view proxies.
 */
@Kroll.proxy(propertyAccessors={
	// background properties
	TiC.PROPERTY_BACKGROUND_COLOR,
	TiC.PROPERTY_BACKGROUND_IMAGE,
	TiC.PROPERTY_BACKGROUND_REPEAT,
	TiC.PROPERTY_BACKGROUND_SELECTED_COLOR,
	TiC.PROPERTY_BACKGROUND_SELECTED_IMAGE,
	TiC.PROPERTY_BACKGROUND_SELECTED_GRADIENT,
	TiC.PROPERTY_BACKGROUND_FOCUSED_COLOR,
	TiC.PROPERTY_BACKGROUND_FOCUSED_IMAGE,
	TiC.PROPERTY_BACKGROUND_FOCUSED_GRADIENT,
	TiC.PROPERTY_BACKGROUND_DISABLED_COLOR,
	TiC.PROPERTY_BACKGROUND_DISABLED_IMAGE,
	TiC.PROPERTY_BACKGROUND_DISABLED_GRADIENT,
	TiC.PROPERTY_BACKGROUND_PADDING,
	TiC.PROPERTY_BACKGROUND_GRADIENT,
	// border properties
	TiC.PROPERTY_BORDER_COLOR,
	TiC.PROPERTY_BORDER_RADIUS,
	TiC.PROPERTY_BORDER_WIDTH,

	// layout / dimension (size/width/height have custom accessors)
	TiC.PROPERTY_LEFT,
	TiC.PROPERTY_TOP,
	TiC.PROPERTY_RIGHT,
	TiC.PROPERTY_BOTTOM,
	TiC.PROPERTY_LAYOUT,
	TiC.PROPERTY_ZINDEX,
	TiC.PROPERTY_HORIZONTAL_WRAP,

	// accessibility
	TiC.PROPERTY_ACCESSIBILITY_HINT, 
	TiC.PROPERTY_ACCESSIBILITY_LABEL, 
	TiC.PROPERTY_ACCESSIBILITY_VALUE,
	TiC.PROPERTY_ACCESSIBILITY_HIDDEN,

	// others
	TiC.PROPERTY_VISIBLE,
	TiC.PROPERTY_ENABLED,
	TiC.PROPERTY_OPACITY,
	TiC.PROPERTY_TOUCH_ENABLED,
	TiC.PROPERTY_FOCUSABLE,
	TiC.PROPERTY_SOFT_KEYBOARD_ON_FOCUS,
	TiC.PROPERTY_TRANSFORM,
	TiC.PROPERTY_ANCHOR_POINT,
	TiC.PROPERTY_TOUCH_PASSTHROUGH,
	TiC.PROPERTY_CLIP_CHILDREN,
	TiC.PROPERTY_VIEW_MASK,
})
public abstract class TiViewProxy extends AnimatableProxy implements Handler.Callback
{
	private static final String TAG = "TiViewProxy";

	private static final int MSG_FIRST_ID = KrollProxy.MSG_LAST_ID + 1;

	private static final int MSG_GETVIEW = MSG_FIRST_ID + 100;
	private static final int MSG_ADD_CHILD = MSG_FIRST_ID + 102;
	private static final int MSG_REMOVE_CHILD = MSG_FIRST_ID + 103;
	private static final int MSG_BLUR = MSG_FIRST_ID + 104;
	private static final int MSG_FOCUS = MSG_FIRST_ID + 105;
	private static final int MSG_SHOW = MSG_FIRST_ID + 106;
	private static final int MSG_HIDE = MSG_FIRST_ID + 107;
	private static final int MSG_ANIMATE = MSG_FIRST_ID + 108;
	private static final int MSG_TOIMAGE = MSG_FIRST_ID + 109;
	private static final int MSG_GETSIZE = MSG_FIRST_ID + 110;
	private static final int MSG_GETRECT = MSG_FIRST_ID + 111;
	private static final int MSG_FINISH_APPLY_PROPS = MSG_FIRST_ID + 112;
	private static final int MSG_GETABSRECT = MSG_FIRST_ID + 113;
	private static final int MSG_QUEUED_ANIMATE = MSG_FIRST_ID + 114;
	private static final int MSG_TRANSITION_VIEWS = MSG_FIRST_ID + 115;
	private static final int MSG_BLUR_BACKGROUND = MSG_FIRST_ID + 116;

	protected static final int MSG_LAST_ID = MSG_FIRST_ID + 999;

	protected ArrayList<TiViewProxy> children;
	protected WeakReference<TiViewProxy> parent;

	protected TiUIView view;
	private boolean isDecorView = false;

	private AtomicBoolean batchPropertyApply = new AtomicBoolean();

	private static int defaultTransitionStyle = TransitionHelper.Types.kTransitionSwipe.ordinal();
	private static int defaultTransitionSubStyle = TransitionHelper.SubTypes.kRightToLeft.ordinal();
	
	private HashMap<String, Object> propertiesToUpdateNativeSide = null;
	protected ArrayList<HashMap> pendingTransitions;
	protected Object pendingTransitionLock;
	/**
	 * Constructs a new TiViewProxy instance.
	 * @module.api
	 */
	public TiViewProxy()
	{
		pendingTransitionLock = new Object();
		pendingTransitions = new ArrayList<HashMap>();
//		defaultValues.put(TiC.PROPERTY_BACKGROUND_REPEAT, false);
//		defaultValues.put(TiC.PROPERTY_VISIBLE, true);
//		setProperty(TiC.PROPERTY_VISIBLE, true);
//		setProperty(TiC.PROPERTY_KEEP_SCREEN_ON, false);
//		setProperty(TiC.PROPERTY_ENABLED, true);
	}

	@Override
	public void handleCreationDict(KrollDict options)
	{
		boolean needsToUpdateProps = false;
		if (options == null) {
			return;
		}
		if (options.containsKey(TiC.PROPERTY_PROPERTIES)) {
			super.handleCreationDict(options.getKrollDict(TiC.PROPERTY_PROPERTIES));
			needsToUpdateProps = true;
		}
		else {
			super.handleCreationDict(options);
		}
		if (options.containsKey(TiC.PROPERTY_CHILD_TEMPLATES) || options.containsKey(TiC.PROPERTY_EVENTS)) {
			initFromTemplate(options, this, true, true);
			if (needsToUpdateProps) {
				updateKrollObjectProperties();
				needsToUpdateProps = false;
			}
			else {
				updatePropertiesNativeSide();
			}
		}
		if (needsToUpdateProps) {
			updateKrollObjectProperties();
		}
	}


	//This handler callback is tied to the UI thread.
	public boolean handleMessage(Message msg)
	{
		switch(msg.what) {
			case MSG_GETVIEW : {
				AsyncResult result = (AsyncResult) msg.obj;
				result.setResult(handleGetView((msg.arg1 == 1),(msg.arg2 == 1)));
				return true;
			}
			case MSG_ADD_CHILD : {
				AsyncResult result = (AsyncResult) msg.obj;
				handleAdd((TiViewProxy) result.getArg(), msg.arg1);
				result.setResult(null); //Signal added.
				return true;
			}
			case MSG_REMOVE_CHILD : {
				AsyncResult result = (AsyncResult) msg.obj;
				handleRemove((TiViewProxy) result.getArg());
				result.setResult(null); //Signal removed.
				return true;
			}
			case MSG_BLUR : {
				handleBlur();
				return true;
			}
			case MSG_FOCUS : {
				handleFocus();
				return true;
			}
			case MSG_SHOW : {
				handleShow((KrollDict) msg.obj);
				return true;
			}
			case MSG_HIDE : {
				handleHide((KrollDict) msg.obj);
				return true;
			}
			case MSG_ANIMATE : {
				handleAnimate();
				return true;
			}
			case MSG_QUEUED_ANIMATE: {
				// An animation that was re-queued
				// because the view's height and width
				// were not yet known (i.e., not yet laid out)
				handleQueuedAnimate();
				return true;
			}
			case MSG_TOIMAGE: {
				AsyncResult result = (AsyncResult) msg.obj;
				result.setResult(handleToImage((Number) result.getArg()));
				return true;
			}
			case MSG_GETSIZE : {
				AsyncResult result = (AsyncResult) msg.obj;
				KrollDict d = null;
				d = new KrollDict();
				d.put(TiC.PROPERTY_X, 0);
				d.put(TiC.PROPERTY_Y, 0);
				if (view != null) {
					View v = view.getNativeView();
					if (v != null) {
						TiDimension nativeWidth = new TiDimension(v.getWidth(), TiDimension.TYPE_WIDTH);
						TiDimension nativeHeight = new TiDimension(v.getHeight(), TiDimension.TYPE_HEIGHT);

						// TiDimension needs a view to grab the window manager, so we'll just use the decorview of the current window
						View decorView = TiApplication.getAppCurrentActivity().getWindow().getDecorView();

						d.put(TiC.PROPERTY_WIDTH, nativeWidth.getAsDefault(decorView));
						d.put(TiC.PROPERTY_HEIGHT, nativeHeight.getAsDefault(decorView));
					}
				}
				if (!d.containsKey(TiC.PROPERTY_WIDTH)) {
					d.put(TiC.PROPERTY_WIDTH, 0);
					d.put(TiC.PROPERTY_HEIGHT, 0);
				}

				result.setResult(d);
				return true;
			}
			case MSG_GETRECT: {
				AsyncResult result = (AsyncResult) msg.obj;
				KrollDict d = new KrollDict();
				if (view != null) {
					View v = view.getOuterView();
					if (v != null) {
						d = TiUIHelper.getViewRectDict(v);
					}
				}
				if (!d.containsKey(TiC.PROPERTY_WIDTH)) {
					d.put(TiC.PROPERTY_WIDTH, 0);
					d.put(TiC.PROPERTY_HEIGHT, 0);
					d.put(TiC.PROPERTY_X, 0);
					d.put(TiC.PROPERTY_Y, 0);
				}

				result.setResult(d);
				return true;
			}
			case MSG_GETABSRECT: {
				AsyncResult result = (AsyncResult) msg.obj;
				KrollDict d = null;
				d = new KrollDict();
				if (view != null) {
					View v = view.getOuterView();
					if (v != null) {
						int position[] = new int[2];
						v.getLocationOnScreen(position);

						View decorView = TiApplication.getAppCurrentActivity().getWindow().getDecorView();

						DisplayMetrics dm = new DisplayMetrics();
						TiApplication.getAppCurrentActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
						
						Rect rect = new Rect();
						decorView.getWindowVisibleDisplayFrame(rect);
				        int statusHeight = rect.top;
				        
						position[1] -= statusHeight; //we remove statusbar height 

						d.put(TiC.PROPERTY_WIDTH, v.getMeasuredWidth());
						d.put(TiC.PROPERTY_HEIGHT, v.getMeasuredHeight());
						d.put(TiC.PROPERTY_X, position[0]);
						d.put(TiC.PROPERTY_Y, position[1]);
					}
				}
				if (!d.containsKey(TiC.PROPERTY_WIDTH)) {
					d.put(TiC.PROPERTY_WIDTH, 0);
					d.put(TiC.PROPERTY_HEIGHT, 0);
					d.put(TiC.PROPERTY_X, 0);
					d.put(TiC.PROPERTY_Y, 0);
				}

				result.setResult(d);
				return true;
			}
			case MSG_FINISH_APPLY_PROPS : {
				handleFinishBatchPropertyApply();
				return true;
			}
			case MSG_TRANSITION_VIEWS : {
				ArrayList<Object> args = (ArrayList<Object>)msg.obj;
				handleTransitionViews((TiViewProxy)args.get(0), (TiViewProxy)args.get(1), args.get(2));
				return true;
			}
			case MSG_BLUR_BACKGROUND : {
				handleBlurBackground((HashMap) msg.obj);
				return true;
			}
		}
		return super.handleMessage(msg);
	}

	/*
	public Context getContext()
	{
		return getActivity();
	}
	*/

	@Kroll.getProperty @Kroll.method
	public KrollDict getRect()
	{
		return (KrollDict) TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_GETRECT), getActivity());
	}

	@Kroll.getProperty @Kroll.method
	public KrollDict getAbsoluteRect()
	{
		return (KrollDict) TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_GETABSRECT), getActivity());
	}

	@Kroll.getProperty @Kroll.method
	public KrollDict getSize()
	{
		return (KrollDict) TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_GETSIZE), getActivity());
	}

	@Kroll.getProperty @Kroll.method
	public Object getWidth()
	{
		if (hasProperty(TiC.PROPERTY_WIDTH)) {
			return getProperty(TiC.PROPERTY_WIDTH);
		}

		return KrollRuntime.UNDEFINED;
	}

	@Kroll.setProperty(retain=false) @Kroll.method
	public void setWidth(Object width)
	{
		setPropertyAndFire(TiC.PROPERTY_WIDTH, width);
	}

	@Kroll.getProperty @Kroll.method
	public Object getHeight()
	{
		if (hasProperty(TiC.PROPERTY_HEIGHT)) {
			return getProperty(TiC.PROPERTY_HEIGHT);
		}

		return KrollRuntime.UNDEFINED;
	}

	@Kroll.setProperty(retain=false) @Kroll.method
	public void setHeight(Object height)
	{
		setPropertyAndFire(TiC.PROPERTY_HEIGHT, height);
	}

	@Kroll.getProperty @Kroll.method
	public Object getCenter()
	{
		Object dict = KrollRuntime.UNDEFINED;
		if (hasProperty(TiC.PROPERTY_CENTER)) {
			dict = getProperty(TiC.PROPERTY_CENTER);
		}

		return dict;
	}

	public void clearView()
	{
		if (view != null) {
			view.release();
		}
		view = null;
	}

	/**
	 * @return the TiUIView associated with this proxy.
	 * @module.api
	 */
	public TiUIView peekView()
	{
		return view;
	}

	public View getNativeView()
	{
		if (view  != null)
			return view.getNativeView();
		return null;
	}
	
	public View getFocusView()
	{
		if (view  != null)
			return view.getFocusView();
		return null;
	}
	
	
	public View getOuterView()
	{
		if (view  != null)
			return view.getOuterView();
		return null;
	}

	public View getParentViewForChild()
	{
		if (view  != null)
			return view.getParentViewForChild();
		return null;
	}

	public void setView(TiUIView view)
	{
		this.view = view;
	}

	//only for tableview magic
	public void clearViews()
	{
		this.view = null;
		//we must use getChildren because of the controls trick in TableViewRowProxy
		for (TiViewProxy child:getChildren()) { 
			child.clearViews();
		}
	}

	public TiUIView forceCreateView(boolean enableModelListener, boolean processProperties)
	{
		this.view = null;
		return getOrCreateView(enableModelListener, processProperties);
	}
	
	public TiUIView forceCreateView(boolean enableModelListener)
	{
		return forceCreateView(enableModelListener, true);
	}
 
	public TiUIView forceCreateView()
	{
		return forceCreateView(true);
	}

	/**
	 * Transfer an existing view to this view proxy.
	 * Special use in tableView. Do not use anywhere else.
	 * Called from TiTableViewRowProxyItem.java
	 * @param transferview - The view to transfer
	 * @param oldProxy - The currentProxy of the view
	 */
	public void transferView(TiUIView transferview, TiViewProxy oldProxy) {
		if(oldProxy != null) {
			oldProxy.setView(null);
			oldProxy.setModelListener(null);
		}
		view = transferview;
		modelListener = transferview;
		view.setProxy(this);
	}
	
	/**
	 * Creates or retrieves the view associated with this proxy.
	 * @return a TiUIView instance.
	 * @module.api
	 */
	public TiUIView getOrCreateView()
	{
		return getOrCreateView(true);
	}
	
	public TiUIView getOrCreateView(boolean enableModelListener)
	{
		return getOrCreateView(enableModelListener, true);
	}

	public TiUIView getOrCreateView(boolean enableModelListener, boolean processProperties)
	{
		if (activity == null || view != null) {
			return view;
		}

		if (TiApplication.isUIThread()) {
			return handleGetView(enableModelListener, processProperties);
		}

		return (TiUIView) TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_GETVIEW, (enableModelListener) ? 1 : 0, (processProperties) ? 1 : 0), 0);
	}

	protected TiUIView handleGetView(boolean enableModelListener)
	{
		return handleGetView(enableModelListener, true);
	}

	protected TiUIView handleGetView(boolean enableModelListener, boolean processProperties)
	{
		Activity activity = getActivity();
		if (view == null && activity != null) {
			if (Log.isDebugModeEnabled()) {
				Log.d(TAG, "getView: " + getClass().getSimpleName(), Log.DEBUG_MODE);
			}

			view = createView(activity);
			if (isDecorView) {
				if (activity != null) {
					((TiBaseActivity)activity).setViewProxy(view.getProxy());
				} else {
					Log.w(TAG, "Activity is null", Log.DEBUG_MODE);
				}
			}
			realizeViews(view, enableModelListener, processProperties);
			if (processProperties == false && enableModelListener == false) {
				view.registerForTouch();
				view.registerForKeyPress();
			}
		}
		return view;
	}
	
	protected TiUIView handleGetView()
	{
		return handleGetView(true);
	}
	
	public void realizeViews()
	{
		realizeViews(view, true, true);
	}

	public void realizeViews(TiUIView view, boolean enableModelListener)
	{
		realizeViews(view, enableModelListener, true);
	}

	public void realizeViews(TiUIView view, boolean enableModelListener, boolean processProperties)
	{
		if (enableModelListener)
		{
			setModelListener(view);
		}
		else if (processProperties)
		{
			// Just call processProperties() to set them on this view.
			// Note that this is done in setModelListener() when it is
			// called.
			view.processProperties(getProperties());
		}


		// Use a copy so bundle can be modified as it passes up the inheritance
		// tree. Allows defaults to be added and keys removed.
		if (children != null) {
			try {
				for (TiViewProxy p : children) {
					TiUIView cv = p.getOrCreateView(enableModelListener, processProperties);
					view.add(cv);
					if (p instanceof TiWindowProxy && !((TiWindowProxy)p).isOpenedOrOpening()) {
						((TiWindowProxy)p).onWindowActivityCreated();
						p.focus();
					}
				}
			} catch (ConcurrentModificationException e) {
				Log.e(TAG, e.getMessage(), e);
			}
		}

		handlePendingAnimation(true);
	}
	
	public void realizeViews(TiUIView view)
	{
		realizeViews(view, true);
	}
	
	public void reloadProperties()
	{
		view.processProperties(getProperties());
		// Use a copy so bundle can be modified as it passes up the inheritance
		// tree. Allows defaults to be added and keys removed.
		if (children != null) {
			try {
				for (TiViewProxy p : children) {
					p.reloadProperties();
				}
			} catch (ConcurrentModificationException e) {
				Log.e(TAG, e.getMessage(), e);
			}
		}
	}

	public void releaseViews(boolean activityFinishing)
	{
		if (view != null) {
			view.blur();
			if  (children != null) {
				for (TiViewProxy p : children) {
					p.releaseViews(activityFinishing);
				}
			}
			view.release();
			view = null;
		}
		setModelListener(null);
		KrollRuntime.suggestGC();
	}
	
	protected void addPropToUpdateNativeSide(String key, Object value) 
	{
		if (propertiesToUpdateNativeSide == null) 
		{
			propertiesToUpdateNativeSide = new HashMap<String, Object>();
		}
		propertiesToUpdateNativeSide.put(key, value);
	}
	
	public void updatePropertiesNativeSide() 
	{
		if (propertiesToUpdateNativeSide != null) 
		{
			updateKrollObjectProperties(propertiesToUpdateNativeSide);
			propertiesToUpdateNativeSide = null;
		}
	}
	
	
	protected void addBinding(String bindId, TiViewProxy bindingProxy)
	{
		if (bindId == null) return;
		setProperty(bindId, bindingProxy);
		addPropToUpdateNativeSide(bindId, bindingProxy);
	}
	
	@SuppressWarnings("unchecked")
	protected void initFromTemplate(HashMap template_,
			TiViewProxy rootProxy, boolean updateKrollProperties, boolean recursive) {
		if (rootProxy != null) {
			rootProxy.addBinding(TiConvert.toString(template_, TiC.PROPERTY_BIND_ID),this);
		}
		if (recursive && template_.containsKey(TiC.PROPERTY_CHILD_TEMPLATES)) {
			Object childProperties = template_
					.get(TiC.PROPERTY_CHILD_TEMPLATES);
			if (childProperties instanceof Object[]) {
				Object[] propertiesArray = (Object[]) childProperties;
				for (int i = 0; i < propertiesArray.length; i++) {
					Object childDict = propertiesArray[i];
					if (childDict instanceof TiViewProxy) {
						this.add((TiViewProxy) childDict);
					} else {
						TiViewProxy childProxy = createViewFromTemplate(
								(HashMap) childDict, rootProxy, updateKrollProperties);
						if (childProxy != null){
							if (updateKrollProperties) childProxy.updateKrollObjectProperties();
							this.add(childProxy);
						}
					}
				}
			}
		}
		if (template_.containsKey(TiC.PROPERTY_EVENTS)) {
			Object events = template_
					.get(TiC.PROPERTY_EVENTS);
			if (events instanceof HashMap) {
				Iterator entries = ((HashMap)events).entrySet().iterator();
				while (entries.hasNext()) {
				    Map.Entry entry = (Map.Entry) entries.next();
				    String key = (String)entry.getKey();
				    Object value = entry.getValue();
				    if (value instanceof KrollFunction) {
						addEventListener(key, new KrollEventFunction(getKrollObject(), (KrollFunction) value));
				    }
				}
			}
		}
	}
	
	public static TiViewProxy createViewFromTemplate(HashMap template_,
			TiViewProxy rootProxy, boolean updateKrollProperties) {
		return createViewFromTemplate(template_, rootProxy, updateKrollProperties, true);
	}
	@SuppressWarnings("unchecked")
	public static TiViewProxy createViewFromTemplate(HashMap template_,
			TiViewProxy rootProxy, boolean updateKrollProperties, boolean recursive) {
		String type = TiConvert.toString(template_, TiC.PROPERTY_TYPE,
				"Ti.UI.View");
		Object properties = (template_.containsKey(TiC.PROPERTY_PROPERTIES)) ? template_
				.get(TiC.PROPERTY_PROPERTIES) : template_;
		try {
			Class<? extends KrollProxy> cls = (Class<? extends KrollProxy>) Class
					.forName(APIMap.getProxyClass(type));
			TiViewProxy proxy = (TiViewProxy) KrollProxy.createProxy(cls, null,
					new Object[] { properties }, null);
			if (proxy == null)
				return null;
			proxy.initFromTemplate(template_, rootProxy, updateKrollProperties, recursive);
			if (updateKrollProperties) {
				rootProxy.updatePropertiesNativeSide();
			}
			return proxy;
		} catch (Exception e) {
			Log.e(TAG, "Error creating view from template: " + e.toString());
			return null;
		}
	}
	
	@SuppressWarnings("unchecked")
	public static TiViewProxy createTypeViewFromDict(HashMap template_,
			String type) {
		Object properties = template_.get(TiC.PROPERTY_PROPERTIES);
		try {
			Class<? extends KrollProxy> cls = (Class<? extends KrollProxy>) Class
					.forName(APIMap.getProxyClass(type));
			TiViewProxy proxy = (TiViewProxy) KrollProxy.createProxy(cls, null,
					new Object[] { properties }, null);
			if (proxy == null)
				return null;
			proxy.initFromTemplate(template_, proxy, false, true);
			return proxy;
		} catch (Exception e) {
			Log.e(TAG, "Error creating view from dict: " + e.toString());
			return null;
		}
	}

	/**
	 * Implementing classes should use this method to create and return the appropriate view.
	 * @param activity the context activity.
	 * @return a TiUIView instance.
	 * @module.api
	 */
	public abstract TiUIView createView(Activity activity);

	/**
	 * Adds a child to this view proxy.
	 * @param child The child view proxy to add.
	 * @module.api
	 */
	@Kroll.method
	public void add(Object args, @Kroll.argument(optional = true) Object index)
	{
		if (args instanceof Object[]) {
			int i = -1; // no index by default
			if (index instanceof Number) {
				i = ((Number) index).intValue();
			}
			int arrayIndex = i;
			for (Object obj : (Object[]) args) {
				add(obj, Integer.valueOf(arrayIndex));
				if (arrayIndex != -1)
					arrayIndex++;
			}
			return;
		} else if (args instanceof HashMap) {
			TiViewProxy childProxy = createViewFromTemplate((HashMap) args,
					this, true);
			if (childProxy != null) {
//				childProxy.updateKrollObjectProperties();
				add(childProxy);
			}
		} else {
			TiViewProxy child = null;
			if (args instanceof TiViewProxy)
				child = (TiViewProxy) args;
			
			if (child == null) {
				Log.e(TAG, "Add called with a null child");
				return;
			}
	
			int i = -1; // no index by default
			if (index instanceof Number) {
				i = ((Number)index).intValue();
			}
			
	
			if (children == null) {
				children = new ArrayList<TiViewProxy>();
			}
			
			if (peekView() != null) {
				if (TiApplication.isUIThread()) {
					handleAdd(child, i);
					return;
				}
	
				
				TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_ADD_CHILD, i, 0), child);
	
			} else {
				if (i >= 0) {
					children.add(i, child);
				}
				else {
					children.add(child);
				}
				child.parent = new WeakReference<TiViewProxy>(this);
			}
		}
	}

	public void add(TiViewProxy child)
	{
		add(child, Integer.valueOf(-1));
	}

	public void handleAdd(TiViewProxy child, int index)
	{
		if (index >= 0) {
			children.add(index, child);
		}
		else {
			children.add(child);
		}
		child.parent = new WeakReference<TiViewProxy>(this);
		child.setActivity(getActivity());
		if (child instanceof TiWindowProxy && !((TiWindowProxy)child).isOpenedOrOpening()) {
			TiWindowProxy childWin = (TiWindowProxy)child;
			childWin.onWindowActivityCreated();
			childWin.focus();
		}
		if (view != null) {
			
			if (this instanceof DecorViewProxy) {
				child.isDecorView = true;
			}
			TiUIView cv = child.getOrCreateView();

			view.add(cv);
		}
	}

	/**
	 * Removes a view from this view proxy, releasing the underlying native view if it exists.
	 * @param child The child to remove.
	 * @module.api
	 */
	@Kroll.method
	public void remove(TiViewProxy child)
	{
		if (child == null) {
			Log.e(TAG, "Add called with null child");
			return;
		}

		if (peekView() != null) {
			if (TiApplication.isUIThread()) {
				handleRemove(child);
				return;
			}

			TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_REMOVE_CHILD), child);

		} else {
			if (children != null) {
				children.remove(child);
				if (child.parent != null && child.parent.get() == this) {
					child.parent = null;
				}
			}
		}
	}

	/**
	 * tries to remove this view from it parent
	 * @module.api
	 */
	@Kroll.method
	public void removeFromParent()
	{
		if (parent != null) {
			getParent().remove(this);
		}
	}

	/**
	 * Removes all children views.
	 * @module.api
	 */
	@Kroll.method
	public void removeAllChildren()
	{
		if (children != null) {
			//children might be altered while we loop through it (threading)
			//so we first copy children as it was when asked to remove all children
			ArrayList<TiViewProxy> childViews = new ArrayList<TiViewProxy>();
			childViews.addAll(children);
			for (TiViewProxy child : childViews) {
				remove(child);
			}
		}
	}

	public void handleRemove(TiViewProxy child)
	{
		if (children != null) {
			children.remove(child);
			if (view != null) {
				view.remove(child.peekView());
			}
			if (child != null) {
				child.releaseViews(false);
				child.setActivity(null);
			}
		}
	}

	@Kroll.method
	public void show(@Kroll.argument(optional=true) KrollDict options)
	{
		if (TiApplication.isUIThread()) {
			handleShow(options);
		} else {
			getMainHandler().obtainMessage(MSG_SHOW, options).sendToTarget();
		}
	}

	protected void handleShow(KrollDict options)
	{
		if (view != null) {
			view.show();
		}
		setProperty(TiC.PROPERTY_VISIBLE, true);
	}

	@Kroll.method
	public void hide(@Kroll.argument(optional=true) KrollDict options)
	{
		if (TiApplication.isUIThread()) {
			handleHide(options);
		} else {
			getMainHandler().obtainMessage(MSG_HIDE, options).sendToTarget();
		}

	}

	protected void handleHide(KrollDict options)
	{
		if (view != null) {
			handlePendingAnimation(false);
			view.hide();
			setProperty(TiC.PROPERTY_VISIBLE, false);
		}
	}

	@Override
	protected TiAnimator createAnimator(){
		return new TiViewAnimator();
	}
	
	@Override
	protected void prepareAnimatorSet(TiAnimatorSet tiSet, List<Animator> list, List<Animator> listReverse,
			HashMap options) {
		if (view != null) {
			view.prepareAnimatorSet(tiSet, list, listReverse, options);
		}
	}
	
	@Override
	protected void handlePendingAnimation()
	{
		handlePendingAnimation(false);
	}
	public void handlePendingAnimation(boolean forceQueue)
	{
		if (view == null) return;
		if (pendingAnimations.size() > 0) {
			if (forceQueue || !(TiApplication.isUIThread())) {
				if (Build.VERSION.SDK_INT < TiC.API_LEVEL_HONEYCOMB) {
					// Even this very small delay can help eliminate the bug
					// whereby the animated view's parent suddenly becomes
					// transparent (pre-honeycomb). cf. TIMOB-9813.
					getMainHandler().sendEmptyMessageDelayed(MSG_ANIMATE, 10);
				} else {
					getMainHandler().sendEmptyMessage(MSG_ANIMATE);
				}
			} else {
				handleAnimate();
			}
		}
	}
	
	ViewSwitcher flipper = null;

	private boolean transitioning;
	@SuppressLint("NewApi")
	protected void handleAnimate()
	{
		
		View view = getOuterView();
		if (view == null) {
			TiAnimator pendingAnimation = null;
			synchronized (pendingAnimationLock) {
				if (pendingAnimations.size() == 0) {
					return;
				}
				pendingAnimation = pendingAnimations.remove(0);
			}
			
			pendingAnimation.applyOptions();
			((TiViewAnimator) pendingAnimation).simulateFinish(this);
			return;
		}
//		else if (view.getWidth() == 0 && view.getHeight() == 0) {
//			getMainHandler().sendEmptyMessage(MSG_QUEUED_ANIMATE);
//			return;
//		}
		super.handlePendingAnimation();
	}

	protected void handleQueuedAnimate()
	{
		handleAnimate();
	}

	@Kroll.method
	public void blur()
	{
		if (TiApplication.isUIThread()) {
			handleBlur();
		} else {
			getMainHandler().sendEmptyMessage(MSG_BLUR);
		}
	}

	protected boolean handleBlur()
	{
		if (view != null) {
			if (!view.blur()) {
				if (children != null) {
					for (TiViewProxy child : children) {
						if (child.handleBlur()) return true;
					}
				}
			} else return true;
		}
		return false;
	}

	@Kroll.method
	public void focus()
	{
		if (TiApplication.isUIThread()) {
			handleFocus();
		} else {
			getMainHandler().sendEmptyMessage(MSG_FOCUS);
		}
	}

	@Kroll.method
	public boolean focused()
	{
		if (view != null) {
			return view.hasFocus();
		}
		return false;
	}

	protected void handleFocus()
	{
		if (view != null) {
			view.focus();
		}
	}

	private class ToImageTask extends AsyncTask< Object, Void, TiBlob >
	{
		KrollFunction callback;
		KrollProxy proxy;

		@Override
		protected TiBlob doInBackground(Object... params)
		{
			callback = (KrollFunction)params[2];
			proxy = (KrollProxy)params[1];
			Number scale = (Number)params[0];
			if (TiApplication.isUIThread()) {
				return handleToImage(scale);
			} else {
				return	(TiBlob) TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_TOIMAGE), scale);
			}
		}
		/**
		 * Always invoked on UI thread.
		 */
		@Override
		protected void onPostExecute(TiBlob image)
		{
			KrollDict result = new KrollDict();
			result.put("image", image);
			this.callback.callAsync(this.proxy.getKrollObject(), new Object[] { result });
		}
	}

	@Kroll.method
	public TiBlob toImage(@Kroll.argument(optional=true) KrollFunction callback, @Kroll.argument(optional=true) Number scale)
	{
		if (scale == null) {
			scale = Float.valueOf(1.0f);
		}
		if (callback == null) {
			if (TiApplication.isUIThread()) {
				return handleToImage(scale);
			} else {
				return	(TiBlob) TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_TOIMAGE), scale);
			}
		}
		else {
			(new ToImageTask()).execute(scale, this, callback);
		}
		return null;
	}

	protected TiBlob handleToImage(Number scale)
	{
		TiUIView view = getOrCreateView();
		if (view == null) {
			return null;
		}

		return view.toImage(scale);
	}
	
	/**
	 * @return The parent view proxy of this view proxy.
	 * @module.api
	 */
	@Kroll.getProperty @Kroll.method
	public TiViewProxy getParent()
	{
		if (this.parent == null) {
			return null;
		}

		return this.parent.get();
	}

	public void setParent(TiViewProxy parent)
	{
		if (parent == null) {
			this.parent = null;
			return;
		}

		this.parent = new WeakReference<TiViewProxy>(parent);
	}

	@Override
	public KrollProxy getParentForBubbling()
	{
		return getParent();
	}

	@Override
	public void setActivity(Activity activity)
	{
		super.setActivity(activity);
		if (children != null) {
			for (TiViewProxy child : children) {
				child.setActivity(activity);
			}
		}
	}

	/**
	 * @return An array of the children view proxies of this view.
	 * @module.api
	 */
	@Kroll.getProperty @Kroll.method
	public TiViewProxy[] getChildren()
	{
		if (children == null) return new TiViewProxy[0];
		return children.toArray(new TiViewProxy[children.size()]);
	}

	@Override
	public void eventListenerAdded(String eventName, int count, KrollProxy proxy)
	{
		super.eventListenerAdded(eventName, count, proxy);
		if (eventName.equals(TiC.EVENT_CLICK) && proxy.equals(this) && count == 1 && !(proxy instanceof TiWindowProxy)) {
			if (!proxy.hasProperty(TiC.PROPERTY_TOUCH_ENABLED)
				|| TiConvert.toBoolean(proxy.getProperty(TiC.PROPERTY_TOUCH_ENABLED))) {
				setClickable(true);
			}
		}
	}

	@Override
	public void eventListenerRemoved(String eventName, int count, KrollProxy proxy)
	{
		super.eventListenerRemoved(eventName, count, proxy);
		if (eventName.equals(TiC.EVENT_CLICK) && count == 0 && proxy.equals(this) && !(proxy instanceof TiWindowProxy)) {
			if (proxy.hasProperty(TiC.PROPERTY_TOUCH_ENABLED)
				&& !TiConvert.toBoolean(proxy.getProperty(TiC.PROPERTY_TOUCH_ENABLED))) {
				setClickable(false);
			}
		}
	}

	public void setClickable(boolean clickable)
	{
		View nv = getNativeView();
		if (nv != null) {
			nv.setClickable(clickable);
		}
	}

//	@Kroll.method
//	public void addClass(Object[] classNames)
//	{
//		// This is a pretty naive implementation right now,
//		// but it will work for our current needs
//		String baseUrl = getBaseUrlForStylesheet();
//		ArrayList<String> classes = new ArrayList<String>();
//		for (Object c : classNames) {
//			classes.add(TiConvert.toString(c));
//		}
//		KrollDict options = TiApplication.getInstance().getStylesheet(baseUrl, classes, null);
//		extend(options);
//	}

	@Kroll.method @Kroll.getProperty
	public boolean getKeepScreenOn()
	{
		Boolean keepScreenOn = null;
		View nv = getNativeView();
		if (nv != null) {
			keepScreenOn = nv.getKeepScreenOn();
		}

		//Keep the proxy in the correct state
		Object current = getProperty(TiC.PROPERTY_KEEP_SCREEN_ON);
		if (current != null) {
			boolean currentValue = TiConvert.toBoolean(current);
			if (keepScreenOn == null) {
				keepScreenOn = currentValue;
			} else {
				if (currentValue != keepScreenOn) {
					setProperty(TiC.PROPERTY_KEEP_SCREEN_ON, keepScreenOn);
				} else {
					keepScreenOn = currentValue;
				}
			}
		} else {
			if (keepScreenOn == null) {
				keepScreenOn = false; // Android default
			}

			setProperty(TiC.PROPERTY_KEEP_SCREEN_ON, keepScreenOn);
		}

		return keepScreenOn;
	}

	@Kroll.method @Kroll.setProperty(retain=false)
	public void setKeepScreenOn(boolean keepScreenOn)
	{
		setPropertyAndFire(TiC.PROPERTY_KEEP_SCREEN_ON, keepScreenOn);
	}

	@Kroll.method
	public KrollDict convertPointToView(KrollDict point, TiViewProxy dest)
	{
		if (point == null) {
			throw new IllegalArgumentException("convertPointToView: point must not be null");
		}

		if (dest == null) {
			throw new IllegalArgumentException("convertPointToView: destinationView must not be null");
		}

		if (!point.containsKey(TiC.PROPERTY_X)) {
			throw new IllegalArgumentException("convertPointToView: required property \"x\" not found in point");
		}

		if (!point.containsKey(TiC.PROPERTY_Y)) {
			throw new IllegalArgumentException("convertPointToView: required property \"y\" not found in point");
		}
		
		TiUIView view = peekView();
		TiUIView destView = dest.peekView();
		
		if (view == destView)
			return point;

		// The spec says to throw an exception if x or y cannot be converted to numbers.
		// TiConvert does that automatically for us.
		int x = TiConvert.toInt(point, TiC.PROPERTY_X);
		int y = TiConvert.toInt(point, TiC.PROPERTY_Y);

		
		if (view == null) {
			Log.w(TAG, "convertPointToView: View has not been attached, cannot convert point");
			return null;
		}

		if (destView == null) {
			Log.w(TAG, "convertPointToView: DestinationView has not been attached, cannot convert point");
			return null;
		}

		View nativeView = view.getNativeView();
		View destNativeView = destView.getNativeView();
		if (nativeView == null || nativeView.getParent() == null) {
			Log.w(TAG, "convertPointToView: View has not been attached, cannot convert point");
			return null;
		}

		if (destNativeView == null || destNativeView.getParent() == null) {
			Log.w(TAG, "convertPointToView: DestinationView has not been attached, cannot convert point");
			return null;
		}

		int viewLocation[] = new int[2];
		int destLocation[] = new int[2];
		nativeView.getLocationInWindow(viewLocation);
		destNativeView.getLocationInWindow(destLocation);

		if (Log.isDebugModeEnabled()) {
			Log.d(TAG, "nativeView location in window, x: " + viewLocation[0] + ", y: " + viewLocation[1], Log.DEBUG_MODE);
			Log.d(TAG, "destNativeView location in window, x: " + destLocation[0] + ", y: " + destLocation[1], Log.DEBUG_MODE);
		}

		int pointWindowX = viewLocation[0] + x;
		int pointWindowY = viewLocation[1] + y;
	
		// Apply reverse transformation to get the original location
		float[] points = new float[] { pointWindowX - destLocation[0], pointWindowY - destLocation[1] };
		points = destView.getPreTranslationValue(points);

		KrollDict destPoint = new KrollDict();
		destPoint.put(TiC.PROPERTY_X, (int) points[0]);
		destPoint.put(TiC.PROPERTY_Y, (int) points[1]);
		return destPoint;
	}

	public boolean inBatchPropertyApply()
	{
		return batchPropertyApply.get();
	}

	@Override
	public void applyPropertiesInternal(Object arg, boolean force, boolean wait)
	{
		batchPropertyApply.set(true);
		super.applyPropertiesInternal(arg, force, true);
		if (TiApplication.isUIThread()) {
			handleFinishBatchPropertyApply();
		} else {
			getMainHandler().sendEmptyMessage(MSG_FINISH_APPLY_PROPS);
		}
		batchPropertyApply.set(false);
	}


	protected void handleFinishBatchPropertyApply()
	{
		if (view == null) return;
		if (view.iszIndexChanged()) {
			view.forceLayoutNativeView(true);
			view.setzIndexChanged(false);
		} else {
			view.forceLayoutNativeView(false);
		}
	}


	@Kroll.method
	public void hideKeyboard()
	{
		View nv = getOuterView();
		if (nv != null) {
			TiUIHelper.showSoftKeyboard(nv, false);
		}
	}
	
	public View parentViewForChild(TiViewProxy child)
	{
		return getNativeView();
	}
	
	/*
	 * Check if the two proxies are compatible outerView wise
	 */
	private boolean checkBorderProps(TiViewProxy oldProxy, TiViewProxy newProxy){
		KrollDict oldProperties = oldProxy.getProperties();
		KrollDict newProperties = newProxy.getProperties();
		boolean oldHasBorder = oldProperties.containsKeyAndNotNull(TiC.PROPERTY_BORDER_COLOR) 
				|| oldProperties.containsKeyAndNotNull(TiC.PROPERTY_BORDER_RADIUS)
				|| oldProperties.containsKeyAndNotNull(TiC.PROPERTY_BORDER_WIDTH);
		boolean newHasBorder = newProperties.containsKeyAndNotNull(TiC.PROPERTY_BORDER_COLOR) 
				|| newProperties.containsKeyAndNotNull(TiC.PROPERTY_BORDER_RADIUS)
				|| newProperties.containsKeyAndNotNull(TiC.PROPERTY_BORDER_WIDTH);

		return (oldHasBorder == newHasBorder);
	}
	
	public Boolean validateTransferToProxy(TiViewProxy newProxy, Boolean deep)
	{
		TiViewProxy oldProxy = this;
		
		if (oldProxy == newProxy) {
			return true;
		}    
		if (newProxy.getClass() != oldProxy.getClass()) {
			return false;
		}
		
		
		if (peekView() == null){
			return false;
		}
	    
		if (!checkBorderProps(oldProxy, newProxy)) {
			return false;
		}
		View ourView = oldProxy.getParent().parentViewForChild(oldProxy);
		View parentView = (View)peekView().getNativeView().getParent();
	    if (parentView != ourView)
	    {
	        return false;
	    }
		
	    if (deep) {
			try {
				TiViewProxy[] oldproxies = getChildren();
				TiViewProxy[] newproxies = newProxy.getChildren();
				if (oldproxies.length != newproxies.length) {
					return false;
				}
				for (int i = 0; i < oldproxies.length; i++) {
					TiViewProxy newSubProxy = newproxies[i];
					TiViewProxy oldSubProxy = oldproxies[i];
		            TiUIView oldview = oldSubProxy.peekView();
		            if (oldview == null){
		                return false;
		            }
					if (!oldSubProxy.validateTransferToProxy(newSubProxy, true))
						return false;
				}
			} catch (ConcurrentModificationException e) {
				Log.e(TAG, e.getMessage(), e);
				return false;
			}
		}
		return true;
	}
	
	private void handleTransitionViews(final TiViewProxy viewOut, final TiViewProxy viewIn, Object arg) {
		if ((viewOut == null && viewIn == null) || (viewOut != null && !children.contains(viewOut))){
			transitioning = false;
			handlePendingTransition();
			return;
		}

		final ViewGroup viewToAddTo = (ViewGroup) getParentViewForChild();
		
		Transition transition = TransitionHelper.transitionFromObject((arg != null)?(HashMap)arg:null, null, null);

		if (viewToAddTo != null) {
			if (viewIn!=null) viewIn.setActivity(getActivity());
			final View viewToAdd = (viewIn!=null)?viewIn.getOrCreateView().getOuterView():null;
			if (viewToAdd!=null) {
				viewToAdd.setVisibility(View.GONE);
				TiUIHelper.addView(viewToAddTo, viewToAdd, viewIn.peekView().getLayoutParams()); //make sure it s removed from its parent
			}
			final View viewToHide = (viewOut!=null)?viewOut.getOuterView():null;
			if (transition != null) {
				transition.setTargets(viewToAddTo, viewToAdd, viewToHide);

				AnimatorSet set = transition.getSet(new AnimatorListener() {
					public void onAnimationEnd(Animator arg0) {	
						if (viewIn!=null) add(viewIn);
						if (viewOut!=null) {
							viewToAddTo.removeView(viewToHide);
							remove(viewOut);
						}
						transitioning = false;
						handlePendingTransition();
					}

					public void onAnimationCancel(Animator arg0) {
						if (viewIn!=null) add(viewIn);
						if (viewOut!=null) {
							viewToAddTo.removeView(viewToHide);
							remove(viewOut);
						}
						transitioning = false;
						handlePendingTransition();
					}

					public void onAnimationRepeat(Animator arg0) {
					}

					public void onAnimationStart(Animator arg0) {
					}
				});
				set.start();
			}
			else {
				if (viewIn!=null) add(viewIn);
				if (viewOut!=null) {
					viewToAddTo.removeView(viewToHide);
					remove(viewOut);
					transitioning = false;
					handlePendingTransition();
				}
			}
			if (viewIn!=null) viewToAdd.setVisibility(View.VISIBLE);
		}
		else {
			if (viewIn!=null) add(viewIn);
			if (viewOut!=null) remove(viewOut);
			transitioning = false;
			handlePendingTransition();
		}
	}
	
	protected void handlePendingTransition()
	{
		HashMap<String, Object> pendingTransition = null;
		synchronized (pendingTransitionLock) {
			if (pendingTransitions.size() == 0) {
				return;
			}
			pendingTransition = pendingTransitions.remove(0);
		}
		
		transitionViews((TiViewProxy)pendingTransition.get("viewOut"), 
				(TiViewProxy)pendingTransition.get("viewIn"), pendingTransition.get("arg"));
	}
	
	@Kroll.method
	public void transitionViews(final TiViewProxy viewOut, final TiViewProxy viewIn, Object arg)
	{
		if (transitioning) {
			synchronized (pendingTransitionLock) {
				HashMap<String, Object> pending = new HashMap<String, Object>();
				pending.put("viewOut", viewOut);
				pending.put("viewIn", viewIn);
				pending.put("arg", arg);
				pendingTransitions.add(pending);
			}
			return;
		}
		transitioning = true;
		if (TiApplication.isUIThread()) {
			handleTransitionViews(viewOut, viewIn, arg);
		} else {
			ArrayList<Object> args = new ArrayList<Object>();
			args.add(viewOut);
			args.add(viewIn);
			args.add(arg);
			getMainHandler().obtainMessage(MSG_TRANSITION_VIEWS, args).sendToTarget();
		}
	}
	
	
	
	private TiBlob handleBlurBackground(HashMap options)
	{
		peekView().blurBackground(options);
		return null;
	}
	
	@SuppressWarnings("unchecked")
	@Kroll.method
	public void blurBackground(Object arg, @Kroll.argument(optional=true) HashMap options)
	{
		if (peekView() != null) {
			String[] properties = null;
			if (arg instanceof String) {
				properties = new String[]{(String) arg};
			}
			else if (arg instanceof Object[]) {
				properties = TiConvert.toStringArray((Object[]) arg);
			}
			if (options == null) {
				options = new KrollDict();
			}
			options.put("properties", properties);
			if (TiApplication.isUIThread()) {
				handleBlurBackground(options);
			} else {
				getMainHandler().obtainMessage(MSG_BLUR_BACKGROUND, options).sendToTarget();
			}
		} 
	}

	@Kroll.method
	public boolean containsView(TiViewProxy proxy)
	{
		if (proxy == this)return true;
		for (TiViewProxy child:getChildren()) { 
			if (child.containsView(proxy)) return true;
		}
		return false;
	}
}
