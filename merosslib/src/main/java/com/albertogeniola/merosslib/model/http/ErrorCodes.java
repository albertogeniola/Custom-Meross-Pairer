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

    @SerializedName("1030")
    CODE_BAD_DOMAIN(1030),

    @SerializedName("1200")
    CODE_TOKEN_EXPIRED (1200),

    @SerializedName("1022")
    CODE_TOKEN_ERROR (1022),

    @SerializedName("1301")
    CODE_TOO_MANY_TOKENS(1301),

    @SerializedName("5000")
    CODE_GENERIC_ERROR(5000);

    private int value;
    ErrorCodes(int val) {
        this.value = val;
    }
    public int getCode()
    {
        return this.value;
    }

    public String getMessage() {
        if (value==CODE_NO_ERROR.value)
            return "All OK";
        else if (value==CODE_MISSING_PASSWORD.value)
            return "Missing password";
        else if (value==CODE_UNEXISTING_ACCOUNT.value)
            return "Account does not exist";
        else if (value==CODE_DISABLED_OR_DELETED_ACCOUNT.value)
            return "Account has been disabled or deleted";
        else if (value==CODE_WRONG_CREDENTIALS.value)
            return "Bad/wrong credentials";
        else if (value==CODE_INVALID_EMAIL.value)
            return "Invalid email address";
        else if (value==CODE_BAD_PASSWORD_FORMAT.value)
            return "Invalid password format";
        else if (value==CODE_WRONG_EMAIL.value)
            return "Wrong email";
        else if (value==CODE_TOKEN_INVALID.value)
            return "The token is invalid";
        else if (value==CODE_TOKEN_EXPIRED.value)
            return "Token has expired";
        else if (value==CODE_TOKEN_ERROR.value)
            return "Token error";
        else if (value==CODE_TOO_MANY_TOKENS.value)
            return "Too many tokens have been released.";
        else if (value==CODE_GENERIC_ERROR.value)
            return "Generic error";
        else
            return "Unknown error code";
    }
}