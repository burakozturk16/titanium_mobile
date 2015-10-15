/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.android;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollModule;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiContext;
import org.appcelerator.titanium.proxy.AnimatableReusableProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiHtml;
import org.appcelerator.titanium.util.TiImageHelper;
import org.appcelerator.titanium.util.TiRHelper;
import org.appcelerator.titanium.util.TiImageHelper.TiDrawableTarget;
import org.appcelerator.titanium.util.TiRHelper.ResourceNotFoundException;
import org.appcelerator.titanium.util.TiUIHelper.FontDesc;
import org.appcelerator.titanium.util.TiUIHelper;
import org.appcelerator.titanium.view.TiDrawableReference;

import ti.modules.titanium.android.notificationmanager.NotificationProxy;

import com.squareup.picasso.Picasso.LoadedFrom;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.View;
import android.widget.RemoteViews;

@Kroll.proxy(creatableInModule = AndroidModule.class)
public class RemoteViewsProxy extends AnimatableReusableProxy implements TiDrawableTarget {
    private static final String TAG = "RemoteViewsProxy";
    protected String packageName;
    protected int layoutId;
    protected RemoteViews remoteViews;
    private int loadingBitmapViewId;
    
    private static AtomicInteger idGenerator;
    private final int remoteViewId;

    private NotificationProxy notification;
    
    private static final String ACTION_CLICK = "TI_REMOTEVIEWS_CLICK";
    private static final String ACTION_INTENT_ID = "action.id";
    private static final String ACTION_REMOTE_VIEW_ID = "remoteview.id";
    
