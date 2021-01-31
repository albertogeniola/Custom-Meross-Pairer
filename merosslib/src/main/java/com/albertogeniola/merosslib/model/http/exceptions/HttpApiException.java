package com.albertogeniola.merosslib.model.http.exceptions;

import com.albertogeniola.merosslib.model.http.ErrorCodes;

import lombok.Getter;

@Getter
public class HttpApiException extends Exception {
    private ErrorCodes code;
    public HttpApiException(ErrorCodes code) {
        super("HTTP Api returned status code " + code.getCode());
        this.code = code;
    }
}
