/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.audio;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiBaseActivity;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiContext;
import org.appcelerator.titanium.TiLifecycle.OnLifecycleEvent;
import org.appcelerator.titanium.TiLifecycle.OnWindowFocusChangedEvent;
import org.appcelerator.titanium.util.TiConvert;

import ti.modules.titanium.filesystem.FileProxy;
import android.app.Activity;

@Kroll.proxy(creatableInModule = AudioModule.class, propertyAccessors = { 
    TiC.PROPERTY_VOLUME,
    TiC.PROPERTY_METADATA
    })
public class PlayerProxy extends KrollProxy implements OnLifecycleEvent,
        OnWindowFocusChangedEvent {
    private static final String TAG = "PlayerProxy";

    @Kroll.constant
    public static final int STATE_BUFFERING = TiSound.STATE_BUFFERING;
    @Kroll.constant
    public static final int STATE_INITIALIZED = TiSound.STATE_INITIALIZED;
    @Kroll.constant
    public static final int STATE_PAUSED = TiSound.STATE_PAUSED;
    @Kroll.constant
    public static final int STATE_PLAYING = TiSound.STATE_PLAYING;
    @Kroll.constant
    public static final int STATE_STARTING = TiSound.STATE_STARTING;
    @Kroll.constant
    public static final int STATE_STOPPED = TiSound.STATE_STOPPED;
    @Kroll.constant
    public static final int STATE_STOPPING = TiSound.STATE_STOPPING;
    @Kroll.constant
    public static final int STATE_WAITING_FOR_DATA = TiSound.STATE_WAITING_FOR_DATA;
    @Kroll.constant
    public static final int STATE_WAITING_FOR_QUEUE = TiSound.STATE_WAITING_FOR_QUEUE;

    protected TiSound snd;
    private boolean windowFocused;
    private boolean resumeInOnWindowFocusChanged;
    private boolean allowBackground = false;

    private boolean mEnableLockscreenControls = true;

    public PlayerProxy() {
        super();

        // TODO - we shouldnt need this as this proxy is created only from the
        // runtime - double check
        // TODO this needs to happen post-set
        // ((TiBaseActivity)getActivity()).addOnLifecycleEventListener(this);

        defaultValues.put(TiC.PROPERTY_VOLUME, 1.0f);
        defaultValues.put(TiC.PROPERTY_TIME, 0);
    }

    public PlayerProxy(TiContext tiContext) {
        this();
    }

    @Override
    protected void initActivity(Activity activity) {
        super.initActivity(activity);
        ((TiBaseActivity) getActivity()).addOnLifecycleEventListener(this);
        ((TiBaseActivity) getActivity())
                .addOnWindowFocusChangedEventListener(this);
    }

    @Override
    public void handleCreationDict(KrollDict options) {
        super.handleCreationDict(options);
        if (options.containsKey(TiC.PROPERTY_URL)) {
            setProperty(
                    TiC.PROPERTY_URL,
                    resolveUrl(null,
                            TiConvert.toString(options, TiC.PROPERTY_URL)));
        } else if (options.containsKey(TiC.PROPERTY_SOUND)) {
            FileProxy fp = (FileProxy) options.get(TiC.PROPERTY_SOUND);
            if (fp != null) {
                String url = fp.getNativePath();
                setProperty(TiC.PROPERTY_URL, url);
            }
        }
        if (options.containsKey(TiC.PROPERTY_ALLOW_BACKGROUND)) {
            allowBackground = options.optBoolean(TiC.PROPERTY_ALLOW_BACKGROUND,
                    allowBackground);
        }
        Log.i(TAG,
                "Creating audio player proxy for url: "
                        + TiConvert.toString(getProperty(TiC.PROPERTY_URL)),
                Log.DEBUG_MODE);
    }

    @Kroll.getProperty
    @Kroll.method
    public String getUrl() {
        return TiConvert.toString(getProperty(TiC.PROPERTY_URL));
    }

    @Kroll.setProperty
    @Kroll.method
    public void setUrl(String url) {
        if (url != null) {
            setProperty(TiC.PROPERTY_URL,
                    resolveUrl(null, TiConvert.toString(url)));
        }
    }

    @Kroll.getProperty
    @Kroll.method
    public int getDuration() {
        TiSound s = getSound();
        if (s != null) {
            return s.getDuration();
        }
        return 0;
    }

    @Kroll.getProperty
    @Kroll.method
    public boolean isPlaying() {
        TiSound s = getSound();
        if (s != null) {
            return s.isPlaying();
        }
        return false;
    }

    @Kroll.getProperty
    @Kroll.method
    public boolean isPaused() {
        TiSound s = getSound();
        if (s != null) {
            return s.isPaused();
        }
        return false;
    }

    // An alias for play so that
    @Kroll.method
    public void start() {
        play();
    }

    @Kroll.method
    public void play() {
        TiSound s = getSound();
        if (s != null) {
            s.play();
        }
    }

    @Kroll.method
    public void pause() {
        TiSound s = getSound();
        if (s != null) {
            s.pause();
        }
    }

    @Kroll.method
    public void release() {
        if (snd != null) {
            snd.release();
            snd = null;
        }
    }

    @Kroll.method
    public void destroy() {
        release();
    }

    @Kroll.method
    public void stop() {
        if (snd != null) {
            snd.stop();
        }
    }

    @Kroll.method
    @Kroll.getProperty
    public double getTime() {
        TiSound s = getSound();
        if (s != null) {
            int time = s.getTime();
            setProperty(TiC.PROPERTY_TIME, time);
        }
        return TiConvert.toDouble(getProperty(TiC.PROPERTY_TIME));
    }

    @Kroll.method
    @Kroll.setProperty
    public void setTime(Object pos) {
        if (pos != null) {
            TiSound s = getSound();
            if (s != null) {
                s.setTime(TiConvert.toInt(pos));
            } else {
                setProperty(TiC.PROPERTY_TIME, TiConvert.toDouble(pos));
            }
        }
    }

    @Kroll.method
    @Kroll.setProperty
    public void setAllowBackground(Object value) {
        allowBackground = TiConvert.toBoolean(value, false);
        if (snd != null) {
            snd.setAllowBackground(allowBackground);
        }

    }

    protected TiSound getSound() {
        if (snd == null) {
            snd = new TiSound(this, mEnableLockscreenControls );
            setModelListener(snd);
            snd.setAllowBackground(allowBackground);
        }
        return snd;
    }

    private boolean allowBackground() {
        return allowBackground;
    }

    public void onStart(Activity activity) {
    }

    public void onResume(Activity activity) {
        if (windowFocused && !allowBackground()) {
            if (snd != null) {
                snd.onResume();
            }
        } else {
            resumeInOnWindowFocusChanged = true;
        }
    }

    public void onPause(Activity activity) {
        if (!allowBackground()) {
            if (snd != null) {
                snd.onPause();
            }
        }
    }

    public void onStop(Activity activity) {
    }

    public void onDestroy(Activity activity) {
        if (snd != null) {
            snd.onDestroy();
        }
        snd = null;
    }

    public void onWindowFocusChanged(boolean hasFocus) {
        windowFocused = hasFocus;
        if (resumeInOnWindowFocusChanged && !allowBackground()) {
            if (snd != null) {
                snd.onResume();
            }
            resumeInOnWindowFocusChanged = false;
        }
    }

    @Override
    public String getApiName() {
        return "Ti.Audio.AudioPlayer";
    }
}
