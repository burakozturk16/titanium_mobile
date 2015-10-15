/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2015 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.ui.widget;

import java.lang.ref.SoftReference;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiFileHelper;
import org.appcelerator.titanium.util.TiUIHelper;
import org.appcelerator.titanium.view.TiUIView;

import android.graphics.Rect;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.SeekBar;

public class TiUISlider extends TiUIView
	implements SeekBar.OnSeekBarChangeListener
{
	private static final String TAG = "TiUISlider";

	private int min;
	private int max;
	private float pos;
	private int offset;
    private int minRange;
    private int minRangeValue;
    private boolean minRangeDefined = false;
    private int maxRange;
    private int maxRangeValue;
    private boolean maxRangeDefined = false;
	private int scaleFactor;
	private ClipDrawable rightClipDrawable;
	private String leftTrackImage = null;
	private String rightTrackImage = null;
	private boolean suppressEvent = false;

    protected static final int TIFLAG_NEEDS_RANGE               = 0x00000001;
    protected static final int TIFLAG_NEEDS_CONTROLS            = 0x00000002;
    protected static final int TIFLAG_NEEDS_THUMBS              = 0x00000004;
    protected static final int TIFLAG_NEEDS_POS                 = 0x00000008;
	
	private SoftReference<Drawable> thumbDrawable;

	public TiUISlider(final TiViewProxy proxy)
	{
		super(proxy);
		Log.d(TAG, "Creating a seekBar", Log.DEBUG_MODE);

		layoutParams.autoFillsWidth = true;
		layoutParams.sizeOrFillWidthEnabled = true;

		this.min = 0;
		this.max = 1;
		this.pos = 0;
		
		SeekBar seekBar = new SeekBar(proxy.getActivity())
		{
			@Override
			protected void onLayout(boolean changed, int left, int top, int right, int bottom)
			{
				super.onLayout(changed, left, top, right, bottom);
				TiUIHelper.firePostLayoutEvent(TiUISlider.this);
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
	
    @Override
    public void propertySet(String key, Object newValue, Object oldValue,
            boolean changedProperty) {

        switch (key) {
        case TiC.PROPERTY_VALUE:
            pos = TiConvert.toFloat(newValue, 0);
            mProcessUpdateFlags |= TIFLAG_NEEDS_CONTROLS;
            break;
        case TiC.PROPERTY_MIN:
            min = TiConvert.toInt(newValue, 0);
            if (!minRangeDefined) {
                minRangeValue = min;
            }
            mProcessUpdateFlags |= TIFLAG_NEEDS_RANGE | TIFLAG_NEEDS_CONTROLS;
            break;
        case TiC.PROPERTY_MAX:
            max = TiConvert.toInt(newValue, 0);
            if (!maxRangeDefined) {
                maxRangeValue = max;
            }
            mProcessUpdateFlags |= TIFLAG_NEEDS_RANGE | TIFLAG_NEEDS_CONTROLS;
            break;
        case "minRange":
            minRangeValue = TiConvert.toInt(newValue, 0);
            minRangeDefined = true;
            mProcessUpdateFlags |= TIFLAG_NEEDS_RANGE | TIFLAG_NEEDS_CONTROLS;
            break;
        case "maxRange":
            maxRangeValue = TiConvert.toInt(newValue, 0);
            maxRangeDefined = true;
            mProcessUpdateFlags |= TIFLAG_NEEDS_RANGE | TIFLAG_NEEDS_CONTROLS;
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
    
    private SeekBar getSeekBar() {
        return (SeekBar) getNativeView();
    }
    
    @Override
    protected void didProcessProperties() {
        super.didProcessProperties();
        if ((mProcessUpdateFlags & TIFLAG_NEEDS_THUMBS) != 0) {
            updateTrackingImages();
            mProcessUpdateFlags &= ~TIFLAG_NEEDS_THUMBS;
        }
        if ((mProcessUpdateFlags & TIFLAG_NEEDS_RANGE) != 0) {
            updateRange();
            mProcessUpdateFlags &= ~TIFLAG_NEEDS_RANGE;
        }
        if ((mProcessUpdateFlags & TIFLAG_NEEDS_CONTROLS) != 0) {
            updateControl();
            mProcessUpdateFlags &= ~TIFLAG_NEEDS_CONTROLS;
        }
		updateRightDrawable();
    }

	private void updateRightDrawable()
	{
		if(rightClipDrawable != null) {
			SeekBar seekBar = (SeekBar) getNativeView();
			double percent = (double) seekBar.getProgress()/ (double)seekBar.getMax();
			int level = 10000 - (int)Math.floor(percent*10000);
			rightClipDrawable.setLevel(level);
		}
	}

	private void updateRange() {
		minRange = Math.max(minRangeValue, min);
		minRange = Math.min(minRangeValue, max);
		proxy.setProperty("minRange", minRange);
		
		maxRange = Math.min(maxRangeValue, max);
		maxRange = Math.max(maxRangeValue, minRange);
		proxy.setProperty("maxRange", maxRange);
	}
	
	private void updateControl() {
		offset = -min;
		scaleFactor = 100;
		int length = (int) Math.floor(Math.sqrt(Math.pow(max - min, 2)));
		if ( (length > 0) && (Integer.MAX_VALUE/length < scaleFactor) ) {
			scaleFactor = Integer.MAX_VALUE/length;
			scaleFactor = (scaleFactor == 0) ? 1 : scaleFactor;
		}
		length *= scaleFactor;
        SeekBar seekBar = getSeekBar();
        if (pos < minRange) {
            pos = minRange;
        }
        if (pos > maxRange) {
            pos = maxRange;
        }
		int curPos = (int)Math.floor(scaleFactor* (pos + offset));
		suppressEvent = true;
		seekBar.setMax(length);
        suppressEvent = false;
        if (seekBar.getProgress() != curPos) {
            seekBar.setProgress(curPos);
        }
	}

	private void updateThumb(String thumbImage) 
	{
        SeekBar seekBar = getSeekBar();
		if (thumbImage != null) {
			String url = proxy.resolveUrl(null, thumbImage);
			Drawable thumb = TiFileHelper.loadDrawable(url);
		 	if (thumb != null) {
				thumbDrawable = new SoftReference<Drawable>(thumb);
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
        SeekBar seekBar = getSeekBar();
		Drawable leftDrawable = null;
		Drawable rightDrawable = null;
		if (leftTrackImage != null) {
			String leftUrl = proxy.resolveUrl(null, leftTrackImage);
			if(leftUrl != null) {
				leftDrawable = TiFileHelper.loadDrawable(leftUrl);
			}
		}
		if (rightTrackImage != null) {
			String rightUrl = proxy.resolveUrl(null, rightTrackImage);
			if(rightUrl != null) {
				rightDrawable = TiFileHelper.loadDrawable(rightUrl);
			}
		}
		LayerDrawable ld = null;
		if(rightDrawable == null) {
			Drawable[] lda = {new ClipDrawable(leftDrawable, Gravity.LEFT, ClipDrawable.HORIZONTAL)};
			ld = new LayerDrawable(lda);
			ld.setId(0, android.R.id.progress);
		} else if(leftDrawable == null) {
			rightClipDrawable = new ClipDrawable(rightDrawable, Gravity.RIGHT, ClipDrawable.HORIZONTAL);
			Drawable[] lda = {rightClipDrawable};
			ld = new LayerDrawable(lda);
			ld.setId(0, android.R.id.secondaryProgress);
		} else {
			Drawable[] lda = {rightDrawable, new ClipDrawable(leftDrawable, Gravity.LEFT, ClipDrawable.HORIZONTAL)};
			ld = new LayerDrawable(lda);
			ld.setId(0, android.R.id.background);
			ld.setId(1, android.R.id.progress);
		}
		seekBar.setProgressDrawable(ld);
	}
	


	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
	    if (suppressEvent) {
	        return;
	    }
		pos = progress*1.0f/scaleFactor;
		
		// Range check
		int actualMinRange = minRange + offset;
		int actualMaxRange = maxRange + offset;
		
		if (pos < actualMinRange) {
			seekBar.setProgress(actualMinRange*scaleFactor);
			pos = minRange;
		} else if (pos > actualMaxRange) {
			seekBar.setProgress(actualMaxRange*scaleFactor);
			pos = maxRange;
		}

		updateRightDrawable();

        float scaledValue = scaledValue();
        proxy.setProperty(TiC.PROPERTY_VALUE, scaledValue);
        Log.d(TAG,
                "Progress " + seekBar.getProgress() + " ScaleFactor " + scaleFactor + " Calculated Position " + pos
                    + " ScaledValue " + scaledValue + " Min " + min + " Max" + max + " MinRange" + minRange + " MaxRange"
                    + maxRange, Log.DEBUG_MODE);
        if (hasListeners(TiC.EVENT_CHANGE, false)) {
            Drawable thumb = (thumbDrawable != null) ? thumbDrawable.get() : null;
            KrollDict offset = new KrollDict();
            offset.put(TiC.EVENT_PROPERTY_X, 0);
            offset.put(TiC.EVENT_PROPERTY_Y, 0);
            KrollDict size = new KrollDict();
            size.put(TiC.PROPERTY_WIDTH, 0);
            size.put(TiC.PROPERTY_HEIGHT, 0);
            if (thumb != null) {
                Rect thumbBounds = thumb.getBounds();
                if (thumbBounds != null) {
                    offset.put(TiC.EVENT_PROPERTY_X, thumbBounds.left - seekBar.getThumbOffset());
                    offset.put(TiC.EVENT_PROPERTY_Y, thumbBounds.top);
                    size.put(TiC.PROPERTY_WIDTH, thumbBounds.width());
                    size.put(TiC.PROPERTY_HEIGHT, thumbBounds.height());
                }
            }
            KrollDict data = new KrollDict();
            
            data.put(TiC.PROPERTY_VALUE, scaledValue);
            data.put(TiC.EVENT_PROPERTY_THUMB_OFFSET, offset);
            data.put(TiC.EVENT_PROPERTY_THUMB_SIZE, size);

            fireEvent(TiC.EVENT_CHANGE, data, false, false);
        }
	}

	public void onStartTrackingTouch(SeekBar seekBar) {
		if (hasListeners(TiC.EVENT_START)){
			KrollDict data = new KrollDict();
			data.put(TiC.PROPERTY_VALUE, scaledValue());
			fireEvent(TiC.EVENT_START, data, false, false);
		}
	}

	public void onStopTrackingTouch(SeekBar seekBar) {
		if (hasListeners(TiC.EVENT_STOP)){
			KrollDict data = new KrollDict();
			data.put(TiC.PROPERTY_VALUE, scaledValue());
			fireEvent(TiC.EVENT_STOP, data, false, false);
		}
	}

	private float scaledValue() {
		return pos - offset;
	}

}