    private BroadcastReceiver onClickReicever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            int viewId = intent.getIntExtra(ACTION_REMOTE_VIEW_ID, 0);
            if (action.equals(ACTION_CLICK) && viewId == RemoteViewsProxy.this.remoteViewId) {
                String id = intent.getStringExtra(ACTION_INTENT_ID);
                if (id != null && hasListeners(TiC.EVENT_CLICK, true)) {
                    KrollDict data = new KrollDict();
                    data.put("clickid", id);
                    fireEvent(TiC.EVENT_CLICK, data, true, false);
                }
            }
            
        }
    };

    public RemoteViewsProxy() {
        super();
        if (idGenerator == null) {
            idGenerator = new AtomicInteger(0);
        }
        remoteViewId = idGenerator.incrementAndGet();
    }

    public RemoteViewsProxy(TiContext context) {
        this();
    }
    
    public void willShow() {
        TiApplication.getAppContext().registerReceiver(onClickReicever, new IntentFilter(ACTION_CLICK));
    }
    
    public void didHide() {
        TiApplication.getAppContext().unregisterReceiver(onClickReicever);
    }
    
    public static RemoteViewsProxy fromObject(Object obj) {
        if (obj instanceof RemoteViewsProxy) {
            return (RemoteViewsProxy) obj;
        } else if (obj instanceof HashMap) {
            return (RemoteViewsProxy) KrollProxy.createProxy(RemoteViewsProxy.class, null, new Object[]{obj}, null);
        }
        return null;
    }

    public void setNotification(final NotificationProxy notif) {
        this.notification = notif;
    }

    @Kroll.method
    public void update() {
        if (this.notification != null) {
            this.notification.update();
        }
    }

    @Override
    public void handleCreationArgs(KrollModule createdInModule, Object[] args) {
        packageName = TiApplication.getInstance().getPackageName();
        layoutId = -1;
        if (args.length >= 1) {
            if (args[0] instanceof Number) {
                layoutId = TiConvert.toInt(args[0]);
            } else if (args.length >= 2 && args[0] instanceof String) {
                packageName = (String) args[0];
                layoutId = TiConvert.toInt(args[1]);
            }
        }
        super.handleCreationArgs(createdInModule, args);
    }
    
    /**
     * @param which Which {@link PendingIntent} to return
     * @return A {@link PendingIntent} ready to control playback
     */
    
    private final PendingIntent retreiveonClickIntent(final Context context, final String id) {
        Intent intent = new Intent(ACTION_CLICK);
        intent.putExtra(ACTION_INTENT_ID, id);
        intent.putExtra(ACTION_REMOTE_VIEW_ID, remoteViewId);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, id.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return pendingIntent;
    }

    @Override
    public void handleCreationDict(KrollDict d) {
        super.handleCreationDict(d);
        if (d == null) {
            return;
        }
        for (String key : d.keySet()) {
            propertySet(key, d.get(key), null, false);
        }
        
        remoteViews = new RemoteViews(packageName, layoutId);
        if (d.containsKey("clicks")) {
            Object obj = d.get("clicks");
            if (obj instanceof Object[]) {
                Object[] array = (Object[])obj;
                final Context context = TiApplication.getAppContext();
                for (int i = 0; i < array.length; i++) {
                    String id = TiConvert.toString(array[i]);
                    int viewId = 0;
                    try {
                        viewId = TiRHelper.getResource("id." + id);
                    } catch (ResourceNotFoundException e) {
                        viewId = 0;
                    }
                    if (viewId > 0) {
                        remoteViews.setOnClickPendingIntent(viewId, retreiveonClickIntent(context, id));
                    }
                }
            }
        }
    }

    @Kroll.method
    public void setBoolean(int viewId, String methodName, boolean value) {
        remoteViews.setBoolean(viewId, methodName, value);
    }

    @Kroll.method
    public void setDouble(int viewId, String methodName, double value) {
        remoteViews.setDouble(viewId, methodName, value);
    }

    @Kroll.method
    public void setInt(int viewId, String methodName, int value) {
        remoteViews.setInt(viewId, methodName, value);
    }

    @Kroll.method
    public void setString(int viewId, String methodName, String value) {
        remoteViews.setString(viewId, methodName, value);
    }

    @Kroll.method
    public void setUri(int viewId, String methodName, String value) {
        remoteViews.setUri(viewId, methodName, Uri.parse(value));
    }

    @Kroll.method
    public void setImageViewResource(int viewId, int srcId) {
        remoteViews.setImageViewResource(viewId, srcId);
    }

    @Kroll.method
    public void setImageViewUri(int viewId, String uriString) {
        Uri uri = Uri.parse(resolveUrl(null, uriString));
        remoteViews.setImageViewUri(viewId, uri);
    }

    @Kroll.method
    public void setOnClickPendingIntent(int viewId,
            PendingIntentProxy pendingIntent) {
        remoteViews.setOnClickPendingIntent(viewId,
                pendingIntent.getPendingIntent());
    }

    @Kroll.method
    public void setProgressBar(int viewId, int max, int progress,
            boolean indeterminate) {
        remoteViews.setProgressBar(viewId, max, progress, indeterminate);
    }

    @Kroll.method
    public void setTextColor(int viewId, int color) {
        remoteViews.setTextColor(viewId, color);
    }

    @Kroll.method
    public void setTextSize(int viewId, Object size) {
        String fontSize = TiConvert.toString(size);
        float[] result = new float[2];
        TiUIHelper.getSizeAndUnits(fontSize, result);
        remoteViews.setTextViewTextSize(viewId, (int) result[0], result[1]);
    }

    @Kroll.method
    public void setImageViewImage(int viewId, Object image) {
//        if (TiApplication.isUIThread()) {
            handleSetImageViewBitmap(viewId, image);
//        } else {
//            getMainHandler().obtainMessage(MSG_SET_IMAGE_BITMAP, viewId, 0,
//                    image).sendToTarget();
//        }
    }

    @Kroll.method
    public void setTextViewText(int viewId, String text) {
        remoteViews.setTextViewText(viewId, text);
    }

    @Kroll.method
    public void setViewVisibility(int viewId, int visibility) {
        remoteViews.setViewVisibility(viewId, visibility);
    }

    @Kroll.method
    public void setChronometer(int viewId, long base, String format,
            boolean started) {
        remoteViews.setChronometer(viewId, base, format, started);
    }
    
    @Override
    public void propertySet(String key, Object newValue, Object oldValue,
            boolean changedProperty) {
        switch (key) {
        case TiC.PROPERTY_PACKAGE_NAME:
            packageName = TiConvert.toString(newValue);
            break;
        case TiC.PROPERTY_LAYOUT_ID:
            layoutId = TiConvert.toInt(newValue);
            break;
        default:
            try {
                int viewId = TiRHelper.getResource("id." + key);
                if (viewId > 0) {
                    for (Map.Entry<String, Object> entry2 : ((HashMap<String, Object>) newValue).entrySet()) {
                        viewPropertySet(viewId, entry2.getKey(), entry2.getValue());
                    }
                } else {
                    super.propertySet(key, newValue, oldValue, changedProperty);
                }
            } catch (Exception e) {
            }
            break;
        }
    }
    
    public void viewPropertySet(int viewId, String key, Object newValue) {
        switch (key) {
        case TiC.PROPERTY_VISIBLE:
            remoteViews.setViewVisibility(viewId, (TiConvert.toBoolean(
                    newValue, true) ? View.VISIBLE : View.GONE));
            break;
        case TiC.PROPERTY_TEXT:
            remoteViews.setTextViewText(viewId, TiConvert.toString(newValue));
            break;
        case TiC.PROPERTY_HTML:
            remoteViews.setTextViewText(viewId,
                    TiHtml.fromHtml((TiConvert.toString(newValue))));
            break;
        case TiC.PROPERTY_PROGRESS:
            if (newValue instanceof HashMap) {
                HashMap map = (HashMap)newValue;
                int max = TiConvert.toInt(map, TiC.PROPERTY_MAX);
                int progress = TiConvert.toInt(map, TiC.PROPERTY_VALUE);
                boolean indeterminate = TiConvert.toBoolean(map, TiC.PROPERTY_INDETERMINATE);
                remoteViews.setProgressBar(viewId, max, progress, indeterminate);
            }
            break;
        case TiC.PROPERTY_IMAGE:
            setImageViewImage(viewId, newValue);
            break;
        case TiC.PROPERTY_FONT:
            FontDesc desc = TiUIHelper.getFontStyle(getActivity(),
                    (HashMap<String, Object>) newValue);
            remoteViews.setTextViewTextSize(viewId, desc.sizeUnit, desc.size);
            break;
        case TiC.PROPERTY_COLOR:
            remoteViews.setTextColor(viewId, TiConvert.toColor(newValue));
            break;
        case "onClickPendingIntent":
            PendingIntentProxy pendingProxy = PendingIntentProxy
                    .fromObject(newValue);
            remoteViews.setOnClickPendingIntent(viewId,
                    pendingProxy.getPendingIntent());
            break;
        default:
            break;
        }
    }

    public RemoteViews getRemoteViews() {
        return remoteViews;
    }

    @Override
    public String getApiName() {
        return "Ti.Android.RemoteViews";
    }
    
    
    
    private void handleSetImageViewBitmap(int viewId, final Object obj) {
        TiDrawableReference imageref = TiDrawableReference
                .fromObject(this, obj);
        loadingBitmapViewId = viewId;
        TiImageHelper.downloadDrawable(this, imageref, true, this);
    }

    @Override
    public void onBitmapLoaded(final Bitmap bitmap, LoadedFrom from) {
        remoteViews.setImageViewBitmap(loadingBitmapViewId, bitmap);
        update();
        loadingBitmapViewId = 0;
    }

    @Override
    public void onBitmapFailed(Drawable errorDrawable) {
        remoteViews.setImageViewBitmap(loadingBitmapViewId, null);
        update();
        loadingBitmapViewId = 0;
    }

    @Override
    public void onPrepareLoad(Drawable placeHolderDrawable) {
    }

    @Override
    public void onDrawableLoaded(Drawable drawable, LoadedFrom from) {
        if (drawable instanceof BitmapDrawable) {
            onBitmapLoaded(((BitmapDrawable) drawable).getBitmap(), from);
        }        
    }
}
