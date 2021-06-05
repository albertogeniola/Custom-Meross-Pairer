package com.albertogeniola.merossconf;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.albertogeniola.merossconf.model.WifiLocationStatus;
import com.albertogeniola.merossconf.ui.PairActivityViewModel;

import static android.location.LocationManager.PROVIDERS_CHANGED_ACTION;
import static android.net.wifi.WifiManager.NETWORK_STATE_CHANGED_ACTION;

public class PairActivity extends AppCompatActivity {
    private PairActivityViewModel viewModel;
    private BroadcastReceiver mReceiver;
    private TextView wifiTextView;
    private TextView locationTextView;
    private LinearLayout wifiLocationStatusLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pair);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        wifiTextView = findViewById(R.id.pairWifiOffTextView);
        locationTextView = findViewById(R.id.pairLocationOffTextView);
        wifiLocationStatusLayout = findViewById(R.id.pairWifiLocationStatusLayout);
        this.viewModel = new ViewModelProvider(this).get(PairActivityViewModel.class);
        this.mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                WifiLocationStatus status = viewModel.getWifiLocationStatus().getValue();
                if (status == null) {
                    status = new WifiLocationStatus(null, null);
                }
                if(intent.getAction().equals(NETWORK_STATE_CHANGED_ACTION) || intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                    status.setWifiEnabledOrEnabling(AndroidUtils.isWifiEnabled(PairActivity.this));
                    viewModel.setWifiLocationStatus(status);
                } else if (intent.getAction().equals(PROVIDERS_CHANGED_ACTION)) {
                    status.setLocationEnabledOrEnabling(AndroidUtils.isLocationEnabled(PairActivity.this));
                    viewModel.setWifiLocationStatus(status);
                }
                updateWifiLocationStatusBar(status.getWifiEnabledOrEnabling(), status.getLocationEnabledOrEnabling());
            }
        };

        boolean wifiOk = AndroidUtils.isWifiEnabled(this);
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

        // Update wifi status bar
        boolean wifiEnabled = AndroidUtils.isWifiEnabled(PairActivity.this);
        boolean locationEnabled = AndroidUtils.isLocationEnabled(PairActivity.this);
        updateWifiLocationStatusBar(wifiEnabled, locationEnabled);
    }

    private void updateWifiLocationStatusBar(@Nullable Boolean wifiEnabled, @Nullable Boolean locationEnabled) {
        if (wifiEnabled != null)
            wifiTextView.setVisibility(wifiEnabled ? View.GONE : View.VISIBLE);
        if (locationEnabled!=null)
            locationTextView.setVisibility(locationEnabled ? View.GONE : View.VISIBLE);
        wifiLocationStatusLayout.setVisibility(wifiTextView.getVisibility()==View.VISIBLE || locationTextView.getVisibility()==View.VISIBLE ? View.VISIBLE : View.GONE);
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
}
