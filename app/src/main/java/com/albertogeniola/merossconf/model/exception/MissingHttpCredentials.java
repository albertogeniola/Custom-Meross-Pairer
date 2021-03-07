package com.albertogeniola.merossconf.model.exception;

public class MissingHttpCredentials extends Exception {
    public MissingHttpCredentials() {
        super();
    }
    public MissingHttpCredentials(String msg) {
        super(msg);
    }
}
