package com.albertogeniola.merosslib.model.protocol;

import com.albertogeniola.merosslib.model.protocol.payloads.GetConfigWifiListPayload;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MessageGetConfigWifiListResponse extends Message {
    @SerializedName("payload")
    private GetConfigWifiListPayload payload;
}
