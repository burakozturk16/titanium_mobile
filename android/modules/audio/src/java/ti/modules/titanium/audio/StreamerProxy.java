/**
 * Akylas
 * Copyright (c) 2014-2015 by Akylas. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */


package ti.modules.titanium.audio;

import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.TiC;

import ti.modules.titanium.audio.streamer.AudioStreamerService;

@Kroll.proxy(creatableInModule = AudioModule.class, propertyAccessors = {
        TiC.PROPERTY_VOLUME, 
        TiC.PROPERTY_METADATA, 
        TiC.PROPERTY_REPEAT_MODE,
        TiC.PROPERTY_SHUFFLE_MODE,
        })
public class StreamerProxy extends BasePlayerProxy {
    private static final String TAG = "StreamerProxy";

    @Override
    public String getApiName() {
        return "Ti.Audio.Streamer";
    }
    
    @SuppressWarnings("rawtypes")
    @Override
    protected Class serviceClass() {
        return AudioStreamerService.class;
    }
}
