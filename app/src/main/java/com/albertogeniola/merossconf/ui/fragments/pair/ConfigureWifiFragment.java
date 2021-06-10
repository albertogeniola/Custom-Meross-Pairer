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
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.albertogeniola.merossconf.AndroidPreferencesManager;
import com.albertogeniola.merossconf.MerossUtils;
import com.albertogeniola.merossconf.R;
import com.albertogeniola.merossconf.model.WifiConfiguration;
import com.albertogeniola.merossconf.ui.PairActivityViewModel;
import com.albertogeniola.merosslib.model.Encryption;
import com.albertogeniola.merosslib.model.protocol.payloads.GetConfigWifiListEntry;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;


public class ConfigureWifiFragment extends Fragment {
    private static final String TAG = "ConfigureWifiFragment";
    private WifiManager mWifiManager;
    private ConnectivityManager mConnectivityManager;

    private Handler mUiHandler;
    private PairActivityViewModel pairActivityViewModel;
    private Spinner wifiSpinner;
    private MaterialButton mNextButton;
    private TextInputLayout wifiPasswordTextView;
    private WifiSpinnerAdapter adapter;
    private boolean mSavePassword;
    private boolean mReceiverRegistered = false;
    private boolean mWaitingWifi = true;

    private String mTargetWifiSsid;
    private WifiConfiguration mWifi;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pairActivityViewModel = new ViewModelProvider(requireActivity()).get(PairActivityViewModel.class);
        mWifiManager = (WifiManager) requireContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mConnectivityManager = (ConnectivityManager) requireContext().getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        mUiHandler = new Handler(requireContext().getMainLooper());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_wifi_config, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        wifiSpinner = view.findViewById(R.id.wifiListSpinner);
        adapter = new WifiSpinnerAdapter(ConfigureWifiFragment.this.getContext(), pairActivityViewModel.getDeviceAvailableWifis().getValue().getPayload().getWifiList());
        wifiSpinner.setAdapter(adapter);

