package com.albertogeniola.merosslib.model.http.exceptions;

import com.albertogeniola.merosslib.model.http.ErrorCodes;

import lombok.Getter;

@Getter
public class HttpApiBadDomainException extends HttpApiException{
    private String domain;
    public HttpApiBadDomainException(ErrorCodes code, String targetDomain) {
        super(code);
        this.domain = targetDomain;
    }
}
