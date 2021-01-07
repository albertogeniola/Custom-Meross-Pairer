package com.albertogeniola.merosslib.model.protocol;

import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@NoArgsConstructor
public class MessageSetConfigWifi extends Message {
    @SerializedName("payload")
    private SetConfigWifiPayload payload;

    public static MessageSetConfigWifi BuildNew(String base64ssid, String base64password) {
        Header header = Header.BuildNew("Appliance.Config.Wifi", Method.SET);
        MessageSetConfigWifi msg = new MessageSetConfigWifi();
        msg.setHeader(header);

        msg.payload = new SetConfigWifiPayload();
        msg.payload.wifi = new SetConfigWifiPayloadWifi();
        msg.payload.wifi.ssid = base64ssid;
        msg.payload.wifi.password = base64password;

        return msg;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    static class SetConfigWifiPayload extends MessagePayload {
        @SerializedName("wifi")
        private SetConfigWifiPayloadWifi wifi;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    static class SetConfigWifiPayloadWifi {
        @SerializedName("ssid")
        private String ssid;

        @SerializedName("password")
        private String password;
    }
}


