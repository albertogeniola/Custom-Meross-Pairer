package com.albertogeniola.merosslib.model.protocol.payloads;


import com.google.gson.annotations.SerializedName;

import lombok.Getter;

@Getter
public class GetSystemAllPayloadAllFirmware {

    @SerializedName("version")
    private String version;

    @SerializedName("compileTime")
    private String compileTime;

    @SerializedName("wifiMac")
    private String wifiMac;

    @SerializedName("innerIp")
    private String innerIp;

    @SerializedName("server")
    private String server;

    @SerializedName("port")
    private int port;

    @SerializedName("userId")
    private int userId;
}
