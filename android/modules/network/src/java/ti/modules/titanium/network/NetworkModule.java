/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.network;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.client.CookieStore;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollModule;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiContext;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiPlatformHelper;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

@Kroll.module
public class NetworkModule extends KrollModule {

	private static final String TAG = "TiNetwork";
	private static CookieStore httpCookieStore;

	public static final String EVENT_ACCESS_POINT_CREATED = "accesspointcreated";
	public static final String EVENT_ACCESS_POINT_FAILED = "accesspointfailed";
	public static final String EVENT_ACCESS_POINT_CLOSED = "accesspointclosed";
	public static final String EVENT_CONNECTIVITY = "change";
	public static final String EVENT_WIFI_SCAN = "wifiscan";
	public static final String NETWORK_USER_AGENT = System.getProperties().getProperty("http.agent") ;

	@Kroll.constant public static final int NETWORK_NONE = 0;
	@Kroll.constant public static final int NETWORK_WIFI = 1;
	@Kroll.constant public static final int NETWORK_MOBILE = 2;
	@Kroll.constant public static final int NETWORK_LAN = 3;
	@Kroll.constant public static final int NETWORK_UNKNOWN = 4;

    public enum State {
        UNKNOWN,

        /** This state is returned if there is connectivity to any network **/
        CONNECTED,
        /**
         * This state is returned if there is no connectivity to any network. This is set
         * to true under two circumstances:
         * <ul>
         * <li>When connectivity is lost to one network, and there is no other available
         * network to attempt to switch to.</li>
         * <li>When connectivity is lost to one network, and the attempt to switch to
         * another network fails.</li>
         */
        NOT_CONNECTED
    }

	class NetInfo {
		public State state;
		public boolean failover;
		public String typeName;
		public int type;
		public String reason;

		public NetInfo() {
			state = State.UNKNOWN;
			failover = false;
			typeName = "NONE";
			type = -1;
			reason = "";
		}
	};

	private NetInfo lastNetInfo;

	private boolean isListeningForConnectivity;
	private boolean isListeningForWifiScan;
	private TiNetworkListener networkListener;
	private ConnectivityManager connectivityManager;
	private TiWifiScanner wifiScannerListener;

	private Handler messageHandler = new Handler() {
		public void handleMessage(Message msg)
		{
			Bundle b = msg.getData();
			
			if (b.containsKey(TiNetworkListener.EXTRA_CONNECTED)) {
				boolean connected = b.getBoolean(TiNetworkListener.EXTRA_CONNECTED);
				int type = b.getInt(TiNetworkListener.EXTRA_NETWORK_TYPE);
				String typeName = b.getString(TiNetworkListener.EXTRA_NETWORK_TYPE_NAME);
				boolean failover = b.getBoolean(TiNetworkListener.EXTRA_FAILOVER);
				String reason = b.getString(TiNetworkListener.EXTRA_REASON);
	
				// Set last state
				synchronized(lastNetInfo) {
					if (connected) {
						lastNetInfo.state = State.CONNECTED;
					} else {
						lastNetInfo.state = State.NOT_CONNECTED;
					}
					lastNetInfo.type = type;
					lastNetInfo.typeName = typeName;
					lastNetInfo.failover = failover;
					lastNetInfo.reason = reason;
				}
	
				KrollDict data = new KrollDict();
				data.put("online", connected);
				int titaniumType = networkTypeToTitanium(connected, type);
				data.put("networkType", titaniumType);
				data.put("networkTypeName", networkTypeToTypeName(titaniumType));
				data.put("reason", reason);
				fireEvent(EVENT_CONNECTIVITY, data);
			}
			else if(b.containsKey(TiWifiScanner.EXTRA_SCAN_RESULTS)) {
				ArrayList<ScanResult> scanResults = b.getParcelableArrayList(TiWifiScanner.EXTRA_SCAN_RESULTS);
				KrollDict data = new KrollDict();
				KrollDict[] array = new KrollDict[scanResults.size()];
				for (int i =  0; i < scanResults.size(); i++) {
					ScanResult result = scanResults.get(i);
					KrollDict dataresult = new KrollDict();
					dataresult.put("ssid", result.SSID);
					dataresult.put("bssid", result.BSSID);
					dataresult.put("capabilities", result.capabilities);
					dataresult.put("frequency", result.frequency);
					dataresult.put("level", result.level);
					array[i] = dataresult;
				}
				data.put("results", array);
				fireEvent(EVENT_WIFI_SCAN, data);
			}
		}
	};

