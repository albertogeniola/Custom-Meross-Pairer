package com.albertogeniola.merossconf;

public class MerossUtils {
    public static boolean isMerossAp(String ssid) {
        return ssid.matches("^Meross_SW_[a-zA-Z0-9]{4}$");
    }
}
