/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package org.appcelerator.titanium.proxy;

import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiServiceBinder;
import org.appcelerator.titanium.TiServiceInterface;
import org.appcelerator.titanium.TiLifecycle.OnLifecycleEvent;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Service;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

@Kroll.proxy
/**
 * This is a proxy representation of the Android Service type.
 * Refer to <a href="http://developer.android.com/reference/android/app/Service.html" >Android Service</a> for more details.
 */
public class ServiceProxy extends KrollProxy {

    @Kroll.constant
    public static final String EVENT_STARTED = "started";
    protected Service service = null;
    protected TiServiceInterface tiService = null;
//    private boolean forBoundServices;

    // private Service service;
    private int serviceInstanceId;
    private IntentProxy intentProxy;
    private final String TAG = "ServiceProxy";

    public static final String NEEDS_STARTING = "needsStarting";

    private OnLifecycleEvent lifecycleListener = null;

    private boolean stopOnDestroy = false;

    protected String logTAG() {
        return TAG;
    }

    public ServiceProxy() {
        super();
        initLifeCycle(); // created from the app, we can get the root activity
        Intent intent = new Intent(TiApplication.getInstance()
                .getApplicationContext(), serviceClass());
        intent.putExtra(NEEDS_STARTING, false);
        setIntent(intent);
    }

    /**
     * For when creating a service proxy directly, for later binding using
     * bindService()
     */
    public ServiceProxy(IntentProxy intentProxy) {
        setIntent(intentProxy);
//        forBoundServices = true;
    }

    /**
     * For when a service started via startService() creates a proxy when it
     * starts running
     */
    public ServiceProxy(Service service, Intent intent,
            Integer serviceInstanceId) {
        super();
        if (service instanceof TiServiceInterface) {
            this.service = service;
            this.tiService = (TiServiceInterface) service;
            setIntent(intent);
            this.serviceInstanceId = serviceInstanceId;
        }

    }

    private void initLifeCycle() {
        if (lifecycleListener != null
                || TiApplication.getInstance().getRootActivity() == null)
            return;
        lifecycleListener = new OnLifecycleEvent() {
            @Override
            public void onStop(Activity activity) {
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

            @Override
            public void onDestroy(Activity activity) {
                realUnbind();
            }

            @Override
            public void onCreate(Activity activity, Bundle savedInstanceState) {
            }
        };
        TiApplication.getInstance().getRootActivity()
                .addOnLifecycleEventListener(lifecycleListener);
    }

    private void realUnbind() {
        if (service != null) {
            if (stopOnDestroy == true)
                tiService.stop();
            unbindService();
        }
        TiApplication.getInstance().getRootActivity()
                .removeOnLifecycleEventListener(lifecycleListener);
        lifecycleListener = null;
    }

    @Kroll.getProperty
    @Kroll.method
    public int getServiceInstanceId() {
        return serviceInstanceId;
    }

    @Kroll.getProperty
    @Kroll.method
    public IntentProxy getIntent() {
        return intentProxy;
    }

    public void setIntent(Intent intent) {
        setIntent(new IntentProxy(intent));
    }

    /**
     * Sets the IntentProxy.
     * 
     * @param intentProxy
     *            the proxy to set.
     */
    public void setIntent(IntentProxy intentProxy) {
        this.intentProxy = intentProxy;
    }

    @Kroll.method
    public void start() {
//        if (!forBoundServices) {
//            Log.w(TAG, "Only services created via Ti.Android.createService can be started via the start() command. Ignoring start() request.");
//            return;
//        }
        bindAndInvokeService();
    }

    @Kroll.method
    public void stop() {
        if (service != null) {
//            if (!forBoundServices) {
//                Log.d(TAG, "stop via stopService", Log.DEBUG_MODE);
//                service.stopSelf();
//            } else {
//                unbindService();
//            }
            this.tiService.stop();
        }
    }

    @Kroll.getProperty
    @Kroll.method
    public boolean getRunning() {
        return this.service != null && isServiceRunning();
    }

    @SuppressWarnings("rawtypes")
    protected Class serviceClass() {
        return Service.class;
    }

    protected boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) TiApplication.getInstance()
                .getApplicationContext()
                .getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager
                .getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass().getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public void bindService() {
        Context context = TiApplication.getInstance();
        boolean result = context.bindService(getIntent().getIntent(),
                this.serviceConnection, Context.BIND_AUTO_CREATE);
        if (!result) {
            Log.e(logTAG(), "Could not bind", Log.DEBUG_MODE);
        }
    }

    protected void bindAndInvokeService() {
        Context context = TiApplication.getInstance();
//        String className = serviceClass().toString();
        boolean alreadStarted = isServiceRunning();
        Intent intent = getIntent().getIntent();
        if (!alreadStarted) {
            context.startService(intent);
        }
        bindService();

    }

    protected void handleBinder(IBinder binder) {
        TiServiceBinder akbinder = (TiServiceBinder) binder;
        Service localservice = (Service) akbinder.getService();
        if (localservice instanceof TiServiceInterface) {
            ServiceProxy proxy = ServiceProxy.this;
            this.service = localservice;
            this.tiService = (TiServiceInterface) localservice;
            proxy.invokeBoundService();
            proxy.serviceInstanceId = tiService.nextServiceInstanceId();
        }

    }

    protected final ServiceConnection serviceConnection = new ServiceConnection() {

        public void onServiceDisconnected(ComponentName name) {
            unbindService();
        }

        public void onServiceConnected(ComponentName name, IBinder binder) {
            handleBinder(binder);
        }
    };

    public void unbindService() {
        Context context = TiApplication.getInstance();
        if (context == null) {
            Log.w(logTAG(),
                    "Cannot unbind service.  tiContext.getTiApp() returned null");
            return;
        }

        if (this.tiService != null) {
            this.tiService.unbindProxy(this);
        }

        try {
            context.unbindService(this.serviceConnection);
        } catch (Exception e) {

        }
        this.service = null;
        this.tiService = null;
    }

    protected void invokeBoundService() {
        this.tiService.start(this);
    }

    @Override
    public boolean fireEvent(String event, Object data) {
        return super.fireEvent(event, data);
    }

    @Override
    public void release() {
        realUnbind();
        super.release();
    }

    @Override
    public String getApiName()
    {
        return "Ti.Android.Service";
    }
}
