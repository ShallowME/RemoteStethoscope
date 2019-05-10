package com.shallow.remotestethoscope.listeners;

public interface OnSendMessageListener extends IConnectionLostListener,IErrorListener {
    /**
     * Call when send a message succeed, and get a response from the remote device.
     *
     * @param response the response from the remote device
     */
    void onSuccess(String response);
}
