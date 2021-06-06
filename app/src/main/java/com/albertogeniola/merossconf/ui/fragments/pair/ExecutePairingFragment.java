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
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.albertogeniola.merossconf.AndroidPreferencesManager;
import com.albertogeniola.merossconf.AndroidUtils;
import com.albertogeniola.merossconf.R;
import com.albertogeniola.merossconf.model.MqttConfiguration;
import com.albertogeniola.merossconf.ssl.DummyTrustManager;
import com.albertogeniola.merossconf.ui.PairActivityViewModel;
import com.albertogeniola.merossconf.ui.views.TaskLine;
import com.albertogeniola.merosslib.MerossDeviceAp;
import com.albertogeniola.merosslib.model.Cipher;
import com.albertogeniola.merosslib.model.Encryption;
import com.albertogeniola.merosslib.model.http.ApiCredentials;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;


public class ExecutePairingFragment extends Fragment {
    private static final String TAG = "PairingFragment";

    private WifiManager mWifiManager;
    private ConnectivityManager mConnectivityManager;
    private ScheduledExecutorService worker;

    private PairActivityViewModel pairActivityViewModel;
    private TextView errorDetailsTextView;
    private TaskLine connectWifiTaskLine, sendPairCommandTaskLine, connectLocalWifiTaskLine, testMqttBrokerTaskLine, currentTask;
    private Handler uiThreadHandler;

