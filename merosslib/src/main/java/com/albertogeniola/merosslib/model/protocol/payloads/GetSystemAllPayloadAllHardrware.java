package com.albertogeniola.merosslib.model.protocol.payloads;

import com.google.gson.annotations.SerializedName;

import lombok.Getter;

@Getter
public class GetSystemAllPayloadAllHardrware {
    @SerializedName("type")
    private String type;

    @SerializedName("subType")
    private String subType;

    @SerializedName("version")
    private String version;

    @SerializedName("chipType")
    private String chipType;

    @SerializedName("uuid")
    private String uuid;

    @SerializedName("macAddress")
    private String macAddress;

}
