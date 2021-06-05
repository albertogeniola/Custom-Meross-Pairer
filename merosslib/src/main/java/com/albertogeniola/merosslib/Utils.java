package com.albertogeniola.merosslib;

import com.albertogeniola.merosslib.model.Cipher;
import com.albertogeniola.merosslib.model.Encryption;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Utils {
    public static Gson getGson() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Cipher.class, new Cipher.TypeDeserializer() );
        gsonBuilder.registerTypeAdapter(Encryption.class, new Encryption.TypeDeserializer() );
        Gson gson = gsonBuilder.create();
        return gson;
    }
}
