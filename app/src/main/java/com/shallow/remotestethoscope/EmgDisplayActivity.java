package com.shallow.remotestethoscope;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Context;
import android.content.Intent;
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
import android.widget.TextView;
import android.widget.Toast;

import com.shallow.remotestethoscope.base.ConstantUtil;
import com.shallow.remotestethoscope.bluetooth.BtManager;
import com.shallow.remotestethoscope.listeners.OnConnectListener;
import com.shallow.remotestethoscope.listeners.OnReceiveMessageListener;
import com.shallow.remotestethoscope.listeners.OnSendMessageListener;
import com.shallow.remotestethoscope.waveview.AudioWaveView;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.TimeZone;


public class EmgDisplayActivity extends AppCompatActivity{

    private final static String TAG = "EmgDisplayActivity";

    private final static String ch1StartCommand = "686702010D0A";

    private final static String ch2StartCommand = "686702100D0A";
    private final static String stopCommand = "686702110D0A";

    public final static String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public final static String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private BtManager btManager;
    private AudioWaveView audioWave;
    private Chronometer chronometer;
    private ImageButton on_pause_btn;
    private TextView aemg;
    private TextView iemg;
    private TextView rms;

    private String mDeviceAddress;

    private boolean op_click_flag =true;
    private long mRecordTime = 0;

    private ArrayList<Integer> emgData = new ArrayList<>();
    private int[] protoData = new int[5];
    private int idx = 0;

    private OnConnectListener mOnConnectListener;
    private OnSendMessageListener mOnSendMessageListener;
    private OnReceiveMessageListener mOnReceiveMessageListener;


    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            String message = msg.obj.toString();
            switch (msg.what) {
                case 0 :
                    Toast.makeText(EmgDisplayActivity.this, message, Toast.LENGTH_SHORT).show();
                    break;

                case 1:
                    Toast.makeText(EmgDisplayActivity.this, message, Toast.LENGTH_SHORT).show();
                    break;
                case 2:
                    Log.d(TAG, message);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emg_display);

        final Intent intent = getIntent();
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        Toolbar toolbar = findViewById(R.id.toolbar_emg);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        audioWave = findViewById(R.id.audioWaveEmg);
        chronometer = findViewById(R.id.timer_emg);
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

        on_pause_btn = findViewById(R.id.on_pause_emg);
        on_pause_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (op_click_flag) {
                    if (mRecordTime == 0) {
                        resolveStart();
                        chronometer.setBase(SystemClock.elapsedRealtime());
                    } else {
                        btManager.setPause(false);
                        audioWave.setPause(false);
                        chronometer.setBase(chronometer.getBase() + (SystemClock.elapsedRealtime() - mRecordTime));
                    }
                    on_pause_btn.setImageResource(R.mipmap.ic_action_pause);
                    chronometer.start();
                } else  {
                    btManager.setPause(true);
                    btManager.setAnalyzing(false);
                    audioWave.setPause(true);
                    chronometer.stop();
                    mRecordTime = SystemClock.elapsedRealtime();
                    on_pause_btn.setImageResource(R.mipmap.ic_action_play_arrow);
                    calCharacteristic();
                }
                op_click_flag = !op_click_flag;
            }
        });
        aemg = findViewById(R.id.aemg);
        iemg = findViewById(R.id.iemg);
        rms = findViewById(R.id.rms);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (btManager != null) {
            btManager.setPause(false);
            btManager.sendMessage(stopCommand, false);
            btManager.disconnectDevice();
            btManager.close();
            btManager = null;
        }
        audioWave.stopView();
    }
    
    private void resolveStart() {
        int offset = dip2px(this, 1);
        int size = getScreenWidth(this) / offset;
        initBtManager(audioWave.getRecList(), size);
        try {
            btManager.connectDevice(mDeviceAddress);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    btManager.sendMessage(ch1StartCommand, true);
                    audioWave.startView(ConstantUtil.DRAW_EMG);
                }
            }, 5000);
        } catch (Exception e) {
            resolveUI();
            Toast.makeText(this, "蓝牙连接或数据采集异常", Toast.LENGTH_SHORT).show();
        }
    }

    private void resolveUI() {
        on_pause_btn.setImageResource(R.mipmap.ic_action_play_arrow);
        op_click_flag = false;

        chronometer.stop();
        mRecordTime = 0;
        chronometer.setText(R.string.clock);
    }

    private void initBtManager(ArrayList<Integer> dataList,  int size) {

        mOnConnectListener = new OnConnectListener() {
            @Override
            public void onConnectStart() {
                sendMessage(0, "开始连接！");
            }

            @Override
            public void onConnecting() {
                sendMessage(0, "正在连接...");
            }

            @Override
            public void onConnectFailed() {
                sendMessage(1, "连接失败!");
                resolveUI();
                audioWave.stopView();
            }

            @Override
            public void onConnectSuccess(String mac) {
                sendMessage(0, mac + " 连接成功！");
            }

            @Override
            public void onError(Exception e) {
                sendMessage(1, "连接异常！");
                resolveUI();
                audioWave.stopView();
            }
        };

        mOnSendMessageListener = new OnSendMessageListener() {
            @Override
            public void onSuccess(String response) {
                sendMessage(0, "成功发送指令！");
            }

            @Override
            public void onConnectionLost(Exception e) {
                sendMessage(1, "连接丢失！");
                resolveUI();
                audioWave.stopView();
            }

            @Override
            public void onError(Exception e) {
                sendMessage(1, "指令发送异常");
                resolveUI();
                audioWave.stopView();
            }
        };

        mOnReceiveMessageListener = new OnReceiveMessageListener() {
            @Override
            public void onNewLine(String s) {
                sendMessage(2, s);
            }

            @Override
            public void onNewData(int data) {
                emgData.add(data);
            }


            @Override
            public void onConnectionLost(Exception e) {
                sendMessage(1, "连接丢失！");
                resolveUI();
                audioWave.stopView();
            }

            @Override
            public void onError(Exception e) {
                sendMessage(1, "发送消息异常！");
                resolveUI();
                audioWave.stopView();
            }
        };
        btManager = BtManager.getInstance(this);
        btManager.setOnConnectListener(mOnConnectListener);
        btManager.setOnSendMessageListener(mOnSendMessageListener);
        btManager.setOnReceiveMessageListener(mOnReceiveMessageListener);
        btManager.requestEnableBt();
        btManager.setDatas(dataList);
        btManager.setMaxSize(size);
    }

    /**
     * @param type    0 状态改变  1 错误信息  2 接收数据
     * @param context Activity上下文
     */
    public void sendMessage(int type, String context) {
        if (mHandler != null) {
            Message message = mHandler.obtainMessage();
            message.what = type;
            message.obj = context;
            mHandler.sendMessage(message);
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

    private void calCharacteristic() {
        DecimalFormat df = new DecimalFormat("#.00");
        double absSum = 0;
        double qualitySum = 0;
        double squareSum = 0;
        Iterator it = emgData.iterator();
        while (it.hasNext()) {
            int emg = (int)it.next();
            absSum += Math.abs(emg) * 6.15;
            qualitySum += Math.abs(emg) * 6.15 * 0.001;
            squareSum += Math.pow(Math.abs(emg), 2);
        }
        String aemgValue = "AEMG: " + df.format(Math.sqrt(absSum / emgData.size()));
        String iemgValue = "iEMG: " + df.format(qualitySum);
        String rmsValue = "rms: " + df.format(Math.sqrt(squareSum / emgData.size()));
        aemg.setText(aemgValue);
        iemg.setText(iemgValue);
        rms.setText(rmsValue);
    }

}
