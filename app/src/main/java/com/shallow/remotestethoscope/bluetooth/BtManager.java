package com.shallow.remotestethoscope.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.TextUtils;
import android.util.Log;

import com.shallow.remotestethoscope.base.ConstantUtil;
import com.shallow.remotestethoscope.base.DataConversion;
import com.shallow.remotestethoscope.listeners.OnConnectListener;
import com.shallow.remotestethoscope.listeners.OnReceiveMessageListener;
import com.shallow.remotestethoscope.listeners.OnSearchDevicesListener;
import com.shallow.remotestethoscope.listeners.OnSendMessageListener;
import com.shallow.remotestethoscope.recyclerview.DeviceModel;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import static com.shallow.remotestethoscope.base.DataConversion.byteArrayToString;
import static com.shallow.remotestethoscope.base.DataConversion.getUnsignedByte;

public class BtManager {

    private final static String TAG = "BtManager";
    private static final String DEVICE_HAS_NOT_BLUETOOTH_MODULE = "device has not bluetooth module!";

    private static volatile BtManager mBtManager;
    private ArrayList<DeviceModel> mBondedList = new ArrayList<>();
    private ArrayList<DeviceModel> mNewList = new ArrayList<>();
    private ArrayList<Integer> mDatas = new ArrayList<>();
    private ExecutorService mExecutorService = Executors.newCachedThreadPool();
    private Queue<String> mMessageBeanQueue = new LinkedBlockingQueue<>();
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothSocket mSocket;
    private ReadRunnable readRunnable;
    private Context mContext;
    private boolean mNeedToUnregister;
    private int mMaxSize;
    private volatile Receiver mReceiver = new Receiver();
    private HashMap<String, Object> paar = new HashMap<>();

    private OnSearchDevicesListener mOnSearchDevicesListener;
    private OnSendMessageListener mOnSendMessageListener;
    private OnConnectListener mOnConnectListener;
    private OnReceiveMessageListener mOnReceiveMessageListener;

    private volatile boolean mReadable = false;
    private volatile boolean mWritable = false;
    private volatile boolean mPause = false;
    private volatile boolean isAnalyzing = false;
    private InputStream mInputStream;
    private OutputStream mOutputStream;

    private ArrayList<Double> input = new ArrayList<>();
    private ArrayList<Double> output = new ArrayList<>();
    private static double[] audla = {1.0000, 3.1806, 3.8612, 2.1122, 0.4383};
    private static double[] audlb = {0.6620, 2.6481, 3.9721, 2.6481, 0.6620};

    private volatile STATUS mCurrStatus = STATUS.FREE;

    private enum STATUS {
        DISCOVERING,
        CONNECTED,
        FREE
    }

