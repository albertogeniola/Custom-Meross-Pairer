package com.albertogeniola.merossconf;

import androidx.annotation.Nullable;

public class MerossUtils {
    public static boolean isMerossAp(@Nullable String ssid) {
        if (ssid == null)
            return false;
        return ssid.matches("^Meross_([A-Z0-9]{2})_[a-zA-Z0-9]{4}$");
    }
}
