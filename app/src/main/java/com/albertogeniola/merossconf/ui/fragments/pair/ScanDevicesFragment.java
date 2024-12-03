package com.albertogeniola.merossconf.ui.fragments.pair;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.CHANGE_NETWORK_STATE;
import static android.Manifest.permission.CHANGE_WIFI_STATE;
import static android.Manifest.permission.NEARBY_WIFI_DEVICES;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.albertogeniola.merossconf.AndroidUtils;
import com.albertogeniola.merossconf.MerossUtils;
import com.albertogeniola.merossconf.R;
import com.albertogeniola.merossconf.model.MerossDeviceAp;
import com.albertogeniola.merossconf.model.WifiLocationStatus;
import com.albertogeniola.merossconf.ui.PairActivityViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class ScanDevicesFragment extends Fragment {
    private static final int REQUEST_PERMISSION_CODE = 1;
    private static final String TAG = "ScanDevicesFragment";

    private PairActivityViewModel pairActivityViewModel;

    private WifiManager wifiManager = null;
    private LocationManager locationManager = null;
    private ProgressBar scanningProgressBar;
    private FloatingActionButton fab;
    private MerossWifiScanAdapter adapter = new MerossWifiScanAdapter();
    private SwipeRefreshLayout swipeContainer;
    private Handler uiHandler;
    private boolean scanning;
    private ActivityResultLauncher<String[]> multiplePermissionLauncher;

    private static String[] REQUIRED_PERMISSIONS = null;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (android.os.Build.VERSION.SDK_INT > 32){
            REQUIRED_PERMISSIONS = new String[] {
                    NEARBY_WIFI_DEVICES,
                    ACCESS_FINE_LOCATION,
                    ACCESS_COARSE_LOCATION,
                    CHANGE_WIFI_STATE,
                    CHANGE_NETWORK_STATE
            };
        } else{
            // do something for phones running an SDK before lollipop
            REQUIRED_PERMISSIONS = new String[] {
                    ACCESS_FINE_LOCATION,
                    ACCESS_COARSE_LOCATION,
                    CHANGE_WIFI_STATE,
                    CHANGE_NETWORK_STATE };
        }

        this.wifiManager = (WifiManager) getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        this.locationManager = (LocationManager)getContext().getSystemService(Context.LOCATION_SERVICE);
        this.uiHandler = new Handler(Looper.getMainLooper());
        scanning = false;
        pairActivityViewModel = new ViewModelProvider(requireActivity()).get(PairActivityViewModel.class);

        multiplePermissionLauncher = registerForActivityResult(new ActivityResultContracts
                .RequestMultiplePermissions(), isGranted -> {
            Log.d("PERMISSIONS", "Launcher result: " + isGranted.toString());
            if (isGranted.containsValue(false)) {
                Log.d("PERMISSIONS", "At least one of the permissions was not granted, launching again...");
                Toast.makeText(requireContext(), "This app won't work without required permissions", Toast.LENGTH_LONG).show();
            } else {
                startScan();
            }
        });
        multiplePermissionLauncher.launch(REQUIRED_PERMISSIONS);
    }

    private void askPermissions(ActivityResultLauncher<String[]> multiplePermissionLauncher) {
        if (!hasPermissions(REQUIRED_PERMISSIONS)) {
            Log.d("PERMISSIONS", "Launching multiple contract permission launcher for ALL required permissions");
            multiplePermissionLauncher.launch(REQUIRED_PERMISSIONS);
        } else {
            Log.d("PERMISSIONS", "All permissions are already granted");
        }
    }

    private boolean hasPermissions(String[] permissions) {
        if (permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                    Log.d("PERMISSIONS", "Permission is not granted: " + permission);
                    return false;
                }
                Log.d("PERMISSIONS", "Permission already granted: " + permission);
            }
            return true;
        }
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_scan, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        requireContext().registerReceiver(wifiScanReceiver, intentFilter);

        checkWifiAndLocation();
    }

    @Override
    public void onPause() {
        super.onPause();
        requireContext().unregisterReceiver(wifiScanReceiver);
    }

    private boolean checkWifiAndLocation() {
        // Show an error message if wifi is not enabled
        if (!AndroidUtils.isWifiEnabled(requireContext())) {
            Snackbar.make(getView(), "Please enable Wifi network to perform the scan", Snackbar.LENGTH_LONG).show();
            return false;
        }

        if (!AndroidUtils.isLocationEnabled(getContext())) {
            Snackbar.make(getView(), "Please enable Location services to perform the scan", Snackbar.LENGTH_LONG).show();
            return false;
        }

        return true;
    }


    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        swipeContainer = view.findViewById(R.id.swipeContainer);
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Your code to refresh the list here.
                // Make sure you call swipeContainer.setRefreshing(false)
                // once the network request has completed successfully.
                swipeContainer.setRefreshing(false);
                startScan();
            }
        });

        scanningProgressBar = view.findViewById(R.id.scanningProgressSpinner);
        final RecyclerView recyclerView = view.findViewById(R.id.wifiList);
        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(llm);
        recyclerView.setAdapter(adapter);

        fab = view.findViewById(R.id.scan_button);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startScan();
            }
        });

        final TextView enableWifiAndLocationTextView = view.findViewById(R.id.enableWifiAndLocationTextView);
        pairActivityViewModel.getWifiLocationStatus().observe(requireActivity(), new Observer<WifiLocationStatus>() {
            @Override
            public void onChanged(WifiLocationStatus wifiLocationStatus) {
                boolean wifiOk = wifiLocationStatus.getWifiEnabledOrEnabling()!=null && wifiLocationStatus.getWifiEnabledOrEnabling();
                boolean locationOk = wifiLocationStatus.getLocationEnabledOrEnabling()!=null && wifiLocationStatus.getLocationEnabledOrEnabling();
                enableWifiAndLocationTextView.setVisibility(wifiOk && locationOk ? View.GONE:View.VISIBLE);
                recyclerView.setVisibility(wifiOk && locationOk ? View.VISIBLE : View.GONE);
                fab.setEnabled(wifiOk && locationOk);
            }
        });
    }

    private final BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            boolean success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);
            if (success) {
                scanSuccess();
            } else {
                // scan failure handling
                scanFailure();
            }
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        startScan();
    }

    private void startScan() {
        if (scanning) {
            Toast.makeText(ScanDevicesFragment.this.getContext(), "Scan already in progress.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!checkWifiAndLocation()) {
            return;
        }

        if (!AndroidUtils.checkPermissions(requireContext(), REQUIRED_PERMISSIONS)) {
            AlertDialog.Builder permissionAlert = new AlertDialog.Builder(requireContext());
            permissionAlert.setTitle("Permission requests");
            permissionAlert.setMessage("Wifi scanning requires access to geolocation services (this is a requirement on Android).");
            permissionAlert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    askPermissions(multiplePermissionLauncher);
                    //requestPermissions(REQUIRED_PERMISSIONS, REQUEST_PERMISSION_CODE);
                }
            });
            permissionAlert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Toast.makeText(getContext(), "Aborted Wifi scan: permissions denied.", Toast.LENGTH_SHORT).show();
                }
            });
            permissionAlert.show();

        } else {
            fab.hide();
            scanning = true;
            scanningProgressBar.setVisibility(View.VISIBLE);
            boolean success = wifiManager.startScan();
            // Setup a timer
            uiHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    scanning = false;
                    scanningProgressBar.setVisibility(View.GONE);
                    fab.show();
                }
            }, 10000);

            if (!success) {
                scanFailure();
            }
        }
    }

    private void updateScanData(List<ScanResult> wifiNetworks) {
        // Only filter access points that match the Meross
        ArrayList<ScanResult> data = new ArrayList<>();
        for (ScanResult r : wifiNetworks) {
            if (MerossUtils.isMerossAp(r.SSID))
                data.add(r);
        }

        adapter.updateData(data);
        fab.show();
        scanningProgressBar.setVisibility(View.GONE);
        this.scanning = false;
    }

    private void scanSuccess() {
        updateScanData(wifiManager.getScanResults());
    }

    private void scanFailure() {
        // handle failure: new scan did NOT succeed
        // consider using old scan results: these are the OLD results!
        updateScanData(wifiManager.getScanResults());
        fab.show();
        scanningProgressBar.setVisibility(View.GONE);
        Toast.makeText(getContext(), "Scan failed wifi networks", Toast.LENGTH_SHORT).show();
        this.scanning = false;
    }

    class MerossWifiScanAdapter extends RecyclerView.Adapter<MerossWifiScanAdapter.MyViewHolder>{
        private ArrayList<ScanResult> scanResult;

        MerossWifiScanAdapter(){
            this.scanResult = new ArrayList<>();
        }

        public void updateData(List<ScanResult> data) {
            this.scanResult.clear();
            this.scanResult.addAll(data);
            notifyDataSetChanged();
        }

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            final View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.meross_wifi_scan_result, parent, false);
            return new MyViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final MyViewHolder holder, int position) {
            final ScanResult sr = this.scanResult.get(position);
            holder.updateScanResult(sr);
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MerossDeviceAp targetAp = new MerossDeviceAp(sr.SSID,sr.BSSID);
                    pairActivityViewModel.setMerossPairingAp(targetAp);
                    NavHostFragment
                            .findNavController(ScanDevicesFragment.this)
                            .navigate(R.id.action_scan_to_fetchDeviceInfo,null, new NavOptions.Builder().setEnterAnim(android.R.animator.fade_in).setExitAnim(android.R.animator.fade_out).build());
                }
            });
        }

        @Override
        public int getItemCount() {
            return scanResult.size();
        }

        class MyViewHolder extends RecyclerView.ViewHolder{
            private final TextView wifiName;
            private final TextView bssidName;
            private final ImageView signalStrength;
            private ScanResult scanResult;

            MyViewHolder(View itemView) {
                super(itemView);
                wifiName = (TextView) itemView.findViewById(R.id.ssid);
                bssidName = (TextView) itemView.findViewById(R.id.bssid);
                signalStrength = (ImageView)itemView.findViewById(R.id.wifiSignalStrength);
            }

            public void updateScanResult(ScanResult sr) {
                this.scanResult = sr;
                this.wifiName.setText(sr.SSID);
                this.bssidName.setText(sr.BSSID);
                int signalLevel = sr.level;
                if (signalLevel>=-40)
                    this.signalStrength.setImageResource(R.drawable.ic_signal_wifi_4_bar_black_24dp);
                else if (signalLevel>=-50)
                    this.signalStrength.setImageResource(R.drawable.ic_signal_wifi_3_bar_black_24dp);
                else if (signalLevel>=-60)
                    this.signalStrength.setImageResource(R.drawable.ic_signal_wifi_2_bar_black_24dp);
                else if (signalLevel>=-70)
                    this.signalStrength.setImageResource(R.drawable.ic_signal_wifi_1_bar_black_24dp);
                else
                    this.signalStrength.setImageResource(R.drawable.ic_signal_wifi_0_bar_black_24dp);
            }
        }
    }
}
