package com.albertogeniola.merosslib.model.protocol;

import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MessageSetConfigKey extends Message {
    @SerializedName("payload")
    private SetConfigKeyPayload payload;

    public static MessageSetConfigKey BuildNew(String hostname, int port, String key, String userId) {
        Header header = Header.BuildNew("Appliance.Config.Key", Method.SET);
        MessageSetConfigKey msg = new MessageSetConfigKey();
        msg.setHeader(header);

        msg.payload = new SetConfigKeyPayload();
        msg.payload.key = new SetConfigKeyPayloadKey();
        msg.payload.key.userId = userId;
        msg.payload.key.key = key;
        msg.payload.key.gateway = new SetConfigKeyPayloadKeyGateway();
        msg.payload.key.gateway.host = hostname;
        msg.payload.key.gateway.port = ""+port;

        // Both secondary as the primary
        msg.payload.key.gateway.secondHost = hostname;
        msg.payload.key.gateway.secondPort = ""+port;
        return msg;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    private static class SetConfigKeyPayload extends MessagePayload {
        @SerializedName("key")
        private SetConfigKeyPayloadKey key;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    private static class SetConfigKeyPayloadKey {
        @SerializedName("gateway")
        private SetConfigKeyPayloadKeyGateway gateway;

        @SerializedName("key")
        private String key;

        @SerializedName("userId")
        private String userId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    private static class SetConfigKeyPayloadKeyGateway {
        @SerializedName("host")
        private String host;

        @SerializedName("port")
        private String port;

        @SerializedName("secondHost")
        private String secondHost;

        @SerializedName("secondPort")
        private String secondPort;
    }
}
