package com.shallow.remotestethoscope;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;


import com.shallow.remotestethoscope.audio.MP3AudioStreamDelegate;
import com.shallow.remotestethoscope.audio.MP3AudioStreamPlayer;
import com.shallow.remotestethoscope.base.ConstantUtil;
import com.shallow.remotestethoscope.base.DBHelper;
import com.shallow.remotestethoscope.base.FileUtils;
import com.shallow.remotestethoscope.waveview.AudioWaveView;


import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

public class AudioPlayActivity extends AppCompatActivity implements MP3AudioStreamDelegate, View.OnClickListener {

    private final static String TAG = "WavePlayActivity";

    private AudioWaveView audioWave;

    private SeekBar seekBar;

    private Chronometer chronometer;

    private ImageButton mp3_on_pause_btn;

    private ImageButton mp3_delete_btn;

    private String mp3Name;

    private long mRecordTime = 0;

    MP3AudioStreamPlayer player;

    Timer timer;

    DBHelper dbHelper;

    boolean playEnd;

    boolean seekBarTouch;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_play);

        dbHelper = new DBHelper(this, "UserData.db", null, 1);

        Toolbar toolbar = findViewById(R.id.toolbar_audio);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        TextView fileName = findViewById(R.id.audioFileName);
        mp3Name = getIntent().getStringExtra("mp3Name");
        fileName.setText(mp3Name);

        chronometer = findViewById(R.id.timer_audio);
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

        audioWave = findViewById(R.id.audioWavePlay);
        seekBar = findViewById(R.id.seekBar);

        mp3_on_pause_btn = findViewById(R.id.mp3_play_pause);
        mp3_delete_btn = findViewById(R.id.mp3_delete);
        mp3_on_pause_btn.setOnClickListener(this);
        mp3_delete_btn.setOnClickListener(this);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                play();
            }
        }, 1000);
        chronometer.setBase(SystemClock.elapsedRealtime());
        chronometer.start();

        mp3_delete_btn.setEnabled(false);
        mp3_delete_btn.getBackground().setAlpha(100);

        seekBar.setEnabled(false);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                seekBarTouch = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                seekBarTouch = false;
                if (!playEnd) {
                    player.seekTo(seekBar.getProgress());
                }
            }
        });

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (playEnd || player == null || !seekBar.isEnabled()) {
                    return;
                }
                long position = player.getCurPosition();
                if (position > 0 && !seekBarTouch) {
                    seekBar.setProgress((int) position);
                }
            }
        }, 200, 200);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        audioWave.stopView();
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        stop();
    }

    private void play() {
        if (player != null) {
            player.stop();
            player.release();
            player = null;
        }

        player = new MP3AudioStreamPlayer();
        String rootPath = FileUtils.getAppPath();
        String uri = rootPath + mp3Name + ".mp3";
        player.setUrlString(uri);
        player.setDelegate(this);

        int size = getScreenHeight(this) / dip2px(this, 1);
        player.setDataList(audioWave.getRecList(), size);

        audioWave.setBaseRecorder(player);
        audioWave.startView(ConstantUtil.DRAW_TONE);
        try {
            player.play();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stop() {
        player.stop();
    }

    /****************************************
     * Delegate methods. These are all fired from a background thread so we have to call any GUI code on the main thread.
     ****************************************/


    @Override
    public void onAudioPlayerPlaybackStarted( final MP3AudioStreamPlayer player) {
        Log.i(TAG, "onRadioPlayerPlaybackStarted");
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                playEnd = false;
                mp3_on_pause_btn.setEnabled(true);
                mp3_delete_btn.setEnabled(false);
                mp3_delete_btn.getBackground().setAlpha(100);
                seekBar.setMax((int)player.getDuration());
                seekBar.setEnabled(true);
            }
        });
    }

    @Override
    public void onAudioPlayerStopped(MP3AudioStreamPlayer player) {
        Log.i(TAG, "onRadioPlayerStopped");
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                playEnd = true;
                mp3_on_pause_btn.setEnabled(true);
                mp3_on_pause_btn.setImageResource(R.mipmap.ic_action_play_arrow);
                mp3_delete_btn.setEnabled(true);
                mp3_delete_btn.getBackground().setAlpha(255);
                seekBar.setEnabled(false);
                chronometer.stop();
                mRecordTime = 0;
            }
        });
    }

    @Override
    public void onAudioPlayerError(MP3AudioStreamPlayer player) {
        Log.i(TAG, "onRadioPlayerError");
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                playEnd = false;
                mp3_on_pause_btn.setEnabled(true);
                mp3_delete_btn.setEnabled(true);
                seekBar.setEnabled(false);
            }
        });
    }

    @Override
    public void onAudioPlayerBuffering(MP3AudioStreamPlayer player) {
        Log.i(TAG, "onRadioPlayerBuffering");
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mp3_on_pause_btn.setEnabled(false);
                mp3_delete_btn.setEnabled(false);
                seekBar.setEnabled(false);
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.mp3_play_pause :

                if (playEnd) {
                    stop();
                    mp3_on_pause_btn.setImageResource(R.mipmap.ic_action_pause);
                    mp3_delete_btn.setEnabled(false);
                    mp3_delete_btn.getBackground().setAlpha(100);
                    seekBar.setEnabled(true);
                    chronometer.setBase(SystemClock.elapsedRealtime());
                    chronometer.setText("00:00:00");
                    chronometer.start();
                    play();
                    return;
                }

                if (player.isPause()) {
                    mp3_on_pause_btn.setImageResource(R.mipmap.ic_action_pause);
                    mp3_delete_btn.setEnabled(false);
                    mp3_delete_btn.getBackground().setAlpha(100);
                    chronometer.setBase(chronometer.getBase() + (SystemClock.elapsedRealtime() - mRecordTime));
                    chronometer.start();
                    player.setPause(false);
                    audioWave.setPause(false);

                } else {
                    mp3_on_pause_btn.setImageResource(R.mipmap.ic_action_play_arrow);
                    mp3_delete_btn.setEnabled(true);
                    mp3_delete_btn.getBackground().setAlpha(255);
                    mRecordTime = SystemClock.elapsedRealtime();
                    chronometer.stop();
                    player.setPause(true);
                    audioWave.setPause(true);
                }
                break;
            case R.id.mp3_delete :
                String rootPath = FileUtils.getAppPath();
                String delPath = rootPath + mp3Name + ".mp3";
                FileUtils.deleteFile(delPath);

                SQLiteDatabase db = dbHelper.getWritableDatabase();
                db.beginTransaction();
                try {
                    db.delete("AudioFile", "mp3_file_name = ?", new String[] {mp3Name});
                    db.setTransactionSuccessful();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    db.endTransaction();
                }
                finish();
                break;
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
