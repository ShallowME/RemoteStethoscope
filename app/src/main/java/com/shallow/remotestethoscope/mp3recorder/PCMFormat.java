package com.shallow.remotestethoscope.mp3recorder;

import android.media.AudioFormat;

public enum PCMFormat {
    PCM_8BIT (1, AudioFormat.ENCODING_PCM_8BIT),
    PCM_16BIT (2, AudioFormat.ENCODING_PCM_16BIT);

    private int bytePerFrame;
    private int audioFormat;

    PCMFormat(int bytePerFrame, int audioFormat) {
        this.bytePerFrame = bytePerFrame;
        this.audioFormat = audioFormat;
    }

    public int getBytePerFrame() {
        return bytePerFrame;
    }

    public int getAudioFormat() {
        return audioFormat;
    }

}
