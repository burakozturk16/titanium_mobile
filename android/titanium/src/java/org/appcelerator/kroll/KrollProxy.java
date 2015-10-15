/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package org.appcelerator.kroll;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.APIMap;
import org.appcelerator.kroll.common.AsyncResult;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.kroll.common.TiMessenger;
import org.appcelerator.kroll.common.TiMessenger.Command;
import org.appcelerator.kroll.common.TiMessenger.CommandNoReturn;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiBaseActivity;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiContext;
import org.appcelerator.titanium.TiViewEventOverrideDelegate;
import org.appcelerator.titanium.proxy.ActivityProxy;
import org.appcelerator.titanium.proxy.TiWindowProxy;
import org.appcelerator.titanium.TiLifecycle.OnLifecycleEvent;
import org.appcelerator.titanium.util.TiActivityHelper;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiRHelper;
import org.appcelerator.titanium.util.TiUIHelper;
import org.appcelerator.titanium.util.TiUrl;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.util.Pair;

import org.json.JSONObject;

/**
 * This is the parent class of all proxies. A proxy is a dynamic object that can
 * be created or queried by the user through a module or another proxy's API.
 * When you create a native view with <a href=
 * "http://developer.appcelerator.com/apidoc/mobile/latest/Titanium.UI.createView-method.html"
 * >Titanium.UI.createView </a>, the view object is a proxy itself.
 */
@Kroll.proxy(name = "KrollProxy", propertyAccessors = { KrollProxy.PROPERTY_HAS_JAVA_LISTENER })
@SuppressWarnings({ "unchecked", "rawtypes" })
public class KrollProxy implements Handler.Callback, KrollProxySupport, OnLifecycleEvent {
    public static interface SetPropertyChangeListener {
        public void onSetProperty(KrollProxy proxy, String name, Object value);
    }

    private static final String TAG = "KrollProxy";
    private static final int INDEX_NAME = 0;
    private static final int INDEX_OLD_VALUE = 1;
    private static final int INDEX_VALUE = 2;

    private static final String ERROR_CREATING_PROXY = "Error creating proxy";

    protected static final int MSG_MODEL_PROPERTY_CHANGE = KrollObject.MSG_LAST_ID + 100;
    protected static final int MSG_LISTENER_ADDED = KrollObject.MSG_LAST_ID + 101;
    protected static final int MSG_LISTENER_REMOVED = KrollObject.MSG_LAST_ID + 102;
    protected static final int MSG_MODEL_PROCESS_PROPERTIES = KrollObject.MSG_LAST_ID + 103;
    protected static final int MSG_MODEL_PROPERTIES_CHANGED = KrollObject.MSG_LAST_ID + 104;
    protected static final int MSG_INIT_KROLL_OBJECT = KrollObject.MSG_LAST_ID + 105;
    protected static final int MSG_SET_PROPERTY = KrollObject.MSG_LAST_ID + 106;
    protected static final int MSG_FIRE_EVENT = KrollObject.MSG_LAST_ID + 107;
    protected static final int MSG_FIRE_SYNC_EVENT = KrollObject.MSG_LAST_ID + 108;
    protected static final int MSG_CALL_PROPERTY_ASYNC = KrollObject.MSG_LAST_ID + 109;
    protected static final int MSG_CALL_PROPERTY_SYNC = KrollObject.MSG_LAST_ID + 110;
    protected static final int MSG_MODEL_APPLY_PROPERTIES = KrollObject.MSG_LAST_ID + 111;
    protected static final int MSG_UPDATE_KROLL_PROPERTIES = KrollObject.MSG_LAST_ID + 112;
    protected static final int MSG_GET_PROPERTY = KrollObject.MSG_LAST_ID + 113;
    protected static final int MSG_LAST_ID = MSG_GET_PROPERTY;
    protected static final String PROPERTY_NAME = "name";
    protected static final String PROPERTY_BUBBLES = "checkParent";
    protected static final String PROPERTY_HAS_JAVA_LISTENER = "_hasJavaListener";

    protected static AtomicInteger proxyCounter = new AtomicInteger();
    protected AtomicInteger listenerIdGenerator;

    protected Map<String, List<KrollDict>> evaluators;
    protected Map<String, HashMap<Integer, KrollEventCallback>> eventListeners;
    protected KrollObject krollObject;
    protected WeakReference<Activity> activity;
    protected String proxyId;
    protected TiUrl creationUrl;
    protected WeakReference<KrollProxyListener> modelListener;
    protected SetPropertyChangeListener setPropertyListener;
    protected KrollModule createdInModule;
    protected boolean coverageEnabled;
    protected KrollDict properties = new KrollDict();
    protected KrollDict defaultValues = new KrollDict();
    protected Handler mainHandler = null;
    protected Handler runtimeHandler = null;

    private KrollDict langConversionTable = null;
    private boolean bubbleParent = true;
    private boolean bubbleParentDefined = false;
    private WeakReference<TiViewEventOverrideDelegate> eventOverrideDelegate = null;

    private List<String> mSyncEvents;

    private HashMap<String, Object> propertiesToUpdateNativeSide = null;
    private boolean readyToUpdateNativeSideProperties = false;

    public static final String PROXY_ID_PREFIX = "proxy$";
    
    protected boolean mProcessInUIThread = false; 

    public KrollProxy(TiContext tiContext) {
        this();
    }

    /**
     * The default KrollProxy constructor. Equivalent to
     * <code>KrollProxy("")</code>
     * 
     * @module.api
     */
    public KrollProxy() {
        this("");
    }

    /**
     * Constructs a KrollProxy, using the passed in creation URL.
     * 
     * @param baseCreationUrl
     *            the creation URL for this proxy, which can be used to resolve
     *            relative paths
     * @module.api
     */
    public KrollProxy(String baseCreationUrl) {
        creationUrl = new TiUrl(baseCreationUrl);
        this.listenerIdGenerator = new AtomicInteger(0);
        this.eventListeners = Collections
                .synchronizedMap(new HashMap<String, HashMap<Integer, KrollEventCallback>>());
        this.langConversionTable = getLangConversionTable();
    }

    private void setupProxy(KrollObject object, Object[] creationArguments,
            TiUrl creationUrl) {
        if (object != null) {
            // Store reference to the native object that represents this proxy
            // so we can drive changes to the JS
            // object
            krollObject = object;
            object.setProxySupport(this);
        }
        this.creationUrl = creationUrl;

        // Associate the activity with the proxy. if the proxy needs activity
        // association delayed until a
        // later point then initActivity should be overridden to be a no-op and
        // then call setActivity directly
        // at the appropriate time
        initActivity(TiApplication.getInstance().getCurrentActivity());

        // Setup the proxy according to the creation arguments TODO - pass in
        // createdInModule
        handleCreationArgs(null, creationArguments);
    }

    // entry point for generator code
    public static KrollProxy createProxy(
            Class<? extends KrollProxy> proxyClass, KrollObject object,
            Object[] creationArguments, String creationUrl) {
        try {
            KrollProxy proxyInstance = proxyClass.newInstance();
            proxyInstance.setupProxy(object, creationArguments,
                    TiUrl.createProxyUrl(creationUrl));
            return proxyInstance;

        } catch (Exception e) {
            Log.e(TAG, ERROR_CREATING_PROXY, e);
        }

        return null;
    }

    /*
     * This method exists so that it can be used in the situations (mainly
     * custom modules) where a proxy is being created with the old TiContext
     * argument.
     */
    public static KrollProxy createDeprecatedProxy(
            Class<? extends KrollProxy> proxyClass, KrollObject object,
            Object[] creationArguments, String creationUrl) {
        try {
            Constructor<? extends KrollProxy> oldConstructor = proxyClass
                    .getConstructor(TiContext.class);

            TiUrl url = TiUrl.createProxyUrl(creationUrl);
            TiContext tiContext = new TiContext(TiApplication.getInstance()
                    .getCurrentActivity(), url.baseUrl);
            KrollProxy proxyInstance = oldConstructor.newInstance(tiContext);

            proxyInstance.setupProxy(object, creationArguments, url);
            return proxyInstance;

        } catch (Exception e) {
            // Reflection, you my only friend
            Log.e(TAG, ERROR_CREATING_PROXY, e);
        }

        return null;
    }

    protected void initActivity(Activity activity) {
        setActivity(activity);
    }

    public void setActivity(Activity activity) {
        if (activity != null) {
            this.activity = new WeakReference<Activity>(activity);
        } else {
            this.activity = null;
        }
    }

