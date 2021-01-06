package com.albertogeniola.merosslib.model;
import com.google.gson.annotations.SerializedName;


public enum OnlineStatus {
    OFFLINE(0),
    ONLINE(1),
    LAN(2),
    UPGRADING(3);

    private int value;
    private OnlineStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
