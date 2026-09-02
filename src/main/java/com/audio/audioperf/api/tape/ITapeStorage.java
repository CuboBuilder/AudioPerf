package com.audio.audioperf.api.tape;

public interface ITapeStorage {
    String getUniqueId();
    String getName();
    int getPosition();
    int getSize();
    int setPosition(int newPosition);
    int seek(int amount);
    int read(boolean simulate);
    int read(byte[] intoArray, boolean simulate);
    void write(byte b);
    int write(byte[] array);
    void onStorageUnload();
}