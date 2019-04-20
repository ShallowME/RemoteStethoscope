package com.shallow.remotestethoscope.base;


public class MP3FileModel {

    public String mp3FileName;

    public String mp3FileTime;

    public String mp3FileDuration;

    public String userName;

    public MP3FileModel(String mp3FileName, String mp3FileTime, String mp3FileDuration, String userName) {
        this.mp3FileName = mp3FileName;
        this.mp3FileTime = mp3FileTime;
        this.mp3FileDuration = mp3FileDuration;
        this.userName = userName;
    }

    public String getMp3FileName() {
        return mp3FileName;
    }

    public void setMp3FileName(String mp3FileName) {
        this.mp3FileName = mp3FileName;
    }

    public String getMp3FileTime() {
        return mp3FileTime;
    }

    public void setMp3FileTime(String mp3FileTime) {
        this.mp3FileTime = mp3FileTime;
    }

    public String getMp3FileDuration() {
        return mp3FileDuration;
    }

    public void setMp3FileDuration(String mp3FileDuration) {
        this.mp3FileDuration = mp3FileDuration;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
