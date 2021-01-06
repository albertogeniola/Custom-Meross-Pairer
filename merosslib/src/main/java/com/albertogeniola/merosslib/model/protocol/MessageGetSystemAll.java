package com.albertogeniola.merosslib.model.protocol;

import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
public class MessageGetSystemAll extends Message {

    @SerializedName("payload")
    private MessagePayload payload;

    public static MessageGetSystemAll BuildNew() {
        Header header = Header.BuildNew("Appliance.System.All", Method.GET);
        MessageGetSystemAll resp = new MessageGetSystemAll();
        resp.setHeader(header);
        resp.payload = new MessagePayload();
        return resp;
    }
}
