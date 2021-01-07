package com.albertogeniola.merosslib.model.protocol;

import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MessageSetConfigKeyResponse extends Message {
    @SerializedName("payload")
    private Object payload;
}