	public void attachActivityLifecycle(Activity activity)
	{
		setActivity(activity);
		((TiBaseActivity) activity).addOnLifecycleEventListener(this);
	}


    /**
     * @return the activity associated with this proxy. It can be null.
     * @module.api
     */
    public Activity getActivity() {
        if (activity == null) {
            return null;
        }
        return activity.get();
    }

    /**
     * Handles the arguments passed into the "create" method for this proxy. If
     * your proxy simply needs to handle a KrollDict, see
     * {@link KrollProxy#handleCreationDict(KrollDict)}
     * 
     * @param args
     * @module.api
     */
    public void handleCreationArgs(KrollModule createdInModule, Object[] args) {
        this.createdInModule = createdInModule;

        if (args.length == 0 || !(args[0] instanceof HashMap)) {
            handleDefaultValues();
            return;
        }

        KrollDict dict;
        if (args[0] instanceof KrollDict) {
            dict = (KrollDict) args[0];
        } else {
            dict = new KrollDict((HashMap) args[0]);
        }
        handleCreationDict(dict);
    }

    /**
     * Handles initialization of the proxy's default property values.
     * 
     * @module.api
     */
    protected void handleDefaultValues() {
        synchronized (properties) {
            for (String key : defaultValues.keySet()) {
                if (!properties.containsKey(key)) {
                    setProperty(key, defaultValues.get(key));
                }
            }
        }

    }

    public Object getDefaultValue(String key) {
        return defaultValues.get(key);
    }

    /**
     * @return the language conversion table used to load localized values for
     *         certain properties from the locale files. For each localizable
     *         property, such as "title," the proxy should define a second
     *         property, such as "titleid", used to specify a localization key
     *         for that property. If the user specifies a localization key in
     *         "titleid", the corresponding localized text from the locale file
     *         is used for "title."
     * 
     *         Subclasses should override this method to return a table mapping
     *         localizable properties to the corresponding localization key
     *         properties.
     * 
     *         For example, if the proxy has two properties, "title" and "text",
     *         and the corresponding localization key properties are "titleid"
     *         and "textid", this might look like: </br>
     * 
     *         <pre>
     * <code>protected KrollDict getLangConversionTable()
     * {
     * 	KrollDict table = new KrollDict();
     * 	table.put("title", "titleid");
     * 	table.put("text", "textid");
     * 	return table;
     * }
     * </pre>
     * 
     *         </code>
     * 
     * @module.api
     * 
     */
    protected KrollDict getLangConversionTable() {
        return null;
    }

    /**
     * Handles initialization of the proxy's locale string properties.
     * 
     * @see #getLangConversionTable()
     */
    private void handleLocaleProperties() {
        if (langConversionTable == null) {
            return;
        }

        /*
         * Iterate through the language conversion table. This table maps target
         * properties to their locale lookup property. Example: title -> titleid
         * 
         * The lookup identifier stored in the locale property (titleid) will be
         * used to query the locale strings file to get the localized value.
         * This localized value will be set to the targeted property (title).
         */
        synchronized (properties) {
            for (Map.Entry<String, Object> entry : langConversionTable
                    .entrySet()) {
                // Get the lookup identifier stored in the locale property.
                String localeProperty = entry.getValue().toString();
                String lookupId = properties.getString(localeProperty);
                if (lookupId == null) {
                    // If no locale lookup identifier is provided, skip this
                    // entry.
                    continue;
                }

                // Lookup the localized string from the locale file.
                String localizedValue = getLocalizedText(lookupId);
                if (localizedValue == null) {
                    // If there is no localized value for this identifier,
                    // log a warning and skip over the entry.
                    Log.w(TAG, "No localized string found for identifier: "
                            + lookupId);
                    continue;
                }

                // Set the localized value to the targeted property.
                String targetProperty = entry.getKey();
                setProperty(targetProperty, localizedValue);
            }
        }

    }

    /**
     * Updates the lookup identifier value of a locale property. This will also
     * update the targeted value with the string found using the new lookup
     * identifier.
     * 
     * @param localeProperty
     *            name of the locale property (example: titleid)
     * @param newLookupId
     *            the new lookup identifier
     * @return a pair containing the name of the target property which was
     *         updated and the new value set on it.
     */
    public Pair<String, String> updateLocaleProperty(String localeProperty,
            String newLookupId) {
        if (langConversionTable == null) {
            return null;
        }

        synchronized (properties) {
            properties.put(localeProperty, newLookupId);
        }

        // Determine which localized property this locale property updates.
        for (Map.Entry<String, Object> entry : langConversionTable.entrySet()) {
            if (entry.getValue().toString().equals(localeProperty)) {
                String targetProperty = entry.getKey();
                String localizedValue = getLocalizedText(newLookupId);
                if (localizedValue == null) {
                    return null;
                }
                setProperty(targetProperty, localizedValue);

                return Pair.create(targetProperty, localizedValue);
            }
        }

        // If we reach this point, the provided locale property is not valid.
        return null;
    }

    /**
     * Return true if the given property is a locale property.
     * 
     * @param propertyName
     *            name of the property to check (ex: titleid)
     * @return true if this property is a locale property
     */
    public boolean isLocaleProperty(String propertyName) {
        return propertyName.endsWith("id");
    }

    /**
     * Looks up a localized string given an identifier.
     * 
     * @param lookupId
     *            the identifier of the localized value to look up.
     * @return the localized string if found, otherwise null.
     */
    private String getLocalizedText(String lookupId) {
        try {
            int resid = TiRHelper.getResource("string." + lookupId);
            if (resid != 0) {
                return getActivity().getString(resid);
            }
            return null;

        } catch (TiRHelper.ResourceNotFoundException e) {
            return null;
        }
    }

    /**
     * Handles the creation {@link KrollDict} passed into the create method for
     * this proxy. This is usually the first (and sometimes only) argument to
     * the proxy's create method.
     * 
     * To set default property values, add them to the
     * {@link KrollProxy#defaultValues map}
     * 
     * @param dict
     * @module.api
     */
    public void handleCreationDict(KrollDict dict) {
        synchronized (properties) {
            properties.clear();
        }
        if (dict == null) {
            return;
        }

        synchronized (properties) {
            properties.putAll(dict);
        }
        handleDefaultValues();
        handleLocaleProperties();

        if (dict.containsKey(TiC.PROPERTY_BUBBLE_PARENT)) {
            bubbleParent = TiConvert.toBoolean(dict,
                    TiC.PROPERTY_BUBBLE_PARENT, true);
            bubbleParentDefined = true;
            dict.remove(TiC.PROPERTY_BUBBLE_PARENT);
        }

        if (dict.containsKey(TiC.PROPERTY_SYNCEVENTS)) {
            setSyncEvents(TiConvert.toStringArray(dict
                    .remove(TiC.PROPERTY_SYNCEVENTS)));
        }

		if (dict.containsKey(TiC.PROPERTY_LIFECYCLE_CONTAINER)) {
			KrollProxy lifecycleProxy = (KrollProxy) dict.get(TiC.PROPERTY_LIFECYCLE_CONTAINER);
			if (lifecycleProxy instanceof TiWindowProxy) {
				ActivityProxy activityProxy = ((TiWindowProxy) lifecycleProxy).getWindowActivityProxy();
				if (activityProxy != null) {
					attachActivityLifecycle(activityProxy.getActivity());
				} else {
					((TiWindowProxy) lifecycleProxy).addProxyWaitingForActivity(this);
				}
			} else {
				Log.e(TAG, TiC.PROPERTY_LIFECYCLE_CONTAINER + " must be a WindowProxy or TabGroupProxy (TiWindowProxy)");
			}
		}

        if (modelListener != null) {
            modelListener.get().processProperties(dict);
        }
    }

    public Handler getMainHandler() {
        if (mainHandler == null) {
            mainHandler = new Handler(TiMessenger.getMainMessenger()
                    .getLooper(), this);
        }

        return mainHandler;
    }

    public Handler getRuntimeHandler() {
        if (runtimeHandler == null) {
            runtimeHandler = new Handler(TiMessenger.getRuntimeMessenger()
                    .getLooper(), this);
        }

        return runtimeHandler;
    }

    public void setKrollObject(KrollObject object) {
        this.krollObject = object;
    }

