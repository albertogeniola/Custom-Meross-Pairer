package com.albertogeniola.merosslib.model.http.exceptions;

import com.albertogeniola.merosslib.model.http.ErrorCodes;

public class HttpApiMissingMFAException extends HttpApiException{
    public HttpApiMissingMFAException(ErrorCodes code) {
        super(code);
    }
}
