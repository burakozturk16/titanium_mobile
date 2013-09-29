/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

package ti.modules.titanium.ui;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollFunction;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.AsyncResult;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.kroll.common.TiMessenger;
import org.appcelerator.titanium.TiActivity;
import org.appcelerator.titanium.TiActivityWindow;
import org.appcelerator.titanium.TiActivityWindows;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiBaseActivity;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiLifecycle.OnLifecycleEvent;
import org.appcelerator.titanium.TiLifecycle.interceptOnHomePressedEvent;
import org.appcelerator.titanium.TiTranslucentActivity;
import org.appcelerator.titanium.TiLifecycle.interceptOnBackPressedEvent;
import org.appcelerator.titanium.TiWindowManager;
import org.appcelerator.titanium.proxy.ActionBarProxy;
import org.appcelerator.titanium.proxy.ActivityProxy;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.proxy.TiWindowProxy;
import org.appcelerator.titanium.transition.Transition;
import org.appcelerator.titanium.transition.TransitionHelper;
import org.appcelerator.titanium.transition.TransitionInAndOut;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiUIHelper;
import org.appcelerator.titanium.view.TiUIView;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.Animator.AnimatorListener;
import com.nineoldandroids.animation.AnimatorSet;

import ti.modules.titanium.ui.transitionstyle.TransitionStyleModule;
import ti.modules.titanium.ui.widget.TiView;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Message;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;

import android.view.ViewGroup;

@SuppressLint({ "ValidFragment", "NewApi" })
@Kroll.proxy(creatableInModule=UIModule.class, propertyAccessors={
	TiC.PROPERTY_MODAL,
	TiC.PROPERTY_WINDOW,
	TiC.PROPERTY_ACTIVITY,
	TiC.PROPERTY_URL,
	TiC.PROPERTY_WINDOW_PIXEL_FORMAT
})
public class NavigationWindowProxy extends WindowProxy implements OnLifecycleEvent, TiActivityWindow, interceptOnBackPressedEvent, TiWindowManager, interceptOnHomePressedEvent
{
	private static final String TAG = "NavigationWindowProxy";
	private static final String PROPERTY_POST_WINDOW_CREATED = "postWindowCreated";

	private static final int MSG_FIRST_ID = TiViewProxy.MSG_LAST_ID + 1;
	private static final int MSG_SET_PIXEL_FORMAT = MSG_FIRST_ID + 100;
	private static final int MSG_SET_TITLE = MSG_FIRST_ID + 101;
	private static final int MSG_SET_WIDTH_HEIGHT = MSG_FIRST_ID + 102;
	private static final int MSG_PUSH = MSG_FIRST_ID + 103;
	private static final int MSG_POP = MSG_FIRST_ID + 104;
	protected static final int MSG_LAST_ID = MSG_FIRST_ID + 999;

	private static int defaultTransitionStyle = TransitionStyleModule.SWIPE;
	private static int defaultTransitionSubStyle = TransitionStyleModule.RIGHT_TO_LEFT;

	private WeakReference<TiBaseActivity> windowActivity;
	ArrayList<TiWindowProxy> windows = new ArrayList<TiWindowProxy>();
	HashMap<TiWindowProxy, Transition> animations = new HashMap<TiWindowProxy, Transition>();

	public NavigationWindowProxy()
	{
		super();
		defaultValues.put(TiC.PROPERTY_WINDOW_PIXEL_FORMAT, PixelFormat.UNKNOWN);
	}

	@Override
	protected KrollDict getLangConversionTable()
	{
		KrollDict table = new KrollDict();
		table.put(TiC.PROPERTY_TITLE, TiC.PROPERTY_TITLEID);
		return table;
	}

//	@Override
//	public TiUIView createView(Activity activity)
//	{
//		TiUIView v = new TiView(this);
//		v.getLayoutParams().autoFillsHeight = true;
//		v.getLayoutParams().autoFillsWidth = true;
//		setView(v);
//		return v;
//	}

	

