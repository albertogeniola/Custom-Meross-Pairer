package com.albertogeniola.merosslib.model.protocol.payloads;

import com.google.gson.annotations.SerializedName;

import lombok.Getter;

@Getter
public class GetConfigWifiListEntry {
    @SerializedName("ssid")
    private String ssid;

    @SerializedName("bssid")
    private String bssid;

    @SerializedName("signal")
    private Double signal;

    @SerializedName("channel")
    private Integer channel;

    @SerializedName("encryption")
    private Integer encryption; // Enum?

    @SerializedName("cipher")
    private Integer cipher; // Enum?
}
