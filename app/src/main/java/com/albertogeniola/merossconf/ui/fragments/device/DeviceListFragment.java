package com.albertogeniola.merossconf.ui.fragments.device;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.albertogeniola.merossconf.AndroidPreferencesManager;
import com.albertogeniola.merossconf.R;
import com.albertogeniola.merossconf.model.HttpClientManager;
import com.albertogeniola.merosslib.model.http.ApiCredentials;
import com.albertogeniola.merosslib.model.http.DeviceInfo;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

/**
 * A fragment representing a list of Devices.
 */
public class DeviceListFragment extends Fragment {
    private DeviceRecyclerViewAdapter mAdapter;
    private FloatingActionButton mRefreshButton;

    public DeviceListFragment() {
    }

    @SuppressWarnings("unused")
    public static DeviceListFragment newInstance(int columnCount) {
        return new DeviceListFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAdapter = new DeviceRecyclerViewAdapter(new ArrayList<DeviceInfo>());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_device_list, container, false);
        RecyclerView recycler_view = view.findViewById(R.id.device_list_view);
        recycler_view.setAdapter(mAdapter);
        mRefreshButton = view.findViewById(R.id.device_list_refresh_fab);
        mRefreshButton.setOnClickListener(this.refreshButtonClickListener);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateData();
    }

    private void updateData() {
        ApiCredentials creds = AndroidPreferencesManager.loadHttpCredentials(requireContext());
        if (creds == null) {
            Toast.makeText(requireContext(), "Please login first.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (creds.getApiServer()==null) {
            Toast.makeText(requireContext(), "No API server specified.", Toast.LENGTH_SHORT).show();
            return;
        }

        mRefreshButton.setEnabled(false);
        final ProgressDialog dialog = new ProgressDialog(getContext());
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setTitle("Fetching data.");
        dialog.setCancelable(false);
        dialog.setMessage("Please wait reading devices info...");
        dialog.show();

        HttpClientManager.getInstance().loadFromCredentials(creds);
        HttpClientManager.getInstance().asyncListDevices(new HttpClientManager.Callback<List<DeviceInfo>>() {
            @Override
            public void onSuccess(List<DeviceInfo> result) {
                mAdapter.setItems(result);
                mRefreshButton.setEnabled(true);
                dialog.dismiss();
            }

            @Override
            public void onFailure(Exception result) {
                mRefreshButton.setEnabled(true);
                dialog.dismiss();
                Toast.makeText(requireContext(), "Failed to obtain device list.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private View.OnClickListener refreshButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            updateData();
        }
    };
}