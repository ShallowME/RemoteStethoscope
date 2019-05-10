package com.shallow.remotestethoscope;


import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import com.shallow.remotestethoscope.base.SwitchButton;
import com.shallow.remotestethoscope.bluetooth.BtManager;
import com.shallow.remotestethoscope.listeners.OnSearchDevicesListener;
import com.shallow.remotestethoscope.recyclerview.DeviceAdapter;
import com.shallow.remotestethoscope.recyclerview.DeviceModel;
import java.util.ArrayList;
import java.util.List;

public class BlueToothActivity extends AppCompatActivity {

    private final static String TAG = "BlueToothActivity";

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 2;


    private BtManager mBtManager;
    private RecyclerView mRecyclerView;
    private DeviceAdapter mDeviceAdapter;
    private DividerItemDecoration mDecoration;
    private RecyclerView.LayoutManager mLayoutManager;
    private ArrayList<DeviceModel> mDevices = new ArrayList<>();
    private SwitchButton switchButton;
    private BluetoothAdapter mBluetoothAdapter;
    private OnSearchDevicesListener mOnSearchDevicesListener;



    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            String message = msg.obj.toString();
            switch (msg.what) {
                case 0 :
                    Toast.makeText(BlueToothActivity.this, message, Toast.LENGTH_SHORT).show();
                    break;

                case 1:
                    Toast.makeText(BlueToothActivity.this, message, Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        if (switchButton != null) {
            switchButton.setToggleOff();
        }
        mBtManager.clearDevicesInfo();
        mDevices.clear();
        mDeviceAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blue_tooth);

        Toolbar toolbar = findViewById(R.id.toolbar_bluetooth);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE is not supported.", Toast.LENGTH_SHORT).show();
            finish();
        }

        final BluetoothManager bluetoothManager =(BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 询问打开蓝牙
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
            }
            if (this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_FINE_LOCATION);
            }
        }

        mRecyclerView = findViewById(R.id.deviceList);
        mDecoration = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.addItemDecoration(mDecoration);
        mDeviceAdapter = new DeviceAdapter(this, mDevices);
        mRecyclerView.setAdapter(mDeviceAdapter);
        initManager();
        switchButton = findViewById(R.id.switchButton);
        switchButton.setOnToggleChanged(new SwitchButton.OnToggleChanged() {
            @Override
            public void onToggle(boolean on) {
                if (on) {
                    mDevices.clear();
                    mBtManager.searchDevices();
                } else {
                    mDevices.clear();
                    mDeviceAdapter.setDatas(mDevices);
                    mDeviceAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    /**
     * 初始化蓝牙管理，设置监听
     */
    public void initManager() {
        mOnSearchDevicesListener = new OnSearchDevicesListener() {
            @Override
            public void onStartDiscovery() {
                sendMessage(0, "正在搜索设备..");
                Log.d(TAG, "onStartDiscovery()");
            }

            @Override
            public void onNewDeviceFound(DeviceModel deviceModel) {
                Log.d(TAG, "new device: " + deviceModel.getDeviceName() + " " + deviceModel.getDeviceMAC());
                mDevices.add(deviceModel);
                mDeviceAdapter.notifyDataSetChanged();
            }

            @Override
            public void onSearchCompleted(List<DeviceModel> bondedList, List<DeviceModel> newList) {
                Log.d(TAG, "SearchCompleted: bondedList" + bondedList.toString());
                Log.d(TAG, "SearchCompleted: newList" + newList.toString());
                sendMessage(0, "搜索完成,点击列表进行连接！");
            }

            @Override
            public void onError(Exception e) {
                sendMessage(1, "搜索失败");
            }

        };
        mBtManager = BtManager.getInstance(getApplicationContext());
        mBtManager.setOnSearchDeviceListener(mOnSearchDevicesListener);
        mBtManager.requestEnableBt();
    }

    /**
     * @param type    0 状态改变  1 错误信息
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


    // 申请打开蓝牙请求的回调
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "蓝牙已经开启", Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "没有蓝牙权限", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION :
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(BlueToothActivity.this,
                            "PERMISSION_REQUEST_COARSE_LOCATION IS LICENSED",
                            Toast.LENGTH_SHORT).show();
                }
                break;

            case PERMISSION_REQUEST_FINE_LOCATION:
                if (grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(BlueToothActivity.this,
                            "PERMISSION_REQUEST_FINE_LOCATION IS LICENSED",
                            Toast.LENGTH_SHORT).show();
                }
                break;
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBtManager != null) {
            mBtManager.close();
            mBtManager = null;
        }
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }

    }

}
