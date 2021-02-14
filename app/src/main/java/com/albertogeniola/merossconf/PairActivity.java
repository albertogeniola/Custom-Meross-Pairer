package com.albertogeniola.merossconf;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.albertogeniola.merossconf.model.WifiLocationStatus;
import com.albertogeniola.merossconf.ui.PairActivityViewModel;

import static android.location.LocationManager.PROVIDERS_CHANGED_ACTION;
import static android.net.wifi.WifiManager.NETWORK_STATE_CHANGED_ACTION;

public class PairActivity extends AppCompatActivity implements ProgressableActivity {
    private ProgressBar progressBar;
    private PairActivityViewModel viewModel;
    private BroadcastReceiver mReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pair);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        this.viewModel = new ViewModelProvider(this).get(PairActivityViewModel.class);
        this.progressBar = findViewById(R.id.progressBar);
        this.mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                WifiLocationStatus status = viewModel.getWifiLocationStatus().getValue();
                if (status == null) {
                    status = new WifiLocationStatus(null, null);
                }

                if(intent.getAction().equals(NETWORK_STATE_CHANGED_ACTION) || intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                    status.setWifiEnabledOrEnabling(AndroidUtils.isWifiEnabbled(PairActivity.this));
                    viewModel.setWifiLocationStatus(status);
                } else if (intent.getAction().equals(PROVIDERS_CHANGED_ACTION)) {
                    status.setLocationEnabledOrEnabling(AndroidUtils.isLocationEnabled(PairActivity.this));
                    viewModel.setWifiLocationStatus(status);
                }
            }
        };

        boolean wifiOk = AndroidUtils.isWifiEnabbled(this);
        boolean positionOk = AndroidUtils.isLocationEnabled(this);
        viewModel.setWifiLocationStatus(new WifiLocationStatus(wifiOk, positionOk));
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(PROVIDERS_CHANGED_ACTION);
        this.registerReceiver(this.mReceiver, filter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.unregisterReceiver(this.mReceiver);
    }

    @Override
    public void setProgressIndeterminate() {
        this.progressBar.setIndeterminate(true);
        this.progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void setProgressDone() {
        this.progressBar.setIndeterminate(true);
        this.progressBar.setVisibility(View.INVISIBLE);
    }
}
