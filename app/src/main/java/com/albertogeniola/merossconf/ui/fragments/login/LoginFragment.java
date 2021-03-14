package com.albertogeniola.merossconf.ui.fragments.login;

import android.app.ProgressDialog;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;

import com.albertogeniola.merossconf.AndroidPreferencesManager;
import com.albertogeniola.merossconf.AndroidUtils;
import com.albertogeniola.merossconf.R;
import com.albertogeniola.merossconf.model.HttpClientManager;
import com.albertogeniola.merossconf.ui.MainActivityViewModel;
import com.albertogeniola.merosslib.model.http.ApiCredentials;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

public class LoginFragment extends Fragment {

    private EditText httpHostnameEditText;
    private EditText httpUsernameEditText;
    private EditText httpPasswordEditText;
    private Button loginButton;

    public LoginFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        // Bind views
        CheckBox showPasswordCheckBox = view.findViewById(R.id.showPasswordCheckBox);
        httpPasswordEditText = ((TextInputLayout)view.findViewById(R.id.httpPasswordEditText)).getEditText();
        httpHostnameEditText = ((TextInputLayout)view.findViewById(R.id.httpHostnameEditText)).getEditText();
        httpUsernameEditText = ((TextInputLayout)view.findViewById(R.id.httpUsernameEditText)).getEditText();
        loginButton = view.findViewById(R.id.loginButton);
        // Show/Hide password logic
        showPasswordCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (httpPasswordEditText.getTransformationMethod() == HideReturnsTransformationMethod.getInstance()) {
                    httpPasswordEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                } else {
                    httpPasswordEditText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                }
            }
        });

        // Login button logic
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performLogin();
            }
        });

        // Setup the edit-text to hide the password characters
        showPasswordCheckBox.setChecked(false);
        httpPasswordEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());

        return view;
    }

    private void performLogin() {
        // Validate inputs.
        // Validate HOSTNAME/URL
        final String strurl = httpHostnameEditText.getText().toString().trim();
        if (strurl.isEmpty()) {
            httpHostnameEditText.setError("Please input the HTTP API url");
            return;
        }
        if (!AndroidUtils.validateBaseUrl(strurl)) {
            httpHostnameEditText.setError("The provided HTTP API URL is invalid");
            return;
        }
        httpHostnameEditText.setError(null);

        // Validate username
        final String username = httpUsernameEditText.getText().toString().trim();
        if (username.isEmpty()) {
            httpUsernameEditText.setError("Please input your username/email");
            return;
        }
        httpUsernameEditText.setError(null);

        // Validate password
        final String password = httpPasswordEditText.getText().toString().trim();
        if (password.isEmpty()) {
            httpPasswordEditText.setError("Please input your password");
            return;
        }
        httpPasswordEditText.setError(null);

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
                Snackbar.make(loginButton, "An error while executing the request.", Snackbar.LENGTH_LONG).show();
            }
        });
    }
}
