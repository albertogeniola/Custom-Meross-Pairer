package com.albertogeniola.merossconf.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TargetWifiAp {
    private String ssid;
    private String bssid;
    private String password;

    public TargetWifiAp(String ssid, String bssid) {
        this(ssid, bssid, null);
    }

    public TargetWifiAp(String ssid, String bssid, String password) {
        this.bssid=bssid;
        this.ssid=ssid;
        this.password=password;
    }
}
