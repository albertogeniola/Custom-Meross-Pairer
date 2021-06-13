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
import android.net.wifi.WifiInfo;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.albertogeniola.merossconf.AndroidPreferencesManager;
import com.albertogeniola.merossconf.R;
import com.albertogeniola.merossconf.model.MqttConfiguration;
import com.albertogeniola.merossconf.model.exception.PermissionNotGrantedException;
import com.albertogeniola.merossconf.ui.PairActivityViewModel;
import com.albertogeniola.merossconf.ui.views.TaskLine;
import com.albertogeniola.merosslib.MerossDeviceAp;
import com.albertogeniola.merosslib.MerossHttpClient;
import com.albertogeniola.merosslib.model.OnlineStatus;
import com.albertogeniola.merosslib.model.http.ApiCredentials;
import com.albertogeniola.merosslib.model.http.DeviceInfo;
import com.albertogeniola.merosslib.model.http.exceptions.HttpApiException;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.net.SocketFactory;


public class ExecutePairingFragment extends AbstractWifiFragment {
    private static final String TAG = "PairingFragment";

    private ScheduledExecutorService worker;

    private PairActivityViewModel pairActivityViewModel;
    private TextView errorDetailsTextView;
    private TaskLine connectWifiTaskLine, sendPairCommandTaskLine, connectLocalWifiTaskLine, testMqttBrokerTaskLine, currentTask;
    private Handler uiThreadHandler;

    private State state = State.INIT;
    private String error = null;

    private MerossDeviceAp mApDevice;
    private ApiCredentials mCreds;

    public ExecutePairingFragment() {
        worker = Executors.newSingleThreadScheduledExecutor();
    }

    // Logic methods
    private void stateMachine(Signal signal) {
        updateUi();

        if (signal == Signal.ERROR) {
            state = State.ERROR;
            updateUi();
            return;
        }

        switch(state) {
            case INIT:
                if (signal == Signal.RESUMED) {connectToDeviceWifiAp();}
                break;
            case CONNECTING_DEVICE_WIFI_AP:
                if (signal == Signal.DEVICE_WIFI_CONNECTED) {configureDevice(mCreds.getUserId(), mCreds.getKey());}
                break;
            case SENDING_PAIRING_COMMAND:
                if (signal == Signal.DEVICE_CONFIGURED) {connectToLocalWifi();}
                break;
            case CONNETING_LOCAL_WIFI:
                if (signal == Signal.LOCAL_WIFI_CONNECTED) { pollDeviceList(); }
                break;
            case VERIFYING_PAIRING_SUCCEEDED:
                if (signal == Signal.DEVICE_PAIRED) {completeActivityFragment();}
                break;
        }

        updateUi();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //requireActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void connectToDeviceWifiAp() {
        state = State.CONNECTING_DEVICE_WIFI_AP;

        String ssid = pairActivityViewModel.getMerossPairingAp().getValue().getSsid();
        String bssid = pairActivityViewModel.getMerossPairingAp().getValue().getBssid();

        try {
            startWifiConnection(ssid, bssid, null, null, 15000);
            // Flow starts again from onWifiConnected() / onWifiUnavailable()
        } catch (PermissionNotGrantedException e) {
            Log.e(TAG, "Missing wifi permissions");
            // DO nothing, as the parent fragment will ask for permissions already.
            // Flow starts back from onWifiPermissionsGranted()
        }
    }

    private void connectToLocalWifi() {
        state = State.CONNETING_LOCAL_WIFI;

        String ssid = pairActivityViewModel.getMerossConfiguredWifi().getValue().getScannedWifi().getSsid();
        String bssid = pairActivityViewModel.getMerossConfiguredWifi().getValue().getScannedWifi().getBssid();
        String passphrase = pairActivityViewModel.getMerossConfiguredWifi().getValue().getClearWifiPassword();

        // Check if we are already connected to such wifi
        // TODO: Check comparison happens with double quotes
        try {
            startWifiConnection(ssid, bssid, passphrase, null, 15000);
            // Flow starts again from onWifiConnected() / onWifiUnavailable()
        } catch (PermissionNotGrantedException e) {
            Log.e(TAG, "Missing wifi permissions");
            // Do nothing, as the parent fragment will ask for permissions already.
            // Flow starts back from onWifiPermissionsGranted()
        }
    }

