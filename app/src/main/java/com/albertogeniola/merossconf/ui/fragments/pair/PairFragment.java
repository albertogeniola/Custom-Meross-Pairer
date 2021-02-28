package com.albertogeniola.merossconf.ui.fragments.pair;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
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
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.albertogeniola.merossconf.AndroidPreferencesManager;
import com.albertogeniola.merossconf.R;
import com.albertogeniola.merossconf.model.MqttConfiguration;
import com.albertogeniola.merossconf.model.TargetWifiAp;
import com.albertogeniola.merossconf.ssl.DummyTrustManager;
import com.albertogeniola.merossconf.ui.PairActivityViewModel;
import com.albertogeniola.merossconf.ui.fragments.connect.ConnectFragment;
import com.albertogeniola.merossconf.ui.views.TaskLine;
import com.albertogeniola.merosslib.MerossDeviceAp;
import com.albertogeniola.merosslib.model.http.ApiCredentials;
import com.google.android.material.snackbar.Snackbar;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;


public class PairFragment extends Fragment {
    private static final String DEFAULT_KEY = "";
    private static final String DEFAULT_USER_ID = "";

    private PairActivityViewModel pairActivityViewModel;
    private TaskLine sendPairCommandTaskLine, connectLocalWifiTaskLine, testMqttBrokerTaskLine, currentTask;
    private Handler uiThreadHandler;
    private ScheduledExecutorService worker;

    private State state = State.INIT;
    private String error = null;
    private WifiManager mWifiManager;
    private ConnectivityManager mConnectivityManager;
    private WifiBroadcastReceiver mReceiver;

    public PairFragment() {
        worker = Executors.newSingleThreadScheduledExecutor();
    }

    // Logic methods
    private void stateMachine(Signal signal) {
        switch(state) {
            case INIT:
                if (signal == Signal.RESUMED) {
                    state = State.SENDING_PAIRING_COMMAND;
                    updateUi();
                    startPairing();
                }
                break;
            case SENDING_PAIRING_COMMAND:
                if (signal == Signal.DEVICE_CONFIGURED) {
                    state = State.CONNETING_LOCAL_WIFI;
                    updateUi();
                    connectToLocalWifi();
                }
                break;
            case CONNETING_LOCAL_WIFI:
                if (signal == Signal.WIFI_CONNECTED) {
                    state = State.CONNECTING_TO_MQTT_BROKER;
                    updateUi();
                    connectToMqttBorker();
                }
                break;
            case CONNECTING_TO_MQTT_BROKER:
                if (signal == Signal.MQTT_CONNECTED) {
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

    private void startPairing() {
        // Check if the user has logged in. In case he is not, show a warning message
        // telling that the userid and key will be populated with predefined defaults
        ApiCredentials creds = AndroidPreferencesManager.loadHttpCredentials(getActivity());
        if (creds == null) {
            final AlertDialog alert = new AlertDialog.Builder(getActivity())
                    .setMessage("You have are not logged in to any HTTP API. " +
                            "The pairing process will therefore send the following defaults:" +
                            "\nKey: '"+DEFAULT_KEY+"' (empty)" +
                            "\nuserId: '"+DEFAULT_USER_ID+"' (empty)")
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            configureDevice(DEFAULT_USER_ID, DEFAULT_KEY);
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            error = "Operation aborted.";
                            stateMachine(Signal.ERROR);
                            dialog.dismiss();
                        }
                    })
                    .create();
            alert.show();
        } else {
            configureDevice(creds.getUserId(), creds.getKey());
        }
    }

    private void connectToLocalWifi() {
        // Register Wifi Broadcast Receiver
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        requireContext().getApplicationContext().registerReceiver(mReceiver, intentFilter);

        String ssid = pairActivityViewModel.getLocalWifiAp().getValue().getSsid();
        String bssid = pairActivityViewModel.getLocalWifiAp().getValue().getBssid();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            WifiConfiguration conf = new WifiConfiguration();
            conf.SSID = "\"" + ssid + "\"";
            conf.BSSID = "\"" + bssid + "\"";
            conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            mWifiManager.addNetwork(conf);
            List<WifiConfiguration> list = null;

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    (getContext().checkSelfPermission(Manifest.permission.CHANGE_WIFI_STATE) != PackageManager.PERMISSION_GRANTED)){
                error = "User denied CHANGE_WIFI_STATE permission. Wifi cannot be enabled.";
                stateMachine(PairFragment.Signal.ERROR);
                return;
            } else {
                list = mWifiManager.getConfiguredNetworks();
            }

            for (WifiConfiguration i : list) {
                if (i.SSID != null && i.SSID.equals("\"" + ssid + "\"")) {
                    mWifiManager.disconnect();
                    mWifiManager.enableNetwork(i.networkId, true);
                    mWifiManager.reconnect();
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
            mConnectivityManager.requestNetwork(networkRequest, new ConnectivityManager.NetworkCallback() {

                @Override
                public void onUnavailable() {
                    // TODO
                }
                @Override
                public void onAvailable(Network network) {
                    // TODO
                }
            });
        }
    }

