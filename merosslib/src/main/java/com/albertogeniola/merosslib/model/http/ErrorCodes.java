package com.albertogeniola.merosslib.model.http;

import com.google.gson.annotations.SerializedName;

public enum ErrorCodes {
    @SerializedName("0")
    CODE_NO_ERROR(0),

    @SerializedName("1001")
    CODE_MISSING_PASSWORD(1001),

    @SerializedName("1002")
    CODE_UNEXISTING_ACCOUNT(1002),

    @SerializedName("1003")
    CODE_DISABLED_OR_DELETED_ACCOUNT(1003),

    @SerializedName("1004")
    CODE_WRONG_CREDENTIALS(1004),

    @SerializedName("1005")
    CODE_INVALID_EMAIL(1005),

    @SerializedName("1006")
    CODE_BAD_PASSWORD_FORMAT(1006),

    @SerializedName("1008")
    CODE_WRONG_EMAIL(1008),

    @SerializedName("1019")
    CODE_TOKEN_INVALID(1019),

    @SerializedName("1200")
    CODE_TOKEN_EXPIRED (1200),

    @SerializedName("1022")
    CODE_TOKEN_ERROR (1022),

    @SerializedName("1301")
    CODE_TOO_MANY_TOKENS(1301),

    @SerializedName("5000")
    CODE_ERROR_GENERIC(5000);

    private int value;
    ErrorCodes(int val) {
        this.value = val;
    }
    public int getCode()
    {
        return this.value;
    }

}