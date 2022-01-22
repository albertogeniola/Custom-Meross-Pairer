package com.albertogeniola.merossconf.ui.fragments.pair;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
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
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import com.albertogeniola.merossconf.AndroidUtils;
import com.albertogeniola.merossconf.model.exception.PermissionNotGrantedException;

import org.eclipse.paho.client.mqttv3.util.Strings;

import java.util.Timer;
import java.util.TimerTask;



public abstract class AbstractWifiFragment extends Fragment {
    private static final String TAG = "AbstractWifiFragment";
    private final Timer mTimer;
    private WifiManager mWifiManager;
    private ConnectivityManager mConnectivityManager;
    private static final String[] WIFI_PERMS = new String[] {Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.CHANGE_NETWORK_STATE};

    private static final int REQUEST_PERMISSION_CODE = 1;

    // holds the state of the wifi connection attempt
    private boolean mWifiConnectionAttemptInProgress = false;

    // keeps track of broadcast receiver registration status
    private boolean mBroadcastReceiverRegistered = false;

    // keeps track of network callback registration status
    private boolean mNetworkCallbackRegistered = false;

    // holds the timeout task
    private TimeoutTask mTimeoutTask;

    // hold target wifi ssid
    private String mTargetSsid;

    public AbstractWifiFragment() {
        mTimer = new Timer();
    }

