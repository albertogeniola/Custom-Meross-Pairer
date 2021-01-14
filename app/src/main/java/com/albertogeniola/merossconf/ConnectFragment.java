package com.albertogeniola.merossconf;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.MacAddress;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.albertogeniola.merosslib.MerossDeviceAp;
import com.albertogeniola.merosslib.model.protocol.MessageGetConfigWifiListResponse;
import com.albertogeniola.merosslib.model.protocol.MessageGetSystemAll;
import com.albertogeniola.merosslib.model.protocol.MessageGetSystemAllResponse;
import com.google.android.material.snackbar.Snackbar;
import org.apache.commons.io.IOUtils;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class ConnectFragment extends Fragment {

    private static final int WIFI_STATE_CHANGE_PERMISSION = 1;

    private ImageSwitcher imageSwitcher;
    private TextView wifiEnableTextView;
    private TextView wifiConnectTextView;
    private TextView fetchDeviceInfoTextView;
    private TextView scanWifiTextView;
    private int animationCounter = 0;
    private String targetSSID;
    private String targetBSSID;
    private WifiManager wifiManager;
    private ConnectivityManager connectivityManager;
    private WifiBroadcastReceiver wifiBroadcastReceiver;
    private Handler uiThreadHandler;
    private ScheduledExecutorService worker;

    private boolean animateWifi = false;
    private State state = State.INIT;
    private String gatewayIp = null;
    private String error = null;
    private MerossDeviceAp device = new MerossDeviceAp();
    private MessageGetSystemAllResponse deviceInfo;
    private MessageGetConfigWifiListResponse deviceAvailableWifis;

    public ConnectFragment() {
        worker = Executors.newSingleThreadScheduledExecutor();
    }

    // Logic methods
    private void stateMachine(Signal signal) {
        switch(state) {
            case INIT:
                if (signal == Signal.RESUMED) {
                    state = State.ENABLING_WIFI;
                    updateUi();
                    enableWifi();
                }
                break;
            case ENABLING_WIFI:
                if (signal == Signal.WIFI_ENABLED) {
                    state = State.CONNECTING_AP;
                    updateUi();
                    connectAp();
                }
                break;
            case CONNECTING_AP:
                if (signal == Signal.AP_CONNECTED) {
                    state = State.GATHERING_INFORMATION;
                    updateUi();
                    collectDeviceInfo(gatewayIp);
                }
                break;
            case GATHERING_INFORMATION:
                if (signal == Signal.INFO_GATHERED) {
                    state = State.SCANNING_WIFI;
                    updateUi();
                    startDeviceWifiScan();
                }
                break;
            case SCANNING_WIFI:
                if (signal == Signal.WIFI_SCAN_COMPLETED) {
                    state = State.DONE;
                    updateUi();
                    completeActivityFragment();
                }
                break;
        }

        if (signal == Signal.ERROR) {
            state = State.ERROR;
            updateUi();
        }
    }

    private void enableWifi() {
        wifiManager = (WifiManager) getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        connectivityManager = (ConnectivityManager) getContext().getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        this.targetSSID = getArguments().getString("SSID");
        this.targetBSSID = getArguments().getString("BSSID");

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && (getContext().checkSelfPermission(Manifest.permission.CHANGE_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED)) {
            requestPermissions(new String[]{Manifest.permission.CHANGE_NETWORK_STATE},
                    WIFI_STATE_CHANGE_PERMISSION);
        } else {
            wifiManager.setWifiEnabled(true);
            stateMachine(Signal.WIFI_ENABLED);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        if (requestCode == WIFI_STATE_CHANGE_PERMISSION && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            stateMachine(Signal.WIFI_ENABLED);
        } else if (requestCode == WIFI_STATE_CHANGE_PERMISSION && grantResults[0] == PackageManager.PERMISSION_DENIED) {
            error = "Wifi permission has not been granted.";
            stateMachine(Signal.ERROR);
        }
    }

    private void connectAp() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            WifiConfiguration conf = new WifiConfiguration();
            conf.SSID = "\"" + targetSSID + "\"";
            conf.BSSID = "\"" + targetBSSID + "\"";
            conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            wifiManager.addNetwork(conf);
            List<WifiConfiguration> list = null;

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    (getContext().checkSelfPermission(Manifest.permission.CHANGE_WIFI_STATE) != PackageManager.PERMISSION_GRANTED)){
                error = "User denied CHANGE_WIFI_STATE permission. Wifi cannot be enabled.";
                stateMachine(Signal.ERROR);
                return;
            } else {
                list = wifiManager.getConfiguredNetworks();
            }

            for (WifiConfiguration i : list) {
                if (i.SSID != null && i.SSID.equals("\"" + targetSSID + "\"")) {
                    wifiManager.disconnect();
                    wifiManager.enableNetwork(i.networkId, true);
                    wifiManager.reconnect();
                    break;
                }
            }
        } else {
            NetworkRequest networkRequest = new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .setNetworkSpecifier(
                            new WifiNetworkSpecifier.Builder()
                                    .setSsid(targetSSID)
                                    .setBssid(MacAddress.fromString(targetBSSID))
                                    .build()
                    )
                    .build();
            connectivityManager.requestNetwork(networkRequest, new ConnectivityManager.NetworkCallback() {

                @Override
                public void onUnavailable() {
                    Log.d("TEST", "Network unavailable");
                    // TODO
                }
                @Override
                public void onAvailable(Network network) {
                    device.setSocketFactory(network.getSocketFactory());
                }
            });
        }
    }

    private void startDeviceWifiScan() {
        try {
            deviceAvailableWifis = device.scanWifi();
            stateMachine(Signal.WIFI_SCAN_COMPLETED);
        } catch (IOException e) {
            e.printStackTrace();
            uiThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    error = "Error occurred while performing device wifi scanning";
                    stateMachine(Signal.ERROR);
                }
            });
        }
    }

    private void collectDeviceInfo(String deviceIp) {
        worker.schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    deviceInfo = device.getConfig();
                    stateMachine(Signal.INFO_GATHERED);
                } catch (IOException e) {
                    e.printStackTrace();
                    uiThreadHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            error = "Error occurred while gathering device info";
                            stateMachine(Signal.ERROR);
                        }
                    });
                }
            }
        },10, TimeUnit.SECONDS);
    }

    private void completeActivityFragment() {
        // Set done and proceed with the next fragment
        Bundle bundle = new Bundle();
        bundle.putSerializable(InfoFragment.DEVICE, this.device);
        bundle.putSerializable(InfoFragment.DEVICE_INFO, this.deviceInfo);
        bundle.putSerializable(InfoFragment.DEVICE_AVAILABLE_WIFIS, this.deviceAvailableWifis);
        NavController ctrl = NavHostFragment.findNavController(ConnectFragment.this);
        ctrl.popBackStack();
        ctrl.navigate(R.id.InfoFragment, bundle);
    }

    // UI
    private void updateUi() {
        Runnable uiUpdater = new Runnable() {
            @Override
            public void run() {
                switch (state) {
                    case INIT:
                        animateWifi = false;
                        wifiEnableTextView.setVisibility(View.INVISIBLE);
                        wifiConnectTextView.setVisibility(View.INVISIBLE);
                        fetchDeviceInfoTextView.setVisibility(View.INVISIBLE);
                        scanWifiTextView.setVisibility(View.INVISIBLE);
                        break;
                    case ENABLING_WIFI:
                        animateWifi = true;
                        startWifiAnimation();
                        wifiEnableTextView.setVisibility(View.VISIBLE);
                        break;
                    case CONNECTING_AP:
                        wifiConnectTextView.setVisibility(View.VISIBLE);
                        break;
                    case GATHERING_INFORMATION:
                        fetchDeviceInfoTextView.setVisibility(View.VISIBLE);
                        break;
                    case SCANNING_WIFI:
                        scanWifiTextView.setVisibility(View.VISIBLE);
                        break;
                    case DONE:
                        animateWifi = false;
                        break;
                    case ERROR:
                        animateWifi = false;
                        imageSwitcher.setImageResource(R.drawable.ic_error_outline_black_24dp);
                        Snackbar.make(ConnectFragment.this.getView(), error, Snackbar.LENGTH_LONG).show();
                        break;
                }
            }
        };

        if (Looper.getMainLooper().getThread().getId() != Thread.currentThread().getId()) {
            uiThreadHandler.post(uiUpdater);
        } else {
            uiUpdater.run();
        }
    }

    private void setupAnimation() {
        imageSwitcher = getView().findViewById(R.id.wifi_animation);
        imageSwitcher.setFactory(new ViewSwitcher.ViewFactory() {
            @Override
            public View makeView() {
                ImageView im = new ImageView(ConnectFragment.this.getActivity());
                im.setScaleType(ImageView.ScaleType.FIT_CENTER);
                im.setAdjustViewBounds(true);
                return im;
            }
        });
        Animation in  = AnimationUtils.loadAnimation(this.getContext(), R.anim.fade_in);
        Animation out  = AnimationUtils.loadAnimation(this.getContext(), R.anim.fade_out);
        imageSwitcher.setInAnimation(in);
        imageSwitcher.setOutAnimation(out);
    }

    private void startWifiAnimation() {
        // Configure the animation
        uiThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (animateWifi) {
                    switch (animationCounter++) {
                        case 0:
                            imageSwitcher.setImageResource(R.drawable.ic_signal_wifi_0_bar_black_24dp);
                            break;
                        case 1:
                            imageSwitcher.setImageResource(R.drawable.ic_signal_wifi_1_bar_black_24dp);
                            break;
                        case 2:
                            imageSwitcher.setImageResource(R.drawable.ic_signal_wifi_2_bar_black_24dp);
                            break;
                        case 3:
                            imageSwitcher.setImageResource(R.drawable.ic_signal_wifi_3_bar_black_24dp);
                            break;
                        case 4:
                            imageSwitcher.setImageResource(R.drawable.ic_signal_wifi_4_bar_black_24dp);
                            break;
                    }
                    animationCounter %= 5;
                    uiThreadHandler.postDelayed(this, 2000);
                }
            }
        });
    }

    // Android activity lifecycle
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        wifiBroadcastReceiver = new WifiBroadcastReceiver();
        uiThreadHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void onPause() {
        super.onPause();
        getContext().unregisterReceiver(wifiBroadcastReceiver);
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.connect_fragment, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        wifiEnableTextView = view.findViewById(R.id.enable_wifi);
        wifiConnectTextView = view.findViewById(R.id.configure_wifi);
        fetchDeviceInfoTextView = view.findViewById(R.id.fetch_device_info);
        scanWifiTextView = view.findViewById(R.id.scan_wifis);
        setupAnimation();
    }

    @Override
    public void onResume() {
        super.onResume();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION );
        getContext().registerReceiver(wifiBroadcastReceiver, intentFilter);

        // As soon as we resume, connect to the given WiFi
        stateMachine(Signal.RESUMED);
    }

    class WifiBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiManager.NETWORK_STATE_CHANGED_ACTION .equals(action)) {
                NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (networkInfo.isConnected()) {
                    if (wifiManager.getConnectionInfo() != null &&
                            wifiManager.getConnectionInfo().getBSSID() != null &&
                            wifiManager.getConnectionInfo().getBSSID().compareTo(targetBSSID) == 0) {
                        // Connected!
                        int tmp = wifiManager.getDhcpInfo().gateway;
                        gatewayIp = String.format("%d.%d.%d.%d", (tmp & 0xff), (tmp >> 8 & 0xff), (tmp >> 16 & 0xff), (tmp >> 24 & 0xff));
                        stateMachine(Signal.AP_CONNECTED);
                    }
                }
            }
        }
    }

    enum State {
        INIT,
        ENABLING_WIFI,
        CONNECTING_AP,
        GATHERING_INFORMATION,
        SCANNING_WIFI,
        DONE,
        ERROR
    }

    enum Signal {
        RESUMED,
        WIFI_ENABLED,
        AP_CONNECTED,
        INFO_GATHERED,
        WIFI_SCAN_COMPLETED,
        ERROR
    }
}
