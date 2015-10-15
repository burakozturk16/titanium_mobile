package org.appcelerator.titanium;

import java.util.concurrent.atomic.AtomicInteger;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollRuntime;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.TiEnhancedServiceProxy;
import org.appcelerator.titanium.proxy.TiEnhancedServiceProxy.TiEnhancedServiceInterface;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
@SuppressLint("NewApi")
public class TiEnhancedService extends Service implements TiEnhancedServiceInterface {

    static final String TAG = "TiEnhancedService";
    public static final String TI_SERVICE_INTENT_ID_KEY = "$__TITANIUM_SERVICE_INTENT_ID__$";
    protected AtomicInteger proxyCounter = new AtomicInteger();

    protected TiEnhancedServiceProxy proxy = null;

    protected boolean mStarted = false;
    
    protected String logTAG() {
        return TAG;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        KrollRuntime.incrementServiceReceiverRefCount();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        KrollRuntime.decrementServiceReceiverRefCount();
    }
    
    protected TiEnhancedServiceProxy getCurrentProxy() {
        return null;
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        boolean needsStart = intent == null || intent.getBooleanExtra(TiEnhancedServiceProxy.NEEDS_STARTING, true);
        Log.d(logTAG(), "onStartCommand: " + needsStart, Log.DEBUG_MODE);
        if (this.proxy == null) {
            this.proxy = getCurrentProxy();
            Log.d(logTAG(), "onStartCommand: first start no proxy", Log.DEBUG_MODE);
        } else {
            Log.d(logTAG(), "onStartCommand: already started", Log.DEBUG_MODE);
        }
        if (needsStart){
            start(this.proxy);
        }
        
        int result = START_STICKY;
        if (intent != null) result = intent.getIntExtra(TiC.INTENT_PROPERTY_START_MODE,
                result);
        return result;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(logTAG(), "onBind", Log.DEBUG_MODE);
        return new TiEnhancedServiceProxy.TiEnhancedServiceBinder(this);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        //this is called when the disconnect button is clicked in the status bar.
        Log.d(TAG, "onUnbind", Log.DEBUG_MODE);
        return super.onUnbind(intent);
    }
    
    protected void start() {
        Log.d(logTAG(), "start service");
    }
    
    protected void bindToProxy(final TiEnhancedServiceProxy proxy) {
        if (this.proxy != null) {
            Log.d(logTAG(),
                    "we were already associated to a proxy, must have started on boot",
                    Log.DEBUG_MODE);
            Log.d(logTAG(),
                    "old proxy instance "
                            + this.proxy.getProperties(), Log.DEBUG_MODE);
            
        }
        this.proxy = proxy;
        if (this.proxy != null) {
            Log.d(logTAG(),
                    "new proxy instance" + this.proxy.getProperties(),
                    Log.DEBUG_MODE);
        }
    }
    
    public void start(TiEnhancedServiceProxy proxy) {
        Log.d(logTAG(), "start", Log.DEBUG_MODE);

        if (this.proxy != proxy) {
            bindToProxy(proxy);
        }        
        
        if (mStarted == false) {
            mStarted = true;
            start();
        }
        else {
            Log.d(logTAG(), "start: service already started", Log.DEBUG_MODE);
        }

        if (this.proxy != null)
            this.proxy.fireEvent(TiEnhancedServiceProxy.EVENT_STARTED, new KrollDict());
    }
    
    public void stop(){
        Log.d(TAG, "stop", Log.DEBUG_MODE);
        if (this.proxy != null) {
            this.proxy.unbindService();
        }
        stopSelf();
    }

    
    /**
     * Implementing subclasses should use this method to release the proxy.
     * 
     * @param proxy
     *            the proxy to release.
     */
    public void unbindProxy(TiEnhancedServiceProxy proxy) {
        this.proxy = null;
    }

    /**
     * @return next service instance id.
     */
    public int nextServiceInstanceId() {
        return proxyCounter.incrementAndGet();
    }
}
