package com.albertogeniola.merossconf;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Base64;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.albertogeniola.merosslib.model.protocol.MessageGetConfigWifiListResponse;
import com.albertogeniola.merosslib.model.protocol.MessageGetSystemAllResponse;
import com.albertogeniola.merosslib.model.protocol.payloads.GetConfigWifiListEntry;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;


public class ConfigFragment extends Fragment {
    public static final String DEVICE_INFO = "DEVICE_INFO";
    public static final String DEVICE_AVAILABLE_WIFIS = "DEVICE_AVAILABLE_WIFIS";

    private Spinner wifiSpinner;
    private MessageGetSystemAllResponse deviceInfo;
    private MessageGetConfigWifiListResponse deviceAvailableWifis;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            deviceInfo = (MessageGetSystemAllResponse) getArguments().getSerializable(DEVICE_INFO);
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

        wifiSpinner.setAdapter(new WifiSpinnerAdapter(ConfigFragment.this.getContext(), deviceAvailableWifis.getPayload().getWifiList()));
    }

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