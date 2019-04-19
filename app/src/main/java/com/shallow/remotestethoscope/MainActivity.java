package com.shallow.remotestethoscope;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.Toast;


import com.shallow.remotestethoscope.base.FileUtils;
import com.shallow.remotestethoscope.mp3recorder.MP3Recorder;
import com.shallow.remotestethoscope.waveview.AudioWaveView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private boolean op_click_flag = true;
    private boolean ls_click_flag =true;
    private boolean mIsRecord = false;

    private Chronometer chronometer;

    private ImageButton play_pause_btn;
    private ImageButton list_save_btn;
    private ImageButton cancel_record_btn;

    private long mRecordTime = 0;
    private String filePath;

    MP3Recorder mRecorder;
    AudioWaveView audioWave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar_main);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
//        audioWave = new AudioWaveView(this);
        chronometer = findViewById(R.id.timer);
        chronometer.setBase(0);
        chronometer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            @Override
            public void onChronometerTick(Chronometer chronometer) {
                long seconds = SystemClock.elapsedRealtime() - chronometer.getBase();
                Date date = new Date(seconds);
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.US);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                chronometer.setText(sdf.format(date));
            }
        });

        play_pause_btn = findViewById(R.id.on_pause_button);
        list_save_btn = findViewById(R.id.list_save_button);
        cancel_record_btn = findViewById(R.id.cancel_record_button);
        play_pause_btn.setOnClickListener(this);
        list_save_btn.setOnClickListener(this);
        cancel_record_btn.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.on_pause_button:
                if (op_click_flag) {
                    if (mRecordTime == 0) {
                        chronometer.setBase(SystemClock.elapsedRealtime());
                        resolveRecord();
                        list_save_btn.setImageResource(R.mipmap.ic_action_correct);
                        ls_click_flag = false;
                    } else {
                        chronometer.setBase(chronometer.getBase() + (SystemClock.elapsedRealtime() - mRecordTime));
                        resolvePause(false);
                    }
                    chronometer.start();
                    play_pause_btn.setImageResource(R.mipmap.ic_action_pause);
                    list_save_btn.setClickable(false);
                    list_save_btn.getBackground().setAlpha(100);

                }
                else {
                    chronometer.stop();
                    resolvePause(true);
                    mRecordTime = SystemClock.elapsedRealtime();
                    play_pause_btn.setImageResource(R.mipmap.ic_action_play_arrow);
                    list_save_btn.setClickable(false);
                    list_save_btn.getBackground().setAlpha(255);
                }
                op_click_flag = !op_click_flag;
                break;

            case R.id.list_save_button:
                break;
        }
    }

    /**
     * 开始录音
     */
    public void resolveRecord() {
        filePath = FileUtils.getAppPath();
        File file = new File(filePath);
        if (!file.exists()) {
            if (!file.mkdir()) {
                Toast.makeText(this, "创建文件失败", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        int offset = dip2px(this, 1);
        filePath = FileUtils.getAppPath() + UUID.randomUUID().toString() + ".mp3";
        File mp3File = new File(filePath);
        mRecorder = new MP3Recorder(mp3File);
        audioWave = findViewById(R.id.audioWave);
        int size = getScreenWidth(this) / offset;
        mRecorder.setDataList(audioWave.getRecList(), size);

        mRecorder.setErrorHandler(new Handler() {

            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == MP3Recorder.ERROR_TYPE) {
                    Toast.makeText(MainActivity.this, "没有麦克风权限", Toast.LENGTH_SHORT).show();

                }
            }
        });

        try {
            mRecorder.start();
            audioWave.startView();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "录音出现异常", Toast.LENGTH_SHORT).show();
            resolveError();
            return;
        }
        mIsRecord = true;
    }

    /**
     * 暂停录音
     */
    private void resolvePause(boolean pause) {
        if (!mIsRecord) {
            return;
        }
        mRecorder.setPause(pause);
        audioWave.setPause(pause);
    }

    /**
     * 停止录音
     */
    public void resolveStopRecord() {
        if (mRecorder != null && mRecorder.isRecording()) {
            mRecorder.setPause(false);
            mRecorder.stop();
            audioWave.stopView();
        }
        mIsRecord = false;
    }

    private void resolveError() {
        FileUtils.deleteFile(filePath);
        filePath = "";
        if (mRecorder != null && mRecorder.isRecording()) {
            mRecorder.stop();
            audioWave.stopView();
        }
    }

    /**
     * 获取屏幕的宽度px
     *
     * @param context 上下文
     * @return 屏幕宽px
     */
    public static int getScreenWidth(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();// 创建了一张白纸
        windowManager.getDefaultDisplay().getMetrics(outMetrics);// 给白纸设置宽高
        return outMetrics.widthPixels;
    }

    /**
     * 获取屏幕的高度px
     *
     * @param context 上下文
     * @return 屏幕高px
     */
    public static int getScreenHeight(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();// 创建了一张白纸
        windowManager.getDefaultDisplay().getMetrics(outMetrics);// 给白纸设置宽高
        return outMetrics.heightPixels;
    }

    /**
     * dip转为PX
     */
    public static int dip2px(Context context, float dipValue) {
        float fontScale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * fontScale + 0.5f);
    }
}
