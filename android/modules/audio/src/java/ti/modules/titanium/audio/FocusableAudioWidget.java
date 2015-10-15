package ti.modules.titanium.audio;

import android.view.KeyEvent;

public interface FocusableAudioWidget {
    public void pause();
    public void onMediaKey(KeyEvent key);
}
