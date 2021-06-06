package com.albertogeniola.merossconf.model;

import android.util.Base64;
import com.albertogeniola.merosslib.model.protocol.payloads.GetConfigWifiListEntry;
import java.io.UnsupportedEncodingException;
import lombok.Getter;

@Getter
public class WifiConfiguration {
    private final GetConfigWifiListEntry scannedWifi;
    private final String clearWifiPassword;

    public WifiConfiguration(GetConfigWifiListEntry scannedWifi, String clearWifiPassword) {
        this.scannedWifi = scannedWifi;
        this.clearWifiPassword = clearWifiPassword;
    }

    public String getWifiPasswordBase64() {
        try{
            return Base64.encodeToString(clearWifiPassword.toString().getBytes("utf8"), Base64.NO_WRAP);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            throw new RuntimeException("UTF8 unsupported");
        }
    }
}
