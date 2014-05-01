/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2014 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package org.appcelerator.titanium;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Stack;
import java.util.concurrent.CopyOnWriteArrayList;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollFunction;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.KrollRuntime;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.kroll.common.TiMessenger;
import org.appcelerator.titanium.TiLifecycle.OnLifecycleEvent;
import org.appcelerator.titanium.TiLifecycle.OnWindowFocusChangedEvent;
import org.appcelerator.titanium.TiLifecycle.interceptOnBackPressedEvent;
import org.appcelerator.titanium.TiLifecycle.interceptOnHomePressedEvent;
import org.appcelerator.titanium.proxy.ActionBarProxy;
import org.appcelerator.titanium.proxy.ActivityProxy;
import org.appcelerator.titanium.proxy.IntentProxy;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.proxy.TiWindowProxy;
import org.appcelerator.titanium.util.TiActivityResultHandler;
import org.appcelerator.titanium.util.TiActivitySupport;
import org.appcelerator.titanium.util.TiActivitySupportHelper;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiMenuSupport;
import org.appcelerator.titanium.util.TiPlatformHelper;
import org.appcelerator.titanium.util.TiUIHelper;
import org.appcelerator.titanium.util.TiWeakList;
import org.appcelerator.titanium.view.TiCompositeLayout;
import org.appcelerator.titanium.view.TiCompositeLayout.LayoutArrangement;
import org.aps.analytics.APSAnalytics;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.IntentSender;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;

import android.view.KeyEvent;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

/**
 * The base class for all non tab Titanium activities. To learn more about Activities, see the
 * <a href="http://developer.android.com/reference/android/app/Activity.html">Android Activity documentation</a>.
 */
