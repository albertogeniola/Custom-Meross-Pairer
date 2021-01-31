package com.albertogeniola.merossconf;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;

import com.albertogeniola.merosslib.MerossHttpClient;
import com.albertogeniola.merosslib.model.http.exceptions.HttpApiException;
import com.albertogeniola.merosslib.model.http.exceptions.HttpInvalidCredentials;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;


public class LoginActivity extends AppCompatActivity {

    private EditText httpHostnameEditText;
    private EditText httpUsernameEditText;
    private EditText httpPasswordEditText;
    private Button loginButton;
    private ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor();
    private Handler uiHandler = new Handler(Looper.getMainLooper());

    public final static String RESULT_MEROSS_HTTP_CLIENT = "result_meross_http_client";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Bind views
        CheckBox showPasswordCheckBox = findViewById(R.id.showPasswordCheckBox);
        httpPasswordEditText = ((TextInputLayout)findViewById(R.id.httpPasswordEditText)).getEditText();
        httpHostnameEditText = ((TextInputLayout)findViewById(R.id.httpHostnameEditText)).getEditText();
        httpUsernameEditText = ((TextInputLayout)findViewById(R.id.httpUsernameEditText)).getEditText();
        loginButton = findViewById(R.id.loginButton);

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
        final AlertDialog dialog = new ProgressDialog.Builder(this).setTitle("Logging in").setMessage("Please wait while logging in...").create();
        dialog.show();
        worker.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    MerossHttpClient client = MerossHttpClient.getByUserAndPassword(strurl, username, password);
                    dialog.dismiss();

                    // Return the httpClient
                    Intent data = new Intent();
                    data.putExtra(RESULT_MEROSS_HTTP_CLIENT, client);
                    setResult(RESULT_OK, data);
                    finish();
                } catch (IOException e) {
                    e.printStackTrace();
                    uiHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            dialog.dismiss();
                            Snackbar.make(loginButton, "A network error occurred while executing the request. ", Snackbar.LENGTH_LONG).show();
                        }
                    });
                } catch (final HttpApiException e) {
                    e.printStackTrace();
                    uiHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            dialog.dismiss();
                            Snackbar.make(loginButton, "The remote HTTP Api returned Error Code " + e.getCode().name(), Snackbar.LENGTH_LONG).show();
                        }
                    });
                } catch (HttpInvalidCredentials e) {
                    e.printStackTrace();
                    uiHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            dialog.dismiss();
                            Snackbar.make(loginButton, "Invalid username/password combination.", Snackbar.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });
    }
}
