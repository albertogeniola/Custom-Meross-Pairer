package com.albertogeniola.merossconf.ui.fragments.login;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.albertogeniola.merossconf.AndroidPreferencesManager;
import com.albertogeniola.merossconf.AndroidUtils;
import com.albertogeniola.merossconf.R;
import com.albertogeniola.merossconf.model.HttpClientManager;
import com.albertogeniola.merossconf.ui.MainActivityViewModel;
import com.albertogeniola.merosslib.model.http.ApiCredentials;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.CircularProgressIndicatorSpec;
import com.google.android.material.progressindicator.IndeterminateDrawable;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Timer;
import java.util.TimerTask;

import static com.albertogeniola.merossconf.AndroidUtils.dpToPx;


public class LoginFragment extends Fragment {
    // Constants
    private static final String SERVICE_TYPE = "_meross-local-api._tcp.";
    private static final  String TAG = "Login";

    // Instance attributes
    private boolean mDiscoveryInProgress;
    private NsdManager mNsdManager;
    private TextInputLayout mHttpHostnameInputLayout;
    private EditText mHttpHostnameEditText;
    private EditText mHttpUsernameEditText;
    private EditText mHttpPasswordEditText;
    private MaterialButton mLoginButton;
    private MaterialButton mDiscoveryButton;

    private Timer mTimer;
    private Handler mUiHandler;

    private CircularProgressIndicator mSearchProgress;
    private IndeterminateDrawable<CircularProgressIndicatorSpec> mProgressIndicatorDrawable;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mNsdManager = (NsdManager) requireContext().getSystemService(Context.NSD_SERVICE);
        mUiHandler = new Handler(this.requireContext().getMainLooper());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        // Bind views
        CheckBox showPasswordCheckBox = view.findViewById(R.id.showPasswordCheckBox);
        mHttpHostnameInputLayout =  view.findViewById(R.id.httpHostnameInputLayout);
        mHttpHostnameEditText = mHttpHostnameInputLayout.getEditText();
        mHttpPasswordEditText = ((TextInputLayout)view.findViewById(R.id.httpPasswordEditText)).getEditText();
        mHttpUsernameEditText = ((TextInputLayout)view.findViewById(R.id.httpUsernameEditText)).getEditText();
        mLoginButton = view.findViewById(R.id.loginButton);
        mDiscoveryButton = view.findViewById(R.id.discoveryButton);

