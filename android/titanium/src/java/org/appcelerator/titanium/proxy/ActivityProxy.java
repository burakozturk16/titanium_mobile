/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package org.appcelerator.titanium.proxy;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollFunction;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiBaseActivity;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.util.TiActivityHelper;
import org.appcelerator.titanium.util.TiActivityResultHandler;
import org.appcelerator.titanium.util.TiActivitySupport;
import org.appcelerator.titanium.util.TiActivitySupportHelper;
import org.appcelerator.titanium.util.TiConvert;

import android.app.Activity;
import android.content.Intent;
import android.os.Message;

@Kroll.proxy(propertyAccessors = {
	TiC.PROPERTY_ON_CREATE_OPTIONS_MENU,
	TiC.PROPERTY_ON_PREPARE_OPTIONS_MENU,
	TiC.PROPERTY_ON_CREATE,
	TiC.PROPERTY_ON_START,
	TiC.PROPERTY_ON_RESTART,
	TiC.PROPERTY_ON_RESUME,
	TiC.PROPERTY_ON_PAUSE,
	TiC.PROPERTY_ON_STOP,
	TiC.PROPERTY_ON_DESTROY
})
/**
 * This is a proxy representation of the Android Activity type.
 * Refer to <a href="http://developer.android.com/reference/android/app/Activity.html">Android Activity</a>
 * for more details.
 */
