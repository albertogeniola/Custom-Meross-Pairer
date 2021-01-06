package com.albertogeniola.merosslib.model.protocol;

import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
public class MessageGetConfigWifiList extends Message {

    @SerializedName("payload")
    private Object payload;

    public static MessageGetConfigWifiList BuildNew() {
        Header header = Header.BuildNew("Appliance.Config.WifiList", Method.GET);
        MessageGetConfigWifiList resp = new MessageGetConfigWifiList();
        resp.setHeader(header);
        resp.payload = new Object();
        return resp;
    }
}
