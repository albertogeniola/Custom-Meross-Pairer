package com.albertogeniola.merossconf;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavHost;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.albertogeniola.merosslib.MerossDeviceAp;
import com.albertogeniola.merosslib.model.protocol.MessageGetConfigWifiListResponse;
import com.albertogeniola.merosslib.model.protocol.MessageGetSystemAllResponse;
import com.albertogeniola.merosslib.model.protocol.payloads.GetSystemAllPayloadAllFirmware;
import com.albertogeniola.merosslib.model.protocol.payloads.GetSystemAllPayloadAllHardrware;


public class InfoFragment extends Fragment {
    public static final String DEVICE = "DEVICE";
    public static final String DEVICE_INFO = "DEVICE_INFO";
    public static final String DEVICE_AVAILABLE_WIFIS = "DEVICE_AVAILABLE_WIFIS";

    private MerossDeviceAp device;

    private TextView discoveredType;
    private TextView discoveredVersion;
    private TextView discoveredChip;
    private TextView discoveredMAC;
    private TextView discoveredUUID;
    private TextView discoveredUserID;
    private TextView discoveredFWVer;
    private TextView discoveredMQTTServer;
    private TextView discoveredMQTTServerPort;
    private TextView discoveredInnerIP;
    private TextView discoveredWifiMac;
    private Button configureButton;

    private MessageGetSystemAllResponse deviceInfo;
    private MessageGetConfigWifiListResponse deviceAvailableWifis;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            deviceInfo = (MessageGetSystemAllResponse) getArguments().getSerializable(DEVICE_INFO);
            deviceAvailableWifis = (MessageGetConfigWifiListResponse) getArguments().getSerializable(DEVICE_AVAILABLE_WIFIS);
            device = (MerossDeviceAp) getArguments().getSerializable(DEVICE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.info_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        discoveredType = view.findViewById(R.id.discoveredType);
        discoveredVersion = view.findViewById(R.id.discoveredVersion);
        discoveredChip = view.findViewById(R.id.discoveredChip);
        discoveredMAC = view.findViewById(R.id.discoveredMAC);
        discoveredUUID = view.findViewById(R.id.discoveredUUID);
        discoveredUserID = view.findViewById(R.id.discoveredUserID);
        discoveredFWVer = view.findViewById(R.id.discoveredFWVer);
        discoveredMQTTServer = view.findViewById(R.id.discoveredMQTTServer);
        discoveredMQTTServerPort = view.findViewById(R.id.discoveredMQTTServerPort);
        discoveredInnerIP = view.findViewById(R.id.discoveredInnerIP);
        discoveredWifiMac = view.findViewById(R.id.discoveredWifiMac);
        configureButton = view.findViewById(R.id.configureButton);

        configureButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                launchConfigFragment();
            }
        });

        loadUiInfo();
    }

    private void launchConfigFragment() {
        Bundle bundle = new Bundle();
        bundle.putSerializable(ConfigFragment.DEVICE, device);
        bundle.putSerializable(ConfigFragment.DEVICE_AVAILABLE_WIFIS, deviceAvailableWifis);
        NavHostFragment.findNavController(InfoFragment.this)
                .navigate(R.id.ConfigFragment, bundle);
    }

    private void loadUiInfo() {
        if (deviceInfo != null) {
            GetSystemAllPayloadAllHardrware hwinfo = deviceInfo.getPayload().getAll().getSystem().getHardware();
            GetSystemAllPayloadAllFirmware fwinfo = deviceInfo.getPayload().getAll().getSystem().getFirmware();

            discoveredType.setText(hwinfo.getType());
            discoveredVersion.setText(hwinfo.getVersion());
            discoveredChip.setText(hwinfo.getChipType());
            discoveredMAC.setText(hwinfo.getMacAddress());
            discoveredUUID.setText(hwinfo.getUuid());
            discoveredUserID.setText("" + (fwinfo.getUserId() == 0 ? "N/A" : fwinfo.getUserId()));
            discoveredFWVer.setText(fwinfo.getVersion());
            discoveredMQTTServer.setText(fwinfo.getServer().isEmpty() ? "N/A" : fwinfo.getServer());
            discoveredMQTTServerPort.setText("" + (fwinfo.getPort() == 0 ? "N/A" : fwinfo.getPort()));
            discoveredInnerIP.setText(fwinfo.getInnerIp().compareTo("0.0.0.0")==0 ? "N/A" : fwinfo.getInnerIp());
            discoveredWifiMac.setText(fwinfo.getWifiMac());
        }
    }
}
