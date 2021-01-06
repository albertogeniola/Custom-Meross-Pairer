package com.albertogeniola.merossconf;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.albertogeniola.merosslib.model.protocol.MessageGetConfigWifiListResponse;
import com.albertogeniola.merosslib.model.protocol.MessageGetSystemAllResponse;
import com.albertogeniola.merosslib.model.protocol.payloads.GetSystemAllPayloadAllFirmware;
import com.albertogeniola.merosslib.model.protocol.payloads.GetSystemAllPayloadAllHardrware;


public class ConfigureFragment extends Fragment {
    public static final String DEVICE_INFO = "DEVICE_INFO";
    public static final String DEVICE_AVAILABLE_WIFIS = "DEVICE_AVAILABLE_WIFIS";

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
        return inflater.inflate(R.layout.fragment_configure_mqtt, container, false);
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

        loadUiInfo();
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
            discoveredMQTTServer.setText(fwinfo.getServer() == "" ? "N/A" : fwinfo.getServer());
            discoveredMQTTServerPort.setText("" + (fwinfo.getPort() == 0 ? "N/A" : fwinfo.getPort()));
            discoveredInnerIP.setText(fwinfo.getInnerIp());
            discoveredWifiMac.setText(fwinfo.getWifiMac());
        }
    }
}
