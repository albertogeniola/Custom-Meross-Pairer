package com.albertogeniola.merossconf;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.albertogeniola.merosslib.model.protocol.MessageGetSystemAllResponse;
import com.albertogeniola.merosslib.model.protocol.payloads.GetSystemAllPayloadAllFirmware;
import com.albertogeniola.merosslib.model.protocol.payloads.GetSystemAllPayloadAllHardrware;


public class InfoFragment extends Fragment {
    private PairActivity parentActivity;

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        parentActivity = (PairActivity) getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_info, container, false);
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
        NavController ctrl = NavHostFragment.findNavController(InfoFragment.this);
        ctrl.navigate(R.id.ConfigFragment);
    }

    private void loadUiInfo() {
        MessageGetSystemAllResponse deviceInfo = parentActivity.getDeviceInfo();
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
