package com.albertogeniola.merosslib.model.http;

import java.util.Date;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class ApiCredentials {

    private String apiServer;

    private String token;

    @NonNull
    private final String userId;

    private String userEmail;

    @NonNull
    private final String key;

    private Date issuedOn;

    public ApiCredentials(String userId, String key) {
        this.userId = userId;
        this.key = key;
    }

    public ApiCredentials(String apiServer, String token, @NonNull String userId, String userEmail, @NonNull String key, Date issuedOn) {
        this.apiServer = apiServer;
        this.token = token;
        this.userId = userId;
        this.userEmail = userEmail;
        this.key = key;
        this.issuedOn = issuedOn;
    }

    public boolean isManuallySet() {
        return apiServer==null || token == null || userEmail == null;
    }

    public boolean isExpired() {
        // TODO: for now, tokens never expire
        return false;
    }
}
