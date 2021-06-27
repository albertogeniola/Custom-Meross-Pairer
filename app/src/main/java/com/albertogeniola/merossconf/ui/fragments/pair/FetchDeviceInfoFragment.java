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
import android.view.WindowManager;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.albertogeniola.merossconf.R;
import com.albertogeniola.merossconf.model.exception.PermissionNotGrantedException;
import com.albertogeniola.merossconf.ui.PairActivityViewModel;
import com.albertogeniola.merossconf.ui.views.TaskLine;
import com.albertogeniola.merosslib.MerossDeviceAp;
import com.albertogeniola.merosslib.model.protocol.MessageGetConfigWifiListResponse;
import com.albertogeniola.merosslib.model.protocol.MessageGetSystemAllResponse;
import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import lombok.SneakyThrows;


public class FetchDeviceInfoFragment extends AbstractWifiFragment {

    private static final int WIFI_STATE_CHANGE_PERMISSION = 1;
    private static final String TAG = "FetchDeviceInfoFragment";
    private static final int CONNECT_AP_TIMEOUT = 20000;

    private TaskLine wifiConnectTask;
    private TaskLine fetchDeviceInfoTask;
    private TaskLine scanWifiTask;
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

    private void connectAp() {
        String ssid = pairActivityViewModel.getMerossPairingAp().getValue().getSsid();
        try {
            startWifiConnection(ssid, null, null, CONNECT_AP_TIMEOUT);
            // The flow starts back from on onWifiConnected / onWifiUnavailable().
        } catch (PermissionNotGrantedException e) {
            Log.w(TAG, "Missing user permissions.");
            // The flow starts back from permissions acquired callback
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
                ctrl.navigate(R.id.action_fetchDeviceInfo_to_showDeviceInfo,null, new NavOptions.Builder().setEnterAnim(android.R.animator.fade_in).setExitAnim(android.R.animator.fade_out).build());
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
                        wifiConnectTask.setState(TaskLine.TaskState.not_started);
                        fetchDeviceInfoTask.setState(TaskLine.TaskState.not_started);
                        scanWifiTask.setState(TaskLine.TaskState.not_started);
                        break;
                    case CONNECTING_AP:
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
        uiThreadHandler = new Handler(Looper.getMainLooper());
        //requireActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //requireActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onWifiConnected(String ssid) {
        stateMachine(Signal.AP_CONNECTED);
    }

    @Override
    protected void onWifiUnavailable(String ssid) {
        stateMachine(Signal.ERROR);
    }

    @Override
    protected void onMissingWifiPermissions(String ssid) {
        stateMachine(Signal.ERROR);
    }

    @SneakyThrows(PermissionNotGrantedException.class)
    @Override
    protected void onWifiPermissionsGranted(String ssid) {
        startWifiConnection(ssid, null, null, 10000);
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
        wifiConnectTask = view.findViewById(R.id.connectWifiTask);
        fetchDeviceInfoTask = view.findViewById(R.id.fetchDeviceInfoTask);
        scanWifiTask = view.findViewById(R.id.scanWifiTask);
    }

    @Override
    public void onResume() {
        super.onResume();

        // As soon as we resume, connect to the given WiFi
        stateMachine(Signal.RESUMED);
    }

    enum State {
        INIT,
        CONNECTING_AP,
        GATHERING_INFORMATION,
        SCANNING_WIFI,
        DONE,
        ERROR
    }

    enum Signal {
        RESUMED,
        AP_CONNECTED,
        INFO_GATHERED,
        WIFI_SCAN_COMPLETED,
        ERROR
    }
}