	public NetworkModule()
	{
		super();

		this.lastNetInfo = new NetInfo();
		this.isListeningForConnectivity = false;
		this.isListeningForWifiScan = false;
	}

	public NetworkModule(TiContext tiContext)
	{
		this();
	}

	@Override
	public void handleCreationArgs(KrollModule createdInModule, Object[] args)
	{
		super.handleCreationArgs(createdInModule, args);

		setProperty("userAgent", NETWORK_USER_AGENT + " Titanium/" + TiApplication.getInstance().getTiBuildVersion());
	}

	@Override
	protected void eventListenerAdded(String event, int count, KrollProxy proxy)
	{
		super.eventListenerAdded(event, count, proxy);
		if ("change".equals(event)) {
			if (!isListeningForConnectivity) {
				manageConnectivityListener(true);
			}
		}
		else if (EVENT_WIFI_SCAN.equals(event)) {
			if (!isListeningForWifiScan) {
				manageWifiScanListener(true);
			}
		}
	}

	@Override
	protected void eventListenerRemoved(String event, int count, KrollProxy proxy)
	{
		super.eventListenerRemoved(event, count, proxy);
		if ("change".equals(event) && count == 0) {
			manageConnectivityListener(false);
		}
	}

	@Kroll.getProperty @Kroll.method
	public boolean getOnline()
	{
		boolean result = false;

		ConnectivityManager cm = getConnectivityManager();
		if (cm != null) {
			NetworkInfo ni = getConnectivityManager().getActiveNetworkInfo();

			if(ni != null && ni.isAvailable() && ni.isConnected()) {
				result = true;
			}
		} else {
			Log.w(TAG, "ConnectivityManager was null", Log.DEBUG_MODE);
		}
		return result;
	}

	protected int networkTypeToTitanium(boolean online, int androidType) {
		int type = NetworkModule.NETWORK_UNKNOWN;
		if (online) {
			switch(androidType) {
			case ConnectivityManager.TYPE_WIFI :
				type = NetworkModule.NETWORK_WIFI;
				break;
			case ConnectivityManager.TYPE_MOBILE :
				type = NetworkModule.NETWORK_MOBILE;
				break;
			default : type = NetworkModule.NETWORK_UNKNOWN;
			}
		} else {
			type = NetworkModule.NETWORK_NONE;
		}
		return type;
	}

	@Kroll.getProperty @Kroll.method
	public int getNetworkType() {
		int type = NETWORK_UNKNOWN;

		// start event needs network type. So get it if we don't have it.
		if (connectivityManager == null) {
			connectivityManager = getConnectivityManager();
		}

		try {
			NetworkInfo ni = connectivityManager.getActiveNetworkInfo();
			if(ni != null && ni.isAvailable() && ni.isConnected()) {
				type = networkTypeToTitanium(true, ni.getType());
			} else {
				type = NetworkModule.NETWORK_NONE;
			}
		} catch (SecurityException e) {
			Log.w(TAG, "Permission has been removed. Cannot determine network type: " + e.getMessage());
		}
		return type;
	}

	@Kroll.getProperty @Kroll.method
	public String getNetworkTypeName()
	{
		return networkTypeToTypeName(getNetworkType());
	}

	@Kroll.getProperty @Kroll.method
	public String getCarrierName()
	{
		TelephonyManager manager = (TelephonyManager)TiApplication.getInstance().getRootActivity().getSystemService(Context.TELEPHONY_SERVICE);
		if (manager != null) {
			manager.getNetworkOperatorName();
		}
		return null;
	}
	
