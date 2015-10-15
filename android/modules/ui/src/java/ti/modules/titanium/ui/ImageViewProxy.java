/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.ui;

import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.TiBlob;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiContext;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.view.TiUIView;

import ti.modules.titanium.ui.widget.TiUIImageView;
import android.app.Activity;

@Kroll.proxy(creatableInModule=UIModule.class, propertyAccessors = {
	TiC.PROPERTY_DECODE_RETRIES,
	TiC.PROPERTY_AUTOROTATE,
	TiC.PROPERTY_DEFAULT_IMAGE,
	TiC.PROPERTY_DURATION,
	TiC.PROPERTY_ENABLE_ZOOM_CONTROLS,
	TiC.PROPERTY_IMAGE,
	TiC.PROPERTY_IMAGES,
	TiC.PROPERTY_REPEAT_COUNT,
	TiC.PROPERTY_URL,
	TiC.PROPERTY_LOCAL_LOAD_SYNC,
	TiC.PROPERTY_ANIMATION_DURATION,
	TiC.PROPERTY_ANIMATED_IMAGES,
	TiC.PROPERTY_AUTOREVERSE,
	TiC.PROPERTY_REVERSE,
    TiC.PROPERTY_TRANSITION,
    TiC.PROPERTY_INDEX,
    TiC.PROPERTY_PROGRESS,
    TiC.PROPERTY_SCALE_TYPE,
	TiC.PROPERTY_ONLY_TRANSITION_IF_REMOTE,
})
public class ImageViewProxy extends ViewProxy
{
	public ImageViewProxy()
	{
		super();
		defaultValues.put(TiC.PROPERTY_SCALE_TYPE, UIModule.SCALE_TYPE_ASPECT_FIT);
	}

	public ImageViewProxy(TiContext tiContext)
	{
		this();
	}

	@Override
	public TiUIView createView(Activity activity) {
		return new TiUIImageView(this);
	}

	private TiUIImageView getImageView() {
		return (TiUIImageView)view;
	}
	
	@Kroll.method
	public void start() {
		setProperty("animating", true);
		setProperty("paused", false);
		if (view != null) {
		    getImageView().start();
		}
	}
	
	@Kroll.method
	public void stop() {
		setProperty("animating", false);
		setProperty("paused", false);
		if (view != null) {
		    getImageView().stop();
		}
	}
	
	@Kroll.method
	public void pause() {
		setProperty("paused", true);
		if (view != null) {
		    getImageView().pause();
		}
	}
	
	@Kroll.method
	public void resume() {
        boolean animating = TiConvert.toBoolean(getProperty("animating"), false);
        if (!animating) {
            start();
            return;
        }
		setProperty("paused", false);
		if (view != null) {
		    getImageView().resume();
		}
	}
	
	@Kroll.method
	public void pauseOrResume() {
		boolean paused = TiConvert.toBoolean(getProperty("paused"), true);
		if (paused) resume();
		else pause();
	}
	
	@Kroll.method
	public TiBlob toBlob() {
		return ((TiUIImageView)getOrCreateView()).toBlob();
	}

	@Override
	public String getApiName()
	{
		return "Ti.UI.ImageView";
	}
	
    @Kroll.getProperty
    @Kroll.method
    public int getIndex() {
        if (view != null) {
            getImageView().getCurrentIndex();
        }
        return 0;
    }
    
    @Kroll.getProperty
    @Kroll.method
    public float getProgress() {
        if (view != null) {
            getImageView().getProgress();
        }
        return 1;
    }
}
