/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2010-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package org.appcelerator.kroll;

import java.util.ArrayList;

import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiBaseActivity;
import org.appcelerator.titanium.TiContext;
import org.appcelerator.titanium.TiLifecycle.OnLifecycleEvent;
import org.appcelerator.titanium.proxy.ReusableProxy;

import android.app.Activity;

/**
 * This is the parent class for all modules. All modules must extend this class.
 */
@Kroll.module(name="KrollModule")
public class KrollModule extends ReusableProxy
	implements KrollProxyListener, OnLifecycleEvent
{
	@Deprecated
	protected TiContext tiContext;

	protected static ArrayList<KrollModuleInfo> customModuleInfoList = new ArrayList<KrollModuleInfo>();

	public static void addCustomModuleInfo(KrollModuleInfo customModuleInfo)
	{
		customModuleInfoList.add(customModuleInfo);
	}

	public static ArrayList<KrollModuleInfo> getCustomModuleInfoList()
	{
		return customModuleInfoList;
	}

	/**
	 * Constructs a new KrollModule object.
	 * @module.api
	 */
	public KrollModule()
	{
		super();
	}

	/**
	 * Instantiates and registers module with TiApplication.
	 * @param name the name of module.
	 * @module.api
	 */
	public KrollModule(String name)
	{
		this();
		// Register module with TiApplication if a name is provided.
		TiApplication.getInstance().registerModuleInstance(name, this);
	}

	public KrollModule(TiContext tiContext)
	{
		this();
		this.tiContext = tiContext;
	}
	
	public void onAppTerminate(TiApplication app)
	{
	}
	
	public void onAppPaused()
    {
    }
	
	public void onAppResumed()
    {
    }

	@Override
	protected void initActivity(Activity activity)
	{
		Activity moduleActivity = TiApplication.getInstance().getRootActivity();
		if (moduleActivity == null) {
			// this should only occur in case such as JS activities etc where root 
			// activity will not be available
			moduleActivity = activity;
		}

		super.initActivity(moduleActivity);
		if (moduleActivity instanceof TiBaseActivity) {
			((TiBaseActivity)moduleActivity).addOnLifecycleEventListener(this);
		}
	}

	/**
	 * A place holder for subclasses to extend. Its purpose is to receive native Android onResume life cycle events.
	 * @param activity the activity attached to this module.
	 * @module.api
	 */
	public void onResume(Activity activity) {
	}

	/**
	 * A place holder for subclasses to extend. Its purpose is to receive native Android onPause life cycle events.
	 * @param activity the activity attached to this module.
	 * @module.api
	 */
	public void onPause(Activity activity) {
	}

	/**
	 * A place holder for subclasses to extend. Its purpose is to receive native Android onDestroy life cycle events.
	 * @param activity the activity attached to this module.
	 * @module.api
	 */
	public void onDestroy(Activity activity) {
	}

	/**
	 * A place holder for subclasses to extend. Its purpose is to receive native Android onStart life cycle events.
	 * @param activity the activity attached to this module.
	 * @module.api
	 */
	public void onStart(Activity activity) {
	}

	/**
	 * A place holder for subclasses to extend. Its purpose is to receive native Android onStop life cycle events.
	 * @param activity the activity attached to this module.
	 * @module.api
	 */
	public void onStop(Activity activity) {	
	}

	@Override
	public String getApiName()
	{
		return "Ti.Module";
	}

}
