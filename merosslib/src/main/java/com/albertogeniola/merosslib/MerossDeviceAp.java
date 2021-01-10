package com.albertogeniola.merosslib;

import com.albertogeniola.merosslib.model.protocol.Message;
import com.albertogeniola.merosslib.model.protocol.MessageGetConfigWifiList;
import com.albertogeniola.merosslib.model.protocol.MessageGetConfigWifiListResponse;
import com.albertogeniola.merosslib.model.protocol.MessageGetSystemAll;
import com.albertogeniola.merosslib.model.protocol.MessageGetSystemAllResponse;
import com.albertogeniola.merosslib.model.protocol.MessageSetConfigKey;
import com.albertogeniola.merosslib.model.protocol.MessageSetConfigKeyResponse;
import com.albertogeniola.merosslib.model.protocol.MessageSetConfigWifi;
import com.albertogeniola.merosslib.model.protocol.MessageSetConfigWifiResponse;
import com.google.gson.Gson;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;

import javax.net.SocketFactory;

import lombok.Getter;
import lombok.Setter;


public class MerossDeviceAp implements Serializable {
    private String ip;
    private String cloudKey;
    private OkHttpClient client;
    private Gson g = new Gson();

    public void setSocketFactory(SocketFactory factory) {
        client.setSocketFactory(factory);
    }

    public MerossDeviceAp(String ip, String cloudKey) {
        this.ip = ip;
        this.cloudKey = cloudKey;
        this.client = new OkHttpClient();
        this.client.setConnectTimeout(40, TimeUnit.SECONDS);
        this.client.setReadTimeout(40, TimeUnit.SECONDS);
    }

    public MerossDeviceAp() {
        this("10.10.10.1", "");
    }

    public MessageGetSystemAllResponse getConfig() throws IOException {
        Message message = MessageGetSystemAll.BuildNew();
        return this.sendMessage(message, MessageGetSystemAllResponse.class);
    }

    public MessageGetConfigWifiListResponse scanWifi() throws IOException {
        Message message = MessageGetConfigWifiList.BuildNew();
        return this.sendMessage(message, MessageGetConfigWifiListResponse.class);
    }

    public MessageSetConfigKeyResponse setConfigKey(String hostname, int port, String key, String userId ) throws IOException {
        Message message = MessageSetConfigKey.BuildNew(hostname, port, key, userId);
        return this.sendMessage(message, MessageSetConfigKeyResponse.class);
    }

    public MessageSetConfigWifiResponse setConfigWifi(String base64ssid, String base64password) throws IOException {
        Message message = MessageSetConfigWifi.BuildNew(base64ssid, base64password);
        return this.sendMessage(message, MessageSetConfigWifiResponse.class);
    }

    private <T> T sendMessage(Message message, Class<T> type) throws IOException {
        message.sign(cloudKey); // Signature is not verified when pairing!

        Request request = new Request.Builder()
                .url("http://" + ip + "/config")
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), g.toJson(message).getBytes("utf8")))
                .build();
        Response response = client.newCall(request).execute();

        if (response.code() != 200 ) {
            throw new IOException("Invalid response code (" + response.code() + ") received from Meross Device");
        }
        return g.fromJson(response.body().string(), type);
    }
}
