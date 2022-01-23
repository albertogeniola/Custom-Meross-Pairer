package com.albertogeniola.merossconf.ui.fragments.login;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.albertogeniola.merossconf.AndroidPreferencesManager;
import com.albertogeniola.merossconf.R;
import com.albertogeniola.merossconf.ui.MainActivityViewModel;
import com.albertogeniola.merosslib.model.http.ApiCredentials;
import com.google.android.material.button.MaterialButton;

import org.eclipse.paho.client.mqttv3.util.Strings;


public class ManualSetupFragment extends Fragment {
    private EditText mManualUserIdEditText;
    private EditText mManualUserKeyEditText;

    public ManualSetupFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_manual_setup, container, false);
        mManualUserIdEditText = v.findViewById(R.id.manualUserIdEditText);
        mManualUserKeyEditText = v.findViewById(R.id.manualUserKeyEditText);
        MaterialButton mSaveManualConfigButton = v.findViewById(R.id.saveManualConfigButton);

        mSaveManualConfigButton.setOnClickListener(this.mSaveListener);

        // Inflate the layout for this fragment
        return v;
    }

    private final View.OnClickListener mSaveListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // Args check
            String userId = mManualUserIdEditText.getText().toString();
            String userKey = mManualUserKeyEditText.getText().toString();
            Integer userIdInt = null;

            // Check UserId
            if (Strings.isEmpty(userId)) {
                mManualUserIdEditText.setError("Missing UserId");
                return;
            }
            else {
                try {
                    userIdInt = Integer.parseInt(userId);
                    if (userIdInt<0)
                        throw new NumberFormatException();
                    else
                        mManualUserIdEditText.setError(null);
                } catch (NumberFormatException ex) {
                    mManualUserIdEditText.setError("UserId must be a positive integer");
                }
            }

            // Store
            MainActivityViewModel mainActivityViewModel = new ViewModelProvider(requireActivity()).get(MainActivityViewModel.class);
            ApiCredentials creds = new ApiCredentials(userId, userKey);
            mainActivityViewModel.setCredentials(creds);
            AndroidPreferencesManager.storeHttpCredentials(requireContext(), creds);

            // Navigate back
            requireActivity().getSupportFragmentManager().popBackStack();
        }
    };


}