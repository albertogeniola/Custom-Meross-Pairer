package com.albertogeniola.merossconf.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.albertogeniola.merosslib.model.http.ApiCredentials;

public class MainActivityViewModel extends ViewModel {
    private MutableLiveData<ApiCredentials> mCredentials;

    public MainActivityViewModel() {
        mCredentials = new MutableLiveData<>();
        mCredentials.setValue(null);
    }

    public LiveData<ApiCredentials> getCredentials() {
        return mCredentials;
    }
    public void setCredentials(ApiCredentials creds) {
        mCredentials.setValue(creds);
    }
}