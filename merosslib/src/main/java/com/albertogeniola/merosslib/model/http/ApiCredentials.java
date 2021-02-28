package com.albertogeniola.merosslib.model.http;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiCredentials {
    private String apiServer;
    private String token;
    private String userId;
    private String userEmail;
    private String key;
    private Date issuedOn;
}
