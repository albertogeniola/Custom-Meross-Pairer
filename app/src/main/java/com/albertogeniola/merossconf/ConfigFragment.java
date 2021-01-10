package com.albertogeniola.merossconf;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.albertogeniola.merosslib.MerossDeviceAp;
import com.albertogeniola.merosslib.model.Encryption;
import com.albertogeniola.merosslib.model.protocol.MessageGetConfigWifiListResponse;
import com.albertogeniola.merosslib.model.protocol.payloads.GetConfigWifiListEntry;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;


public class ConfigFragment extends Fragment {
    public static final String DEVICE = "DEVICE";
    public static final String DEVICE_AVAILABLE_WIFIS = "DEVICE_AVAILABLE_WIFIS";

    private static final String DEFAULT_KEY = "";
    private static final String DEFAULT_USER_ID = "";

    private Spinner wifiSpinner;
    private TextView wifiPasswordTextView;
    private TextView mqttHostTextView;
    private TextView mqttPortTextView;
    private Button pairButton;
    private MerossDeviceAp device;
    private MessageGetConfigWifiListResponse deviceAvailableWifis;
    private WifiSpinnerAdapter adapter;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            device = (MerossDeviceAp) getArguments().getSerializable(DEVICE);
            deviceAvailableWifis = (MessageGetConfigWifiListResponse) getArguments().getSerializable(DEVICE_AVAILABLE_WIFIS);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.config_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        wifiSpinner = view.findViewById(R.id.wifiListSpinner);
        adapter = new WifiSpinnerAdapter(ConfigFragment.this.getContext(), deviceAvailableWifis.getPayload().getWifiList());
        wifiSpinner.setAdapter(adapter);

        wifiPasswordTextView = view.findViewById(R.id.wifi_password);
        mqttHostTextView = view.findViewById(R.id.mqtt_hostname);
        mqttPortTextView = view.findViewById(R.id.mqtt_port);
        pairButton = view.findViewById(R.id.pair_button);
        pairButton.setOnClickListener(pairButtonClick);
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
            wifiPasswordTextView.setError(selectedWifi.getEncryption() != Encryption.OPEN && wifiPasswordTextView.getText().toString().isEmpty() ? "That wifi requires a password." : null);

            // make sure the host is valid
            String hostnameStr = mqttHostTextView.getText().toString();
            mqttHostTextView.setError(hostnameStr.isEmpty() ? "Invalid mqtt host" : null);

            String portstr = mqttPortTextView.getText().toString();
            int port;
            try {
                port = Integer.parseInt(portstr);
                if (port<1 || port > 65535)
                    throw new NumberFormatException();
                mqttPortTextView.setError(null);
            } catch (NumberFormatException e) {
                mqttPortTextView.setError("The MQTT port is invalid");
                return;
            }

            // Navigate to the next fragment
            Bundle bundle = new Bundle();
            bundle.putSerializable(PairFragment.DEVICE, device);
            bundle.putString(PairFragment.HOSTNAME, hostnameStr);
            bundle.putInt(PairFragment.PORT, port);
            bundle.putString(PairFragment.WIFI_SSID_BASE64, selectedWifi.getSsid());
            try {
                bundle.putString(PairFragment.WIFI_PASSWORD_BASE64, Base64.encodeToString(wifiPasswordTextView.getText().toString().getBytes("utf8"),Base64.NO_WRAP));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                throw new RuntimeException("UTF8 unsupported");
            }

            NavHostFragment.findNavController(ConfigFragment.this)
                    .navigate(R.id.PairFragment, bundle);
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