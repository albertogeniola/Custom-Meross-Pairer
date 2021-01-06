package com.albertogeniola.merosslib.model.protocol.payloads;

import com.google.gson.annotations.SerializedName;

import lombok.Getter;

@Getter
public class GetSystemAllPayloadAll {
    @SerializedName("system")
    private GetSystemAllPayloadAllSystem system;

    @SerializedName("digest")
    private Object digest;  // This object class depends on the specific device type,
                            // so we cannot bind it here.
}