    private final BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            if(!paar.containsKey(device.getAddress())){
                Log.i("ble", "device " + device.getAddress() + "   " + device.getName());
                paar.put(device.getAddress(), "mac:" + device.getAddress());
                DeviceModel dm = new DeviceModel();
                if (device.getName() == null) {
                    dm.setDeviceName("Unknown Device");
                } else {
                    dm.setDeviceName(device.getName());
                }
                dm.setDeviceMAC(device.getAddress());
                dm.setDeviceRssi("Rssi : " + rssi);
                mNewList.add(dm);
                mOnSearchDevicesListener.onNewDeviceFound(dm);
            }
//            else{
//                mBluetoothAdapter.stopLeScan(mLeScanCallback);
//                if (mOnSearchDevicesListener != null) {
//                    mOnSearchDevicesListener.onSearchCompleted(mBondedList, mNewList);
//                }
//            }
        }
    };

    /**
     * Obtains the BtHelperClient getInstance the given context.
     *
     * @param context context
     * @return an instance of BtHelperClient
     */
    public static BtManager getInstance(Context context) {
        if (mBtManager == null) {
            synchronized (BtManager.class) {
                if (mBtManager == null)
                    mBtManager = new BtManager(context);
            }
        }
        return mBtManager;
    }

    /**
     * private constructor for singleton
     *
     * @param context context
     */
    private BtManager(Context context) {
        mContext = context.getApplicationContext();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    /**
     * discovery ble bluetooth devices
     */
    private void searchBLEDevices() {
        if (mOnSearchDevicesListener == null) {
            throw new NullPointerException();
        }
        if (mBondedList == null) mBondedList = new ArrayList<>();
        if (mNewList == null) mNewList = new ArrayList<>();
        if (mBluetoothAdapter == null) {
            mOnSearchDevicesListener.onError(new NullPointerException(DEVICE_HAS_NOT_BLUETOOTH_MODULE));
            return;
        }
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        mBluetoothAdapter.startLeScan(mLeScanCallback);
    }

    /**
     * discovery normal bluetooth devices
     */
    public void searchDevices(){
        if (mCurrStatus == STATUS.FREE) {
            mCurrStatus = STATUS.DISCOVERING;
        }
        checkNotNull(mOnSearchDevicesListener);
        if (mBondedList == null) mBondedList = new ArrayList<>();
        if (mNewList == null) mNewList = new ArrayList<>();
        if (mBluetoothAdapter == null) {
            mBluetoothAdapter =  BluetoothAdapter.getDefaultAdapter();
        }
        if (mReceiver == null) mReceiver = new Receiver();

        //ACTION_FOUND
        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        mContext.registerReceiver(mReceiver, intentFilter);

        intentFilter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        mContext.registerReceiver(mReceiver, intentFilter);

        mNeedToUnregister = true;
        mBondedList.clear();
        mNewList.clear();
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        mBluetoothAdapter.startDiscovery();
        mOnSearchDevicesListener.onStartDiscovery();

    }

    /**
     * 搜索蓝牙广播
     */
    private class Receiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                String action = intent.getAction();
                if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                    if (mOnSearchDevicesListener != null) {
                        mOnSearchDevicesListener.onStartDiscovery();
                    }
                } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (paar != null && !paar.containsKey(device.getAddress())) {
                        paar.put(device.getAddress(), "mac:" + device.getAddress());
                        if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                            if (mNewList != null) {
                                DeviceModel dm = new DeviceModel();
                                if (device.getName() == null) {
                                    dm.setDeviceName("Unknown Device");
                                } else {
                                    dm.setDeviceName(device.getName());
                                }
                                dm.setDeviceMAC(device.getAddress());
                                int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                                dm.setDeviceRssi("Rssi : " + rssi);
                                mNewList.add(dm);
                                mOnSearchDevicesListener.onNewDeviceFound(dm);
                            }
                        } else if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                            if (mBondedList != null) {
                                DeviceModel dm = new DeviceModel();
                                dm.setDeviceName(device.getName());
                                dm.setDeviceMAC(device.getAddress());
                                int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                                dm.setDeviceRssi("Rssi : " + rssi);
                                mBondedList.add(dm);
                                mOnSearchDevicesListener.onNewDeviceFound(dm);
                            }
                        }
                    }
                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    if (mOnSearchDevicesListener != null) {
                        mOnSearchDevicesListener.onSearchCompleted(mBondedList, mNewList);
                    }