    /**
     * @return the KrollObject associated with this proxy if it exists.
     *         Otherwise create it in the KrollRuntime thread.
     * @module.api
     */
    public KrollObject getKrollObject() {
        if (krollObject == null) {
            if (KrollRuntime.getInstance().isRuntimeThread()) {
                initKrollObject();

            } else {
                TiMessenger.sendBlockingRuntimeMessage(getRuntimeHandler()
                        .obtainMessage(MSG_INIT_KROLL_OBJECT));
            }
        }

        return krollObject;
    }

    public void initKrollObject() {
        KrollRuntime.getInstance().initObject(this);
    }

    /**
     * @return the absolute URL of the location in code where the proxy was
     *         created in Javascript.
     * @module.api
     */
    public TiUrl getCreationUrl() {
        return creationUrl;
    }

    @Kroll.method
    public void setCreationUrl(String url) {
        creationUrl = TiUrl.createProxyUrl(url);
    }

    // native extending support allows us to whole-sale apply properties and
    // only fire one event / job
    @Kroll.method
    public void extend(KrollDict options) {
        if (options == null || options.isEmpty()) {
            return;
        }
        applyPropertiesInternal(options, false, true);
    }

    private void firePropertiesChanged(Object[][] changes) {
        if (modelListener == null) {
            return;
        }

        int changesLength = changes.length;
        // if (modelListener != null) {
        // modelListener.propertyChanged((String) name, change[INDEX_OLD_VALUE],
        // change[INDEX_VALUE], this);
        // }
        for (int i = 0; i < changesLength; ++i) {
            Object[] change = changes[i];
            if (change.length != 3) {
                continue;
            }

            Object name = change[INDEX_NAME];
            if (name == null || !(name instanceof String)) {
                continue;
            }

            if (modelListener != null) {
                modelListener.get().propertyChanged((String) name,
                        change[INDEX_OLD_VALUE], change[INDEX_VALUE], this);
            }
        }
        // if (modelListener != null) {
        // modelListener.propertyChanged((String) name, change[INDEX_OLD_VALUE],
        // change[INDEX_VALUE], this);
        // }
    }

    public Object getIndexedProperty(int index) {
        // TODO(josh): return undefined value
        return 0;
    }

    public void setIndexedProperty(int index, Object value) {
        // no-op
    }

    /**
     * @param name
     *            the lookup key.
     * @return true if the proxy contains this property, false otherwise.
     * @module.api
     */
    public boolean hasProperty(String name) {
        synchronized (properties) {
            return properties.containsKey(name);
        }
    }

    /**
     * Returns the property value given its key. Properties are cached on the
     * Proxy and updated from JS for relevant annotated APIs
     * 
     * @param name
     *            the lookup key.
     * @return the property object or null if a property for the given key does
     *         not exist.
     * @module.api
     */
    public Object getProperty(String name) {
        synchronized (properties) {
            return properties.get(name);
        }
    }
    
    public Object getProperty(String name, boolean lookJS) {
        synchronized (properties) {
            Object value = properties.get(name);
            if (!lookJS || value != null) {
                return value;
            }
            return getJsPropertySync(name);
        }
    }

    /**
     * Returns the property value given its key. Properties are cached on the
     * Proxy and updated from JS for relevant annotated APIs
     * 
     * @param name
     *            the lookup key.
     * @return the property object or null if a property for the given key does
     *         not exist.
     * @module.api
     */
    public Object getProperty(String name, Object defaultValue) {
        synchronized (properties) {
            if (properties.containsKey(name)) {
                return properties.get(name);
            }
        }
        return defaultValue;
    }

    /**
     * @deprecated use setPropertyAndFire instead
     */
    @Deprecated
    public void setProperty(String name, Object value, boolean fireChange) {
        if (!fireChange) {
            setProperty(name, value);

        } else {
            setPropertyAndFire(name, value);
        }
    }

    public void propagateSetProperty(String name, Object value) {
        if (setPropertyListener != null) {
            setPropertyListener.onSetProperty(this, name, value);
        }
    }

    /**
     * This sets the named property WITHOUT updating the actual JS object.
     * 
     * @module.api
     */
    public void setPropertyJava(String name, Object value) {
        synchronized (properties) {
            properties.put(name, value);
        }
    }

    /**
     * This sets the named property as well as updating the actual JS object.
     * 
     * @module.api
     */
    public void setProperty(String name, Object value) {
        synchronized (properties) {
            properties.put(name, value);
        }

        // That line is for listitemproxy to update its data
        propagateSetProperty(name, value);

        if (KrollRuntime.getInstance().isRuntimeThread()) {
            doSetProperty(name, value);

        } else {
            Message message = getRuntimeHandler().obtainMessage(
                    MSG_SET_PROPERTY, value);
            message.getData().putString(PROPERTY_NAME, name);
            message.sendToTarget();
        }
    }
    
    public void updateKrollObjectProperties() {
        updateKrollObjectProperties(getProperties());
        propertiesToUpdateNativeSide = null;
    }

    public void updateKrollObjectProperties(HashMap<String, Object> props) {
        updateKrollObjectProperties(props, true);
    }
    
    public void updateKrollObjectProperties(HashMap<String, Object> props, final boolean wait) {
        if (KrollRuntime.getInstance().isRuntimeThread()) {
            doUpdateKrollObjectProperties(props);

        } else {
            if (wait) {
                Message msg = getRuntimeHandler().obtainMessage(
                        MSG_UPDATE_KROLL_PROPERTIES);
                TiMessenger.sendBlockingRuntimeMessage(msg, props);
            } else {
                Message message = getRuntimeHandler().obtainMessage(
                        MSG_UPDATE_KROLL_PROPERTIES, props);
                message.sendToTarget();
            }
        }
    }

    /**
     * This sets the named property as well as updating the actual JS object.
     * 
     * @module.api
     */
    public void setProperties(KrollDict newProps) {
        KrollDict realProperties = (newProps != null) ? new KrollDict(newProps)
                : new KrollDict();
        synchronized (properties) {
            for (String key : properties.keySet()) {
                if (!realProperties.containsKey(key)) {
                    realProperties.put(key, null);
                }
            }
        }

        KrollDict changedProps = new KrollDict();
        for (Object key : realProperties.keySet()) {
            String name = TiConvert.toString(key);
            Object value = realProperties.get(key);
            Object current = getProperty(name);
            if (shouldFireChange(current, value)) {
                changedProps.put(name, value);
                onPropertyChanged(name, value, current);
            }
        }

        synchronized (properties) {
            properties.clear();
            if (newProps != null)
                properties.putAll(newProps);
        }

        if (modelListener != null) {
            if (!mProcessInUIThread || TiApplication.isUIThread()) {
                modelListener.get().processApplyProperties(changedProps);
            } else {
                Message message = getMainHandler().obtainMessage(
                        MSG_MODEL_APPLY_PROPERTIES, changedProps);
                message.sendToTarget();
            }
        }
    }

    public class KrollPropertyChangeSet extends KrollPropertyChange {
        public int entryCount;
        public String[] keys;
        public Object[] oldValues;
        public Object[] newValues;

        public KrollPropertyChangeSet(int capacity) {
            super(null, null, null);
            entryCount = 0;
            keys = new String[capacity];
            oldValues = new Object[capacity];
            newValues = new Object[capacity];
        }

        public void addChange(String key, Object oldValue, Object newValue) {
            keys[entryCount] = key;
            oldValues[entryCount] = oldValue;
            newValues[entryCount] = newValue;
            entryCount++;
        }

        public void fireEvent(KrollProxy proxy, KrollProxyListener listener) {
            if (listener == null) {
                return;
            }
            for (int i = 0; i < entryCount; i++) {
                listener.propertyChanged(keys[i], oldValues[i], newValues[i],
                        proxy);
            }
        }
    }

