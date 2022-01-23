package com.albertogeniola.merossconf.model;

import android.content.Context;
import android.os.AsyncTask;

import androidx.annotation.Nullable;

import com.albertogeniola.merossconf.AndroidPreferencesManager;
import com.albertogeniola.merossconf.model.exception.MissingHttpCredentials;
import com.albertogeniola.merosslib.MerossHttpClient;
import com.albertogeniola.merosslib.model.http.ApiCredentials;
import com.albertogeniola.merosslib.model.http.DeviceInfo;

import java.util.List;
import java.util.concurrent.Executor;

public class HttpClientManager {
    // Singleton pattern
    private static HttpClientManager mClientManager;
    public static HttpClientManager getInstance() {
        if (mClientManager == null) {
            mClientManager = new HttpClientManager();
        }
        return mClientManager;
    }

    // Instance attributes
    private MerossHttpClient mClient;

    private HttpClientManager() {
        mClient = new MerossHttpClient();
    }

    public MerossHttpClient loadFromCredentials(ApiCredentials creds) {
        mClient = new MerossHttpClient(creds);
        return mClient;
    }

    public void asyncLogin(final String serverUrl, final String username, final String password, Callback<ApiCredentials> callback) {
        if (mClient == null) {
            throw new IllegalStateException("HttpClient has not been loaded yet.");
        }

        CallbackTask<ApiCredentials> t = new CallbackTask<ApiCredentials>(callback) {
            @Override
            protected ApiCredentials run(MerossHttpClient client) throws Exception {
                client.login(serverUrl, username, password);
                return client.getCredentials();
            }
        };
        t.execute(mClient);
    }

    public void asyncLogout(Callback<Void> callback) {
        if (mClient == null)
            throw new IllegalStateException("HttpClient has not been loaded yet");

        CallbackTask<Void> t = new CallbackTask<Void>(callback) {
            @Override
            protected Void run(MerossHttpClient client) throws Exception {
                client.logout();
                return null;
            }
        };
        t.execute(mClient);
    }

    public void asyncListDevices(Callback<List<DeviceInfo>> callback) {
        if (mClient == null)
            throw new IllegalStateException("HttpClient has not been loaded yet");

        CallbackTask<List<DeviceInfo>> t = new CallbackTask<List<DeviceInfo>>(callback) {
            @Override
            protected List<DeviceInfo> run(MerossHttpClient client) throws Exception {
                return client.listDevices();
            }
        };
        t.execute(mClient);
    }

    private static MerossHttpClient fetchStoredClient(Context c) throws MissingHttpCredentials {
        ApiCredentials creds = AndroidPreferencesManager.loadHttpCredentials(c);
        if (creds == null)
            throw new MissingHttpCredentials("No HTTP credentials are available");

        if (creds.isExpired())
            throw new MissingHttpCredentials("Stored HTTP credentials has expired");

        return new MerossHttpClient(creds);
    }

    private static abstract class CallbackTask<T> {
        private Callback<T> mCallback;

        public CallbackTask(Callback<T> callback) {
            mCallback = callback;
        }

        public void execute(MerossHttpClient client) {
            execute(client, null);
        }

        public void execute(MerossHttpClient client, @Nullable Executor executor) {
            HttpClientTask<T> task = new HttpClientTask<T>() {
                private Exception mException = null;
                @Override
                protected T doInBackground(MerossHttpClient... merossHttpClients) {
                    try {
                        MerossHttpClient client = merossHttpClients[0];
                        return run(client);
                    } catch (Exception e) {
                        mException = e;
                        return null;
                    }
                }

                @Override
                protected void onPostExecute(T result) {
                    if (mException == null)
                        mCallback.onSuccess(result);
                    else
                        mCallback.onFailure(mException);
                }
            };

            if (executor == null)
                task.execute(client);
            else
                task.executeOnExecutor(executor, client);
        }

        protected abstract T run(MerossHttpClient client) throws Exception;
    }

    private abstract static class HttpClientTask<T> extends AsyncTask<MerossHttpClient, Void, T> {}

    public interface Callback<T> {
        public void onSuccess(T result);
        public void onFailure(Exception result);
    }
}
