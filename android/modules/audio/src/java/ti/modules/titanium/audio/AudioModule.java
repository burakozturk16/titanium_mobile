/**
 * Akylas
 * Copyright (c) 2014-2015 by Akylas. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

package ti.modules.titanium.audio;

import org.appcelerator.kroll.KrollModule;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.ContextSpecific;
import org.appcelerator.titanium.TiContext;

import ti.modules.titanium.audio.streamer.AudioStreamerExoService;
import android.os.Handler;

@Kroll.module @ContextSpecific
public class AudioModule extends KrollModule
	implements Handler.Callback
{
	private static final String TAG = "TiAudio";

	@Kroll.constant public static final int SHUFFLE_NONE = 0;
    @Kroll.constant public static final int SHUFFLE_SONGS = 1;
    @Kroll.constant public static final int SHUFFLE_RANDOM = 2;
    @Kroll.constant public static final int SHUFFLE_ALBUMS = SHUFFLE_SONGS;
    @Kroll.constant public static final int SHUFFLE_DEFAULT = SHUFFLE_NONE;
    @Kroll.constant public static final int REPEAT_NONE = 0;
	@Kroll.constant public static final int REPEAT_ONE = 1;
	@Kroll.constant public static final int REPEAT_ALL = 2;
    @Kroll.constant public static final int REPEAT_DEFAULT = REPEAT_NONE;


	public AudioModule()
	{
		super();
	}

	public AudioModule(TiContext tiContext)
	{
		this();
	}
	
    public static FocusableAudioWidget sFocusedAudioWidget = null;

    public static void widgetGetsFocused(final FocusableAudioWidget widget) {
        sFocusedAudioWidget = widget;
    }

    public static void widgetAbandonsFocused(
            final FocusableAudioWidget widget) {
        if (widget == sFocusedAudioWidget) {
            sFocusedAudioWidget = null;
        }
    }

    public static FocusableAudioWidget focusedAudioWidget() {
        return sFocusedAudioWidget;
    }
}

