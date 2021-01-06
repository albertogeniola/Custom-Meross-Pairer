package com.albertogeniola.merosslib.model.protocol.payloads;

import com.albertogeniola.merosslib.model.OnlineStatus;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;

@Getter
public class GetSystemAllPayloadAllOnline {
    @SerializedName("online")
    private OnlineStatus status;
}