    public void applyPropertiesInternal(Object arg, boolean force, boolean wait) {
        if (!(arg instanceof HashMap)) {
            Log.w(TAG, "Cannot apply properties: invalid type for properties",
                    Log.DEBUG_MODE);
            return;
        }
        HashMap<String, Object> props = (HashMap<String, Object>) arg;
        KrollDict changedProps = new KrollDict();
        for (Map.Entry<String, Object> entry : props.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();
            Object current = getProperty(name);
            if (current instanceof KrollProxy && value instanceof HashMap) {
                // we handle binded objects (same as done with listitems)
                ((KrollProxy) current).applyPropertiesInternal(value, force,
                        wait);
            } else {
                if (name.equals(TiC.PROPERTY_BUBBLE_PARENT)) {
                    bubbleParent = TiConvert.toBoolean(value, true);
                    bubbleParentDefined = true;
                } else if (name.equals(TiC.PROPERTY_SYNCEVENTS)) {
                    setSyncEvents(TiConvert.toStringArray(value));
                } else if (force || shouldFireChange(current, value)) {
                    setProperty(name, value);
                    changedProps.put(name, value);
                    onPropertyChanged(name, value, current);
                }
            }
        }
        if (modelListener != null) {
            if (!mProcessInUIThread || TiApplication.isUIThread()) {
                modelListener.get().processApplyProperties(changedProps);
            } else {
                if (wait) {
                    TiMessenger.sendBlockingMainMessage(getMainHandler()
                            .obtainMessage(MSG_MODEL_APPLY_PROPERTIES),
                            changedProps);
                } else {
                    Message message = getMainHandler().obtainMessage(
                            MSG_MODEL_APPLY_PROPERTIES, changedProps);
                    message.sendToTarget();
                }
            }
        }
        updateKrollObjectProperties(props, wait);
    }

    public void applyPropertiesInternal(Object arg, boolean force) {
        applyPropertiesInternal(arg, force, false);
    }

    @Kroll.method
    public void applyProperties(Object arg, @Kroll.argument(optional = true) Object options) {
        boolean wait = false;
        if (options instanceof HashMap) {
            wait = TiConvert.toBoolean((HashMap) options, "wait", wait);
        }
        applyPropertiesInternal(arg, false, wait);
    }
    
    public void applyProperties(Object arg) {
        applyProperties(arg, null);
    }

    /**
     * Asynchronously calls a function referenced by a property on this object.
     * This may be called safely on any thread.
     * 
     * @see KrollObject#callProperty(String, Object[])
     * @param name
     *            the property that references the function
     * @param args
     *            the arguments to pass when calling the function.
     */
    public void callPropertyAsync(String name, Object[] args) {
        Message msg = getRuntimeHandler().obtainMessage(
                MSG_CALL_PROPERTY_ASYNC, args);
        msg.getData().putString(PROPERTY_NAME, name);
        msg.sendToTarget();
    }

    /**
     * Synchronously calls a function referenced by a property on this object.
     * This may be called safely on any thread.
     * 
     * @see KrollObject#callProperty(String, Object[])
     * @param name
     *            the property that references the function
     * @param args
     *            the arguments to pass when calling the function.
     */
    public void callPropertySync(String name, Object[] args) {
        if (KrollRuntime.getInstance().isRuntimeThread()) {
            getKrollObject().callProperty(name, args);
        } else {
            Message msg = getRuntimeHandler().obtainMessage(
                    MSG_CALL_PROPERTY_SYNC);
            msg.getData().putString(PROPERTY_NAME, name);
            TiMessenger.sendBlockingRuntimeMessage(msg, args);
        }
    }
    
    public Object getJsPropertySync(String name) {
        if (KrollRuntime.getInstance().isRuntimeThread()) {
            return getKrollObject().getProperty(name);
        } else {
            Message msg = getRuntimeHandler().obtainMessage(
                    MSG_GET_PROPERTY);
            msg.getData().putString(PROPERTY_NAME, name);
            return TiMessenger.sendBlockingRuntimeMessage(msg);
        }
    }

    protected void doSetProperty(String name, Object value) {
        getKrollObject().setProperty(name, value);
    }

    protected void doUpdateKrollObjectProperties() {
        getKrollObject().updateNativeProperties(getProperties());
    }

    protected void doUpdateKrollObjectProperties(HashMap<String, Object> props) {
        getKrollObject().updateNativeProperties(props);
    }

    @Kroll.getProperty
    @Kroll.method
    public boolean getBubbleParent() {
        return bubbleParent;
    }

    @Kroll.setProperty
    @Kroll.method
    public void setBubbleParent(Object value) {
        bubbleParent = TiConvert.toBoolean(value);
    }

    /**
     * Fires an event asynchronously via KrollRuntime thread, which can be
     * intercepted on JS side.
     * 
     * @param event
     *            the event to be fired.
     * @param data
     *            the data to be sent.
     * @return whether this proxy has an eventListener for this event.
     * @module.api
     */
    public boolean fireEvent(String event) {
        return fireEvent(event, null, false, true);
    }

    /**
     * Fires an event asynchronously via KrollRuntime thread, which can be
     * intercepted on JS side.
     * 
     * @param event
     *            the event to be fired.
     * @param data
     *            the data to be sent.
     * @return whether this proxy has an eventListener for this event.
     * @module.api
     */
    @Kroll.method
    public boolean fireEvent(String event,
            @Kroll.argument(optional = true) Object data) {
        return fireEvent(event, data, false, true);
    }

    /**
     * Send an event to the view who is next to receive the event.
     * 
     * @param eventName
     *            event to send to the next view
     * @param data
     *            the data to include in the event
     * @return true if the event was handled
     */
    @Kroll.method(name = "_addEvaluator")
    public int addEvaluator(String eventName, Object data) {
        if (eventName == null) {
            throw new IllegalStateException(
                    "addEvaluator expects a non-null eventName");

        } else if (!(data instanceof HashMap)) {
            throw new IllegalStateException(
                    "addEvaluator expects a non-null listener");
        }
        
        if(evaluators==null){
            evaluators = new HashMap<String, List<KrollDict>>();
            setProperty(PROPERTY_HAS_JAVA_LISTENER, true);
        }

        synchronized (evaluators) {
            List<KrollDict> theListeners = evaluators.get(eventName);
            if (theListeners == null) {
                theListeners = new ArrayList< KrollDict>();
                evaluators.put(eventName, theListeners);
            }
            theListeners.add(TiConvert.toKrollDict(data));
            return theListeners.size();
        }
    }
    
    @Kroll.method(name = "_removeEvaluator")
    public void removeEvaluator(String eventName, Object data) {
        if (eventName == null || !(data instanceof HashMap) || evaluators == null) {
            return;
        }
        synchronized (evaluators) {
            List<KrollDict> theListeners = evaluators.get(eventName);
            
            if (theListeners != null) {
                for (KrollDict hashMap : theListeners) {
                    if (hashMap.equals(data)) {
                        theListeners.remove(hashMap);
                        break;
                    }
                }
                if (theListeners.isEmpty()) {
                    evaluators.remove(eventName);
                }
                if (evaluators.isEmpty()) {
                    setProperty(PROPERTY_HAS_JAVA_LISTENER, false);
                }
            }
        }
    }
    
    /**
     * Send an event to the view who is next to receive the event.
     * 
     * @param eventName
     *            event to send to the next view
     * @param data
     *            the data to include in the event
     * @return true if the event was handled
     */
    @Kroll.method(name = "_fireEventToParent")
    public boolean fireEventToParent(String eventName, Object data) {
        if (bubbleParent) {
            KrollProxy parentProxy = getParentForBubbling();
            if (parentProxy != null) {
                return parentProxy.fireEvent(eventName, data, true, false);
            }
        }
        return false;
    }

    /**
     * Fires an event synchronously via KrollRuntime thread, which can be
     * intercepted on JS side.
     * 
     * @param event
     *            the event to be fired.
     * @param data
     *            the data to be sent.
     * @return whether this proxy has an eventListener for this event.
     * @module.api
     */
    public boolean fireSyncEvent(String event, Object data) {
        return fireSyncEvent(event, data, false);
    }

    public boolean fireSyncEvent(String event, Object data, final boolean bubble) {
        return fireSyncEvent(event, data, false, true);

    }

    /**
     * Fires an event synchronously via KrollRuntime thread, which can be
     * intercepted on JS side.
     * 
     * @param event
     *            the event to be fired.
     * @param data
     *            the data to be sent.
     * @return whether this proxy has an eventListener for this event.
     * @module.api
     */
    public boolean fireSyncEvent(String event, Object data,
            final boolean bubble, boolean checkListeners) {
        if (checkListeners && !hasListeners(event, bubble)) {
            return false;
        }
        if (KrollRuntime.getInstance().isRuntimeThread()) {
            return doFireEvent(event, data, bubble);

        } else {
            Message message = getRuntimeHandler().obtainMessage(
                    MSG_FIRE_SYNC_EVENT);
            message.getData().putString(PROPERTY_NAME, event);
            message.getData().putBoolean(PROPERTY_BUBBLES, bubble);

            return (Boolean) TiMessenger.sendBlockingRuntimeMessage(message,
                    data);
        }
    }

