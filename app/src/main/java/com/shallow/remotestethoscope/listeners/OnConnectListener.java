package com.shallow.remotestethoscope.listeners;

public interface OnConnectListener extends IErrorListener {
    void onConnectStart();

    void onConnecting();

    void onConnectFailed();

    void onConnectSuccess(String mac);
}
