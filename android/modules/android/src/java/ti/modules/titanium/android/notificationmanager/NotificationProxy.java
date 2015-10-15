/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2014 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.android.notificationmanager;

import java.util.Date;
import java.util.HashMap;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiContext;
import org.appcelerator.titanium.proxy.ReusableProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiImageHelper;
import org.appcelerator.titanium.util.TiUIHelper;
import org.appcelerator.titanium.util.TiImageHelper.TiDrawableTarget;
import org.appcelerator.titanium.view.TiDrawableReference;

import com.squareup.picasso.Picasso.LoadedFrom;

import ti.modules.titanium.android.AndroidModule;
import ti.modules.titanium.android.PendingIntentProxy;
import ti.modules.titanium.android.RemoteViewsProxy;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;

@Kroll.proxy(creatableInModule = AndroidModule.class, propertyAccessors = {
        TiC.PROPERTY_CONTENT_TEXT, TiC.PROPERTY_CONTENT_TITLE })
public class NotificationProxy extends ReusableProxy implements TiDrawableTarget {
    private static final String TAG = "TiNotification";

    private int currentId = -1;
	protected Builder notificationBuilder;
	private int flags, ledARGB, ledOnMS, ledOffMS;
	private Uri sound;
	private int audioStreamType;

    private RemoteViewsProxy contentView = null;
    private RemoteViewsProxy bigContentView = null;
    private int iconLevel;
    
    protected int mProcessUpdateFlags = 0;
    public static final int TIFLAG_NEEDS_UPDATE          = 0x00000001;
    
    public static NotificationProxy fromObject(Object obj) {
        if (obj instanceof NotificationProxy) {
            return (NotificationProxy) obj;
        } else if (obj instanceof HashMap) {
            NotificationProxy proxy = (NotificationProxy) createProxy(NotificationProxy.class, null, new Object[] { obj }, null);
            return proxy;
        }
        return null;
    }
	
	public NotificationProxy() 
	{
		super();
		notificationBuilder =  new NotificationCompat.Builder(TiApplication.getAppContext())
        .setSmallIcon(android.R.drawable.stat_sys_warning)
        .setWhen(System.currentTimeMillis());
		
		//set up default values
		flags = Notification.FLAG_AUTO_CANCEL;
		audioStreamType = Notification.STREAM_DEFAULT;
		
	}

	public NotificationProxy(TiContext tiContext) 
	{
		this();
	}


	@Override
    protected void didProcessProperties() {
        super.didProcessProperties();
        if ((mProcessUpdateFlags & TIFLAG_NEEDS_UPDATE) != 0) {
            if (this.currentId >= 0) {
                NotificationManager manager = getManager();
                Log.d(TAG, "updating notification " + this.currentId, Log.DEBUG_MODE);
                manager.notify(this.currentId, getNotification());
                mProcessUpdateFlags &= ~TIFLAG_NEEDS_UPDATE;
            }
        }
    }


