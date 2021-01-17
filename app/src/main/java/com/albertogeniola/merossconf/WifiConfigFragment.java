package com.albertogeniola.merossconf;

import android.content.Context;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.albertogeniola.merosslib.model.Encryption;
import com.albertogeniola.merosslib.model.protocol.payloads.GetConfigWifiListEntry;
import com.google.android.material.textfield.TextInputLayout;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;


public class WifiConfigFragment extends Fragment {
    private PairActivity parentActivity;

    private Spinner wifiSpinner;
    private TextInputLayout wifiPasswordTextView;
    private ImageButton showPasswordButton;
    private Button nextButton;
    private WifiSpinnerAdapter adapter;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        parentActivity = (PairActivity) getActivity();
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
        adapter = new WifiSpinnerAdapter(WifiConfigFragment.this.getContext(), parentActivity.getDeviceAvailableWifis().getPayload().getWifiList());
        wifiSpinner.setAdapter(adapter);

        wifiPasswordTextView = view.findViewById(R.id.wifi_password);
        nextButton = view.findViewById(R.id.next_button);
        nextButton.setOnClickListener(pairButtonClick);
        showPasswordButton = view.findViewById(R.id.showPasswordButton);
        showPasswordButton.setOnClickListener(showPasswordClick);

    }

    private View.OnClickListener showPasswordClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (wifiPasswordTextView.getEditText().getTransformationMethod() == HideReturnsTransformationMethod.getInstance()) {
                wifiPasswordTextView.getEditText().setTransformationMethod(PasswordTransformationMethod.getInstance());
            } else {
                wifiPasswordTextView.getEditText().setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            }
        }
    };

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


            // Navigate to the next fragment
            parentActivity.setDevice(parentActivity.getDevice());
            parentActivity.setTargetWifiSSID(selectedWifi.getSsid());
            parentActivity.setTargetWifiPassword(selectedWifi.getSsid());
            try {
                parentActivity.setTargetWifiPassword(Base64.encodeToString(wifiPasswordTextView.getEditText().getText().toString().getBytes("utf8"),Base64.NO_WRAP));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                throw new RuntimeException("UTF8 unsupported");
            }

            NavHostFragment.findNavController(WifiConfigFragment.this)
                    .navigate(R.id.mqtt_config_fragment);
        }
    };

    public class WifiSpinnerAdapter extends ArrayAdapter<GetConfigWifiListEntry> {
        private ArrayList<GetConfigWifiListEntry> values;

        public WifiSpinnerAdapter(Context context, List<GetConfigWifiListEntry> values) {
            super(context, R.layout.wifi_dropdown_item, R.id.wifi_name);
            this.values = new ArrayList<>(values);
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

            try {
                wifiName.setText(new String(Base64.decode(value.getSsid(), Base64.DEFAULT), "utf8"));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
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