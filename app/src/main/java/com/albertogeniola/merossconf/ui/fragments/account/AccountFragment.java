package com.albertogeniola.merossconf.ui.fragments.account;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.albertogeniola.merossconf.AndroidPreferencesManager;
import com.albertogeniola.merossconf.Constants;
import com.albertogeniola.merossconf.MainActivity;
import com.albertogeniola.merossconf.R;
import com.albertogeniola.merossconf.model.HttpClientManager;
import com.albertogeniola.merossconf.ui.MainActivityViewModel;
import com.albertogeniola.merossconf.ui.fragments.login.LoginFragment;
import com.albertogeniola.merosslib.model.http.ApiCredentials;

import org.eclipse.paho.client.mqttv3.util.Strings;

public class AccountFragment extends Fragment {
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        final MainActivityViewModel mainActivityViewModel = new ViewModelProvider(requireActivity()).get(MainActivityViewModel.class);
        View root = inflater.inflate(R.layout.fragment_account, container, false);
        final CardView httpInfoCard = root.findViewById(R.id.httpInfoCard);
        final EditText httpUrlEditText = root.findViewById(R.id.httpUrlEditText);
        final EditText userIdEditText = root.findViewById(R.id.userIdEditText);
        final EditText httpTokenEditText = root.findViewById(R.id.httpTokenEditText);
        final EditText mqttKeyEditText = root.findViewById(R.id.mqttKeyEditText);
        final Button httpLogoutButton = root.findViewById(R.id.httpLogoutButton);
        final Button haBrokerLoginButton = root.findViewById(R.id.haBrokerLoginButton);
        final Button merossCloudLoginButton = root.findViewById(R.id.merossCloudLoginButton);
        final CardView loginCardView = root.findViewById(R.id.loginCard);
        final Button manualSetupButton = root.findViewById(R.id.setManualButton);
        final ImageView loggedInAccountLogoImageView = root.findViewById(R.id.loggedInAccountLogo);

        mainActivityViewModel.getCredentials().observe(getViewLifecycleOwner(), new Observer<ApiCredentials>() {
            @Override
            public void onChanged(ApiCredentials apiCredentials) {
                if (apiCredentials == null || Strings.isEmpty(apiCredentials.getApiServer())) {
                    loggedInAccountLogoImageView.setImageResource(R.drawable.question_mark);
                } else if (apiCredentials.getApiServer().compareTo(Constants.MEROSS_CLOUD_EP)==0) {
                    loggedInAccountLogoImageView.setImageResource(R.drawable.meross_logo);
                } else {
                    loggedInAccountLogoImageView.setImageResource(R.drawable.ha_logo);
                }

                if (apiCredentials == null || Strings.isEmpty(apiCredentials.getApiServer())) {
                    httpUrlEditText.setText("Not set");
                } else {
                    httpUrlEditText.setText(apiCredentials.getApiServer());
                }

                if (apiCredentials == null || Strings.isEmpty(apiCredentials.getUserId())) {
                    userIdEditText.setText("Not set");
                } else {
                    userIdEditText.setText(apiCredentials.getUserId());
                }

                if (apiCredentials == null || Strings.isEmpty(apiCredentials.getToken())) {
                    httpTokenEditText.setText("Not set");
                } else {
                    httpTokenEditText.setText(apiCredentials.getToken());
                }

                if (apiCredentials == null || Strings.isEmpty(apiCredentials.getKey())) {
                    mqttKeyEditText.setText("Not set (empty string)");
                } else {
                    mqttKeyEditText.setText(apiCredentials.getKey());
                }

                if (apiCredentials != null && apiCredentials.isManuallySet()) {
                    httpLogoutButton.setText("Discard");
                } else {
                    httpLogoutButton.setText("Logout");
                }

                httpLogoutButton.setEnabled(apiCredentials != null);
                httpInfoCard.setVisibility(apiCredentials == null ? View.GONE : View.VISIBLE);
                loginCardView.setVisibility(apiCredentials == null ? View.VISIBLE : View.GONE);
                haBrokerLoginButton.setEnabled(apiCredentials == null);
                merossCloudLoginButton.setEnabled(apiCredentials == null);
                manualSetupButton.setVisibility(apiCredentials == null ? View.VISIBLE : View.GONE);
            }
        });

        haBrokerLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle args = new Bundle();
                args.putBoolean(LoginFragment.Args.ENABLE_BROKER_DISCOVERY, true);
                args.putString(LoginFragment.Args.HTTP_BROKER_EMAIL, Constants.HA_ADDON_DEFAULT_EMAIL);
                args.putString(LoginFragment.Args.HTTP_BROKER_PASSWORD, Constants.HA_ADDON_DEFAULT_PASSWORD);
                args.putInt(LoginFragment.Args.INTRO_TEXT_RESOURCE_ID, R.string.login_intro_ha_broker);
                args.putInt(LoginFragment.Args.INTRO_IMAGE_RESOURCE_ID, R.drawable.ha_logo);
                args.putBoolean(LoginFragment.Args.REQUIRES_WIFI_LOCATION, true);
                NavHostFragment.findNavController(AccountFragment.this).navigate(R.id.login_fragment, args);
            }
        });
        merossCloudLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle args = new Bundle();
                args.putBoolean(LoginFragment.Args.ENABLE_BROKER_DISCOVERY, false);
                args.putString(LoginFragment.Args.HTTP_BROKER_URL, Constants.MEROSS_CLOUD_EP);
                args.putInt(LoginFragment.Args.INTRO_TEXT_RESOURCE_ID, R.string.login_intro_text_meross);
                args.putInt(LoginFragment.Args.INTRO_IMAGE_RESOURCE_ID, R.drawable.meross_logo);
                args.putBoolean(LoginFragment.Args.REQUIRES_WIFI_LOCATION, false);
                NavHostFragment.findNavController(AccountFragment.this).navigate(R.id.login_fragment, args);
            }
        });

        manualSetupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavHostFragment.findNavController(AccountFragment.this).navigate(R.id.manual_setup_fragment);
            }
        });

        httpLogoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // If the stored credentials is just manual, simply discard it
                if (mainActivityViewModel.getCredentials().getValue().isManuallySet()) {
                    mainActivityViewModel.setCredentials(null);
                    AndroidPreferencesManager.removeHttpCredentials(requireContext());
                    return;
                }

                // If the stored credentials has been obtained by login, ask for confirmation first
                AlertDialog dialog = new AlertDialog.Builder(requireActivity()).setMessage("Are you sure you want to discard current HTTP credentials?").setTitle("Confirm").setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, int which) {
                        HttpClientManager.getInstance().asyncLogout(new HttpClientManager.Callback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                mainActivityViewModel.setCredentials(null);
                                AndroidPreferencesManager.removeHttpCredentials(requireContext());
                                dialog.dismiss();
                            }

                            @Override
                            public void onFailure(Exception ex) {
                                Toast.makeText(requireContext(),"An error occurred while logging out: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
                                AndroidPreferencesManager.removeHttpCredentials(requireContext());
                                mainActivityViewModel.setCredentials(null);
                                dialog.dismiss();
                            }
                        });
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).create();
                dialog.show();
            }
        });

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        ((MainActivity)requireActivity()).setWifiLocationWarnRequired(false);
    }

}