    @Override
	public void propertySet(String key, Object newValue, Object oldValue,
            boolean changedProperty) {
        switch (key) {
        case TiC.PROPERTY_ICON:
            notificationBuilder.setSmallIcon(TiUIHelper.getResourceId(newValue, this));
            mProcessUpdateFlags |= TIFLAG_NEEDS_UPDATE;
            break;
        case TiC.PROPERTY_TICKER_TEXT:
            notificationBuilder.setTicker(TiConvert.toString(newValue));
            mProcessUpdateFlags |= TIFLAG_NEEDS_UPDATE;
            break;
        case TiC.PROPERTY_CONTENT_TITLE:
            notificationBuilder.setContentTitle(TiConvert.toString(newValue));
            mProcessUpdateFlags |= TIFLAG_NEEDS_UPDATE;
            break;
        case TiC.PROPERTY_CONTENT_TEXT:
            notificationBuilder.setContentText(TiConvert.toString(newValue));
            mProcessUpdateFlags |= TIFLAG_NEEDS_UPDATE;
            break;
        case TiC.PROPERTY_WHEN:
            if (newValue instanceof Date) {
                notificationBuilder.setWhen(((Date)newValue).getTime());
            } else {
                notificationBuilder.setWhen(((Double) TiConvert.toDouble(newValue)).longValue());
            }
            break;
        case TiC.PROPERTY_AUDIO_STREAM_TYPE:
            audioStreamType = TiConvert.toInt(newValue);
            if (sound != null) {
                notificationBuilder.setSound(this.sound, audioStreamType);
            }
            break;
        case TiC.PROPERTY_CONTENT_VIEW:
            if (TiC.JELLY_BEAN_OR_GREATER) {
                if (contentView != null) {
                    contentView.didHide();
                    contentView.setNotification(null);
                    contentView.setParentForBubbling(null);
                }
                contentView = RemoteViewsProxy.fromObject(newValue);
                if (contentView != null) {
                    contentView.setNotification(this);
                    contentView.setParentForBubbling(this);
                }
            }
            mProcessUpdateFlags |= TIFLAG_NEEDS_UPDATE;
            break;
        case TiC.PROPERTY_BIG_CONTENT_VIEW:
            if (TiC.JELLY_BEAN_OR_GREATER) {
                if (bigContentView != null) {
                    bigContentView.didHide();
                    bigContentView.setNotification(null);
                    bigContentView.setParentForBubbling(null);
                }
                bigContentView = RemoteViewsProxy.fromObject(newValue);
                if (bigContentView != null) {
                    bigContentView.setNotification(this);
                    bigContentView.setParentForBubbling(this);
                }
            }
            mProcessUpdateFlags |= TIFLAG_NEEDS_UPDATE;
            break;
        case TiC.PROPERTY_CONTENT_INTENT:
            notificationBuilder.setContentIntent(PendingIntentProxy.fromObject(newValue).getPendingIntent());  
            mProcessUpdateFlags |= TIFLAG_NEEDS_UPDATE;
            break;
        case TiC.PROPERTY_DEFAULTS:
            notificationBuilder.setDefaults(TiConvert.toInt(newValue));
            break;
        case TiC.PROPERTY_DELETE_INTENT:
            notificationBuilder.setDeleteIntent(PendingIntentProxy.fromObject(newValue).getPendingIntent());  
            break;
        case TiC.PROPERTY_FLAGS:
            this.flags = TiConvert.toInt(newValue);
            break;
        case TiC.PROPERTY_ICON_LEVEL:
            this.iconLevel = TiConvert.toInt(newValue);
            mProcessUpdateFlags |= TIFLAG_NEEDS_UPDATE;
            break;
        case TiC.PROPERTY_LED_ARGB:
            this.ledARGB = TiConvert.toInt(newValue);
            notificationBuilder.setLights(this.ledARGB, ledOnMS, ledOffMS);
            mProcessUpdateFlags |= TIFLAG_NEEDS_UPDATE;
            break;
        case TiC.PROPERTY_LED_OFF_MS:
            this.ledOffMS = TiConvert.toInt(newValue);
            notificationBuilder.setLights(ledARGB, ledOnMS, this.ledOffMS);
            mProcessUpdateFlags |= TIFLAG_NEEDS_UPDATE;
            break;
        case TiC.PROPERTY_LED_ON_MS:
            this.ledOnMS = TiConvert.toInt(newValue);
            notificationBuilder.setLights(ledARGB, ledOnMS, this.ledOffMS);
            mProcessUpdateFlags |= TIFLAG_NEEDS_UPDATE;
            break;
        case TiC.PROPERTY_NUMBER:
            notificationBuilder.setNumber(TiConvert.toInt(newValue));
            break;
        case TiC.PROPERTY_SOUND:
            String url = TiConvert.toString(newValue);
            if (url == null) {
                Log.e(TAG, "Url is null");
                return;
            }
            sound = Uri.parse(resolveUrl(null, url));
            notificationBuilder.setSound(sound, audioStreamType);
            mProcessUpdateFlags |= TIFLAG_NEEDS_UPDATE;
            break;
        case TiC.PROPERTY_VIBRATE_PATTERN:
            if (newValue instanceof Object[]) {
                Object[] pattern = (Object[]) newValue;
                long[] vibrate = new long[pattern.length];
                for (int i = 0; i < pattern.length; i++) {
                    vibrate[i] = ((Double)TiConvert.toDouble(pattern[i])).longValue();
                }
                notificationBuilder.setVibrate(vibrate);
            }
            break;
        case TiC.PROPERTY_VISIBILITY:
            notificationBuilder.setVisibility(TiConvert.toInt(newValue));
            mProcessUpdateFlags |= TIFLAG_NEEDS_UPDATE;
            break;
        case TiC.PROPERTY_CATEGORY:
            notificationBuilder.setCategory(TiConvert.toString(newValue));
            mProcessUpdateFlags |= TIFLAG_NEEDS_UPDATE;
            break;
        case TiC.PROPERTY_PRIORITY:
            notificationBuilder.setPriority(TiConvert.toInt(newValue));
            mProcessUpdateFlags |= TIFLAG_NEEDS_UPDATE;
            break;
        default:
            super.propertySet(key, newValue, oldValue, changedProperty);
            break;
        }
    }