    /**
     * Fires an event synchronously via KrollRuntime thread, which can be
     * intercepted on JS side.
     * 
     * @param event
     *            the event to be fired.
     * @param data
     *            the data to be sent.
     * @param maxTimeout
     *            the maximum time to wait for the result to return, in the unit
     *            of milliseconds.
     * @return whether this proxy has an eventListener for this event.
     * @module.api
     */
    public boolean fireSyncEvent(String event, Object data, long maxTimeout) {
        if (!hasListeners(event, true)) {
            return false;
        }
        if (KrollRuntime.getInstance().isRuntimeThread()) {
            return doFireEvent(event, data);

        } else {
            Message message = getRuntimeHandler().obtainMessage(
                    MSG_FIRE_SYNC_EVENT);
            message.getData().putString(PROPERTY_NAME, event);

            Object result = TiMessenger.sendBlockingRuntimeMessage(message,
                    data, maxTimeout);
            return TiConvert.toBoolean(result, false);
        }
    }

    /**
     * Fires an event that can optionally be "bubbled" to the parent view.
     * 
     * @param eventName
     *            event to get dispatched to listeners
     * @param data
     *            data to include in the event
     * @param bubbles
     *            if true will send the event to the parent view after it has
     *            been dispatched to this view's listeners.
     * @return true if the event was handled
     */
    public boolean fireEvent(String event, Object data, boolean bubbles) {
        return fireEvent(event, data, bubbles, true);
    }

    /**
     * Fires an event asynchronously via KrollRuntime thread, which can be
     * intercepted on JS side.
     * 
     * @param event
     *            the event to be fired.
     * @param data
     *            the data to be sent.
     * @param bubbles
     *            should bubble to parent.
     * @param checkListeners
     *            should check for Listeners. Optimisation if the check was
     *            already done
     * @return whether the message is sent.
     * @module.api
     */
    public boolean fireEvent(String event, Object data, boolean bubbles,
            boolean checkListeners) {
        if (mSyncEvents != null && mSyncEvents.contains(event)) {
            return fireSyncEvent(event, data, bubbles, checkListeners);
        }
        if (checkListeners && !hasListeners(event, bubbles)) {
            return false;
        }
        if (bubbleParentDefined) {
            bubbles = bubbleParent;
        }
        if (eventOverrideDelegate != null) {
            data = eventOverrideDelegate.get().overrideEvent(data, event, this);
        }
        HashMap<String, Object> dict = (HashMap) data;
        Object sourceProxy = this;
        if (dict == null) {
            data = dict = new KrollDict();
            dict.put(TiC.EVENT_PROPERTY_SOURCE, sourceProxy);
        } else if (dict instanceof HashMap) {
            sourceProxy = dict.get(TiC.EVENT_PROPERTY_SOURCE);
            if (sourceProxy == null) {
                dict.put(TiC.EVENT_PROPERTY_SOURCE, sourceProxy);
            }
        }
        if (sourceProxy instanceof KrollProxy) {
            Object bindId = ((KrollProxy) sourceProxy).getProperty(TiC.PROPERTY_BIND_ID);
            if (bindId != null) {
                dict.put(TiC.PROPERTY_BIND_ID, bindId);
            }
        }
        
        dict.put(TiC.EVENT_PROPERTY_TYPE, event);
        if (evaluators != null && data instanceof HashMap) {
            List<KrollDict> theListeners = null;
            synchronized (evaluators) {
                theListeners = evaluators.get(event);
            }
            if (theListeners != null) {
                for (KrollDict hashMap : theListeners) {
                    TiUIHelper.applyMathDict(hashMap, TiConvert.toKrollDict(data), this);
                }
            }
        }
        
        
        Message message = getRuntimeHandler().obtainMessage(MSG_FIRE_EVENT,
                data);
        message.getData().putString(PROPERTY_NAME, event);
        message.getData().putBoolean(PROPERTY_BUBBLES, bubbles);
        message.sendToTarget();
        return true;
    }

    public boolean doFireEvent(String event, Object data, boolean bubbles) {
        boolean reportSuccess = false;
        int code = 0;
        KrollObject source = null;
        String message = null;

        KrollDict krollData = null;

        /* TODO: Is eventListeners still used? */
        if (!eventListeners.isEmpty()) {
//            HashMap<String, Object> dict = (HashMap) data;
//            if (dict == null) {
//                dict = new KrollDict();
//                dict.put(TiC.EVENT_PROPERTY_SOURCE, this);
//            } else if (dict instanceof HashMap) {
//                Object sourceProxy = dict.get(TiC.EVENT_PROPERTY_SOURCE);
//                if (sourceProxy == null) {
//                    dict.put(TiC.EVENT_PROPERTY_SOURCE, this);
//                }
//            }
//            dict.put(TiC.EVENT_PROPERTY_TYPE, event);
            // onEventFired(event, dict);
            HashMap<Integer, KrollEventCallback> listeners = eventListeners
                    .get(event);
            if (listeners != null) {
                for (Integer listenerId : listeners.keySet()) {
                    KrollEventCallback callback = listeners.get(listenerId);
                    if (callback != null) {
                        callback.call(data);
                    }
                }
            }
        }

        if (data != null) {
            if (data instanceof KrollDict) {
                krollData = (KrollDict) data;
            } else if (data instanceof HashMap) {
                try {
                    krollData = new KrollDict((HashMap) data);
                } catch (Exception e) {
                }
            } else if (data instanceof JSONObject) {
                try {
                    krollData = new KrollDict((JSONObject) data);
                } catch (Exception e) {
                }
            }
        }

        if (krollData != null) {
            Object hashValue = krollData.get(TiC.PROPERTY_BUBBLES);
            if (hashValue != null) {
                bubbles &= TiConvert.toBoolean(hashValue);
                krollData.remove(TiC.PROPERTY_BUBBLES);
            }
            hashValue = krollData.get(TiC.PROPERTY_SUCCESS);
            if (hashValue instanceof Boolean) {
                boolean successValue = ((Boolean) hashValue).booleanValue();
                hashValue = krollData.get(TiC.PROPERTY_CODE);
                if (hashValue instanceof Integer) {
                    int codeValue = ((Integer) hashValue).intValue();
                    if (successValue == (codeValue == 0)) {
                        reportSuccess = true;
                        code = codeValue;
                        krollData.remove(TiC.PROPERTY_SUCCESS);
                        krollData.remove(TiC.PROPERTY_CODE);
                    } else {
                        Log.w(TAG,
                                "DEPRECATION WARNING: Events with 'code' and 'success' should have success be true if and only if code is nonzero. For java modules, consider the putCodeAndMessage() method to do this for you. The capability to use other types will be removed in a future version.",
                                Log.DEBUG_MODE);
                    }
                } else if (successValue) {
                    Log.w(TAG,
                            "DEPRECATION WARNING: Events with 'success' of true should have an integer 'code' property that is 0. For java modules, consider the putCodeAndMessage() method to do this for you. The capability to use other types will be removed in a future version.",
                            Log.DEBUG_MODE);
                } else {
                    Log.w(TAG,
                            "DEPRECATION WARNING: Events with 'success' of false should have an integer 'code' property that is nonzero. For java modules, consider the putCodeAndMessage() method to do this for you. The capability to use other types will be removed in a future version.",
                            Log.DEBUG_MODE);
                }
            } else if (hashValue != null) {
                Log.w(TAG,
                        "DEPRECATION WARNING: The 'success' event property is reserved to be a boolean. For java modules, consider the putCodeAndMessage() method to do this for you. The capability to use other types will be removed in a future version.",
                        Log.DEBUG_MODE);
            }
            hashValue = krollData.get(TiC.EVENT_PROPERTY_ERROR);
            if (hashValue instanceof String) {
                message = (String) hashValue;
                krollData.remove(TiC.EVENT_PROPERTY_ERROR);
            } else if (hashValue != null) {
                Log.w(TAG,
                        "DEPRECATION WARNING: The 'error' event property is reserved to be a string. For java modules, consider the putCodeAndMessage() method to do this for you. The capability to use other types will be removed in a future version.",
                        Log.DEBUG_MODE);
            }
            hashValue = krollData.get(TiC.EVENT_PROPERTY_SOURCE);
            if (hashValue instanceof KrollProxy) {
                if (hashValue != this) {
                    source = ((KrollProxy) hashValue).getKrollObject();
                }
                krollData.remove(TiC.EVENT_PROPERTY_SOURCE);
            }
            if (krollData.size() == 0) {
                krollData = null;
            }
        }

        return getKrollObject().fireEvent(source, event, krollData, bubbles,
                reportSuccess, code, message);
    }

