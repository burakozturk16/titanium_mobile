package com.akylas.titanium.ks;

import ti.modules.titanium.audio.AudioModule;
import ti.modules.titanium.audio.FocusableAudioWidget;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.view.KeyEvent;

public class TiMediaButtonEventReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        FocusableAudioWidget focusedAudioWidget = AudioModule.focusedAudioWidget();
        if (intent.getAction() == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
            if (focusedAudioWidget != null) {
                focusedAudioWidget.pause();
            }
        } else if (intent.getAction() == Intent.ACTION_MEDIA_BUTTON) {
            // The event will fire twice, up and down.
            // we only want to handle the down event though.
            KeyEvent key = (KeyEvent) intent
                    .getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (key.getAction() != KeyEvent.ACTION_DOWN)
                return;
            if (focusedAudioWidget != null) {
                focusedAudioWidget.onMediaKey(key);
            }
        }
    }
}