    private void connectToMqttBorker() {
        String uri = "ssl://" + pairActivityViewModel.getTargetMqttConfig().getValue().getHostname() + ":" + pairActivityViewModel.getTargetMqttConfig().getValue().getPort();
        MqttAndroidClient mqttAndroidClient = new MqttAndroidClient(
                requireContext().getApplicationContext(),
                uri,
                "TEST"); // TODO: Change this
        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(false);
        options.setCleanSession(false);
        options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
        options.setServerURIs(new String[] {uri});

        // Disable SSL checks...
        // TODO: parametrize this
        TrustManager[] trustManagers = new DummyTrustManager[]{new DummyTrustManager()};
        SSLContext sc = null;
        try {
            sc = SSLContext.getInstance ("SSL");
            sc.init (null, trustManagers, new java.security.SecureRandom ());
            options.setSocketFactory(sc.getSocketFactory());
        } catch (KeyManagementException | NoSuchAlgorithmException e) {
            e.printStackTrace ();
            // TODO
        }

        mqttAndroidClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                // TODO
                Log.i("TEST", "connection lost");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                // TODO
                Log.i("TEST", "connection lost");
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                // TODO
                Log.i("TEST", "connection lost");
            }
        });

        /* Establish an MQTT connection */
        try {
            mqttAndroidClient.connect(options, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.i("TEST", "connect succeed");
                    // TODO
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.i("TEST", "connect failed");
                    // TODO
                }
            });

        } catch (MqttException e) {
            e.printStackTrace();
        }

    }

    private void configureDevice(final String userId, final String key) {
        worker.schedule(new Runnable() {
            @Override
            public void run() {
                MerossDeviceAp device = pairActivityViewModel.getDevice().getValue();
                try {
                    LiveData<MqttConfiguration> mqttConfig = pairActivityViewModel.getTargetMqttConfig();
                    device.setConfigKey(
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

                TargetWifiAp credentials = pairActivityViewModel.getLocalWifiAp().getValue();
                try {
                    device.setConfigWifi(credentials.getSsid(), credentials.getPassword());
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
                    return;
                }

            }
        },3, TimeUnit.SECONDS);
    }


    private void completeActivityFragment() {
        NavController ctrl = NavHostFragment.findNavController(this);
        ctrl.popBackStack(R.id.ScanFragment, false);
        ctrl.navigate(R.id.PairDone);
    }

    // UI
    private void updateUi() {
        Runnable uiUpdater = new Runnable() {
            @Override
            public void run() {
                switch (state) {
                    case INIT:
                        sendPairCommandTaskLine.setState(TaskLine.TaskState.not_started);
                        connectLocalWifiTaskLine.setState(TaskLine.TaskState.not_started);
                        testMqttBrokerTaskLine.setState(TaskLine.TaskState.not_started);
                        currentTask = null;
                        break;
                    case SENDING_PAIRING_COMMAND:
                        sendPairCommandTaskLine.setState(TaskLine.TaskState.running);
                        currentTask = sendPairCommandTaskLine;
                        break;
                    case CONNETING_LOCAL_WIFI:
                        sendPairCommandTaskLine.setState(TaskLine.TaskState.completed);
                        connectLocalWifiTaskLine.setState(TaskLine.TaskState.running);
                        currentTask = connectLocalWifiTaskLine;
                        break;
                    case CONNECTING_TO_MQTT_BROKER:
                        connectLocalWifiTaskLine.setState(TaskLine.TaskState.completed);
                        testMqttBrokerTaskLine.setState(TaskLine.TaskState.running);
                        currentTask = testMqttBrokerTaskLine;
                        break;
                    case DONE:
                        testMqttBrokerTaskLine.setState(TaskLine.TaskState.completed);
                        currentTask = null;
                        break;
                    case ERROR:
                        Snackbar.make(PairFragment.this.getView(), error, Snackbar.LENGTH_LONG).show();
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
        sendPairCommandTaskLine = view.findViewById(R.id.sendPairCommandTaskLine);
        connectLocalWifiTaskLine = view.findViewById(R.id.connectToBrokerWifi);
        testMqttBrokerTaskLine  = view.findViewById(R.id.connectToMqttBrokerTaskLike);
    }

    @Override
    public void onResume() {
        super.onResume();

        // As soon as we resume, connect to the given WiFi
        stateMachine(Signal.RESUMED);
    }


    enum State {
        INIT,
        SENDING_PAIRING_COMMAND,
        CONNETING_LOCAL_WIFI,
        CONNECTING_TO_MQTT_BROKER,
        DONE,
        ERROR
    }

    enum Signal {
        RESUMED,
        DEVICE_CONFIGURED,
        WIFI_CONNECTED,
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
                    if (mWifiManager.getConnectionInfo() != null && mWifiManager.getConnectionInfo().getSSID() != null) {
                        String targetSSID = "\"" + new String(Base64.decode(pairActivityViewModel.getLocalWifiAp().getValue().getSsid(), Base64.DEFAULT)) + "\"" ;
                        if (targetSSID.compareTo(mWifiManager.getConnectionInfo().getSSID()) == 0) {
                            PairFragment.this.requireContext().getApplicationContext().unregisterReceiver(this);
                            stateMachine(Signal.WIFI_CONNECTED);
                        }
                    }
                }
            }
        }
    }
}
