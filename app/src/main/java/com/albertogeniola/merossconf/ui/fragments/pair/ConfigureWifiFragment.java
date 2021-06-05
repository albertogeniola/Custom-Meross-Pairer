package com.albertogeniola.merossconf.ui.fragments.pair;

import android.content.Context;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
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
import com.google.android.material.textfield.TextInputLayout;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;


public class ConfigureWifiFragment extends Fragment {
    private PairActivityViewModel pairActivityViewModel;

    private Spinner wifiSpinner;
    private TextInputLayout wifiPasswordTextView;
    private WifiSpinnerAdapter adapter;
    private boolean mSavePassword;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pairActivityViewModel = new ViewModelProvider(requireActivity()).get(PairActivityViewModel.class);
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
        Button nextButton = view.findViewById(R.id.next_button);
        nextButton.setOnClickListener(pairButtonClick);
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

    private View.OnClickListener pairButtonClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // Validate the configuration...
            if (wifiSpinner.getSelectedItemPosition() < 0) {
                Toast.makeText(getContext(), "Please select a Wifi AP from the dropdown", Toast.LENGTH_LONG).show();
                return;
            }

            // If the wifi requires a password, make sure the user inputed one.
            GetConfigWifiListEntry selectedWifi = adapter.getItem(wifiSpinner.getSelectedItemPosition());
            if (selectedWifi.getEncryption() != Encryption.OPEN && wifiPasswordTextView.getEditText().getText().toString().isEmpty()) {
                wifiPasswordTextView.setError("That wifi requires a password.");
                return ;
            } else {
                wifiPasswordTextView.setError(null);
            }

            String clearPassword = wifiPasswordTextView.getEditText().getText().toString();
            WifiConfiguration conf = new WifiConfiguration(selectedWifi, clearPassword);
            pairActivityViewModel.setMerossWifiConfiguration(conf);

            // Save the password
            if (mSavePassword)
                AndroidPreferencesManager.storeWifiStoredPassword(requireContext(), selectedWifi.getBssid(), clearPassword);

            // Navigate to the next fragment
            NavHostFragment.findNavController(ConfigureWifiFragment.this)
                    .navigate(R.id.ConfigureMqttFragment);
        }
    };

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
}