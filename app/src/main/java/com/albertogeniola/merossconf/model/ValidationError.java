package com.albertogeniola.merossconf.model;

public class ValidationError extends Exception {
    public ValidationError(String msg) {
        super(msg);
    }
}
