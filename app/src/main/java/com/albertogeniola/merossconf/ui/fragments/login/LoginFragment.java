package com.albertogeniola.merossconf.ui.fragments.login;

import android.app.ProgressDialog;
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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.albertogeniola.merossconf.AndroidPreferencesManager;
import com.albertogeniola.merossconf.AndroidUtils;
import com.albertogeniola.merossconf.MainActivity;
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
    private static final String SERVICE_TYPE = "_meross-api._tcp.";
    private static final  String TAG = "Login";

    // Instance attributes
    private Context mAppContext = null;
    private boolean mDiscoveryInProgress = false;
    private boolean mDiscoveryEnabled = false;
    private boolean mRequiresWifiLocation = false;
    private NsdManager mNsdManager;
    private TextInputLayout mHttpHostnameInputLayout;
    private EditText mHttpHostnameEditText;
    private EditText mHttpUsernameEditText;
    private EditText mHttpPasswordEditText;
    private MaterialButton mLoginButton;
    private MaterialButton mDiscoveryButton;
    private TextView httpLoginIntroText;
    private ImageView loginLogo;

    private Timer mTimer;
    private Handler mUiHandler;

    private CircularProgressIndicator mSearchProgress;
    private IndeterminateDrawable<CircularProgressIndicatorSpec> mProgressIndicatorDrawable;

    public static class Args {
        public static final String HTTP_BROKER_URL = "com.albertogeniola.merossconf.ui.fragments.account.AccountFragment.Args.HTTP_BROKER_URL";
        public static final String HTTP_BROKER_EMAIL = "com.albertogeniola.merossconf.ui.fragments.account.AccountFragment.Args.HTTP_BROKER_EMAIL";
        public static final String HTTP_BROKER_PASSWORD = "com.albertogeniola.merossconf.ui.fragments.account.AccountFragment.Args.HTTP_BROKER_PASSWORD";
        public static final String ENABLE_BROKER_DISCOVERY = "com.albertogeniola.merossconf.ui.fragments.account.AccountFragment.Args.ENABLE_BROKER_DISCOVERY";
        public static final String INTRO_TEXT_RESOURCE_ID = "com.albertogeniola.merossconf.ui.fragments.account.AccountFragment.Args.INTRO_TEXT_RESOURCE_ID";
        public static final String INTRO_IMAGE_RESOURCE_ID = "com.albertogeniola.merossconf.ui.fragments.account.AccountFragment.Args.INTRO_IMAGE_RESOURCE_ID";
        public static final String REQUIRES_WIFI_LOCATION = "com.albertogeniola.merossconf.ui.fragments.account.AccountFragment.Args.REQUIRES_WIFI_LOCATION";
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAppContext = requireContext().getApplicationContext();
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
        httpLoginIntroText = view.findViewById(R.id.httpLoginIntroText);
        loginLogo = view.findViewById(R.id.loginLogo);

        // Configure HostEditText for progress showing
        mSearchProgress = new CircularProgressIndicator(requireContext(), null);
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

        // Configure the UI based on received args, if any.
        Bundle args = getArguments();
        if (args == null)
            args = new Bundle();

        mRequiresWifiLocation = args.getBoolean(Args.REQUIRES_WIFI_LOCATION, false);
        mDiscoveryEnabled = args.getBoolean(Args.ENABLE_BROKER_DISCOVERY, false);

        mHttpHostnameEditText.setText(args.getString(Args.HTTP_BROKER_URL, ""));
        mHttpUsernameEditText.setText(args.getString(Args.HTTP_BROKER_EMAIL, ""));
        mHttpPasswordEditText.setText(args.getString(Args.HTTP_BROKER_PASSWORD, ""));
        mDiscoveryButton.setEnabled(mDiscoveryEnabled);
        mDiscoveryButton.setVisibility(mDiscoveryEnabled ? View.VISIBLE : View.INVISIBLE);
        httpLoginIntroText.setText(args.getInt(Args.INTRO_TEXT_RESOURCE_ID, R.string.login_intro_text));
        loginLogo.setImageResource(args.getInt(Args.INTRO_IMAGE_RESOURCE_ID, R.drawable.login_icon));

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
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }

        if (mDiscoveryInProgress)
            mNsdManager.stopServiceDiscovery(mDiscoveryListener);

        super.onDestroy();
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

                if (message != null) {
                    Toast.makeText(mAppContext, message, Toast.LENGTH_SHORT).show();
                }
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
        ((MainActivity)requireActivity()).setWifiLocationWarnRequired(mRequiresWifiLocation);

        if (mDiscoveryEnabled)
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
            mNsdManager.resolveService(service, mResolveListener);
        }

        @Override
        public void onStartDiscoveryFailed(String serviceType, int errorCode) {
            Log.e(TAG, "Discovery failed: Error code:" + errorCode);
            configureUi(true,  false, null, "Discovery failed");
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
            Log.e(TAG, "service lost: " + serviceInfo.getServiceType());
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
            if (mDiscoveryInProgress)
                mNsdManager.stopServiceDiscovery(mDiscoveryListener);
        }
    }
}
