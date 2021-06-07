package com.albertogeniola.merosslib.model.http;

import com.google.gson.annotations.SerializedName;

import lombok.Getter;

@Getter
public class LoginResponseData {
    @SerializedName("token")
    private String token;

    @SerializedName("key")
    private String key;

    @SerializedName("userid")
    private String userId;

    @SerializedName("email")
    private String email;
}
