package com.albertogeniola.merossconf;

import android.content.Context;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;

import com.albertogeniola.merosslib.model.Cipher;
import com.albertogeniola.merosslib.model.Encryption;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.regex.Pattern;


public class AndroidUtils {
    public static boolean isLocationEnabled(Context context)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // This is new method provided in API 28
            LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            return lm.isLocationEnabled();
        } else {
            // This is Deprecated in API 28
            int mode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE,
                    Settings.Secure.LOCATION_MODE_OFF);
            return  (mode != Settings.Secure.LOCATION_MODE_OFF);

        }
    }

    public static boolean isWifiEnabbled(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        return wifiManager.isWifiEnabled();
    }

    public static boolean validateBaseUrl(String url) {
        return Pattern.matches("^(http|https)\\:\\/\\/([\\_\\-a-zA-Z0-9\\.]+)(\\:[0-9]+)?$", url);
    }

}