public class ActivityProxy extends KrollProxy
	implements TiActivityResultHandler
{
	private static final String TAG = "ActivityProxy";
	private static final int MSG_FIRST_ID = KrollProxy.MSG_LAST_ID + 1;
	private static final int MSG_INVALIDATE_OPTIONS_MENU = MSG_FIRST_ID + 100;
	private static final int MSG_OPEN_OPTIONS_MENU = MSG_FIRST_ID + 101;

	protected Activity wrappedActivity;
	protected IntentProxy intentProxy;
	protected DecorViewProxy savedDecorViewProxy;
	protected ActionBarProxy actionBarProxy;

	private KrollFunction resultCallback;
	
	public ActivityProxy()
	{
	}

	public ActivityProxy(Activity activity)
	{
		this();
		setActivity(activity);
		setWrappedActivity(activity);
	}

	public void setWrappedActivity(Activity activity)
	{
		this.wrappedActivity = activity;
		if (actionBarProxy != null) {
		    actionBarProxy.setActivity(this.wrappedActivity);
		}
		if (this.wrappedActivity != null) {
		    Intent intent = activity.getIntent();
	        if (intent != null) {
	            intentProxy = new IntentProxy(activity.getIntent());
	        }
		} else {
		    intentProxy = null;
		}
	}

	protected Activity getWrappedActivity()
	{
		if (wrappedActivity != null) {
			return wrappedActivity;
		}
		return TiApplication.getInstance().getRootActivity();
	}

	@Kroll.method
	public DecorViewProxy getDecorView()
	{
		if (savedDecorViewProxy == null) {
			Activity activity = getActivity();
			if (!(activity instanceof TiBaseActivity)) {
				Log.e(TAG, "Unable to return decor view, activity is not TiBaseActivity", Log.DEBUG_MODE);

				return null;
			}

			DecorViewProxy decorViewProxy = new DecorViewProxy(((TiBaseActivity)activity).getLayout());
			decorViewProxy.setActivity(activity);
			savedDecorViewProxy = decorViewProxy;
		}
		
		return savedDecorViewProxy;
		
	}

	@Kroll.method
	public void startActivity(Object intentValue)
	{
	    IntentProxy intent = IntentProxy.fromObject(intentValue);
	    if (intent != null) {
	        Activity activity = getWrappedActivity();
	        if (activity != null) {
	            activity.startActivity(intent.getIntent());
	        }
	    }
		
	}

	@Kroll.method
	public void startActivityForResult(Object intentValue, KrollFunction callback)
	{
	    IntentProxy intent = IntentProxy.fromObject(intentValue);
        if (intent == null) { 
            return;
        }
		Activity activity = getWrappedActivity();
		if (activity != null) {
			TiActivitySupport support = null;
			if (activity instanceof TiActivitySupport) {
				support = (TiActivitySupport)activity;
			} else {
				support = new TiActivitySupportHelper(activity);
			}

			this.resultCallback = callback;
			int requestCode = support.getUniqueResultCode();
			support.launchActivityForResult(intent.getIntent(), requestCode, this);
		}
	}

	@Kroll.method
	public void startActivityFromChild(ActivityProxy child, Object intentValue, int requestCode)
	{
	    IntentProxy intent = IntentProxy.fromObject(intentValue);
        if (intent == null) { 
            return;
        }
		Activity activity = getWrappedActivity();
		if (activity != null) {
			activity.startActivityFromChild(child.getWrappedActivity(), intent.getIntent(), requestCode);
		}
	}

	@Kroll.method
	public boolean startActivityIfNeeded(Object intentValue, int requestCode)
	{
	    IntentProxy intent = IntentProxy.fromObject(intentValue);
        if (intent == null) { 
            return false;
        }
		Activity activity = getWrappedActivity();
		if (activity != null) {
			return activity.startActivityIfNeeded(intent.getIntent(), requestCode);
		}
		return false;
	}

	@Kroll.method
	public boolean startNextMatchingActivity(Object intentValue)
	{
	    IntentProxy intent = IntentProxy.fromObject(intentValue);
        if (intent == null) { 
            return false;
        }
		Activity activity = getWrappedActivity();
		if (activity != null) {
			return activity.startNextMatchingActivity(intent.getIntent());
		}
		return false;
	}

	@Kroll.method
	public void sendBroadcast(Object intentValue)
	{
	    IntentProxy intent = IntentProxy.fromObject(intentValue);
        if (intent == null) { 
            return;
        }
		Activity activity = getWrappedActivity();
		if (activity != null) {
			activity.sendBroadcast(intent.getIntent());
		}
	}

	@Kroll.method
	public void sendBroadcastWithPermission(Object intentValue, @Kroll.argument(optional = true) String receiverPermission)
	{
	    IntentProxy intent = IntentProxy.fromObject(intentValue);
        if (intent == null) { 
            return;
        }
		Activity activity = getWrappedActivity();
		if (activity != null) {
			activity.sendBroadcast(intent.getIntent(), receiverPermission);
		}
	}
	
	@Kroll.method
	public String getString(int resId, Object[] formatArgs)
	{
		Activity activity = getWrappedActivity();
		if (activity != null) {
			if (formatArgs == null || formatArgs.length == 0) {
				return activity.getString(resId);
			} else {
				return activity.getString(resId, formatArgs);
			}
		}
		return null;
	}

	@Kroll.method @Kroll.getProperty
	public IntentProxy getIntent()
	{
		return intentProxy;
	}

	@Kroll.method @Kroll.setProperty
	public void setRequestedOrientation(int orientation)
	{
		Activity activity = getWrappedActivity();
		if (activity != null) {
			activity.setRequestedOrientation(orientation);
		}
	}

	@Kroll.method
	public void setResult(int resultCode,
		@Kroll.argument(optional=true) Object intentValue)
	{
	    IntentProxy intent = IntentProxy.fromObject(intentValue);

		Activity activity = getWrappedActivity();
		if (activity != null) {
			if (intent == null) {
				activity.setResult(resultCode);
			} else {
				activity.setResult(resultCode, intent.getIntent());
			}
		}
	}

	@Kroll.method
	public void finish()
	{
		Activity activity = getWrappedActivity();
		if (activity != null) {
			activity.finish();
		}
	}

	@Kroll.method
	public String getDir(String name, int mode)
	{
		Activity activity = getWrappedActivity();
		if (activity != null) {
			return activity.getDir(name, mode).getAbsolutePath();
		}
		return null;
	}

	@Kroll.method @Kroll.getProperty
	public TiWindowProxy getWindow()
	{
		Activity activity = getWrappedActivity();
		if (!(activity instanceof TiBaseActivity)) {
			return null;
		}

		TiBaseActivity tiActivity = (TiBaseActivity) activity;
		return tiActivity.getWindowProxy();
	}

	@Kroll.method @Kroll.getProperty
	public ActionBarProxy getActionBar()
	{
		return getOrCreateActionBarProxy();
	}
	
    public ActionBarProxy getActionBarProxy() {
        return actionBarProxy;
    }

    public ActionBarProxy getOrCreateActionBarProxy() {
        if (actionBarProxy == null) {
            TiBaseActivity activity = (TiBaseActivity) getWrappedActivity();
            actionBarProxy = new ActionBarProxy(activity);
        }

        return actionBarProxy;
    }

    @Kroll.method
    @Kroll.getProperty
    public double getActionBarHeight() {
        return TiActivityHelper.getActionBarHeight(getWrappedActivity());
    }

	@Kroll.method
	public void openOptionsMenu()
	{
		if (TiApplication.isUIThread()) {
			handleOpenOptionsMenu();
		} else {
			getMainHandler().obtainMessage(MSG_OPEN_OPTIONS_MENU).sendToTarget();
		}
	}

	@Kroll.method
	public void invalidateOptionsMenu()
	{
		if (TiApplication.isUIThread()) {
			handleInvalidateOptionsMenu();
		} else {
			getMainHandler().obtainMessage(MSG_INVALIDATE_OPTIONS_MENU).sendToTarget();
		}
	}
	
	@Kroll.method
	public void moveTaskToBack()
	{
		Activity activity = getWrappedActivity();
		if (activity != null) {
			activity.moveTaskToBack(true);
		}
	}

	private void handleOpenOptionsMenu()
	{
		Activity activity = getWrappedActivity();
		if (activity != null) {
			activity.openOptionsMenu();
		}
	}

	private void handleInvalidateOptionsMenu()
	{
		TiBaseActivity activity = (TiBaseActivity) getWrappedActivity();
		if (activity != null) {
			activity.supportInvalidateOptionsMenu();
		}
	}

	public void onResult(Activity activity, int requestCode, int resultCode, Intent data)
	{
		IntentProxy intent = null;
		if (data != null) {
			intent = new IntentProxy(data);
		}

		KrollDict event = new KrollDict();
		event.put(TiC.EVENT_PROPERTY_REQUEST_CODE, requestCode);
		event.put(TiC.EVENT_PROPERTY_RESULT_CODE, resultCode);
		event.put(TiC.EVENT_PROPERTY_INTENT, intent);
		event.put(TiC.EVENT_PROPERTY_SOURCE, this);
		this.resultCallback.callAsync(krollObject, event);
	}

	public void onError(Activity activity, int requestCode, Exception e)
	{
		KrollDict event = new KrollDict();
		event.put(TiC.EVENT_PROPERTY_REQUEST_CODE, requestCode);
		event.putCodeAndMessage(TiC.ERROR_CODE_UNKNOWN, e.getMessage());
		event.put(TiC.EVENT_PROPERTY_SOURCE, this);
		this.resultCallback.callAsync(krollObject, event);
	}

	public void release()
	{
		super.release();
		if (actionBarProxy != null) {
		    actionBarProxy.release();
		    actionBarProxy = null;
		}
		wrappedActivity = null;
	}

	@Override
	public boolean handleMessage(Message msg)
	{
		switch (msg.what) {
			case MSG_INVALIDATE_OPTIONS_MENU: {
				handleInvalidateOptionsMenu();
				return true;
			}
			case MSG_OPEN_OPTIONS_MENU: {
				handleOpenOptionsMenu();
				return true;
			}
		}
		return super.handleMessage(msg);
	}
	
	public void propertySet(String key, Object newValue, Object oldValue,
            boolean changedProperty) {
	    switch (key) {
	    case TiC.PROPERTY_ACTION_BAR:
            ActionBarProxy actionBarProxy = getActionBar();
            if (actionBarProxy != null) {
                actionBarProxy.setProperties(TiConvert.toKrollDict(newValue));
                invalidateOptionsMenu();
            }
            break;

        case TiC.PROPERTY_ON_CREATE_OPTIONS_MENU:
        case TiC.PROPERTY_ON_PREPARE_OPTIONS_MENU:
            if (changedProperty) {
                invalidateOptionsMenu();
            }
            break;

        default:
            break;
        }
	}
	
	   
    @Override
    public void onPropertyChanged(String key, Object newValue, Object oldValue) {
        propertySet(key, newValue, oldValue, true);
    }
	
    @Override
    public void handleCreationDict(final KrollDict options)
    {
        super.handleCreationDict(options);
        if (!options.containsKey(TiC.PROPERTY_ACTION_BAR)) {
            propertySet(TiC.PROPERTY_ACTION_BAR, null, null, false);
        }
    }

	@Override
	public String getApiName()
	{
		return "Ti.Android.Activity";
	}
}
