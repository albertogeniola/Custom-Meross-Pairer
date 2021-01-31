package com.albertogeniola.merosslib.model.http;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

import lombok.Getter;

@Getter
public class ApiResponse {
    @SerializedName("apiStatus")
    private ErrorCodes apiStatus;

    @SerializedName("info")
    private String info;

    @SerializedName("data")
    private Map<String, Object> data;
}
