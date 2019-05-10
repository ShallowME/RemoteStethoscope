package com.shallow.remotestethoscope.listeners;

public interface OnReceiveMessageListener extends IErrorListener, IConnectionLostListener {

    /**
     * call when have some response
     * @param s
     */
    void onNewLine(String s);

}