    private State state = State.INIT;
    private String error = null;
    private WifiBroadcastReceiver mReceiver;

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
                if (signal == Signal.LOCAL_WIFI_CONNECTED) {connectToMqttBorker();}
                break;
            case CONNECTING_TO_MQTT_BROKER:
                if (signal == Signal.MQTT_CONNECTED) {completeActivityFragment();}
                break;
        }

        updateUi();
    }

    private void registerForWifiChanges() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        requireContext().getApplicationContext().registerReceiver(mReceiver, intentFilter);
    }

    private void unregisterWifiChanges() {
        requireContext().getApplicationContext().unregisterReceiver(mReceiver);
    }

    private void connectToDeviceWifiAp() {
        state = State.CONNECTING_DEVICE_WIFI_AP;

        String ssid = pairActivityViewModel.getMerossPairingAp().getValue().getSsid();
        String targetSsid = "\""+ssid+"\"";
        String bssid = pairActivityViewModel.getMerossPairingAp().getValue().getBssid();

        // Check if we are already connected to such wifi
        WifiInfo connectedWifi = mWifiManager.getConnectionInfo();
        if (connectedWifi == null ||
                connectedWifi.getSSID() == null ||
                connectedWifi.getSSID().compareTo(targetSsid)!=0 ||
                connectedWifi.getBSSID().compareTo(bssid)!=0)
            connectToKnownWifi(ssid, bssid);
        else
            stateMachine(Signal.DEVICE_WIFI_CONNECTED);
    }

    private void connectToLocalWifi() {
        state = State.CONNETING_LOCAL_WIFI;

        String ssid = pairActivityViewModel.getMerossConfiguredWifi().getValue().getScannedWifi().getSsid();
        String targetSsid = "\""+ssid+"\"";
        String bssid = pairActivityViewModel.getMerossConfiguredWifi().getValue().getScannedWifi().getBssid();

        // Check if we are already connected to such wifi
        WifiInfo connectedWifi = mWifiManager.getConnectionInfo();
        if (connectedWifi == null ||
                connectedWifi.getSSID() == null ||
                connectedWifi.getSSID().compareTo(targetSsid)!=0 ||
                connectedWifi.getBSSID().compareTo(bssid)!=0)
            connectToKnownWifi(ssid, bssid);
        else
            stateMachine(Signal.LOCAL_WIFI_CONNECTED);
    }

    private void connectToKnownWifi(String ssid, String bssid) {
        bssid = bssid.replace("-", ":");
        // Check Wifi permissions
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                (getContext().checkSelfPermission(Manifest.permission.CHANGE_WIFI_STATE) != PackageManager.PERMISSION_GRANTED)){
            error = "User denied CHANGE_WIFI_STATE permission. Wifi cannot be enabled.";
            stateMachine(ExecutePairingFragment.Signal.ERROR);
            return;
        }

        // In case the device is "old", we rely on the classic WifiManager API
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            WifiConfiguration targetWifiConf = null;

            String targetSsid = "\"" + ssid +"\"";

            // Locate the existing network configuration entry.
            // We assume it is existing: the user either created it during the discovery session
            // or when it has logged in to the HTTP API
            for (WifiConfiguration conf : mWifiManager.getConfiguredNetworks()) {
                if (conf.BSSID.toLowerCase().compareTo(bssid.toLowerCase())==0
                    && conf.SSID.compareTo(targetSsid)==0) {

                    // Found a matching configuration.
                    targetWifiConf = conf;
                    break;
                }
            }

            if (targetWifiConf == null) {
                error = "Could not find a known network named '"+ssid+"' with bssid '"+bssid+"'.";
                stateMachine(ExecutePairingFragment.Signal.ERROR);
                return;
            } else {
                Log.i(TAG, "Issuing wifi connection against network "+targetWifiConf);
                mWifiManager.disconnect();
                mWifiManager.enableNetwork(targetWifiConf.networkId, true);
                mWifiManager.reconnect();
            }
        }

        // If the device is recent, we rely on Network Request API
        else {
            NetworkRequest networkRequest = new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) // The wifi does not necessarily need Internet Connection
                    .setNetworkSpecifier(
                        new WifiNetworkSpecifier.Builder()
                                .setSsid(ssid)
                                .setBssid(MacAddress.fromString(bssid))
                                .build()
                    )
                    .build();
            mConnectivityManager.requestNetwork(networkRequest, new ConnectivityManager.NetworkCallback() {
                @Override
                public void onUnavailable() {
                    error = "Wifi network unavailable";
                    stateMachine(Signal.ERROR);
                    Log.i(TAG, "Network unavailable");
                }
                @Override
                public void onAvailable(Network network) {
                    Log.i(TAG, "Found network " + network);
                    mApDevice.setSocketFactory(network.getSocketFactory());
                }
            });
        }
    }

    private void connectToMqttBorker() {
        state = State.CONNECTING_TO_MQTT_BROKER;
        String uri = "ssl://" + pairActivityViewModel.getTargetMqttConfig().getValue().getHostname() + ":" + pairActivityViewModel.getTargetMqttConfig().getValue().getPort();
        MqttAndroidClient mqttAndroidClient = new MqttAndroidClient(
                requireContext().getApplicationContext(),
                uri,
                "app:check"); // TODO: Change this
        MqttConnectOptions options = new MqttConnectOptions();
        String fake_mac = "00:00:00:00:00:00";
        String userId = mCreds.getUserId();
        String key = mCreds.getKey();
        options.setUserName(fake_mac);
        String password = calculateMQttPassword(userId, fake_mac, key);
        options.setPassword(password.toCharArray());
        options.setAutomaticReconnect(false);
        options.setCleanSession(false);
        options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
        options.setServerURIs(new String[] {uri});

        // Disable SSL checks...
        // TODO: parametrize this
        TrustManager[] trustManagers = new DummyTrustManager[]{new DummyTrustManager()};
        SSLContext sc = null;
        try {
            // FIXME: wait a bit so that the underlying network becomes available.
            Thread.sleep(3000);
            sc = SSLContext.getInstance ("SSL");
            sc.init (null, trustManagers, new java.security.SecureRandom ());
            options.setSocketFactory(sc.getSocketFactory());
        } catch (KeyManagementException | NoSuchAlgorithmException | InterruptedException e) {
            e.printStackTrace ();
            error = "Error occurred while connecting to remote broker";
            stateMachine(Signal.ERROR);
        }

        /* Establish an MQTT connection */
        try {
            mqttAndroidClient.connect(options, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    try {
                        asyncActionToken.getClient().disconnect();
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }

                    stateMachine(Signal.MQTT_CONNECTED);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    error = "Failed MQTT Connection: " + exception.getMessage();
                    stateMachine(Signal.ERROR);
                }
            });

        } catch (MqttException e) {
            e.printStackTrace();
            error = "Failed MQTT Connection: " + e.getMessage();
            stateMachine(Signal.ERROR);
        }

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
        ctrl.popBackStack(R.id.ScanDeviceFragment, false);
        ctrl.navigate(R.id.PairCompletedFragment);
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
                    case CONNECTING_TO_MQTT_BROKER:
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
        mWifiManager = (WifiManager) requireContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mConnectivityManager = (ConnectivityManager) requireContext().getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        mReceiver = new WifiBroadcastReceiver();
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
    }

    @Override
    public void onResume() {
        super.onResume();
        mCreds = AndroidPreferencesManager.loadHttpCredentials(requireContext());
        mApDevice = pairActivityViewModel.getDevice().getValue();

        // Register for Wifi changes
        registerForWifiChanges();
        // As soon as we resume, connect to the given WiFi
        stateMachine(Signal.RESUMED);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterWifiChanges();
    }

    enum State {
        INIT,
        CONNECTING_DEVICE_WIFI_AP,
        SENDING_PAIRING_COMMAND,
        CONNETING_LOCAL_WIFI,
        CONNECTING_TO_MQTT_BROKER,
        DONE,
        ERROR
    }

    enum Signal {
        RESUMED,
        DEVICE_WIFI_CONNECTED,
        DEVICE_CONFIGURED,
        LOCAL_WIFI_CONNECTED,
        MQTT_CONNECTED,
        ERROR
    }

    class WifiBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiManager.NETWORK_STATE_CHANGED_ACTION .equals(action)) {
                NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (networkInfo.isConnected()) {
                    String currentSsid = mWifiManager.getConnectionInfo().getSSID();
                    if (mWifiManager.getConnectionInfo() != null && currentSsid != null) {
                        String targetLocalSSID = "\"" + pairActivityViewModel.getMerossConfiguredWifi().getValue().getScannedWifi().getSsid() + "\"" ;
                        String targetApSSID = "\"" + pairActivityViewModel.getMerossPairingAp().getValue().getSsid() + "\"" ;
                        if (targetLocalSSID.compareTo(currentSsid) == 0) {
                            stateMachine(Signal.LOCAL_WIFI_CONNECTED);
                        } else if (targetApSSID.compareTo(currentSsid) == 0) {
                            stateMachine(Signal.DEVICE_WIFI_CONNECTED);
                        }
                    }
                }
            }
        }
    }
}
