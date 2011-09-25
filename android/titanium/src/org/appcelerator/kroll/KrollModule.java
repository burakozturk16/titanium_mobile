/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package org.appcelerator.kroll;

import java.util.ArrayList;
import java.util.List;

import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.TiBaseActivity;
import org.appcelerator.titanium.TiLifecycle.OnLifecycleEvent;

import android.app.Activity;

@Kroll.module
public class KrollModule extends KrollProxy
	implements KrollProxyListener, OnLifecycleEvent
{
	private static final String TAG = "KrollModule";
	protected static ArrayList<Class<? extends KrollModule>> initializedTemplates = new ArrayList<Class<? extends KrollModule>>();

	public KrollModule() {
		super();

		//Class<? extends KrollModule> moduleClass = getClass();
		/*if (!initializedTemplates.contains(moduleClass)) {
			V8Runtime.initModuleTemplate(moduleClass);
			initializedTemplates.add(moduleClass);
		}*/

		((TiBaseActivity)getActivity()).addOnLifecycleEventListener(this);
		modelListener = this;
	}

	// TODO @Override
	public void onResume(Activity activity) {
	}

	// TODO @Override
	public void onPause(Activity activity) {
	}
	
	// TODO @Override
	public void onDestroy(Activity activity) {
	}
	
	// TODO @Override
	public void onStart(Activity activity) {
	}
	
	// TODO @Override
	public void onStop(Activity activity) {	
	}
	
	// TODO @Override
	public void listenerAdded(String type, int count, KrollProxy proxy) {
	}
	
	// TODO @Override
	public void listenerRemoved(String type, int count, KrollProxy proxy) {
	}
	
	// TODO @Override
	public void processProperties(KrollDict d) {
	}
	
	// TODO @Override
	public void propertyChanged(String key, Object oldValue, Object newValue, KrollProxy proxy) {
	}
	
	// TODO @Override
	public void propertiesChanged(List<KrollPropertyChange> changes, KrollProxy proxy) {
		for (KrollPropertyChange change : changes) {
			propertyChanged(change.getName(), change.getOldValue(), change.getNewValue(), proxy);
		}
	}
}
