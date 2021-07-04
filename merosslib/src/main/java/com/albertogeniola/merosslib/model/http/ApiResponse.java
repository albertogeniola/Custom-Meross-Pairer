package com.albertogeniola.merosslib.model.http;

import com.google.gson.annotations.SerializedName;

import lombok.Getter;

@Getter
public class ApiResponse<T> {
    @SerializedName("apiStatus")
    private ErrorCodes apiStatus;

    @SerializedName("info")
    private String info;

    @SerializedName("data")
    private T data;
}
