/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.network;
 
import java.util.ArrayList;
import java.util.List;

import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiApplication;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
 
/**
 * A wrapper for a broadcast receiver that provides network connectivity
 * state information, independent of network type (mobile, Wi-Fi, etc.).
 * {@hide}
 */
public class TiWifiScanner {
    private static final String TAG = "TiWifiScanner";
 
    private WifiManager wm;
    private IntentFilter wifiScannerIntentFilter;
    private WifiScannerBroadcastReceiver receiver;
    public static final String EXTRA_SCAN_RESULTS = "scanResults";
 
    private Handler messageHandler;
    private Context context; // null on release, might need to be softRef.
    private boolean listening;
 
    private class WifiScannerBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
 
            if (!action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                 return;
            }
 
            if (messageHandler == null) {
            	Log.w(TAG, "Network receiver is active but no handler has been set.");
            	return;
            }
            
            if (wm == null) {
            	Log.w(TAG, "Can't access WifiManager. Yet we got WifiScannerBroadcastReceiver ...");
            	return;
            }
            
            List<ScanResult> results = wm.getScanResults();
 
        	Message message = Message.obtain(messageHandler);
 
        	Bundle b = message.getData();
        	b.putParcelableArrayList(EXTRA_SCAN_RESULTS, (ArrayList<? extends Parcelable>) results);
        	message.sendToTarget();
        }
    };
 
    /**
     * Create a new TitaniumNetworkListener.
     */
    public TiWifiScanner(Handler messageHandler) {
		TiApplication tiApp = TiApplication.getInstance();
    	if(tiApp.getRootActivity().checkCallingOrSelfPermission(Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_GRANTED) {
    		wm = (WifiManager) tiApp.getRootActivity().getSystemService(Context.WIFI_SERVICE);
    	}
    	else {
			Log.w(TAG, "Must have android.permission.ACCESS_WIFI_STATE to get mac address.");
    	}
        this.receiver = new WifiScannerBroadcastReceiver();
        this.messageHandler = messageHandler;
        this.wifiScannerIntentFilter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
    }
 
    public void attach(Context context) {
    	if (!listening) {
    		if (this.context == null) {
    			this.context = context;
    		} else {
    			throw new IllegalStateException("Context was not cleaned up from last release.");
    		}
    		context.registerReceiver(receiver, wifiScannerIntentFilter);
    		listening = true;
    	} else {
    		Log.w(TAG, "ScanWifi listener is already attached");
    	}
    }
 
    public void detach() {
    	if (listening) {
			context.unregisterReceiver(receiver);
			context = null;
			listening = false;
    	}
    }
    
    public void scanWifi()
	{
		if (wm != null) {
			wm.startScan();
		} else {
        	Log.w(TAG, "Can't access WifiManager. Yet we got WifiScannerBroadcastReceiver ...");
		}
	}
}
 