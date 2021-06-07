package com.albertogeniola.merosslib.model.http;

import com.albertogeniola.merosslib.model.OnlineStatus;
import com.google.gson.annotations.SerializedName;

import java.util.List;

import lombok.Getter;

@Getter
public class DeviceInfo {
    @SerializedName("uuid")
    private String uuid;

    @SerializedName("onlineStatus")
    private OnlineStatus onlineStatus;

    @SerializedName("devName")
    private String devName;

    @SerializedName("bindTime")
    private String bindTime;

    @SerializedName("deviceType")
    private String deviceType;

    @SerializedName("subType")
    private String subType;

    @SerializedName("channels")
    private List[] channels;

    @SerializedName("region")
    private String region;

    @SerializedName("fmwareVersion")
    private String fmwareVersion;

    @SerializedName("hdwareVersion")
    private String hdwareVersion;

    @SerializedName("userDevIcon")
    private String userDevIcon;

    @SerializedName("iconType")
    private int iconType;

    @SerializedName("skillNumber")
    private String skillNumber;

    @SerializedName("domain")
    private String domain;

    @SerializedName("reservedDomain")
    private String reservedDomain;
}
