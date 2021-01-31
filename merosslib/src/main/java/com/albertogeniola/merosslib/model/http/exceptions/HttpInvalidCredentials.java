package com.albertogeniola.merosslib.model.http.exceptions;

public class HttpInvalidCredentials extends Exception {
    public HttpInvalidCredentials() {
        super("Invalid username-email / password combination");
    }
}
