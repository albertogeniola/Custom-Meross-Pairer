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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import org.apache.commons.io.IOUtils;


public class MerossDeviceAp implements Serializable {
    private String ip;
    private String cloudKey;

    public MerossDeviceAp(String ip, String cloudKey) {
        this.ip = ip;
        this.cloudKey = cloudKey;
    }

    public MerossDeviceAp(String ip) {
        this(ip, "");
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
        URL url = new URL("http://" + ip + "/config");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setDoOutput(true);
        con.setUseCaches(false);
        con.setDoInput(true);

        try (BufferedOutputStream osw = new BufferedOutputStream(con.getOutputStream())) {
            Gson g = new Gson();
            osw.write(g.toJson(message).getBytes("utf8"));
        }

        int code = con.getResponseCode();
        if (code != 200 ) {
            throw new IOException("Invalid response code (" + code + ") received from Meross Device");
        }

        try (InputStream inputStr = con.getInputStream()) {
            String encoding = con.getContentEncoding() == null ? "UTF-8" : con.getContentEncoding();
            String jsonResponse = IOUtils.toString(inputStr, encoding);
            Gson g = new Gson();
            return g.fromJson(jsonResponse, type);
        }
    }
}