	@Kroll.getProperty @Kroll.method
	public Boolean getWifiEnabled()
	{
		Boolean result = false;
		TiApplication tiApp = TiApplication.getInstance();

		if(tiApp.getRootActivity().checkCallingOrSelfPermission(Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_GRANTED) {
			WifiManager wm = (WifiManager) tiApp.getRootActivity().getSystemService(Context.WIFI_SERVICE);
			if (wm != null) {
				result = wm.isWifiEnabled();
			}
		} else {
			Log.w(TAG, "Must have android.permission.ACCESS_WIFI_STATE to get mac address.");
		}
		return result;
	}
	
	@Kroll.method
	public void scanWifi()
	{
		if (wifiScannerListener != null) {
			wifiScannerListener.scanWifi();
		} else {
			Log.w(TAG, "No need to scanWifi as no one is listening.");
		}
	}
	
	@Kroll.method
	public void closeWifiAccessPoint() {
		WifiConfiguration currentConf = getCurrentWifiApConfiguration();
		if (currentConf != null) {
			TiApplication tiApp = TiApplication.getInstance();
			if(tiApp.getRootActivity().checkCallingOrSelfPermission(Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_GRANTED) {
				WifiManager wm = (WifiManager) tiApp.getRootActivity().getSystemService(Context.WIFI_SERVICE);
				if (wm != null) {
					Method[] wmMethods = wm.getClass().getDeclaredMethods();   //Get all declared methods in WifiManager class
					Method setWifiApEnabledMethod = null;
					for(Method method: wmMethods){
						if(method.getName().equals("setWifiApEnabled")) {
							setWifiApEnabledMethod = method;
							break;
						}
					}
					if (setWifiApEnabledMethod != null) {
						try {
			            	Log.d(TAG, "Closing a Wi-Fi Network", Log.DEBUG_MODE);
			                boolean apstatus=(Boolean) setWifiApEnabledMethod.invoke(wm, currentConf,false);          
			                if(apstatus)
			                {
			                	wm.setWifiEnabled(true);
				            	Log.d(TAG, "AccessPoint closed", Log.DEBUG_MODE);
				            	KrollDict data = new KrollDict();
								data.put("ssid", currentConf.SSID);
								data.put("pwd", currentConf.preSharedKey);
								fireEvent(EVENT_ACCESS_POINT_CLOSED, data);
			                }else {
				            	Log.d(TAG, "failed to close accessPoint", Log.DEBUG_MODE);

			                }
						} catch (IllegalArgumentException e1) {
							e1.printStackTrace();
						} catch (IllegalAccessException e1) {
							e1.printStackTrace();
						} catch (InvocationTargetException e1) {
							e1.printStackTrace();
						}
				    }
					else {
		            	Log.w(TAG, "Can't create accessPoint, Your phone's API does not contain setWifiApEnabled method");
				    }
				}
				else {
	            	Log.w(TAG, "Can't access WifiManager");
				}
			}
			else {
				Log.w(TAG, "Must have android.permission.ACCESS_WIFI_STATE to get mac address.");
			}  
		}
	}
	
	@Kroll.method
	public void createWifiAccessPoint(String ssid, String pwd) {
		TiApplication tiApp = TiApplication.getInstance();
		if(tiApp.getRootActivity().checkCallingOrSelfPermission(Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_GRANTED) {
			WifiManager wm = (WifiManager) tiApp.getRootActivity().getSystemService(Context.WIFI_SERVICE);
			if (wm != null) {
				if(wm.isWifiEnabled())
			    {
					wm.setWifiEnabled(false);          
			    }
				Method[] wmMethods = wm.getClass().getDeclaredMethods();   //Get all declared methods in WifiManager class
				Method setWifiApEnabledMethod = null;
				for(Method method: wmMethods){
					if(method.getName().equals("setWifiApEnabled")) {
						setWifiApEnabledMethod = method;
						break;
					}
				}
				if (setWifiApEnabledMethod != null) {
		            WifiConfiguration netConfig = new WifiConfiguration();
		            netConfig.SSID = ssid;
		            netConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
		            if (pwd.length() > 0)
		            {
		            	netConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
			            netConfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
			            netConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
			            netConfig.preSharedKey=pwd;
			            netConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
			            netConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
			            netConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
			            netConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
		            }
			            
		            try {
		            	Log.d(TAG, "Creating a Wi-Fi Network \""+netConfig.SSID+"\"", Log.DEBUG_MODE);
		                boolean apstatus=(Boolean) setWifiApEnabledMethod.invoke(wm, netConfig,true);          
		                if(apstatus)
		                {
			            	Log.d(TAG, "Wi-Fi Network \""+netConfig.SSID+"\" created", Log.DEBUG_MODE);
			            	KrollDict data = new KrollDict();
							data.put("ssid", netConfig.SSID);
							data.put("bssid", netConfig.BSSID);
							data.put("pwd", pwd);
							fireEvent(EVENT_ACCESS_POINT_CREATED, data);
		                }else {
			            	Log.d(TAG, "failed to create Wi-Fi Network \""+netConfig.SSID+"\"", Log.DEBUG_MODE);
			            	KrollDict data = new KrollDict();
							data.put("ssid", netConfig.SSID);
							data.put("pwd", pwd);
							fireEvent(EVENT_ACCESS_POINT_FAILED, data);
		                }

		            } catch (IllegalArgumentException e) {
		                e.printStackTrace();
		            } catch (IllegalAccessException e) {
		                e.printStackTrace();
		            } catch (InvocationTargetException e) {
		                e.printStackTrace();
		            }
			    }
				else {
	            	Log.w(TAG, "Can't create accessPoint, Your phone's API does not contain setWifiApEnabled method");
			    }
			}
			else {
            	Log.w(TAG, "Can't access WifiManager");
			}
		}
		else {
			Log.w(TAG, "Must have android.permission.ACCESS_WIFI_STATE to get mac address.");
		}    
	}
	
	private WifiConfiguration getCurrentWifiApConfiguration() {
		WifiConfiguration result = null;
		TiApplication tiApp = TiApplication.getInstance();
		if(tiApp.getRootActivity().checkCallingOrSelfPermission(Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_GRANTED) {
			WifiManager wm = (WifiManager) tiApp.getRootActivity().getSystemService(Context.WIFI_SERVICE);
			if (wm != null) {
				Method[] wmMethods = wm.getClass().getDeclaredMethods();   //Get all declared methods in WifiManager class
				Method getWifiApConfigurationMethod = null;
				Method isWifiApEnabledMethod = null;
				for(Method method: wmMethods){
					if(method.getName().equals("getWifiApConfiguration"))
						getWifiApConfigurationMethod = method;
					else if(method.getName().equals("isWifiApEnabled"))
						isWifiApEnabledMethod = method;
				}
				if (getWifiApConfigurationMethod != null && isWifiApEnabledMethod != null) {
					try {
						Boolean isEnabled = (Boolean)isWifiApEnabledMethod.invoke(wm);
						if (isEnabled) {
							result = (WifiConfiguration)getWifiApConfigurationMethod.invoke(wm);
						}
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return result;
	}
	
	@Kroll.method
	public KrollDict currentWifiAccessPoint()
	{
		KrollDict result = null;
		
		WifiConfiguration currentConf = getCurrentWifiApConfiguration();
		if (currentConf != null){
			result = new KrollDict();
			result.put("ssid", currentConf.SSID);
			result.put("bssid", currentConf.BSSID);
			result.put("pwd", currentConf.preSharedKey);
		}
		return result;
	}
	
	@Kroll.setProperty @Kroll.method
	public void setWifiEnabled(Boolean enabled)
	{
		TiApplication tiApp = TiApplication.getInstance();

		if(tiApp.getRootActivity().checkCallingOrSelfPermission(Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_GRANTED) {
			WifiManager wm = (WifiManager) tiApp.getRootActivity().getSystemService(Context.WIFI_SERVICE);
			if (wm != null) {
				wm.setWifiEnabled(enabled);
			}
			else {
				Log.w(TAG, "Cannot access WifiManager");
			}
		} else {
			Log.w(TAG, "Must have android.permission.ACCESS_WIFI_STATE to get mac address.");
		}
	}

	private String networkTypeToTypeName(int type)
	{
		switch(type)
		{
			case 0 : return "NONE";
			case 1 : return "WIFI";
			case 2 : return "MOBILE";
			case 3 : return "LAN";
			default : return "UNKNOWN";
		}
	}
	
	@Kroll.method @Kroll.topLevel
	public String encodeURIComponent(String component) {
		return Uri.encode(component);
	}
	
	@Kroll.method @Kroll.topLevel
	public String decodeURIComponent(String component) {
		return Uri.decode(component);
	}
	
	protected void manageConnectivityListener(boolean attach) {
		if (attach) {
			if (!isListeningForConnectivity) {
				if (hasListeners(EVENT_CONNECTIVITY)) {
					if (networkListener == null) {
						networkListener = new TiNetworkListener(messageHandler);
					}
					networkListener.attach(TiApplication.getInstance().getApplicationContext());
					isListeningForConnectivity = true;
					Log.d(TAG, "Adding connectivity listener", Log.DEBUG_MODE);
				}
			}
		} else {
			if (isListeningForConnectivity) {
				networkListener.detach();
				isListeningForConnectivity = false;
				Log.d(TAG, "Removing connectivity listener.", Log.DEBUG_MODE);
			}
		}
	}
	
	protected void manageWifiScanListener(boolean attach) {
		if (attach) {
			if (!isListeningForWifiScan) {
				if (hasListeners(EVENT_WIFI_SCAN)) {
					if (wifiScannerListener == null) {
						wifiScannerListener = new TiWifiScanner(messageHandler);
					}
					wifiScannerListener.attach(TiApplication.getInstance().getApplicationContext());
					isListeningForWifiScan = true;
					Log.d(TAG, "Adding wifiScanner listener", Log.DEBUG_MODE);
				}
			}
		} else {
			if (isListeningForWifiScan) {
				wifiScannerListener.detach();
				isListeningForWifiScan = false;
				Log.d(TAG, "Removing wifiScanner listener.", Log.DEBUG_MODE);
			}
		}
	}

	private ConnectivityManager getConnectivityManager()
	{
		ConnectivityManager cm = null;

		Context a = TiApplication.getInstance();
		if (a != null) {
			cm = (ConnectivityManager) a.getSystemService(Context.CONNECTIVITY_SERVICE);
		} else {
			Log.w(TAG, "Activity is null when trying to retrieve the connectivity service", Log.DEBUG_MODE);
		}

		return cm;
	}
	
	private WifiManager getWifiManager() {
        TiApplication tiApp = TiApplication.getInstance();
	    if(tiApp.getRootActivity().checkCallingOrSelfPermission(Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_GRANTED) {
            return (WifiManager) tiApp.getRootActivity().getSystemService(Context.WIFI_SERVICE);
	    }
	    return null;
	}

	@Override
	public void onDestroy(Activity activity) {
		super.onDestroy(activity);
		manageConnectivityListener(false);
		connectivityManager = null;
	}

	public static CookieStore getHTTPCookieStoreInstance()
	{
		if (httpCookieStore == null) {
			httpCookieStore = new BasicCookieStore();
		}
		return httpCookieStore;
	}

	/**
	 * Adds a cookie to the HTTPClient cookie store. Any existing cookie with the same domain and name will be replaced with
	 * the new cookie. This seems like a bug in org.apache.http.impl.client.BasicCookieStore because based on RFC6265
	 * (http://tools.ietf.org/html/rfc6265#section-4.1.2), an existing cookie with the same cookie-name, domain-value and
	 * path-value with the new cookie will be evicted and replaced.
	 * @param cookieProxy the cookie to add
	 */
	@Kroll.method
	public void addHTTPCookie(CookieProxy cookieProxy)
	{
		BasicClientCookie cookie = cookieProxy.getHTTPCookie();
		if (cookie != null) {
			getHTTPCookieStoreInstance().addCookie(cookie);
		}
	}

	/**
	 * Gets all the cookies with the domain, path and name matched with the given values. If name is null, gets all the cookies with
	 * the domain and path matched.
	 * @param domain the domain of the cookie to get. It is case-insensitive.
	 * @param path the path of the cookie to get. It is case-sensitive.
	 * @param name the name of the cookie to get. It is case-sensitive.
	 * @return an array of cookies. If name is null, returns all the cookies with the domain and path matched.
	 */
	@Kroll.method
	public CookieProxy[] getHTTPCookies(String domain, String path, String name)
	{
		if (domain == null || domain.length() == 0) {
			if (Log.isDebugModeEnabled()) {
				Log.e(TAG, "Unable to get the HTTP cookies. Need to provide a valid domain.");
			}
			return null;
		}
		if (path == null || path.length() == 0) {
			path = "/";
		}
		ArrayList<CookieProxy> cookieList = new ArrayList<CookieProxy>();
		List<Cookie> cookies = getHTTPCookieStoreInstance().getCookies();
		for (Cookie cookie : cookies) {
			String cookieName = cookie.getName();
			String cookieDomain = cookie.getDomain();
			String cookiePath = cookie.getPath();
			if ((name == null || cookieName.equals(name)) && domainMatch(cookieDomain, domain)
				&& pathMatch(cookiePath, path)) {
				cookieList.add(new CookieProxy(cookie));
			}
		}
		if (!cookieList.isEmpty()) {
			return cookieList.toArray(new CookieProxy[cookieList.size()]);
		}
		return null;
	}

	/**
	 * Gets all the cookies with the domain matched with the given value.
	 * @param domain the domain of the cookie to get. It is case-insensitive.
	 * @return an array of cookies with the domain matched.
	 */
	@Kroll.method
	public CookieProxy[] getHTTPCookiesForDomain(String domain)
	{
		if (domain == null || domain.length() == 0) {
			if (Log.isDebugModeEnabled()) {
				Log.e(TAG, "Unable to get the HTTP cookies. Need to provide a valid domain.");
			}
			return null;
		}
		ArrayList<CookieProxy> cookieList = new ArrayList<CookieProxy>();
		List<Cookie> cookies = getHTTPCookieStoreInstance().getCookies();
		for (Cookie cookie : cookies) {
			String cookieDomain = cookie.getDomain();
			if (domainMatch(cookieDomain, domain)) {
				cookieList.add(new CookieProxy(cookie));
			}
		}
		if (!cookieList.isEmpty()) {
			return cookieList.toArray(new CookieProxy[cookieList.size()]);
		}
		return null;
	}

	/** Removes the cookie with the domain, path and name exactly the same as the given values.
	 * @param domain the domain of the cookie to remove. It is case-insensitive.
	 * @param path the path of the cookie to remove. It is case-sensitive.
	 * @param name the name of the cookie to remove. It is case-sensitive.
	 */
	@Kroll.method
	public void removeHTTPCookie(String domain, String path, String name)
	{
		if (domain == null || name == null) {
			if (Log.isDebugModeEnabled()) {
				Log.e(TAG, "Unable to remove the HTTP cookie. Need to provide a valid domain / name.");
			}
			return;
		}
		CookieStore cookieStore = getHTTPCookieStoreInstance();
		List<Cookie> cookies = new ArrayList<Cookie>(cookieStore.getCookies());
		cookieStore.clear();
		for (Cookie cookie : cookies) {
			String cookieName = cookie.getName();
			String cookieDomain = cookie.getDomain();
			String cookiePath = cookie.getPath();
			if (!(name.equals(cookieName) && stringEqual(domain, cookieDomain, false) && stringEqual(path, cookiePath, true))) {
				cookieStore.addCookie(cookie);
			}
		}
	}

	/**
	 * Removes all the cookies with the domain matched with the given value.
	 * @param domain the domain of the cookie to remove. It is case-insensitive.
	 */
	@Kroll.method
	public void removeHTTPCookiesForDomain(String domain)
	{
		CookieStore cookieStore = getHTTPCookieStoreInstance();
		List<Cookie> cookies = new ArrayList<Cookie>(cookieStore.getCookies());
		cookieStore.clear();
		for (Cookie cookie : cookies) {
			String cookieDomain = cookie.getDomain();
			if (!(domainMatch(cookieDomain, domain))) {
				cookieStore.addCookie(cookie);
			}
		}
	}

	/**
	 * Removes all the cookies in the HTTPClient cookie store.
	 */
	@Kroll.method
	public void removeAllHTTPCookies()
	{
		CookieStore cookieStore = getHTTPCookieStoreInstance();
		cookieStore.clear();
	}

	/**
	 * Adds a cookie to the system cookie store. Any existing cookie with the same domain, path and name will be replaced with
	 * the new cookie. The cookie being set must not have expired, otherwise it will be ignored.
	 * @param cookieProxy the cookie to add
	 */
	@Kroll.method
	public void addSystemCookie(CookieProxy cookieProxy)
	{
		BasicClientCookie cookie = cookieProxy.getHTTPCookie();
		String cookieString = cookie.getName() + "=" + cookie.getValue();
		String domain = cookie.getDomain();
		if (domain == null) {
			Log.w(TAG, "Unable to add system cookie. Need to provide domain.");
			return;
		}
		cookieString += "; domain=" + domain;

		String path = cookie.getPath();
		Date expiryDate = cookie.getExpiryDate();
		boolean secure = cookie.isSecure();
		boolean httponly = TiConvert.toBoolean(cookieProxy.getProperty(TiC.PROPERTY_HTTP_ONLY), false);
		if (path != null) {
			cookieString += "; path=" + path;
		}
		if (expiryDate != null) {
			cookieString += "; expires=" + CookieProxy.systemExpiryDateFormatter.format(expiryDate);
		}
		if (secure) {
			cookieString += "; secure";
		}
		if (httponly) {
			cookieString += " httponly";
		}
		CookieSyncManager.createInstance(TiApplication.getInstance().getRootOrCurrentActivity());
		CookieManager cookieManager = CookieManager.getInstance();
		cookieManager.setCookie(domain, cookieString);
		CookieSyncManager.getInstance().sync();
	}

	/**
	 * Gets all the cookies with the domain, path and name matched with the given values. If name is null, gets all the cookies with
	 * the domain and path matched.
	 * @param domain the domain of the cookie to get. It is case-insensitive.
	 * @param path the path of the cookie to get. It is case-sensitive.
	 * @param name the name of the cookie to get. It is case-sensitive.
	 * @return an array of cookies only with name and value specified. If name is null, returns all the cookies with the domain and path matched.
	 */
	@Kroll.method
	public CookieProxy[] getSystemCookies(String domain, String path, String name)
	{
		if (domain == null || domain.length() == 0) {
			if (Log.isDebugModeEnabled()) {
				Log.e(TAG, "Unable to get the HTTP cookies. Need to provide a valid domain.");
			}
			return null;
		}
		if (path == null || path.length() == 0) {
			path = "/";
		}

		ArrayList<CookieProxy> cookieList = new ArrayList<CookieProxy>();
		CookieSyncManager.createInstance(TiApplication.getInstance().getRootOrCurrentActivity());
		CookieManager cookieManager = CookieManager.getInstance();
		String url = domain.toLowerCase() + path;
		String cookieString = cookieManager.getCookie(url); // The cookieString is in the format of NAME=VALUE[;
															// NAME=VALUE]
		if (cookieString != null) {
			String[] cookieValues = cookieString.split("; ");
			for (int i = 0; i < cookieValues.length; i++) {
				String[] pair = cookieValues[i].split("=", 2);
				String cookieName = pair[0];
				String value = pair.length == 2 ? pair[1] : null;
				if (name == null || cookieName.equals(name)) {
					cookieList.add(new CookieProxy(cookieName, value, null, null));
				}
			}
		}
		if (!cookieList.isEmpty()) {
			return cookieList.toArray(new CookieProxy[cookieList.size()]);
		}
		return null;
	}

	/**
	 * Removes the cookie with the domain, path and name exactly the same as the given values.
	 * @param domain the domain of the cookie to remove. It is case-insensitive.
	 * @param path the path of the cookie to remove. It is case-sensitive.
	 * @param name the name of the cookie to remove. It is case-sensitive.
	 */
	@Kroll.method
	public void removeSystemCookie(String domain, String path, String name)
	{
		if (domain == null || name == null) {
			if (Log.isDebugModeEnabled()) {
				Log.e(TAG, "Unable to remove the system cookie. Need to provide a valid domain / name.");
			}
			return;
		}
		String lower_domain = domain.toLowerCase();
		String cookieString = name + "=; domain=" + lower_domain + "; path=" + path + "; expires=" + CookieProxy.systemExpiryDateFormatter.format(new Date(0));
		CookieSyncManager.createInstance(TiApplication.getInstance().getRootOrCurrentActivity());
		CookieManager cookieManager = CookieManager.getInstance();
		cookieManager.setCookie(lower_domain, cookieString);
		CookieSyncManager.getInstance().sync();
	}

	/**
	 * Removes all the cookies in the system cookie store.
	 */
	@Kroll.method
	public void removeAllSystemCookies()
	{
		CookieSyncManager.createInstance(TiApplication.getInstance().getRootOrCurrentActivity());
		CookieManager cookieManager = CookieManager.getInstance();
		cookieManager.removeAllCookie();
		CookieSyncManager.getInstance().sync();
	}

	/**
	 * Helper method to decide whether the domain matches the cookie's domain. If the both domains are null, return true.
	 * The domain matching follows RFC6265 (http://tools.ietf.org/html/rfc6265#section-5.1.3).
	 * @param cookieDomain cookie's domain
	 * @param domain domain to match
	 * @return true if the domain matches cookieDomain; false otherwise. If the both domains are null, return true.
	 */
	private boolean domainMatch(String cookieDomain, String domain)
	{
		if (cookieDomain == null && domain == null) {
			return true;
		}
		if (cookieDomain == null || domain == null) {
			return false;
		}

		String lower_cookieDomain = cookieDomain.toLowerCase();
		String lower_domain = domain.toLowerCase();
		if (lower_cookieDomain.startsWith(".")) {
			if (lower_domain.endsWith(lower_cookieDomain.substring(1))) {
				int cookieLen = lower_cookieDomain.length();
				int domainLen = lower_domain.length();
				if (domainLen > cookieLen -1) {
					// make sure bar.com doesn't match .ar.com
					return lower_domain.charAt(domainLen - cookieLen) == '.';
				}
				return true;
			}
			return false;
		} else {
			return lower_domain.equals(lower_cookieDomain);
		}
	}

	/**
	 * Helper method to decide whether the path matches the cookie's path. If the cookie's path is null or an empty string, return true.
	 * If the path is null or an empty string, use "/" as the default value. The path matching follows RFC6265 (http://tools.ietf.org/html/rfc6265#section-5.1.4).
	 * @param cookiePath cookie's path
	 * @param path path to match
	 * @return true if the path matches cookiePath; false otherwise. If cookiePath is null or an empty string, return true.
	 */
	private boolean pathMatch(String cookiePath, String path)
	{
		if (cookiePath == null || cookiePath.length() == 0) {
			return true;
		}
		if (path == null || path.length() == 0) {
			path = "/";
		}

		if (path.startsWith(cookiePath)) {
			int cookieLen = cookiePath.length();
			int pathLen = path.length();
			if (cookiePath.charAt(cookieLen - 1) != '/' && pathLen > cookieLen) {
				// make sure /wee doesn't match /we
				return path.charAt(cookieLen) == '/';
			}
			return true;
		}
		return false;
	}

	/**
	 * Helper method to determine whether two strings are equal.
	 * @param s1 string to compare
	 * @param s2 string to compare
	 * @param isCaseSensitive true if using case-sensitive comparison; false if using case-insensitive comparison.
	 * @return true if the two strings are both null or they are equal.
	 */
	private boolean stringEqual(String s1, String s2, boolean isCaseSensitive)
	{
		if (s1 == null && s2 == null) {
			return true;
		}
		if (s1 != null && s2 != null) {
			if ((isCaseSensitive && s1.equals(s2)) || (!isCaseSensitive && s1.toLowerCase().equals(s2.toLowerCase()))) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String getApiName()
	{
		return "Ti.Network";
	}
	

    @Kroll.getProperty @Kroll.method
    public String getAddress() {
        return TiPlatformHelper.getInstance().getIpAddress();
    }

    @Kroll.getProperty @Kroll.method
    public String getNetmask() {
        return TiPlatformHelper.getInstance().getNetmask();
    }
    
    private final String formatIp(final int ipAddress) {
        return String.format("%d.%d.%d.%d",
                (ipAddress & 0xff),
                (ipAddress >> 8 & 0xff),
                (ipAddress >> 16 & 0xff),
                (ipAddress >> 24 & 0xff));
    }

	
	@Kroll.getProperty @Kroll.method
    public KrollDict getNetworkInfo()
    {
	    KrollDict result = new KrollDict();
        KrollDict wifi = new KrollDict();
        KrollDict cell = new KrollDict();

	    WifiManager wifiManager = getWifiManager();
	    if (wifiManager != null) {
            final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
            DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
            if (connectionInfo != null){
                result = new KrollDict();
                wifi.put("ssid", connectionInfo.getSSID().replace("\"", ""));
                wifi.put("bssid", connectionInfo.getBSSID());
                wifi.put("ip", formatIp(connectionInfo.getIpAddress()));
                wifi.put("linkSpeed", connectionInfo.getLinkSpeed() + WifiInfo.LINK_SPEED_UNITS);
                wifi.put("macAddress", connectionInfo.getMacAddress());
            }
            if (dhcpInfo != null) {
                wifi.put("netmask", formatIp(dhcpInfo.netmask));
            }
	    }
        cell.put("carrierName", getCarrierName());
        result.put("wifi", wifi);
        result.put("wwan", cell);
        return result;
    }
	
	@Kroll.getProperty @Kroll.method
    public KrollDict getNetworkStats()
    {
        long currentTime = java.lang.System.currentTimeMillis();
        KrollDict stats = new KrollDict();
        KrollDict wifi = new KrollDict();
        KrollDict cell = new KrollDict();
        
        long cellReceived = TrafficStats.getMobileRxBytes();
        long cellSent = TrafficStats.getMobileTxBytes();
        
        cell.put("received_bytes", cellReceived);
        cell.put("sent_bytes", cellSent);
        wifi.put("received_bytes",  TrafficStats.getTotalRxBytes() - cellReceived);
        wifi.put("sent_bytes",  TrafficStats.getTotalTxBytes() - cellSent);
        stats.put("wifi", wifi);
        stats.put("wwan", cell);
        stats.put("boottime", currentTime - android.os.SystemClock.elapsedRealtime());
        stats.put("timestamp", currentTime);
        return stats;
    }
    
}