    public boolean doFireEvent(String event, Object data) {
        return doFireEvent(event, data, true);
    }

    public void firePropertyChanged(String name, Object oldValue,
            Object newValue) {
        onPropertyChanged(name, newValue, oldValue);
        if (modelListener != null) {
            if (!mProcessInUIThread || TiApplication.isUIThread()) {
                modelListener.get().propertyChanged(name, oldValue, newValue, this);

            } else {
                KrollPropertyChange pch = new KrollPropertyChange(name,
                        oldValue, newValue);
                getMainHandler().obtainMessage(MSG_MODEL_PROPERTY_CHANGE, pch)
                        .sendToTarget();
            }
        }
    }

    public void onHasListenersChanged(String event, boolean hasListeners) {
        Message msg = getMainHandler().obtainMessage(
                hasListeners ? MSG_LISTENER_ADDED : MSG_LISTENER_REMOVED);
        msg.obj = event;
        TiMessenger.getMainMessenger().sendMessage(msg);
    }

    /**
     * @param event
     *            the event to check
     * @return whether the associated KrollObject has an event listener for the
     *         passed in event.
     * @module.api
     */
    public boolean hasListeners(String event) {
        return hasListeners(event, true);
    }

    public boolean _hasListeners(String event) {
        return hasNonJSEventListener(event)
                || hasEvaluatorListener(event)
                || getKrollObject().hasListeners(event);
    }

    /**
     * @param event
     *            the event to check
     * @return whether the associated KrollObject has an event listener for the
     *         passed in event.
     * @module.api
     */
    public boolean hasListeners(String event, boolean checkParent) {
        boolean hasListener = _hasListeners(event);
        if (bubbleParentDefined) {
            checkParent = bubbleParent;
        }
        // Checks whether the parent has the listener or not
        if (checkParent && !hasListener) {
            KrollProxy parentProxy = getParentForBubbling();
            if (parentProxy != null && bubbleParent) {
                return parentProxy.hasListeners(event, true);
            }
        }

        return hasListener;
    }

    /**
     * Returns true if any view in the hierarchy has the event listener.
     */
    public boolean hierarchyHasListener(String event) {
        return hasListeners(event, true);
    }

    /**
     * Returns true if any view in the hierarchy has the event listener.
     */
    public KrollProxy firstHierarchyListener(String event) {
        boolean hasListener = hasListeners(event);

        // Checks whether the parent has the listener or not
        if (!hasListener) {
            KrollProxy parentProxy = getParentForBubbling();
            if (parentProxy != null && bubbleParent) {
                return parentProxy.firstHierarchyListener(event);
            }
            return null;
        }

        return this;
    }

    public boolean shouldFireChange(Object oldValue, Object newValue) {
        return (oldValue != null || newValue != null) && 
                ((oldValue == null || newValue == null) || (!oldValue.equals(newValue)));
    }

    /**
     * Same behavior as {@link #setProperty(String, Object)}, but also invokes
     * {@link KrollProxyListener#propertyChanged(String, Object, Object, KrollProxy)}
     * .
     * 
     * @param name
     *            the property name.
     * @param value
     *            the property value.
     * @module.api
     */
    public void setPropertyAndFire(String name, Object value) {
        Object current = getProperty(name);

        if (shouldFireChange(current, value)) {
            setProperty(name, value);
            firePropertyChanged(name, current, value);
        }
    }

    public void setPropertyAndForceFire(String name, Object value) {
        Object current = getProperty(name);
        setProperty(name, value);

        firePropertyChanged(name, current, value);
    }
    
    public void onPropertyChanged(String name, Object value, Object oldValue) {
        // to be overriden
    }

    public void onPropertyChanged(String name, Object value) {
        String propertyName = name;
        Object newValue = value;

        if (name.equals(TiC.PROPERTY_BUBBLE_PARENT)) {
            bubbleParent = TiConvert.toBoolean(value, true);
            bubbleParentDefined = true;
            return;
        } else if (name.equals(TiC.PROPERTY_SYNCEVENTS)) {
            setSyncEvents(TiConvert.toStringArray(value));
            return;
        } else if (isLocaleProperty(name)) {
            Log.i(TAG, "Updating locale: " + name, Log.DEBUG_MODE);
            Pair<String, String> update = updateLocaleProperty(name,
                    TiConvert.toString(value));
            if (update != null) {
                propertyName = update.first;
                newValue = update.second;
            }
        }
        Object oldValue;
        synchronized (properties) {
            oldValue = properties.get(propertyName);
            properties.put(propertyName, newValue);
        }
        firePropertyChanged(propertyName, oldValue, newValue);
    }

    public void onPropertiesChanged(Object[][] changes) {
        int changesLength = changes.length;
        boolean isUiThread = !mProcessInUIThread || TiApplication.isUIThread();

        for (int i = 0; i < changesLength; ++i) {
            Object[] change = changes[i];
            if (change.length != 3) {
                continue;
            }

            Object name = change[INDEX_NAME];
            if (name == null || !(name instanceof String)) {
                continue;
            }

            String nameString = (String) name;
            Object value = change[INDEX_VALUE];

            synchronized (properties) {
                properties.put(nameString, change[INDEX_VALUE]);
            }
            if (isUiThread && modelListener != null) {
                modelListener.get().propertyChanged(nameString,
                        change[INDEX_OLD_VALUE], value, this);
            }
        }

        if (isUiThread || modelListener == null) {
            return;
        }

        Message message = getMainHandler().obtainMessage(
                MSG_MODEL_PROPERTIES_CHANGED, changes);
        message.sendToTarget();
    }

    public ActivityProxy getActivityProxy() {
        Activity activity = getActivity();
        if (activity instanceof TiBaseActivity) {
            return ((TiBaseActivity) activity).getActivityProxy();
        }

        return null;
    }

    /**
     * Returns proxy that should receive the event next in a case of bubbling.
     * Return null if the class does not bubble or there is no parent.
     * Optionally return null if the "bubbleParent" property is false -- i.e.,
     * bubbleParent must be checked as well.
     * 
     * @return proxy which is next to receive events
     */
    public KrollProxy getParentForBubbling() {
        return null;
    }

    /**
     * Returns a KrollDict object that contains all current properties
     * associated with this proxy.
     * 
     * @return KrollDict properties object.
     * @module.api
     */
    public KrollDict getProperties() {
        return properties;
    }
    
    public KrollDict getClonedProperties() {
        KrollDict props = null;
        synchronized (properties) {
            props = (KrollDict) properties.clone();
        }
        return props;
    }

    /**
     * @return the KrollModule that this proxy was created in.
     */
    public KrollModule getCreatedInModule() {
        return createdInModule;
    }