//                    searchBLEDevices();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Connect the bluetooth device
     *
     * @param mac 蓝牙设备MAC地址
     */
    public void connectDevice(String mac) {
        if (mCurrStatus != STATUS.CONNECTED) {
            if (mac == null && TextUtils.isEmpty(mac)) {
                throw new IllegalArgumentException("mac address is null or empty.");
            }
            if (!BluetoothAdapter.checkBluetoothAddress(mac)) {
                throw new IllegalArgumentException("mac address is not correct! make sure it's upper case!");
            }
            if (!mReadable) {
                mReadable = true;
            }
            if (!mWritable) {
                mWritable = true;
            }
            if (mOnConnectListener != null) {
                mOnConnectListener.onConnectStart();
                ConnectDeviceRunnable connectDeviceRunnable = new ConnectDeviceRunnable(mac);
                checkNotNull(mExecutorService);
                mExecutorService.submit(connectDeviceRunnable);
            }
        }
    }

    /**
     * Disconnect the bluetooth device
     */
    public void disconnectDevice() {
        if (mCurrStatus == STATUS.CONNECTED) {
            mReadable = false;
            mWritable = false;
            if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()
                    && mSocket != null && mSocket.isConnected()) {
                try{
                    mSocket.close();
                    mSocket = null;
                    if (readRunnable != null) {
                        readRunnable = null;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.i(TAG, "Disconnect bluetooth failed please check bluetooth is enable and the mSocket is connected !");
                }

            }
        } else {
            Log.i(TAG, "Bluetooth is not connected! Please connect device!");
        }
    }

    /**
     * 连接bluetooth线程
     */
    private class ConnectDeviceRunnable implements Runnable {

        private String mac;

        public ConnectDeviceRunnable(String mac) {
            this.mac = mac;
        }

        @Override
        public void run() {
            try {
                if (mOnConnectListener == null) {
                    Log.i(TAG, "OnConnectListener is null.");
                    return;
                }
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mac);
                mBluetoothAdapter.cancelDiscovery();
                mCurrStatus = STATUS.FREE;
                Log.d(TAG, "prepare to connect : " +
                        (device.getName() != null ? device.getName() : "Unknown Device") +
                        " " + device.getAddress());
                mSocket = device.createInsecureRfcommSocketToServiceRecord(UUID.fromString(ConstantUtil.SERVICE_UUID));
                mOnConnectListener.onConnecting();
                mSocket.connect();
                mInputStream = mSocket.getInputStream();
                mOutputStream = mSocket.getOutputStream();
                mCurrStatus = STATUS.CONNECTED;
                mOnConnectListener.onConnectSuccess(mac);
            } catch (IOException e) {
                e.printStackTrace();
                mOnConnectListener.onConnectFailed();
                try{
                    mInputStream.close();
                    mOutputStream.close();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
                mCurrStatus = STATUS.FREE;
            }
        }
    }


    /**
     * Closes the connection and releases any system resources associated
     * with the stream.
     */
    public void close() {
        try {
            if (mBluetoothAdapter != null) {
                mBluetoothAdapter.cancelDiscovery();
                mBluetoothAdapter = null;
            }
            if (mNeedToUnregister) {
                mContext.unregisterReceiver(mReceiver);
                mReceiver = null;
                mNeedToUnregister = !mNeedToUnregister;
            }
            if (mMessageBeanQueue != null) {
                mMessageBeanQueue.clear();
                mMessageBeanQueue = null;
            }
            mWritable = false;
            mReadable = false;

            if (mSocket != null) {
                mSocket.close();
                mSocket = null;
            }
            if (mInputStream != null) {
                mInputStream.close();
                mInputStream = null;
            }
            if (mOutputStream != null) {
                mOutputStream.close();
                mOutputStream = null;
            }
            if (mExecutorService != null) {
                mExecutorService.shutdown();
                mExecutorService = null;
            }
            mReceiver = null;
            mBtManager = null;
            mCurrStatus = STATUS.FREE;
        } catch (Exception e) {
            e.printStackTrace();
            mSocket = null;
        }
    }

    /**
     *
     * Send a message to a remote device.
     * If the local device did't connected to the remote devices, it will call connectDevice(), then send the message.
     * You can obtain a response getInstance the remote device, just as http.
     * However, it will blocked if didn't get response getInstance the remote device.
     *
     * @param message the message need to send
     * @param needResponse if need to obtain a response getInstance the remote device
     */
    public void sendMessage(String message, boolean needResponse) {
        if (mCurrStatus == STATUS.CONNECTED) {
            if (mBluetoothAdapter == null) {
                mOnSendMessageListener.onError(new NullPointerException(DEVICE_HAS_NOT_BLUETOOTH_MODULE));
                return;
            }
            byte[] commandHex = DataConversion.stringToByteArray(message);
            try{
                mOutputStream.write(commandHex);
                mOutputStream.flush();
                Log.i(TAG, "Sending message :" + message);
                mOnSendMessageListener.onSuccess("Success to send message");
            } catch (IOException e) {
                e.printStackTrace();
                mOnSendMessageListener.onConnectionLost(e);
                mCurrStatus = STATUS.FREE;
            }
            if (needResponse) {
                if (readRunnable == null) {
                    readRunnable = new ReadRunnable();
                    mExecutorService.submit(readRunnable);
                } else {
                    Log.i(TAG, "readRunnable is not null !");
                }
            }
        } else {
            Log.i(TAG, "Bluetooth is not connected!");
        }
    }


    /**
     * 蓝牙设备输入线程
     */
    private class WriteRunnable implements Runnable {

        @Override
        public void run() {
            if (mOnConnectListener == null) {
                Log.i(TAG, "OnConnectListener is null");
                return;
            }
            mWritable = true;
            while (mCurrStatus != STATUS.CONNECTED);
            while (mWritable && !mMessageBeanQueue.isEmpty()) {
                String commandStr = mMessageBeanQueue.poll();
                if (commandStr != null) {
                    byte[] commandHex = DataConversion.stringToByteArray(commandStr);
                    try{
                        mOutputStream.write(commandHex);
                        mOutputStream.flush();
                        Log.i(TAG, "Sending message :" + commandStr);
                        mOnSendMessageListener.onSuccess("Success to send message");
                    } catch (IOException e) {
                        e.printStackTrace();
                        mOnSendMessageListener.onConnectionLost(e);
                        mCurrStatus = STATUS.FREE;
                        break;
                    }
                }
            }
        }
    }

    /**
     * 蓝牙数据读取线程
     */
    private class ReadRunnable implements Runnable {

        @Override
        public void run() {
            Log.i(TAG, "开始接收数据");
            mReadable = true;
            while (mCurrStatus != STATUS.CONNECTED);
            while (mReadable) {
                if(!mPause) {
                    try {
                        byte[] signals = new byte[1024];
                        mInputStream.read(signals);
                        Log.i(TAG, DataConversion.byteArrayToString(signals));
                        for (int i = 0; i < signals.length;) {
                            if (getUnsignedByte(signals[i]) == 0x68 &&
                                    getUnsignedByte(signals[i + 1]) == 0x67 &&
                                    getUnsignedByte(signals[i + 2]) == 0xe0 &&
                                    getUnsignedByte(signals[i + 3]) == 0x10  && !isAnalyzing) {
                                isAnalyzing = true;
                                i += 4;
                            } else if (getUnsignedByte(signals[i]) == 0x0f &&
                                    getUnsignedByte(signals[i + 1]) == 0xff && isAnalyzing ) {
                                isAnalyzing = false;
                                i += 2;
                            } else {
                                if (isAnalyzing) {
                                    int dataReceived = DataConversion.byteToInt(signals[i], signals[i + 1]);
                                    if (dataReceived > 32767) {
                                        dataReceived = dataReceived - 65535;
                                    }
                                    i += 2;
                                    Log.i(TAG, String.valueOf(dataReceived));
                                    mOnReceiveMessageListener.onNewData(dataReceived);
                                    synchronized (mDatas) {
                                        if (mDatas.size() > mMaxSize) {
                                            mDatas.remove(0);
                                        }
                                        mDatas.add((int)filter(dataReceived * 6.15));
                                    }
                                } else {
                                    i += 2;
                                }
                            }
                            mOnReceiveMessageListener.onNewLine(byteArrayToString(signals));
                        }
                    }catch (IOException e) {
                        e.printStackTrace();
                        mOnReceiveMessageListener.onConnectionLost(e);
                        mCurrStatus = STATUS.FREE;
                    }
                }

            }
        }
    }

    private double filter(double val) {
        double result = 0;
        if (input.size() < 4) {
            input.add(val);
            output.add(val);
            result = val;
        } else {
            result = audlb[0] * val;
            for (int i = 1; i <= 4; i++) {
                result = -audla[i] * output.get(4 - i) + audlb[i] * input.get(4 - i);
            }
            input.remove(0);
            input.add(val);
            output.remove(0);
            output.add(result);
        }
        return result;
    }

    /**
     * Request for enable the device's bluetooth asynchronously.
     * Throw a NullPointerException if the device doesn't have a bluetooth module.
     */
    public void requestEnableBt() {
        if (mBluetoothAdapter == null) {
            throw new NullPointerException(DEVICE_HAS_NOT_BLUETOOTH_MODULE);
        }
        if (!mBluetoothAdapter.isEnabled())
            mBluetoothAdapter.enable();
    }

    public void clearDevicesInfo() {
        paar.clear();
        mBondedList.clear();
        mNewList.clear();
    }

    public void setDatas(ArrayList<Integer> datas) {
        this.mDatas = datas;
    }

    public void setMaxSize(int maxSize) {
        this.mMaxSize = maxSize;
    }

    public void setPause(boolean pause) {
        this.mPause = pause;
    }

    public void setAnalyzing(boolean analyzing) {
        isAnalyzing = analyzing;
    }

    public void setOnSearchDeviceListener(OnSearchDevicesListener onSearchDevicesListener) {
        this.mOnSearchDevicesListener = onSearchDevicesListener;
    }

    public void setOnSendMessageListener(OnSendMessageListener onSendMessageListener) {
        this.mOnSendMessageListener = onSendMessageListener;
    }

    public void setOnConnectListener(OnConnectListener onConnectListener) {
        this.mOnConnectListener = onConnectListener;
    }

    public void setOnReceiveMessageListener(OnReceiveMessageListener onReceiveMessageListener) {
        this.mOnReceiveMessageListener = onReceiveMessageListener;
    }

    /**
     * 校验
     *
     * @param o 待校验对象
     */
    private void checkNotNull(Object o) {
        if (o == null)
            throw new NullPointerException();
    }

}
