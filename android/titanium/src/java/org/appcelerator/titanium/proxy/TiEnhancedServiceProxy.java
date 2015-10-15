package org.appcelerator.titanium.proxy;

import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiLifecycle.OnLifecycleEvent;
import org.appcelerator.titanium.proxy.IntentProxy;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

@Kroll.proxy
public class TiEnhancedServiceProxy extends ReusableProxy {
    
    @Kroll.constant public static final String EVENT_STARTED = "started";
    protected Service service = null;
    
    public interface TiEnhancedServiceInterface {

        void start(TiEnhancedServiceProxy serviceProxy);

        void unbindProxy(TiEnhancedServiceProxy serviceProxy);
        public int nextServiceInstanceId();
        
        void stop();

    }
    
    public static class TiEnhancedServiceBinder extends Binder {
        private Service service;
        public TiEnhancedServiceBinder(Service service) {
            this.service = service;
        }
        public Service getService() {
            return this.service;
        }
    }

    protected TiEnhancedServiceInterface akService = null;
    
//  private Service service;
    private int serviceInstanceId;
    private IntentProxy intentProxy;
    private final String TAG = "TiEnhancedServiceProxy";
    
    public static final String NEEDS_STARTING = "needsStarting";
    
    private OnLifecycleEvent lifecycleListener = null;
    
    protected boolean stopOnDestroy = false;
    
    protected String logTAG() {
        return TAG;
    }

    public TiEnhancedServiceProxy() {
        this(null, null, null);
    }
    
    /**
     * For when a service started via startService() creates a proxy when it
     * starts running
     */
    public TiEnhancedServiceProxy(Service service, Intent intent, Integer serviceInstanceId) {
        super();
        if (service instanceof TiEnhancedServiceInterface) {
            Log.d(logTAG(), "TiEnhancedServiceProxy via start Service " + this + "/" + service, Log.DEBUG_MODE);
            this.service = service;
            this.akService = (TiEnhancedServiceInterface)service;
            setIntent(intent);
            this.serviceInstanceId = serviceInstanceId;
        }
        else {
            initLifeCycle();  //created from the app, we can get the root activity
        }
        if (intent != null) {
            setIntent(intent);
        }
        else {
            setIntent(new Intent(TiApplication.getInstance().getApplicationContext(), serviceClass()));
        }
    }

    private void initLifeCycle () {
        if (lifecycleListener != null || TiApplication.getInstance().getRootActivity() == null) return;
        lifecycleListener = new OnLifecycleEvent()
        {
            @Override
            public void onStop(Activity activity)
            {
            }

            @Override
            public void onStart(Activity activity)
            {
            }

            @Override
            public void onResume(Activity activity)
            {
            }

            @Override
            public void onPause(Activity activity)
            {
            }

            @Override
            public void onDestroy(Activity activity)
            {
                Log.d(logTAG(), "onDestroy", Log.DEBUG_MODE);
                realUnbind();
            }

            @Override
            public void onCreate(Activity activity, Bundle savedInstanceState) {
            }
        };
        Log.d(logTAG(), "initLifeCycle", Log.DEBUG_MODE);
        TiApplication.getInstance().getRootActivity().addOnLifecycleEventListener(lifecycleListener);
    }
    
