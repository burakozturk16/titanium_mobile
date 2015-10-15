/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2014 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package org.appcelerator.titanium;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.appcelerator.kroll.KrollApplication;
import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollModule;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.KrollRuntime;
import org.appcelerator.kroll.common.CurrentActivityListener;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.kroll.common.TiConfig;
import org.appcelerator.kroll.common.TiDeployData;
import org.appcelerator.kroll.common.TiMessenger;
import org.appcelerator.kroll.util.KrollAssetHelper;
import org.appcelerator.kroll.util.TiTempFileHelper;
import org.appcelerator.titanium.analytics.TiAnalyticsEventFactory;
import org.appcelerator.titanium.util.TiActivityHelper;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiFileHelper;
import org.appcelerator.titanium.util.TiHTTPHelper;
import org.appcelerator.titanium.util.TiPlatformHelper;
import org.appcelerator.titanium.util.TiUIHelper;
import org.appcelerator.titanium.util.TiWeakList;
import org.json.JSONException;
import org.json.JSONObject;

import ti.modules.titanium.TitaniumModule;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.accessibility.AccessibilityManager;

import com.appcelerator.analytics.APSAnalytics;
import com.appcelerator.analytics.APSAnalytics.DeployType;
import com.squareup.okhttp.Cache;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.picasso.LruCache;
import com.squareup.picasso.OkHttpDownloader;
import com.squareup.picasso.Picasso;

/**
 * The main application entry point for all Titanium applications and services.
 */
