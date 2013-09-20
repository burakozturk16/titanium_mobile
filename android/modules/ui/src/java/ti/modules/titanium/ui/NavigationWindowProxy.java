/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

package ti.modules.titanium.ui;

import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;

import org.appcelerator.kroll.KrollDict;
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
import org.appcelerator.titanium.TiTranslucentActivity;
import org.appcelerator.titanium.TiLifecycle.interceptOnBackPressedEvent;
import org.appcelerator.titanium.TiWindowManager;
import org.appcelerator.titanium.proxy.ActivityProxy;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.proxy.TiWindowProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiRHelper;
import org.appcelerator.titanium.util.TiRHelper.ResourceNotFoundException;
import org.appcelerator.titanium.view.TiAnimation;
import org.appcelerator.titanium.view.TiCompositeLayout;
import org.appcelerator.titanium.view.TiUIFragment;
import org.appcelerator.titanium.view.TiUIView;

import ti.modules.titanium.ui.widget.TiView;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Message;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;

import android.os.Bundle;
import android.view.ViewGroup;

@SuppressLint({ "ValidFragment", "NewApi" })
@Kroll.proxy(creatableInModule=UIModule.class, propertyAccessors={
	TiC.PROPERTY_MODAL,
	TiC.PROPERTY_ACTIVITY,
	TiC.PROPERTY_URL,
	TiC.PROPERTY_WINDOW_PIXEL_FORMAT
})
public class NavigationWindowProxy extends TiWindowProxy implements OnLifecycleEvent, TiActivityWindow, interceptOnBackPressedEvent, TiWindowManager
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

	private WeakReference<TiBaseActivity> windowActivity;
	FragmentManager manager = null;

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

	@Override
	public TiUIView createView(Activity activity)
	{
		TiUIView v = new TiView(this);
		v.getLayoutParams().autoFillsHeight = true;
		v.getLayoutParams().autoFillsWidth = true;
		setView(v);
		return v;
	}
	
	@Override
	public void onFirstLayout()
	{
		super.onFirstLayout();
		fireEvent(TiC.EVENT_OPEN, null);
	}

	

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
	
	
	public boolean removeCurrentFragment()
	{
		TiBaseActivity activity = ((TiBaseActivity) getActivity());
		 if (activity.getFragmentManager().getBackStackEntryCount() > 0){
	        // Get the fragment fragment manager - and pop the backstack
	    	activity.getFragmentManager().popBackStackImmediate();
	    	TiWindowProxy toRemove = windows.get(windows.size() - 1);
	    	windows.remove(windows.size() - 1);
			if (windows.size() > 0) {
				getMainHandler().obtainMessage(MSG_SET_TITLE, windows.get(windows.size() - 1).getProperty(TiC.PROPERTY_TITLE, "")).sendToTarget();
			}
			KrollDict data = null;
			data = new KrollDict();
			data.put("_closeFromActivityForcedToDestroy", true);
			toRemove.fireSyncEvent(TiC.EVENT_CLOSE, data);
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
	
	private static int viewId = 10000;
	@Override
	public void handlePostOpen()
	{
		TiBaseActivity activity = (TiBaseActivity) getActivity();
		getParentViewForChild().setId(viewId++);
		handlePush((WindowProxy)getProperty(TiC.PROPERTY_WINDOW), true, null);
		activity.addOnLifecycleEventListener(this);
		activity.addInterceptOnBackPressedEventListener(this);
		super.handlePostOpen();
	}

	@Override
	public void onWindowActivityCreated()
	{
		// Fire the open event after setContentView() because getActionBar() need to be called
		// after setContentView(). (TIMOB-14914)
		opened = true;
		opening = false;

		// fireEvent(TiC.EVENT_OPEN, null);
		handlePostOpen();

		super.onWindowActivityCreated();
	}
	
	protected int getContainerId(){
		return getParentViewForChild().getId();
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
				if (activity != null) {
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
				Pair<WindowProxy, Object> pair = (Pair<WindowProxy, Object>) result.getArg();
				handlePush(pair.first, false, pair.second);
				result.setResult(null); // signal opened
				return true;
			}
			case MSG_POP: {
				AsyncResult result = (AsyncResult) msg.obj;
				Pair<WindowProxy, Object> pair = (Pair<WindowProxy, Object>) result.getArg();
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
	
	ArrayList<TiWindowProxy> windows = new ArrayList<TiWindowProxy>();
	
	private boolean handlePop(final WindowProxy proxy, Object arg) 
	{
		if (!opened || opening) return false;
		if (!windows.contains(proxy)) return false;
		proxy.setWindowManager(null);
		if (windows.get(windows.size() - 1) == proxy)
		{
			removeCurrentFragment();
			return true;
		}
		else {
			FragmentTransaction transaction = getFragmentManager().beginTransaction();
			transaction.remove(proxy.getFragment());
			transaction.commit();
		}
		return false;
	}
	
	private void handlePush(final WindowProxy proxy, boolean isFirst, Object arg) 
	{
		if (!opened || opening) return;
		FragmentTransaction transaction = getFragmentManager().beginTransaction();
		Fragment fragment = proxy.getFragment();
		if (isFirst) {
			transaction.add(getContainerId(), fragment);
		}
		else {
			proxy.setWindowManager(this);
			if (arg != null) {
				try {
					transaction.setCustomAnimations(
							TiRHelper.getResource("anim.card_flip_right_in"),TiRHelper.getResource("anim.card_flip_right_out"),
							TiRHelper.getResource("anim.card_flip_left_in"), TiRHelper.getResource("anim.card_flip_left_out"));
				} catch (ResourceNotFoundException e) {
					e.printStackTrace();
				}
			}	
			transaction.replace(getContainerId(), fragment);
			transaction.addToBackStack(null);
		}
		transaction.commit();
		if (!windows.contains(proxy)) windows.add(proxy);
		proxy.onWindowActivityCreated();
		proxy.setActivity(getActivity());
		getMainHandler().obtainMessage(MSG_SET_TITLE, proxy.getProperty(TiC.PROPERTY_TITLE, "")).sendToTarget();
	}
	
	@Kroll.method
	public void push(final WindowProxy proxy, @Kroll.argument(optional = true) Object arg)
	{
		if (TiApplication.isUIThread()) {
			handlePush(proxy, false, arg);
			return;
		}

		TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_PUSH), new Pair<WindowProxy, Object>(proxy, arg));
	}
	
	@Kroll.method
	public void pop(final WindowProxy proxy, @Kroll.argument(optional = true) Object arg)
	{
		if (TiApplication.isUIThread()) {
			handlePop(proxy, arg);
			return;
		}

		TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_POP), new Pair<WindowProxy, Object>(proxy, arg));
	}
	
	public FragmentManager getFragmentManager() {
		return ((TiBaseActivity) getActivity()).getFragmentManager();
//		if (manager == null) {
//			manager = getFragment().getChildFragmentManager();
//		}
//		return manager;
	}
	
	@Override
	public Fragment getFragment() {
		if (fragment == null) {
			fragment = new Fragment() {
				@Override
				public View onCreateView(LayoutInflater inflater,
						ViewGroup container, Bundle savedInstanceState) {
					View view = getOrCreateView().getParentViewForChild();
					return view;
				}
			};
		}
		return fragment;
	}


	@Override
	public boolean interceptOnBackPressed() {
		TiBaseActivity activity = ((TiBaseActivity) getActivity());
		TiWindowProxy topWindow = activity.topWindowOnStack();

		// Prevent default Android behavior for "back" press
		// if the top window has a listener to handle the event.
		if (topWindow != null && topWindow.hasListeners(TiC.EVENT_ANDROID_BACK)) {
			topWindow.fireEvent(TiC.EVENT_ANDROID_BACK, null);
			return true;
		}
		return removeCurrentFragment();
	}

	@Override
	public boolean handleClose(TiWindowProxy proxy, Object arg) {
		
		if (TiApplication.isUIThread()) {
			return handlePop((WindowProxy) proxy, arg);
		}
		Object result = TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_POP), new Pair<WindowProxy, Object>((WindowProxy) proxy, arg));
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
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onResume(Activity activity) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPause(Activity activity) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStop(Activity activity) {
		FragmentTransaction transaction = getFragmentManager().beginTransaction();
		for (int i = 0; i < windows.size(); i++) {
			TiWindowProxy proxy = windows.get(i);
			transaction.remove(proxy.getFragment());
			KrollDict data = null;
			data = new KrollDict();
			data.put("_closeFromActivityForcedToDestroy", true);
			proxy.fireSyncEvent(TiC.EVENT_CLOSE, data);
		}
		transaction.commit();
		windows.clear();
	}

	@Override
	public void onDestroy(Activity activity) {
		// TODO Auto-generated method stub
		
	}
}
