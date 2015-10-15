/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package org.appcelerator.titanium.proxy;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollFunction;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.KrollRuntime;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.AsyncResult;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.kroll.common.TiMessenger;
import org.appcelerator.kroll.common.TiMessenger.CommandNoReturn;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiBaseActivity;
import org.appcelerator.titanium.TiBlob;
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
import android.os.Handler;
import android.os.Message;
import android.os.AsyncTask;
import android.view.View;
import android.view.ViewAnimationUtils;
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
	TiC.PROPERTY_MIN_WIDTH,
	TiC.PROPERTY_MIN_HEIGHT,
	TiC.PROPERTY_MAX_WIDTH,
	TiC.PROPERTY_MAX_HEIGHT,
	TiC.PROPERTY_FULLSCREEN,

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
	TiC.PROPERTY_TRANSLATION_Z
})
public abstract class TiViewProxy extends AnimatableProxy implements Handler.Callback
{
	private static final String TAG = "TiViewProxy";

	private static final int MSG_FIRST_ID = KrollProxy.MSG_LAST_ID + 1;

	private static final int MSG_GETVIEW = MSG_FIRST_ID + 100;
	private static final int MSG_ADD_CHILD = MSG_FIRST_ID + 102;
	private static final int MSG_BLUR = MSG_FIRST_ID + 104;
	private static final int MSG_FOCUS = MSG_FIRST_ID + 105;
	private static final int MSG_SHOW = MSG_FIRST_ID + 106;
	private static final int MSG_HIDE = MSG_FIRST_ID + 107;
	private static final int MSG_ANIMATE = MSG_FIRST_ID + 108;
	private static final int MSG_TOIMAGE = MSG_FIRST_ID + 109;
	private static final int MSG_GETSIZE = MSG_FIRST_ID + 110;
	private static final int MSG_GETRECT = MSG_FIRST_ID + 111;
	private static final int MSG_GETABSRECT = MSG_FIRST_ID + 113;
	private static final int MSG_QUEUED_ANIMATE = MSG_FIRST_ID + 114;
	private static final int MSG_BLUR_BACKGROUND = MSG_FIRST_ID + 116;
	private static final int MSG_HIDE_KEYBOARD = MSG_FIRST_ID + 118;

	protected static final int MSG_LAST_ID = MSG_FIRST_ID + 999;

	protected TiUIView view;
	private boolean isDecorView = false;

//	private static int defaultTransitionStyle = TransitionHelper.Types.kTransitionSwipe.ordinal();
//	private static int defaultTransitionSubStyle = TransitionHelper.SubTypes.kRightToLeft.ordinal();
	
	protected ArrayList<HashMap> pendingTransitions;
	protected Object pendingTransitionLock;