	@Override
	public void open(@Kroll.argument(optional = true) Object arg)throws IllegalArgumentException
	{
		
		if (!hasProperty(TiC.PROPERTY_WINDOW) || !(getProperty(TiC.PROPERTY_WINDOW) instanceof WindowProxy)) {
			throw new IllegalArgumentException("You must set a 'window' property");
		}
		HashMap<String, Object> option = null;
		if (arg instanceof HashMap) {
			option = (HashMap<String, Object>) arg;
		}
		if (option != null) {
			properties.putAll(option);
		}

		if (hasProperty(TiC.PROPERTY_ORIENTATION_MODES)) {
			Object obj = getProperty(TiC.PROPERTY_ORIENTATION_MODES);
			if (obj instanceof Object[]) {
				orientationModes = TiConvert.toIntArray((Object[]) obj);
			}
		}

		super.open(arg);
	}

	@Override
	public void close(@Kroll.argument(optional = true) Object arg)
	{
		if (!(opened || opening)) {
			return;
		}
		super.close(arg);
	}
	
	@Override
	public boolean interceptOnHomePressed() {
		ActivityProxy activityProxy = getActivityProxy();
		if (activityProxy != null) {
			ActionBarProxy actionBarProxy = activityProxy.getActionBar();
			if (actionBarProxy != null) {
				KrollFunction onHomeIconItemSelected = (KrollFunction) actionBarProxy
					.getProperty(TiC.PROPERTY_ON_HOME_ICON_ITEM_SELECTED);
				if (onHomeIconItemSelected != null) {
					KrollDict event = new KrollDict();
					event.put(TiC.EVENT_PROPERTY_SOURCE, actionBarProxy);
					event.put(TiC.EVENT_PROPERTY_WINDOW, ((TiBaseActivity) getActivity()).getWindowProxy());
					onHomeIconItemSelected.call(activityProxy.getKrollObject(), new Object[] { event });
					return true;
				}
			}
		}
		if (!pushing && !poping)
			popCurrentWindow(null);
		return true;
	}
	
	private void updateHomeButton(TiWindowProxy proxy){
		boolean canGoBack = (windows.size() > 1);
    	ActionBarProxy actionBarProxy = getActivityProxy().getActionBar();
    	ActionBar actionBar = getActivity().getActionBar();
    	if (proxy == null) {
    		actionBar.setDisplayHomeAsUpEnabled(canGoBack);
    		actionBar.setHomeButtonEnabled(canGoBack);
    	}
    	else {
    		KrollDict props = actionBarProxy.getProperties();
    		actionBar.setDisplayHomeAsUpEnabled(props.optBoolean(TiC.PROPERTY_DISPLAY_HOME_AS_UP, canGoBack));
    		actionBar.setHomeButtonEnabled(canGoBack || props.get(TiC.PROPERTY_ON_HOME_ICON_ITEM_SELECTED) != null);
    	}
	}
	
	private void removeWindow(TiWindowProxy proxy) {
		proxy.setWindowManager(null);
		windows.remove(proxy);
		animations.remove(proxy);
	}
	private void addWindow(TiWindowProxy proxy, Transition transition) {
		if (!windows.contains(proxy)) {
			proxy.setWindowManager(this);
			windows.add(proxy);
		}
		if (transition != null) animations.put(proxy, transition);
	}
	
	public TiWindowProxy getCurrentWindow()
	{
		int size = windows.size();
		if (size > 0) return windows.get(size - 1);
		return this;
	}
	
	public TiWindowProxy popWindow()
	{
		int size = windows.size();
		if (size > 0) return windows.remove(size - 1);
		return null;
	}
	

	private boolean pushing = false;
	private boolean poping = false;
	
