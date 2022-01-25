package com.albertogeniola.merossconf.ui.fragments.pair;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import androidx.annotation.UiThread;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.albertogeniola.merossconf.AndroidPreferencesManager;
import com.albertogeniola.merossconf.Constants;
import com.albertogeniola.merossconf.MerossUtils;
import com.albertogeniola.merossconf.R;
import com.albertogeniola.merossconf.model.WifiConfiguration;
import com.albertogeniola.merossconf.model.exception.PermissionNotGrantedException;
import com.albertogeniola.merossconf.ui.PairActivityViewModel;
import com.albertogeniola.merosslib.model.Encryption;
import com.albertogeniola.merosslib.model.protocol.payloads.GetConfigWifiListEntry;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import lombok.SneakyThrows;


public class ConfigureWifiFragment extends AbstractWifiFragment {
    private static final String TAG = "ConfigureWifiFragment";
    private NsdManager mNsdManager;
    private static final String SERVICE_TYPE = "_meross-mqtt._tcp.";
    private static final int WIFI_CONNECT_DELAY = 5000;

    private Handler mUiHandler;
    private PairActivityViewModel pairActivityViewModel;
    private Spinner wifiSpinner;
    private MaterialButton mNextButton;
    private MaterialButton mSkipButton;
    private TextInputLayout mWifiPasswordTextView;
    private WifiSpinnerAdapter adapter;
    private ProgressBar mLinearProgressBar;
    private CheckBox mSaveWifiPasswordCheckBox;
    private boolean mDiscoveryInProgress = false;
    private boolean mResolveInProgress = false;
    private WifiConfiguration mSelectedWifi = null;

    private Timer mTimer;

    private static final String VALIDATE_AND_PROCEED = "Validate and proceed";
    private static final String DISCOVERY_MQTT = "MQTT discovery...";
    private static final String MQTT_RESOLVE = "Resolving service...";
    private static final String COMPLETED = "Completed";
    private boolean mPaused = false;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pairActivityViewModel = new ViewModelProvider(requireActivity()).get(PairActivityViewModel.class);
        mNsdManager = (NsdManager) requireContext().getSystemService(Context.NSD_SERVICE);
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

        mWifiPasswordTextView = view.findViewById(R.id.wifi_password);
        mNextButton = view.findViewById(R.id.next_button);
        mSaveWifiPasswordCheckBox = view.findViewById(R.id.saveWifiPasswordCheckBox);
        mSkipButton = view.findViewById(R.id.next_no_validation_button);
        mLinearProgressBar = view.findViewById(R.id.wifi_progress_bar);

