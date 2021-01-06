package com.albertogeniola.merosslib.model.protocol.payloads;

import com.albertogeniola.merosslib.model.protocol.MessagePayload;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;

@Getter
public class GetSystemAllPayload extends MessagePayload {
    @SerializedName("all")
    private GetSystemAllPayloadAll all;
}