	public boolean popWindow(TiWindowProxy proxy, Object arg)
	{
		int index = windows.indexOf(proxy);
		if (index <= 0) {
			poping = false;		
			return true;
		}
		if (proxy == getCurrentWindow()) 
		{
			return popCurrentWindow(arg);
		}
		else {
			removeWindow(proxy);
			ViewGroup viewToRemoveFrom = (ViewGroup) getParentViewForChild();
			View viewToRemove = proxy.getOuterView();
			
			if (viewToRemove != null) {
				viewToRemoveFrom.removeView(proxy.getOuterView());
			}
			proxy.closeFromActivity(false);
			proxy.setActivity(null);
			poping = false;
			return true;
		}
	}
	public boolean popCurrentWindow(Object arg)
	{
		TiBaseActivity activity = ((TiBaseActivity) getActivity());
		 if (windows.size() > 1) //we dont pop the window 0!
		 {
			final TiWindowProxy toRemove = popWindow();
			TiWindowProxy winToFocus = getCurrentWindow();
			
			int transitionStyle = -1;
			Transition transition = null;
			if (animations.containsKey(toRemove)) {
				transition = animations.get(toRemove);
				transitionStyle = transition.getType();
			}
			KrollDict options = null;
			if (arg != null && arg instanceof HashMap<?, ?>) {
					options = new KrollDict((HashMap<String, Object>) arg);
			} else {
				options = new KrollDict();
			}
			boolean animated = options.optBoolean(TiC.PROPERTY_ANIMATED, true);
			int  optTransitionStyle = options.optInt(TiC.PROPERTY_TRANSITION_STYLE, -1);
			int  optTransitionSubStyle = options.optInt(TiC.PROPERTY_TRANSITION_SUBSTYLE, defaultTransitionSubStyle);
			int duration = options.optInt(TiC.PROPERTY_TRANSITION_DURATION, -1);
			if ((optTransitionStyle != -1 || duration != -1) && animated) {
				if (optTransitionStyle == -1) optTransitionStyle = transitionStyle;
				transition = TransitionHelper.transitionForType(optTransitionStyle, optTransitionSubStyle, duration);
			}
			
			
			final ViewGroup viewToRemoveFrom = (ViewGroup) getParentViewForChild();
			
			if (viewToRemoveFrom != null) {
				final View viewToRemove = toRemove.getOuterView();
				final View viewToFocus = winToFocus.getOuterView();
				viewToFocus.setVisibility(View.GONE);
				TiUIHelper.addView(viewToRemoveFrom, viewToFocus, winToFocus.peekView().getLayoutParams());
				if (transition != null && animated) {
					transition.setTargetsForReversed(viewToFocus, viewToRemove);
					AnimatorSet set = transition.getReversedSet(new AnimatorListener() {
						@Override
						public void onAnimationStart(Animator arg0) {	
						}
						
						@Override
						public void onAnimationRepeat(Animator arg0) {							
						}
						
						@Override
						public void onAnimationEnd(Animator arg0) {	
							poping = false;
							viewToRemoveFrom.removeView(viewToRemove);
							toRemove.closeFromActivity(false);
							toRemove.setActivity(null);
						}

						@Override
						public void onAnimationCancel(Animator arg0) {		
							poping = false;
							viewToRemoveFrom.removeView(viewToRemove);
							toRemove.closeFromActivity(false);
							toRemove.setActivity(null);
						}
					});
					set.start();
				}
				else {
					poping = false;
					viewToRemoveFrom.removeView(viewToRemove);
					toRemove.closeFromActivity(false);
					toRemove.setActivity(null);
				}
				viewToFocus.setVisibility(View.VISIBLE);
			}
			
	    	removeWindow(toRemove);
	    	
			activity.setWindowProxy(winToFocus);
	    	updateHomeButton(winToFocus);
			winToFocus.focus();

			return true;
	    }
		return false;
	}
	
	@Override
	protected void handleOpen(KrollDict options)
	{
		Activity topActivity = TiApplication.getAppCurrentActivity();
		Intent intent = new Intent(topActivity, TiActivity.class);
		fillIntent(topActivity, intent);

		int windowId = TiActivityWindows.addWindow(this);
		intent.putExtra(TiC.INTENT_PROPERTY_USE_ACTIVITY_WINDOW, true);
		intent.putExtra(TiC.INTENT_PROPERTY_WINDOW_ID, windowId);

		boolean animated = TiConvert.toBoolean(options, TiC.PROPERTY_ANIMATED, true);
		if (!animated) {
			intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
			topActivity.startActivity(intent);
			topActivity.overridePendingTransition(0, 0); // Suppress default transition.
		} else if (options.containsKey(TiC.INTENT_PROPERTY_ENTER_ANIMATION)
			|| options.containsKey(TiC.INTENT_PROPERTY_EXIT_ANIMATION)) {
			topActivity.startActivity(intent);
			topActivity.overridePendingTransition(TiConvert.toInt(options.get(TiC.INTENT_PROPERTY_ENTER_ANIMATION), 0),
				TiConvert.toInt(options.get(TiC.INTENT_PROPERTY_EXIT_ANIMATION), 0));
		} else {
			topActivity.startActivity(intent);
		}
	}

