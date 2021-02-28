package com.albertogeniola.merossconf;

public class MerossUtils {
    public static boolean isMerossAp(String ssid) {
        return ssid.matches("^Meross_([A-Z0-9]{2})_[a-zA-Z0-9]{4}$");
    }
}
