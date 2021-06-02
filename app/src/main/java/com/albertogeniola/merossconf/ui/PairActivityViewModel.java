package com.albertogeniola.merossconf.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.albertogeniola.merossconf.model.MqttConfiguration;
import com.albertogeniola.merossconf.model.MerossDeviceAp;
import com.albertogeniola.merossconf.model.WifiConfiguration;
import com.albertogeniola.merossconf.model.WifiLocationStatus;
import com.albertogeniola.merosslib.model.protocol.MessageGetConfigWifiListResponse;
import com.albertogeniola.merosslib.model.protocol.MessageGetSystemAllResponse;
import com.albertogeniola.merosslib.model.protocol.payloads.GetConfigWifiListEntry;

public class PairActivityViewModel extends ViewModel {
    // Wrapper around the Meross AP
    private MutableLiveData<com.albertogeniola.merosslib.MerossDeviceAp> apDevice;
    private MutableLiveData<MerossDeviceAp> merossDeviceWifiAp;
    private MutableLiveData<WifiConfiguration> localWifiNetwork;
    private MutableLiveData<MessageGetConfigWifiListResponse> deviceAvailableWifis;
    private MutableLiveData<MqttConfiguration> targetMqttConfig;
    private MutableLiveData<MessageGetSystemAllResponse> deviceInfo;
    private MutableLiveData<WifiLocationStatus> wifiLocationStatus;


    public PairActivityViewModel() {
        apDevice = new MutableLiveData<>(null);
        merossDeviceWifiAp = new MutableLiveData<>(null);
        localWifiNetwork = new MutableLiveData<>(null);
        targetMqttConfig = new MutableLiveData<>(null);
        deviceInfo = new MutableLiveData<>(null);
        deviceAvailableWifis = new MutableLiveData<>(null);
        wifiLocationStatus = new MutableLiveData<>(null);
    }

    public LiveData<com.albertogeniola.merosslib.MerossDeviceAp> getDevice() {
        return apDevice;
    }
    public void setApDevice(com.albertogeniola.merosslib.MerossDeviceAp device) {
        this.apDevice.setValue(device);
    }

    public LiveData<MerossDeviceAp> getMerossPairingAp() {
        return this.merossDeviceWifiAp;
    }
    public void setMerossPairingAp(MerossDeviceAp apInfo) {
        this.merossDeviceWifiAp.setValue(apInfo);
    }

    public LiveData<WifiConfiguration> getMerossConfiguredWifi() {
        return this.localWifiNetwork;
    }
    public void setMerossWifiConfiguration(WifiConfiguration wifiConfiguration) {
        this.localWifiNetwork.setValue(wifiConfiguration);
    }

    public LiveData<MessageGetSystemAllResponse> getDeviceInfo() {
        return this.deviceInfo;
    }
    public void setDeviceInfo(MessageGetSystemAllResponse deviceInfo) {
        this.deviceInfo.setValue(deviceInfo);
    }

    public LiveData<MessageGetConfigWifiListResponse> getDeviceAvailableWifis() {
        return this.deviceAvailableWifis;
    }
    public void setDeviceAvailableWifis(MessageGetConfigWifiListResponse deviceAvailableWifis) {
        this.deviceAvailableWifis.setValue(deviceAvailableWifis);
    }

    public LiveData<MqttConfiguration> getTargetMqttConfig() {
        return this.targetMqttConfig;
    }
    public void setTargetMqttConfig(MqttConfiguration mqttConfig) {
        this.targetMqttConfig.setValue(mqttConfig);
    }

    public LiveData<WifiLocationStatus> getWifiLocationStatus() {
        return this.wifiLocationStatus;
    }
    public void setWifiLocationStatus(WifiLocationStatus status) {
        this.wifiLocationStatus.setValue(status);
    }
}