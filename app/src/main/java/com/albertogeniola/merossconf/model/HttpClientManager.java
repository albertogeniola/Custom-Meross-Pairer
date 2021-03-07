package com.albertogeniola.merossconf.model;

import android.content.Context;

import com.albertogeniola.merossconf.AndroidPreferencesManager;
import com.albertogeniola.merossconf.model.exception.MissingHttpCredentials;
import com.albertogeniola.merosslib.MerossHttpClient;
import com.albertogeniola.merosslib.model.http.ApiCredentials;

public class HttpClientManager {
    private static MerossHttpClient mClient;

    public static MerossHttpClient getInstance(Context c) {
        if (mClient == null) {
            mClient = fetchStoredClient(c);
        }
        return mClient;
    }

    private static MerossHttpClient fetchStoredClient(Context c) throws MissingHttpCredentials {
        ApiCredentials creds = AndroidPreferencesManager.loadHttpCredentials(c);
        if (creds == null)
            throw new MissingHttpCredentials("No HTTP credentials are available");

        if (creds.isExpired())
            throw new MissingHttpCredentials("Stored HTTP credentials has expired");

        MerossHttpClient client = new MerossHttpClient();
    }
}
