package com.shallow.remotestethoscope.recyclerview;

public class DeviceModel {
    private String deviceName;
    private String deviceMAC;
    private String deviceRssi;

    public DeviceModel() { }

    public DeviceModel(String deviceName, String deviceMAC, String deviceRssi) {
        this.deviceName = deviceName;
        this.deviceMAC = deviceMAC;
        this.deviceRssi = deviceRssi;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDeviceMAC() {
        return deviceMAC;
    }

    public void setDeviceMAC(String deviceMAC) {
        this.deviceMAC = deviceMAC;
    }

    public String getDeviceRssi() {
        return deviceRssi;
    }

    public void setDeviceRssi(String deviceRssi) {
        this.deviceRssi = deviceRssi;
    }

    @Override
    public String toString() {
        return "DeviceModel{" +
                "deviceName='" + deviceName + '\'' +
                ", deviceMAC='" + deviceMAC + '\'' +
                ", deviceRssi='" + deviceRssi + '\'' +
                '}';
    }
}