    public boolean handleMessage(Message msg) {
        switch (msg.what) {
        case MSG_MODEL_PROPERTY_CHANGE: {
            if (modelListener != null) {
                ((KrollPropertyChange) msg.obj).fireEvent(this, modelListener.get());
            }

            return true;
        }
        case MSG_LISTENER_ADDED:
        case MSG_LISTENER_REMOVED: {
            if (modelListener == null) {
                return true;
            }

            String event = (String) msg.obj;

            if (msg.what == MSG_LISTENER_ADDED) {
                eventListenerAdded(event, 1, this);

            } else {
                eventListenerRemoved(event, 0, this);
            }

            return true;
        }
        case MSG_MODEL_PROCESS_PROPERTIES: {
            if (modelListener != null) {
                modelListener.get().processProperties(getClonedProperties());
            }
            return true;
        }
        case MSG_MODEL_APPLY_PROPERTIES: {
            if (modelListener != null) {
                if (msg.obj instanceof AsyncResult) {
                    AsyncResult result = (AsyncResult) msg.obj;
                    modelListener
                            .get().processApplyProperties((KrollDict) result.getArg());
                    result.setResult(null);
                    return true;

                } else {
                    modelListener.get().processApplyProperties((KrollDict) msg.obj);
                }
            }
            return true;
        }
        case MSG_MODEL_PROPERTIES_CHANGED: {
            firePropertiesChanged((Object[][]) msg.obj);

            return true;
        }
        case MSG_INIT_KROLL_OBJECT: {
            initKrollObject();
            ((AsyncResult) msg.obj).setResult(null);

            return true;
        }
        case MSG_SET_PROPERTY: {
            Object value = msg.obj;
            String property = msg.getData().getString(PROPERTY_NAME);
            doSetProperty(property, value);

            return true;
        }
        case MSG_GET_PROPERTY: {
            AsyncResult asyncResult = (AsyncResult) msg.obj;
            String propertyName = msg.getData().getString(PROPERTY_NAME);
            asyncResult.setResult(getKrollObject().getProperty(propertyName));
            return true;
        }
        case MSG_FIRE_EVENT: {
            Object data = msg.obj;
            String event = msg.getData().getString(PROPERTY_NAME);
            boolean checkParent = msg.getData().getBoolean(PROPERTY_BUBBLES);
            doFireEvent(event, data, checkParent);

            return true;
        }
        case MSG_FIRE_SYNC_EVENT: {
            AsyncResult asyncResult = (AsyncResult) msg.obj;
            boolean handled = doFireEvent(msg.getData()
                    .getString(PROPERTY_NAME), asyncResult.getArg(), msg
                    .getData().getBoolean(PROPERTY_BUBBLES));
            asyncResult.setResult(handled);

            return handled;
        }
        case MSG_CALL_PROPERTY_ASYNC: {
            String propertyName = msg.getData().getString(PROPERTY_NAME);
            Object[] args = (Object[]) msg.obj;
            getKrollObject().callProperty(propertyName, args);

            return true;
        }
        case MSG_CALL_PROPERTY_SYNC: {
            String propertyName = msg.getData().getString(PROPERTY_NAME);
            AsyncResult asyncResult = (AsyncResult) msg.obj;
            Object[] args = (Object[]) asyncResult.getArg();
            getKrollObject().callProperty(propertyName, args);
            asyncResult.setResult(null);

            return true;
        }
        case MSG_UPDATE_KROLL_PROPERTIES: {
            if (msg.obj instanceof AsyncResult) {
                AsyncResult asyncResult = (AsyncResult) msg.obj;
                doUpdateKrollObjectProperties((HashMap<String, Object>) asyncResult
                        .getArg());
                asyncResult.setResult(null);
                return true;

            } else {
                doUpdateKrollObjectProperties((HashMap<String, Object>) msg.obj);
            }
           
            return true;
        }
        }

        return false;
    }

    // TODO: count should be removed since we no longer report it.
    // These methods only gets called now when the first listener
    // is added or the last one has been removed.
    /**
     * Called when a event listener is added to the proxy
     * 
     * @param event
     *            the event that the listener has been added for
     * @param count
     *            the number of listeners for this event. should not be used as
     *            this value is not reported correctly
     * @param proxy
     *            the proxy that the event was added to. otherwise known as
     *            "this"
     * @return <code>void</code>
     */
    protected void eventListenerAdded(String event, int count, KrollProxy proxy) {
        if (modelListener != null) {
            modelListener.get().listenerAdded(event, count, this);
        }
    }

    /**
     * Called when a event listener is removed from the proxy
     * 
     * @param event
     *            the event that the listener has been removed for
     * @param count
     *            the number of listeners for this event. should not be used as
     *            this value is not reported correctly
     * @param proxy
     *            the proxy that the event was removed from. otherwise known as
     *            "this"
     * @return <code>void</code>
     */
    protected void eventListenerRemoved(String event, int count,
            KrollProxy proxy) {
        if (modelListener != null) {
            modelListener.get().listenerRemoved(event, count, this);
        }
    }

    /**
     * Associates this proxy with the passed in {@link KrollProxyListener}.
     * 
     * @param modelListener
     *            the passed in KrollProxyListener.
     * @module.api
     */
    public void setModelListener(KrollProxyListener modelListener) {
        setModelListener(modelListener, true);
    }

    public KrollProxyListener getModelListener() {
        return modelListener.get();
    }

    public void setSetPropertyListener(SetPropertyChangeListener listener) {
        this.setPropertyListener = listener;
    }

    public void setModelListener(KrollProxyListener modelListener,
            boolean applyProps) {
        if (modelListener != null) {
            if (modelListener.equals(this.modelListener)) {
                return;
            }
            this.modelListener = new WeakReference<KrollProxyListener>(modelListener);
            if (applyProps) {
                if (!mProcessInUIThread || TiApplication.isUIThread()) {
                    modelListener.processProperties(getClonedProperties());
                } else {
                    getMainHandler().sendEmptyMessage(MSG_MODEL_PROCESS_PROPERTIES);
                }
            }
        } else {
            this.modelListener = null;
        }
    }

    public int addEventListener(String eventName, KrollEventCallback callback) {
        int listenerId = -1;

        if (eventName == null) {
            throw new IllegalStateException(
                    "addEventListener expects a non-null eventName");

        } else if (callback == null) {
            throw new IllegalStateException(
                    "addEventListener expects a non-null listener");
        }

        synchronized (eventListeners) {
            if (eventListeners.isEmpty()) {
                setProperty(PROPERTY_HAS_JAVA_LISTENER, true);
            }

            HashMap<Integer, KrollEventCallback> listeners = eventListeners
                    .get(eventName);
            if (listeners == null) {
                listeners = new HashMap<Integer, KrollEventCallback>();
                eventListeners.put(eventName, listeners);
            }

            // if (Log.isDebugModeEnabled()) {
            //     Log.d(TAG, "Added for eventName '" + eventName + "' with id "
            //             + listenerId, Log.DEBUG_MODE);
            // }
            listenerId = listenerIdGenerator.incrementAndGet();
            listeners.put(listenerId, callback);
        }

        return listenerId;
    }

    public void removeEventListener(String eventName, int listenerId) {
        if (eventName == null) {
            throw new IllegalStateException(
                    "removeEventListener expects a non-null eventName");
        }

        synchronized (eventListeners) {
            HashMap<Integer, KrollEventCallback> listeners = eventListeners
                    .get(eventName);
            if (listeners != null) {
                if (listeners.remove(listenerId) == null) {
                    Log.d(TAG, "listenerId " + listenerId
                            + " not for eventName '" + eventName + "'",
                            Log.DEBUG_MODE);
                }
                if (listeners.isEmpty()) {
                    eventListeners.remove(eventName);
                }
                if (eventListeners.isEmpty()) {
                    // If we don't have any java listeners, we set the property
                    // to false
                    setProperty(PROPERTY_HAS_JAVA_LISTENER, false);
                }
            }
        }
    }

    public void onEventFired(String event, Object data) {
        // HashMap<Integer, KrollEventCallback> listeners =
        // eventListeners.get(event);
        // if (listeners != null) {
        // for (Integer listenerId : listeners.keySet()) {
        // KrollEventCallback callback = listeners.get(listenerId);
        // if (callback != null) {
        // callback.call(data);
        // }
        // }
        // }
    }

    public boolean hasNonJSEventListener(String event) {
        return eventListeners.containsKey(event)
                && eventListeners.get(event) != null;
    }
    
    public boolean hasEvaluatorListener(String event) {
        return evaluators != null && evaluators.get(event) != null;
    }

    /**
     * Resolves the passed in scheme / path, and uses the Proxy's creationUrl if
     * the path is relative.
     * 
     * @param scheme
     *            the scheme of Url.
     * @param path
     *            the path of Url.
     * @return a string representation of URL given its components.
     * @module.api
     */
    public String resolveUrl(String scheme, String path) {
        return TiUrl.resolve(creationUrl.baseUrl, path, scheme);
    }

    public String getProxyId() {
        return proxyId;
    }

    protected KrollDict createErrorResponse(int code, String message) {
        KrollDict error = new KrollDict();
        error.putCodeAndMessage(code, message);
        error.put(TiC.ERROR_PROPERTY_MESSAGE, message);
        return error;
    }

    /**
     * Releases the KrollObject, freeing memory.
     * 
     * @module.api
     */
    public void release() {
        if (krollObject != null) {
            krollObject.release();
            krollObject = null;
        }
        if (properties != null) {
            synchronized (properties) {
//                Iterator it = properties.entrySet().iterator();
//                while (it.hasNext()) {
//                    Map.Entry pairs = (Map.Entry)it.next();
//                    Object value = pairs.getValue();
//                    if (value instanceof KrollProxy) {
//                        ((KrollProxy) value).release();
//                    }
//                }
                properties.clear();
            }
        }
        
        modelListener = null;
        evaluators = null;
        eventListeners = null;
        eventOverrideDelegate = null;
        mSyncEvents = null;
        defaultValues.clear();
        createdInModule = null;
    }

//    // TODO RM_TICONTEXT
//    @Deprecated
//    public TiContext getTiContext() {
//        return new TiContext(getActivity(), proxyId);
//    }