    private boolean viewRealised = false;
	/**
	 * Constructs a new TiViewProxy instance.
	 * @module.api
	 */
	public TiViewProxy()
	{
	    super();
	    mProcessInUIThread = true;
		pendingTransitionLock = new Object();
		pendingTransitions = new ArrayList<HashMap>();
//		defaultValues.put(TiC.PROPERTY_BACKGROUND_REPEAT, false);
//		defaultValues.put(TiC.PROPERTY_VISIBLE, true);
//		setProperty(TiC.PROPERTY_VISIBLE, true);
//		setProperty(TiC.PROPERTY_KEEP_SCREEN_ON, false);
//		setProperty(TiC.PROPERTY_ENABLED, true);
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
			case MSG_BLUR : {
				handleBlur();
				return true;
			}
			case MSG_HIDE_KEYBOARD : {
				handleHideKeyboard();
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
						d.put(TiC.PROPERTY_WIDTH, nativeWidth.getAsDefault());
						d.put(TiC.PROPERTY_HEIGHT, nativeHeight.getAsDefault());
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
						Activity activity  = TiApplication.getAppCurrentActivity();
						if (activity != null) {
						    View decorView = activity.getWindow().getDecorView();

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

//			case MSG_TRANSITION_VIEWS : {
//				ArrayList<Object> args = (ArrayList<Object>)msg.obj;
//				handleTransitionViews((TiViewProxy)args.get(0), (TiViewProxy)args.get(1), args.get(2));
//				return true;
//			}
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
	
    @Kroll.getProperty
    @Kroll.method
    public boolean getTouchPassThrough() {
        if (view != null)
            return view.getTouchPassThrough();
        return false;
    }

    @Kroll.getProperty
    @Kroll.method
    public boolean getDispatchPressed() {
        if (view != null)
            return view.getDispatchPressed();
        return false;
    }

    @Kroll.getProperty
    @Kroll.method
    public boolean getPreventListViewSelection() {
        if (view != null)
            return view.getPreventListViewSelection();
        return false;
    }

    @Kroll.getProperty @Kroll.method
    public boolean getClipChildren() {
        if (view  != null)
            return view.getClipChildren();
        return false;
    }

	public void clearView()
	{
		if (view != null) {
			view.release();
		}
		view = null;
	}
	
	public boolean viewAttached() {
	    return view != null;
	}
    
    public boolean viewInitialised() {
        return viewAttached() && viewRealised;
    }
    
    public boolean viewRealised() {
        return viewRealised;
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
		for (KrollProxy child:getChildren()) { 
		    if (child instanceof TiViewProxy) {
	            ((TiViewProxy) child).clearViews();
		    }
		}
	}

	public TiUIView forceCreateView(final boolean enableModelListener, final boolean processProperties)
	{
		this.view = null;
		return getOrCreateView(enableModelListener, processProperties);
	}
	
	public TiUIView forceCreateView(final boolean enableModelListener)
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
		setModelListener(transferview, false);
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
	
	public TiUIView getOrCreateView(final boolean enableModelListener)
	{
		return getOrCreateView(enableModelListener, true);
	}

	public TiUIView getOrCreateView(final boolean enableModelListener, final boolean processProperties)
	{
	    if (activity == null) {
	        return null;
	    }
		if (view != null) {
			return view;
		}

		if (TiApplication.isUIThread()) {
			return handleGetView(enableModelListener, processProperties);
		}

		return (TiUIView) TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_GETVIEW, (enableModelListener) ? 1 : 0, (processProperties) ? 1 : 0), 0);
	}

	protected TiUIView handleGetView(final boolean enableModelListener)
	{
		return handleGetView(enableModelListener, true);
	}

	protected TiUIView handleGetView(final boolean enableModelListener, final boolean processProperties)
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
//				} else {
//					Log.w(TAG, "Activity is null", Log.DEBUG_MODE);
				}
			}
			realizeViews(view, enableModelListener, processProperties);
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

	public void realizeViews(TiUIView view, final boolean enableModelListener)
	{
		realizeViews(view, enableModelListener, true);
	}

	public void realizeViews(TiUIView view, final boolean enableModelListener, final boolean processProperties)
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
		    synchronized (children) {
		        for (KrollProxy p : children) {
                    if (p instanceof TiViewProxy) {
                        TiUIView cv = ((TiViewProxy) p).getOrCreateView(enableModelListener, processProperties);
                        view.add(cv);
                        if (p instanceof TiWindowProxy && !((TiWindowProxy)p).isOpenedOrOpening()) {
                            ((TiWindowProxy)p).onWindowActivityCreated();
                            ((TiViewProxy) p).focus();
                        }
                    }
                }
	        }
		}
		viewDidRealize(enableModelListener, processProperties);
	}
	
	protected void viewDidRealize(final boolean enableModelListener, final boolean processProperties) {
	    if (processProperties == false && enableModelListener == false) {
            view.registerForTouch();
            view.registerForKeyPress();
        }
        viewRealised  = true;
	    view.didRealize();
        handlePendingAnimation();
	}
	
	public void realizeViews(TiUIView view)
	{
		realizeViews(view, true);
	}
	
	public void releaseViews(final boolean activityFinishing)
	{
		if (view != null) {
			view.blur();
			if (children != null) {
			    synchronized (children) {
	                for (KrollProxy child : children) {
	                    if (child instanceof TiViewProxy) {
	                        ((TiViewProxy) child).releaseViews(activityFinishing);
	                    }
	                }
	            }
			}
			if (modelListener != null && modelListener.get() == view) {
                modelListener = null;
            }
			view.release();
			view = null;
			viewRealised = false;
		}
	}
	
	@Override
    public void release() {
        releaseViews(true);
        super.release();
    }
	/**
	 * Implementing classes should use this method to create and return the appropriate view.
	 * @param activity the context activity.
	 * @return a TiUIView instance.
	 * @module.api
	 */
	public abstract TiUIView createView(Activity activity);
	
	protected void handleChildAdded(final KrollProxy child, final int index) {
	    super.handleChildAdded(child, index);
	    if (peekView() != null) {
	        if (!TiApplication.isUIThread()) {
	            getActivity().runOnUiThread(new Runnable() {
	                @Override
	                public void run() {
	                    handleAdd((TiViewProxy) child, index);
	                }
	            });
	            return;
	        }
            handleAdd((TiViewProxy) child, index);
        }
    }
	
    public void handleAdd(TiViewProxy child, int index)
    {
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
            view.insertAt(cv, index);
        }
    }

	protected void handleChildRemoved(final KrollProxy child, final int index,
            final boolean shouldDetach) {
	    if (!(child instanceof TiViewProxy)) {
	        return;
	    }
	    if (!TiApplication.isUIThread()) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    handleChildRemoved(child, index, shouldDetach);
                }
            });
            return;
		}
	    if (view != null) {
            view.remove(((TiViewProxy)child).peekView());
        }