        wifiPasswordTextView = view.findViewById(R.id.wifi_password);
        mNextButton = view.findViewById(R.id.next_button);
        setUiValidatingWifi(false);
        mNextButton.setOnClickListener(nextButtonClick);
        CheckBox showPasswordButton = view.findViewById(R.id.showPasswordCheckbox);
        wifiPasswordTextView.getEditText().setTransformationMethod(PasswordTransformationMethod.getInstance());
        showPasswordButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (wifiPasswordTextView.getEditText().getTransformationMethod() == HideReturnsTransformationMethod.getInstance()) {
                    wifiPasswordTextView.getEditText().setTransformationMethod(PasswordTransformationMethod.getInstance());
                } else {
                    wifiPasswordTextView.getEditText().setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                }
            }
        });

        CheckBox saveWifiPasswordCheckBox = view.findViewById(R.id.saveWifiPasswordCheckBox);
        saveWifiPasswordCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mSavePassword = isChecked;
            }
        });
        mSavePassword = saveWifiPasswordCheckBox.isChecked();

        wifiSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                GetConfigWifiListEntry selection = adapter.getItem(position);
                String savedPassword = AndroidPreferencesManager.getWifiStoredPassword(requireContext(), selection.getBssid());
                if (savedPassword != null)
                    wifiPasswordTextView.getEditText().setText(savedPassword);
                else
                    wifiPasswordTextView.getEditText().setText("");
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Unregister the broadcast receiver in case we did not unregister it yet
        unregisterWifiBroadcastReceiver();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void setUiValidatingWifi(boolean validatingWifi) {
        mWaitingWifi = validatingWifi;
        mNextButton.setText(validatingWifi ? "Validating..." : "Next");
        mNextButton.setClickable(!validatingWifi);

        if (!validatingWifi) {
            unregisterWifiBroadcastReceiver();
        }
    }

    private View.OnClickListener nextButtonClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // Validate the configuration...
            if (wifiSpinner.getSelectedItemPosition() < 0) {
                Toast.makeText(getContext(), "Please select a Wifi AP from the dropdown", Toast.LENGTH_LONG).show();
                return;
            }

            // If the wifi requires a password, make sure the user inputted one.
            GetConfigWifiListEntry selectedWifi = adapter.getItem(wifiSpinner.getSelectedItemPosition());
            if (selectedWifi.getEncryption() != Encryption.OPEN && wifiPasswordTextView.getEditText().getText().toString().isEmpty()) {
                wifiPasswordTextView.setError("That wifi requires a password.");
                return ;
            } else {
                wifiPasswordTextView.setError(null);
            }

            // Start wifi connection validation
            String clearPassword = wifiPasswordTextView.getEditText().getText().toString();
            WifiConfiguration conf = new WifiConfiguration(selectedWifi, clearPassword);
            startWifiConnectionValidation(conf);
        }
    };

    private void connectToWifi(WifiConfiguration selectedWifi) {
        registerWifiBroadcastReceiver();

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                (getContext().checkSelfPermission(Manifest.permission.CHANGE_WIFI_STATE) != PackageManager.PERMISSION_GRANTED)){
            setWifiValidationFailed("User denied CHANGE_WIFI_STATE permission. Wifi cannot be enabled.");
            return;
        }
        String ssid = selectedWifi.getScannedWifi().getSsid();

        String targetWifiBssid = selectedWifi.getScannedWifi().getBssid().replaceAll("-",":").toLowerCase();
        mTargetWifiSsid = "\"" + ssid +"\"";

        // In case the device is "old", we rely on the classic WifiManager API
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            android.net.wifi.WifiConfiguration targetWifiConf = null;

            // Locate the existing network configuration entry.
            for (android.net.wifi.WifiConfiguration conf : mWifiManager.getConfiguredNetworks()) {
                if (conf.BSSID.replaceAll("-",":").toLowerCase().compareTo(targetWifiBssid)==0
                        && conf.SSID.compareTo(mTargetWifiSsid)==0) {
                    // Found a matching configuration.
                    targetWifiConf = conf;
                    break;
                }
            }

            if (targetWifiConf == null) {
                // TODO: test this
                android.net.wifi.WifiConfiguration wifiConf = new android.net.wifi.WifiConfiguration();
                wifiConf.SSID = ssid;
                wifiConf.BSSID = targetWifiBssid;
                mWifiManager.addNetwork(wifiConf);
            }

            if (targetWifiConf == null) {
                setWifiValidationFailed("Could not find a known network named '"+ssid+"' with bssid '"+targetWifiBssid+"'.");
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
            WifiNetworkSpecifier.Builder specifierBuilder = new WifiNetworkSpecifier.Builder()
                    .setSsid(ssid)
                    .setBssid(MacAddress.fromString(targetWifiBssid));
            if (selectedWifi.getClearWifiPassword() != null)
                specifierBuilder.setWpa2Passphrase(selectedWifi.getClearWifiPassword());

            WifiNetworkSpecifier specifier = specifierBuilder.build();
            NetworkRequest networkRequest = new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) // The wifi does not necessarily need Internet Connection
                    .setNetworkSpecifier(specifier)
                    .build();

            mConnectivityManager.requestNetwork(networkRequest, new ConnectivityManager.NetworkCallback() {
                @Override
                public void onUnavailable() {
                    mUiHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            setWifiValidationFailed("Wifi network unavailable");
                        }
                    });
                }
                @Override
                public void onAvailable(Network network) {
                    Log.i(TAG, "Found network " + network);
                    //mNetworkSocketFactory = network.getSocketFactory();
                }
            });
        }
    }

    private void setWifiValidationFailed(String message) {
        Snackbar.make(getView(), message, Snackbar.LENGTH_LONG ).setAnchorView(mNextButton).show();
        setUiValidatingWifi(false);
    }

    private void setWifiValidationSucceeded() {
        Toast.makeText(requireContext(), "Wifi validation succeeded.", Toast.LENGTH_SHORT).show();

        pairActivityViewModel.setMerossWifiConfiguration(mWifi);

        // Save the password
        if (mSavePassword)
            AndroidPreferencesManager.storeWifiStoredPassword(requireContext(), mWifi.getScannedWifi().getBssid(), mWifi.getClearWifiPassword());

        // Navigate to the next fragment
        NavHostFragment.findNavController(ConfigureWifiFragment.this)
                .navigate(R.id.ConfigureMqttFragment);

        setUiValidatingWifi(false);
    }

    private void startWifiConnectionValidation(WifiConfiguration selectedWifi) {
        mWifi = selectedWifi;

        setUiValidatingWifi(true);

        // Start wifi connection
        connectToWifi(selectedWifi);

        // Set a timer to abort in case we are taking too long...
        mUiHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mWaitingWifi) {

                    setWifiValidationFailed("Could not connect to the desired wifi");
                }
            }
        }, 30000);

    }

    public class WifiSpinnerAdapter extends ArrayAdapter<GetConfigWifiListEntry> {
        private ArrayList<GetConfigWifiListEntry> values;

        public WifiSpinnerAdapter(Context context, List<GetConfigWifiListEntry> values) {
            super(context, R.layout.wifi_dropdown_item, R.id.wifi_name);
            this.values = new ArrayList<>();
            for (GetConfigWifiListEntry entry : values) {
                if (! MerossUtils.isMerossAp(entry.getSsid()))
                    this.values.add(entry);
            }
        }

        @Override
        public int getCount(){
            return values.size();
        }

        @Override
        public GetConfigWifiListEntry getItem(int position){
            return values.get(position);
        }

        @Override
        public long getItemId(int position){
            return position;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            View view;
            if (convertView == null)
                view = getLayoutInflater().inflate(R.layout.wifi_dropdown_item, parent, false);
            else
                view = convertView;

            TextView wifiName = view.findViewById(R.id.wifi_name);
            TextView bssid = view.findViewById(R.id.wifi_bssid);
            TextView channel = view.findViewById(R.id.wifi_channel);
            ProgressBar signal = view.findViewById(R.id.wifi_signal);

            GetConfigWifiListEntry value = values.get(position);
            double quality = value.getSignal();

            wifiName.setText(value.getSsid());
            bssid.setText(value.getBssid());
            channel.setText("" + value.getChannel());
            signal.setIndeterminate(false);
            signal.setProgress((int)quality);

            return view;
        }

        @Override
        public View getDropDownView(int position, View convertView,
                                    @NonNull ViewGroup parent) {
            return getView(position, convertView, parent);
        }
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiManager.NETWORK_STATE_CHANGED_ACTION .equals(action)) {
                NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (networkInfo != null
                        && networkInfo.isConnected()
                        && mWifiManager.getConnectionInfo() != null
                        && mWifiManager.getConnectionInfo().getSSID() != null) {
                    String currentSsid = mWifiManager.getConnectionInfo().getSSID();
                    Log.i(TAG, "WifiState updated. Current SSID: "+currentSsid);

                    if (mTargetWifiSsid.compareTo(currentSsid) == 0) {
                        setWifiValidationSucceeded();
                    }
                }
            }
        }
    };

    private synchronized void unregisterWifiBroadcastReceiver() {
        if (mReceiverRegistered) {
            ConfigureWifiFragment.this.requireContext().getApplicationContext().unregisterReceiver(mReceiver);
            mReceiverRegistered = false;
        }
    }

    private synchronized void registerWifiBroadcastReceiver() {
        if (!mReceiverRegistered) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
            ConfigureWifiFragment.this.requireContext().getApplicationContext().registerReceiver(mReceiver, intentFilter, null, mUiHandler);
            mReceiverRegistered = true;
        }
    }
}