        configureUi(true, VALIDATE_AND_PROCEED, null);
        mNextButton.setOnClickListener(nextButtonClick);
        mSkipButton.setOnClickListener(skipButtonClick);
        CheckBox showPasswordButton = view.findViewById(R.id.showPasswordCheckbox);
        mWifiPasswordTextView.getEditText().setTransformationMethod(PasswordTransformationMethod.getInstance());
        showPasswordButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mWifiPasswordTextView.getEditText().getTransformationMethod() == HideReturnsTransformationMethod.getInstance()) {
                    mWifiPasswordTextView.getEditText().setTransformationMethod(PasswordTransformationMethod.getInstance());
                } else {
                    mWifiPasswordTextView.getEditText().setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                }
            }
        });

        wifiSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                GetConfigWifiListEntry selection = adapter.getItem(position);
                String savedPassword = AndroidPreferencesManager.getWifiStoredPassword(requireContext(), selection.getBssid());
                if (savedPassword != null)
                    mWifiPasswordTextView.getEditText().setText(savedPassword);
                else
                    mWifiPasswordTextView.getEditText().setText("");
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        mPaused = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        mPaused = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mDiscoveryInProgress)
            mNsdManager.stopServiceDiscovery(mDiscoveryListener);
        mResolveInProgress = false;
    }

    @Override
    protected void onWifiConnected(String ssid) {
        // If the wifi connection succeeds, store the wifi info into the parent model
        mUiHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                pairActivityViewModel.setMerossWifiConfiguration(mSelectedWifi);
            }
        }, WIFI_CONNECT_DELAY);

        // Store the password, if required
        if (mSaveWifiPasswordCheckBox.isChecked())
            AndroidPreferencesManager.storeWifiStoredPassword(requireContext(), mSelectedWifi.getScannedWifi().getBssid(), mWifiPasswordTextView.getEditText().getText().toString());

        // As soo as the Wifi is connected, we start the Mqtt Discovery
        if (mDiscoveryInProgress) {
            Log.e(TAG, "Discovery is already in progress.");
            // TODO: shall we stop and re-run?
            // mNsdManager.stopServiceDiscovery(mDiscoveryListener);
            // mNsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
        } else if (mResolveInProgress) {
            Log.e(TAG, "Resolve in progress. Cannot issue discovery.");
        } else {
            Log.i(TAG, "Wifi connected, issuing discovery.");
            startApiDiscovery();
        }
    }

    private void startApiDiscovery() {
        // Setup UI
        configureUi(false,  DISCOVERY_MQTT, null);

        // Start mDNS discovery
        if (!mDiscoveryInProgress) {
            mNsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
        }

        // Start a timer for aborting discovery after some time  if nothing is found.
        if (mTimer == null) {
            mTimer = new Timer();
            mTimer.schedule(new ConfigureWifiFragment.TimeoutTask(), Constants.MDNS_DISCOVERY_TIMEOUT_MILLISEOONDS);
        }
    }

    @Override
    protected void onWifiUnavailable(String ssid) {
        configureUi(true, VALIDATE_AND_PROCEED, "Wifi validation failed. Please double check credentials and try again.");
    }

    @Override
    protected void onMissingWifiPermissions(String ssid) {
        configureUi(true, VALIDATE_AND_PROCEED, "Please grant Wifi and Location permissions to this app. It won't work without them.");
    }

    @SneakyThrows(PermissionNotGrantedException.class)
    @Override
    protected void onWifiPermissionsGranted(String ssid) {
        startWifiConnection(ssid, mSelectedWifi.getClearWifiPassword(), null, 60000);
    }

    @UiThread
    private void configureUi(final Boolean uiEnabled,
                             final String nextButtonText,
                             @Nullable final String textMessage) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                wifiSpinner.setEnabled(uiEnabled);
                mNextButton.setEnabled(uiEnabled);
                mSkipButton.setEnabled(uiEnabled);
                mSaveWifiPasswordCheckBox.setEnabled(uiEnabled);
                mNextButton.setText(nextButtonText);
                mLinearProgressBar.setVisibility(uiEnabled ? View.GONE : View.VISIBLE);
                mWifiPasswordTextView.setEnabled(uiEnabled);

                if (textMessage != null) {
                    Toast.makeText(requireContext(), textMessage, Toast.LENGTH_SHORT).show();
                }
            }
        };
        if (Looper.getMainLooper().getThread()==Thread.currentThread())
            r.run();
        else
            mUiHandler.post(r);
    }

    private final View.OnClickListener skipButtonClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            // Just store the current configuration and proceed to the next fragment
            pairActivityViewModel.setMerossWifiConfiguration(mSelectedWifi);

            // Store the password, if required
            if (mSaveWifiPasswordCheckBox.isChecked())
                AndroidPreferencesManager.storeWifiStoredPassword(requireContext(), mSelectedWifi.getScannedWifi().getBssid(), mWifiPasswordTextView.getEditText().toString());

            // Launch the next fragment
            NavController ctrl = NavHostFragment.findNavController(ConfigureWifiFragment.this);
            ctrl.navigate(R.id.action_configureWifi_to_configureMqtt, null, new NavOptions.Builder().setEnterAnim(android.R.animator.fade_in).setExitAnim(android.R.animator.fade_out).build());
        }
    };

    private final View.OnClickListener nextButtonClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // Validate the configuration...
            if (wifiSpinner.getSelectedItemPosition() < 0) {
                Toast.makeText(getContext(), "Please select a Wifi AP from the dropdown", Toast.LENGTH_LONG).show();
                return;
            }

            // If the wifi requires a password, make sure the user inputted one.
            GetConfigWifiListEntry selectedWifi = adapter.getItem(wifiSpinner.getSelectedItemPosition());
            if (selectedWifi.getEncryption() != Encryption.OPEN && mWifiPasswordTextView.getEditText().getText().toString().isEmpty()) {
                mWifiPasswordTextView.setError("That wifi requires a password.");
                return ;
            } else {
                mWifiPasswordTextView.setError(null);
            }

            // Start wifi connection validation
            String clearPassword = mWifiPasswordTextView.getEditText().getText().toString();
            WifiConfiguration conf = new WifiConfiguration(selectedWifi, clearPassword);
            startWifiConnectionValidation(conf);
        }
    };

    private void notifyResolveCompleted(@Nullable final String hostname,
                                      @Nullable final Integer port) {
        // FIXME: Workaround, as it appears there is no way to stop the mNSDManager from
        //  notifying resolved services ater a network/reconnection (stopping the discovery does
        //  not help)
        if (mPaused)
            return;

        configureUi(false, COMPLETED, null);

        // In case the discovery found a valid service, put it into a parcel for the next fragment
        final Bundle args = new Bundle();
        if (hostname != null && port != null) {
            args.putString("hostname", hostname);
            args.putInt("port", port);
            Toast.makeText(requireContext(), "Found a Meross MQTT broker in this LAN", Toast.LENGTH_SHORT).show();
        }

        // Cancel the timeout task
        if (mTimer!=null) {
            mTimer.cancel();
            mTimer = null;
        }

        Runnable r = new Runnable() {
            @Override
            public void run() {
                // Navigate to the next fragment
                NavController ctrl = NavHostFragment.findNavController(ConfigureWifiFragment.this);
                ctrl.navigate(R.id.action_configureWifi_to_configureMqtt, args, new NavOptions.Builder().setEnterAnim(android.R.animator.fade_in).setExitAnim(android.R.animator.fade_out).build());
            }
        };
        mUiHandler.post(r);
    }

    private void startWifiConnectionValidation(WifiConfiguration selectedWifi) {
        mSelectedWifi = selectedWifi;
        configureUi(false, "Connecting to wifi...", null);

        // Start wifi connection
        try {
            startWifiConnection(mSelectedWifi.getScannedWifi().getSsid(), mSelectedWifi.getClearWifiPassword(), null, 20000);
            // The flow starts back from on onWifiConnected / onWifiUnavailable().
        } catch (PermissionNotGrantedException e) {
            // The flow starts back from onWifiPermissionsGranted()
        }
    }

    public class WifiSpinnerAdapter extends ArrayAdapter<GetConfigWifiListEntry> {
        private final ArrayList<GetConfigWifiListEntry> values;

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

    // mDNS discovery listener
    private final NsdManager.DiscoveryListener mDiscoveryListener = new NsdManager.DiscoveryListener() {
        // Called as soon as service discovery begins.
        @Override
        public void onDiscoveryStarted(String serviceType) {
            Log.d(TAG, "Service discovery started");
            mDiscoveryInProgress = true;
        }

        @Override
        public void onServiceFound(NsdServiceInfo service) {
            Log.d(TAG, "Service discovery success" + service);
            mNsdManager.resolveService(service, mResolveListener);
        }

        @Override
        public void onStartDiscoveryFailed(String serviceType, int errorCode) {
            Log.e(TAG, "Discovery failed: Error code:" + errorCode);
            configureUi(true,  VALIDATE_AND_PROCEED, "Discovery failed");
            mNsdManager.stopServiceDiscovery(this);
        }

        @Override
        public void onStopDiscoveryFailed(String serviceType, int errorCode) {
            Log.e(TAG, "Discovery failed: Error code:" + errorCode);
            mNsdManager.stopServiceDiscovery(this);
        }

        @Override
        public void onDiscoveryStopped(String serviceType) {
            Log.i(TAG, "Discovery stopped: " + serviceType);
            mDiscoveryInProgress = false;
        }

        @Override
        public void onServiceLost(NsdServiceInfo serviceInfo) {
            // When the network service is no longer available.
            // Internal bookkeeping code goes here.
            Log.e(TAG, "service lost: " + serviceInfo.getServiceType());
        }
    };

    // mDNS resolver
    private final NsdManager.ResolveListener mResolveListener = new NsdManager.ResolveListener() {
        @Override
        public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {

            Log.e(TAG, "Resolve failed" + errorCode);
            mResolveInProgress = false;
            configureUi(false, VALIDATE_AND_PROCEED, "mDNSResolve failed");
        }
        @Override
        public void onServiceResolved(final NsdServiceInfo serviceInfo) {
            Log.e(TAG, "Resolve Succeeded. " + serviceInfo);
            mResolveInProgress = false;
            configureUi(false, VALIDATE_AND_PROCEED, null);
            notifyResolveCompleted(serviceInfo.getHost().getHostName(), serviceInfo.getPort());
        }
    };

    private class TimeoutTask extends TimerTask {
        @Override
        public void run() {
            if (mDiscoveryInProgress)
                mNsdManager.stopServiceDiscovery(mDiscoveryListener);

            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    // Just store the current configuration and proceed to the next fragment
                    pairActivityViewModel.setMerossWifiConfiguration(mSelectedWifi);

                    // Launch the next fragment
                    NavController ctrl = NavHostFragment.findNavController(ConfigureWifiFragment.this);
                    ctrl.navigate(R.id.action_configureWifi_to_configureMqtt, null, new NavOptions.Builder().setEnterAnim(android.R.animator.fade_in).setExitAnim(android.R.animator.fade_out).build());

                    configureUi(true,  VALIDATE_AND_PROCEED, "Failed to locate MQTT endpoint via mDNS");
                }
            });
        }
    }
}