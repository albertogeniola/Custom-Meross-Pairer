package com.albertogeniola.merosslib.model.http;

import java.util.Date;

import jdk.internal.jline.internal.Nullable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@Getter
public class ApiCredentials {

    @Nullable
    private String apiServer;

    @Nullable
    private String token;

    @NonNull
    private final String userId;

    @Nullable
    private String userEmail;

    @NonNull
    private final String key;

    @Nullable
    private Date issuedOn;

    public ApiCredentials(String userId, String key) {
        this.userId = userId;
        this.key = key;
    }

    public ApiCredentials(@Nullable String apiServer, @Nullable String token, @NonNull String userId, @Nullable String userEmail, @NonNull String key, @Nullable Date issuedOn) {
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
