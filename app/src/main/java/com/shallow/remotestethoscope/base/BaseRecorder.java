package com.shallow.remotestethoscope.base;

public abstract class BaseRecorder {
    protected int mVolume;

    public abstract int getRealVolume();

    /**
     * calculate the volume
     *
     * @param buffer buffer
     * @param readSize readSize
     */
    protected void calculateRealVolume(short[] buffer, int readSize) {
        double sum = 0;
        for (int i = 0; i < readSize; i++) {
            sum += buffer[i] * buffer[i];
        }
        if (readSize > 0) {
            double amplitude = sum / readSize;
            mVolume = (int) Math.sqrt(amplitude);
        }
    }
}