    private void realUnbind() {
        Log.d(logTAG(), "realUnbind", Log.DEBUG_MODE);
        if (service != null) {
            if (stopOnDestroy == true)
                akService.stop();
            unbindService();
        } else {
            Log.d(logTAG(), "onDestroy: service is null", Log.DEBUG_MODE);
        }
        TiApplication.getInstance().getRootActivity().removeOnLifecycleEventListener(lifecycleListener);
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
    public void startService() {
        startService(getIntent().getIntent());
    }
    
    public void startService(final Intent intent) {
        Log.d(logTAG(), "Starting service " + this, Log.DEBUG_MODE);
        bindAndInvokeService(intent);
    }

    @Kroll.method
    public void stopService() {
        if (service != null) {
            Log.d(logTAG(), "Stopping service", Log.DEBUG_MODE);
            this.akService.stop();
        }
    }
    
    @Kroll.getProperty
    @Kroll.method
    public boolean getStarted() {
        Log.d(logTAG(), "getStarted " + this, Log.DEBUG_MODE);
        return this.service != null;
    }
    
    @SuppressWarnings("rawtypes")
    protected Class serviceClass() {
        return Service.class;
    }

    protected boolean isMyServiceRunning() {
        Log.d(logTAG(), "isMyServiceRunning " + serviceClass().toString(), Log.DEBUG_MODE);
        ActivityManager manager = (ActivityManager) TiApplication.getInstance().getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
//          Log.d(logTAG(), "isMyServiceRunning: " + serviceClass().getName() + " and " + service.service.getClassName(), Log.DEBUG_MODE);
            if (serviceClass().getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
    
    public void bindService(final Intent intent){
        Context context = TiApplication.getInstance();
        Log.d(logTAG(), "bindService " + this.serviceConnection, Log.DEBUG_MODE);
        boolean result = context.bindService(intent, this.serviceConnection, Context.BIND_AUTO_CREATE);
        if (!result)
        {
            Log.e(logTAG(), "Could not bind", Log.DEBUG_MODE);
        }
    }
    
    protected void bindAndInvokeService() {
        bindAndInvokeService(getIntent().getIntent());
    }
    
    protected void bindAndInvokeService(final Intent intent) {
        Context context = TiApplication.getInstance();
        String className = serviceClass().toString();
        Log.d(logTAG(), "bindAndInvokeService " + className, Log.DEBUG_MODE);
        boolean alreadStarted = isMyServiceRunning();
        intent.putExtra(TiEnhancedServiceProxy.NEEDS_STARTING, false);
        if (!alreadStarted) {
            Log.d(logTAG(), "bindAndInvokeService: service not running", Log.DEBUG_MODE);
        }
        context.startService(intent);
        if (this.service == null) {
            //not connected
            bindService(intent);
        }
        
    }
    
    protected void handleBinder (IBinder binder) {
        Log.d(logTAG(), "handleBinder", Log.DEBUG_MODE);
        TiEnhancedServiceBinder akbinder = (TiEnhancedServiceBinder) binder;
        Service localservice = (Service) akbinder.getService();
        if (localservice instanceof TiEnhancedServiceInterface) {
            TiEnhancedServiceProxy proxy = TiEnhancedServiceProxy.this;
            this.service = localservice;
            this.akService = (TiEnhancedServiceInterface)localservice;
            proxy.invokeBoundService();
            proxy.serviceInstanceId = akService.nextServiceInstanceId();
        }
        
    }
    
    protected final ServiceConnection serviceConnection = new ServiceConnection() {

        public void onServiceDisconnected(ComponentName name) {
            Log.d(logTAG(), "Service disconnected", Log.DEBUG_MODE);
            unbindService();
        }

        public void onServiceConnected(ComponentName name, IBinder binder) {
            Log.d(logTAG(), "Service connected", Log.DEBUG_MODE);
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

        if (this.akService != null) {
            this.akService.unbindProxy(this);
        }

        Log.d(logTAG(), "Unbinding service " + this.serviceConnection, Log.DEBUG_MODE);
        try {
            context.unbindService(this.serviceConnection);
        }
        catch (Exception e) {
            
        }
        this.service = null;
        this.akService = null;
    }

    protected void invokeBoundService() {
        Log.d(logTAG(), "invokeBoundService: " + this.akService.getClass().toString() + ":" + this + "/" + akService, Log.DEBUG_MODE);
        this.akService.start(this);
    }
    
    @Override
    public boolean fireEvent(String event, Object data)
    {
        Log.d(logTAG(), "fireEvent " + event, Log.DEBUG_MODE);
        return super.fireEvent(event, data);
    }

    @Override
    public void release() {
        Log.d(logTAG(), "release", Log.DEBUG_MODE);
        realUnbind();
        super.release();
    }
}