    // For subclasses to override
    @Kroll.method
    @Kroll.getProperty
    public String getApiName() {
        return "Ti.Proxy";
    }

    protected void addPropToUpdateNativeSide(String key, Object value) {
        if (propertiesToUpdateNativeSide == null) {
            propertiesToUpdateNativeSide = new HashMap<String, Object>();
        }
        propertiesToUpdateNativeSide.put(key, value);
    }

    public void updatePropertiesNativeSide() {
        if (readyToUpdateNativeSideProperties
                && propertiesToUpdateNativeSide != null) {
            updateKrollObjectProperties(propertiesToUpdateNativeSide);
            propertiesToUpdateNativeSide = null;
        }
    }
    
    public boolean isReadyToUpdateNativeSideProperties() {
        return readyToUpdateNativeSideProperties;
    }

    protected void setReadyToUpdateNativeSideProperties(final boolean value) {
        if (readyToUpdateNativeSideProperties != value) {
            readyToUpdateNativeSideProperties = value;
            if (value) {
                updatePropertiesNativeSide();
            }
        }
    }

    public void addBinding(String bindId, KrollProxy bindingProxy) {
        if (bindId == null)
            return;
        //dont update JS side yet
        setPropertyJava(bindId, bindingProxy);
        addPropToUpdateNativeSide(bindId, bindingProxy);
    }

    @SuppressWarnings("unchecked")
    protected void initFromTemplate(HashMap template_, KrollProxy rootProxy,
            boolean updateKrollProperties, boolean recursive) {
        if (rootProxy != null) {
            String bindId = TiConvert.toString(template_, TiC.PROPERTY_BIND_ID,
                    null);
            if (bindId != null) {
                setPropertyJava(TiC.PROPERTY_BIND_ID, bindId);
            }
            // always call addBinding even with bindId=null, subclasses might
            // need it
            if (rootProxy != this) {
                rootProxy.addBinding(bindId, this);
            }
        }
        if (template_.containsKey(TiC.PROPERTY_EVENTS)) {
            Object events = template_.get(TiC.PROPERTY_EVENTS);

            if (events instanceof HashMap) {
                Iterator entries = ((HashMap) events).entrySet().iterator();
                while (entries.hasNext()) {
                    Map.Entry entry = (Map.Entry) entries.next();
                    String key = (String) entry.getKey();
                    Object value = entry.getValue();
                    int count = -1;
                    if (value instanceof KrollFunction) {
                        count = addEventListener(key, new KrollEventFunction(
                                getKrollObject(), (KrollFunction) value));
                    } else {
                        count = addEvaluator(key, value);
                    }
                    eventListenerAdded(key, count, this);
                }
            }
        }
    }

    public KrollProxy createProxyFromObject(Object data, KrollProxy rootProxy,
            boolean updateKrollProperties) {
        if (data instanceof HashMap) {
            KrollProxy result = createProxyFromTemplate((HashMap) data,
                    rootProxy, updateKrollProperties);
            if (result != null) {
                result.setActivity(rootProxy.getActivity());
                result.updateKrollObjectProperties();
            }
            return result;
        } else if (data instanceof KrollProxy) {
            ((KrollProxy) data).setActivity(rootProxy.getActivity());
            return (KrollProxy) data;
        }
        return null;
    }

    public KrollProxy createProxyFromTemplate(HashMap template_,
            KrollProxy rootProxy, boolean updateKrollProperties) {
        return createProxyFromTemplate(template_, rootProxy,
                updateKrollProperties, true);
    }
    
    private static final String DEFAULT_TEMPLATE_TYPE = "Ti.UI.View";
    protected String defaultProxyTypeFromTemplate() {
        return DEFAULT_TEMPLATE_TYPE;
    }

    @SuppressWarnings("unchecked")
    public KrollProxy createProxyFromTemplate(HashMap template_,
            KrollProxy rootProxy, boolean updateKrollProperties,
            boolean recursive) {
        if (template_ == null || template_.isEmpty()) {
            return null;
        }
//        boolean creationArgsHandlesTemplate = true;
        String type = TiConvert.toString(template_, TiC.PROPERTY_TYPE, defaultProxyTypeFromTemplate());
        Object props = template_;
        if (template_.containsKey(TiC.PROPERTY_PROPERTIES)) {
            props =  template_.get(TiC.PROPERTY_PROPERTIES);
//            creationArgsHandlesTemplate = false;
        } else if (template_.containsKey(TiC.PROPERTY_CHILD_TEMPLATES) || 
                template_.containsKey(TiC.PROPERTY_EVENTS)){
            props = new HashMap(template_);
            ((HashMap) props).remove(TiC.PROPERTY_EVENTS);
            ((HashMap) props).remove(TiC.PROPERTY_CHILD_TEMPLATES);
        }
        try {
            Class<? extends KrollProxy> cls = (Class<? extends KrollProxy>) Class
                    .forName(APIMap.getProxyClass(type));
            KrollProxy proxy = KrollProxy.createProxy(cls, null,
                    new Object[] { props }, null);
            if (proxy == null)
                return null;
//            if (creationArgsHandlesTemplate) {
//                template_.remove(TiC.PROPERTY_EVENTS);
//                template_.remove(TiC.PROPERTY_CHILD_TEMPLATES);
//            }
            proxy.initFromTemplate(template_, rootProxy, updateKrollProperties,
                    recursive);
            if (updateKrollProperties) {
                rootProxy.updatePropertiesNativeSide();
            }
            return proxy;
        } catch (Exception e) {
            Log.e(TAG, "Error creating view from template: " + e.toString());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public KrollProxy createTypeViewFromDict(HashMap template_, String type) {
        Object props = template_.get(TiC.PROPERTY_PROPERTIES);
        try {
            Class<? extends KrollProxy> cls = (Class<? extends KrollProxy>) Class
                    .forName(APIMap.getProxyClass(type));
            KrollProxy proxy = createProxy(cls, null, new Object[] { props },
                    null);
            if (proxy == null)
                return null;
            proxy.initFromTemplate(template_, proxy, true, true);
            return proxy;
        } catch (Exception e) {
            Log.e(TAG, "Error creating view from dict: " + e.toString());
            return null;
        }
    }

    public void reloadProperties() {
        if (modelListener != null) {
            synchronized (properties) {
                modelListener.get().processProperties(getClonedProperties());
            }
        }
    }

    public void addSyncEvent(final String event) {
        if (mSyncEvents == null) {
            mSyncEvents = new ArrayList<String>();
        }
        if (!mSyncEvents.contains(event)) {
            mSyncEvents.add(event);
        }
    }

    public void setSyncEvents(final String[] events) {
        mSyncEvents = Arrays.asList(events);
    }

    public void removeSyncEvent(final String event) {
        if (mSyncEvents != null) {
            mSyncEvents.remove(event);
        }
    }

    public void setEventOverrideDelegate(
            final TiViewEventOverrideDelegate eventOverrideDelegate) {
        if (eventOverrideDelegate != null) {
            this.eventOverrideDelegate = new WeakReference<TiViewEventOverrideDelegate>(eventOverrideDelegate);
        } else {
            this.eventOverrideDelegate = null;
        }
    }

/**
	 * A place holder for subclasses to extend. Its purpose is to receive native Android onCreate life cycle events.
	 * @param activity the activity attached to this module.
	 * @module.api
	 */
	public void onCreate(Activity activity, Bundle savedInstanceState) {
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

    public <T> T getValueInUIThread(final Command<T> command, T defaultValue){
        return TiActivityHelper.getValueInUIThread(getActivity(), this, command, defaultValue);
    }
    
    public void runInUiThread(final CommandNoReturn command, final boolean blocking) {
        if (TiApplication.isUIThread()) {
            command.execute();
        } else {
            if (blocking) {
                TiMessenger.sendBlockingMainCommand(command);
            } else {
                TiMessenger.sendMainCommand(command);
            }

        }
    }
    public <T> T getInUiThread(final Command<T> command) {
        if (TiApplication.isUIThread()) {
            return command.execute();
        } else {
            return (T) TiMessenger.sendBlockingMainCommand(command);

        }
    }
}
