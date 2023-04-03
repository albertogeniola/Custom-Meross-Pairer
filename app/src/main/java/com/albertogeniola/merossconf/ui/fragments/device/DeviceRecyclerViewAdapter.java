package com.albertogeniola.merossconf.ui.fragments.device;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.albertogeniola.merossconf.databinding.FragmentDeviceInfoBinding;
import com.albertogeniola.merosslib.model.OnlineStatus;
import com.albertogeniola.merosslib.model.http.DeviceInfo;

import java.util.List;

public class DeviceRecyclerViewAdapter extends RecyclerView.Adapter<DeviceRecyclerViewAdapter.ViewHolder> {

    private final List<DeviceInfo> mValues;

    public DeviceRecyclerViewAdapter(List<DeviceInfo> items) {
        mValues = items;
    }

    public void setItems(List<DeviceInfo> items) {
        mValues.clear();
        mValues.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        return new ViewHolder(FragmentDeviceInfoBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));

    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        holder.mDeviceUuid.setText(mValues.get(position).getUuid());
        holder.mDeviceName.setText(mValues.get(position).getDevName());
        holder.deviceClass.setText(mValues.get(position).getDeviceType());
        holder.mDeviceOnline.setText(mValues.get(position).getOnlineStatus() == null ? OnlineStatus.UNKNOWN.name() : mValues.get(position).getOnlineStatus().name());
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView mDeviceUuid;
        public final TextView mDeviceName;
        public final TextView deviceClass;
        public final TextView mDeviceOnline;
        public DeviceInfo mItem;

        public ViewHolder(FragmentDeviceInfoBinding binding) {
            super(binding.getRoot());
            mDeviceUuid = binding.deviceUuid;
            mDeviceName = binding.deviceName;
            deviceClass = binding.deviceClass;
            mDeviceOnline = binding.deviceOnlineStatus;
        }

        @NonNull
        @Override
        public String toString() {
            return super.toString() + mDeviceName.getText();
        }
    }
}