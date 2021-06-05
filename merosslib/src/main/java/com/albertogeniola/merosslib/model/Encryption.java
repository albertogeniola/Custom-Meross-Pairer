package com.albertogeniola.merosslib.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public enum Encryption {
    OPEN(0),
    SHARE(1),
    WEPAUTO(2),
    WPA1(3),
    WPA1PSK(4),
    WPA2(5),
    WPA2PSK(6),
    WPA1WPA2(7),
    WPA1PSKWPA2PSK(8);

    private int value;
    private Encryption(int value) {
        this.value = value;
    }
    public static Encryption findByAbbr(int value)
    {
        for (Encryption currEnum : Encryption.values())
        {
            if (currEnum.value == value)
            {
                return currEnum;
            }
        }
        return null;
    }

    public static class TypeDeserializer implements JsonDeserializer<Encryption>, JsonSerializer<Encryption>
    {
        @Override
        public Encryption deserialize(JsonElement json,
                                  Type typeOfT, JsonDeserializationContext ctx)
                throws JsonParseException
        {
            int typeInt = json.getAsInt();
            return Encryption.findByAbbr(typeInt);
        }

        @Override
        public JsonElement serialize(Encryption src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.value);
        }
    }
}