    // Holds the callback for network connectivity (only used for version > LOLLIPOP)
    private ConnectivityManager.NetworkCallback mNetworkCallback;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mWifiManager = (WifiManager) requireContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mConnectivityManager = (ConnectivityManager) requireContext().getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        // Only init the network callback if we are going to use it
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            initNetworkCallBack();
        }

        restoreState(savedInstanceState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Unschedule the timeout task, if any
        if (mTimeoutTask!=null) {
            mTimeoutTask.cancel();
            mTimeoutTask = null;
        }

        // Reset the connection attempt state flag
        if (mWifiConnectionAttemptInProgress) {
            mWifiConnectionAttemptInProgress = false;
        }

        // Unregister any broadcast receiver
        if (mBroadcastReceiverRegistered) {
            unRegisterWifiBroadcastReceiver();
        }

        // Unregister network callbacks
        if (mNetworkCallbackRegistered && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            mConnectivityManager.unregisterNetworkCallback(mNetworkCallback);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private void restoreState(Bundle savedState) {
        if (savedState != null) {
            mNetworkCallbackRegistered = savedState.getBoolean("mNetworkCallbackRegistered", false);
            mBroadcastReceiverRegistered = savedState.getBoolean("mBroadcastReceiverRegistered", false);
            mTargetSsid = savedState.getString("mTargetSsid", null);
            mWifiConnectionAttemptInProgress = savedState.getBoolean("mWifiConnectionAttemptInProgress", false);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("mNetworkCallbackRegistered", mNetworkCallbackRegistered);
        outState.putBoolean("mBroadcastReceiverRegistered", mBroadcastReceiverRegistered);
        outState.putString("mTargetSsid", mTargetSsid);
        outState.putBoolean("mWifiConnectionAttemptInProgress", mWifiConnectionAttemptInProgress);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    /**
     * Starts the wifi connection to the specified ssid
     * @param targetSsid Target Wifi SSID to connect to
     * @param targetPassphrase (Optional) Wifi passphrase to use
     * @param reason A reason to show to the user that explains why Wifi is being requested.
     * @param timeout Number of milliseconds to wait before issuing a timeout to the wifi operation
     * @throws PermissionNotGrantedException In case no enough permissions have been granted
     *                                       to the app by the user. In such case, permissions
     *                                       is automatically requested by this method.
     */
    public void startWifiConnection(String targetSsid,
                                    @Nullable String targetPassphrase,
                                    @Nullable String reason,
                                    int timeout) throws PermissionNotGrantedException {
        if (mWifiConnectionAttemptInProgress) {
            throw new IllegalStateException("There already is another connection attempt in progress.");
        } else {
            mWifiConnectionAttemptInProgress = true;
        }

        // Check for Wifi and Location permissions
        if (!AndroidUtils.checkPermissions(requireContext(), WIFI_PERMS)) {

            // If a reason was specified, show it here.
            if (reason!=null) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage(reason).setTitle("Wifi Access Request");
                AlertDialog dialog = builder.create();
                dialog.show();
            }

            // If the user did not yet grant Wifi and Location permissions, ask them here and abort the connection.
            requestPermissions(WIFI_PERMS, REQUEST_PERMISSION_CODE);

            // Return false, so the caller know it has to retry the operation after the
            // user's consent to use wifi
            mWifiConnectionAttemptInProgress = false;
            throw new PermissionNotGrantedException();
        }

        // Enable Wifi
        mWifiManager.setWifiEnabled(true);

        // Store ssid for callback later usage
        mTargetSsid = targetSsid;

        // Since Android Q, Android requires the developer to work with NetworkRequest API.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            startWifiConnectionLegacy(targetSsid, targetPassphrase, timeout);
        } else {
            startWifiConnectionAndroidQ(targetSsid, targetPassphrase, timeout);
        }
    }

    /**
     * Starts the connection attempt against the legacy API.
     * To do so, it registers a broadcast receiver for the network connection events and
     * waits for the connection to attempt to the specified network.
     * @param ssid
     * @param passphrase
     * @param timeout Timeout in milliseconds
     */
    @SuppressLint("MissingPermission") //This piece of code is called only after permissions have bee granted
    private void startWifiConnectionLegacy(String ssid, @Nullable String passphrase, int timeout) {

        WifiConfiguration targetWifiConf = null;

        // Locate the existing network configuration entry.
        for (android.net.wifi.WifiConfiguration conf : mWifiManager.getConfiguredNetworks()) {

            if (conf.SSID.compareTo("\""+ssid+"\"") == 0) {
                // Found a matching configuration: make sure the network passphrase is OK
                if (conf.preSharedKey != null && conf.preSharedKey.length()>0 && Strings.isEmpty(passphrase) || Strings.isEmpty(conf.preSharedKey) && !Strings.isEmpty(passphrase)) {
                    Log.i(TAG, "Network configuration ("+conf.networkId+") ignored: passphrase mismatch.");
                    continue;
                }
                // If we reach this part, it means we found a valid, consistent wifi configuration to be used.
                Log.i(TAG, "Network configuration ("+conf.networkId+") found for ssid "+ssid+".");
                targetWifiConf = conf;
                break;
            }
        }

        Integer networkId = null;
        if (targetWifiConf == null || targetWifiConf.networkId == -1) {
            Log.i(TAG, "Adding new Wifi configuration for the desired network.");
            targetWifiConf = new android.net.wifi.WifiConfiguration();
            targetWifiConf.SSID = "\""+ssid+"\"";

            if (!Strings.isEmpty(passphrase)) {
                targetWifiConf.preSharedKey = "\""+passphrase+"\"";
                targetWifiConf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            } else {
                targetWifiConf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            }
            networkId = mWifiManager.addNetwork(targetWifiConf);
        } else {
            networkId = targetWifiConf.networkId;
        }

        // Disconnect from current network
        Log.i(TAG, "Forcing disconnection/reconnection to the selected wifi "+targetWifiConf);
        mWifiManager.disconnect();

        // Start the timeout countdown
        mTimeoutTask = new TimeoutTask();
        mTimer.schedule(mTimeoutTask, timeout);
        // Register the broadcast receiver
        registerWifiBroadcastReceiver(ssid);

        // Reconnect!
        mWifiManager.enableNetwork(networkId, true);
        mWifiManager.reconnect();
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void startWifiConnectionAndroidQ(String ssid, @Nullable String passphrase, int timeout) {
        WifiNetworkSpecifier.Builder specifierBuilder = new WifiNetworkSpecifier.Builder()
                .setSsid(ssid);
        if (passphrase != null)
            specifierBuilder.setWpa2Passphrase(passphrase);

        WifiNetworkSpecifier specifier = specifierBuilder.build();
        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) // The wifi does not necessarily need Internet Connection
                .setNetworkSpecifier(specifier)
                .build();

        mNetworkCallbackRegistered = true;
        mConnectivityManager.requestNetwork(networkRequest, mNetworkCallback, timeout);
    }

    /**
     * Registers a broadcast received that listens to WIFI_STATE changes. It also sets the value
     * of the SSID to configure the broadcast receiver to intercept the desired network
     * connection.
     * @param ssid SSID value that represet the network we are looking for
     */
    private void registerWifiBroadcastReceiver(String ssid) {
        if (!mBroadcastReceiverRegistered) {
            mBroadcastReceiverRegistered = true;
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
            requireActivity().registerReceiver(legacyWifiBroadcastReceiver, intentFilter);
        } else {
            Log.e(TAG, "Broadcast receiver already registered");
        }
    }

    /**
     * Unregisters the broadcast receiver used for legacy Wifi connection API
     */
    private void unRegisterWifiBroadcastReceiver() {
        if (mBroadcastReceiverRegistered) {
            requireActivity().unregisterReceiver(legacyWifiBroadcastReceiver);
            mBroadcastReceiverRegistered = false;
            mTargetSsid = null;

        } else {
            Log.e(TAG, "Broadcast receiver already unregistered");
        }
    }

    /**
     * Callback called whenever the connection to the given wifi succeeds.
     */
    protected abstract void onWifiConnected(String ssid);

    /**
     * Callback called whenever the connection to the given wifi fails.
     */
    protected abstract void onWifiUnavailable(String ssid);

    /**
     * Callback called when the user refuses to provide enough wifi permissions
     */
    protected abstract void onMissingWifiPermissions(String ssid);

    /**
     * Callback called when the user has granted necessary wifi permissions
     */
    protected abstract void onWifiPermissionsGranted(String ssid);

    private void notifyWifiConnected() {
        mWifiConnectionAttemptInProgress = false;
        if (mTimeoutTask!=null) {
            mTimeoutTask.cancel();
            mTimeoutTask = null;
        }
        onWifiConnected(mTargetSsid);
    }

    private void notifyWifiUnavailable() {
        mWifiConnectionAttemptInProgress = false;
        if (mTimeoutTask!=null) {
            mTimeoutTask.cancel();
            mTimeoutTask = null;
        }
        onWifiUnavailable(mTargetSsid);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void initNetworkCallBack() {
        mNetworkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onUnavailable() {
                super.onUnavailable();
                notifyWifiUnavailable();
            }

            @Override
            public void onLost(@NonNull Network network) {
                super.onLost(network);
                // TODO
                /*
                mConnectivityManager.bindProcessToNetwork(null);
                mConnectivityManager.unregisterNetworkCallback(this);
                // Here you can have a fallback option to show a 'Please connect manually' page with an Intent to the Wifi settings
                */
            }

            @Override
            public void onAvailable(@NonNull Network network) {
                Log.i(TAG, "Found network " + network);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // To make sure that requests don't go over mobile data
                    mConnectivityManager.bindProcessToNetwork(network);
                } else {
                    ConnectivityManager.setProcessDefaultNetwork(network);
                }
                notifyWifiConnected();
            }
        };
    }

    private class TimeoutTask extends TimerTask {
        @Override
        public void run() {
            Log.w(TAG, "Wifi search timeout reached.");
            if (mBroadcastReceiverRegistered) {
                unRegisterWifiBroadcastReceiver();
            }
            notifyWifiUnavailable();
        }
    }

    private final BroadcastReceiver legacyWifiBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiManager.NETWORK_STATE_CHANGED_ACTION .equals(action)) {
                NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (networkInfo == null) {
                    Log.w(TAG, "Network info is null!");
                    return;
                }

                WifiInfo connectionInfo = mWifiManager.getConnectionInfo();
                String connectedSsid = null;
                String connectedBssid = null;

                if (connectionInfo!=null) {
                    connectedSsid = connectionInfo.getSSID();
                    connectedBssid = connectionInfo.getBSSID();
                }

                if (connectedSsid == null) {
                    Log.w(TAG, "Connected SSID is null.");
                    return;
                } else
                    Log.i(TAG, "WifiState updated. Connected:  " + networkInfo.isConnected() + ", SSID: " + connectedSsid);

                if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI && networkInfo.isConnected()) {
                    Log.i(TAG, "WifiState updated. Current SSID: "+connectedSsid);

                    String quotedSsid = "\""+mTargetSsid+"\"";
                    if (quotedSsid.compareTo(connectedSsid) == 0) {
                        // Ok, we found the network we were looking for.
                        notifyWifiConnected();
                        unRegisterWifiBroadcastReceiver();
                    }
                }
            }
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.i(TAG, "User permissions handler...");
        boolean permsOk = true;
        if (requestCode == REQUEST_PERMISSION_CODE) {
            for (int i=0; i<permissions.length;i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    permsOk = false;
                    break;
                }
            }

            if (!permsOk)
                onMissingWifiPermissions(mTargetSsid);
            else
                onWifiPermissionsGranted(mTargetSsid);
        }
    }
}
