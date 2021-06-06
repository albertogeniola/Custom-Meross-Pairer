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
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.CircularProgressIndicatorSpec;
import com.google.android.material.progressindicator.IndeterminateDrawable;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import static com.albertogeniola.merossconf.AndroidUtils.dpToPx;


public class LoginFragment extends Fragment {
    // Constants
    private static final String SERVICE_TYPE = "_meross-local-api._tcp.";
    private static final  String TAG = "Login";

    // Instance attributes
    private NsdManager mNsdManager;
    private WifiManager mWifiManager;
    private TextInputLayout mHttpHostnameInput;
    private EditText mHttpHostnameEditText;
    private EditText mHttpUsernameEditText;
    private EditText mHttpPasswordEditText;
    private Button mLoginButton;

    private Handler uiHandler;

    private CircularProgressIndicator mSearchProgress;


    public LoginFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mNsdManager = (NsdManager) requireContext().getSystemService(Context.NSD_SERVICE);
        mWifiManager = (WifiManager) requireContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        uiHandler = new Handler(this.getContext().getMainLooper());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        // Bind views
        CheckBox showPasswordCheckBox = view.findViewById(R.id.showPasswordCheckBox);
        mHttpHostnameInput = (TextInputLayout)view.findViewById(R.id.httpHostnameEditText);
        mHttpHostnameEditText = ((TextInputLayout)view.findViewById(R.id.httpHostnameEditText)).getEditText();
        mHttpPasswordEditText = ((TextInputLayout)view.findViewById(R.id.httpPasswordEditText)).getEditText();
        mHttpUsernameEditText = ((TextInputLayout)view.findViewById(R.id.httpUsernameEditText)).getEditText();
        mLoginButton = view.findViewById(R.id.loginButton);

        // Configure HostEditText for progress showing
        mSearchProgress = new CircularProgressIndicator(this.requireActivity(), null);
        mSearchProgress.setIndicatorSize((int)dpToPx(requireContext(), 15));
        mSearchProgress.setIndeterminate(true);

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

        // Setup the edit-text to hide the password characters
        showPasswordCheckBox.setChecked(false);
        mHttpPasswordEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());

        return view;
    }

    private void notifyDiscoveryEnded(@Nullable final String result, @Nullable final String message) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                mHttpHostnameInput.setEndIconMode(TextInputLayout.END_ICON_NONE);
                mHttpHostnameInput.setEndIconDrawable(null);
                mHttpHostnameEditText.setText(result == null ? "" : result);
                mHttpHostnameEditText.setEnabled(true);

                if (result!=null && message==null) {
                    Snackbar.make(getView(),"Local Meross HTTP service found!", Snackbar.LENGTH_SHORT)
                            .setAnchorView(mHttpHostnameInput)
                            .show();
                } else if (message != null) {
                    Snackbar.make(getView(), message, Snackbar.LENGTH_SHORT).show();
                }
            }
        };

        if (Looper.myLooper() == Looper.getMainLooper())
            r.run();
        else
            uiHandler.post(r);
    }

    private void startApiDiscovery() {
        // Setup UI
        IndeterminateDrawable<CircularProgressIndicatorSpec> progressIndicatorDrawable = mSearchProgress.getIndeterminateDrawable();
        mHttpHostnameInput.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
        mHttpHostnameInput.setEndIconDrawable(progressIndicatorDrawable);
        mHttpHostnameEditText.setText("Searching...");
        mHttpHostnameEditText.setEnabled(false);

        // Start mDNS discovery
        mNsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
    }

    @Override
    public void onResume() {
        super.onResume();
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
        }

        @Override
        public void onServiceFound(NsdServiceInfo service) {
            Log.d(TAG, "Service discovery success" + service);
            mNsdManager.resolveService(service, mResolveListener);
        }

        @Override
        public void onStartDiscoveryFailed(String serviceType, int errorCode) {
            Log.e(TAG, "Discovery failed: Error code:" + errorCode);
            LoginFragment.this.notifyDiscoveryEnded(null, "Automatic discovery failed");
        }

        @Override
        public void onStopDiscoveryFailed(String serviceType, int errorCode) {
            Log.e(TAG, "Discovery failed: Error code:" + errorCode);
            LoginFragment.this.notifyDiscoveryEnded(null, null);
        }

        @Override
        public void onDiscoveryStopped(String serviceType) {
            Log.i(TAG, "Discovery stopped: " + serviceType);
            LoginFragment.this.notifyDiscoveryEnded(null, null);
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
            LoginFragment.this.notifyDiscoveryEnded(null, "Automatic discovery failed");
        }
        @Override
        public void onServiceResolved(final NsdServiceInfo serviceInfo) {
            Log.e(TAG, "Resolve Succeeded. " + serviceInfo);
            notifyDiscoveryEnded("http://" + serviceInfo.getHost().getHostName()+":"+serviceInfo.getPort(), null);
        }
    };
}
