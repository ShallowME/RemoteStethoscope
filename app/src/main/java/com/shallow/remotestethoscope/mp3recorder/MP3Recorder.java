package com.shallow.remotestethoscope.mp3recorder;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.NoiseSuppressor;
import android.os.Handler;
import android.os.Process;


import com.shallow.remotestethoscope.base.BaseRecorder;
import com.shallow.remotestethoscope.mp3recorder.util.LameUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class MP3Recorder extends BaseRecorder {
    //=======================AudioRecord & AudioTrack Default Setting==========================

    //Use voice_communication as audio source
    private static final int DEFAULT_AUDIO_SOURCE = MediaRecorder.AudioSource.VOICE_COMMUNICATION;

    //Use 44.1kHz as sampling rate
    private static final int DEFAULT_SAMPLING_RATE = 44100;

    //Use mono soundtrack while recording
    private static final int DEFAULT_RECORD_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;

    //Use mono soundtrack while playing
    private static final int DEFAULT_PLAY_CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO;

    //Use PCM 16-byte encoding
    private static final PCMFormat DEFAULT_AUDIO_FORMAT = PCMFormat.PCM_16BIT;

    //=======================Lame Default setting=============================

    //Default lame quality parameter
    private static final int DEFAULT_LAME_MP3_QUALITY = 7;

    //Default lame channel parameter
    private static final int DEFAULT_LAME_IN_CHANNEL = 1;

    //Default mp3 encoding bit rate
    private static final int DEFAULT_LAME_MP3_BIT_RATE = 32;


    //==================================================================

    //Every 160 frames as a cycle to notify encoding thread
    private static final int FRAME_COUNT = 160;
    public static final int ERROR_TYPE = 22;

    private AudioRecord mAudioRecord = null;
    private AudioTrack mAudioTrack = null;
    private AcousticEchoCanceler acousticEchoCanceler;
    private NoiseSuppressor noiseSuppressor;
    private DataEncodeThread mEncodeThread;
    private File mRecordFile;
    private ArrayList<Integer> dataList;
    private Handler errorHandler;

    private short[] mPCMBuffer;
    private boolean mIsRecording = false;
    private boolean mSendError;
    private boolean mPause;

    private int mRecBufferSize;
    private int mPlayBufferSize;

    private int mMaxSize;

    private int mWaveSpeed = 300;

    private int audioSessionId = -1;

    private ArrayList<Double> input = new ArrayList<>();
    private ArrayList<Double> output = new ArrayList<>();
    private static double[] audla = {1.0000, -2.8869, 3.2397 , -1.6565, 0.3240};
    private static double[] audlb = {0.0013, 0.0051, 0.0076, 0.0051, 0.0013};

    /**
     * Default constructor. Setup recorder with default sampling rate, mono channel
     * and 10 bits PCM
     * @param recordFile target file
     */
    public MP3Recorder(File recordFile) {
        this.mRecordFile = recordFile;
    }


    public void start() throws IOException {
        if (mIsRecording) {
            return;
        }
        mIsRecording = true;
        initAudioRecorder();

        try {
            mAudioRecord.startRecording();
            mAudioTrack.play();
        } catch (Exception e) {
            System.out.println("start recording error");
            e.printStackTrace();
        }

        new Thread() {
            boolean isError = false;

            @Override
            public void run() {
                android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
                while (mIsRecording) {
                    int readSize = mAudioRecord.read(mPCMBuffer, 0, mRecBufferSize);

                    if (readSize == AudioRecord.ERROR_INVALID_OPERATION ||
                            readSize == AudioRecord.ERROR_BAD_VALUE) {
                        if (errorHandler != null && !mSendError) {
                            mSendError = true;
                            errorHandler.sendEmptyMessage(ERROR_TYPE);
                            mIsRecording = false;
                            isError = true;
                        }
                    } else {
                        if (readSize > 0) {
                            if (mPause) {
                                continue;
                            }
                            short[] tmpBuf = new short[readSize];
                            System.arraycopy(mPCMBuffer, 0, tmpBuf, 0, readSize);
                            calc(tmpBuf, 0, tmpBuf.length);

                            mAudioTrack.write(tmpBuf, 0, tmpBuf.length);

                            mEncodeThread.addTask(mPCMBuffer, readSize);
                            calculateRealVolume(mPCMBuffer, readSize);
                            sendData(mPCMBuffer, readSize);
                        } else {
                            if (errorHandler != null && !mSendError) {
                                mSendError = true;
                                errorHandler.sendEmptyMessage(ERROR_TYPE);
                                mIsRecording = false;
                                isError = true;
                            }
                        }
                    }
                }

                try {
                    mAudioRecord.stop();
                    mAudioRecord.release();
                    mAudioRecord = null;

                    mAudioTrack.stop();
                    mAudioTrack.release();
                    mAudioTrack = null;

                } catch (Exception e) {
                    System.out.println("stop error");
                    e.printStackTrace();
                }

                if (isError) {
                    mEncodeThread.sendErrorMessage();
                } else {
                    mEncodeThread.sendStopMessage();
                }
            }
        }.start();

    }

    private void sendData(short[] shorts, int readSize) {
        if (dataList != null) {
            int length = readSize / mWaveSpeed;
            short resultMax = 0, resultMin = 0;
            for (short i = 0, k = 0; i < length; i++, k += mWaveSpeed) {
                int sum = 0;
                for (short j = k, max = 0, min = 1000; j < k + mWaveSpeed; j++) {
                    if (shorts[j] > max) {
                        max = shorts[j];
                        resultMax = max;
                    } else if (shorts[j] < min) {
                        min = shorts[j];
                        resultMin = min;
                    }
                    sum += Math.abs((int)shorts[j]);
                }
                synchronized (dataList) {
                    if (dataList.size() > mMaxSize) {
                        dataList.remove(0);
                    }
//                    dataList.add((int)resultMax);
                    dataList.add((int)sum / mWaveSpeed);
                }
            }
        }
    }

    private int filter(int val) {
        double result = 0;
        if (input.size() < 4) {
            input.add((double)val);
            output.add((double)val);
            result = val;
        } else {
            result = audlb[0] * val;
            for (int i = 1; i <= 4; i++) {
                result = -audla[i] * output.get(4 - i) + audlb[i] * input.get(4 - i);
            }
            input.remove(0);
            input.add((double)val);
            output.remove(0);
            output.add(result);
        }
        return (int)result;
    }

    @Override
    public int getRealVolume() {
        return mVolume;
    }

    private static final int MAX_VOLUME = 2000;

    /**
     * Get relative volume
     *
     * @return max volume
     */
    public int getVolume() {
        if (mVolume >= MAX_VOLUME) {
            return MAX_VOLUME;
        }
        return mVolume;
    }

    public int getMaxVolume() { return MAX_VOLUME; }

    public void stop() {
        mPause = false;
        mIsRecording = false;
    }

    public boolean isRecording() { return mIsRecording; }

    /**
     * Setup maximum data list,
     * which is generally the size of control or line offset
     *
     * @param dataList data
     * @param maxSize maximum count
     */
    public void setDataList(ArrayList<Integer> dataList, int maxSize){
        this.dataList = dataList;
        this.mMaxSize = maxSize;
    }

    public boolean isPause() {
        return mPause;
    }

    public void setPause(boolean pause) {
        this.mPause = pause;
    }

    /**
     * Setup error handler
     *
     * @param errorHandler  for error notification
     */
    public void setErrorHandler(Handler errorHandler) { this.errorHandler = errorHandler; }

    public int getWaveSpeed() {
        return mWaveSpeed;
    }

    /**
     * Setup speed of pcm data.
     * Default 300.
     *
     * @param waveSpeed speed of pcm data
     */
    public void setWaveSpeed(int waveSpeed) {
        if (mWaveSpeed <= 0) {
            return;
        }
        this.mWaveSpeed = waveSpeed;
    }



    private void initAudioRecorder() throws IOException {
        //Get minimum size of buffer that saves sound data
        mRecBufferSize = AudioRecord.getMinBufferSize(DEFAULT_SAMPLING_RATE,
                DEFAULT_RECORD_CHANNEL_CONFIG, DEFAULT_AUDIO_FORMAT.getAudioFormat());

        //Get minimum size of buffer that plays sound data
        mPlayBufferSize = AudioTrack.getMinBufferSize(DEFAULT_SAMPLING_RATE,
                DEFAULT_PLAY_CHANNEL_CONFIG, DEFAULT_AUDIO_FORMAT.getAudioFormat());

        int bytesPerFrame = DEFAULT_AUDIO_FORMAT.getBytePerFrame();

        // Get number of samples. Calculate the buffer size
        // (Round up to the factor of given frame size)
        int frameSize = mRecBufferSize / bytesPerFrame;
        if (frameSize % FRAME_COUNT != 0) {
            frameSize += (FRAME_COUNT - frameSize % FRAME_COUNT);
            mRecBufferSize = frameSize * bytesPerFrame;
        }

        mAudioRecord = new AudioRecord(DEFAULT_AUDIO_SOURCE, DEFAULT_SAMPLING_RATE,
                DEFAULT_RECORD_CHANNEL_CONFIG, DEFAULT_AUDIO_FORMAT.getAudioFormat(), mRecBufferSize);

        audioSessionId = mAudioRecord.getAudioSessionId();
        initAEC();
        initNS();
        initAudioTrack();

        mPCMBuffer = new short[mRecBufferSize];

        //Initialize lame buffer
        //Mp3 sampling rate is the same as the recorded pam sampling rate
        //The bit rate is 32kbps
        LameUtil.init(DEFAULT_SAMPLING_RATE, DEFAULT_LAME_IN_CHANNEL, DEFAULT_SAMPLING_RATE, DEFAULT_LAME_MP3_BIT_RATE, DEFAULT_LAME_MP3_QUALITY);

        mEncodeThread = new DataEncodeThread(mRecordFile, mRecBufferSize);
        mEncodeThread.start();
        mAudioRecord.setRecordPositionUpdateListener(mEncodeThread, mEncodeThread.getHandler());
        mAudioRecord.setPositionNotificationPeriod(FRAME_COUNT);
    }


    public static void deleteFile(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            if (file.isFile()) {
                file.delete();
            } else {
                String[] filePaths = file.list();
                for (String path : filePaths) {
                    deleteFile(filePath + File.separator + filePath);
                }
            }
        }
    }

    private void initAudioTrack() {
        if (mAudioTrack == null) {
            if (audioSessionId == -1) {
                mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, DEFAULT_SAMPLING_RATE,
                        DEFAULT_PLAY_CHANNEL_CONFIG, DEFAULT_AUDIO_FORMAT.getAudioFormat(), mPlayBufferSize,
                        AudioTrack.MODE_STREAM);
            } else {
                mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, DEFAULT_SAMPLING_RATE,
                        DEFAULT_PLAY_CHANNEL_CONFIG, DEFAULT_AUDIO_FORMAT.getAudioFormat(), mPlayBufferSize,
                        AudioTrack.MODE_STREAM, audioSessionId);
            }
        }
    }

    private void initAEC() {
        if (AcousticEchoCanceler.isAvailable()) {
            if (acousticEchoCanceler == null) {
                acousticEchoCanceler = AcousticEchoCanceler.create(audioSessionId);
                if (acousticEchoCanceler != null) {
                    acousticEchoCanceler.setEnabled(true);
                }
            }
        }
    }

    private void initNS() {
        if (NoiseSuppressor.isAvailable()) {
            if (noiseSuppressor == null) {
                noiseSuppressor = NoiseSuppressor.create(audioSessionId);
                if (noiseSuppressor != null) {
                    noiseSuppressor.setEnabled(true);
                }
            }
        }
    }

     private void calc(short[] lin,int off,int len) {
        int i,j;
        for (i = 0; i < len; i++) {
            j = lin[i+off];
            lin[i+off] = (short)(j>>2);
        }
    }

}
