package com.albertogeniola.merosslib.model;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;


public enum OnlineStatus {
    OFFLINE(0),
    ONLINE(1),
    LAN(2),
    UPGRADING(3);

    private int value;
    private OnlineStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static OnlineStatus findByAbbr(int value)
    {
        for (OnlineStatus currEnum : OnlineStatus.values())
        {
            if (currEnum.value == value)
            {
                return currEnum;
            }
        }
        return null;
    }

    public static class TypeDeserializer implements JsonDeserializer<OnlineStatus>, JsonSerializer<OnlineStatus>
    {
        @Override
        public OnlineStatus deserialize(JsonElement json,
                                  Type typeOfT, JsonDeserializationContext ctx)
                throws JsonParseException
        {
            int typeInt = json.getAsInt();
            return OnlineStatus.findByAbbr(typeInt);
        }

        @Override
        public JsonElement serialize(OnlineStatus src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.value);
        }
    }
}
