package com.albertogeniola.merosslib.model.protocol;

import com.albertogeniola.merosslib.model.Cipher;
import com.albertogeniola.merosslib.model.Encryption;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@NoArgsConstructor
public class MessageSetConfigWifi extends Message {
    @SerializedName("payload")
    private SetConfigWifiPayload payload;

    public static MessageSetConfigWifi BuildNew(String base64ssid, String base64password, String bssid, int channel, Cipher cipher, Encryption encryption) {
        Header header = Header.BuildNew("Appliance.Config.Wifi", Method.SET);
        MessageSetConfigWifi msg = new MessageSetConfigWifi();
        msg.setHeader(header);

        msg.payload = new SetConfigWifiPayload();
        msg.payload.wifi = new SetConfigWifiPayloadWifi();
        msg.payload.wifi.base64ssid = base64ssid;
        msg.payload.wifi.base64password = base64password;
        msg.payload.wifi.bssid = bssid;
        msg.payload.wifi.channel = channel;
        msg.payload.wifi.cipher = cipher;
        msg.payload.wifi.encryption = encryption;

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
        private String base64ssid;

        @SerializedName("password")
        private String base64password;

        @SerializedName("bssid")
        private String bssid;

        @SerializedName("channel")
        private int channel;

        @SerializedName("cipher")
        private Cipher cipher;

        @SerializedName("encryption")
        private Encryption encryption;
    }
}