public abstract class TiApplication extends Application implements
        KrollApplication {
    private static final String SYSTEM_UNIT = "system";
    private static final String TAG = "TiApplication";
    private static final String PROPERTY_THREAD_STACK_SIZE = "ti.android.threadstacksize";
    private static final String PROPERTY_COMPILE_JS = "ti.android.compilejs";
    private static final String PROPERTY_ENABLE_COVERAGE = "ti.android.enablecoverage";
    private static final String PROPERTY_DEFAULT_UNIT = "ti.ui.defaultunit";
    private static final String PROPERTY_USE_LEGACY_WINDOW = "ti.android.useLegacyWindow";
    private  static String TITANIUM_USER_AGENT;
    
    private static long sMainThreadId = 0;

    private static int sAppDensityDpi = -1;
    private static float sAppDensity = -1;
    private static float sAppScaledDensity = -1;
    private static String sAppDensityString = null;

    protected static WeakReference<TiApplication> sTiApp = null;

    public static final String DEPLOY_TYPE_DEVELOPMENT = "development";
    public static final String DEPLOY_TYPE_TEST = "test";
    public static final String DEPLOY_TYPE_PRODUCTION = "production";
    public static final int DEFAULT_THREAD_STACK_SIZE = 16 * 1024; // 16K as a
                                                                   // "sane"
                                                                   // default
    public static final String APPLICATION_PREFERENCES_NAME = "titanium";
    public static final String PROPERTY_FASTDEV = "ti.android.fastdev";
    public static final int TRIM_MEMORY_RUNNING_LOW = 10; // Application.TRIM_MEMORY_RUNNING_LOW
                                                          // for API 16+

    // Whether or not using legacy window. This is set in the application's
    // tiapp.xml with the
    // "ti.android.useLegacyWindow" property.
    public static boolean USE_LEGACY_WINDOW = false;

    private boolean restartPending = false;
    private String baseUrl;
    private String startUrl;
    private HashMap<String, SoftReference<KrollProxy>> proxyMap;
    private TiWeakList<KrollProxy> appEventProxies = new TiWeakList<KrollProxy>();
    private WeakReference<TiRootActivity> rootActivity;
    private TiProperties appProperties;
    private WeakReference<Activity> currentActivity;
    private String buildVersion = "", buildTimestamp = "", buildHash = "";
    private String defaultUnit;
    private BroadcastReceiver externalStorageReceiver;
    private AccessibilityManager accessibilityManager = null;
    private boolean forceFinishRootActivity = false;

    protected TiDeployData deployData;
    protected TiTempFileHelper tempFileHelper;
    protected ITiAppInfo appInfo;
    protected TiStylesheet stylesheet;
    protected static HashMap<String, WeakReference<KrollModule>> modules;
	protected String[] filteredAnalyticsEvents;
    
    protected static ArrayList<AppStateListener> sAppStateListeners = new ArrayList<AppStateListener>();

    public static interface AppStateListener {
        public void onAppPaused();
        public void onAppResume();
    }

    public static void addAppStateListener(
            AppStateListener a) {
        sAppStateListeners.add(a);
    }

    public static void removeAppStateListener(
            AppStateListener a) {
        sAppStateListeners.remove(a);
    }

    public static AtomicBoolean isActivityTransition = new AtomicBoolean(false);
    protected static ArrayList<ActivityTransitionListener> activityTransitionListeners = new ArrayList<ActivityTransitionListener>();
    protected static TiWeakList<Activity> activityStack = new TiWeakList<Activity>();

    public static interface ActivityTransitionListener {
        public void onActivityTransition(boolean state);
    }

    public static void addActivityTransitionListener(
            ActivityTransitionListener a) {
        activityTransitionListeners.add(a);
    }

    public static void removeActivityTransitionListener(
            ActivityTransitionListener a) {
        activityTransitionListeners.remove(a);
    }

    public static void updateActivityTransitionState(boolean state) {
        isActivityTransition.set(state);
        for (int i = 0; i < activityTransitionListeners.size(); ++i) {
            activityTransitionListeners.get(i).onActivityTransition(state);
        }

    }

    public CountDownLatch rootActivityLatch = new CountDownLatch(1);

    public TiApplication() {
        Log.checkpoint(TAG, "checkpoint, app created.");

        loadBuildProperties();

        sMainThreadId = Looper.getMainLooper().getThread().getId();
        sTiApp = new WeakReference<TiApplication>(this);

        modules = new HashMap<String, WeakReference<KrollModule>>();
        TiMessenger.getMessenger(); // initialize message queue for main thread

        Log.i(TAG, "Titanium " + buildVersion + " (" + buildTimestamp + " "
                + buildHash + ")");
    }

    /**
     * Retrieves the instance of TiApplication. There is one instance per
     * Android application.
     * 
     * @return the instance of TiApplication.
     * @module.api
     */
    public static TiApplication getInstance() {
        if (sTiApp != null) {
            TiApplication tiAppRef = sTiApp.get();
            if (tiAppRef != null) {
                return tiAppRef;
            }
        }

        Log.e(TAG, "Unable to get the TiApplication instance");
        return null;
    }

    public static void addToActivityStack(Activity activity) {
        activityStack.add(new WeakReference<Activity>(activity));
    }

    public static void removeFromActivityStack(Activity activity) {
        activityStack.remove(activity);
    }
    
    public static Object getAppSystemService(final String name) {
        return getInstance().getSystemService(name);
    }

    // Calls finish on the list of activities in the stack. This should only be
    // called when we want to terminate the
    // application (typically when the root activity is destroyed)
    public static void terminateActivityStack() {
        

        if (activityStack == null || activityStack.size() == 0) {
            return;
        }

        WeakReference<Activity> activityRef;
        Activity currentActivity;

        for (int i = activityStack.size() - 1; i >= 0; i--) {
            // We need to check the stack size here again. Since we call
            // finish(), that could potentially
            // change the activity stack while we are looping through them.
            // TIMOB-12487
            if (i < activityStack.size()) {
                activityRef = activityStack.get(i);
                if (activityRef != null) {
                    currentActivity = activityRef.get();
                    if (currentActivity != null
                            && !currentActivity.isFinishing()) {
                        currentActivity.finish();
                    }
                }
            }
        }
        activityStack.clear();
    }

    public boolean activityStackHasLaunchActivity() {
        if (activityStack == null || activityStack.size() == 0) {
            return false;
        }
        for (WeakReference<Activity> activityRef : activityStack) {
            if (activityRef != null
                    && activityRef.get() instanceof TiLaunchActivity) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check whether the current activity is in foreground or not.
     * 
     * @return true if the current activity is in foreground; false otherwise.
     * @module.api
     */
    public static boolean isCurrentActivityInForeground() {
        Activity currentActivity = getAppCurrentActivity();
        if (currentActivity instanceof TiBaseActivity) {
            return ((TiBaseActivity) currentActivity).isInForeground();
        }
        return false;
    }

    /**
     * Check whether the current activity is paused or not.
     * 
     * @return true if the current activity is paused; false otherwise.
     * @module.api
     */
    public static boolean isCurrentActivityPaused() {
        Activity currentActivity = getAppCurrentActivity();
        if (currentActivity instanceof TiBaseActivity) {
            return ((TiBaseActivity) currentActivity).isActivityPaused();
        }
        return false;
    }

    /**
     * This is a convenience method to avoid having to check
     * TiApplication.getInstance() is not null every time we need to grab the
     * current activity.
     * 
     * @return the current activity
     * @module.api
     */
    public static Activity getAppCurrentActivity() {
        TiApplication tiApp = getInstance();
        if (tiApp == null) {
            return null;
        }

        return tiApp.getCurrentActivity();
    }

    /**
     * This is a convenience method to avoid having to check
     * TiApplication.getInstance() is not null every time we need to grab the
     * root or current activity.
     * 
     * @return root activity if exists. If root activity doesn't exist, returns
     *         current activity if exists. Otherwise returns null.
     * @module.api
     */
    public static Activity getAppRootOrCurrentActivity() {
        TiApplication tiApp = getInstance();
        if (tiApp == null) {
            return null;
        }

        return tiApp.getRootOrCurrentActivity();
    }

    /**
     * @return the current activity if exists. Otherwise, the thread will wait
     *         for a valid activity to be visible.
     * @module.api
     */
    public Activity getCurrentActivity() {
        int activityStackSize;

        while ((activityStackSize = activityStack.size()) > 0) {
            Activity activity = (activityStack.get(activityStackSize - 1))
                    .get();

            // Skip and remove any activities which are dead or in the process
            // of finishing.
            if (activity == null || activity.isFinishing()) {
                activityStack.remove(activityStackSize - 1);
                continue;
            }

            return activity;
        }

        Log.d(TAG, "activity stack is empty, unable to get current activity",
                Log.DEBUG_MODE);
        return null;
    }

    /**
     * @return root activity if exists. If root activity doesn't exist, returns
     *         current activity if exists. Otherwise returns null.
     */
    public Activity getRootOrCurrentActivity() {
        Activity activity;
        if (rootActivity != null) {
            activity = rootActivity.get();
            if (activity != null) {
                return activity;
            }
        }

        if (currentActivity != null) {
            activity = currentActivity.get();
            if (activity != null) {
                return activity;
            }
        }

        Log.e(TAG,
                "No valid root or current activity found for application instance");
        return null;
    }

    protected void loadBuildProperties() {
        buildVersion = "1.0";
        buildTimestamp = "N/A";
        buildHash = "N/A";
        InputStream versionStream = getClass().getClassLoader()
                .getResourceAsStream(
                        "org/appcelerator/titanium/build.properties");
        if (versionStream != null) {
            Properties properties = new Properties();
            try {
                properties.load(versionStream);
                if (properties.containsKey("build.version")) {
                    buildVersion = properties.getProperty("build.version");
                }
                if (properties.containsKey("build.timestamp")) {
                    buildTimestamp = properties.getProperty("build.timestamp");
                }
                if (properties.containsKey("build.githash")) {
                    buildHash = properties.getProperty("build.githash");
                }
            } catch (IOException e) {
            }
        }
    }
    
    private boolean loadingProps = false;
    public void loadAppProperties() {
        // Load the JSON file:
        if (loadingProps || TiProperties.systemPropertiesLoaded()) {
            return;
        }
        loadingProps = true;
        String appPropertiesString = KrollAssetHelper
                .readAsset("Resources/_app_props_.json");
        if (appPropertiesString != null) {
            try {
                TiProperties.setSystemProperties(new JSONObject(
                        appPropertiesString));
            } catch (JSONException e) {
                Log.e(TAG, "Unable to load app properties.");
            }
        }
        loadingProps = false;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Application onCreate", Log.DEBUG_MODE);

        final UncaughtExceptionHandler defaultHandler = Thread
                .getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
            public void uncaughtException(Thread t, Throwable e) {
                if (isAnalyticsEnabled()) {
                    String tiVer = buildVersion + "," + buildTimestamp + ","
                            + buildHash;
                    Log.e(TAG,
                            "Sending event: exception on thread: "
                                    + t.getName() + " msg:" + e.toString()
                                    + "; Titanium " + tiVer, e);
                    TiPlatformHelper.getInstance().postAnalyticsEvent(
                            TiAnalyticsEventFactory.createErrorEvent(t, e,
                                    tiVer));
                }
                defaultHandler.uncaughtException(t, e);
            }
        });

        appProperties = new TiProperties(getApplicationContext(),
                APPLICATION_PREFERENCES_NAME, false);

        baseUrl = TiC.URL_ANDROID_ASSET_RESOURCES;

        File fullPath = new File(baseUrl, getStartFilename("app.js"));
        baseUrl = fullPath.getParent();

        proxyMap = new HashMap<String, SoftReference<KrollProxy>>(5);

        tempFileHelper = new TiTempFileHelper(this);
    }

    @Override
    public void onTerminate() {
        stopExternalStorageMonitor();
        accessibilityManager = null;
        super.onTerminate();
    }

    @Override
    public void onLowMemory() {
        // Release all the cached images
        if (_picassoMermoryCache != null) {
            _picassoMermoryCache.clear();
        }
        super.onLowMemory();
    }

    @SuppressLint("NewApi")
    @Override
    public void onTrimMemory(int level) {
        if (TiC.HONEYCOMB_OR_GREATER
                && level >= TRIM_MEMORY_RUNNING_LOW) {
            // Release all the cached images
            if (_picassoMermoryCache != null) {
                _picassoMermoryCache.clear();
            }
        }
        super.onTrimMemory(level);
    }

    public void postAppInfo() {
        deployData = new TiDeployData(this);

        TiPlatformHelper.getInstance().initialize();
    }
    
    public static class TiBitmapMemoryCache extends LruCache
    {
        public TiBitmapMemoryCache(int maxSize) {
            super(maxSize);
        }
    }
    
    
    
    private static TiBitmapMemoryCache _picassoMermoryCache;
    public static TiBitmapMemoryCache getImageMemoryCache() {
        if (_picassoMermoryCache == null) {
//            int maxMemory = (int) (Runtime.getRuntime().maxMemory());
//            int cacheSize = maxMemory / 7;
            _picassoMermoryCache = new TiBitmapMemoryCache(TiActivityHelper.calculateMemoryCacheSize(getAppContext()));
        }
        return _picassoMermoryCache;
    }
    
    private static Picasso _picasso;
    public static Picasso getPicassoInstance() {
        if (_picasso == null) {
            _picasso = new Picasso.Builder(getAppContext())
            .memoryCache(getImageMemoryCache())
            .downloader(new OkHttpDownloader(getPicassoHttpClientInstance()))
            .build();
        }
        return _picasso;
    }
    
    static File createDefaultCacheDir(Context context, String path) {
        File cacheDir = getAppContext().getExternalCacheDir();
        if (cacheDir == null)
            cacheDir = getAppContext().getCacheDir();
        File cache = new File(cacheDir, path);
        if (!cache.exists()) {
            cache.mkdirs();
        }
        return cache;
    }
    
    private static HashMap<String, String> _currentCacheDir = new HashMap<>();
    private static HashMap<String, Cache> _currentCache = new HashMap<>();

    public static Cache getDiskCache(final String name) {
        if (_currentCache.get(name) == null) {
            File cacheDir = createDefaultCacheDir(getAppContext(), name);
            if (_currentCacheDir.get(name) == null || cacheDir == null || !cacheDir.equals(_currentCacheDir.get(name).toString())) {
                    if (cacheDir != null ) {
                        int maxCacheSize = getInstance().getAppProperties().getInt(CACHE_SIZE_KEY, DEFAULT_CACHE_SIZE) * 1024 * 1024;
                        _currentCacheDir.put(name, cacheDir.toString());
                        _currentCache.put(name, new Cache(cacheDir, maxCacheSize));
                    } else {
                        _currentCache.remove(name);
                        _currentCacheDir.remove(name);
                    }
            }
        }
        return _currentCache.get(name);
    }
//    public static Cache getHttpDiskCache() {
//        return getDiskCache("http");
//    }
    
    public static void clearDiskCache(final String name) {
        Cache cache = getDiskCache(name);
        if (cache != null) {
            try {
                cache.evictAll();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    public static void closeCache(final String name) {
        Cache cache = _currentCache.get(name);
        if (cache != null) {
            try {
                cache.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            _currentCache.remove(name);
        }
    }
    
    private static OkHttpClient _httpClient;
    private static OkHttpClient _picassoHttpClient;
    private static final String CACHE_SIZE_KEY = "ti.android.cache.size.max";
    private static final int DEFAULT_CACHE_SIZE = 25; // 25MB
    public static OkHttpClient getOkHttpClientInstance() {
        if (_httpClient == null) {
            _httpClient = new OkHttpClient();
            _httpClient.setCache(getDiskCache("http"));
            _httpClient.interceptors().add(new com.squareup.okhttp.Interceptor() {
                @Override public com.squareup.okhttp.Response intercept(Chain chain) throws IOException {
                    com.squareup.okhttp.Request.Builder builder = chain.request().newBuilder();
                    builder.addHeader("TiCache", "true");
                    builder.addHeader("Cache-Control", "no-cached");
                    builder.addHeader("User-Agent", TiApplication.getInstance()
                            .getUserAgent());
                    builder.addHeader("X-Requested-With", "XMLHttpRequest");
                    
                    return chain.proceed(builder.build());
                }
              });
        }
        return _httpClient;
    }
    public static OkHttpClient getPicassoHttpClientInstance() {
        if (_picassoHttpClient == null) {
            _picassoHttpClient = new OkHttpClient();
            _picassoHttpClient.setCache(getDiskCache("image"));
        }
        return _picassoHttpClient;
    }
    
    private static void updateCaches() {
        closeCache("http");
        closeCache("image");
        getOkHttpClientInstance().setCache(getDiskCache("http"));
        getPicassoHttpClientInstance().setCache(getDiskCache("image"));
    }
    
    
    public static void prepareURLConnection(HttpURLConnection connection,
            HashMap options) {
        connection.setUseCaches(true);
        connection.addRequestProperty("TiCache", "true");
        connection.addRequestProperty("Cache-Control", "no-cached");
        connection.addRequestProperty("User-Agent", TiApplication.getInstance()
                .getUserAgent());
        connection.addRequestProperty("X-Requested-With", "XMLHttpRequest");

        if (options != null) {
            Object value = options.get("headers");
            if (value != null && value instanceof HashMap) {
                HashMap<String, Object> headers = (HashMap<String, Object>) value;
                for (Map.Entry<String, Object> entry : headers.entrySet()) {
                    connection.addRequestProperty(entry.getKey(),
                            TiConvert.toString(entry.getValue()));
                }
            }
            if (options.containsKey("timeout")) {
                int timeout = TiConvert.toInt(options, "timeout");
                connection.setConnectTimeout(timeout);
            }
            if (options.containsKey("autoRedirect")) {
                connection.setInstanceFollowRedirects(TiConvert.toBoolean(
                        options, "autoRedirect"));
            }
            if (options.containsKey("method")) {
                Object data = options.get("data");
                if (data instanceof String) {
                    connection.setRequestProperty("Content-Type",
                            "charset=utf-8");
                } else if (data instanceof HashMap) {
                    connection.setRequestProperty("Content-Type",
                            "application/json; charset=utf-8");
                }
                String dataToSend = TiConvert.toString(data);
                if (dataToSend != null) {
                    byte[] outputInBytes;
                    try {
                        outputInBytes = dataToSend.getBytes("UTF-8");
                        OutputStream os = connection.getOutputStream();
                        os.write(outputInBytes);
                        os.close();
                        connection.setRequestMethod(TiConvert.toString(options,
                                "method"));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    
                }
            }
        }
    }
    
    public static OkHttpClient getPicassoHttpClient(final KrollDict options) {
        OkHttpClient client = getPicassoHttpClientInstance();
        if (options == null) {
            return client;
        }
        client = client.clone();
        if (options.containsKey("timeout")) {
            int timeout = TiConvert.toInt(options, "timeout");
            client.setConnectTimeout(timeout, TimeUnit.MILLISECONDS);
            client.setReadTimeout(timeout, TimeUnit.MILLISECONDS);
            client.setWriteTimeout(timeout, TimeUnit.MILLISECONDS);
        }
        if (options.containsKey("autoRedirect")) {
            boolean redirect = TiConvert.toBoolean(options, "autoRedirect");
            client.setFollowRedirects(redirect);
            client.setFollowSslRedirects(redirect);
        }
        client.interceptors().add(new com.squareup.okhttp.Interceptor() {
          @Override public com.squareup.okhttp.Response intercept(Chain chain) throws IOException {
              com.squareup.okhttp.Request.Builder builder = chain.request().newBuilder();
              TiHTTPHelper.prepareBuilder(builder, TiConvert.toString(options, "method", "get"), options.get("data"));
              Object value = options.get("headers");
              if (value != null && value instanceof HashMap) {
                  HashMap<String, Object> headers = (HashMap<String, Object>) value;
                  for (Map.Entry<String, Object> entry : headers.entrySet()) {
                      builder.addHeader(entry.getKey(), TiConvert.toString(entry.getValue()));
                  }
              }
              return chain.proceed(builder.build());
          }
        });
        return client;
    }

    
    public static OkHttpClient httpClient(final HashMap options) {
        if (options == null) {
            return getOkHttpClientInstance();
        }
        OkHttpClient client = getOkHttpClientInstance().clone();
        int timeout = 20000;        

        if (options.containsKey("timeout")) {
            timeout = TiConvert.toInt(options, "timeout");
        }
        if (options.containsKey("autoRedirect")) {
            client.setFollowSslRedirects(TiConvert.toBoolean(options, "autoRedirect"));
        }

        client.setConnectTimeout(timeout, TimeUnit.MILLISECONDS);
        client.setReadTimeout(timeout, TimeUnit.MILLISECONDS);
        return client;
    }

    public void postOnCreate() {
        loadAppProperties();

        KrollRuntime runtime = KrollRuntime.getInstance();
        if (runtime != null) {
            Log.i(TAG,
                    "Titanium Javascript runtime: " + runtime.getRuntimeName());
        } else {
            // This ought not to be possible.
            Log.w(TAG, "Titanium Javascript runtime: unknown");
        }

        TiConfig.DEBUG = TiConfig.LOGD = appProperties.getBool(
                "ti.android.debug", false);
        USE_LEGACY_WINDOW = appProperties.getBool(PROPERTY_USE_LEGACY_WINDOW,
                false);

        startExternalStorageMonitor();

        // Register the default cache handler
        KrollRuntime.setPrimaryExceptionHandler(new TiExceptionHandler());
    }

//    private File getRemoteCacheDir() {
//        File cacheDir = new File(tempFileHelper.getTempDirectory(),
//                "remote-cache");
//        if (!cacheDir.exists()) {
//            cacheDir.mkdirs();
//            tempFileHelper.excludeFileOnCleanup(cacheDir);
//        }
//        return cacheDir.getAbsoluteFile();
//    }

    public void setRootActivity(TiRootActivity rootActivity) {
        this.rootActivity = new WeakReference<TiRootActivity>(rootActivity);
        rootActivityLatch.countDown();
        
        TiPlatformHelper.getInstance().initAnalytics();
        TiPlatformHelper.getInstance().setSdkVersion(
                "ti." + getTiBuildVersion());
        TiPlatformHelper.getInstance().setAppName(getAppInfo().getName());
        TiPlatformHelper.getInstance().setAppId(getAppInfo().getId());
        TiPlatformHelper.getInstance().setAppVersion(getAppInfo().getVersion());

        String deployType = appProperties.getString("ti.deploytype", "unknown");
        String buildType = appInfo.getBuildType();
        if ("unknown".equals(deployType)) {
            deployType = getDeployType();
        }
        if (buildType != null && !buildType.equals("")) {
            TiPlatformHelper.getInstance().setBuildType(buildType);
        }
		// Just use type 'other' enum since it's open ended.
		DeployType.OTHER.setName(deployType);
		TiPlatformHelper.getInstance().setDeployType(DeployType.OTHER);
        Log.d(TAG, "TiPlatformHelper.deployType: " + TiPlatformHelper.getInstance().getDeployType(), Log.DEBUG_MODE);
        if (isAnalyticsEnabled()) {
            APSAnalytics.getInstance().sendAppEnrollEvent();
        } else {
            Log.d(TAG, "Analytics have been disabled", Log.DEBUG_MODE);
        }
        tempFileHelper.scheduleCleanTempDir();
    }

    /**
     * @return the app's root activity if exists, null otherwise.
     */
    public TiRootActivity getRootActivity() {
        if (rootActivity == null) {
            return null;
        }

        return rootActivity.get();
    }

    /**
     * @return whether the root activity is available
     */
    public boolean isRootActivityAvailable() {
        if (rootActivity != null) {
            Activity activity = rootActivity.get();
            if (activity != null) {
                return !activity.isFinishing();
            }
        }

        return false;
    }

    public void setCurrentActivity(Activity callingActivity, Activity newValue) {
        synchronized (this) {
            Activity currentActivity = getCurrentActivity();
            if (currentActivity == null || callingActivity == currentActivity) {
                this.currentActivity = new WeakReference<Activity>(newValue);
            }
        }
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getStartUrl() {
        return startUrl;
    }

    private String getStartFilename(String defaultStartFile) {
        return defaultStartFile;
    }

    public void addAppEventProxy(KrollProxy appEventProxy) {
        if (appEventProxy != null && !appEventProxies.contains(appEventProxy)) {
            appEventProxies.add(new WeakReference<KrollProxy>(appEventProxy));
        }
    }

    public void removeAppEventProxy(KrollProxy appEventProxy) {
        appEventProxies.remove(appEventProxy);
    }

    public boolean fireAppEvent(String eventName, KrollDict data) {
        boolean handled = false;
        for (WeakReference<KrollProxy> weakProxy : appEventProxies) {
            KrollProxy appEventProxy = weakProxy.get();
            if (appEventProxy == null) {
                continue;
            }

            boolean proxyHandled = appEventProxy.fireEvent(eventName, data);
            handled = handled || proxyHandled;
        }

        return handled;
    }

    /**
     * @return the app's properties, which are listed in tiapp.xml. App
     *         properties can also be set at runtime by the application in
     *         Javascript.
     * @module.api
     */
    public TiProperties getAppProperties() {
        loadAppProperties();
        return appProperties;
    }

    /**
     * @deprecated
     */
    public TiProperties getSystemProperties() {
        // This should actually be removed, but we are changing it to
        // 'appProperties' instead so we don't break module
        // developers who use this.
        return appProperties;
    }

    public ITiAppInfo getAppInfo() {
        return appInfo;
    }
    
    public static Context getAppContext() {
        return getInstance().getApplicationContext();
    }
    
    public static float getAppDensity() {
        if (sAppDensity == -1) {
            sAppDensity =  TiDimension.getDisplayMetrics().density;
        }
        return sAppDensity;
    }
    
    public static int getAppDensityDpi() {
        if (sAppDensityDpi == -1) {
            sAppDensityDpi =  TiDimension.getDisplayMetrics().densityDpi;
        }
        return sAppDensityDpi;
    }
    
    public static float getAppScaledDensity() {
        if (sAppScaledDensity == -1) {
            sAppScaledDensity =  TiDimension.getDisplayMetrics().scaledDensity;
        }
        return sAppScaledDensity;
    }
    
    public static String getAppDensityString() {
        if (sAppDensityString == null) {
            switch(getAppDensityDpi()) {
            case DisplayMetrics.DENSITY_HIGH :
            case 213: //TV
                sAppDensityString = "high";
            case DisplayMetrics.DENSITY_MEDIUM :
                sAppDensityString = "medium";
            case 280: //Introduce in API 22.
            case 320 : // DisplayMetrics.DENSITY_XHIGH (API 9)
                sAppDensityString = "xhigh";
            case 400:
            case 480 :
                sAppDensityString = "xxhigh";
            case 560:
            case 640 :
                sAppDensityString = "xxxhigh";
            case DisplayMetrics.DENSITY_LOW :
                sAppDensityString = "low";
            default :
                sAppDensityString = "medium";
            }
        }
        return sAppDensityString;
    }

    /**
     * @return the app's GUID. Each application has a unique GUID.
     */
    public String getAppGUID() {
        return getAppInfo().getGUID();
    }

    public KrollDict getStylesheet(String basename, Collection<String> classes,
            String objectId) {
        if (stylesheet != null) {
            return stylesheet.getStylesheet(objectId, classes, getAppDensityString(),
                    basename);
        }
        return null;
    }

    public void registerProxy(KrollProxy proxy) {
        String proxyId = proxy.getProxyId();
        if (!proxyMap.containsKey(proxyId)) {
            proxyMap.put(proxyId, new SoftReference<KrollProxy>(proxy));
        }
    }

    public KrollProxy unregisterProxy(String proxyId) {
        KrollProxy proxy = null;

        SoftReference<KrollProxy> ref = proxyMap.remove(proxyId);
        if (ref != null) {
            proxy = ref.get();
        }

        return proxy;
    }

    public boolean isAnalyticsEnabled()
	{
		return getAppInfo().isAnalyticsEnabled();
	}
	
	public void setFilterAnalyticsEvents(String[] events)
	{
		filteredAnalyticsEvents = events;
	}
	
	public boolean isAnalyticsFiltered(String eventName)
	{
		if (filteredAnalyticsEvents == null) {
			return false;
		}

		for (int i = 0; i < filteredAnalyticsEvents.length; ++i) {
			String currentName = filteredAnalyticsEvents[i];
			if (eventName.equals(currentName)) {
				return true;
			}
					
		}
		return false;
	}

    public String getDeployType() {
        return getAppInfo().getDeployType();
    }
    
    public String getUserAgent() {
        if (TITANIUM_USER_AGENT == null) {
            TITANIUM_USER_AGENT = "Appcelerator Titanium/" + getTiBuildVersion()
                    + " ("+ Build.MODEL + "; Android API Level: "
                    + Integer.toString(Build.VERSION.SDK_INT) + "; "
                    + TiPlatformHelper.getInstance().getLocale() +";)";
        }
        return TITANIUM_USER_AGENT;
    }

    /**
     * @return the build version, which is built in as part of the SDK.
     */
    public String getTiBuildVersion() {
        return buildVersion;
    }

    public String getTiBuildTimestamp() {
        return buildTimestamp;
    }

    public String getTiBuildHash() {
        return buildHash;
    }

    public String getDefaultUnit() {
        if (defaultUnit == null) {
            defaultUnit = getAppProperties().getString(PROPERTY_DEFAULT_UNIT,
                    SYSTEM_UNIT);
            // Check to make sure default unit is valid, otherwise use system
            Pattern unitPattern = Pattern.compile("system|px|dp|dip|mm|cm|in");
            Matcher m = unitPattern.matcher(defaultUnit);
            if (!m.matches()) {
                defaultUnit = SYSTEM_UNIT;
            }
        }
        return defaultUnit;
    }

    public int getThreadStackSize() {
        return getAppProperties().getInt(PROPERTY_THREAD_STACK_SIZE,
                DEFAULT_THREAD_STACK_SIZE);
    }

    public boolean forceCompileJS() {
        return getAppProperties().getBool(PROPERTY_COMPILE_JS, false);
    }

    public TiDeployData getDeployData() {
        return deployData;
    }

    public boolean isFastDevMode() {
        /*
         * Fast dev is enabled by default in development mode, and disabled
         * otherwise When the property is set, it overrides the default behavior
         * on emulator only Deploy types are as follow: Emulator: 'development'
         * Device: 'test'
         */
        boolean development = getDeployType().equals(
                TiApplication.DEPLOY_TYPE_DEVELOPMENT);
        if (!development) {
            return false;
        }
        return getAppProperties().getBool(TiApplication.PROPERTY_FASTDEV,
                development);
    }

    public boolean isCoverageEnabled() {
        if (!getDeployType().equals(TiApplication.DEPLOY_TYPE_PRODUCTION)) {
            return getAppProperties().getBool(
                    TiApplication.PROPERTY_ENABLE_COVERAGE, false);
        }
        return false;
    }

    public void scheduleRestart(int delay) {
        Log.w(TAG, "Scheduling application restart");
        if (Log.isDebugModeEnabled()) {
            Log.d(TAG,
                    "Here is call stack leading to restart. (NOTE: this is not a real exception, just a stack trace.) :");
            (new Exception()).printStackTrace();
        }
        this.restartPending = true;
        TiRootActivity rootActivity = getRootActivity();
        if (rootActivity != null) {
            rootActivity.restartActivity(delay);
        }
    }

    public boolean isRestartPending() {
        return restartPending;
    }

    public TiTempFileHelper getTempFileHelper() {
        return tempFileHelper;
    }

    /**
     * @return true if the current thread is the main thread, false otherwise.
     * @module.api
     */
    public static boolean isUIThread() {
        if (sMainThreadId == Thread.currentThread().getId()) {
            return true;
        }

        return false;
    }

    public KrollModule getModuleByName(String name) {
        WeakReference<KrollModule> module = modules.get(name);
        if (module == null) {
            return null;
        }

        return module.get();
    }

    public void registerModuleInstance(String name, KrollModule module) {
        if (modules.containsKey(name)) {
            Log.w(TAG, "Registering module with name already in use.");
        }

        modules.put(name, new WeakReference<KrollModule>(module));
    }

    public void waitForCurrentActivity(CurrentActivityListener l) {
        TiUIHelper.waitForCurrentActivity(l);
    }

    public boolean isDebuggerEnabled() {
        return getDeployData().isDebuggerEnabled();
    }

    private void startExternalStorageMonitor() {
        externalStorageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Intent.ACTION_MEDIA_MOUNTED.equals(intent.getAction())) {
                    updateCaches();
                    Log.i(TAG,
                            "SD card has been mounted. Enabling cache for http responses.",
                            Log.DEBUG_MODE);

                } else {
                    // if the sd card is removed, we don't cache http responses
                    updateCaches();
                    Log.i(TAG,
                            "SD card has been unmounted. Disabling cache for http responses.",
                            Log.DEBUG_MODE);
                }
            }
        };

        IntentFilter filter = new IntentFilter();

        filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_REMOVED);
        filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
        filter.addDataScheme("file");

        registerReceiver(externalStorageReceiver, filter);
    }

    private void stopExternalStorageMonitor() {
        unregisterReceiver(externalStorageReceiver);
    }

    public void dispose() {
        appEventProxies.clear();
        proxyMap.clear();
        if (TiApplication.activityTransitionListeners != null) {
            TiApplication.activityTransitionListeners.clear();
        }
        if (TiApplication.sAppStateListeners != null) {
            TiApplication.sAppStateListeners.clear();
        }
        if (TiApplication.activityStack != null) {
            TiApplication.activityStack.clear();
        }
        if (TiApplication.modules != null) {
            TiApplication instance = getInstance();
            for (WeakReference<KrollModule> module : modules.values()) {
                module.get().onAppTerminate(instance);
                module.get().release();
            }
            TiApplication.modules.clear();
        }
        TiActivityWindows.dispose();
        TiActivitySupportHelpers.dispose();
        TiFileHelper.getInstance().destroyTempFiles();
    }

    public void cancelTimers() {
        TitaniumModule.cancelTimers();
    }

    /**
     * Our forced restarts (for conditions such as android bug 2373, TIMOB-1911
     * and TIMOB-7293) don't create new processes or pass through
     * TiApplication() (the ctor). We need to reset some state to better mimic a
     * complete application restart.
     */
    public void beforeForcedRestart() {
        restartPending = false;
        currentActivity = null;
        appEventProxies.clear();
        proxyMap.clear();
        TiApplication.isActivityTransition.set(false);
        if (TiApplication.activityTransitionListeners != null) {
            TiApplication.activityTransitionListeners.clear();
        }
        if (TiApplication.sAppStateListeners != null) {
            TiApplication.sAppStateListeners.clear();
        }
        if (TiApplication.activityStack != null) {
            TiApplication.activityStack.clear();
        }
    }

    public AccessibilityManager getAccessibilityManager() {
        if (accessibilityManager == null) {
            accessibilityManager = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
        }
        return accessibilityManager;
    }

    public void setForceFinishRootActivity(boolean forced) {
        forceFinishRootActivity = forced;
    }

    public boolean getForceFinishRootActivity() {
        return forceFinishRootActivity;
    }

    public abstract void verifyCustomModules(TiRootActivity rootActivity);

    private static boolean paused = false;
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == PAUSE) {
                for (int i = 0; i < sAppStateListeners.size(); ++i) {
                    sAppStateListeners.get(i).onAppPaused();
                }
                fireAppEvent(TiC.EVENT_PAUSE, null);
                paused = true;
                final AsyncTask<Void, Void, Void> sendTask = new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(final Void... params) {
                        for (WeakReference<KrollModule> module : modules.values()) {
                            module.get().onAppPaused();
                        }
                        return null;
                    }
                };
                sendTask.execute();
            }
        }
    };
    private static final int PAUSE = 123;
    private static final int PAUSE_TIMEOUT = 400;

    public void activityPaused(Activity activity) {
        if (!paused && activity == getAppCurrentActivity()
                && !isCurrentActivityPaused()) {
            mHandler.removeMessages(PAUSE);
            mHandler.sendEmptyMessageDelayed(PAUSE, PAUSE_TIMEOUT);
        }
    }

    public void activityStopped(Activity activity) {
    }

    public void activityResumed(Activity activity) {
        mHandler.removeMessages(PAUSE);
        if (paused && activity == getAppCurrentActivity()
                && isCurrentActivityPaused()) {
            for (int i = 0; i < sAppStateListeners.size(); ++i) {
                sAppStateListeners.get(i).onAppResume();
            }
            fireAppEvent(TiC.EVENT_RESUME, null);
            paused = false;
            final AsyncTask<Void, Void, Void> sendTask = new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(final Void... params) {
                    for (WeakReference<KrollModule> module : modules.values()) {
                        module.get().onAppResumed();
                    }
                    return null;
                }
            };
            sendTask.execute();
        }
    }

    public void activityStarted(Activity activity) {
        cancelPauseEvent();
    }

    public void cancelPauseEvent() {
        mHandler.removeMessages(PAUSE);
    }

    public boolean isPaused() {
        return paused;
    }
    
    
    public static boolean testPermission(final String permission) {
        int res = getAppContext().checkCallingOrSelfPermission("android.permission." + permission);
        return (res == PackageManager.PERMISSION_GRANTED);
    }
    
    