    private void pollDeviceList() {
        state = State.VERIFYING_PAIRING_SUCCEEDED;

        final long timeout = GregorianCalendar.getInstance().getTimeInMillis() + 30000; // 30 seconds timeout
        ScheduledFuture<?> future = worker.schedule(new Runnable() {
            private @Nullable DeviceInfo findDevice(Collection<DeviceInfo> devices, String deviceUuid) {
                for (DeviceInfo d : devices) {
                    Log.d(TAG, "Device " + d.getUuid() + " has been found with status: " + d.getOnlineStatus());
                    if (d.getUuid().compareTo(deviceUuid) == 0) {
                        return d;
                    }
                }
                return null;
            }

            @Override
            public void run() {
                MerossHttpClient client = new MerossHttpClient(mCreds);
                String targetUuid = pairActivityViewModel.getDeviceInfo().getValue().getPayload().getAll().getSystem().getHardware().getUuid();
                boolean succeeed = false;
                boolean timedOut = GregorianCalendar.getInstance().getTimeInMillis() >= timeout;
                boolean exitNow = false;
                while(!exitNow && !succeeed && !Thread.currentThread().isInterrupted() && !timedOut) {
                    try {

                        List<DeviceInfo> devices = client.listDevices();
                        DeviceInfo d = findDevice(devices, targetUuid);
                        if (d == null) {
                            Log.i(TAG, "Device " +targetUuid + " not paired yet.");
                        } else if (d.getOnlineStatus() == OnlineStatus.ONLINE || d.getOnlineStatus() == OnlineStatus.LAN) {
                            Log.i(TAG, "Device " +targetUuid + " is online.");
                            succeeed = true;
                        } else {
                            Log.i(TAG, "Device " +targetUuid + " is paired, but not ready yet or in an unknown status.");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "An IOException occurred while polling the HTTP API server.", e);
                    } catch (HttpApiException e) {
                        Log.e(TAG, "The HTTP API server reported status " + e.getCode(), e);
                    } finally {
                        timedOut = GregorianCalendar.getInstance().getTimeInMillis() >= timeout;
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            exitNow = true;
                            error = "An error occurred while poling the HTTP API server";
                            stateMachine(Signal.ERROR);
                        }
                    }
                }


                final boolean finalSucceeed = succeeed;
                uiThreadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (finalSucceeed)
                            stateMachine(Signal.DEVICE_PAIRED);
                        else
                            stateMachine(Signal.ERROR);
                    }
                });
            }
        }, 2, TimeUnit.SECONDS);
    }

    private String calculateMQttPassword(String userId, String fake_mac, String key) {
        StringBuilder sb = new StringBuilder();
        String md5pwd = md5(fake_mac+key);
        sb.append(userId);
        sb.append("_");
        sb.append(md5pwd);
        return sb.toString();
    }

    public static String md5(String message) {
        String digest = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(message.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder(2*hash.length);
            for(byte b : hash){
                sb.append(String.format("%02x", b&0xff));
            } digest = sb.toString();
        } catch (Exception ex) {
            throw new RuntimeException("MD5 error");
        }

        return digest;
    }

    private void configureDevice(final String userId, final String key) {
        state = State.SENDING_PAIRING_COMMAND;

        worker.schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    LiveData<MqttConfiguration> mqttConfig = pairActivityViewModel.getTargetMqttConfig();
                    mApDevice.setConfigKey(
                            mqttConfig.getValue().getHostname(),
                            mqttConfig.getValue().getPort(),
                                    key,
                                    userId);
                } catch (IOException e) {
                    e.printStackTrace();
                    uiThreadHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            error = "Error occurred while configuring device MQTT server";
                            stateMachine(Signal.ERROR);
                        }
                    });
                    return;
                }

                com.albertogeniola.merossconf.model.WifiConfiguration credentials = pairActivityViewModel.getMerossConfiguredWifi().getValue();
                try {
                    mApDevice.setConfigWifi(credentials.getScannedWifi(), credentials.getWifiPasswordBase64());
                    stateMachine(Signal.DEVICE_CONFIGURED);
                } catch (IOException e) {
                    e.printStackTrace();
                    uiThreadHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            error = "Error occurred while configuring device WIFI connection";
                            stateMachine(Signal.ERROR);
                        }
                    });
                }

            }
        },3, TimeUnit.SECONDS);
    }

    private void completeActivityFragment() {
        state = State.DONE;
        NavController ctrl = NavHostFragment.findNavController(this);
        ctrl.navigate(R.id.action_executePair_to_pairCompleted,null, new NavOptions.Builder().setEnterAnim(android.R.animator.fade_in).setExitAnim(android.R.animator.fade_out).build());
    }

    // UI
    private void updateUi() {
        Runnable uiUpdater = new Runnable() {
            @Override
            public void run() {
                switch (state) {
                    case INIT:
                        errorDetailsTextView.setText("");
                        errorDetailsTextView.setVisibility(View.GONE);
                        connectWifiTaskLine.setState(TaskLine.TaskState.not_started);
                        sendPairCommandTaskLine.setState(TaskLine.TaskState.not_started);
                        connectLocalWifiTaskLine.setState(TaskLine.TaskState.not_started);
                        testMqttBrokerTaskLine.setState(TaskLine.TaskState.not_started);
                        currentTask = null;
                        break;
                    case CONNECTING_DEVICE_WIFI_AP:
                        errorDetailsTextView.setVisibility(View.GONE);
                        connectWifiTaskLine.setState(TaskLine.TaskState.running);
                        currentTask = connectWifiTaskLine;
                        break;
                    case SENDING_PAIRING_COMMAND:
                        errorDetailsTextView.setVisibility(View.GONE);
                        connectWifiTaskLine.setState(TaskLine.TaskState.completed);
                        sendPairCommandTaskLine.setState(TaskLine.TaskState.running);
                        currentTask = sendPairCommandTaskLine;
                        break;
                    case CONNETING_LOCAL_WIFI:
                        errorDetailsTextView.setVisibility(View.GONE);
                        sendPairCommandTaskLine.setState(TaskLine.TaskState.completed);
                        connectLocalWifiTaskLine.setState(TaskLine.TaskState.running);
                        currentTask = connectLocalWifiTaskLine;
                        break;
                    case VERIFYING_PAIRING_SUCCEEDED:
                        errorDetailsTextView.setVisibility(View.GONE);
                        connectLocalWifiTaskLine.setState(TaskLine.TaskState.completed);
                        testMqttBrokerTaskLine.setState(TaskLine.TaskState.running);
                        currentTask = testMqttBrokerTaskLine;
                        break;
                    case DONE:
                        errorDetailsTextView.setVisibility(View.GONE);
                        testMqttBrokerTaskLine.setState(TaskLine.TaskState.completed);
                        currentTask = null;
                        break;
                    case ERROR:
                        errorDetailsTextView.setText(error);
                        errorDetailsTextView.setVisibility(View.VISIBLE);
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
        uiThreadHandler = new Handler(Looper.getMainLooper());
        pairActivityViewModel = new ViewModelProvider(requireActivity()).get(PairActivityViewModel.class);
    }

    @Override
    protected void onWifiConnected(String ssid, String bssid) {
        String targetLocalSSID = pairActivityViewModel.getMerossConfiguredWifi().getValue().getScannedWifi().getSsid();
        String targetApSSID = pairActivityViewModel.getMerossPairingAp().getValue().getSsid() ;
        if (targetLocalSSID.compareTo(ssid) == 0) {
            stateMachine(Signal.LOCAL_WIFI_CONNECTED);
        } else if (targetApSSID.compareTo(ssid) == 0) {
            stateMachine(Signal.DEVICE_WIFI_CONNECTED);
        }
    }

    @Override
    protected void onWifiUnavailable(String ssid, String bssid) {
        error = "Failed to connect to " + ssid;
        stateMachine(Signal.ERROR);
    }

    @Override
    protected void onMissingWifiPermissions(String ssid, String bssid) {
        error = "You must provide Wifi and Location access to this app to complete the operation.";
        stateMachine(Signal.ERROR);
    }

    @Override
    protected void onWifiPermissionsGranted(String ssid, String bssid) {
        String pairingSsid = pairActivityViewModel.getMerossPairingAp().getValue().getSsid();
        String localSsid = pairActivityViewModel.getMerossConfiguredWifi().getValue().getScannedWifi().getSsid();

        if (ssid.compareTo(pairingSsid) == 0) {
            connectToDeviceWifiAp();
        } else if (ssid.compareTo(localSsid) == 0) {
            connectToLocalWifi();
        }
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_pair, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        connectWifiTaskLine = view.findViewById(R.id.connectApWifiTaskLine);
        sendPairCommandTaskLine = view.findViewById(R.id.sendPairCommandTaskLine);
        connectLocalWifiTaskLine = view.findViewById(R.id.connectToBrokerWifi);
        testMqttBrokerTaskLine  = view.findViewById(R.id.connectToMqttBrokerTaskLike);
        errorDetailsTextView = view.findViewById(R.id.errorDetailTextView);

        //requireActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void onResume() {
        super.onResume();
        mCreds = AndroidPreferencesManager.loadHttpCredentials(requireContext());
        mApDevice = pairActivityViewModel.getDevice().getValue();

        // As soon as we resume, connect to the given WiFi
        stateMachine(Signal.RESUMED);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    enum State {
        INIT,
        CONNECTING_DEVICE_WIFI_AP,
        SENDING_PAIRING_COMMAND,
        CONNETING_LOCAL_WIFI,
        VERIFYING_PAIRING_SUCCEEDED,
        DONE,
        ERROR
    }

    enum Signal {
        RESUMED,
        DEVICE_WIFI_CONNECTED,
        DEVICE_CONFIGURED,
        LOCAL_WIFI_CONNECTED,
        DEVICE_PAIRED,
        ERROR
    }
}
