package com.albertogeniola.merosslib.model.http.exceptions;

import com.albertogeniola.merosslib.model.http.ErrorCodes;

public class HttpApiTokenException extends HttpApiException{
    public HttpApiTokenException(ErrorCodes code) {
        super(code);
    }
}
