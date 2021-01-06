package com.albertogeniola.merosslib.model.protocol.payloads;

import com.google.gson.annotations.SerializedName;

import lombok.Getter;

@Getter
public class GetSystemAllPayloadAllTime {
    @SerializedName("timestamp")
    private long timestamp;

    @SerializedName("timezone")
    private String timezone;

    // "timeRule": []
}
