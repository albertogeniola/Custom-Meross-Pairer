package com.albertogeniola.merossconf;

import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.albertogeniola.merosslib.MerossDeviceAp;
import com.albertogeniola.merosslib.model.protocol.MessageGetConfigWifiListResponse;
import com.albertogeniola.merosslib.model.protocol.MessageGetSystemAllResponse;

import lombok.Getter;
import lombok.Setter;


public class PairActivity extends AppCompatActivity implements ProgressableActivity {
    private ProgressBar progressBar;

    private WifiManager wifiManager = null;
    private LocationManager locationManager = null;

    @Getter
    @Setter
    private MerossDeviceAp device;

    @Getter
    @Setter
    private String deviceApSSID = null;

    @Getter
    @Setter
    private String deviceApBSSID = null;

    @Getter
    @Setter
    private String targetWifiSSID = null;

    @Getter
    @Setter
    private String targetWifiPassword = null;

    @Getter
    @Setter
    private String targetMqttHostname = null;

    @Getter
    @Setter
    private int targetMqttPort;

    @Getter
    @Setter
    private MessageGetSystemAllResponse deviceInfo;

    @Getter
    @Setter
    private MessageGetConfigWifiListResponse deviceAvailableWifis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pair);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        this.progressBar = findViewById(R.id.progressBar);
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