	public void setCurrentId(final int currentId) {
        if (this.currentId != currentId) {
            willShow();
            // about to be shown, let register for delete

            this.currentId = currentId;
        }

    }

	public void update() {
        update(null);
    }

    public void willShow() {
        // if (notification.deleteIntent == null) {
        // Intent intent = new Intent(NOTIFICATION_DELETED_ACTION);
        // intent.putExtra("notif.id", currentId);
        // notification.deleteIntent = PendingIntent.getBroadcast(getActivity(),
        // 0, intent, 0);
        // TiApplication.getAppContext().registerReceiver(deleteReceiver, new
        // IntentFilter(NOTIFICATION_DELETED_ACTION));
        // }
        if (contentView != null) {
            contentView.willShow();
        }
        if (bigContentView != null) {
            bigContentView.willShow();
        }
    }

    public void didHide() {
        // TiApplication.getAppContext().unregisterReceiver(deleteReceiver);
        if (contentView != null) {
            contentView.didHide();
        }
        if (bigContentView != null) {
            bigContentView.didHide();
        }
        currentId = 0;
    }

    private NotificationManager getManager() {
        return (NotificationManager) TiApplication.getInstance()
                .getSystemService(Activity.NOTIFICATION_SERVICE);
    }

	@Kroll.method
    public boolean update(@Kroll.argument(optional = true) HashMap args) {
        if (args != null) {
            applyProperties(args);
        }
        if (this.currentId >= 0) {
            NotificationManager manager = getManager();
            Log.d(TAG, "updating notification " + this.currentId, Log.DEBUG_MODE);
            manager.notify(this.currentId, getNotification());
            return true;
        }
        return false;
    }

	public void handleSetLargeIcon(final Bitmap bitmap) {
        notificationBuilder.setLargeIcon(bitmap);
	}

	protected void checkLatestEventInfoProperties(KrollDict d)
	{
		if (d.containsKeyAndNotNull(TiC.PROPERTY_CONTENT_TITLE)
			|| d.containsKeyAndNotNull(TiC.PROPERTY_CONTENT_TEXT)) {
			String contentTitle = "";
			String contentText = "";
			if (d.containsKeyAndNotNull(TiC.PROPERTY_CONTENT_TITLE)) {
				contentTitle = TiConvert.toString(d, TiC.PROPERTY_CONTENT_TITLE);
				notificationBuilder.setContentTitle(contentTitle);
			}
			if (d.containsKeyAndNotNull(TiC.PROPERTY_CONTENT_TEXT)) {
				contentText = TiConvert.toString(d, TiC.PROPERTY_CONTENT_TEXT);
				notificationBuilder.setContentText(contentText);
			}
	
		}
	}

	@Kroll.method
    public void cancel() {
        if (this.currentId >= 0) {
            NotificationManager manager = getManager();
            manager.cancel(this.currentId);
        }
    }

	@Kroll.method
	public void setLatestEventInfo(String contentTitle, String contentText, PendingIntentProxy contentIntent)
	{
		notificationBuilder.setContentIntent(contentIntent.getPendingIntent())
		.setContentText(contentText)
		.setContentTitle(contentTitle);
	}

	public Notification getNotification()
	{ 
	    Notification notification = notificationBuilder.build();
        notification.flags = this.flags;
        if (this.contentView != null) {
            notification.contentView = this.contentView.getRemoteViews();
        }
        if (TiC.JELLY_BEAN_OR_GREATER && this.bigContentView != null) {
            notification.bigContentView = this.bigContentView.getRemoteViews();
        }
		return notification;
	}

	@Override
	public String getApiName()
	{
		return "Ti.Android.Notification";
	}
	
	
	private void handleSetLargeIcon(final Object obj) {
        TiDrawableReference imageref = TiDrawableReference
                .fromObject(this, obj);
        TiImageHelper.downloadDrawable(this, imageref, true, this);
    }

	@Override
    public void onBitmapLoaded(Bitmap bitmap, LoadedFrom from) {
        handleSetLargeIcon(bitmap);
        update();
    }

    @Override
    public void onBitmapFailed(Drawable errorDrawable) {
        handleSetLargeIcon(null);
        update();
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