public abstract class TiBaseActivity extends SherlockFragmentActivity
	implements TiActivitySupport/*, ITiWindowHandler*/
{
	private static final String TAG = "TiBaseActivity";

	private static OrientationChangedListener orientationChangedListener = null;

	private boolean onDestroyFired = false;
	private int originalOrientationMode = -1;
	private boolean inForeground = false; // Indicates whether this activity is in foreground or not.
	private TiWeakList<OnLifecycleEvent> lifecycleListeners = new TiWeakList<OnLifecycleEvent>();
	private TiWeakList<OnWindowFocusChangedEvent> windowFocusChangedListeners = new TiWeakList<OnWindowFocusChangedEvent>();
	protected TiWeakList<interceptOnBackPressedEvent> interceptOnBackPressedListeners = new TiWeakList<interceptOnBackPressedEvent>();
	protected TiWeakList<interceptOnHomePressedEvent> interceptOnHomePressedListeners = new TiWeakList<interceptOnHomePressedEvent>();

	protected View layout;
	protected TiActivitySupportHelper supportHelper;
	protected int supportHelperId = -1;
	protected TiWindowProxy window;
	protected TiViewProxy view;
	protected ActivityProxy activityProxy;
	protected TiWeakList<ConfigurationChangedListener> configChangedListeners = new TiWeakList<ConfigurationChangedListener>();
	protected int orientationDegrees;
	protected TiMenuSupport menuHelper;
	protected Messenger messenger;
	protected int msgActivityCreatedId = -1;
	protected int msgId = -1;
	protected static int previousOrientation = -1;
	//Storing the activity's dialogs and their persistence 
	private CopyOnWriteArrayList<DialogWrapper> dialogs = new CopyOnWriteArrayList<DialogWrapper>();
	private Stack<TiWindowProxy> windowStack = new Stack<TiWindowProxy>();

	public TiWindowProxy lwWindow;
	public boolean isResumed = false;
	
	private boolean isPaused = false;
	
	private boolean fullscreen = false;
	private boolean defaultFullscreen = false;
	private boolean navBarHidden = false;
	private boolean defaultNavBarHidden = false;
	private int defaultSoftInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN |  WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN;
	private int softInputMode = defaultSoftInputMode;

	public class DialogWrapper {
		boolean isPersistent;
		AlertDialog dialog;
		WeakReference<TiBaseActivity> dialogActivity;
		
		public DialogWrapper(AlertDialog d, boolean persistent, WeakReference<TiBaseActivity> activity) {
			isPersistent = persistent;
			dialog = d;
			dialogActivity = activity;
		}
		
		public TiBaseActivity getActivity()
		{
			if (dialogActivity == null) {
				return null;
			} else {
				return dialogActivity.get();
			}
		}

		public void setActivity(WeakReference<TiBaseActivity> da)
		{
			dialogActivity = da;
		}

		public AlertDialog getDialog() {
			return dialog;
		}
		
		public void setDialog(AlertDialog d) {
			dialog = d;
		}
		
		public void release() 
		{
			dialog = null;
			dialogActivity = null;
		}

		public boolean getPersistent()
		{
			return isPersistent;
		}

		public void setPersistent(boolean p) 
		{
			isPersistent = p;
		}
	}

	public void addWindowToStack(TiWindowProxy proxy)
	{
		if (windowStack.contains(proxy)) {
			Log.e(TAG, "Window already exists in stack", Log.DEBUG_MODE);
			return;
		}
		boolean isEmpty = windowStack.empty();
		if (!isEmpty) {
			windowStack.peek().onWindowFocusChange(false);
		}
		windowStack.add(proxy);
		if (!isEmpty) {
			proxy.onWindowFocusChange(true);
		}
		
		updateTitle(proxy);
	}

	public void removeWindowFromStack(TiWindowProxy proxy)
	{
		proxy.onWindowFocusChange(false);

		boolean isTopWindow = ( (!windowStack.isEmpty()) && (windowStack.peek() == proxy) ) ? true : false;
		windowStack.remove(proxy);
		
		if (!windowStack.empty()) {
			TiWindowProxy nextWindow = windowStack.peek();
			updateTitle(nextWindow);
			//Fire focus only if activity is not paused and the removed window was topWindow
			if (isResumed && isTopWindow) {
				nextWindow.onWindowFocusChange(true);
//				updateTitle(proxy);
			}
		}
		else
		{
			updateTitle(this.window);
		}
		
	}

	/**
	 * Returns the window at the top of the stack.
	 * @return the top window or null if the stack is empty.
	 */
	public TiWindowProxy topWindowOnStack()
	{
		return (windowStack.isEmpty()) ? null : windowStack.peek();
	}

	// could use a normal ConfigurationChangedListener but since only orientation changes are
	// forwarded, create a separate interface in order to limit scope and maintain clarity 
	public static interface OrientationChangedListener
	{
		public void onOrientationChanged (int configOrientationMode);
	}

	public static void registerOrientationListener (OrientationChangedListener listener)
	{
		orientationChangedListener = listener;
	}

	public static void deregisterOrientationListener()
	{
		orientationChangedListener = null;
	}

	public static interface ConfigurationChangedListener
	{
		public void onConfigurationChanged(TiBaseActivity activity, Configuration newConfig);
	}

	/**
	 * @return the instance of TiApplication.
	 */
	public TiApplication getTiApp()
	{
		return (TiApplication) getApplication();
	}

	/**
	 * @return the window proxy associated with this activity.
	 */
	public TiWindowProxy getWindowProxy()
	{
		return this.window;
	}

	private KrollDict updatePropertiesFromWindow(KrollDict properties, KrollDict windowProperties)
	{
		KrollDict actionBarDict = null;
		if (properties.containsKey(TiC.PROPERTY_ACTION_BAR)) {
			actionBarDict = properties.getKrollDict(TiC.PROPERTY_ACTION_BAR);
		}
		else {
			actionBarDict = new KrollDict(); //to make sure we go into processProperties
		}
		
		if (windowProperties.containsKey(TiC.PROPERTY_BAR_COLOR) && !actionBarDict.containsKey(TiC.PROPERTY_BACKGROUND_COLOR)) {
			actionBarDict.put(TiC.PROPERTY_BACKGROUND_COLOR, windowProperties.get(TiC.PROPERTY_BAR_COLOR));
		}
		if (windowProperties.containsKey(TiC.PROPERTY_BAR_IMAGE) && !actionBarDict.containsKey(TiC.PROPERTY_BACKGROUND_IMAGE)) {
			actionBarDict.put(TiC.PROPERTY_BACKGROUND_IMAGE, windowProperties.get(TiC.PROPERTY_BAR_IMAGE));
		}
		if (windowProperties.containsKey(TiC.PROPERTY_BAR_ICON) && !actionBarDict.containsKey(TiC.PROPERTY_ICON)) {
			actionBarDict.put(TiC.PROPERTY_ICON, windowProperties.get(TiC.PROPERTY_BAR_ICON));
		}
		properties.put(TiC.PROPERTY_ACTION_BAR, actionBarDict);
		return properties;
	}

	/**
	 * Sets the window proxy.
	 * @param proxy
	 */
	@SuppressLint("NewApi")
	public void setWindowProxy(TiWindowProxy proxy)
	{
		this.window = proxy;
		updateTitle(this.window);
		
		KrollDict props = this.window.getProperties();
		boolean fullscreen = props.optBoolean(TiC.PROPERTY_FULLSCREEN, this.defaultFullscreen);
		boolean newNavBarHidden = props.optBoolean(TiC.PROPERTY_NAV_BAR_HIDDEN, this.defaultFullscreen);
		int softInputMode = props.optInt(TiC.PROPERTY_WINDOW_SOFT_INPUT_MODE, this.defaultSoftInputMode);
		boolean hasSoftInputMode = softInputMode != -1;
		
		if (fullscreen != this.fullscreen) {
			this.fullscreen = fullscreen;
			setFullscreen(fullscreen);
		}
		if (newNavBarHidden != this.navBarHidden) {
			this.navBarHidden = newNavBarHidden;
			ActionBar actionBar = getSupportActionBar();
			if (actionBar != null) {
				if (this.navBarHidden) actionBar.hide();
				else actionBar.show();
			}
			
		}
		
		
		if (hasSoftInputMode && softInputMode != this.softInputMode) {
			Log.d(TAG, "windowSoftInputMode: " + softInputMode, Log.DEBUG_MODE);
			getWindow().setSoftInputMode(softInputMode);  
		}
		KrollDict activityDict = null;
		if (this.window.hasProperty(TiC.PROPERTY_ACTIVITY)) {
			activityDict = new KrollDict((HashMap)this.window.getProperty(TiC.PROPERTY_ACTIVITY));
		}
		else {
			activityDict = new KrollDict(); //to make sure we update actionbar
		}
		updatePropertiesFromWindow(activityDict, this.window.getProperties());

		getActivityProxy().setProperties(activityDict);
	}

	/**
	 * Sets the proxy for our layout (used for post layout event)
	 * 
	 * @param proxy
	 */
	public void setLayoutProxy(TiViewProxy proxy)
	{
		if (layout instanceof TiCompositeLayout) {
			((TiCompositeLayout) layout).setView(proxy.peekView());
		}
	}

	/**
	 * Sets the view proxy.
	 * @param proxy
	 */
	public void setViewProxy(TiViewProxy proxy)
	{
		this.view = proxy;
	}

	/**
	 * @return activity proxy associated with this activity.
	 */
	public ActivityProxy getActivityProxy()
	{
		return activityProxy;
	}

	public void addDialog(DialogWrapper d) 
	{
		if (!dialogs.contains(d)) {
			dialogs.add(d);
		}
	}
	
	public void removeDialog(Dialog d) 
	{
		for (int i = 0; i < dialogs.size(); i++) {
			DialogWrapper p = dialogs.get(i);
			if (p.getDialog().equals(d)) {
				p.release();
				dialogs.remove(i);
				return;
			}
		}
	}
	public void setActivityProxy(ActivityProxy proxy)
	{
		this.activityProxy = proxy;
	}

	/**
	 * @return the activity's current layout.
	 */
	public View getLayout()
	{
		return layout;
	}

	public void setLayout(View layout)
	{
		this.layout = layout;
	}

	public void addConfigurationChangedListener(ConfigurationChangedListener listener)
	{
		configChangedListeners.add(new WeakReference<ConfigurationChangedListener>(listener));
	}

	public void removeConfigurationChangedListener(ConfigurationChangedListener listener)
	{
		configChangedListeners.remove(listener);
	}

	public void registerOrientationChangedListener (OrientationChangedListener listener)
	{
		orientationChangedListener = listener;
	}

	public void deregisterOrientationChangedListener()
	{
		orientationChangedListener = null;
	}

	protected boolean getIntentBoolean(String property, boolean defaultValue)
	{
		Intent intent = getIntent();
		if (intent != null) {
			if (intent.hasExtra(property)) {
				return intent.getBooleanExtra(property, defaultValue);
			}
		}

		return defaultValue;
	}

	protected int getIntentInt(String property, int defaultValue)
	{
		Intent intent = getIntent();
		if (intent != null) {
			if (intent.hasExtra(property)) {
				return intent.getIntExtra(property, defaultValue);
			}
		}

		return defaultValue;
	}

	protected String getIntentString(String property, String defaultValue)
	{
		Intent intent = getIntent();
		if (intent != null) {
			if (intent.hasExtra(property)) {
				return intent.getStringExtra(property);
			}
		}

		return defaultValue;
	}


	protected void updateTitle(TiWindowProxy proxy)
	{
		if (proxy == null) return;

		if (proxy.hasProperty(TiC.PROPERTY_TITLE)) {
			String oldTitle = (String) getTitle();
			String newTitle = TiConvert.toString(proxy.getProperty(TiC.PROPERTY_TITLE));

			if (oldTitle == null) {
				oldTitle = "";
			}

			if (newTitle == null) {
				newTitle = "";
			}

			if (!newTitle.equals(oldTitle)) {
				final String fnewTitle = newTitle;
				final ActionBar actionBar = getSupportActionBar();
				if (actionBar != null) {
					actionBar.setTitle(fnewTitle);
				}
				else {
					runOnUiThread(new Runnable(){
						public void run() {
							setTitle(fnewTitle);
						}
					});
				}
			}
		}
	}
	
	private void checkUpEventSent(MotionEvent event){
		if (!windowStack.isEmpty()) {
			Iterator itr = windowStack.iterator();
		    while( itr.hasNext() ) {
		        TiWindowProxy window = (TiWindowProxy)itr.next();
		        window.checkUpEventSent(event);
		    }
		}
		if (window != null) {
			 window.checkUpEventSent(event);
		}
	}

	// Subclasses can override to provide a custom layout
	protected View createLayout()
	{
		LayoutArrangement arrangement = LayoutArrangement.DEFAULT;

		String layoutFromIntent = getIntentString(TiC.INTENT_PROPERTY_LAYOUT, "");
		if (layoutFromIntent.equals(TiC.LAYOUT_HORIZONTAL)) {
			arrangement = LayoutArrangement.HORIZONTAL;

		} else if (layoutFromIntent.equals(TiC.LAYOUT_VERTICAL)) {
			arrangement = LayoutArrangement.VERTICAL;
		}

		// set to null for now, this will get set correctly in setWindowProxy()
		return new TiCompositeLayout(this, arrangement, null) {
			private boolean firstFocusRequest = true;
			
			@Override
		    public boolean requestFocus(int direction, Rect previouslyFocusedRect) {
		        if (firstFocusRequest) {
		        	firstFocusRequest = false;
		        	return false;
		        }
		        return super.requestFocus(direction, previouslyFocusedRect);
		    }
			
			@Override
		    public boolean onInterceptTouchEvent(MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_UP) {
					final MotionEvent copy = MotionEvent.obtain(event);
					final Handler handler = new Handler();
					handler.postDelayed(new Runnable() {
					  @Override
					  public void run() {
						  checkUpEventSent(copy);
					  }
					}, 10);
				}
		        return super.onInterceptTouchEvent(event);
		    }
		};
	}

	protected void setFullscreen(boolean fullscreen)
	{
		if (fullscreen) {
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
		else {
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
	}

	protected void setNavBarHidden(final boolean hidden)
	{
		final ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			if (hidden) {
				actionBar.hide();
			}
			else {
				actionBar.show();
			}
		}
	}
	
	
	// Subclasses can override to handle post-creation (but pre-message fire) logic
	protected void windowCreated()
	{
		defaultFullscreen = fullscreen = getIntentBoolean(TiC.PROPERTY_FULLSCREEN, false);
		defaultNavBarHidden = navBarHidden = getIntentBoolean(TiC.PROPERTY_NAV_BAR_HIDDEN, false);
		boolean modal = getIntentBoolean(TiC.PROPERTY_MODAL, false);
		softInputMode = getIntentInt(TiC.PROPERTY_WINDOW_SOFT_INPUT_MODE, WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		boolean hasSoftInputMode = softInputMode != -1;
		int windowFlags = getIntentInt(TiC.PROPERTY_WINDOW_FLAGS, 0);
		final Window window = getWindow();
		
//	    getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
		window.requestFeature(Window.FEATURE_PROGRESS);
		window.requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		
		setFullscreen(fullscreen);
		setNavBarHidden(navBarHidden);	
		
		if (windowFlags > 0) {
			window.addFlags(windowFlags);
		}
		
		if (modal) {
			if (Build.VERSION.SDK_INT < TiC.API_LEVEL_ICE_CREAM_SANDWICH) {
				// This flag is deprecated in API 14. On ICS, the background is not blurred but straight black.
				window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
			}
		}

		if (hasSoftInputMode) {
			Log.d(TAG, "windowSoftInputMode: " + softInputMode, Log.DEBUG_MODE);
			window.setSoftInputMode(softInputMode);
		}

		boolean useActivityWindow = getIntentBoolean(TiC.INTENT_PROPERTY_USE_ACTIVITY_WINDOW, false);
		if (useActivityWindow) {
			int windowId = getIntentInt(TiC.INTENT_PROPERTY_WINDOW_ID, -1);
			TiActivityWindows.windowCreated(this, windowId);
		}
	}

	@Override
	/**
	 * When the activity is created, this method adds it to the activity stack and
	 * fires a javascript 'create' event.
	 * @param savedInstanceState Bundle of saved data.
	 */
	protected void onCreate(Bundle savedInstanceState)
	{
		TiApplication.getInstance().activityStarted(this);
		Log.d(TAG, "Activity " + this + " onCreate", Log.DEBUG_MODE);

		inForeground = true;
		TiApplication tiApp = getTiApp();

		if (tiApp.isRestartPending()) {
			super.onCreate(savedInstanceState);
			if (!isFinishing()) {
				finish();
			}
			return;
		}

		// If all the activities has been killed and the runtime has been disposed, we cannot recover one
		// specific activity because the info of the top-most view proxy has been lost (TiActivityWindows.dispose()).
		// In this case, we have to restart the app.
		if (TiBaseActivity.isUnsupportedReLaunch(this, savedInstanceState)) {
			Log.w(TAG, "Runtime has been disposed. Finishing.");
			super.onCreate(savedInstanceState);
			tiApp.scheduleRestart(250);
			finish();
			return;
		}

		TiApplication.addToActivityStack(this);

		// create the activity proxy here so that it is accessible from the activity in all cases
		activityProxy = new ActivityProxy(this);
		

		// Increment the reference count so we correctly clean up when all of our activities have been destroyed
		KrollRuntime.incrementActivityRefCount();

		Intent intent = getIntent();
		if (intent != null) {
			if (intent.hasExtra(TiC.INTENT_PROPERTY_MESSENGER)) {
				messenger = (Messenger) intent.getParcelableExtra(TiC.INTENT_PROPERTY_MESSENGER);
				msgActivityCreatedId = intent.getIntExtra(TiC.INTENT_PROPERTY_MSG_ACTIVITY_CREATED_ID, -1);
				msgId = intent.getIntExtra(TiC.INTENT_PROPERTY_MSG_ID, -1);
			}

			if (intent.hasExtra(TiC.PROPERTY_WINDOW_PIXEL_FORMAT)) {
				getWindow().setFormat(intent.getIntExtra(TiC.PROPERTY_WINDOW_PIXEL_FORMAT, PixelFormat.UNKNOWN));
			}
		}

		// Doing this on every create in case the activity is externally created.
		TiPlatformHelper.getInstance().intializeDisplayMetrics(this);

		if (layout == null) {
			layout = createLayout();
		}
		if (intent != null && intent.hasExtra(TiC.PROPERTY_KEEP_SCREEN_ON)) {
			layout.setKeepScreenOn(intent.getBooleanExtra(TiC.PROPERTY_KEEP_SCREEN_ON, layout.getKeepScreenOn()));
		}

		super.onCreate(savedInstanceState);
		
		// we only want to set the current activity for good in the resume state but we need it right now.
		// save off the existing current activity, set ourselves to be the new current activity temporarily 
		// so we don't run into problems when we give the proxy the event
		Activity tempCurrentActivity = tiApp.getCurrentActivity();
		tiApp.setCurrentActivity(this, this);

		windowCreated();

		if (activityProxy != null) {
			activityProxy.fireEvent(TiC.EVENT_CREATE, null);
		}

		// set the current activity back to what it was originally
		tiApp.setCurrentActivity(this, tempCurrentActivity);

		setContentView(layout);

		sendMessage(msgActivityCreatedId);
		// for backwards compatibility
		sendMessage(msgId);

		// store off the original orientation for the activity set in the AndroidManifest.xml
		// for later use
		originalOrientationMode = getRequestedOrientation();

		if (window != null) {
			if (window.getWindowManager() != null)
				window.getWindowManager().onWindowActivityCreated();
			else {
				window.onWindowActivityCreated();
			}
		}
	}

	public int getOriginalOrientationMode()
	{
		return originalOrientationMode;
	}

	public boolean isInForeground()
	{
		return inForeground;
	}
	
	public boolean isActivityPaused()
	{
		return isPaused;
	}

	protected void sendMessage(final int msgId)
	{
		if (messenger == null || msgId == -1) {
			return;
		}

		// fire an async message on this thread's queue
		// so we don't block onCreate() from returning
		TiMessenger.postOnMain(new Runnable() {
			public void run()
			{
				handleSendMessage(msgId);
			}
		});
	}

	protected void handleSendMessage(int messageId)
	{
		try {
			Message message = TiMessenger.getMainMessenger().getHandler().obtainMessage(messageId, this);
			messenger.send(message);

		} catch (RemoteException e) {
			Log.e(TAG, "Unable to message creator. finishing.", e);
			finish();

		} catch (RuntimeException e) {
			Log.e(TAG, "Unable to message creator. finishing.", e);
			finish();
		}
	}

	protected TiActivitySupportHelper getSupportHelper()
	{
		if (supportHelper == null) {
			this.supportHelper = new TiActivitySupportHelper(this);
			// Register the supportHelper so we can get it back when the activity is recovered from force-quitting.
			supportHelperId = TiActivitySupportHelpers.addSupportHelper(supportHelper);
		}

		return supportHelper;
	}

	// Activity Support
	public int getUniqueResultCode()
	{
		return getSupportHelper().getUniqueResultCode();
	}

	/**
	 * See TiActivitySupport.launchActivityForResult for more details.
	 */
	public void launchActivityForResult(Intent intent, int code, TiActivityResultHandler resultHandler)
	{
		getSupportHelper().launchActivityForResult(intent, code, resultHandler);
	}
	
	/**
	 * See TiActivitySupport.launchIntentSenderForResult for more details.
	 */
	public void launchIntentSenderForResult(IntentSender intent, int requestCode, Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags, Bundle options, TiActivityResultHandler resultHandler)
	{
		getSupportHelper().launchIntentSenderForResult(intent, requestCode, fillInIntent, flagsMask, flagsValues, extraFlags, options, resultHandler);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		getSupportHelper().onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onBackPressed()
	{
		if (!handleBackKeyPressed()){
			// If event is not handled by any listeners allow default behavior.
			super.onBackPressed();
		}
	}
	
	public boolean handleAndroidBackEvent() {
		KrollProxy proxy = null;
		if (activityProxy.hasListeners(TiC.EVENT_ANDROID_BACK)) {
			proxy = activityProxy;
		} else{
			TiWindowProxy topWindow = topWindowOnStack();
			if (topWindow != null && topWindow.hierarchyHasListener(TiC.EVENT_ANDROID_BACK)) {
				proxy = topWindow;
			}
			else if(window != null) {
				proxy = window.firstHierarchyListener(TiC.EVENT_ANDROID_BACK);
			}
		}
		
		// Prevent default Android behavior for "back" press
		// if the top window has a listener to handle the event.
		if (proxy != null) {
			proxy.fireEvent(TiC.EVENT_ANDROID_BACK);
			return true;
		}
		return false;
	}
	
	private boolean handleBackKeyPressed(){
		synchronized (interceptOnBackPressedListeners.synchronizedList()) {
			for (interceptOnBackPressedEvent listener : interceptOnBackPressedListeners.nonNull()) {
				try {
					if (listener.interceptOnBackPressed()) {
						return true;
					}

				} catch (Throwable t) {
					Log.e(TAG, "Error dispatching interceptOnBackPressed event: " + t.getMessage(), t);
				}
			}
		}
		return handleAndroidBackEvent();
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) 
	{
		boolean handled = false;
		
		TiViewProxy window;
		if (this.window != null) {
			window = this.window;
		} else {
			window = this.view;
		}
		
		if (window == null) {
			return super.dispatchKeyEvent(event);
		}

		switch(event.getKeyCode()) {
			case KeyEvent.KEYCODE_BACK : {
				
				if (event.getAction() == KeyEvent.ACTION_UP) {
					handled = handleBackKeyPressed();					
				}
				break;
			}
			case KeyEvent.KEYCODE_CAMERA : {
				if (window.hasListeners(TiC.EVENT_ANDROID_CAMERA)) {
					if (event.getAction() == KeyEvent.ACTION_UP) {
						window.fireEvent(TiC.EVENT_ANDROID_CAMERA, null);
					}
					handled = true;
				}
				break;
			}
			case KeyEvent.KEYCODE_FOCUS : {
				if (window.hasListeners(TiC.EVENT_ANDROID_FOCUS)) {
					if (event.getAction() == KeyEvent.ACTION_UP) {
						window.fireEvent(TiC.EVENT_ANDROID_FOCUS, null);
					}
					handled = true;
				}

				break;
			}
			case KeyEvent.KEYCODE_SEARCH : {
				if (window.hasListeners(TiC.EVENT_ANDROID_SEARCH)) {
					if (event.getAction() == KeyEvent.ACTION_UP) {
						window.fireEvent(TiC.EVENT_ANDROID_SEARCH, null);
					}
					handled = true;
				}

				break;
			}
			case KeyEvent.KEYCODE_VOLUME_UP : {
				if (window.hasListeners(TiC.EVENT_ANDROID_VOLUP)) {
					if (event.getAction() == KeyEvent.ACTION_UP) {
						window.fireEvent(TiC.EVENT_ANDROID_VOLUP, null);
					}
					handled = true;
				}

				break;
			}
			case KeyEvent.KEYCODE_VOLUME_DOWN : {
				if (window.hasListeners(TiC.EVENT_ANDROID_VOLDOWN)) {
					if (event.getAction() == KeyEvent.ACTION_UP) {
						window.fireEvent(TiC.EVENT_ANDROID_VOLDOWN, null);
					}
					handled = true;
				}

				break;
			}
		}
			
		if (!handled) {
			handled = super.dispatchKeyEvent(event);
		}

		return handled;
	}
	
	private ActionBarProxy getActionBarProxy(){
		if (activityProxy != null) {
			return activityProxy.getActionBar();
		}
		return null;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// If targetSdkVersion is set to 11+, Android will invoke this function 
		// to initialize the menu (since it's part of the action bar). Due
		// to the fix for Android bug 2373, activityProxy won't be initialized b/c the
		// activity is expected to restart, so we will ignore it.
		if (activityProxy == null) {
			return false;
		}

		if (menuHelper == null) {
			menuHelper = new TiMenuSupport(activityProxy);
		}

		return menuHelper.onCreateOptionsMenu(super.onCreateOptionsMenu(menu), menu);
	}
	
	public Menu getMenu() {
	    if (menuHelper != null) {
	        return menuHelper.getMenu();
	    }
	    return null;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId()) {
			case android.R.id.home:
				synchronized (interceptOnHomePressedListeners.synchronizedList()) {
					for (interceptOnHomePressedEvent listener : interceptOnHomePressedListeners.nonNull()) {
						try {
							if (listener.interceptOnHomePressed()) {
								return true;
							}

						} catch (Throwable t) {
							Log.e(TAG, "Error dispatching interceptOnHomePressed event: " + t.getMessage(), t);
						}
					}
				}
				
				ActionBarProxy actionBarProxy = getActionBarProxy();
				if (actionBarProxy != null) {
					KrollFunction onHomeIconItemSelected = (KrollFunction) actionBarProxy
						.getProperty(TiC.PROPERTY_ON_HOME_ICON_ITEM_SELECTED);
					if (onHomeIconItemSelected != null) {
						KrollDict event = new KrollDict();
						event.put(TiC.EVENT_PROPERTY_SOURCE, actionBarProxy);
						event.put(TiC.EVENT_PROPERTY_WINDOW, window);
						onHomeIconItemSelected.call(activityProxy.getKrollObject(), new Object[] { event });
						return true;
					}
				}
				
				
				return true;
			default:
				return menuHelper.onOptionsItemSelected(item);
		}
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		return menuHelper.onPrepareOptionsMenu(super.onPrepareOptionsMenu(menu), menu);
	}

	public static void callOrientationChangedListener(Configuration newConfig) 
	{
		if (orientationChangedListener != null && previousOrientation != newConfig.orientation) {
			previousOrientation = newConfig.orientation;
			orientationChangedListener.onOrientationChanged (newConfig.orientation);
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);

		for (WeakReference<ConfigurationChangedListener> listener : configChangedListeners) {
			if (listener.get() != null) {
				listener.get().onConfigurationChanged(this, newConfig);
			}
		}

		callOrientationChangedListener(newConfig);
	}

	@Override
	protected void onNewIntent(Intent intent) 
	{
		super.onNewIntent(intent);

		Log.d(TAG, "Activity " + this + " onNewIntent", Log.DEBUG_MODE);

		if (activityProxy != null) {
			IntentProxy ip = new IntentProxy(intent);
			KrollDict data = new KrollDict();
			data.put(TiC.PROPERTY_INTENT, ip);
			activityProxy.fireSyncEvent(TiC.EVENT_NEW_INTENT, data);
			// TODO: Deprecate old event
			activityProxy.fireSyncEvent("newIntent", data);
		}
	}

	public void addOnLifecycleEventListener(OnLifecycleEvent listener)
	{
		lifecycleListeners.add(new WeakReference<OnLifecycleEvent>(listener));
	}

	public void addOnWindowFocusChangedEventListener(OnWindowFocusChangedEvent listener)
	{
		windowFocusChangedListeners.add(new WeakReference<OnWindowFocusChangedEvent>(listener));
	}

	public void addInterceptOnBackPressedEventListener(interceptOnBackPressedEvent listener)
	{
		interceptOnBackPressedListeners.add(new WeakReference<interceptOnBackPressedEvent>(listener));
	}
	
	public void removeInterceptOnBackPressedEventListener(interceptOnBackPressedEvent listener)
	{
		for (int i = 0; i < interceptOnBackPressedListeners.size(); i++) {
			interceptOnBackPressedEvent iListener = interceptOnBackPressedListeners.get(i).get();
			if (listener == iListener) {
				interceptOnBackPressedListeners.remove(i);
				return;
			}
		}
	}
	
	public void addInterceptOnHomePressedEventListener(interceptOnHomePressedEvent listener)
	{
		interceptOnHomePressedListeners.add(new WeakReference<interceptOnHomePressedEvent>(listener));
	}
	
	public void removeInterceptOnHomePressedEventListener(interceptOnHomePressedEvent listener)
	{
		for (int i = 0; i < interceptOnHomePressedListeners.size(); i++) {
			interceptOnHomePressedEvent iListener = interceptOnHomePressedListeners.get(i).get();
			if (listener == iListener) {
				interceptOnHomePressedListeners.remove(i);
				return;
			}
		}
	}

	public void removeOnLifecycleEventListener(OnLifecycleEvent listener)
	{
		for (int i = 0; i < lifecycleListeners.size(); i++) {
			OnLifecycleEvent iListener = lifecycleListeners.get(i).get();
			if (listener == iListener) {
				lifecycleListeners.remove(i);
				return;
			}
		}
	}

	private void releaseDialogs(boolean finish)
	{
		//clean up dialogs when activity is pausing or finishing
		for (Iterator<DialogWrapper> iter = dialogs.iterator(); iter.hasNext(); ) {
			DialogWrapper p = iter.next();
			Dialog dialog = p.getDialog();
			boolean persistent = p.getPersistent();
			//if the activity is pausing but not finishing, clean up dialogs only if
			//they are non-persistent
			
			if (finish || !persistent) {
				if (dialog != null && dialog.isShowing()) {
					if (dialog.getCurrentFocus() != null) {
						TiUIHelper.hideSoftKeyboard(dialog.getCurrentFocus());
					}
					dialog.dismiss();
				}
				dialogs.remove(p);
			}
			else {
				if (dialog != null && dialog.getCurrentFocus() != null) {
					TiUIHelper.hideSoftKeyboard(dialog.getCurrentFocus());
				}
			}
		}
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus)
	{
		synchronized (windowFocusChangedListeners.synchronizedList()) {
			for (OnWindowFocusChangedEvent listener : windowFocusChangedListeners.nonNull()) {
				try {
					listener.onWindowFocusChanged(hasFocus);

				} catch (Throwable t) {
					Log.e(TAG, "Error dispatching onWindowFocusChanged event: " + t.getMessage(), t);
				}
			}
		}
		super.onWindowFocusChanged(hasFocus);
	}
	
	@Override
    public void startActivity(Intent intent) {
		//this activity onPause is called before the new activity onCreate :s
		//this prevent unwanted pause events to be sent
		super.startActivity(intent);
    }
	
	@Override
	/**
	 * When this activity pauses, this method sets the current activity to null, fires a javascript 'pause' event,
	 * and if the activity is finishing, remove all dialogs associated with it.
	 */
	protected void onPause() 
	{
		TiApplication.getInstance().activityPaused(this); //call before setting inForeground
		inForeground = false;
		super.onPause();
		isResumed = false;
		isPaused = true;

		Log.d(TAG, "Activity " + this + " onPause", Log.DEBUG_MODE);

		TiApplication tiApp = getTiApp();
		if (tiApp.isRestartPending()) {
			releaseDialogs(true);
			if (!isFinishing()) {
				finish();
			}
			return;
		}

		if (!windowStack.empty()) {
			windowStack.peek().onWindowFocusChange(false);
		}
	
		TiApplication.updateActivityTransitionState(true);
		tiApp.setCurrentActivity(this, null);
		TiUIHelper.hideSoftKeyboard(getWindow().getDecorView());

		//release non-persistent dialogs
		releaseDialogs(this.isFinishing());

		if (activityProxy != null) {
			activityProxy.fireEvent(TiC.EVENT_PAUSE, null);
		}

		synchronized (lifecycleListeners.synchronizedList()) {
			for (OnLifecycleEvent listener : lifecycleListeners.nonNull()) {
				try {
					TiLifecycle.fireLifecycleEvent(this, listener, TiLifecycle.LIFECYCLE_ON_PAUSE);

				} catch (Throwable t) {
					Log.e(TAG, "Error dispatching lifecycle event: " + t.getMessage(), t);
				}
			}
		}

		// Checkpoint for ti.end event
		if (tiApp != null && tiApp.collectAnalytics()) {
			APSAnalytics.sendSessionBackgroundEvent();
		}
	}

	@Override
	/**
	 * When the activity resumes, this method updates the current activity to this and fires a javascript
	 * 'resume' event.
	 */
	protected void onResume()
	{
		TiApplication.getInstance().activityResumed(this);
		inForeground = true;
		super.onResume();
		if (isFinishing()) {
			return;
		}

		Log.d(TAG, "Activity " + this + " onResume", Log.DEBUG_MODE);

		TiApplication tiApp = getTiApp();
		if (tiApp.isRestartPending()) {
			if (!isFinishing()) {
				finish();
			}
			return;
		}

		if (!windowStack.empty()) {
			windowStack.peek().onWindowFocusChange(true);
		} 
		
		tiApp.setCurrentActivity(this, this);
		TiApplication.updateActivityTransitionState(false);
		
		if (activityProxy != null) {
			activityProxy.fireEvent(TiC.EVENT_RESUME, null);
		}
		
		synchronized (lifecycleListeners.synchronizedList()) {
			for (OnLifecycleEvent listener : lifecycleListeners.nonNull()) {
				try {
					TiLifecycle.fireLifecycleEvent(this, listener, TiLifecycle.LIFECYCLE_ON_RESUME);

				} catch (Throwable t) {
					Log.e(TAG, "Error dispatching lifecycle event: " + t.getMessage(), t);
				}
			}
		}

		isResumed = true;
		isPaused = false;

		// Checkpoint for ti.start event
		//String deployType = tiApp.getAppProperties().getString("ti.deploytype", "unknown");
        if (tiApp.collectAnalytics()) {
            APSAnalytics.sendSessionForegroundEvent();
        }
	}
	
//	@Override
//	public void startActivity(Intent intent)	{
//		TiApplication.getInstance().setStartingActivity(true);
//		super.startActivity(intent);
//	}
	
	@Override
	/**
	 * When this activity starts, this method updates the current activity to this if necessary and
	 * fire javascript 'start' and 'focus' events. Focus events will only fire if 
	 * the activity is not a tab activity.
	 */
	protected void onStart()
	{
		inForeground = true;
		super.onStart();
		if (isFinishing()) {
			return;
		}

		// Newer versions of Android appear to turn this on by default.
		// Turn if off until an activity indicator is shown.
		setProgressBarIndeterminateVisibility(false);

		Log.d(TAG, "Activity " + this + " onStart", Log.DEBUG_MODE);

		TiApplication tiApp = getTiApp();

		if (tiApp.isRestartPending()) {
			if (!isFinishing()) {
				finish();
			}
			return;
		}

		updateTitle(this.window);

		if (activityProxy != null) {
			// we only want to set the current activity for good in the resume state but we need it right now.
			// save off the existing current activity, set ourselves to be the new current activity temporarily 
			// so we don't run into problems when we give the proxy the event
			Activity tempCurrentActivity = tiApp.getCurrentActivity();
			tiApp.setCurrentActivity(this, this);

			activityProxy.fireEvent(TiC.EVENT_START, null);

			// set the current activity back to what it was originally
			tiApp.setCurrentActivity(this, tempCurrentActivity);
		}

		synchronized (lifecycleListeners.synchronizedList()) {
			for (OnLifecycleEvent listener : lifecycleListeners.nonNull()) {
				try {
					TiLifecycle.fireLifecycleEvent(this, listener, TiLifecycle.LIFECYCLE_ON_START);

				} catch (Throwable t) {
					Log.e(TAG, "Error dispatching lifecycle event: " + t.getMessage(), t);
				}
			}
		}
		// store current configuration orientation
		// This fixed bug with double orientation chnage firing when activity starts in landscape 
		previousOrientation = getResources().getConfiguration().orientation;
	}

	@Override
	/**
	 * When this activity stops, this method fires the javascript 'blur' and 'stop' events. Blur events will only fire
	 * if the activity is not a tab activity.
	 */
	protected void onStop()
	{
		TiApplication.getInstance().activityStopped(this);
		inForeground = false;
		super.onStop();

		Log.d(TAG, "Activity " + this + " onStop", Log.DEBUG_MODE);

		if (getTiApp().isRestartPending()) {
			if (!isFinishing()) {
				finish();
			}
			return;
		}

		if (activityProxy != null) {
			activityProxy.fireEvent(TiC.EVENT_STOP, null);
		}

		synchronized (lifecycleListeners.synchronizedList()) {
			for (OnLifecycleEvent listener : lifecycleListeners.nonNull()) {
				try {
					TiLifecycle.fireLifecycleEvent(this, listener, TiLifecycle.LIFECYCLE_ON_STOP);

				} catch (Throwable t) {
					Log.e(TAG, "Error dispatching lifecycle event: " + t.getMessage(), t);
				}
			}
		}
		KrollRuntime.suggestGC();
	}

	@Override
	/**
	 * When this activity restarts, this method updates the current activity to this and fires javascript 'restart'
	 * event.
	 */
	protected void onRestart()
	{
		inForeground = true;
		super.onRestart();

		Log.d(TAG, "Activity " + this + " onRestart", Log.DEBUG_MODE);
		
		TiApplication tiApp = getTiApp();
		if (tiApp.isRestartPending()) {
			if (!isFinishing()) {
				finish();
			}

			return;
		}

		if (activityProxy != null) {
			// we only want to set the current activity for good in the resume state but we need it right now.
			// save off the existing current activity, set ourselves to be the new current activity temporarily 
			// so we don't run into problems when we give the proxy the event
			Activity tempCurrentActivity = tiApp.getCurrentActivity();
			tiApp.setCurrentActivity(this, this);

			activityProxy.fireEvent(TiC.EVENT_RESTART, null);

			// set the current activity back to what it was originally
			tiApp.setCurrentActivity(this, tempCurrentActivity);
		}
	}

	@Override
	/**
	 * When the activity is about to go into the background as a result of user choice, this method fires the 
	 * javascript 'userleavehint' event.
	 */
	protected void onUserLeaveHint()
	{
		Log.d(TAG, "Activity " + this + " onUserLeaveHint", Log.DEBUG_MODE);

		if (getTiApp().isRestartPending()) {
			if (!isFinishing()) {
				finish();
			}
			return;
		}

		if (activityProxy != null) {
			activityProxy.fireEvent(TiC.EVENT_USER_LEAVE_HINT, null);
		}

		super.onUserLeaveHint();
	}
	
	@Override
	/**
	 * When this activity is destroyed, this method removes it from the activity stack, performs
	 * clean up, and fires javascript 'destroy' event. 
	 */
	protected void onDestroy()
	{
		Log.d(TAG, "Activity " + this + " onDestroy", Log.DEBUG_MODE);

		inForeground = false;
		TiApplication tiApp = getTiApp();
		//Clean up dialogs when activity is destroyed. 
		releaseDialogs(true);

		if (tiApp.isRestartPending()) {
			super.onDestroy();
			if (!isFinishing()) {
				finish();
			}
			return;
		}

		synchronized (lifecycleListeners.synchronizedList()) {
			for (OnLifecycleEvent listener : lifecycleListeners.nonNull()) {
				try {
					TiLifecycle.fireLifecycleEvent(this, listener, TiLifecycle.LIFECYCLE_ON_DESTROY);

				} catch (Throwable t) {
					Log.e(TAG, "Error dispatching lifecycle event: " + t.getMessage(), t);
				}
			}
		}

		super.onDestroy();

		boolean isFinishing = isFinishing();

		// If the activity is finishing, remove the windowId and supportHelperId so the window and supportHelper can be released.
		// If the activity is forced to destroy by Android OS, keep the windowId and supportHelperId so the activity can be recovered.
		if (isFinishing) {
			int windowId = getIntentInt(TiC.INTENT_PROPERTY_WINDOW_ID, -1);
			TiActivityWindows.removeWindow(windowId);
			TiActivitySupportHelpers.removeSupportHelper(supportHelperId);
		}

		fireOnDestroy();

		if (layout instanceof TiCompositeLayout) {
			Log.d(TAG, "Layout cleanup.", Log.DEBUG_MODE);
			((TiCompositeLayout) layout).removeAllViews();
		}
		layout = null;

		//LW windows
		if (window == null && view != null) {
			view.releaseViews(isFinishing);
			view = null;
		}
		
		if (!windowStack.isEmpty()) {
			Iterator itr = windowStack.iterator();
		    while( itr.hasNext() ) {
		        TiWindowProxy window = (TiWindowProxy)itr.next();
		        window.closeFromActivity(isFinishing);
		    }
		}

		if (window != null) {
			window.closeFromActivity(isFinishing);
			window = null;
		}

		if (menuHelper != null) {
			menuHelper.destroy();
			menuHelper = null;
		}

		if (activityProxy != null) {
			activityProxy.release();
			activityProxy = null;
		}

		// Don't dispose the runtime if the activity is forced to destroy by Android,
		// so we can recover the activity later.
		KrollRuntime.decrementActivityRefCount(isFinishing);
		KrollRuntime.suggestGC();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);

		// If the activity is forced to destroy by Android, save the supportHelperId so
		// we can get it back when the activity is recovered.
		if (!isFinishing() && supportHelper != null) {
			outState.putInt("supportHelperId", supportHelperId);
		}
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState)
	{
		super.onRestoreInstanceState(savedInstanceState);

		if (savedInstanceState.containsKey("supportHelperId")) {
			supportHelperId = savedInstanceState.getInt("supportHelperId");
			supportHelper = TiActivitySupportHelpers.retrieveSupportHelper(this, supportHelperId);
			if (supportHelper == null) {
				Log.e(TAG, "Unable to retrieve the activity support helper.");
			}
		}
	}

	// called in order to ensure that the onDestroy call is only acted upon once.
	// should be called by any subclass
	protected void fireOnDestroy()
	{
		if (!onDestroyFired) {
			if (activityProxy != null) {
				activityProxy.fireEvent(TiC.EVENT_DESTROY, null);
			}
			onDestroyFired = true;
		}
	}

	protected boolean shouldFinishRootActivity()
	{
		if (window != null)
			return window.shouldExitOnClose();
		return false;
	}

	@Override
	public void finish()
	{
		finish(false);
	}

	public void finish(boolean force)
	{
		super.finish();
		
		if (shouldFinishRootActivity() || force == true) {
			TiApplication app = getTiApp();
			if (app != null) {
				TiRootActivity rootActivity = app.getRootActivity();
				if (rootActivity != null && !(rootActivity.equals(this)) && !rootActivity.isFinishing()) {
					rootActivity.finish();
				} else if (rootActivity == null && !app.isRestartPending()) {
					// When the root activity has been killed and garbage collected and the app is not scheduled to restart,
					// we need to force finish the root activity while this activity has an intent to finish root.
					// This happens when the "Don't keep activities" option is enabled and the user stays in some activity
					// (eg. heavyweight window, tabgroup) other than the root activity for a while and then he wants to back
					// out the app.
					app.setForceFinishRootActivity(true);
				}
			}
		}
	}

	// These activityOnXxxx are all used by TiLaunchActivity when
	// the android bug 2373 is detected and the app is being re-started.
	// By calling these from inside its on onXxxx handlers, TiLaunchActivity
	// can avoid calling super.onXxxx (super being TiBaseActivity), which would
	// result in a bunch of Titanium-specific code running when we don't need it
	// since we are restarting the app as fast as possible. Calling these methods
	// allows TiLaunchActivity to fulfill the requirement that the Android built-in
	// Activity's onXxxx must be called. (Think of these as something like super.super.onXxxx
	// from inside TiLaunchActivity.)
	protected void activityOnPause()
	{
		super.onPause();
	}
	protected void activityOnRestart()
	{
		super.onRestart();
	}
	protected void activityOnResume()
	{
		super.onResume();
	}
	protected void activityOnStop()
	{
		super.onStop();
	}
	protected void activityOnStart()
	{
		super.onStart();
	}
	protected void activityOnDestroy()
	{
		super.onDestroy();
	}

	public void activityOnCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
	}

	/**
	 * Called by the onCreate methods of TiBaseActivity to determine if an unsupported application
	 * re-launch appears to be occurring.
	 * @param activity The Activity getting the onCreate
	 * @param savedInstanceState The argument passed to the onCreate. A non-null value is a "tell"
	 * that the system is re-starting a killed application.
	 */
	public static boolean isUnsupportedReLaunch(Activity activity, Bundle savedInstanceState)
	{
		// If all the activities has been killed and the runtime has been disposed, we have to relaunch
		// the app.
		if (KrollRuntime.getInstance().getRuntimeState() == KrollRuntime.State.DISPOSED &&
				savedInstanceState != null && !(activity instanceof TiLaunchActivity)) {
			return true;
		}
		return false;
	}
	
	@Override
	protected Dialog onCreateDialog (int id, Bundle args) {
		Log.d(TAG, "onCreateDialog");
		TiApplication.getInstance().cancelPauseEvent();
		return super.onCreateDialog(id, args);
	}
}

