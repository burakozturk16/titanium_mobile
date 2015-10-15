package ti.modules.titanium.audio.streamer;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiEnhancedService;
import org.appcelerator.titanium.TiApplication.AppStateListener;
import org.appcelerator.titanium.proxy.TiEnhancedServiceProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiImageHelper;
import org.appcelerator.titanium.util.TiImageHelper.TiDrawableTarget;
import org.appcelerator.titanium.view.TiDrawableReference;

import ti.modules.titanium.audio.AudioFocusHelper;
import ti.modules.titanium.audio.AudioModule;
import ti.modules.titanium.audio.BasePlayerProxy;
import ti.modules.titanium.audio.FocusableAudioWidget;
import ti.modules.titanium.audio.MediaButtonHelper;
import ti.modules.titanium.audio.RemoteControlClientCompat;
import ti.modules.titanium.audio.RemoteControlHelper;
import ti.modules.titanium.audio.RemoteControlClientCompat.MetadataEditorCompat;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.RemoteControlClient;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.RemoteControlClient.MetadataEditor;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.Handler.Callback;
import android.os.PowerManager.WakeLock;
import android.view.KeyEvent;

import com.squareup.picasso.Cache;
import com.squareup.picasso.Target;
import com.squareup.picasso.Picasso.LoadedFrom;

