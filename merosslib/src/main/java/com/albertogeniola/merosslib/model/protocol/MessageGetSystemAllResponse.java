package com.albertogeniola.merosslib.model.protocol;

import com.albertogeniola.merosslib.model.protocol.payloads.GetSystemAllPayload;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MessageGetSystemAllResponse extends Message {
    @SerializedName("payload")
    private GetSystemAllPayload payload;
}