	@Override
	protected void handleClose(KrollDict options)
	{
		if (windowActivity == null) {
			//we must have been opened without creating the activity.
			clearWindowsStack();
			closeFromActivity(false);
			return;
		}
		boolean animated = TiConvert.toBoolean(options, TiC.PROPERTY_ANIMATED, true);
		TiBaseActivity activity = (windowActivity != null) ? windowActivity.get() : null;
		if (activity != null && !activity.isFinishing()) {
			activity.finish();
			if (!animated) {
				activity.overridePendingTransition(0, 0); // Suppress default transition.
			} else if (options.containsKey(TiC.INTENT_PROPERTY_ENTER_ANIMATION)
				|| options.containsKey(TiC.INTENT_PROPERTY_EXIT_ANIMATION)) {
				activity.overridePendingTransition(TiConvert.toInt(options.get(TiC.INTENT_PROPERTY_ENTER_ANIMATION), 0),
					TiConvert.toInt(options.get(TiC.INTENT_PROPERTY_EXIT_ANIMATION), 0));
			}

			// Finishing an activity is not synchronous, so we remove the activity from the activity stack here
			TiApplication.removeFromActivityStack(activity);
			windowActivity = null;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void windowCreated(TiBaseActivity activity) {
		windowActivity = new WeakReference<TiBaseActivity>(activity);
		activity.setWindowProxy(this);
		setActivity(activity);

		// Handle the "activity" property.
		ActivityProxy activityProxy = activity.getActivityProxy();
		KrollDict options = null;
		if (hasProperty(TiC.PROPERTY_ACTIVITY)) {
			Object activityObject = getProperty(TiC.PROPERTY_ACTIVITY);
			if (activityObject instanceof HashMap<?, ?>) {
				options = new KrollDict((HashMap<String, Object>) activityObject);
				activityProxy.handleCreationDict(options);
			}
		}

		Window win = activity.getWindow();
		// Handle the background of the window activity if it is a translucent activity.
		// If it is a modal window, set a translucent dimmed background to the window.
		// If the opacity is given, set a transparent background to the window. In this case, if no backgroundColor or
		// backgroundImage is given, the window will be completely transparent.
		boolean modal = TiConvert.toBoolean(getProperty(TiC.PROPERTY_MODAL), false);
		Drawable background = null;
		if (modal) {
			background = new ColorDrawable(0x9F000000);
		} else if (hasProperty(TiC.PROPERTY_OPACITY)) {
			background = new ColorDrawable(0x00000000);
		}
		if (background != null) {
			win.setBackgroundDrawable(background);
		}

		// Handle the width and height of the window.
		if (hasProperty(TiC.PROPERTY_WIDTH) || hasProperty(TiC.PROPERTY_HEIGHT)) {
			int w = TiConvert.toInt(getProperty(TiC.PROPERTY_WIDTH), LayoutParams.MATCH_PARENT);
			int h = TiConvert.toInt(getProperty(TiC.PROPERTY_HEIGHT), LayoutParams.MATCH_PARENT);
			win.setLayout(w, h);
		}

		activity.getActivityProxy().getDecorView().add(this);
		activity.addWindowToStack(this);
		

		// Need to handle the cached activity proxy properties and url window in the JS side.
		callPropertySync(PROPERTY_POST_WINDOW_CREATED, null);
	}
	
	
	@Override
	public void setActivity(Activity activity)
	{
		TiBaseActivity oldActivity = (TiBaseActivity) getActivity();
		TiBaseActivity newActivity = (TiBaseActivity) activity;
		
		if (newActivity == oldActivity) return;
		super.setActivity(activity);
		
		if (oldActivity != null) {
			oldActivity.removeOnLifecycleEventListener(this);
			oldActivity.removeInterceptOnBackPressedEventListener(this);
			oldActivity.removeInterceptOnHomePressedEventListener(this);
		}
		
		if (newActivity != null) {
			newActivity.addOnLifecycleEventListener(this);
			newActivity.addInterceptOnBackPressedEventListener(this);
			newActivity.addInterceptOnHomePressedEventListener(this);
		}
	}

	private static int viewId = 10000;
	@Override
	public void onWindowActivityCreated()
	{
		// Fire the open event after setContentView() because getActionBar() need to be called
		// after setContentView(). (TIMOB-14914)
		if (opened ||opening) {
			if (windows.size() > 0) {
				((TiBaseActivity) getActivity()).setWindowProxy(windows.get(windows.size() - 1));
			}
			updateHomeButton(getCurrentWindow());
			return;
		}
		updateHomeButton(getCurrentWindow());
		
		opened = true;
		opening = false;

		handlePostOpen();
		
		getParentViewForChild().setId(viewId++);
		handlePush((WindowProxy)getProperty(TiC.PROPERTY_WINDOW), true, null);
		
		

		super.onWindowActivityCreated();
	}
	
	protected int getContainerId(){
		int id = getParentViewForChild().getId();
		Log.d(TAG, "getContainerId " + id);
		return id;
	}

	@Override
	protected Activity getWindowActivity()
	{
		return (windowActivity != null) ? windowActivity.get() : getActivity();
	}

	private void fillIntent(Activity activity, Intent intent)
	{
		if (hasProperty(TiC.PROPERTY_FULLSCREEN)) {
			intent.putExtra(TiC.PROPERTY_FULLSCREEN, TiConvert.toBoolean(getProperty(TiC.PROPERTY_FULLSCREEN), false));
		}
		if (hasProperty(TiC.PROPERTY_NAV_BAR_HIDDEN)) {
			intent.putExtra(TiC.PROPERTY_NAV_BAR_HIDDEN, TiConvert.toBoolean(getProperty(TiC.PROPERTY_NAV_BAR_HIDDEN), false));
		}
		if (hasProperty(TiC.PROPERTY_WINDOW_SOFT_INPUT_MODE)) {
			intent.putExtra(TiC.PROPERTY_WINDOW_SOFT_INPUT_MODE, TiConvert.toInt(getProperty(TiC.PROPERTY_WINDOW_SOFT_INPUT_MODE), -1));
		}
		if (hasProperty(TiC.PROPERTY_EXIT_ON_CLOSE)) {
			intent.putExtra(TiC.INTENT_PROPERTY_FINISH_ROOT, TiConvert.toBoolean(getProperty(TiC.PROPERTY_EXIT_ON_CLOSE), false));
		}
		boolean modal = false;
		if (hasProperty(TiC.PROPERTY_MODAL)) {
			modal = TiConvert.toBoolean(getProperty(TiC.PROPERTY_MODAL), false);
			if (modal) {
				intent.setClass(activity, TiTranslucentActivity.class);
			}
			intent.putExtra(TiC.PROPERTY_MODAL, modal);
		}
		if (!modal && hasProperty(TiC.PROPERTY_OPACITY)) {
			intent.setClass(activity, TiTranslucentActivity.class);
		}
		if (hasProperty(TiC.PROPERTY_WINDOW_PIXEL_FORMAT)) {
			intent.putExtra(TiC.PROPERTY_WINDOW_PIXEL_FORMAT, TiConvert.toInt(getProperty(TiC.PROPERTY_WINDOW_PIXEL_FORMAT), PixelFormat.UNKNOWN));
		}
	}

	@Override
	public void onPropertyChanged(String name, Object value)
	{
		if (TiC.PROPERTY_WINDOW_PIXEL_FORMAT.equals(name)) {
			getMainHandler().obtainMessage(MSG_SET_PIXEL_FORMAT, value).sendToTarget();
		} else if (TiC.PROPERTY_TITLE.equals(name)) {
			getMainHandler().obtainMessage(MSG_SET_TITLE, value).sendToTarget();
		} else if (TiC.PROPERTY_TOP.equals(name) || TiC.PROPERTY_BOTTOM.equals(name) || TiC.PROPERTY_LEFT.equals(name)
			|| TiC.PROPERTY_RIGHT.equals(name)) {
			// The "top", "bottom", "left" and "right" properties do not work for heavyweight windows.
			return;
		}

		super.onPropertyChanged(name, value);
	}

	@Override
	@Kroll.setProperty(retain=false) @Kroll.method
	public void setWidth(Object width)
	{
		Object current = getProperty(TiC.PROPERTY_WIDTH);
		if (shouldFireChange(current, width)) {
			int w = TiConvert.toInt(width, LayoutParams.MATCH_PARENT);
			int h = TiConvert.toInt(getProperty(TiC.PROPERTY_HEIGHT), LayoutParams.MATCH_PARENT);
			if (TiApplication.isUIThread()) {
				setWindowWidthHeight(w, h);
			} else {
				getMainHandler().obtainMessage(MSG_SET_WIDTH_HEIGHT, w, h).sendToTarget();
			}
		}
		super.setWidth(width);
	}

	@Override
	@Kroll.setProperty(retain=false) @Kroll.method
	public void setHeight(Object height)
	{
		Object current = getProperty(TiC.PROPERTY_HEIGHT);
		if (shouldFireChange(current, height)) {
			int h = TiConvert.toInt(height, LayoutParams.MATCH_PARENT);
			int w = TiConvert.toInt(getProperty(TiC.PROPERTY_WIDTH), LayoutParams.MATCH_PARENT);
			if (TiApplication.isUIThread()) {
				setWindowWidthHeight(w, h);
			} else {
				getMainHandler().obtainMessage(MSG_SET_WIDTH_HEIGHT, w, h).sendToTarget();
			}
		}
		super.setHeight(height);
	}

	@Override
	public boolean handleMessage(Message msg)
	{
		switch (msg.what) {
			case MSG_SET_PIXEL_FORMAT: {
				Activity activity = getWindowActivity();
				if (activity != null) {
					Window win = activity.getWindow();
					if (win != null) {
						win.setFormat(TiConvert.toInt((Object)(msg.obj), PixelFormat.UNKNOWN));
						win.getDecorView().invalidate();
					}
				}
				return true;
			}
			case MSG_SET_TITLE: {
				Activity activity = getWindowActivity();
				if (activity != null && windows.size() == 0) {
					activity.setTitle(TiConvert.toString((Object)(msg.obj), ""));
				}
				return true;
			}
			case MSG_SET_WIDTH_HEIGHT: {
				setWindowWidthHeight(msg.arg1, msg.arg2);
				return true;
			}
			case MSG_PUSH: {
				AsyncResult result = (AsyncResult) msg.obj;
				Pair<TiWindowProxy, Object> pair = (Pair<TiWindowProxy, Object>) result.getArg();
				handlePush(pair.first, false, pair.second);
				result.setResult(null); // signal opened
				return true;
			}
			case MSG_POP: {
				AsyncResult result = (AsyncResult) msg.obj;
				Pair<TiWindowProxy, Object> pair = (Pair<TiWindowProxy, Object>) result.getArg();
				result.setResult(handlePop(pair.first, pair.second)); // signal opened
				return true;
			}
		}
		return super.handleMessage(msg);
	}

	private void setWindowWidthHeight(int w, int h)
	{
		Activity activity = getWindowActivity();
		if (activity != null) {
			Window win = activity.getWindow();
			if (win != null) {
				win.setLayout(w, h);
			}
		}
	}

	@Kroll.method(name = "_getWindowActivityProxy")
	public ActivityProxy getWindowActivityProxy()
	{
		if (opened) {
			return super.getActivityProxy();
		} else {
			return null;
		}
	}
	
	
	private boolean handlePop(final TiWindowProxy proxy, Object arg) 
	{
		if (!opened || opening || !windows.contains(proxy)) {
			poping = false;
			return true;
		}
		return popWindow(proxy, arg);
	}

	
	
	private void handlePush(final TiWindowProxy proxy, boolean isFirst, Object arg) 
	{
		if (!opened || opening) {
			pushing = false;
			return;
		}
		Transition transition = null;
		KrollDict options = null;
		if (arg != null && arg instanceof HashMap<?, ?>) {
				options = new KrollDict((HashMap<String, Object>) arg);
		} else {
			options = new KrollDict();
		}
		boolean animated = options.optBoolean(TiC.PROPERTY_ANIMATED, !isFirst);
		int transitionStyle = options.optInt(TiC.PROPERTY_TRANSITION_STYLE, defaultTransitionStyle);
		int transitionSubStyle = options.optInt(TiC.PROPERTY_TRANSITION_SUBSTYLE, defaultTransitionSubStyle);
		int duration = options.optInt(TiC.PROPERTY_TRANSITION_DURATION, -1);
		
		final ViewGroup viewToAddTo = (ViewGroup) getParentViewForChild();
		
		if (!isFirst && animated) {
			transition = TransitionHelper.transitionForType(transitionStyle , transitionSubStyle, duration);
		}		
		if (viewToAddTo != null) {
			final View viewToAdd = proxy.getOrCreateView().getOuterView();
   			viewToAdd.setVisibility(View.GONE);			
			TiUIHelper.addView(viewToAddTo, viewToAdd, proxy.peekView().getLayoutParams());
			if (transition != null) {	
				TiWindowProxy winToBlur = getCurrentWindow();
				final View viewToHide = winToBlur.getOuterView();
				
				transition.setTargets(viewToAdd, viewToHide);

				AnimatorSet set = transition.getSet(new AnimatorListener() {
					public void onAnimationStart(Animator arg0) {							
					}
					
					public void onAnimationRepeat(Animator arg0) {							
					}
					
					public void onAnimationEnd(Animator arg0) {	
						viewToAddTo.removeView(viewToHide);
						pushing = false;
					}

					public void onAnimationCancel(Animator arg0) {		
						viewToAddTo.removeView(viewToHide);
						pushing = false; 
					}
				});
				set.start();
			}
   			viewToAdd.setVisibility(View.VISIBLE);			
		}
		addWindow(proxy, transition);
		
		TiBaseActivity activity = (TiBaseActivity) getActivity();
		proxy.setActivity(activity);
		proxy.onWindowActivityCreated();
		activity.setWindowProxy((TiWindowProxy) proxy);
		updateHomeButton(proxy);
	}
	
	@Kroll.method
	public void push(final TiWindowProxy proxy, @Kroll.argument(optional = true) Object arg)
	{
		if (pushing || poping) return;
		pushing = true;
		if (TiApplication.isUIThread()) {
			handlePush(proxy, false, arg);
			return;
		}

		TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_PUSH), new Pair<TiWindowProxy, Object>(proxy, arg));
	}
	
