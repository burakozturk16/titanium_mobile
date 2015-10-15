/**
 * Akylas
 * Copyright (c) 2014-2015 by Akylas. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */


package ti.modules.titanium.audio;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.TiEnhancedServiceProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiRHelper;

import ti.modules.titanium.audio.streamer.AudioService;
import ti.modules.titanium.audio.streamer.AudioService.State;
import android.app.Service;
import android.content.Intent;

@Kroll.proxy(parentModule = AudioModule.class, propertyAccessors = {
        TiC.PROPERTY_VOLUME, 
        TiC.PROPERTY_METADATA, 
        TiC.PROPERTY_REPEAT_MODE,
        TiC.PROPERTY_SHUFFLE_MODE,
        })
public class BasePlayerProxy extends TiEnhancedServiceProxy {
    private static final String TAG = "BasePlayerProxy";

    AudioService tiService = null;
    List<Object> mPlaylist;
    private boolean needsStopOnBind = false;

    public BasePlayerProxy() {
        super(null, null, null);
        if (isMyServiceRunning()) {
            startService();
        }
    }

    /**
     * For when a service started via startService() creates a proxy when it
     * starts running
     */
    public BasePlayerProxy(Service service, Intent intent,
            Integer serviceInstanceId) {
        super(service, intent, serviceInstanceId);
        defaultValues.put(TiC.PROPERTY_VOLUME, 1.0f);
        defaultValues.put(TiC.PROPERTY_TIME, 0);
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected Class serviceClass() {
        return AudioService.class;
    }

    @Override
    public void unbindService() {
        super.unbindService();
        this.tiService = null;
    }

    protected void invokeBoundService() {
        this.tiService = (AudioService) this.service;
        if (needsStopOnBind) {
            needsStopOnBind = false;
            this.tiService.reset();
        }
        super.invokeBoundService();
    }

    private void runAction(String cmd) {
        Intent intent = new Intent(getIntent().getIntent());
        // intent.setAction(action);
        intent.putExtra(AudioService.CMDNAME, cmd);
        startService(intent);
        // else if (target == mPauseButton)
        // startService(new Intent(AudioStreamerExoService.ACTION_PAUSE));
        // else if (target == mSkipButton)
        // startService(new Intent(AudioStreamerExoService.ACTION_SKIP));
        // else if (target == mRewindButton)
        // startService(new Intent(AudioStreamerExoService.ACTION_REWIND));
        // else if (target == mStopButton)
        // startService(new Intent(AudioStreamerExoService.ACTION_STOP));
    }
    
    private boolean mEnableLockscreenControls = true;

    private List<Object> addItemToPlaylist(Object item) {
        if (mPlaylist == null) {
            mPlaylist = new ArrayList<Object>();
        }
        if (item instanceof Object[]) {
            List<Object> result = new ArrayList<Object>();
            Object[] array = (Object[]) item;
            for (int i = 0; i < array.length; i++) {
                result.addAll(addItemToPlaylist(array[i]));
            }
            return result;
        } else {
            mPlaylist.add(item);
            return Arrays.asList(new Object[] { item });
        }
    }

    private List<Object> removeItemsFromPlaylist(Object item) {
        if (mPlaylist != null) {
            if (item instanceof Object[]) {
                List<Object> result = new ArrayList<Object>();
                Object[] array = (Object[]) item;
                for (int i = 0; i < array.length; i++) {
                    result.addAll(removeItemsFromPlaylist(array[i]));
                }
                return result;
            } else {
                if (mPlaylist.remove(item)) {
                    return Arrays.asList(new Object[] { item });
                }
            }
        }

        return null;
    }
//
//    @Override
//    public void handleCreationDict(KrollDict options) {
//        super.handleCreationDict(options);
//        if (options.containsKey(TiC.PROPERTY_PLAYLIST)) {
//            addItemToPlaylist(options.get(TiC.PROPERTY_PLAYLIST));
//        }
//        // } else if (options.containsKey(TiC.PROPERTY_SOUND)) {
//        // FileProxy fp = (FileProxy) options.get(TiC.PROPERTY_SOUND);
//        // if (fp != null) {
//        // String url = fp.getNativePath();
//        // setProperty(TiC.PROPERTY_URL, url);
//        // }
//        // }
//        // if (options.containsKey(TiC.PROPERTY_ALLOW_BACKGROUND)) {
//        // allowBackground = options.optBoolean(TiC.PROPERTY_ALLOW_BACKGROUND,
//        // allowBackground);
//        // }
//        // Log.i(TAG,
//        // "Creating audio player proxy for url: "
//        // + TiConvert.toString(getProperty(TiC.PROPERTY_URL)),
//        // Log.DEBUG_MODE);
//    }
    
    @Override
    public void propertySet(String key, Object newValue, Object oldValue,
            boolean changedProperty) {
        switch (key) {
        case TiC.PROPERTY_PLAYLIST:
            mPlaylist = null;
            addItemToPlaylist(newValue);
            if (tiService != null && changedProperty) {
                tiService.setPlaylist(mPlaylist);
            }
            else if (changedProperty){
                needsStopOnBind = true;
            }
            break;
        case TiC.PROPERTY_VOLUME:
            if (tiService != null) {
                tiService.setVolume(TiConvert.toFloat(newValue, 1.0f));
            }
            break;
        case TiC.PROPERTY_TIME:
            if (tiService != null) {
                tiService.seek(TiConvert.toLong(newValue));
            }
            break;
        case TiC.PROPERTY_METADATA:
            if (tiService != null) {
                tiService.updateMetadata();
            }
            break;
        case TiC.PROPERTY_REPEAT_MODE:
            if (tiService != null) {
                tiService.setRepeatMode(TiConvert.toInt(newValue, tiService.getRepeatMode()));
            }
            break;
        case TiC.PROPERTY_SHUFFLE_MODE:
            if (tiService != null) {
                tiService.setShuffleMode(TiConvert.toInt(newValue, tiService.getShuffleMode()));
            }
            break;
        default:
            super.propertySet(key, newValue, oldValue, changedProperty);
            break;
        }
    }

    @Kroll.getProperty
    @Kroll.method
    public long getDuration() {
        if (tiService != null) {
            return tiService.duration();
        }
        return 0;
    }

    @Kroll.getProperty
    @Kroll.method
    public boolean isPlaying() {
        if (tiService != null) {
            return tiService.isPlaying();
        }
        return false;
    }

    @Kroll.getProperty
    @Kroll.method
    public boolean isPaused() {
        if (tiService != null) {
            return tiService.isPaused();
        }
        return false;
    }

    @Kroll.getProperty
    @Kroll.method
    public boolean isStopped() {
        if (tiService != null) {
            return tiService.isStopped();
        }
        return true;
    }

    @Kroll.method
    public void start(@Kroll.argument(optional = true) Object obj) {
        runAction(AudioService.CMDPLAY);
    }

    @Kroll.method
    public void play() {
        runAction(AudioService.CMDPLAY);
    }

    @Kroll.method
    public void pause() {
        runAction(AudioService.CMDPAUSE);
    }

    @Kroll.method
    public void stop() {
        runAction(AudioService.CMDSTOP);
    }

    @Kroll.method
    public void playPause() {
        runAction(AudioService.CMDTOGGLEPAUSE);
    }

    @Kroll.method
    public void next() {
        runAction(AudioService.CMDNEXT);
    }

    @Kroll.method
    public void previous() {
        runAction(AudioService.CMDPREVIOUS);
    }
    
    @Kroll.method
    public void seek(long time) {
        if (tiService != null) {
            tiService.seek(time);
        }
    }

    @Kroll.method
    public void close() {
        stopService();
    }

    @Kroll.method
    @Kroll.getProperty
    public double getTime() {
        if (tiService != null) {
            long time = tiService.position();
            setProperty(TiC.PROPERTY_TIME, time);
            return time;
        }
        return TiConvert.toLong(getProperty(TiC.PROPERTY_TIME), 0);
    }
    
    @Kroll.method
    @Kroll.getProperty
    public int getState() {
        if (tiService != null) {
            int state = tiService.state();
            setProperty(TiC.PROPERTY_STATE, state);
            return state;
        }
        return TiConvert.toInt(getProperty(TiC.PROPERTY_STATE), AudioService.State.STATE_STOPPED);
    }
    
    @Kroll.method
    @Kroll.getProperty
    public String getStateDescription() {
        if (tiService != null) {
            String description = tiService.stateDescription();
            return description;
        }
        return AudioService.STATE_DESC.get(AudioService.State.STATE_STOPPED);
    }
    
    @Kroll.method
    @Kroll.getProperty
    public Object getCurrentItem() {
        if (tiService != null) {
            return tiService.getCurrent();
        } else if (mPlaylist != null && mPlaylist.size() > 0) {
            return mPlaylist.get(0);
        }
        return null;
    }
    
    @Kroll.method
    @Kroll.getProperty
    public Object[] getPlaylist() {
        if (tiService != null) {
            return tiService.getPlaylist();
        } else if (mPlaylist != null) {
            return mPlaylist.toArray();
        }
        return null;
    }
    
    @Kroll.method
    @Kroll.getProperty
    public Object[] getQueue() {
        if (tiService != null) {
            return tiService.getQueue();
        }
        return null;
    }
    
    
    @Kroll.method
    @Kroll.getProperty
    public int getIndex() {
        if (tiService != null) {
            return tiService.getQueuePosition();
        } else if (mPlaylist != null) {
            return 0;
        }
        return -1;
    }


    public int getNotificationIcon() {
        return TiRHelper.getResource(getProperty("notifIcon"), "drawable",
                false);
    }

    public int getNotificationViewId() {
        return TiRHelper.getResource(getProperty("notifViewId"), "layout",
                false);
    }

    public int getNotificationExtandedViewId() {
        return TiRHelper.getResource(getProperty("notifExpandedViewId"),
                "layout", false);
    }

    public boolean getEnableLockscreenControls() {
        return TiConvert.toBoolean(getProperty("enableLockscreenControls"),
                true);
    }
    public boolean getEnableNotification() {
        return TiConvert.toBoolean(getProperty("enableNotification"),
                true);
    }


    @Kroll.method
    public void addToPlaylist(Object item) {
        List<Object> result = addItemToPlaylist(item);
        if (tiService != null) {
            tiService.addToPlayList(result, Integer.MAX_VALUE);
        }
    }

    @Kroll.method
    public void removeFromPlaylist(Object item) {
        List<Object> result = removeItemsFromPlaylist(item);
        if (tiService != null) {
            tiService.removeFromPlayList(result);
        }
    }
    
    public List<Object> getInternalPlaylist() {
        return mPlaylist;
    }

    @Override
    public String getApiName() {
        return "Ti.Audio.Streamer";
    }
}
