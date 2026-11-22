package com.carlaunchercp;

import android.content.Context;
import android.media.AudioManager;
import android.os.SystemClock;
import android.view.KeyEvent;

/**
 * Transport commands for whatever app is playing right now.
 *
 * <p>The launcher does not bind to a specific player: it pushes the hardware media keys
 * into the system, which routes them to the app that currently owns the media session
 * (Spotify, YouTube Music, the FM radio app, a podcast player, ...). That is the same path
 * a steering-wheel button uses, so it works without any special permission and without
 * knowing which player is running.</p>
 */
public final class MediaKeys {

    public static final int PREVIOUS = KeyEvent.KEYCODE_MEDIA_PREVIOUS;
    public static final int PLAY_PAUSE = KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE;
    public static final int NEXT = KeyEvent.KEYCODE_MEDIA_NEXT;

    private MediaKeys() {
    }

    /** Sends a full press (down + up) of a media key to the active media session. */
    public static void send(Context context, int keyCode) {
        AudioManager audio = (AudioManager) context.getApplicationContext()
                .getSystemService(Context.AUDIO_SERVICE);
        if (audio == null) return;

        long downTime = SystemClock.uptimeMillis();
        audio.dispatchMediaKeyEvent(new KeyEvent(downTime, downTime,
                KeyEvent.ACTION_DOWN, keyCode, 0));
        audio.dispatchMediaKeyEvent(new KeyEvent(downTime, downTime + 80L,
                KeyEvent.ACTION_UP, keyCode, 0));
    }
}