        // Configure HostEditText for progress showing
        mSearchProgress = new CircularProgressIndicator(this.requireActivity(), null);
        mSearchProgress.setIndicatorSize((int)dpToPx(requireContext(), 15));
        mSearchProgress.setIndeterminate(true);
        mProgressIndicatorDrawable = mSearchProgress.getIndeterminateDrawable();
        // Show/Hide password logic
        showPasswordCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mHttpPasswordEditText.getTransformationMethod() == HideReturnsTransformationMethod.getInstance()) {
                    mHttpPasswordEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                } else {
                    mHttpPasswordEditText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                }
            }
        });

        // Login button logic
        mLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performLogin();
            }
        });

        // Discovery button logic
        mDiscoveryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startApiDiscovery();
            }
        });

        // Setup the edit-text to hide the password characters
        showPasswordCheckBox.setChecked(false);
        mHttpPasswordEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());

        return view;
    }

    private void startApiDiscovery() {
        // Setup UI
        configureUi(false,  true, null, null);

        // Start mDNS discovery
        if (!mDiscoveryInProgress) {
            mNsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
        }

        // Start a timer for aborting discovery after 10 seconds if nothing is found.
        if (mTimer == null) {
            mTimer = new Timer();
            mTimer.schedule(new TimeoutTask(), 5000);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
    }

    private void configureUi(final boolean uiEnabled, final boolean discoveryInProgress, @Nullable final String hostnameValue, @Nullable final String message) {
        Runnable logic = new Runnable() {
            @Override
            public void run() {
                mLoginButton.setEnabled(uiEnabled);
                mDiscoveryButton.setEnabled(uiEnabled);
                mHttpUsernameEditText.setEnabled(uiEnabled);
                mHttpPasswordEditText.setEnabled(uiEnabled);

                if (discoveryInProgress) {
                    mHttpHostnameInputLayout.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
                    mHttpHostnameInputLayout.setEndIconDrawable(mProgressIndicatorDrawable);
                } else {
                    mHttpHostnameInputLayout.setEndIconMode(TextInputLayout.END_ICON_NONE);
                    mHttpHostnameInputLayout.setEndIconDrawable(null);
                }

                if (hostnameValue != null) {
                    mHttpHostnameEditText.setText(hostnameValue);
                }

                if (message != null)
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        };

        if (Looper.myLooper() == Looper.getMainLooper()) {
            logic.run();
        } else {
            mUiHandler.post(logic);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mDiscoveryInProgress = false;
        startApiDiscovery();
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            mNsdManager.stopServiceDiscovery(mDiscoveryListener);
        } catch (Exception e) {
            Log.e(TAG, "Failed to stop service discovery");
        }
    }

    private void performLogin() {
        // Validate inputs.
        // Validate HOSTNAME/URL
        final String strurl = mHttpHostnameEditText.getText().toString().trim();
        if (strurl.isEmpty()) {
            mHttpHostnameEditText.setError("Please input the HTTP API url");
            return;
        }
        if (!AndroidUtils.validateBaseUrl(strurl)) {
            mHttpHostnameEditText.setError("The provided HTTP API URL is invalid");
            return;
        }
        mHttpHostnameEditText.setError(null);

        // Validate username
        final String username = mHttpUsernameEditText.getText().toString().trim();
        if (username.isEmpty()) {
            mHttpUsernameEditText.setError("Please input your username/email");
            return;
        }
        mHttpUsernameEditText.setError(null);

        // Validate password
        final String password = mHttpPasswordEditText.getText().toString().trim();
        if (password.isEmpty()) {
            mHttpPasswordEditText.setError("Please input your password");
            return;
        }
        mHttpPasswordEditText.setError(null);

        // Execute the login.
        final ProgressDialog dialog = new ProgressDialog(getContext());
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setTitle("Logging in");
        dialog.setCancelable(false);
        dialog.setMessage("Please wait while logging in...");
        dialog.show();

        HttpClientManager.getInstance().asyncLogin(strurl, username, password, new HttpClientManager.Callback<ApiCredentials>() {
            @Override
            public void onSuccess(ApiCredentials creds) {
                AndroidPreferencesManager.storeHttpCredentials(requireContext(), creds);
                dialog.dismiss();
                MainActivityViewModel mainActivityViewModel = new ViewModelProvider(requireActivity()).get(MainActivityViewModel.class);
                mainActivityViewModel.setCredentials(creds);
                NavHostFragment.findNavController(LoginFragment.this).popBackStack();
            }

            @Override
            public void onFailure(Exception result) {
                dialog.dismiss();
                Snackbar.make(mLoginButton, "An error while executing the request.", Snackbar.LENGTH_LONG).show();
            }
        });
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
            if (mDiscoveryInProgress)
                mNsdManager.stopServiceDiscovery(mDiscoveryListener);
            mNsdManager.resolveService(service, mResolveListener);
        }

        @Override
        public void onStartDiscoveryFailed(String serviceType, int errorCode) {
            Log.e(TAG, "Discovery failed: Error code:" + errorCode);
            configureUi(true,  false, null, "Discovery failed");
        }

        @Override
        public void onStopDiscoveryFailed(String serviceType, int errorCode) {
            Log.e(TAG, "Discovery failed: Error code:" + errorCode);
        }

        @Override
        public void onDiscoveryStopped(String serviceType) {
            Log.i(TAG, "Discovery stopped: " + serviceType);
            mDiscoveryInProgress = false;
        }

        @Override
        public void onServiceLost(NsdServiceInfo serviceInfo) {
            Log.e(TAG, "service lost: " + serviceInfo.getServiceType());
            if (mDiscoveryInProgress)
                mNsdManager.stopServiceDiscovery(mDiscoveryListener);
        }
    };

    // mDNS resolver
    private final NsdManager.ResolveListener mResolveListener = new NsdManager.ResolveListener() {
        @Override
        public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
            Log.e(TAG, "Resolve failed" + errorCode);
            configureUi(true,  false, null, "Discovery failed");
        }
        @Override
        public void onServiceResolved(final NsdServiceInfo serviceInfo) {
            Log.e(TAG, "Found local API!. " + serviceInfo);
            String result = "http://" + serviceInfo.getHost().getHostName()+":"+serviceInfo.getPort();
            configureUi(true,  false, result, "Found local API!");
        }
    };

    private class TimeoutTask extends TimerTask {
        @Override
        public void run() {
            configureUi(true,  false, null, "No Local HTTP service found.");
            mTimer = null;
        }
    }
}
