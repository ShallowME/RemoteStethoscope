package com.shallow.remotestethoscope;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AlertDialogLayout;
import androidx.appcompat.widget.Toolbar;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;


import com.shallow.remotestethoscope.base.DBHelper;
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

    private ImageButton on_pause_btn;
    private ImageButton list_save_btn;
    private ImageButton cancel_record_btn;

    private long mRecordTime = 0;
    private String filePath;

    MP3Recorder mRecorder;
    AudioWaveView audioWave;
    DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar_main);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        dbHelper = new DBHelper(this, "UserData.db", null, 1);
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

        on_pause_btn = findViewById(R.id.on_pause_button);
        list_save_btn = findViewById(R.id.list_save_button);
        cancel_record_btn = findViewById(R.id.cancel_record_button);
        cancel_record_btn.setEnabled(false);
        cancel_record_btn.getBackground().setAlpha(100);
        on_pause_btn.setOnClickListener(this);
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
                        cancel_record_btn.setEnabled(true);
                        cancel_record_btn.getBackground().setAlpha(255);
                    } else {
                        chronometer.setBase(chronometer.getBase() + (SystemClock.elapsedRealtime() - mRecordTime));
                        resolvePause(false);
                    }
                    chronometer.start();
                    on_pause_btn.setImageResource(R.mipmap.ic_action_pause);
                    list_save_btn.setEnabled(false);
                    list_save_btn.getBackground().setAlpha(100);

                }
                else {
                    chronometer.stop();
                    resolvePause(true);
                    mRecordTime = SystemClock.elapsedRealtime();
                    on_pause_btn.setImageResource(R.mipmap.ic_action_play_arrow);
                    list_save_btn.setEnabled(true);
                    list_save_btn.getBackground().setAlpha(255);
                }
                op_click_flag = !op_click_flag;
                break;

            case R.id.list_save_button:
                if (ls_click_flag) {
                    Intent intent = new Intent(MainActivity.this, FileActivity.class);
                    startActivity(intent);
                } else {
                    final EditText et = new EditText(this);

                    new AlertDialog.Builder(this).setTitle("输入文件名")
                            .setIcon(R.mipmap.ic_action_edit_file)
                            .setView(et)
                            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    String basePath = FileUtils.getAppPath();
                                    String input = et.getText().toString();
                                    if (input.equals("")) {
                                        Toast.makeText(MainActivity.this, "文件名不能为空", Toast.LENGTH_SHORT).show();
                                    } else {
                                        File audioFile = new File(filePath);
                                        String newPath = basePath + File.separator + input + ".mp3";
                                        if (new File(newPath).exists()) {
                                            Toast.makeText(MainActivity.this, "文件名已存在", Toast.LENGTH_SHORT).show();
                                        } else {
                                            audioFile.renameTo(new File(newPath));
                                            SharedPreferences userSetting = getSharedPreferences("setting", 0);
                                            String username = userSetting.getString("username", "");
                                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINESE);
                                            SQLiteDatabase db = dbHelper.getWritableDatabase();
                                            db.beginTransaction();
                                            try {
                                                ContentValues values = new ContentValues();
                                                values.put("mp3_file_name", input);
                                                values.put("mp3_file_time", sdf.format(new Date()));
                                                values.put("mp3_file_duration", chronometer.getText().toString());
                                                values.put("username", username);
                                                db.insert("AudioFile", null, values);
                                                values.clear();
                                                db.setTransactionSuccessful();
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            } finally {
                                                db.endTransaction();
                                            }
                                        }
                                    }
                                }
                            })
                            .setNegativeButton("取消", null)
                            .show();
                }
                break;

            case R.id.cancel_record_button:
                resolveReset();
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

        filePath = FileUtils.getAppPath() + UUID.randomUUID().toString() + ".mp3";
        File mp3File = new File(filePath);
        mRecorder = new MP3Recorder(mp3File);
        audioWave = findViewById(R.id.audioWave);
        int offset = dip2px(this, 1);
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
            resolveReset();
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

    private void resolveReset() {
        FileUtils.deleteFile(filePath);
        filePath = "";
        if (mRecorder != null && mRecorder.isRecording()) {
            mRecorder.stop();
            audioWave.stopView();
        }

        resolveNormalUI();
    }

    private void resolveNormalUI() {
        on_pause_btn.setImageResource(R.mipmap.ic_action_play_arrow);
        op_click_flag = true;
        list_save_btn.setImageResource(R.mipmap.ic_action_bullet_list);
        ls_click_flag = true;
        cancel_record_btn.setEnabled(false);
        cancel_record_btn.getBackground().setAlpha(100);
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
