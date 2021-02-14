package com.albertogeniola.merossconf;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.albertogeniola.merossconf.model.MqttConfiguration;
import com.albertogeniola.merosslib.model.http.ApiCredentials;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

public class AndroidPreferencesManager {
    private static String PREFS_CONFS = "com.albertogeniola.merossconf.shared_preferences";
    private static String KEY_MQTT_CONF = "mqtt";
    private static String KEY_HTTP_CONF = "http";
    private static final Gson g = new Gson();

    public static void storeNewMqttConfiguration(Context c, MqttConfiguration conf) {
        SharedPreferences settings = c.getSharedPreferences(PREFS_CONFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        String json = g.toJson(conf);
        editor.putString(KEY_MQTT_CONF, json);
        editor.apply();
    }

    public static List<MqttConfiguration> loadAllMqttConfigurations(Context c) {
        SharedPreferences settings = c.getSharedPreferences(PREFS_CONFS, Context.MODE_PRIVATE);
        List<MqttConfiguration> res = null;
        try {
            res = g.fromJson(settings.getString(KEY_MQTT_CONF, null), new TypeToken<List<MqttConfiguration>>() {}.getType());
        } catch (Exception e) {
            // Error while loading configurations
            Toast.makeText(c, "Error while loading MQTT configurations", Toast.LENGTH_SHORT).show();
        }
        if (res == null)
            return new ArrayList<>();
        else
            return res;

    }

    public static void storeHttpCredentials(Context c, ApiCredentials httpCredentials) {
        SharedPreferences settings = c.getSharedPreferences(PREFS_CONFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        String json = g.toJson(httpCredentials);
        editor.putString(KEY_HTTP_CONF, json);
        editor.apply();
    }

    @Nullable
    public static ApiCredentials loadHttpCredentials(Context c) {
        SharedPreferences settings = c.getSharedPreferences(PREFS_CONFS, Context.MODE_PRIVATE);
        return g.fromJson(settings.getString(KEY_HTTP_CONF, null), ApiCredentials.class);
    }
}
