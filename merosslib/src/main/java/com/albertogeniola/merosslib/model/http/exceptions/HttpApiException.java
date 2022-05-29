package com.albertogeniola.merosslib.model.http.exceptions;

import com.albertogeniola.merosslib.model.http.ErrorCodes;

import lombok.Getter;

@Getter
public class HttpApiException extends Exception {
    private ErrorCodes code;
    public HttpApiException(ErrorCodes code) {
        this(code, "HTTP Api returned status code " + code.getCode());
    }

    public HttpApiException(ErrorCodes code, String message) {
        super(message);
        this.code = code;
    }

    public String getErrorMessage() {
        return code.getMessage();
    }
}
