package com.shallow.remotestethoscope.listeners;

import android.bluetooth.BluetoothDevice;

import com.shallow.remotestethoscope.recyclerview.DeviceModel;

import java.util.List;

public interface OnSearchDevicesListener extends IErrorListener {
    /**
     * Call before discovery devices.
     */
    void onStartDiscovery();

    /**
     * Call when found a new device.
     *
     * @param deviceModel the new device
     */
    void onNewDeviceFound(DeviceModel deviceModel);

    /**
     * Call when the discovery process completed.
     *
     * @param bondedList the remote devices those are bonded(paired).
     * @param newList    the remote devices those are not bonded(paired).
     */
    void onSearchCompleted(List<DeviceModel> bondedList, List<DeviceModel> newList);
}
