package com.albertogeniola.merossconf.ui.fragments.pair;

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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.albertogeniola.merossconf.R;
import com.albertogeniola.merossconf.ui.PairActivityViewModel;
import com.albertogeniola.merossconf.ui.views.TaskLine;
import com.albertogeniola.merosslib.MerossDeviceAp;
import com.albertogeniola.merosslib.model.protocol.MessageGetConfigWifiListResponse;
import com.albertogeniola.merosslib.model.protocol.MessageGetSystemAllResponse;
import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class FetchDeviceInfoFragment extends Fragment {

    private static final int WIFI_STATE_CHANGE_PERMISSION = 1;

    private TaskLine enablingWifiTask;
    private TaskLine wifiConnectTask;
    private TaskLine fetchDeviceInfoTask;
    private TaskLine scanWifiTask;
    private WifiManager wifiManager;
    private ConnectivityManager connectivityManager;
    private WifiBroadcastReceiver wifiBroadcastReceiver;
    private Handler uiThreadHandler;
    private ScheduledExecutorService worker;

    private State state = State.INIT;
    private TaskLine currentTask = null;
    private String gatewayIp = null;
    private String error = null;
    private MerossDeviceAp device = new MerossDeviceAp();
    private MessageGetSystemAllResponse deviceInfo;
    private MessageGetConfigWifiListResponse deviceAvailableWifis;

    private PairActivityViewModel pairActivityViewModel;

    public FetchDeviceInfoFragment() {
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
        String ssid = pairActivityViewModel.getMerossPairingAp().getValue().getSsid();
        String bssid = pairActivityViewModel.getMerossPairingAp().getValue().getBssid();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            WifiConfiguration conf = new WifiConfiguration();
            conf.SSID = "\"" + ssid + "\"";
            conf.BSSID = "\"" + bssid + "\"";
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
                if (i.SSID != null && i.SSID.equals("\"" + ssid + "\"")) {
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
                                    .setSsid(ssid)
                                    .setBssid(MacAddress.fromString(bssid))
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
        uiThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                // Set done and proceed with the next fragment
                pairActivityViewModel.setApDevice(device);
                pairActivityViewModel.setDeviceInfo(deviceInfo);
                pairActivityViewModel.setDeviceAvailableWifis(deviceAvailableWifis);
                NavController ctrl = NavHostFragment.findNavController(FetchDeviceInfoFragment.this);
                ctrl.popBackStack();
                ctrl.navigate(R.id.ShowDeviceInfoFragment);
            }
        });
    }

    // UI
    private void updateUi() {
        Runnable uiUpdater = new Runnable() {
            @Override
            public void run() {
                switch (state) {
                    case INIT:
                        enablingWifiTask.setState(TaskLine.TaskState.not_started);
                        wifiConnectTask.setState(TaskLine.TaskState.not_started);
                        fetchDeviceInfoTask.setState(TaskLine.TaskState.not_started);
                        scanWifiTask.setState(TaskLine.TaskState.not_started);
                        break;
                    case ENABLING_WIFI:
                        enablingWifiTask.setState(TaskLine.TaskState.running);
                        currentTask = enablingWifiTask;
                        break;
                    case CONNECTING_AP:
                        enablingWifiTask.setState(TaskLine.TaskState.completed);
                        wifiConnectTask.setState(TaskLine.TaskState.running);
                        currentTask = wifiConnectTask;
                        break;
                    case GATHERING_INFORMATION:
                        wifiConnectTask.setState(TaskLine.TaskState.completed);
                        fetchDeviceInfoTask.setState(TaskLine.TaskState.running);
                        currentTask = fetchDeviceInfoTask;
                        break;
                    case SCANNING_WIFI:
                        fetchDeviceInfoTask.setState(TaskLine.TaskState.completed);
                        scanWifiTask.setState(TaskLine.TaskState.running);
                        currentTask = scanWifiTask;
                        break;
                    case DONE:
                        scanWifiTask.setState(TaskLine.TaskState.completed);
                        break;
                    case ERROR:
                        Snackbar.make(FetchDeviceInfoFragment.this.getView(), error, Snackbar.LENGTH_LONG).show();
                        if (currentTask != null) {
                            currentTask.setState(TaskLine.TaskState.failed);
                        }
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

    // Android activity lifecycle
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pairActivityViewModel = new ViewModelProvider(requireActivity()).get(PairActivityViewModel.class);
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
        return inflater.inflate(R.layout.fragment_connect, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        enablingWifiTask = view.findViewById(R.id.enablingWifiTask);
        wifiConnectTask = view.findViewById(R.id.connectWifiTask);
        fetchDeviceInfoTask = view.findViewById(R.id.fetchDeviceInfoTask);
        scanWifiTask = view.findViewById(R.id.scanWifiTask);
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
                            wifiManager.getConnectionInfo().getBSSID().compareTo(pairActivityViewModel.getMerossPairingAp().getValue().getBssid()) == 0) {
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