//    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private static int googlePlayServicesState = -1;
    private static boolean googlePlayServicesAvailable = false;
    private static String googlePlayServicesErrorMessage = null;
    public static int getGooglePlayServicesState() {
        if (googlePlayServicesState == -1) {
            final Activity activity = getAppRootOrCurrentActivity();
            try {
                Class<?> c = Class.forName("com.google.android.gms.common.GooglePlayServicesUtil");
                Method  isGooglePlayServicesAvailable = c.getDeclaredMethod ("isGooglePlayServicesAvailable", Context.class);
                googlePlayServicesState = (int) isGooglePlayServicesAvailable.invoke(null, new Object[] {activity});
            } catch (Exception e) {
                googlePlayServicesState = 1;
                Throwable cause = e.getCause();
                if (cause != null) {
                    googlePlayServicesErrorMessage = cause.getMessage();
                }
                e.printStackTrace();
            }
            googlePlayServicesAvailable = googlePlayServicesState == 0;
        }
        return googlePlayServicesState;
    }
    public static String getGooglePlayServicesErrorString() {
        if (googlePlayServicesErrorMessage == null) {
            try {
                Class<?> c = Class.forName("com.google.android.gms.common.GooglePlayServicesUtil");
                Method  isGooglePlayServicesAvailable = c.getDeclaredMethod ("getErrorString", Integer.class);
                googlePlayServicesErrorMessage = (String) isGooglePlayServicesAvailable.invoke(null, new Object[] {getGooglePlayServicesState()});
                
            } catch (Exception e) {
            }
        }
        return googlePlayServicesErrorMessage;
    }
    
    public static boolean isGooglePlayServicesAvailable() {
        if (googlePlayServicesState == -1) {
            getGooglePlayServicesState();
        }
        return googlePlayServicesAvailable;
    }
}
