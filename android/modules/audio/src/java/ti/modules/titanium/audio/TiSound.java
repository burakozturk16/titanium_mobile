/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.audio;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollPropertyChange;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.KrollProxyListener;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiImageHelper;
import org.appcelerator.titanium.util.TiRHelper;
import org.appcelerator.titanium.util.TiImageHelper.TiDrawableTarget;
import org.appcelerator.titanium.view.TiDrawableReference;

import ti.modules.titanium.audio.RemoteControlClientCompat.MetadataEditorCompat;

import com.squareup.picasso.Cache;
import com.squareup.picasso.Picasso.LoadedFrom;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.RemoteControlClient;
import android.media.RemoteControlClient.MetadataEditor;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.PowerManager;
import android.view.KeyEvent;
import android.webkit.URLUtil;

public class TiSound implements MediaPlayer.OnCompletionListener,
        MediaPlayer.OnErrorListener, KrollProxyListener,
        MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnInfoListener,
        MediaPlayer.OnPreparedListener, TiDrawableTarget, MusicFocusable, FocusableAudioWidget {
    private static final String TAG = "TiSound";

    public static final int STATE_BUFFERING = 0; // current playback is in the
                                                 // buffering from the network
                                                 // state
    public static final int STATE_INITIALIZED = 1; // current playback is in the
                                                   // initialization state
    public static final int STATE_PAUSED = 2; // current playback is in the
                                              // paused state
    public static final int STATE_PLAYING = 3; // current playback is in the
                                               // playing state
    public static final int STATE_STARTING = 4; // current playback is in the
                                                // starting playback state
    public static final int STATE_STOPPED = 5; // current playback is in the
                                               // stopped state
    public static final int STATE_STOPPING = 6; // current playback is in the
                                                // stopping state
    public static final int STATE_WAITING_FOR_DATA = 7; // current playback is
                                                        // in the waiting for
                                                        // audio data from the
                                                        // network state
    public static final int STATE_WAITING_FOR_QUEUE = 8; // current playback is
                                                         // in the waiting for
                                                         // audio data to fill
                                                         // the queue state

    public static final String STATE_BUFFERING_DESC = "buffering"; // current
                                                                   // playback
                                                                   // is in the
                                                                   // buffering
                                                                   // from the
                                                                   // network
                                                                   // state
    public static final String STATE_INITIALIZED_DESC = "initialized"; // current
                                                                       // playback
                                                                       // is in
                                                                       // the
                                                                       // initialization
                                                                       // state
    public static final String STATE_PAUSED_DESC = "paused"; // current playback
                                                             // is in the paused
                                                             // state
    public static final String STATE_PLAYING_DESC = "playing"; // current
                                                               // playback is in
                                                               // the playing
                                                               // state
    public static final String STATE_STARTING_DESC = "starting"; // current
                                                                 // playback is
                                                                 // in the
                                                                 // starting
                                                                 // playback
                                                                 // state
    public static final String STATE_STOPPED_DESC = "stopped"; // current
                                                               // playback is in
                                                               // the stopped
                                                               // state
    public static final String STATE_STOPPING_DESC = "stopping"; // current
                                                                 // playback is
                                                                 // in the
                                                                 // stopping
                                                                 // state
    public static final String STATE_WAITING_FOR_DATA_DESC = "waiting for data"; // current
                                                                                 // playback
                                                                                 // is
                                                                                 // in
                                                                                 // the
                                                                                 // waiting
                                                                                 // for
                                                                                 // audio
                                                                                 // data
                                                                                 // from
                                                                                 // the
                                                                                 // network
                                                                                 // state
    // current
    // playback
    // is
    // in
    // the
    // waiting
    // for
    // audio
    // data
    // to
    // fill
    // the
    // queue
    // state
    public static final String STATE_WAITING_FOR_QUEUE_DESC = "waiting for queue";

    public static final String EVENT_COMPLETE = "complete";
    public static final String EVENT_BUFFERING = "buffering";
    public static final String EVENT_ERROR = "error";
    public static final String EVENT_CHANGE = "change";
    public static final String EVENT_PROGRESS = "progress";

    public static final String EVENT_COMPLETE_JSON = "{ type : '"
            + EVENT_COMPLETE + "' }";


    protected Handler handler;

    private boolean paused = false;
    private boolean looping = false;

    protected KrollProxy proxy;
    protected MediaPlayer mp;
    protected float volume;
    protected boolean playOnResume;
    protected boolean remote;
    protected Timer progressTimer;

    private boolean pausePending = false;
    private boolean stopPending = false;
    private boolean playPending = false;
    private boolean prepareRequired = false;
    private boolean allowBackground = false;
    private WifiLock wifiLock;

    private WifiManager wifiManager;
    private AudioManager mAudioManager;
    
    MetadataEditorCompat currentMetadataEditor = null;
    
    /**
     * Enables the remote control client
     */
    private boolean mEnableLockscreenControls;

    // The volume we set the media player to when we lose audio focus, but are
    // allowed to reduce
    // the volume instead of stopping playback.
    public static final float DUCK_VOLUME = 0.1f;
    // our AudioFocusHelper object, if it's available (it's available on SDK
    // level >= 8)
    // If not available, this will be null. Always check for null before using!
    AudioFocusHelper mAudioFocusHelper = null;

    // do we have audio focus?
    enum AudioFocus {
        NoFocusNoDuck, // we don't have audio focus, and can't duck
        NoFocusCanDuck, // we don't have focus, but can play at a low volume
                        // ("ducking")
        Focused // we have full audio focus
    }

    AudioFocus mAudioFocus = AudioFocus.NoFocusNoDuck;
    // our RemoteControlClient object, which will use remote control APIs
    // available in
    // SDK level >= 14, if they're available.
    RemoteControlClientCompat mRemoteControlClientCompat;

    // The component name of MusicIntentReceiver, for use with media button and
    // remote control
    // APIs
    ComponentName mMediaButtonReceiverComponent;

    private int mState;

    @Override
    public void onMediaKey(KeyEvent key) {
        switch (key.getKeyCode()) {
        case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            if (isPlaying()) {
                pause();
            } else {
                play();
            }
            break;
        case KeyEvent.KEYCODE_MEDIA_PLAY:
            play();
            break;
        case KeyEvent.KEYCODE_HEADSETHOOK:
        case KeyEvent.KEYCODE_MEDIA_PAUSE:
            pause();
            break;
        case KeyEvent.KEYCODE_MEDIA_STOP:
            break;
        case KeyEvent.KEYCODE_MEDIA_NEXT:
            break;
        case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
            break;
        default:
            return;
        }
    }

    public TiSound(KrollProxy proxy, final boolean enableLockscreenControls) {
        this.proxy = proxy;
        this.playOnResume = false;
        this.remote = false;
        final Context context = TiApplication.getAppContext();
        
        wifiManager = (WifiManager) context
                .getSystemService(Context.WIFI_SERVICE);
        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL,
                "ti_audio_wifi_lock");

        // create the Audio Focus Helper, if the Audio Focus feature is
        // available (SDK 8 or above)
        if (android.os.Build.VERSION.SDK_INT >= 8)
            mAudioFocusHelper = new AudioFocusHelper(context, this);
        else
            mAudioFocus = AudioFocus.Focused; // no focus feature, so we always
                                              // "have" audio focus
        
        
        // Initialze the audio manager and register any headset controls for
        // playback
        mAudioManager = (AudioManager) context
                .getSystemService(Context.AUDIO_SERVICE);
        String packageName = TiApplication.getInstance().getPackageName();
        mMediaButtonReceiverComponent = new ComponentName(packageName,
                packageName + ".TiMediaButtonEventReceiver");
        MediaButtonHelper.registerMediaButtonEventReceiverCompat(mAudioManager, mMediaButtonReceiverComponent);

        // Use the remote control APIs (if available and the user allows it) to
        // set the playback state
        mEnableLockscreenControls = enableLockscreenControls;
        setUpRemoteControlClient();
    }

    

    
    public void setEnableLockscreenControls(final boolean enableLockscreenControls) {
        mEnableLockscreenControls = enableLockscreenControls;
        if (mEnableLockscreenControls) {
            setUpRemoteControlClient();
            // Update the controls according to the current playback
            setState(mState);
            updateMetadata();
        } else {
            // Remove then unregister the conrols
            mRemoteControlClientCompat
                    .setPlaybackState(RemoteControlClient.PLAYSTATE_STOPPED);
            RemoteControlHelper.unregisterRemoteControlClient(mAudioManager,
                    mRemoteControlClientCompat);
        }
    }

    void relaxResources(boolean releaseMediaPlayer) {
        // stop and release the Media Player, if it's available
        if (releaseMediaPlayer && mp != null) {
            mp.reset();
            mp.release();
            stopProgressTimer();
            mp.setOnCompletionListener(null);
            mp.setOnErrorListener(null);
            mp.setOnBufferingUpdateListener(null);
            mp.setOnInfoListener(null);
            mp = null;
            Log.d(TAG, "Native resources released.", Log.DEBUG_MODE);
            remote = false;
        }

        giveUpAudioFocus();
        releaseWifiLock();
    }

    public void release() {
        relaxResources(true);
        giveUpAudioFocus();
        RemoteControlHelper.unregisterRemoteControlClient(mAudioManager,
                mRemoteControlClientCompat);
        MediaButtonHelper.unregisterMediaButtonEventReceiverCompat(mAudioManager, mMediaButtonReceiverComponent);
    }

    void tryToGetAudioFocus() {
        if (mAudioFocus != AudioFocus.Focused && mAudioFocusHelper != null
                && mAudioFocusHelper.requestFocus())
            mAudioFocus = AudioFocus.Focused;
        MediaButtonHelper.registerMediaButtonEventReceiverCompat(mAudioManager, mMediaButtonReceiverComponent);
        AudioModule.widgetGetsFocused(this);
    }

    void giveUpAudioFocus() {
        if (mAudioFocus == AudioFocus.Focused && mAudioFocusHelper != null
                && mAudioFocusHelper.abandonFocus())
            mAudioFocus = AudioFocus.NoFocusNoDuck;
        AudioModule.widgetAbandonsFocused(this);
    }

    /**
     * Makes sure the media player exists and has been reset. This will create
     * the media player if needed, or reset the existing media player if one
     * already exists.
     */
    void createMediaPlayerIfNeeded() {
        if (mp == null) {
            mp = new MediaPlayer();

            // Make sure the media player will acquire a wake-lock while
            // playing. If we don't do
            // that, the CPU might go to sleep while the song is playing,
            // causing playback to stop.
            //
            // Remember that to use this, we have to declare the
            // android.permission.WAKE_LOCK
            // permission in AndroidManifest.xml.
            if (allowBackground) {
                setWakeMode(PowerManager.PARTIAL_WAKE_LOCK);
            }
            // we want the media player to notify us when it's ready preparing,
            // and when it's done
            // playing:
            mp.setOnCompletionListener(this);
            mp.setOnErrorListener(this);
            mp.setOnInfoListener(this);
            mp.setOnBufferingUpdateListener(this);
        } else
            mp.reset();
    }
    
    /**
     * Initializes the remote control client
     */
    private void setUpRemoteControlClient() {
        if (mEnableLockscreenControls) {
            if (mRemoteControlClientCompat == null) {
                final Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
                mediaButtonIntent.setComponent(mMediaButtonReceiverComponent);
                mRemoteControlClientCompat = new RemoteControlClientCompat(
                        PendingIntent.getBroadcast(TiApplication.getAppContext(), 0, mediaButtonIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT));
                RemoteControlHelper.registerRemoteControlClient(mAudioManager,
                        mRemoteControlClientCompat);
            }
            // Flags for the media transport control that this client supports.
            final int flags = RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS
                    | RemoteControlClient.FLAG_KEY_MEDIA_NEXT
                    | RemoteControlClient.FLAG_KEY_MEDIA_PLAY
                    | RemoteControlClient.FLAG_KEY_MEDIA_PAUSE
                    | RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE
                    | RemoteControlClient.FLAG_KEY_MEDIA_STOP;
            mRemoteControlClientCompat.setTransportControlFlags(flags);
        }
    }

    void playUrl(String url) {
        setState(STATE_STARTING);
        prepareRequired = false;
        relaxResources(false); // release everything except MediaPlayer
        createMediaPlayerIfNeeded();
        tryToGetAudioFocus();
        try {
            boolean isAsset = URLUtil.isAssetUrl(url);
            if (isAsset || url.startsWith("android.resource")) {
                Context context = TiApplication.getInstance();
                AssetFileDescriptor afd = null;
                try {
                    if (isAsset) {
                        String path = url.substring(TiConvert.ASSET_URL
                                .length());
                        afd = context.getAssets().openFd(path);
                    } else {
                        Uri uri = Uri.parse(url);
                        afd = context.getResources().openRawResourceFd(
                                TiRHelper.getResource("raw."
                                        + uri.getLastPathSegment()));
                    }
                    // Why mp.setDataSource(afd) doesn't work is a problem for
                    // another day.
                    // http://groups.google.com/group/android-developers/browse_thread/thread/225c4c150be92416
                    mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    mp.setDataSource(afd.getFileDescriptor(),
                            afd.getStartOffset(), afd.getLength());
                } catch (IOException e) {
                    Log.e(TAG, "Error setting file descriptor: ", e);
                } finally {
                    if (afd != null) {
                        afd.close();
                    }
                }
            } else {
                Uri uri = Uri.parse(url);
                if (uri.getScheme().equals(TiC.PROPERTY_FILE)) {
                    if (TiC.ICS_OR_GREATER) {
                        mp.setDataSource(uri.getPath());
                    } else {
                        // For 2.2 and below, MediaPlayer uses the native player
                        // which requires
                        // files to have worldreadable access, workaround is to
                        // open an input
                        // stream to the file and give that to the player.
                        FileInputStream fis = null;
                        try {
                            fis = new FileInputStream(uri.getPath());
                            mp.setDataSource(fis.getFD());
                        } catch (IOException e) {
                            Log.e(TAG, "Error setting file descriptor: ", e);
                        } finally {
                            if (fis != null) {
                                fis.close();
                            }
                        }
                    }
                } else {
                    remote = true;
                    mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    mp.setDataSource(url);
                }
            }

            String loop = TiConvert.toString(proxy
                    .getProperty(TiC.PROPERTY_LOOPING));
            if (loop != null) {
                looping = Boolean.parseBoolean(loop);
                mp.setLooping(looping);
            }

            if (remote) { // try async
                setState(STATE_STARTING);
                mp.setOnPreparedListener(this);
                Log.w(TAG, "prepareAsync from playUrl");
                mp.prepareAsync();
                playPending = true;
            } else {
                mp.prepare();
                setState(STATE_INITIALIZED);
                setVolume(volume);
                if (proxy.hasProperty(TiC.PROPERTY_TIME)) {
                    setTime(TiConvert.toInt(proxy
                            .getProperty(TiC.PROPERTY_TIME)));
                }
                startPlaying();
            }
            aquireWifiLock();

        } catch (Throwable t) {
            Log.w(TAG, "Issue while initializing : ", t);
            relaxResources(true);
        }
    }

    public boolean isLooping() {
        return looping;
    }

    public boolean isPaused() {
        return paused;
    }

    public boolean isPlaying() {
        boolean result = false;
        if (mp != null) {
            result = mp.isPlaying();
        }
        return result;
    }

    public void pause() {
        try {
            if (mp != null) {
                if (mp.isPlaying()) {
                    Log.d(TAG, "audio is playing, pause", Log.DEBUG_MODE);
                    stopProgressTimer();
                    if (mp.isPlaying()) mp.pause();
                    paused = true;
                    setState(STATE_PAUSED);
                    releaseWifiLock();
                } else if (playPending) {
                    pausePending = true;
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "Issue while pausing : ", t);
        }
    }

    public void play() {
        try {
            if (mp == null || prepareRequired || mState == STATE_STOPPED) {
                playUrl(TiConvert.toString(proxy.getProperty(TiC.PROPERTY_URL)));
            } else {
//                if (prepareRequired) {
//                    prepareAndPlay();
//                } else {
                    startPlaying();
//                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "Issue while playing : ", t);
            reset();
        }
    }

    private void prepareAndPlay() throws IllegalStateException, IOException {
        prepareRequired = false;
        if (remote) {
            playPending = true;
            mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.setOnPreparedListener(null);
                    mp.seekTo(0);
                    playPending = false;
                    if (!stopPending && !pausePending) {
                        startPlaying();
                    }
                    pausePending = false;
                    stopPending = false;
                }
            });
            Log.w(TAG, "prepareAsync from prepareAndPlay");
            mp.prepareAsync();
        } else {
            mp.prepare();
            mp.seekTo(0);
            startPlaying();
        }
    }

    public void reset() {
        try {
            if (mp != null && (mp.isPlaying() || isPaused())) {
                stopProgressTimer();

                setState(STATE_STOPPING);
                mp.seekTo(0);
                looping = false;
                paused = false;
                setState(STATE_STOPPED);
            }
        } catch (Throwable t) {
            Log.w(TAG, "Issue while resetting : ", t);
        }
    }

    public void setLooping(boolean loop) {
        try {
            if (loop != looping) {
                if (mp != null) {
                    mp.setLooping(loop);
                }
                looping = loop;
            }
        } catch (Throwable t) {
            Log.w(TAG, "Issue while configuring looping : ", t);
        }
    }

    public void setVolume(float volume) {
        try {
            if (mAudioFocus == AudioFocus.NoFocusCanDuck) {
                if (mp != null) {
                    mp.setVolume(DUCK_VOLUME, DUCK_VOLUME); // we'll be
                                                            // relatively quiet
                }
                return;
            }
            if (volume < 0.0f) {
                this.volume = 0.0f;
                Log.w(TAG,
                        "Attempt to set volume less than 0.0. Volume set to 0.0");
            } else if (volume > 1.0) {
                this.volume = 1.0f;
                proxy.setProperty(TiC.PROPERTY_VOLUME, volume);
                Log.w(TAG,
                        "Attempt to set volume greater than 1.0. Volume set to 1.0");
            } else {
                this.volume = volume; // Store in 0.0 to 1.0, scale when setting
                                      // hw
            }
            if (mp != null) {
                float scaledVolume = this.volume;
                 
//                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (int) (mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) * scaledVolume), 0);
                mp.setVolume(scaledVolume, scaledVolume);
            }
        } catch (Throwable t) {
            Log.w(TAG, "Issue while setting volume : ", t);
        }
    }

    public int getDuration() {
        int duration = 0;
        if (mp != null && !playPending) {
            duration = mp.getDuration(); // Can only get duration after the
                                         // media player is initialized.
        }
        return duration;
    }

    public int getTime() {
        int time = 0;

        if (mp != null) {
            time = mp.getCurrentPosition();
        } else {
            time = TiConvert.toInt(proxy.getProperty(TiC.PROPERTY_TIME));
        }

        return time;
    }

    public void setTime(int position) {
        if (position < 0) {
            position = 0;
        }

        if (mp != null) {
            int duration = mp.getDuration();
            if (position > duration) {
                position = duration;
            }

            try {
                mp.seekTo(position);
            } catch (IllegalStateException e) {
                Log.w(TAG,
                        "Error calling seekTo() in an incorrect state. Ignoring.");
            }
        }

        proxy.setProperty(TiC.PROPERTY_TIME, position);
    }

    private void setState(int state) {
        mState = state;
        proxy.setProperty("state", state);
        String stateDescription = "";
        int remoteState = 0;
        switch (state) {
        case STATE_BUFFERING:
            stateDescription = STATE_BUFFERING_DESC;
            break;
        case STATE_INITIALIZED:
            stateDescription = STATE_INITIALIZED_DESC;
            break;
        case STATE_PAUSED:
            remoteState = RemoteControlClient.PLAYSTATE_PAUSED;
            stateDescription = STATE_PAUSED_DESC;
            break;
        case STATE_PLAYING:
            remoteState = RemoteControlClient.PLAYSTATE_PLAYING;
            stateDescription = STATE_PLAYING_DESC;
            break;
        case STATE_STARTING:
            stateDescription = STATE_STARTING_DESC;
            break;
        case STATE_STOPPED:
            remoteState = RemoteControlClient.PLAYSTATE_STOPPED;
            stateDescription = STATE_STOPPED_DESC;
            break;
        case STATE_STOPPING:
            remoteState = RemoteControlClient.PLAYSTATE_STOPPED;
            stateDescription = STATE_STOPPING_DESC;
            break;
        case STATE_WAITING_FOR_DATA:
            stateDescription = STATE_WAITING_FOR_DATA_DESC;
            break;
        case STATE_WAITING_FOR_QUEUE:
            stateDescription = STATE_WAITING_FOR_QUEUE_DESC;
            break;
        }
        if (mRemoteControlClientCompat != null) {
            mRemoteControlClientCompat.setPlaybackState(remoteState);
        }

        proxy.setProperty("stateDescription", stateDescription);
        Log.d(TAG, "Audio state changed: " + stateDescription, Log.DEBUG_MODE);
        
        
        if (proxy.hasListeners("state", false)) {
            KrollDict data = new KrollDict();
            data.put("state", state);
            data.put("description", stateDescription);
            proxy.fireEvent("state", data, false, false);
        }
    }

    public void stop() {
        try {

            if (mp != null) {
                if (mp.isPlaying() || isPaused()) {
                    Log.d(TAG, "audio is playing, stop()", Log.DEBUG_MODE);
                    setState(STATE_STOPPING);
                    mp.stop();
                    setState(STATE_STOPPED);
                    stopProgressTimer();
                    prepareRequired = true;
                } else if (playPending) {
                    stopPending = true;
                }

                if (isPaused()) {
                    paused = false;
                }
            }
            giveUpAudioFocus();
            releaseWifiLock();
        } catch (Throwable t) {
            Log.e(TAG, "Error : ", t);
        }
    }

    public void onCompletion(MediaPlayer mp) {
        if (proxy.hasListeners(EVENT_COMPLETE, false)) {
            KrollDict data = new KrollDict();
            data.putCodeAndMessage(TiC.ERROR_CODE_NO_ERROR, null);
            proxy.fireEvent(EVENT_COMPLETE, data);
        }
        stop();
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        String msg = "Unknown media issue.";

        switch (what) {
        case MediaPlayer.MEDIA_INFO_BAD_INTERLEAVING:
            msg = "Stream not interleaved or interleaved improperly.";
            break;
        case MediaPlayer.MEDIA_INFO_NOT_SEEKABLE:
            msg = "Stream does not support seeking";
            break;
        case MediaPlayer.MEDIA_INFO_UNKNOWN:
            msg = "Unknown media issue";
            break;
        case MediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING:
            msg = "Video is too complex for decoder, video lagging."; // shouldn't
                                                                      // occur,
                                                                      // but
                                                                      // covering
                                                                      // bases.
            break;
        }

        if (proxy.hasListeners(EVENT_ERROR, false)) {
            KrollDict data = new KrollDict();
            data.putCodeAndMessage(TiC.ERROR_CODE_UNKNOWN, msg);
            proxy.fireEvent(EVENT_ERROR, data);
        }

        return true;
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        int code = what;
        if (what == 0) {
            code = -1;
        }
        String msg = "Unknown media error.";
        if (what == MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
            msg = "Media server died";
        }
        relaxResources(true);

        if (proxy.hasListeners(EVENT_ERROR, false)) {
            KrollDict data = new KrollDict();
            data.putCodeAndMessage(code, msg);
            data.put(TiC.PROPERTY_MESSAGE, msg);
            proxy.fireEvent(EVENT_ERROR, data);
        }

        return true;
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
//        Log.d(TAG, "Buffering: " + percent + "%", Log.DEBUG_MODE);
        if (proxy.hasListeners(EVENT_BUFFERING, false)) {
            KrollDict event = new KrollDict();
            event.put("progress", percent);
            proxy.fireEvent(EVENT_BUFFERING, event);
        }
    }

    private void startProgressTimer() {
        if (progressTimer == null) {
            progressTimer = new Timer(true);
        } else {
            progressTimer.cancel();
            progressTimer = new Timer(true);
        }

        progressTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    if (mp != null && mp.isPlaying()) {
                        if (proxy.hasListeners(EVENT_PROGRESS, false)) {
                            int position = mp.getCurrentPosition();
                            KrollDict event = new KrollDict();
                            event.put("progress", position);
                            proxy.fireEvent(EVENT_PROGRESS, event);
                        }
                    }
                } catch (Throwable t) {
                    Log.e(TAG, "Issue while progressTimer run: ", t);
                }
            }
        }, 1000, 1000);
    }

    private void stopProgressTimer() {
        if (progressTimer != null) {
            progressTimer.cancel();
            progressTimer = null;
        }
    }

    public void onDestroy() {
        if (mp != null) {
            // Before we stop, make sure that timer is stopped
            stopProgressTimer();
            mp.release();
            mp = null;
        }
        // TitaniumMedia clears out the references after onDestroy.
    }

    public void onPause() {
        if (mp != null) {
            if (isPlaying()) {
                pause();
                playOnResume = true;
            }
        }
    }

    public void onResume() {
        if (mp != null) {
            if (playOnResume) {
                play();
                playOnResume = false;
            }
        }
    }

    @Override
    public void listenerAdded(String type, int count, KrollProxy proxy) {
    }

    @Override
    public void listenerRemoved(String type, int count, KrollProxy proxy) {
    }
    
    public void propertySet(String key, Object newValue, Object oldValue,
            boolean changedProperty) {
        switch (key) {
        case TiC.PROPERTY_VOLUME:
            setVolume(TiConvert.toFloat(newValue, 1.0f));
            break;
        case TiC.PROPERTY_METADATA:
            updateMetadata((HashMap<String, Object>) newValue);
            break;
        default:
            break;
        }
    }

    @Override
    public void processProperties(KrollDict d) {
        for (Map.Entry<String, Object> entry : d.entrySet()) {
            propertySet(entry.getKey(), entry.getValue(), null, false);
        }
    }

    @Override
    public void processApplyProperties(KrollDict d) {
        for (Map.Entry<String, Object> entry : d.entrySet()) {
            propertySet(entry.getKey(), entry.getValue(), null, true);
        }
    }

    @Override
    public void propertyChanged(String key, Object oldValue, Object newValue,
            KrollProxy proxy) {
        propertySet(key, newValue, oldValue, true);
    }

    @Override
    public void propertiesChanged(List<KrollPropertyChange> changes,
            KrollProxy proxy) {
        for (KrollPropertyChange change : changes) {
            propertySet(change.getName(), change.getOldValue(),
                    change.getNewValue(), true);
        }
    }

    private void aquireWifiLock() {
        if (allowBackground && wifiLock != null && !wifiLock.isHeld()) {
            wifiLock.acquire();
        }
    }

    private void releaseWifiLock() {
        if (wifiLock != null && wifiLock.isHeld()) {
            wifiLock.release();
        }
    }

    public void setAllowBackground(final boolean value) {
        allowBackground = value;
        if (isPlaying()) {
            if (allowBackground) {
                setWakeMode(PowerManager.PARTIAL_WAKE_LOCK);
                aquireWifiLock();
            } else {
                setWakeMode(PowerManager.SCREEN_DIM_WAKE_LOCK);
                releaseWifiLock();
            }
        }
    }

    private void startPlaying() {
        if (mp != null) {
            if (!isPlaying() && !playPending) {
                Log.d(TAG, "audio is not playing, starting.", Log.DEBUG_MODE);
                Log.d(TAG, "Play: Volume set to " + volume, Log.DEBUG_MODE);
                if (mAudioFocus == AudioFocus.NoFocusNoDuck) {
                    tryToGetAudioFocus();
                }
                setVolume(volume);
                mp.start();
                paused = false;
                startProgressTimer();
            }
            setState(STATE_PLAYING);
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.setOnPreparedListener(null);
        setState(STATE_INITIALIZED);
        setVolume(volume);
        if (proxy.hasProperty(TiC.PROPERTY_TIME)) {
            setTime(TiConvert.toInt(proxy.getProperty(TiC.PROPERTY_TIME)));
        }
        playPending = false;
        if (!pausePending && !stopPending) {
            try {
                startPlaying();
            } catch (Throwable t) {
                Log.w(TAG, "Issue while playing : ", t);
                reset();
            }
        }
        pausePending = false;
        stopPending = false;
    }

    public void setWakeMode(final int mode) {
        mp.setWakeMode(TiApplication.getAppContext(), mode);
    }

    // / Updates the metadata on the lock screen
    private String loadingUrl = null;

    private class LoadLocalCoverArtTask extends
            AsyncTask<TiDrawableReference, Void, Void> {
        private Cache cache;
        private TiDrawableReference imageref;

        LoadLocalCoverArtTask(Cache cache) {
            this.cache = cache;
        }

        @Override
        protected Void doInBackground(TiDrawableReference... params) {
            imageref = params[0];
            String cacheKey = imageref.getCacheKey();
            Cache cache = TiApplication.getImageMemoryCache();
            Bitmap bitmap = (cacheKey != null) ? cache.get(cacheKey) : null;
            if (bitmap == null) {
                Drawable drawable = imageref.getDrawable();
                if (drawable instanceof BitmapDrawable) {
                    bitmap = ((BitmapDrawable) drawable).getBitmap();
                    cache.set(imageref.getUrl(), bitmap);
                }
            }
            
            loadingUrl = null;
            updateBitmapMetadata(bitmap);
            return null;
        }
    }

    static final HashMap<String, Integer> METADATAS = new HashMap<String, Integer>() {
        {
            put("album", MediaMetadataRetriever.METADATA_KEY_ALBUM);
            put("artist", MediaMetadataRetriever.METADATA_KEY_ARTIST);
            put("albumartist", MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST);
            put("title", MediaMetadataRetriever.METADATA_KEY_TITLE);
            put("author", MediaMetadataRetriever.METADATA_KEY_AUTHOR);
            put("compilation", MediaMetadataRetriever.METADATA_KEY_COMPILATION);
            put("composer", MediaMetadataRetriever.METADATA_KEY_COMPOSER);
            put("date", MediaMetadataRetriever.METADATA_KEY_DATE);
            put("genre", MediaMetadataRetriever.METADATA_KEY_GENRE);
        }
    };
    
    static final HashMap<String, Integer> METADATAS_LONG = new HashMap<String, Integer>() {
        {
            put("tracknumber", MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER);
            put("duration", MediaMetadataRetriever.METADATA_KEY_DURATION);
//            put("bitrate", MediaMetadataRetriever.METADATA_KEY_BITRATE);
            put("year", MediaMetadataRetriever.METADATA_KEY_YEAR);
        }
    };


    private void handleUpdateBitmapMetadata(final Bitmap bitmap) {
        //this needs to be called in a background thread because of the copy
        if (mRemoteControlClientCompat == null)
            return;
        Bitmap newBitmap = bitmap.copy(bitmap.getConfig(), bitmap.isMutable() ? true : false);
        if (currentMetadataEditor != null) {
            currentMetadataEditor.putBitmap(MetadataEditor.BITMAP_KEY_ARTWORK, newBitmap);
            return;
        }

        MetadataEditorCompat metadataEditor = mRemoteControlClientCompat
                .editMetadata(false);
        metadataEditor.putBitmap(MetadataEditor.BITMAP_KEY_ARTWORK, newBitmap);
        metadataEditor.apply();
    }

    private void updateMetadata(final HashMap<String, Object> dict) {
        if (mRemoteControlClientCompat == null)
            return;
        
        // for now we DONT call with "true". There is a recycle on the bitmap
        // which is bad news for when the bitmap is use somewhere else.
        //instead we update all datas :s
        currentMetadataEditor = mRemoteControlClientCompat
                .editMetadata(true);
        currentMetadataEditor.putBitmap(MetadataEditor.BITMAP_KEY_ARTWORK, null);
        for (Map.Entry<String, Object> entry : dict.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (METADATAS.containsKey(key)) {
                currentMetadataEditor.putString(METADATAS.get(key),
                        TiConvert.toString(value));
            } else if (METADATAS_LONG.containsKey(key)) {
                currentMetadataEditor.putLong(METADATAS_LONG.get(key),
                        TiConvert.toInt(value));
            }
        }
        if (!dict.containsKey("duration") && mp != null && mp.isPlaying()) {
            currentMetadataEditor.putLong(MediaMetadataRetriever.METADATA_KEY_DURATION, mp.getDuration());
        }

        TiDrawableReference imageref = TiDrawableReference.fromObject(proxy,
                dict.get("artwork"));
        if (!imageref.isTypeNull()) {
            TiImageHelper.downloadDrawable(proxy, imageref, true, this);
        }
        currentMetadataEditor.apply();
        currentMetadataEditor = null;
    }
    
    public void updateMetadata() {
        if (proxy.hasProperty(TiC.PROPERTY_METADATA)) {
            updateMetadata((HashMap<String, Object>) proxy
                    .getProperty(TiC.PROPERTY_METADATA));
        }
    }

    private void updateBitmapMetadata(final Bitmap bitmap) {
        (new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] objects) {
                handleUpdateBitmapMetadata(bitmap);
                return null;
            }
        }).execute();
    }

    @Override
    public void onBitmapLoaded(Bitmap bitmap, LoadedFrom from) {
        loadingUrl = null;
        updateBitmapMetadata(bitmap);
    }
    
    @Override
    public void onDrawableLoaded(Drawable drawable, LoadedFrom from) {
        if (drawable instanceof BitmapDrawable) {
            onBitmapLoaded(((BitmapDrawable) drawable).getBitmap(), from);
        }        
    }

    @Override
    public void onBitmapFailed(Drawable errorDrawable) {
        loadingUrl = null;
        updateBitmapMetadata(null);
    }

    @Override
    public void onPrepareLoad(Drawable placeHolderDrawable) {
    }

    @Override
    public void onGainedAudioFocus() {
        mAudioFocus = AudioFocus.Focused;
        // restart media player with new focus settings
        if (mp != null && mState == STATE_PLAYING) {
            setVolume(volume);
            if (!paused && !mp.isPlaying()) {
                mp.start();
            }
        }
    }

    @Override
    public void onLostAudioFocus(boolean canDuck) {
        mAudioFocus = canDuck ? AudioFocus.NoFocusCanDuck
                : AudioFocus.NoFocusNoDuck;

        // start/restart/pause media player with new focus settings
        if (mp != null && mp.isPlaying()) {
            if (mAudioFocus == AudioFocus.NoFocusNoDuck) {
                pause();
            }
            setVolume(volume);
        }
    }

}
