/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.app;

import org.appcelerator.kroll.KrollModule;
import org.appcelerator.kroll.KrollRuntime;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiBaseActivity;
import org.appcelerator.titanium.TiContext;
import org.appcelerator.titanium.proxy.ActivityProxy;
import org.appcelerator.titanium.proxy.IntentProxy;
import org.appcelerator.titanium.proxy.RProxy;

import android.app.Activity;
import android.content.Intent;

@Kroll.module(parentModule=AppModule.class)
public class AndroidModule extends KrollModule
{
	protected RProxy r;
	private static final String TAG = "App.AndroidModule";


	public AndroidModule()
	{
		super();
	}

	public AndroidModule(TiContext context)
	{
		this();
	}

	@Kroll.getProperty(name="R")
	public RProxy getR()
	{
		if (r == null) {
			r = new RProxy(RProxy.RESOURCE_TYPE_APPLICATION);
		}
		return r;
	}
	
	@Kroll.getProperty(name="wasStartedAtBoot")
	public Boolean wasStartedAtBoot()
	{
		TiApplication tiApp = TiApplication.getInstance();
		return tiApp.getRootActivity().wasStartedAtBoot();
	}

	// this shouldn't be called from anything other than the runtime thread
	@Kroll.method
	public ActivityProxy getTopActivity()
	{
		if (KrollRuntime.getActivityRefCount() == 0) {
			// No activity to wait for. This can be the case if, for example,
			// the Application is being started for a Service, not an Activity.
			return null;
		}
		TiApplication tiApp = TiApplication.getInstance();
		Activity activity = tiApp.getCurrentActivity();
		if (activity == null || !(activity instanceof TiBaseActivity)) {
			try {
				tiApp.rootActivityLatch.await();
				activity = tiApp.getRootActivity();
			} catch (InterruptedException e) {
				Log.e(TAG, "Interrupted awaiting rootActivityLatch");
			}
		}

		if (activity instanceof TiBaseActivity) {
			return ((TiBaseActivity)activity).getActivityProxy();
		} else {
			return null;
		}
	}

	@Kroll.getProperty @Kroll.method
	public IntentProxy getLaunchIntent()
	{
		TiApplication app = TiApplication.getInstance();
		if (app != null) {
			TiBaseActivity rootActivity = app.getRootActivity();
			if (rootActivity != null) {
				Intent intent = rootActivity.getIntent();
				if (intent != null) {
					return new IntentProxy(intent);
				}
			}
		}
		return null;
	}

	@Override
	public String getApiName()
	{
		return "Ti.App.Android";
	}
	
    @Kroll.method
    public int getGooglePlayServicesState() {
        return TiApplication.getGooglePlayServicesState();
    }

}

