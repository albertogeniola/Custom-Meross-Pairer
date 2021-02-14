package com.albertogeniola.merossconf.ui.fragments.pair;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.albertogeniola.merossconf.AndroidPreferencesManager;
import com.albertogeniola.merossconf.R;
import com.albertogeniola.merossconf.model.MqttConfiguration;
import com.albertogeniola.merossconf.model.TargetWifiAp;
import com.albertogeniola.merossconf.ui.PairActivityViewModel;
import com.albertogeniola.merosslib.MerossDeviceAp;
import com.albertogeniola.merosslib.model.http.ApiCredentials;
import com.albertogeniola.merosslib.model.protocol.MessageSetConfigWifiResponse;
import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class PairFragment extends Fragment {
    private static final String DEFAULT_KEY = "";
    private static final String DEFAULT_USER_ID = "";

    private PairActivityViewModel pairActivityViewModel;

    private ImageSwitcher imageSwitcher;
    private TextView configureMqttTextView;
    private TextView configureWifiTextView;

    private Handler uiThreadHandler;
    private ScheduledExecutorService worker;

    private int animationCounter = 0;
    private boolean animateWifi = false;
    private State state = State.INIT;
    private String error = null;

    public PairFragment() {
        worker = Executors.newSingleThreadScheduledExecutor();
    }

    // Logic methods
    private void stateMachine(Signal signal) {
        switch(state) {
            case INIT:
                if (signal == Signal.RESUMED) {
                    state = State.CONFIGURING_MQTT;
                    updateUi();
                    preConfigureMqtt();
                }
                break;
            case CONFIGURING_MQTT:
                if (signal == Signal.MQTT_CONFIGURED) {
                    state = State.CONFIGURING_WIFI;
                    updateUi();
                    configureWifi();
                }
                break;
            case CONFIGURING_WIFI:
                if (signal == Signal.WIFI_CONFIGURED) {
                    state = State.DONE;
                    updateUi();
                    completeActivityFragment();
                }
                break;
        }

        if (signal == Signal.ERROR) {
            state = State.ERROR;
            updateUi();
        }
    }

    private void preConfigureMqtt() {
        // Check if the user has logged in. In case he is not, show a warning message
        // telling that the userid and key will be populated with predefined defaults
        ApiCredentials creds = AndroidPreferencesManager.loadHttpCredentials(getActivity());
        if (creds == null) {
            final AlertDialog alert = new AlertDialog.Builder(getActivity())
                    .setMessage("You have are not logged in to any HTTP API. " +
                            "The pairing process will therefore send the following defaults:" +
                            "\nKey: '"+DEFAULT_KEY+"' (empty)" +
                            "\nuserId: '"+DEFAULT_USER_ID+"' (empty)")
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            configureMqtt(DEFAULT_USER_ID, DEFAULT_KEY);
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            error = "Operation aborted.";
                            stateMachine(Signal.ERROR);
                            dialog.dismiss();
                        }
                    })
                    .create();
            alert.show();
        } else {
            configureMqtt(creds.getUserId(), creds.getKey());
        }
    }

    private void configureMqtt(final String userId, final String key) {
        worker.schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    LiveData<MqttConfiguration> mqttConfig = pairActivityViewModel.getTargetMqttConfig();
                    pairActivityViewModel.getDevice().getValue().setConfigKey(
                            mqttConfig.getValue().getHostname(),
                            mqttConfig.getValue().getPort(),
                                    key,
                                    userId);
                    stateMachine(Signal.MQTT_CONFIGURED);
                } catch (IOException e) {
                    e.printStackTrace();
                    uiThreadHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            error = "Error occurred while configuring device MQTT server";
                            stateMachine(Signal.ERROR);
                        }
                    });
                }
            }
        },3, TimeUnit.SECONDS);
    }

    private void configureWifi() {
        final MerossDeviceAp device = pairActivityViewModel.getDevice().getValue();
        final TargetWifiAp credentials = pairActivityViewModel.getTargetWifiAp().getValue();
        worker.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    MessageSetConfigWifiResponse response = device.setConfigWifi(credentials.getSsid(), credentials.getPassword());
                    stateMachine(Signal.WIFI_CONFIGURED);
                } catch (IOException e) {
                    e.printStackTrace();
                    uiThreadHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            error = "Error occurred while configuring device WIFI connection";
                            stateMachine(Signal.ERROR);
                        }
                    });
                }
            }
        });
    }

    private void completeActivityFragment() {
        NavController ctrl = NavHostFragment.findNavController(this);
        ctrl.popBackStack(R.id.ScanFragment, false);
        ctrl.navigate(R.id.PairDone);
    }

    // UI
    private void updateUi() {
        Runnable uiUpdater = new Runnable() {
            @Override
            public void run() {
                switch (state) {
                    case INIT:
                        animateWifi = false;
                        configureMqttTextView.setVisibility(View.INVISIBLE);
                        configureWifiTextView.setVisibility(View.INVISIBLE);
                        break;
                    case CONFIGURING_MQTT:
                        animateWifi = true;
                        startWifiAnimation();

                        configureMqttTextView.setVisibility(View.VISIBLE);
                        break;
                    case CONFIGURING_WIFI:
                        configureWifiTextView.setVisibility(View.VISIBLE);
                        break;
                    case DONE:
                        animateWifi = false;
                        break;
                    case ERROR:
                        animateWifi = false;
                        imageSwitcher.setImageResource(R.drawable.ic_error_outline_black_24dp);
                        Snackbar.make(PairFragment.this.getView(), error, Snackbar.LENGTH_LONG).show();
                        break;
                }
            }
        };

        if (Looper.getMainLooper().getThread().getId() != Thread.currentThread().getId()) {
            uiThreadHandler.post(uiUpdater);
        } else {
            uiUpdater.run();
        }
    }

    private void setupAnimation() {
        imageSwitcher = getView().findViewById(R.id.wifi_animation);
        imageSwitcher.setFactory(new ViewSwitcher.ViewFactory() {
            @Override
            public View makeView() {
                ImageView im = new ImageView(PairFragment.this.getActivity());
                im.setScaleType(ImageView.ScaleType.FIT_CENTER);
                im.setAdjustViewBounds(true);
                return im;
            }
        });
        Animation in  = AnimationUtils.loadAnimation(this.getContext(), R.anim.fade_in);
        Animation out  = AnimationUtils.loadAnimation(this.getContext(), R.anim.fade_out);
        imageSwitcher.setInAnimation(in);
        imageSwitcher.setOutAnimation(out);
    }

    private void startWifiAnimation() {
        // Configure the animation
        uiThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (animateWifi) {
                    switch (animationCounter++) {
                        case 0:
                            imageSwitcher.setImageResource(R.drawable.ic_signal_wifi_0_bar_black_24dp);
                            break;
                        case 1:
                            imageSwitcher.setImageResource(R.drawable.ic_signal_wifi_1_bar_black_24dp);
                            break;
                        case 2:
                            imageSwitcher.setImageResource(R.drawable.ic_signal_wifi_2_bar_black_24dp);
                            break;
                        case 3:
                            imageSwitcher.setImageResource(R.drawable.ic_signal_wifi_3_bar_black_24dp);
                            break;
                        case 4:
                            imageSwitcher.setImageResource(R.drawable.ic_signal_wifi_4_bar_black_24dp);
                            break;
                    }
                    animationCounter %= 5;
                    uiThreadHandler.postDelayed(this, 2000);
                }
            }
        });
    }

    // Android activity lifecycle
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        uiThreadHandler = new Handler(Looper.getMainLooper());
        pairActivityViewModel = new ViewModelProvider(requireActivity()).get(PairActivityViewModel.class);
    }


    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_pair, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        configureMqttTextView = view.findViewById(R.id.configure_mqtt);
        configureWifiTextView = view.findViewById(R.id.configure_wifi);
        setupAnimation();
    }

    @Override
    public void onResume() {
        super.onResume();

        // As soon as we resume, connect to the given WiFi
        stateMachine(Signal.RESUMED);
    }


    enum State {
        INIT,
        CONFIGURING_MQTT,
        CONFIGURING_WIFI,
        DONE,
        ERROR
    }

    enum Signal {
        RESUMED,
        MQTT_CONFIGURED,
        WIFI_CONFIGURED,
        ERROR
    }
}
