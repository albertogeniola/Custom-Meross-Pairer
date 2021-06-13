package com.albertogeniola.merosslib.model.http;

import com.albertogeniola.merosslib.model.OnlineStatus;
import com.google.gson.annotations.SerializedName;

import java.util.List;

import lombok.Getter;

@Getter
public class DeviceInfo {
    @SerializedName("uuid")
    private String uuid;

    @SerializedName("online_status")
    private OnlineStatus onlineStatus;

    @SerializedName("devName")
    private String devName;

    @SerializedName("bind_time")
    private String bindTime;

    @SerializedName("device_type")
    private String deviceType;

    @SerializedName("sub_type")
    private String subType;

    @SerializedName("channels")
    private List<Object> channels;

    @SerializedName("region")
    private String region;

    @SerializedName("fmware_version")
    private String fmwareVersion;

    @SerializedName("hdware_version")
    private String hdwareVersion;

    @SerializedName("user_dev_icon")
    private String userDevIcon;

    @SerializedName("icon_type")
    private int iconType;

    @SerializedName("skill_number")
    private String skillNumber;

    @SerializedName("domain")
    private String domain;

    @SerializedName("reserved_domain")
    private String reservedDomain;
}
