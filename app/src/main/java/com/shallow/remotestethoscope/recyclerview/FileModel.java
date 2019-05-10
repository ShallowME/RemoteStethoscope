package com.shallow.remotestethoscope.recyclerview;

public class FileModel {
    String mp3Name;
    String mp3Detail;
    int mp3Img;
    private boolean mp3Status;

    public FileModel() { }

    public FileModel(String mp3Name, String mp3Detail, int mp3Img) {
        this.mp3Name = mp3Name;
        this.mp3Detail = mp3Detail;
        this.mp3Img = mp3Img;
    }

    public String getMp3Name() {
        return mp3Name;
    }

    public void setMp3Name(String mp3Name) {
        this.mp3Name = mp3Name;
    }

    public String getMp3Detail() {
        return mp3Detail;
    }

    public void setMp3Detail(String mp3Detail) {
        this.mp3Detail = mp3Detail;
    }

    public int getMp3Img() {
        return mp3Img;
    }

    public void setMp3Img(int mp3Img) {
        this.mp3Img = mp3Img;
    }

    public boolean isMp3Status() {
        return mp3Status;
    }

    public void setMp3Status(boolean mp3Status) {
        this.mp3Status = mp3Status;
    }
}