//        handleRemove((TiViewProxy) child, shouldDetach);
	    super.handleChildRemoved(child, index, shouldDetach);
    }
//
//    public void handleRemove(TiViewProxy child, final boolean shouldDetach)
//    {
//        if (view != null) {
//            view.remove(child.peekView());
//        }
//    }

	@Kroll.method
	public void show(@Kroll.argument(optional=true) KrollDict options)
	{
		setProperty(TiC.PROPERTY_VISIBLE, true);
		if (TiApplication.isUIThread()) {
			handleShow(options);
		} else {
			getMainHandler().obtainMessage(MSG_SHOW, options).sendToTarget();
		}
	}

	protected void handleShow(KrollDict options)
	{
		if (view != null) {
			if (TiC.LOLLIPOP_OR_GREATER && TiConvert.toBoolean(options, TiC.PROPERTY_ANIMATED, false)) {
				View nativeView = view.getOuterView();
				int width = nativeView.getWidth();
				int height = nativeView.getHeight();
				int radius = Math.max(width, height);
				android.animation.Animator anim = ViewAnimationUtils.createCircularReveal(nativeView, width/2, height/2, 0, radius);
				view.show();
				anim.start();
				return;
			}
			view.show();
		}
	}

	@Kroll.method
	public void hide(@Kroll.argument(optional=true) KrollDict options)
	{
		setProperty(TiC.PROPERTY_VISIBLE, false);
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
		}
	}

	@Override
	protected TiAnimatorSet createAnimator(){
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
				if (!TiC.HONEYCOMB_OR_GREATER) {
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
			((TiViewAnimator) pendingAnimation).simulateFinish(this);
			return;
		}
		else if (view.getWidth() == 0 && view.getHeight() == 0) {
			getMainHandler().sendEmptyMessage(MSG_QUEUED_ANIMATE);
			return;
		}
		peekView().forceLayoutNativeView(true);
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
				    synchronized (children) {
				        for (KrollProxy child : children) {
	                        if (child instanceof TiViewProxy) {
	                            if (((TiViewProxy) child).handleBlur()) return true;
	                        }
	                    }
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
        View nv = getNativeView();
        if (nv != null) {
            return nv.getKeepScreenOn();
        }

        return false;// Android default
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
		View ourView = ((TiViewProxy) oldProxy.getParent()).parentViewForChild(oldProxy);
		View parentView = (View)peekView().getNativeView().getParent();
	    if (parentView != ourView)
	    {
	        return false;
	    }
		
	    if (deep) {
			try {
				KrollProxy[] oldproxies = getChildren();
				KrollProxy[] newproxies = newProxy.getChildren();
				if (oldproxies.length != newproxies.length) {
					return false;
				}
				for (int i = 0; i < oldproxies.length; i++) {
					KrollProxy newSubProxy = newproxies[i];
					KrollProxy oldSubProxy = oldproxies[i];
					if (newSubProxy instanceof TiViewProxy && 
					        oldSubProxy instanceof TiViewProxy) {
					    TiUIView oldview = ((TiViewProxy) oldSubProxy).peekView();
	                    if (oldview == null){
	                        return false;
	                    }
	                    if (!((TiViewProxy) oldSubProxy).validateTransferToProxy((TiViewProxy) newSubProxy, true))
	                        return false;
					}
		            
				}
			} catch (ConcurrentModificationException e) {
				Log.e(TAG, e.getMessage(), e);
				return false;
			}
		}
		return true;
	}
	
	@SuppressWarnings("null")
    private void handleTransitionViews(final TiViewProxy viewOut, final TiViewProxy viewIn, Object arg) {
		
	    boolean viewOutIsNotChild = false;
        if (viewOut != null && children != null) {
            synchronized (children) {
                viewOutIsNotChild = !children.contains(viewOut);
            }
        }
        if ((viewOut == null && viewIn == null) || viewOutIsNotChild) {
            transitioning = false;
            handlePendingTransition();
            return;
        }

		final ViewGroup viewToAddTo = (ViewGroup) getParentViewForChild();
		
		Transition transition = TransitionHelper.transitionFromObject((arg != null)?(HashMap)arg:null, null, null);

		if (viewToAddTo != null) {
			if (viewIn!=null) viewIn.setActivity(getActivity());
			final View viewToAdd = (viewIn != null) ? viewIn.getOrCreateView().getOuterView() : null;
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
                            removeProxy(viewOut, false);
						}
						transitioning = false;
						handlePendingTransition();
					}

					public void onAnimationCancel(Animator arg0) {
						if (viewIn!=null) add(viewIn);
						if (viewOut!=null) {
							viewToAddTo.removeView(viewToHide);
							removeProxy(viewOut, false);
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
	public void transitionViews(final TiViewProxy viewOut, final TiViewProxy viewIn, @Kroll.argument(optional=true) final Object arg)
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
		runInUiThread(new CommandNoReturn() {
            @Override
            public void execute() {
                handleTransitionViews(viewOut, viewIn, arg);                
            }
        }, false);
//		if (TiApplication.isUIThread()) {
//			handleTransitionViews(viewOut, viewIn, arg);
//		} else {
//			ArrayList<Object> args = new ArrayList<Object>();
//			args.add(viewOut);
//			args.add(viewIn);
//			args.add(arg);
//			getMainHandler().obtainMessage(MSG_TRANSITION_VIEWS, args).sendToTarget();
//		}
	}
	
	
	
	private TiBlob handleBlurBackground(HashMap options)
	{
	    getOrCreateView().blurBackground(options);
		return null;
	}
	
	@SuppressWarnings("unchecked")
	@Kroll.method
	public void blurBackground(Object arg, @Kroll.argument(optional=true) HashMap options)
	{
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

	@Kroll.method
	public boolean containsView(TiViewProxy proxy)
	{
		if (proxy == this)return true;
		for (KrollProxy child:getChildren()) { 
            if (child instanceof TiViewProxy) {
                if (((TiViewProxy) child).containsView(proxy)) return true;
            }
        }
		return false;
	}

	@Kroll.method
	public void hideKeyboard()
	{
		if (TiApplication.isUIThread()) {
			handleHideKeyboard();
		} else {
			getMainHandler().sendEmptyMessage(MSG_HIDE_KEYBOARD);
		}
	}
	
	protected void handleHideKeyboard()
	{
		View nv = getOuterView();
		if (nv != null) {
			TiUIHelper.showSoftKeyboard(nv, false);
		}
	}
}
