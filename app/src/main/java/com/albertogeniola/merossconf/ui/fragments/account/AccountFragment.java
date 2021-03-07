package com.albertogeniola.merossconf.ui.fragments.account;

import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.core.os.HandlerCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.albertogeniola.merossconf.AndroidPreferencesManager;
import com.albertogeniola.merossconf.AndroidUtils;
import com.albertogeniola.merossconf.R;
import com.albertogeniola.merossconf.ui.MainActivityViewModel;
import com.albertogeniola.merosslib.MerossHttpClient;
import com.albertogeniola.merosslib.model.http.ApiCredentials;
import com.albertogeniola.merosslib.model.http.exceptions.HttpApiException;
import com.albertogeniola.merosslib.model.http.exceptions.HttpInvalidCredentials;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AccountFragment extends Fragment {
    private Executor mExecutor = Executors.newSingleThreadExecutor();
    private Handler uiHandler = HandlerCompat.createAsync(Looper.getMainLooper());

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
        final Button httpLoginButton = root.findViewById(R.id.httpLoginButton);
        final CardView loginCardView = root.findViewById(R.id.loginCard);

        mainActivityViewModel.getCredentials().observe(getViewLifecycleOwner(), new Observer<ApiCredentials>() {
            @Override
            public void onChanged(ApiCredentials apiCredentials) {
                httpUrlEditText.setText(apiCredentials == null ? "N/A" : apiCredentials.getApiServer());
                userIdEditText.setText(apiCredentials == null ? "N/A" : apiCredentials.getUserId());
                httpTokenEditText.setText(apiCredentials == null ? "N/A" : apiCredentials.getToken());
                mqttKeyEditText.setText(apiCredentials == null ? "N/A" : apiCredentials.getKey());
                httpLogoutButton.setEnabled(apiCredentials != null);
                httpInfoCard.setVisibility(apiCredentials == null ? View.GONE : View.VISIBLE);
                loginCardView.setVisibility(apiCredentials == null ? View.VISIBLE : View.GONE);
                httpLoginButton.setEnabled(apiCredentials == null);
            }
        });

        httpLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavHostFragment.findNavController(AccountFragment.this).navigate(R.id.login_fragment);
            }
        });

        httpLogoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog dialog = new AlertDialog.Builder(getActivity()).setMessage("Are you sure you want to discard current HTTP credentials?").setTitle("Confirm").setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, int which) {
                        new AsyncTask<Void, Void, String>() {

                            @Override
                            protected String doInBackground(Void... voids) {
                                try {
                                    MerossHttpClient.getInstance().logout();
                                    return null;
                                } catch (HttpInvalidCredentials e) {
                                    e.printStackTrace();
                                    return "Invalid Credentials";
                                } catch (HttpApiException | IOException e) {
                                    e.printStackTrace();
                                    return "An error occurred";
                                }
                            }

                            @Override
                            protected void onPostExecute(String err) {
                                if (err != null) {
                                    Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show();
                                }

                                mainActivityViewModel.setCredentials(null);
                                AndroidPreferencesManager.removeHttpCredentials(requireContext());
                                dialog.dismiss();
                            }
                        }.execute();
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

}
