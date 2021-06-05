package com.albertogeniola.merosslib.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public enum Cipher {
    NONE(0),
    WEP(1),
    TKIP(2),
    AES(3),
    TIKPAES(4);

    private int value;
    private Cipher(int value) {
        this.value = value;
    }
    public static Cipher findByAbbr(int value)
    {
        for (Cipher currEnum : Cipher.values())
        {
            if (currEnum.value == value)
            {
                return currEnum;
            }
        }
        return null;
    }

    public static class TypeDeserializer implements JsonDeserializer<Cipher>, JsonSerializer<Cipher>
    {
        @Override
        public Cipher deserialize(JsonElement json,
                                  Type typeOfT, JsonDeserializationContext ctx)
                throws JsonParseException
        {
            int typeInt = json.getAsInt();
            return Cipher.findByAbbr(typeInt);
        }

        @Override
        public JsonElement serialize(Cipher src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.value);
        }
    }
}
