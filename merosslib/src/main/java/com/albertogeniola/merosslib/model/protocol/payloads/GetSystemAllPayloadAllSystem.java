package com.albertogeniola.merosslib.model.protocol.payloads;

import com.google.gson.annotations.SerializedName;

import lombok.Getter;

@Getter
public class GetSystemAllPayloadAllSystem {
    @SerializedName("hardware")
    private GetSystemAllPayloadAllHardrware hardware;

    @SerializedName("firmware")
    private GetSystemAllPayloadAllFirmware firmware;

    @SerializedName("time")
    private GetSystemAllPayloadAllTime time;

    @SerializedName("online")
    private GetSystemAllPayloadAllOnline online;
}