	@Kroll.method @Kroll.getProperty
	public int getStackSize()
	{
		return windows.size();
	}
	
	@Kroll.method
	public void pop(final TiWindowProxy proxy, @Kroll.argument(optional = true) Object arg)
	{
		if (pushing || poping) return;
		poping = true;
		if (TiApplication.isUIThread()) {
			handlePop(proxy, arg);
			return;
		}

		TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_POP), new Pair<TiWindowProxy, Object>(proxy, arg));
	}

	@Override
	public boolean interceptOnBackPressed() {
		if (windows.size() > 0) {
			TiWindowProxy topWindow = windows.get(windows.size() - 1);

			// Prevent default Android behavior for "back" press
			// if the top window has a listener to handle the event.
			if (topWindow != null && topWindow.hasListeners(TiC.EVENT_ANDROID_BACK)) {
				topWindow.fireEvent(TiC.EVENT_ANDROID_BACK, null);
				return true;
			}
		}
		if (!pushing && !poping)
			return popCurrentWindow(null);
		else return true;
	}

	@Override
	public boolean handleClose(TiWindowProxy proxy, Object arg) {
		poping = true;
		if (TiApplication.isUIThread()) {
			return handlePop(proxy, arg);
		}
		Object result = TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_POP), new Pair<TiWindowProxy, Object>(proxy, arg));
		if (result instanceof Boolean) {
			return ((Boolean) result).booleanValue();
		} else {
			return false;
		}
	}

	@Override
	public boolean handleOpen(TiWindowProxy proxy, Object arg) {
		return false;
	}

	@Override
	public void onStart(Activity activity) {
	}

	@Override
	public void onResume(Activity activity) {
	}

	@Override
	public void onPause(Activity activity) {		
	}
	
	public void clearWindowsStack(){
		if (windows.size() == 0) return;
		ViewGroup viewToRemoveFrom = (ViewGroup) getParentViewForChild();
		for (int i = 0; i < windows.size(); i++) {
			TiWindowProxy proxy = windows.get(i);
			View viewToRemove = proxy.getOuterView();
			if (viewToRemove != null) {
				viewToRemoveFrom.removeView(viewToRemove);
			}
			proxy.closeFromActivity(false);
		}
		windows.clear();
	}

	@Override
	public void onStop(Activity activity) {
		clearWindowsStack();
	}

	@Override
	public void onDestroy(Activity activity) {		
	}
}
