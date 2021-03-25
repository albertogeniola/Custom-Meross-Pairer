package com.albertogeniola.merosslib.model.http.exceptions;

import com.albertogeniola.merosslib.model.http.ErrorCodes;

public class HttpApiInvalidCredentialsException extends HttpApiException {
    public HttpApiInvalidCredentialsException(ErrorCodes code) {
        super(code);
    }
}
