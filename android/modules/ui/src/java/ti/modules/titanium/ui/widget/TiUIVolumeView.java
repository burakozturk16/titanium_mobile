/**
 * Akylas
 * Copyright (c) 2014-2015 by Akylas. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.ui.widget;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiFileHelper;
import org.appcelerator.titanium.util.TiUIHelper;
import org.appcelerator.titanium.view.TiUIView;

import android.content.Context;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.media.AudioManager;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.SeekBar;

public class TiUIVolumeView extends TiUIView
    implements SeekBar.OnSeekBarChangeListener
{
    private static final String TAG = "TiUIVolumeView";
    
    protected static final int TIFLAG_NEEDS_RANGE               = 0x00000001;
    protected static final int TIFLAG_NEEDS_CONTROLS            = 0x00000002;
    protected static final int TIFLAG_NEEDS_THUMBS              = 0x00000004;
    protected static final int TIFLAG_NEEDS_POS                 = 0x00000008;

//    private int min;
    private int max;
    private int volumeStream = AudioManager.STREAM_MUSIC;
    
    private String leftTrackImage = null;
    private String rightTrackImage = null;
    private boolean suppressEvent = true;
    
    private AudioManager mAudioManager = null; 

//    private SoftReference<Drawable> thumbDrawable;

    public TiUIVolumeView(final TiViewProxy proxy)
    {
        super(proxy);
        Log.d(TAG, "Creating a seekBar", Log.DEBUG_MODE);

        layoutParams.autoFillsWidth = true;
        
        mAudioManager = (AudioManager) TiApplication.getAppSystemService(Context.AUDIO_SERVICE);

        SeekBar seekBar = new SeekBar(proxy.getActivity())
        {
            @Override
            protected void onLayout(boolean changed, int left, int top, int right, int bottom)
            {
                super.onLayout(changed, left, top, right, bottom);
                TiUIHelper.firePostLayoutEvent(TiUIVolumeView.this);
            }
            
            @Override
            public boolean dispatchTouchEvent(MotionEvent event) {
                if (touchPassThrough == true)
                    return false;
                return super.dispatchTouchEvent(event);
            }
        };
        

        seekBar.setOnSeekBarChangeListener(this);
        setNativeView(seekBar);
    }
    
    private SeekBar getSeekBar() {
        return (SeekBar) getNativeView();
    }
    
    
    @Override
    public void propertySet(String key, Object newValue, Object oldValue,
            boolean changedProperty) {
        switch (key) {
        case "stream":
            volumeStream = TiConvert.toInt(newValue, volumeStream);
            suppressEvent = true;
            mProcessUpdateFlags |= TIFLAG_NEEDS_CONTROLS;
            break;
        
        case "thumbImage":
            updateThumb(TiConvert.toString(newValue));
            break;
        case "leftTrackImage":
            leftTrackImage = TiConvert.toString(newValue);
            mProcessUpdateFlags |= TIFLAG_NEEDS_THUMBS;
            break;
        case "rightTrackImage":
            rightTrackImage = TiConvert.toString(newValue);
            mProcessUpdateFlags |= TIFLAG_NEEDS_THUMBS;
            break;
        default:
            super.propertySet(key, newValue, oldValue, changedProperty);
            break;
        }
    }
    
    @Override
    protected void didProcessProperties() {
        super.didProcessProperties();
        if ((mProcessUpdateFlags & TIFLAG_NEEDS_THUMBS) != 0) {
            updateTrackingImages();
            mProcessUpdateFlags &= ~TIFLAG_NEEDS_THUMBS;
        }
        if ((mProcessUpdateFlags & TIFLAG_NEEDS_CONTROLS) != 0) {
            updateControl();
            mProcessUpdateFlags &= ~TIFLAG_NEEDS_CONTROLS;
        }
    }
    
    private void updateControl() {
        SeekBar seekBar = (SeekBar) getNativeView();
        this.max = mAudioManager.getStreamMaxVolume(volumeStream);
        int current = mAudioManager
                .getStreamVolume(volumeStream);
        seekBar.setMax(this.max);
        suppressEvent = false;
        seekBar.setProgress(current);
    }

    private void updateThumb(final String thumbImage) 
    {
        SeekBar seekBar = getSeekBar();
        TiFileHelper tfh = null;
        if (thumbImage != null) {
            if (tfh == null) {
                tfh = new TiFileHelper(seekBar.getContext());
            }
            String url = proxy.resolveUrl(null, thumbImage);
            Drawable thumb = tfh.loadDrawable(url);
            if (thumb != null) {
//                thumbDrawable = new SoftReference<Drawable>(thumb);
                seekBar.setThumb(thumb);
            } else {
                Log.e(TAG, "Unable to locate thumb image for progress bar: " + url);
            }
        } else {
            seekBar.setThumb(null);
        }
    }
    
    private void updateTrackingImages() 
    {
        TiFileHelper tfh = null;
        SeekBar seekBar = getSeekBar();
        if (leftTrackImage != null && rightTrackImage != null) {
            if (tfh == null) {
                tfh = new TiFileHelper(seekBar.getContext());
            }
            String leftUrl = proxy.resolveUrl(null, leftTrackImage);
            String rightUrl = proxy.resolveUrl(null, rightTrackImage);

            Drawable rightDrawable = tfh.loadDrawable(rightUrl);
            Drawable leftDrawable = tfh.loadDrawable(leftUrl);
            if (rightDrawable != null && leftDrawable != null) {
                Drawable[] lda = {
                    rightDrawable,
                    new ClipDrawable(leftDrawable, Gravity.LEFT, ClipDrawable.HORIZONTAL)
                };
                LayerDrawable ld = new LayerDrawable(lda);
                ld.setId(0, android.R.id.background);
                ld.setId(1, android.R.id.progress);
                seekBar.setProgressDrawable(ld);
            } else {
                if (leftDrawable == null) {
                    Log.e(TAG, "Unable to locate left image for progress bar: " + leftUrl);
                }
                if (rightDrawable == null) {
                    Log.e(TAG, "Unable to locate right image for progress bar: " + rightUrl);
                }
                // release
                leftDrawable = null;
                rightDrawable = null;
            }
        } else if (leftTrackImage == null && rightTrackImage == null) {
            seekBar.setProgressDrawable(null);
        } else {
            Log.w(TAG, "Custom tracking images must both be set before they will be drawn.");
        }
    }
    
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (suppressEvent) {
            return;
        }
        mAudioManager.setStreamVolume(volumeStream, progress, 0);
        if (!hasListeners(TiC.EVENT_CHANGE, false)) {
            return;
        }
        KrollDict data = new KrollDict();
        data.put(TiC.PROPERTY_VALUE, progress);
        data.put(TiC.PROPERTY_MAX, this.max);
        fireEvent(TiC.EVENT_CHANGE, data, false, false);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        if (hasListeners(TiC.EVENT_START, false)){
            KrollDict data = new KrollDict();
            data.put(TiC.PROPERTY_VALUE, value());
            data.put(TiC.PROPERTY_MAX, this.max);
            fireEvent(TiC.EVENT_START, data, false, false);
        }
    }
    
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (hasListeners(TiC.EVENT_STOP, false)){
            KrollDict data = new KrollDict();
            data.put(TiC.PROPERTY_VALUE, value());
            data.put(TiC.PROPERTY_MAX, this.max);
            fireEvent(TiC.EVENT_STOP, data, false, false);
        }
    }

    private float value() {
        return ((SeekBar) getNativeView()).getProgress();
    }

}