@SuppressWarnings({ "deprecation", "serial" })
public abstract class AudioService extends TiEnhancedService implements TiDrawableTarget,
        Callback, AppStateListener, FocusableAudioWidget {
    private static final String TAG = "AudioService";

    public static final String CMDNAME = "command";

    public static final String CMDTOGGLEPAUSE = "togglepause";

    public static final String CMDSTOP = "stop";

    public static final String CMDPAUSE = "pause";

    public static final String CMDPLAY = "play";

    public static final String CMDPREVIOUS = "previous";

    public static final String CMDNEXT = "next";

    public static final String CMDNOTIF = "buttonId";

    protected abstract class AudioPlayer {
        protected Handler mHandler;
        protected Timer mProgressTimer;
        protected boolean mIsInitialized = false;
        protected boolean mIsPreparing = false;
        protected final WeakReference<AudioService> mService;
        
        public AudioPlayer(final AudioService service) {
            mService = new WeakReference<AudioService>(service);
        }
        
        protected AudioService getService() {
            return mService.get();
        }
        
        protected void setState(final int state) {
            getService().setState(state);
        }

        protected abstract void setVolume(float mCurrentVolume);

        public void setHandler(Handler handler) {            
            mHandler = handler;
        }

        protected abstract void release();

        public boolean isInitialized() {
            return mIsInitialized;
        }
        
        public boolean isPrepared() {
            return mIsInitialized && !mIsPreparing;
        }
        
        protected abstract void stop();

        protected abstract boolean openFile(Object object, boolean b);

        protected abstract void setAudioSessionId(int sessionId);

        protected abstract int getAudioSessionId();

        protected abstract long duration();

        protected abstract long seek(long position);

        protected abstract long position();

        protected abstract boolean isPlaying();

        protected abstract boolean isPaused();

        protected abstract void pause();

        protected abstract void start();
        
        protected void startProgressTimer() {
            if (mProgressTimer == null) {
                mProgressTimer = new Timer(true);
            } else {
                mProgressTimer.cancel();
                mProgressTimer = new Timer(true);
            }
            if (isPlaying()) {
                getService().onProgress();
            }
            mProgressTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        if (isPlaying()) {
                            mService.get().onProgress();
                        }
                    } catch (Throwable t) {
                        Log.e(TAG, "Issue while progressTimer run: ", t);
                    }
                }
            }, 1000, 1000);
        }

        protected void stopProgressTimer() {
            if (mProgressTimer != null) {
                mProgressTimer.cancel();
                mProgressTimer = null;
            }
        }
    }

    protected interface BasePlayingItem {

    }

    /**
     * Indicates when the track ends
     */
    protected static final int TRACK_ENDED = 1;

    /**
     * Indicates that the current track was changed the next track
     */
    protected static final int TRACK_WENT_TO_NEXT = 2;

    /**
     * Indicates when the release the wake lock
     */
    protected static final int RELEASE_WAKELOCK = 3;

    /**
     * Indicates the player died
     */
    protected static final int SERVER_DIED = 4;

    /**
     * Indicates some sort of focus change, maybe a phone call
     */
    protected static final int FOCUSCHANGE = 5;

    /**
     * Indicates to fade the volume down
     */
    protected static final int FADEDOWN = 6;

    /**
     * Indicates to fade the volume back up
     */
    protected static final int FADEUP = 7;

    /**
     * Idle time before stopping the foreground notfication (1 minute)
     */
    protected static final int IDLE_DELAY = 60000;

    /**
     * The max size allowed for the track history
     */
    protected static final int MAX_HISTORY_SIZE = 100;

    private static final int MSG_FIRST_ID = 100;
    protected static final int MSG_PICASSO_REQUEST = MSG_FIRST_ID + 1;
    protected static final int MSG_UPDATE_BITMAP_METADATA = MSG_FIRST_ID + 2;

    /**
     * Keeps a mapping of the track history
     */
    // private static final LinkedList<Integer> mHistory = new
    // LinkedList<Integer>();

    /**
     * Used to shuffle the tracks
     */
    // private static final Shuffler mShuffler = new Shuffler();

    /**
     * 4x1 widget
     */
    protected AudioStreamerAppWidgetInterface mAppWidgetSmall;

    /**
     * 4x2 widget
     */
    protected AudioStreamerAppWidgetInterface mAppWidgetLarge;

    /**
     * 4x2 alternate widget
     */
    protected AudioStreamerAppWidgetInterface mAppWidgetLargeAlternate;

    /**
     * The media player
     */
    protected AudioPlayer mPlayer;

    // /**
    // * The path of the current file to play
    // */
    // private String mFileToPlay;

    /**
     * Keeps the service running when the screen is off
     */
    protected WakeLock mWakeLock;

    protected Object mCursor;

    /**
     * Monitors the audio state
     */
    protected AudioManager mAudioManager;
    protected WifiManager mWifiManager;
    protected WifiLock mWifiLock;

    /**
     * Used to know when the service is active
     */
    protected boolean mServiceInUse = false;

    /**
     * Used to know if something should be playing or not
     */
    protected boolean mIsSupposedToBePlaying = false;

    /**
     * Used to track what type of audio focus loss caused the playback to pause
     */
    protected boolean mPausedByTransientLossOfFocus = false;

    protected boolean mBuildNotification = false;

    protected boolean mForcedTrackToEnd = false;

    public static final float DUCK_VOLUME = 0.1f;
    // our AudioFocusHelper object, if it's available (it's available on SDK
    // level >= 8)
    // If not available, this will be null. Always check for null before using!
    protected AudioFocusHelper mAudioFocusHelper = null;

    // do we have audio focus?
    protected enum AudioFocus {
        NoFocusNoDuck, // we don't have audio focus, and can't duck
        NoFocusCanDuck, // we don't have focus, but can play at a low volume
                        // ("ducking")
        Focused // we have full audio focus
    }

    protected AudioFocus mAudioFocus = AudioFocus.NoFocusNoDuck;

    /**
     * Lock screen controls ICS+
     */
    protected RemoteControlClientCompat mRemoteControlClientCompat;

    /**
     * Enables the remote control client
     */
    protected boolean mEnableLockscreenControls = false;
    protected boolean mEnableNotification = false;

    protected ComponentName mMediaButtonReceiverComponent;

    // private int mPlayListLen = 0;

    protected int mPlayPos = -1;

    protected int mNextPlayPos = -1;

    protected int mOpenFailedCounter = 0;

    // private int mMediaMountedCount = 0;

    protected int mShuffleMode = AudioModule.SHUFFLE_NONE;

    protected int mRepeatMode = AudioModule.REPEAT_NONE;

    protected int mServiceStartId = -1;

    protected List<Object> mQueue = null;
    protected List<Object> mOriginalQueue = null;

    protected MusicPlayerHandler mPlayerHandler;

    protected DelayedHandler mDelayedStopHandler;

    protected boolean mPausePending = false;
    protected boolean mStopPending = false;
    protected boolean mPlayPending = false;

    public static final String STATE_WAITING_FOR_QUEUE_DESC = "waiting for queue";

    
    public final class Events {
        public static final String COMPLETE = "complete";
        public static final String BUFFERING = "buffering";
        public static final String ERROR = "error";
        public static final String STATE = "state";
        public static final String CHANGE = "change";
        public static final String PROGRESS = "progress";
        public static final String END = "end";
    }

    // buffering from the network
    // state

    public final class State {
        public static final int STATE_BUFFERING = 0; // current playback is in the
        public static final int STATE_INITIALIZED = 1;
        public static final int STATE_PAUSED = 2;
        public static final int STATE_PLAYING = 3;
        public static final int STATE_STARTING = 4;
        public static final int STATE_STOPPED = 5;
        public static final int STATE_STOPPING = 6;
        public static final int STATE_WAITING_FOR_DATA = 7;
        public static final int STATE_WAITING_FOR_QUEUE = 8;
        public static final int STATE_CONNECTING = 9;
    }

    public static final HashMap<Integer, String> STATE_DESC = new HashMap<Integer, String>() {
        {
            put(State.STATE_BUFFERING, "buffering");
            put(State.STATE_INITIALIZED, "initialized");
            put(State.STATE_PAUSED, "paused");
            put(State.STATE_PLAYING, "playing");
            put(State.STATE_STARTING, "starting");
            put(State.STATE_STOPPED, "stopped");
            put(State.STATE_STOPPING, "stopping");
            put(State.STATE_WAITING_FOR_DATA, "waiting for data");
            put(State.STATE_WAITING_FOR_QUEUE, "waiting for queue");
            put(State.STATE_CONNECTING, "connecting");
        }
    };
    
    public static final HashMap<Integer, Integer> REMOTE_STATE = new HashMap<Integer, Integer>() {
        {
            put(State.STATE_BUFFERING, RemoteControlClient.PLAYSTATE_BUFFERING);
            put(State.STATE_PAUSED, RemoteControlClient.PLAYSTATE_PAUSED);
            put(State.STATE_PLAYING, RemoteControlClient.PLAYSTATE_PLAYING);
            put(State.STATE_STOPPED, RemoteControlClient.PLAYSTATE_STOPPED);
            put(State.STATE_STOPPING, RemoteControlClient.PLAYSTATE_STOPPED);
        }
    };
    

    protected float mVolume = 1.0f;
    protected boolean inBackground = false;

    protected List<Intent> mPendingCommandIntents = new ArrayList<Intent>();
    protected boolean mIgnoreStateChange = false;
    /**
     * Used to build the notification
     */
    protected NotificationHelper mNotificationHelper;
    protected Handler uiHandler;
    protected MetadataEditorCompat currentMetadataEditor = null;

    /**
     * {@inheritDoc}
     */
    @Override
    public IBinder onBind(final Intent intent) {
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        mServiceInUse = true;
        return super.onBind(intent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onUnbind(final Intent intent) {
        mServiceInUse = false;

        if (mIsSupposedToBePlaying || mPausedByTransientLossOfFocus) {
            // Something is currently playing, or will be playing once
            // an in-progress action requesting audio focus ends, so don't stop
            // the service now.
            return true;

            // If there is a playlist but playback is paused, then wait a while
            // before stopping the service, so that pause/resume isn't slow.
            // Also delay stopping the service if we're transitioning between
            // tracks.
        } else if ((mQueue != null && mQueue.size() > 0)
                || mPlayerHandler.hasMessages(TRACK_ENDED)) {
            final Message msg = mDelayedStopHandler.obtainMessage();
            mDelayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY);
            return true;
        }
        return super.onUnbind(intent);
    }

    protected KrollDict getTrackEvent() {
        KrollDict data = new KrollDict();
        data.put("track", mCursor);
        data.put(TiC.PROPERTY_DURATION, duration());
        data.put(TiC.PROPERTY_INDEX, mPlayPos);
        return data;
    }

    public void onError(final int what, final String msg) {
        if (proxyHasListeners(Events.ERROR, false)) {
            KrollDict data = getTrackEvent();
            data.putCodeAndMessage(what, msg);
            proxy.fireEvent(Events.ERROR, data, false, false);
        }
    }

    private long mCurrentBufferingPosition = -1;

    public void onBufferingUpdate(long position, int percent) {
        if (position <= 0 || mCurrentBufferingPosition == position)
            return;
        mCurrentBufferingPosition = position;
        if (proxyHasListeners(Events.BUFFERING, false)) {
            KrollDict data = getTrackEvent();
            if (position != -1) {
                data.put(TiC.PROPERTY_POSITION, position);
            }
            data.put(TiC.PROPERTY_PROGRESS, percent);
            proxy.fireEvent(Events.BUFFERING, data, false, false);
        }
    }

    protected boolean proxyHasListeners(final String type,
            final boolean checkParent) {
        return proxy != null && proxy.hasListeners(type, checkParent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRebind(final Intent intent) {
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        mServiceInUse = true;
    }

    @Override
    protected void start() {
        super.start();
        if (mPendingCommandIntents.size() > 0) {
            List<Intent> toHandle = new ArrayList<Intent>(
                    mPendingCommandIntents);
            mPendingCommandIntents.clear();
            for (int i = 0; i < toHandle.size(); i++) {
                handleIntent(toHandle.get(i));
            }
        }
    }

    protected BasePlayerProxy getPlayerProxy() {
        return ((BasePlayerProxy) proxy);
    }

    @Override
    protected void bindToProxy(final TiEnhancedServiceProxy proxy) {
        super.bindToProxy(proxy);
        if (this.proxy != null) {
            BasePlayerProxy playerProxy = getPlayerProxy();
            setShuffleMode(TiConvert.toInt(
                    playerProxy.getProperty(TiC.PROPERTY_SHUFFLE_MODE),
                    mShuffleMode));
            setRepeatMode(TiConvert.toInt(
                    playerProxy.getProperty(TiC.PROPERTY_REPEAT_MODE),
                    mRepeatMode));
            

            mEnableNotification = playerProxy
                    .getEnableNotification();
            // Initialze the notification helper
            mNotificationHelper = new NotificationHelper(this,
                    playerProxy.getNotificationIcon(),
                    playerProxy.getNotificationViewId(),
                    playerProxy.getNotificationExtandedViewId());
            
            // Use the remote control APIs (if available and the user allows it)
            // to
            // set the playback state
            mEnableLockscreenControls = playerProxy
                    .getEnableLockscreenControls();
            setUpRemoteControlClient(playerProxy);

            setVolume(TiConvert.toFloat(proxy.getProperty(TiC.PROPERTY_VOLUME),
                    1.0f));
            if (mStarted) {
                // we were already started
                if (mState == State.STATE_PLAYING) {
                    buildNotification();
                }
            }
        }
    }

    protected class Commands {
        public final String PLAYSTATE_CHANGED;
        public final String META_CHANGED;
        public final String QUEUE_CHANGED;
        public final String REPEATMODE_CHANGED;
        public final String SHUFFLEMODE_CHANGED;
        public final String SERVICECMD;
        public final String TOGGLEPAUSE_ACTION;
        public final String PAUSE_ACTION;
        public final String STOP_ACTION;
        public final String PREVIOUS_ACTION;
        public final String NEXT_ACTION;
        public final String REPEAT_ACTION;
        public final String SHUFFLE_ACTION;
        public final String KILL_FOREGROUND;
        public final String REFRESH;
        public final String START_BACKGROUND;
        public final String UPDATE_LOCKSCREEN;

        public Commands(String cmdPrefix) {
            PLAYSTATE_CHANGED = cmdPrefix + "playstatechanged";
            META_CHANGED = cmdPrefix + "metachanged";
            QUEUE_CHANGED = cmdPrefix + "queuechanged";
            REPEATMODE_CHANGED = cmdPrefix + "repeatmodechanged";
            SHUFFLEMODE_CHANGED = cmdPrefix + "shufflemodechanged";
            SERVICECMD = cmdPrefix + "musicservicecommand";
            TOGGLEPAUSE_ACTION = cmdPrefix + "togglepause";
            PAUSE_ACTION = cmdPrefix + "pause";
            STOP_ACTION = cmdPrefix + "stop";
            PREVIOUS_ACTION = cmdPrefix + "previous";
            NEXT_ACTION = cmdPrefix + "next";
            REPEAT_ACTION = cmdPrefix + "repeat";
            SHUFFLE_ACTION = cmdPrefix + "shuffle";
            KILL_FOREGROUND = cmdPrefix + "killforeground";
            REFRESH = cmdPrefix + "refresh";
            START_BACKGROUND = cmdPrefix + "startbackground";
            UPDATE_LOCKSCREEN = cmdPrefix + "updatelockscreen";
        }
    }

    protected static String getCmdPrefix() {
        return "akylas.triton.player";
    }

    public static Commands cmds = null;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate() {
        if (cmds == null) {
            cmds = new Commands(getCmdPrefix());
        }

        super.onCreate();
        TiApplication.addAppStateListener(this);

        // Start up the thread running the service. Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block. We also make it
        // background priority so CPU-intensive work will not disrupt the UI.
        final HandlerThread thread = new HandlerThread("MusicPlayerHandler",
                android.os.Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // Initialize the handlers
        mPlayerHandler = new MusicPlayerHandler(this, thread.getLooper());
        mDelayedStopHandler = new DelayedHandler(this);

        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        mWifiLock = mWifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL,
                "ti_audiostreamer_wifi_lock");

        // create the Audio Focus Helper, if the Audio Focus feature is
        // available (SDK 8 or above)
        if (android.os.Build.VERSION.SDK_INT < 8)
            mAudioFocus = AudioFocus.Focused; // no focus feature, so we always
                                              // "have" audio focus

        // Initialze the audio manager and register any headset controls for
        // playback
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        String packageName = TiApplication.getInstance().getPackageName();
        mMediaButtonReceiverComponent = new ComponentName(packageName,
                packageName + ".TiMediaButtonEventReceiver");
        MediaButtonHelper.registerMediaButtonEventReceiverCompat(mAudioManager,
                mMediaButtonReceiverComponent);

        uiHandler = new Handler(Looper.getMainLooper(), this);

        // Initialze the media player
        mPlayer = createPlayer(this);
        mPlayer.setHandler(mPlayerHandler);

        // Initialze the intent filter and each action
        final IntentFilter filter = new IntentFilter();
        filter.addAction(cmds.SERVICECMD);
        filter.addAction(cmds.TOGGLEPAUSE_ACTION);
        filter.addAction(cmds.PAUSE_ACTION);
        filter.addAction(cmds.STOP_ACTION);
        filter.addAction(cmds.NEXT_ACTION);
        filter.addAction(cmds.PREVIOUS_ACTION);
        filter.addAction(cmds.REPEAT_ACTION);
        filter.addAction(cmds.SHUFFLE_ACTION);
        filter.addAction(cmds.KILL_FOREGROUND);
        filter.addAction(cmds.START_BACKGROUND);
        filter.addAction(cmds.UPDATE_LOCKSCREEN);
        // Attach the broadcast listener
        registerReceiver(mIntentReceiver, filter);

        // Initialize the wake lock
        final PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                getClass().getName());
        mWakeLock.setReferenceCounted(false);

        // Listen for the idle state
        final Message message = mDelayedStopHandler.obtainMessage();
        mDelayedStopHandler.sendMessageDelayed(message, IDLE_DELAY);

    }

    protected AudioPlayer createPlayer(AudioService audioService) {
        return null;
    }

    /**
     * Initializes the remote control client
     * 
     * @param playerProxy
     */
    private void setUpRemoteControlClient(BasePlayerProxy playerProxy) {
        if (mEnableLockscreenControls) {
            if (mRemoteControlClientCompat == null) {
                final Intent mediaButtonIntent = new Intent(
                        Intent.ACTION_MEDIA_BUTTON);
                mediaButtonIntent.setComponent(mMediaButtonReceiverComponent);
                mRemoteControlClientCompat = new RemoteControlClientCompat(
                        PendingIntent.getBroadcast(getApplicationContext(), 0,
                                mediaButtonIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT));
                RemoteControlHelper.registerRemoteControlClient(mAudioManager,
                        mRemoteControlClientCompat);
            }
            int flags = 0;
            Object buttons = playerProxy.getProperty("lockscreenButtons");
            if (buttons instanceof Object[]) {
                Object[] array = (Object[]) buttons;
                for (int i = 0; i < array.length; i++) {
                    String button = TiConvert.toString(array[i]);
                    switch (button) {
                    case "stop":
                        flags |= RemoteControlClient.FLAG_KEY_MEDIA_STOP;
                        break;
                    case "play":
                        flags |= RemoteControlClient.FLAG_KEY_MEDIA_PLAY;
                        break;
                    case "playpause":
                        flags |= RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE;
                        break;
                    case "next":
                        flags |= RemoteControlClient.FLAG_KEY_MEDIA_NEXT;
                        break;
                    case "previous":
                        flags |= RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS;
                        break;
                    case "pause":
                        flags |= RemoteControlClient.FLAG_KEY_MEDIA_PAUSE;
                        break;
                    default:
                        break;
                    }
                }
            } else {
                flags = RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS
                        | RemoteControlClient.FLAG_KEY_MEDIA_NEXT
                        | RemoteControlClient.FLAG_KEY_MEDIA_PLAY
                        | RemoteControlClient.FLAG_KEY_MEDIA_PAUSE
                        | RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE
                        | RemoteControlClient.FLAG_KEY_MEDIA_STOP;
            }
            mRemoteControlClientCompat.setTransportControlFlags(flags);
        }
    }

    protected void closeCursor() {
        if (mCursor != null) {
            mCursor = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDestroy() {
        TiApplication.removeAppStateListener(this);
        super.onDestroy();

        // Release the player
        mPlayer.release();
        mPlayer = null;

        unregisterReceiver(mIntentReceiver);

        // Remove the audio focus listener and lock screen controls
        giveUpAudioFocus();

        RemoteControlHelper.unregisterRemoteControlClient(mAudioManager,
                mRemoteControlClientCompat);

        // Remove any callbacks from the handlers
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        mPlayerHandler.removeCallbacksAndMessages(null);

        closeCursor();
        // Release the wake lock
        mWakeLock.release();

    }

    private void handleIntent(final Intent intent) {
        if (!mStarted) {
            mPendingCommandIntents.add(intent);
            return;
        }
        if (intent != null) {
            final String action = intent.getAction();
            final String command = intent.getStringExtra("command");
            if (CMDNEXT.equals(command) || cmds.NEXT_ACTION.equals(action)) {
                next();
            } else if (CMDPREVIOUS.equals(command)
                    || cmds.PREVIOUS_ACTION.equals(action)) {
                if (position() < 2000) {
                    prev();
                } else {
                    seek(0);
                    play();
                }
            } else if (CMDTOGGLEPAUSE.equals(command)
                    || cmds.TOGGLEPAUSE_ACTION.equals(action)) {
                if (mIsSupposedToBePlaying) {
                    pause();
                    mPausedByTransientLossOfFocus = false;
                } else {
                    play();
                }
            } else if (CMDPAUSE.equals(command)
                    || cmds.PAUSE_ACTION.equals(action)) {
                pause();
                mPausedByTransientLossOfFocus = false;
            } else if (CMDPLAY.equals(command)) {
                play();
            } else if (CMDSTOP.equals(command)
                    || cmds.STOP_ACTION.equals(action)) {
                pause();
                mPausedByTransientLossOfFocus = false;
                seek(0);
                setState(State.STATE_STOPPED);
                mBuildNotification = false;
            } else if (cmds.REPEAT_ACTION.equals(action)) {
                cycleRepeat();
            } else if (cmds.SHUFFLE_ACTION.equals(action)) {
                cycleShuffle();
            } else if (cmds.KILL_FOREGROUND.equals(action)) {
                mBuildNotification = false;
                killNotification();
            } else if (cmds.START_BACKGROUND.equals(action)) {
                mBuildNotification = true;
                buildNotification();
            } else if (cmds.UPDATE_LOCKSCREEN.equals(action)) {
                mEnableLockscreenControls = intent.getBooleanExtra(
                        cmds.UPDATE_LOCKSCREEN, true);
                if (mEnableLockscreenControls) {
                    setUpRemoteControlClient(getPlayerProxy());
                    // Update the controls according to the current playback
                    notifyChange(cmds.PLAYSTATE_CHANGED);
                    notifyChange(cmds.META_CHANGED);
                } else {
                    // Remove then unregister the conrols
                    mRemoteControlClientCompat
                            .setPlaybackState(RemoteControlClient.PLAYSTATE_STOPPED);
                    RemoteControlHelper.unregisterRemoteControlClient(
                            mAudioManager, mRemoteControlClientCompat);
                }
            } else if (mAppWidgetSmall != null
                    && mAppWidgetSmall.getWidgetUpdateId().equals(command)) {
                final int[] small = intent
                        .getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
                mAppWidgetSmall.performUpdate(AudioService.this, small);
            } else if (mAppWidgetLarge != null
                    && mAppWidgetLarge.getWidgetUpdateId().equals(command)) {
                final int[] large = intent
                        .getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
                mAppWidgetLarge.performUpdate(AudioService.this, large);
            } else if (mAppWidgetLargeAlternate != null
                    && mAppWidgetLargeAlternate.getWidgetUpdateId().equals(
                            command)) {
                final int[] largeAlt = intent
                        .getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
                mAppWidgetLargeAlternate.performUpdate(AudioService.this,
                        largeAlt);
            }
        }

    }

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
            stop();
            break;
        case KeyEvent.KEYCODE_MEDIA_NEXT:
            next();
            break;
        case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
            prev();
            break;
        default:
            return;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int onStartCommand(final Intent intent, final int flags,
            final int startId) {
        mServiceStartId = startId;

        int result = super.onStartCommand(intent, flags, startId);
        mDelayedStopHandler.removeCallbacksAndMessages(null);

        handleIntent(intent);
        // Make sure the service will shut down on its own if it was
        // just started but not bound to and nothing is playing
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        final Message msg = mDelayedStopHandler.obtainMessage();
        mDelayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY);
        return result;
    }

    /**
     * Builds the notification
     */
    public void buildNotification() {
        if (mEnableNotification && (mBuildNotification || inBackground)) {
            try {
                mNotificationHelper.buildNotificationIfNeeded();
                // updateMetadata();
            } catch (final IllegalStateException parcelBitmap) {
                parcelBitmap.printStackTrace();
            }
        }
    }

    /**
     * Removes the foreground notification
     */
    public void killNotification() {
        stopForeground(true);
        mNotificationHelper.hideNotification();
    }

    /**
     * Stops playback
     * 
     * @param remove_status_icon
     *            True to go to the idle state, false otherwise
     */
    private void stop(final boolean remove_status_icon) {
        
        if (mPlayPending) {
            mStopPending = true;
            mPlayPending = false;
            return;
        }
        if (mPlayer.isInitialized()) {
            mPlayer.stop();
        } else {
            setState(State.STATE_STOPPED);
        }
        closeCursor();
        stopForeground(remove_status_icon);
        if (remove_status_icon) {
            mIsSupposedToBePlaying = false;
            giveUpAudioFocus();
        }
        if (isStopped()) {
            return;
        }
        setState(State.STATE_STOPPING);
    }

    /**
     * Removes the range of tracks specified from the play list. If a file
     * within the range is the file currently being played, playback will move
     * to the next file after the range.
     * 
     * @param first
     *            The first file to be removed
     * @param last
     *            The last file to be removed
     * @return the number of tracks deleted
     */
    private int removeTracksInternal(int first, int last) {
        synchronized (this) {
            int currentLength = getPlaylistSize();
            if (last < first) {
                return 0;
            } else if (first < 0) {
                first = 0;
            } else if (last >= currentLength) {
                last = currentLength - 1;
            }

            int newPos = mPlayPos;

            for (int i = first; i <= last; i++) {
                Object removed = mOriginalQueue.remove(i);
                if (mQueue.remove(removed)) {
                    newPos++;
                }
            }
            currentLength = getPlaylistSize();
            if (newPos != mPlayPos) {
                if (currentLength == 0) {
                    stop(true);
                    mPlayPos = -1;
                } else {
                    if (newPos >= currentLength) {
                        mPlayPos = 0;
                    } else {
                        mPlayPos = newPos;
                    }
                    final boolean wasPlaying = isPlaying();
                    stop(false);
                    openCurrentAndNext();
                    if (wasPlaying) {
                        play();
                    }
                }
                // notifyChange(META_CHANGED);
            }
            return last - first + 1;
        }
    }

    /**
     * Adds a list to the playlist
     * 
     * @param list
     *            The list to add
     * @param position
     *            The position to place the tracks
     */
    public void removeFromPlayList(final List<Object> list) {
        for (int i = 0; i < list.size(); i++) {
            removeTrack(list.get(i));
        }
    }

    private int getPlaylistSize() {
        if (mOriginalQueue != null) {
            return mOriginalQueue.size();
        }
        return 0;
    }

    private int getQueueSize() {
        if (mQueue != null) {
            return mQueue.size();
        }
        return 0;
    }

    /**
     * Adds a list to the playlist
     * 
     * @param list
     *            The list to add
     * @param position
     *            The position to place the tracks
     */
    public void addToPlayList(final List<Object> list, int position) {
        int currentLength = getPlaylistSize();
        if (position < 0) {
            position = 0;
        }
        if (position > currentLength) {
            position = currentLength;
        }

        boolean needsSorting = false;
        if (mOriginalQueue == null) {
            mOriginalQueue = new ArrayList<Object>();
            needsSorting = true;
        }

        mOriginalQueue.addAll(position, list);
        if (needsSorting) {
            createCurrentQueue();
        } else {
            mQueue.addAll(position, list);
        }
        currentLength = getPlaylistSize();
        if (currentLength == 0) {
            closeCursor();
            // notifyChange(META_CHANGED);
        } else {
            notifyChange(cmds.QUEUE_CHANGED);
        }
    }

    /**
     * Called to open a new file as the current track and prepare the next for
     * playback
     */
    private boolean openCurrentAndNext() {
        return openCurrentAndMaybeNext(true);
    }
    
    private void setIsSupposeToBePlaying(final boolean newValue) {
        if (mIsSupposedToBePlaying != newValue) {
            mIsSupposedToBePlaying = newValue;
//            notifyChange(cmds.PLAYSTATE_CHANGED);
        }
    }

    /**
     * Called to open a new file as the current track and prepare the next for
     * playback
     * 
     * @param openNext
     *            True to prepare the next track for playback, false otherwise.
     */
    private boolean openCurrentAndMaybeNext(final boolean openNext) {
        synchronized (this) {
            closeCursor();
            int currentLength = getQueueSize();

            if (currentLength == 0) {
                return false;
            }
            stop(false);

            mCursor = mQueue.get(mPlayPos);
            while (true) {
                if (mCursor != null && openFile(mCursor)) {
                    break;
                }
                closeCursor();
                if (mOpenFailedCounter++ < 10 && currentLength > 1) {
                    final int pos = getNextPosition(false);
                    if (pos < 0) {
//                        gotoIdleState();
//                        setIsSupposeToBePlaying(false);
                        return false;
                    }
                    mPlayPos = pos;
                    stop(false);
                    mPlayPos = pos;
                    mCursor = mQueue.get(mPlayPos);
                } else {
                    mOpenFailedCounter = 0;
//                    gotoIdleState();
//                    setIsSupposeToBePlaying(false);
                    return false;
                }
            }
            if (openNext) {
                setNextTrack();
            }
            return true;
        }
    }

    private void createCurrentQueue() {
        if (mOriginalQueue == null) {
            mQueue = null;
        } else {
            mQueue = new ArrayList<Object>(mOriginalQueue);
            if (mShuffleMode != AudioModule.SHUFFLE_NONE) {
                Collections.shuffle(mQueue);
            }
            notifyChange(cmds.QUEUE_CHANGED);
        }

    }

    /**
     * @param force
     *            True to force the player onto the track next, false otherwise.
     * @return The next position to play.
     */
    private int getNextPosition(final boolean force) {

        if (mRepeatMode == AudioModule.REPEAT_ONE && !force) {
            if (mPlayPos < 0) {
                return 0;
            }
            return mPlayPos;
        }
        int currentLength = getQueueSize();
        if (mPlayPos >= currentLength - 1) {
            if (mRepeatMode == AudioModule.REPEAT_ALL) {
                if (mShuffleMode != AudioModule.SHUFFLE_NONE) {
                    createCurrentQueue();
                }
                return 0;
            }
            return -1;
        }
        return mPlayPos + 1;
    }

    /**
     * Sets the track track to be played
     */
    private void setNextTrack() {
        mNextPlayPos = getNextPosition(false);
        if (mPlayPos != mNextPlayPos && mNextPlayPos >= 0 && mQueue != null) {
            mPlayer.openFile(mQueue.get(mNextPlayPos), true);
        }
    }

    /**/
    // private boolean wasRecentlyUsed(final int idx, int lookbacksize) {
    // if (lookbacksize == 0) {
    // return false;
    // }
    // final int histsize = mHistory.size();
    // if (histsize < lookbacksize) {
    // lookbacksize = histsize;
    // }
    // final int maxidx = histsize - 1;
    // for (int i = 0; i < lookbacksize; i++) {
    // final long entry = mHistory.get(maxidx - i);
    // if (entry == idx) {
    // return true;
    // }
    // }
    // return false;
    // }

    /**
     * Notify the change-receivers that something has changed.
     */
    protected void notifyChange(final String what) {
        // final Intent intent = new Intent(what);
        // sendStickyBroadcast(intent);

        // Update the lockscreen controls
        updateRemoteControlClient(what);

        if (what.equals(cmds.META_CHANGED)) {
            if (proxyHasListeners(Events.CHANGE, false)) {
                proxy.fireEvent(Events.CHANGE, getTrackEvent(), false, false);
            }
            updateMetadata();
        } else if (what.equals(cmds.QUEUE_CHANGED)) {
        }
        else if (what.equals(cmds.PLAYSTATE_CHANGED)) {
            if (mState == State.STATE_STOPPED) {
                killNotification();
            } else {
                mNotificationHelper.goToIdleState(mIsSupposedToBePlaying);
            }
        }

        // Update the app-widgets
        if (mAppWidgetSmall != null) {
            mAppWidgetSmall.notifyChange(this, what);
        }
        if (mAppWidgetLarge != null) {
            mAppWidgetLarge.notifyChange(this, what);
        }
        if (mAppWidgetLargeAlternate != null) {
            mAppWidgetLargeAlternate.notifyChange(this, what);
        }
    }

    private void updateRemoteControlClient(final String what) {
        if (what.equals(cmds.PLAYSTATE_CHANGED)) {
            if (mEnableLockscreenControls && mRemoteControlClientCompat != null) {
                // If the playstate change notify the lock screen
                // controls
                mRemoteControlClientCompat
                        .setPlaybackState(mIsSupposedToBePlaying ? RemoteControlClient.PLAYSTATE_PLAYING
                                : RemoteControlClient.PLAYSTATE_PAUSED);
            }
        }

    }

    /**
     * Opens a file and prepares it for playback
     * 
     * @param path
     *            The path of the file to open
     */
    public boolean openFile(final Object object) {
        synchronized (this) {
            if (object == null) {
                return false;
            }
            if (mPlayer.openFile(object, false)) {
                mOpenFailedCounter = 0;
                return true;
            }
            stop(true);
            return false;
        }
    }

    /**
     * Returns the audio session ID
     * 
     * @return The current media player audio session ID
     */
    public int getAudioSessionId() {
        synchronized (this) {
            return mPlayer.getAudioSessionId();
        }
    }

    /**
     * Sets the audio session ID.
     * 
     * @param sessionId
     *            : the audio session ID.
     */
    public void setAudioSessionId(final int sessionId) {
        synchronized (this) {
            mPlayer.setAudioSessionId(sessionId);
        }
    }

    /**
     * Returns the shuffle mode
     * 
     * @return The current shuffle mode (all, party, none)
     */
    public int getShuffleMode() {
        return mShuffleMode;
    }

    /**
     * Returns the repeat mode
     * 
     * @return The current repeat mode (all, one, none)
     */
    public int getRepeatMode() {
        return mRepeatMode;
    }

    public int removeTrack(final Object object) {
        int numremoved = 0;
        synchronized (this) {
            int index = mOriginalQueue.indexOf(object);
            while (index != -1) {
                numremoved += removeTracksInternal(index, index);
            }
        }
        if (numremoved > 0) {
            notifyChange(cmds.QUEUE_CHANGED);
        }
        return numremoved;
    }

    /**
     * Removes the range of tracks specified from the play list. If a file
     * within the range is the file currently being played, playback will move
     * to the next file after the range.
     * 
     * @param first
     *            The first file to be removed
     * @param last
     *            The last file to be removed
     * @return the number of tracks deleted
     */
    public int removeTracks(final int first, final int last) {
        final int numremoved = removeTracksInternal(first, last);
        if (numremoved > 0) {
            notifyChange(cmds.QUEUE_CHANGED);
        }
        return numremoved;
    }

    /**
     * Returns the position in the queue
     * 
     * @return the current position in the queue
     */
    public int getQueuePosition() {
        synchronized (this) {
            return mPlayPos;
        }
    }

    /**
     * Returns the current
     * 
     * @return The current track
     */
    public Object getCurrent() {
        if (mCursor != null) {
            return mCursor;
        } else if (mOriginalQueue != null && mPlayPos >=0) {
            return mOriginalQueue.get(mPlayPos);
        }
        return null;
    }

    /**
     * Seeks the current track to a specific time
     * 
     * @param position
     *            The time to seek to
     * @return The time to play the track at
     */
    public long seek(long position) {
        if (mPlayer.isInitialized()) {
            if (position < 0) {
                position = 0;
            } else if (position > mPlayer.duration()) {
                position = mPlayer.duration();
            }
            return mPlayer.seek(position);
        }
        return -1;
    }

    /**
     * Returns the current position in time of the currenttrack
     * 
     * @return The current playback position in miliseconds
     */
    public long position() {
        if (mPlayer.isInitialized()) {
            return mPlayer.position();
        }
        return -1;
    }

    /**
     * Returns the current state
     * 
     * @return The current player state
     */
    public int state() {
        return mState;
    }

    /**
     * Returns the current state description
     * 
     * @return The current player state description
     */
    public String stateDescription() {
        return mStateDescription;
    }

    /**
     * Returns the full duration of the current track
     * 
     * @return The duration of the current track in miliseconds
     */
    public long duration() {
        if (mPlayer.isPrepared()) {
            return mPlayer.duration();
        }
        return -1;
    }

    /**
     * Returns the queue (shuffled or playlist)
     * 
     * @return The queue as a Object[]
     */
    public Object[] getQueue() {
        synchronized (this) {
            if (mQueue != null) {
                return mQueue.toArray();
            }
            return null;
        }
    }

    /**
     * Returns the playlist
     * 
     * @return The playlist as a Object[]
     */
    public Object[] getPlaylist() {
        synchronized (this) {
            if (mOriginalQueue != null) {
                return mOriginalQueue.toArray();
            }
            return null;
        }
    }

    /**
     * @return True if music is playing, false otherwise
     */
    public boolean isPlaying() {
        if (mPlayer != null) {
            return mPlayer.isPlaying();
        }
        return mState == State.STATE_PLAYING;
    }

    /**
     * @return True if music is paused, false otherwise
     */
    public boolean isPaused() {
        if (mPlayer != null) {
            return mPlayer.isPaused();
        }
        return mState == State.STATE_PAUSED;
    }

    /**
     * @return True if music is stopped, false otherwise
     */
    public boolean isStopped() {
        return mState == State.STATE_STOPPED;
    }

    /**
     * Opens a list for playback
     * 
     * @param list
     *            The list of tracks to open c
     */
    public void open(final List<Object> list, final int position) {
        if (list == null) {
            return;
        }
        synchronized (this) {
            // final int listlength = list.size();
            mOriginalQueue = null;
            addToPlayList(list, -1);
            notifyChange(cmds.QUEUE_CHANGED);
            if (position >= 0) {
                mPlayPos = position;
            } else {
                mPlayPos = 0;
            }
            // mHistory.clear();
            openCurrentAndNext();
        }
    }

    /**
     * Stops playback.
     */
    public void stop() {
        stop(true);
    }

    public void reset() {
        setPlaylist(null);
    }

    /**
     * Resumes or starts playback.
     */
    public void play() {
        if (mPausePending || mStopPending) {
            stop(!mPausePending || mStopPending);
            return;
        }
        mPausePending = false;
        mStopPending = false;
        tryToGetAudioFocus();

        if (mPlayer.isInitialized()) {

            if (mPlayer.isPrepared()) {
                mPlayPending = false;
                mPlayer.start();
//                setIsSupposeToBePlaying(mPlayer.isPlaying());

                mPlayerHandler.removeMessages(FADEDOWN);
                mPlayerHandler.sendEmptyMessage(FADEUP);

            }
        } else if (mOriginalQueue == null) { // first play
            if (proxy != null) {
                open(getPlayerProxy().getInternalPlaylist(), 0);
            }
        } else if (getQueueSize() > 0){
            if (mPlayPos == -1) {
                mPlayPos = 0;
            }
            if (openCurrentAndNext()) {
                play();
            }
            // notifyChange(META_CHANGED);
        }
    }

    /**
     * Temporarily pauses playback.
     */
    @Override
    public void pause() {
        synchronized (this) {
            mPlayerHandler.removeMessages(FADEUP);

            if (mIsSupposedToBePlaying) {
                mPlayer.pause();
//                gotoIdleState();
//                setIsSupposeToBePlaying(false);
            } else if (mPlayPending) {
                mPausePending = true;
                mPlayPending = false;
            }
        }
    }

    /**
     * Changes from the current track to the next track
     * 
     * @param force
     *            : used to still get next track even in repeat_one.
     */
    public void gotoNext(final boolean force) {
        synchronized (this) {
            if (mShuffleMode == AudioModule.SHUFFLE_RANDOM) {
                createCurrentQueue();
                mPlayPos = 0;
            } else {
                final int pos = getNextPosition(force);
                if (pos == mPlayPos) {
                    return;
                }
                if (pos < 0) {
                    if (mIsSupposedToBePlaying) {
                        proxy.fireEvent(Events.END, null, false, true);
                        stop();
                        mPlayPos = -1;
                    }
                    return;
                }
                mPlayPos = pos;
            }
            mForcedTrackToEnd = true;
            stop(false);
            openCurrentAndNext();
            if (mPlayPending) {
                play();
            }
            // notifyChange(META_CHANGED);
        }
    }

    /**
     * Changes from the current track to the next track
     */
    protected void next() {
        gotoNext(false);
    }

    /**
     * Changes from the current track to the previous played track
     */
    protected void prev() {
        synchronized (this) {
            if (mPlayPos > 0) {
                mPlayPos--;
            } else {
                if (mShuffleMode == AudioModule.SHUFFLE_RANDOM) {
                    createCurrentQueue();
                }
                mPlayPos = mQueue.size() - 1;
            }
            stop(false);
            openCurrent();
            play();
            // notifyChange(META_CHANGED);
        }
    }

    /**
     * We don't want to open the current and next track when the user is using
     * the {@code #prev()} method because they won't be able to travel back to
     * the previously listened track if they're shuffling.
     */
    private void openCurrent() {
        openCurrentAndMaybeNext(false);
    }

    /**
     * Sets the repeat mode
     * 
     * @param repeatmode
     *            The repeat mode to use
     */
    public void setRepeatMode(final int repeatmode) {
        synchronized (this) {
            mRepeatMode = repeatmode;
            setNextTrack();
            notifyChange(cmds.REPEATMODE_CHANGED);
        }
    }

    /**
     * Sets the shuffle mode
     * 
     * @param shufflemode
     *            The shuffle mode to use
     */
    public void setShuffleMode(final int shufflemode) {
        synchronized (this) {
            if (mShuffleMode == shufflemode) {
                return;
            }
            mShuffleMode = shufflemode;

            notifyChange(cmds.SHUFFLEMODE_CHANGED);
        }
    }

    public void setPlaylist(final List<Object> list) {
        final boolean playing = mState == State.STATE_PLAYING;
        final boolean emptyPlaylist = list != null;
        mIgnoreStateChange = emptyPlaylist;
        stop();
        mIgnoreStateChange = false;
        mOriginalQueue = null;
        mQueue = null;
        mPlayPos = 0;
        if (list != null) {
            addToPlayList(list, Integer.MAX_VALUE);
        }
        if (!emptyPlaylist && playing) {
            play();
        }
    }

    /**
     * Cycles through the different repeat modes
     */
    private void cycleRepeat() {
        if (mRepeatMode == AudioModule.REPEAT_NONE) {
            setRepeatMode(AudioModule.REPEAT_ALL);
        } else if (mRepeatMode == AudioModule.REPEAT_ALL) {
            setRepeatMode(AudioModule.REPEAT_ONE);
            if (mShuffleMode != AudioModule.SHUFFLE_NONE) {
                setShuffleMode(AudioModule.SHUFFLE_NONE);
            }
        } else {
            setRepeatMode(AudioModule.REPEAT_NONE);
        }
    }

    /**
     * Cycles through the different shuffle modes
     */
    private void cycleShuffle() {
        if (mShuffleMode == AudioModule.SHUFFLE_NONE) {
            setShuffleMode(AudioModule.SHUFFLE_SONGS);
            if (mRepeatMode == AudioModule.REPEAT_ONE) {
                setRepeatMode(AudioModule.REPEAT_ALL);
            }
        } else if (mShuffleMode == AudioModule.SHUFFLE_SONGS
                || mShuffleMode == AudioModule.SHUFFLE_RANDOM) {
            setShuffleMode(AudioModule.SHUFFLE_NONE);
        }
    }

    /**
     * Called when one of the lists should refresh or requery.
     */
    public void refresh() {
        notifyChange(cmds.REFRESH);
    }

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {

        /**
         * {@inheritDoc}
         */
        @Override
        public void onReceive(final Context context, final Intent intent) {
            handleIntent(intent);
        }
    };

    private final OnAudioFocusChangeListener mAudioFocusListener = new OnAudioFocusChangeListener() {

        /**
         * {@inheritDoc}
         */
        @Override
        public void onAudioFocusChange(final int focusChange) {
            mPlayerHandler.obtainMessage(FOCUSCHANGE, focusChange, 0)
                    .sendToTarget();
        }
    };
    protected int mState = State.STATE_STOPPED;
    private String mStateDescription;

    private static final class DelayedHandler extends Handler {

        private final WeakReference<AudioService> mService;

        /**
         * Constructor of <code>DelayedHandler</code>
         * 
         * @param service
         *            The service to use.
         */
        public DelayedHandler(final AudioService service) {
            mService = new WeakReference<AudioService>(service);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void handleMessage(final Message msg) {
            if (mService.get().isPlaying()
                    || mService.get().mPausedByTransientLossOfFocus
                    || mService.get().mServiceInUse
                    || mService.get().mPlayerHandler.hasMessages(TRACK_ENDED)) {
                return;
            }
            mService.get().stopSelf(mService.get().mServiceStartId);
        }
    }

    private static final class MusicPlayerHandler extends Handler {

        private final WeakReference<AudioService> mService;

        private float mVolume = 1.0f;
        private float mCurrentVolume = 0.0f;
        private boolean mVolumeFading = false;

        /**
         * Constructor of <code>MusicPlayerHandler</code>
         * 
         * @param service
         *            The service to use.
         * @param looper
         *            The thread to run on.
         */
        public MusicPlayerHandler(final AudioService service,
                final Looper looper) {
            super(looper);
            mService = new WeakReference<AudioService>(service);
        }

        public void setVolume(final float volume) {
            mVolume = volume;
            if (!mVolumeFading) {
                mCurrentVolume = mVolume;
                mService.get().mPlayer.setVolume(mCurrentVolume);
            }
        }

        public boolean isVolumeFading() {
            return mVolumeFading;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void handleMessage(final Message msg) {
            switch (msg.what) {
            case FADEDOWN:
                mCurrentVolume -= .05f;
                if (mCurrentVolume > .2f) {
                    mVolumeFading = true;
                    sendEmptyMessageDelayed(FADEDOWN, 10);
                } else {
                    mVolumeFading = false;
                    mCurrentVolume = .2f;
                }
                mService.get().mPlayer.setVolume(mCurrentVolume);
                break;
            case FADEUP:
                mCurrentVolume += .01f;
                if (mCurrentVolume < mVolume) {
                    mVolumeFading = true;
                    sendEmptyMessageDelayed(FADEUP, 10);
                } else {
                    mVolumeFading = false;
                    mCurrentVolume = mVolume;
                }
                mService.get().mPlayer.setVolume(mCurrentVolume);
                break;
            case SERVER_DIED:
                if (mService.get().mIsSupposedToBePlaying) {
                    mService.get().gotoNext(false);
                } else {
                    mService.get().openCurrentAndNext();
                }
                break;
            case TRACK_WENT_TO_NEXT:
                mService.get().mPlayPos = mService.get().mNextPlayPos;
                mService.get().closeCursor();
                if (mService.get().mPlayPos != -1) {
                    mService.get().mCursor = mService.get().mQueue.get(mService
                            .get().mPlayPos);
                    mService.get().buildNotification();
                    // mService.get().notifyChange(META_CHANGED);
                    mService.get().setNextTrack();
                    break;
                }
                // dont break if -1 (stop)
            case TRACK_ENDED:
                if (mService.get().mRepeatMode == AudioModule.REPEAT_ONE) {
                    if (mService.get().mCursor != null) {
                        mService.get().seek(0);
                        mService.get().play();
                    } else {
                        mService.get().gotoNext(true);
                    }
                } else {
                    mService.get().gotoNext(false);
                }
                break;
            case RELEASE_WAKELOCK:
                mService.get().mWakeLock.release();
                break;
            case FOCUSCHANGE:
                switch (msg.arg1) {
                case AudioManager.AUDIOFOCUS_LOSS:
                    if (mService.get().isPlaying()) {
                        mService.get().mPausedByTransientLossOfFocus = false;
                    }
                    mService.get().pause();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    removeMessages(FADEUP);
                    sendEmptyMessage(FADEDOWN);
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    if (mService.get().isPlaying()) {
                        mService.get().mPausedByTransientLossOfFocus = true;
                    }
                    mService.get().pause();
                    break;
                case AudioManager.AUDIOFOCUS_GAIN:
                    if (!mService.get().isPlaying()
                            && mService.get().mPausedByTransientLossOfFocus) {
                        mService.get().mPausedByTransientLossOfFocus = false;
                        mCurrentVolume = 0f;
                        mService.get().mPlayer.setVolume(mCurrentVolume);
                        mService.get().play();
                    } else {
                        removeMessages(FADEDOWN);
                        sendEmptyMessage(FADEUP);
                    }
                    break;
                default:
                }
                break;
            default:
                break;
            }
        }
    }
    
    public void acquireWakeLock(final int timeout) {
        mWakeLock.acquire(timeout);
    }

    // private static final class Shuffler {
    //
    // private final LinkedList<Integer> mHistoryOfNumbers = new
    // LinkedList<Integer>();
    //
    // private final TreeSet<Integer> mPreviousNumbers = new TreeSet<Integer>();
    //
    // private final Random mRandom = new Random();
    //
    // private int mPrevious;
    //
    // /**
    // * Constructor of <code>Shuffler</code>
    // */
    // public Shuffler() {
    // super();
    // }
    //
    // /**
    // * @param interval
    // * The length the queue
    // * @return The position of the next track to play
    // */
    // public int nextInt(final int interval) {
    // int next;
    // do {
    // next = mRandom.nextInt(interval);
    // } while (next == mPrevious && interval > 1
    // && !mPreviousNumbers.contains(Integer.valueOf(next)));
    // mPrevious = next;
    // mHistoryOfNumbers.add(mPrevious);
    // mPreviousNumbers.add(mPrevious);
    // cleanUpHistory();
    // return next;
    // }
    //
    // /**
    // * Removes old tracks and cleans up the history preparing for new tracks
    // * to be added to the mapping
    // */
    // private void cleanUpHistory() {
    // if (!mHistoryOfNumbers.isEmpty()
    // && mHistoryOfNumbers.size() >= MAX_HISTORY_SIZE) {
    // for (int i = 0; i < Math.max(1, MAX_HISTORY_SIZE / 2); i++) {
    // mPreviousNumbers.remove(mHistoryOfNumbers.removeFirst());
    // }
    // }
    // }
    // };

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
            Bitmap bitmap = (cacheKey != null) ? cache.get(cacheKey) : null;
            if (bitmap == null) {
                Drawable drawable = imageref.getDrawable();
                if (drawable instanceof BitmapDrawable) {
                    bitmap = ((BitmapDrawable) drawable).getBitmap();
                    cache.set(imageref.getUrl(), bitmap);
                }
            }

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
            put("tracknumber",
                    MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER);
            put("duration", MediaMetadataRetriever.METADATA_KEY_DURATION);
            put("year", MediaMetadataRetriever.METADATA_KEY_YEAR);
        }
    };

    protected void onProgress() {
        if (proxyHasListeners(Events.PROGRESS, false)) {
            KrollDict event = getTrackEvent();
            event.put("progress", mPlayer.position());
            proxy.fireEvent(Events.PROGRESS, event, false, false);
        }
    }

    private void handleUpdateBitmapMetadata(final Bitmap bitmap) {
        mNotificationHelper.updateAlbumArt(bitmap);
        // this needs to be called in a background thread because of the copy
        if (mRemoteControlClientCompat == null || bitmap == null)
            return;
        Bitmap newBitmap = bitmap.copy(bitmap.getConfig(),
                bitmap.isMutable() ? true : false);
        if (currentMetadataEditor != null) {
            currentMetadataEditor.putBitmap(MetadataEditor.BITMAP_KEY_ARTWORK,
                    newBitmap);
            return;
        }

        MetadataEditorCompat metadataEditor = mRemoteControlClientCompat
                .editMetadata(false);
        metadataEditor.putBitmap(MetadataEditor.BITMAP_KEY_ARTWORK, newBitmap);
        metadataEditor.apply();

    }

    private boolean updatingMetadata = false;

    protected void updateMetadata(final Map<String, Object> metadata) {
        if (updatingMetadata)
            return;
        updatingMetadata = true;
        if (mRemoteControlClientCompat != null) {
         // for now we DONT call with "true". There is a recycle on the bitmap
            // which is bad news for when the bitmap is use somewhere else.
            // instead we update all datas :s
            currentMetadataEditor = mRemoteControlClientCompat.editMetadata(true);
            currentMetadataEditor
                    .putBitmap(MetadataEditor.BITMAP_KEY_ARTWORK, null);
            if (metadata != null) {
                for (Map.Entry<String, Object> entry : metadata.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    if (METADATAS.containsKey(key)) {
                        currentMetadataEditor.putString(METADATAS.get(key),
                                TiConvert.toString(value));
                    } else if (METADATAS_LONG.containsKey(key)) {
                        try {
                           currentMetadataEditor.putLong(METADATAS_LONG.get(key),
                                    TiConvert.toLong(value));              
                        }
                        catch(Exception e) {
                            e.printStackTrace();
                        }
                       }
                }
                if (!metadata.containsKey("duration") && mPlayer != null
                        && mPlayer.isPlaying()) {
                    currentMetadataEditor.putLong(
                            MediaMetadataRetriever.METADATA_KEY_DURATION,
                            mPlayer.duration());
                }
                TiDrawableReference imageref = TiDrawableReference.fromObject(
                        proxy, metadata.get("artwork"));

                if (!imageref.isTypeNull()) {
                    TiImageHelper.downloadDrawable(proxy, imageref, true, this);
                }
            }
            currentMetadataEditor.apply();
            currentMetadataEditor = null;
        }
        
        mNotificationHelper.updateMetadata(metadata);
        updatingMetadata = false;
    }

    public void updateMetadata() {
        if (isStopped()) {
            return;
        }
        HashMap<String, Object> metadata = null;
        if (proxy != null && proxy.hasProperty(TiC.PROPERTY_METADATA)) {
            metadata = (HashMap<String, Object>) proxy
                    .getProperty(TiC.PROPERTY_METADATA);
        } else if (mCursor instanceof HashMap) {
            metadata = (HashMap<String, Object>) mCursor;
        }

        updateMetadata(metadata);
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
        updateBitmapMetadata(null);
    }

    @Override
    public void onPrepareLoad(Drawable placeHolderDrawable) {
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
        case MSG_UPDATE_BITMAP_METADATA: {
            handleUpdateBitmapMetadata((Bitmap) msg.obj);
            return true;
        }
        }
        return false;
    }

    private int mRemoteControlClientState = 0;

    public void setState(int state) {
        if (state == mState)
            return;
        mState = state;

        int remoteState = mRemoteControlClientState;
        mStateDescription = "";
        if (REMOTE_STATE.containsKey(state)) {
            remoteState = REMOTE_STATE.get(state);
        }
        if (STATE_DESC.containsKey(state)) {
            mStateDescription = STATE_DESC.get(state);
        }
        if (mRemoteControlClientCompat != null
                && mRemoteControlClientState != remoteState) {
            mRemoteControlClientState = remoteState;
            mRemoteControlClientCompat
                    .setPlaybackState(mRemoteControlClientState);
        }
        boolean playing = mState == State.STATE_PLAYING;
        setIsSupposeToBePlaying(playing);
        notifyChange(cmds.PLAYSTATE_CHANGED);

        if (proxy != null) {
            proxy.setProperty("state", state);
            // proxy.setProperty("stateDescription", stateDescription);
        }
        Log.d(TAG, "Audio state changed: " + mStateDescription, Log.DEBUG_MODE);

        if (!mIgnoreStateChange && proxyHasListeners(Events.STATE, false)) {
            KrollDict data = getTrackEvent();
            data.put("state", state);
            data.put("description", mStateDescription);
            proxy.fireEvent(Events.STATE, data, false, false);
        }
    }

    public void setVolume(final float volume) {
        mVolume = volume;
        mPlayerHandler.setVolume(volume);
    }

    @Override
    public void onAppPaused() {
        inBackground = true;
        if (mIsSupposedToBePlaying && proxy != null
                && TiConvert
                        .toBoolean(proxy.getProperty("notifyOnPause"), true)) {
            if (mState == State.STATE_STOPPED) {
                killNotification();
            } else {
                mNotificationHelper.showNotification();
            }
        }
    }

    @Override
    public void onAppResume() {
        inBackground = false;
    }

    public void onStartPlaying(final BasePlayingItem playingItem) {
        if (proxy != null && proxy.hasProperty(TiC.PROPERTY_TIME)) {
            seek(TiConvert.toLong(proxy.getProperty(TiC.PROPERTY_TIME), 0));
        }
        if (mPlayPending) {
            play();
        }

        if (!mIsSupposedToBePlaying) {
            mIsSupposedToBePlaying = true;
//            notifyChange(cmds.PLAYSTATE_CHANGED);
        }
        // Update the notification
        mBuildNotification = mIsSupposedToBePlaying;
        buildNotification();
        notifyChange(cmds.META_CHANGED);

    }

    private void aquireWifiLock() {
        if (mWifiLock != null && !mWifiLock.isHeld()) {
            mWifiLock.acquire();
        }
    }

    private void releaseWifiLock() {
        if (mWifiLock != null && mWifiLock.isHeld()) {
            mWifiLock.release();
        }
    }

    void tryToGetAudioFocus() {
        if (mAudioFocus != AudioFocus.Focused
                && AudioManager.AUDIOFOCUS_REQUEST_GRANTED == mAudioManager
                        .requestAudioFocus(mAudioFocusListener,
                                AudioManager.STREAM_MUSIC,
                                AudioManager.AUDIOFOCUS_GAIN)) {
            mAudioFocus = AudioFocus.Focused;
            RemoteControlHelper.registerRemoteControlClient(mAudioManager,
                    mRemoteControlClientCompat);
            AudioModule.widgetGetsFocused(this);
        }
        aquireWifiLock();

    }

    void giveUpAudioFocus() {
        if (mAudioFocus == AudioFocus.Focused
                && AudioManager.AUDIOFOCUS_REQUEST_GRANTED == mAudioManager
                        .abandonAudioFocus(mAudioFocusListener)) {
            // Remove the audio focus listener and lock screen controls
            mAudioFocus = AudioFocus.NoFocusNoDuck;
            RemoteControlHelper.unregisterRemoteControlClient(mAudioManager,
                    mRemoteControlClientCompat);
            AudioModule.widgetAbandonsFocused(this);
        }
        releaseWifiLock();

    }

    protected static final class CompatMediaPlayer extends MediaPlayer
            implements OnCompletionListener {

        private boolean mCompatMode = true;

        private MediaPlayer mNextPlayer;

        private OnCompletionListener mCompletion;

        /**
         * Constructor of <code>CompatMediaPlayer</code>
         */
        public CompatMediaPlayer() {
            try {
                MediaPlayer.class.getMethod("setNextMediaPlayer",
                        MediaPlayer.class);
                mCompatMode = false;
            } catch (final NoSuchMethodException e) {
                mCompatMode = true;
                super.setOnCompletionListener(this);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setNextMediaPlayer(final MediaPlayer next) {
            if (mCompatMode) {
                mNextPlayer = next;
            } else {
                super.setNextMediaPlayer(next);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setOnCompletionListener(final OnCompletionListener listener) {
            if (mCompatMode) {
                mCompletion = listener;
            } else {
                super.setOnCompletionListener(listener);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onCompletion(final MediaPlayer mp) {
            if (mNextPlayer != null) {
                // SystemClock.sleep(25);
                mNextPlayer.start();
            }
            mCompletion.onCompletion(this);
        }
    }
}
