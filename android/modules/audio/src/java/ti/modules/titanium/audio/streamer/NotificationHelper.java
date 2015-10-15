/*
 * Copyright (C) 2012 Andrew Neal Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package ti.modules.titanium.audio.streamer;

import java.util.HashMap;
import java.util.Map;

import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiRHelper;
import org.appcelerator.titanium.util.TiRHelper.ResourceNotFoundException;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.util.Log;
import android.widget.RemoteViews;

/**
 * Builds the notification for Apollo's service. Jelly Bean and higher uses the
 * expanded notification by default.
 * 
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class NotificationHelper {
    private static final boolean HONEYCOMB_OR_GREATER = (Build.VERSION.SDK_INT >= 11);
    private static final boolean JELLY_BEAN_OR_GREATER = (Build.VERSION.SDK_INT >= 16);
    
    public static final String NOTIFICATION_DRAWBLE_PLAY = "tiasn_btn_play";
    public static final String NOTIFICATION_DRAWBLE_PAUSE = "tiasn_btn_pause";

    
    public static final String NOTIFICATION_BASE_PREV = "tiasn_base_prev";
    public static final String NOTIFICATION_BASE_NEXT = "tiasn_base_next";
    public static final String NOTIFICATION_BASE_PLAY = "tiasn_base_play";
    public static final String NOTIFICATION_BASE_TITLE = "tiasn_base_title";
    public static final String NOTIFICATION_BASE_ARTIST = "tiasn_base_artist";
    public static final String NOTIFICATION_BASE_ALBUM = "tiasn_base_album";
    public static final String NOTIFICATION_BASE_IMAGE = "tiasn_base_image";
    private int id_notification_base_prev = 0;
    private int id_notification_base_next = 0;
    private int id_notification_base_play = 0;
    private int id_notification_base_title = 0;
    private int id_notification_base_artist = 0;
    private int id_notification_base_album = 0;
    private int id_notification_base_image = 0;
    public static final String NOTIFICATION_EXPANDED_COLLAPSE = "tiasn_base_collapse";
    public static final String NOTIFICATION_EXPANDED_PREV = "tiasn_expanded_prev";
    public static final String NOTIFICATION_EXPANDED_NEXT = "tiasn_expanded_next";
    public static final String NOTIFICATION_EXPANDED_PLAY = "tiasn_expanded_play";
    public static final String NOTIFICATION_EXPANDED_TITLE = "tiasn_expanded_title";
    public static final String NOTIFICATION_EXPANDED_ARTIST = "tiasn_expanded_artist";
    public static final String NOTIFICATION_EXPANDED_ALBUM = "tiasn_expanded_album";
    public static final String NOTIFICATION_EXPANDED_IMAGE = "tiasn_expanded_image";
    private static final String TAG = "NotificationHelper";
    private int id_notification_expanded_collapse = 0;
    private int id_notification_expanded_prev = 0;
    private int id_notification_expanded_next = 0;
    private int id_notification_expanded_play = 0;
    private int id_notification_expanded_title = 0;
    private int id_notification_expanded_artist = 0;
    private int id_notification_expanded_album = 0;
    private int id_notification_expanded_image = 0;

    /**
     * Notification ID
     */
    private final int mNotificationId;

    /**
     * NotificationManager
     */
    private final NotificationManager mNotificationManager;

    /**
     * Context
     */
    private final AudioService mService;
    private final Class mServiceClass;

    /**
     * Custom notification layout
     */
    private RemoteViews mView;
    private int mViewId = 0;

    /**
     * The Notification
     */
    private Notification mNotification = null;

    /**
     * API 16+ bigContentView
     */
    private RemoteViews mExpandedView;
    private int mExpandedViewId = 0;

    private int mIcon;
    private int mDrawablePlay;
    private int mDrawablePause;

    /**
     * Constructor of <code>NotificationHelper</code>
     * 
     * @param service
     *            The {@link Context} to use
     */
    public NotificationHelper(final AudioService service, int icon,
            int viewId, int expandedViewId) {
        mService = service;
        mServiceClass = mService.getClass();
        mNotificationId = mServiceClass.toString().hashCode();
        this.mIcon = icon;
        this.mViewId = viewId;
        this.mExpandedViewId = expandedViewId;
        mNotificationManager = (NotificationManager) service
                .getSystemService(Context.NOTIFICATION_SERVICE);
    }
    public void buildNotificationIfNeeded() {
        if (mNotification == null) {
            buildNotification();
        }
    }

    /**
     * Call this to build the {@link Notification}.
     */
    public void buildNotification() {

        // Default notfication layout
        if (mViewId == 0 && mExpandedViewId == 0) {
            return;
        }
        
        try {
            mDrawablePlay = TiRHelper.getResource("drawable."
                    + NOTIFICATION_DRAWBLE_PLAY);
            mDrawablePause = TiRHelper.getResource("drawable."
                    + NOTIFICATION_DRAWBLE_PAUSE);
        } catch (ResourceNotFoundException e1) {
        }
        
        if (mViewId > 0) {
            mView = new RemoteViews(mService.getPackageName(), mViewId);
            try {
                id_notification_base_title = TiRHelper.getResource("id."
                        + NOTIFICATION_BASE_TITLE);
                id_notification_base_artist = TiRHelper.getResource("id."
                        + NOTIFICATION_BASE_ARTIST);
                id_notification_base_album = TiRHelper.getResource("id."
                        + NOTIFICATION_BASE_ALBUM);
                id_notification_base_image = TiRHelper.getResource("id."
                        + NOTIFICATION_BASE_IMAGE);
                id_notification_base_play = TiRHelper.getResource("id."
                        + NOTIFICATION_BASE_PLAY);
                id_notification_base_prev = TiRHelper.getResource("id."
                        + NOTIFICATION_BASE_PREV);
                id_notification_base_next = TiRHelper.getResource("id."
                        + NOTIFICATION_BASE_NEXT);
            } catch (ResourceNotFoundException e) {
            }
            // Set up the content view
            // initCollapsedLayout(trackName, albumName, artistName, albumArt);
        }

        if (HONEYCOMB_OR_GREATER) {
            // Notification Builder
            mNotification = new NotificationCompat.Builder(mService)
                    .setSmallIcon(mIcon).setContentIntent(getPendingIntent())
                    .setOngoing(true)
                    .setPriority(Notification.PRIORITY_DEFAULT)
                    .setContent(mView).build();
            // Control playback from the notification
            initPlaybackActions();
            if (JELLY_BEAN_OR_GREATER) {
                // Expanded notifiction style
                mExpandedView = new RemoteViews(mService.getPackageName(),
                        mExpandedViewId);
                try {
                    id_notification_expanded_title = TiRHelper
                            .getResource("id." + NOTIFICATION_EXPANDED_TITLE);
                    id_notification_expanded_artist = TiRHelper
                            .getResource("id." + NOTIFICATION_EXPANDED_ARTIST);
                    id_notification_expanded_album = TiRHelper
                            .getResource("id." + NOTIFICATION_EXPANDED_ALBUM);
                    id_notification_expanded_image = TiRHelper
                            .getResource("id." + NOTIFICATION_EXPANDED_IMAGE);
                    id_notification_expanded_play = TiRHelper.getResource("id."
                            + NOTIFICATION_EXPANDED_PLAY);
                    id_notification_expanded_prev = TiRHelper.getResource("id."
                            + NOTIFICATION_EXPANDED_PREV);
                    id_notification_expanded_next = TiRHelper.getResource("id."
                            + NOTIFICATION_EXPANDED_NEXT);
                    id_notification_expanded_collapse = TiRHelper
                            .getResource("id." + NOTIFICATION_EXPANDED_COLLAPSE);
                } catch (ResourceNotFoundException e) {
                }

                mNotification.bigContentView = mExpandedView;
                // Control playback from the notification
                initExpandedPlaybackActions();
                // Set up the expanded content view
                // initExpandedLayout(trackName, albumName, artistName,
                // albumArt);
            }
            mService.startForeground(mNotificationId, mNotification);
        } else {
            Builder notificationBuilder = new NotificationCompat.Builder(mService);
            notificationBuilder.setSmallIcon(mIcon);
            notificationBuilder.setContentIntent(getPendingIntent());
            notificationBuilder.setOngoing(true);
            mNotification = notificationBuilder.build();
            mNotification.contentView = mView;
            if (JELLY_BEAN_OR_GREATER) {
                mNotification.bigContentView = mExpandedView;

            }
            mService.startForeground(mNotificationId, mNotification);
        }
    }

    /**
     * Changes the playback controls in and out of a paused state
     * 
     * @param isPlaying
     *            True if music is playing, false otherwise
     */
    public void goToIdleState(final boolean isPlaying) {
        if (mNotification == null || mNotificationManager == null) {
            return;
        }
        if (HONEYCOMB_OR_GREATER && mView != null) {
            mView.setImageViewResource(id_notification_base_play,
                    isPlaying ? mDrawablePause : mDrawablePlay);
        }

        if (JELLY_BEAN_OR_GREATER && mExpandedView != null) {
            mExpandedView.setImageViewResource(id_notification_expanded_play,
                    isPlaying ? mDrawablePause : mDrawablePlay);
        }
        try {
            mNotificationManager.notify(mNotificationId, mNotification);
        } catch (final IllegalStateException e) {
            Log.e("NotificationHelper", "goToIdleState - " + e);
            // FIXME Every so often an ISE is throw reading
            // "can't parcel recycled Bitmap". Figure out and understand why
            // this is happening, then prevent it.
        }
    }

    /**
     * Open to the now playing screen
     */
    private PendingIntent getPendingIntent() {
        TiApplication app = TiApplication.getInstance();
        Intent i = app.getPackageManager().getLaunchIntentForPackage(app.getPackageName());
        i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        i.setAction(Intent.ACTION_MAIN);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                app, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
        return pendingIntent;
    }

    /**
     * Lets the buttons in the remote view control playback in the expanded
     * layout
     */
    private void initExpandedPlaybackActions() {
        // Play and pause
        if (id_notification_expanded_play > 0) {
            mExpandedView.setOnClickPendingIntent(
                    id_notification_expanded_play, retreivePlaybackActions(1));
            mExpandedView.setImageViewResource(id_notification_expanded_play,
                    mDrawablePause);
        }

        // Skip tracks
        if (id_notification_expanded_next > 0) {
            mExpandedView.setOnClickPendingIntent(
                    id_notification_expanded_next, retreivePlaybackActions(2));
        }

        // Previous tracks
        if (id_notification_expanded_prev > 0) {
            mExpandedView.setOnClickPendingIntent(
                    id_notification_expanded_prev, retreivePlaybackActions(3));
        }

        // Stop and collapse the notification
        if (id_notification_expanded_collapse > 0) {
            mExpandedView.setOnClickPendingIntent(
                    id_notification_expanded_collapse,
                    retreivePlaybackActions(4));
        }
    }

    /**
     * Lets the buttons in the remote view control playback in the normal layout
     */
    private void initPlaybackActions() {
        // Play and pause
        if (id_notification_base_play > 0) {
            mView.setOnClickPendingIntent(id_notification_base_play,
                    retreivePlaybackActions(1));
            mView.setImageViewResource(id_notification_base_play,
                    mDrawablePause);
        }

        // Skip tracks
        if (id_notification_base_next > 0) {
            mView.setOnClickPendingIntent(id_notification_base_next,
                    retreivePlaybackActions(2));
        }

        // Previous tracks
        if (id_notification_base_prev > 0) {
            mView.setOnClickPendingIntent(id_notification_base_prev,
                    retreivePlaybackActions(3));
        }
    }

    /**
     * @param which
     *            Which {@link PendingIntent} to return
     * @return A {@link PendingIntent} ready to control playback
     */
    @SuppressWarnings("static-access")
    private final PendingIntent retreivePlaybackActions(final int which) {
        Intent action;
        PendingIntent pendingIntent;
        final ComponentName serviceName = new ComponentName(mService,
                mServiceClass);
        switch (which) {
        case 1:
            // Play and pause
            action = new Intent(mService.cmds.TOGGLEPAUSE_ACTION);
            action.setComponent(serviceName);
            pendingIntent = PendingIntent.getService(mService, 1, action, 0);
            return pendingIntent;
        case 2:
            // Skip tracks
            action = new Intent(mService.cmds.NEXT_ACTION);
            action.setComponent(serviceName);
            pendingIntent = PendingIntent.getService(mService, 2, action, 0);
            return pendingIntent;
        case 3:
            // Previous tracks
            action = new Intent(mService.cmds.PREVIOUS_ACTION);
            action.setComponent(serviceName);
            pendingIntent = PendingIntent.getService(mService, 3, action, 0);
            return pendingIntent;
        case 4:
            // Stop and collapse the notification
            action = new Intent(mService.cmds.STOP_ACTION);
            action.setComponent(serviceName);
            pendingIntent = PendingIntent.getService(mService, 4, action, 0);
            return pendingIntent;
        default:
            break;
        }
        return null;
    }

    /**
     * Sets the track name, artist name, and album art in the normal layout
     */
    private void updateCollapsedLayout(final String trackName,
            final String artistName, final String albumName) {
        if (mView == null)
            return;
        // Track name
        mView.setTextViewText(id_notification_base_title, trackName);
        // Artist name
        mView.setTextViewText(id_notification_base_artist, artistName);
        // Album name
        mView.setTextViewText(id_notification_base_album, albumName);
    }

    /**
     * Sets the track name, album name, artist name, and album art in the
     * expanded layout
     */
    private void updateExpandedLayout(final String trackName,
            final String artistName, final String albumName) {
        if (mExpandedView == null)
            return;
        // Track name
        mExpandedView
                .setTextViewText(id_notification_expanded_title, trackName);
        // Artist name
        mExpandedView.setTextViewText(id_notification_expanded_artist,
                artistName);
        // Album name
        mExpandedView
                .setTextViewText(id_notification_expanded_album, albumName);
    }

    public void updateAlbumArt(final Bitmap bitmap) {
        if (mNotification == null) {
            return;
        }
        if (mView != null) {
            mView.setImageViewBitmap(id_notification_base_image, bitmap);
        }
        if (mExpandedView != null) {
            mExpandedView.setImageViewBitmap(id_notification_expanded_image,
                    bitmap);
        }
        if (mNotification != null) {
            mNotificationManager.notify(mNotificationId, mNotification);
        }
    }

    public void updateMetadata(final Map<String, Object> dict) {
        if (mNotification == null) {
            return;
        }
        Log.d(TAG, "updateMetadata" + dict);
        String title = null;
        String artist = null;
        String album = null;
        if (dict != null) {
            title = TiConvert.toString(dict.get("title"));
            artist = TiConvert.toString(dict.get("artist"));
            album = TiConvert.toString(dict.get("album"));
        }
        updateCollapsedLayout(title, artist, album);
        updateExpandedLayout(title, artist, album);
        if (title != null && artist != null) {
            mNotification.tickerText = title + " - " + artist;
        }
        else {
            mNotification.tickerText = null;
        }
        mNotificationManager.notify(mNotificationId, mNotification);
    }

    public void showNotification() {
        if (mNotification == null) {
            return;
        }
        CharSequence oldvalue = mNotification.tickerText;
        if (oldvalue != null) {
            if (oldvalue.toString().endsWith(" ")) {
                mNotification.tickerText = oldvalue.subSequence(0, oldvalue.length() - 1);
            } else {
                mNotification.tickerText = oldvalue + " ";
            }
        }
        mNotificationManager.notify(mNotificationId, mNotification);
    }
    
    public void hideNotification() {
        if (mNotification == null) {
            return;
        }
        mService.stopForeground(true);
        mNotificationManager.cancel(mNotificationId);
    }
}
