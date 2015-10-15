/**
 * Akylas
 * Copyright (c) 2014-2015 by Akylas. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.ui;

import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.TiContext;
import org.appcelerator.titanium.view.TiUIView;

import ti.modules.titanium.ui.widget.TiUIVolumeView;
import android.app.Activity;
import android.media.AudioManager;

@Kroll.proxy(creatableInModule=UIModule.class, propertyAccessors = {
    "leftTrackImage","rightTrackImage",
    "stream"
})
public class VolumeViewProxy extends ViewProxy
{
    public VolumeViewProxy()
    {
        super();
        defaultValues.put("stream", AudioManager.STREAM_MUSIC);
    }

    public VolumeViewProxy(TiContext tiContext)
    {
        this();
    }

    @Override
    public TiUIView createView(Activity activity)
    {
        return new TiUIVolumeView(this);
    }

    @Override
    public String getApiName()
    {
        return "Ti.UI.VolumeView";
    }
}
