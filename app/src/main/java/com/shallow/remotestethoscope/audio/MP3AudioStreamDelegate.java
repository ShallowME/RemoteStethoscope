package com.shallow.remotestethoscope.audio;

public interface MP3AudioStreamDelegate {
    void onAudioPlayerPlaybackStarted(MP3AudioStreamPlayer player);

    void onAudioPlayerStopped(MP3AudioStreamPlayer player);

    void onAudioPlayerError(MP3AudioStreamPlayer player);

    void onAudioPlayerBuffering(MP3AudioStreamPlayer player);
}
