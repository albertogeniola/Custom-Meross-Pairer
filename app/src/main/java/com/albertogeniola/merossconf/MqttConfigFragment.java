package com.albertogeniola.merossconf;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.albertogeniola.merossconf.model.MqttConfiguration;
import com.google.gson.Gson;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;


public class MqttConfigFragment extends Fragment {
    private static String PERFS_MQTT_CONFS = "com.albertogeniola.merossconf.mqtt_shared_preferences";

    private View newConfigurationFrame;
    private PairActivity parentActivity;
    private EditText mqttConfigurationNameEditText;
    private EditText mqttHostEditText;
    private EditText mqttPortEditText;
    private Spinner mqttConfigurationSpinner;
    private ImageButton addConfButton;
    private Button pairButton;
    private CheckBox saveCheckbox;
    private TextView noSavedConfTextView;
    private ArrayAdapter<MqttConfiguration> adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        parentActivity = (PairActivity) getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_mqtt_config, container, false);
    }

    private MqttConfiguration[] loadMqttConfigurations() {
        ArrayList<MqttConfiguration> res = new ArrayList<>();
        Gson g = new Gson();
        SharedPreferences settings = getContext().getSharedPreferences(PERFS_MQTT_CONFS, Context.MODE_PRIVATE);
        Map<String, ?> confs = settings.getAll();
        for (Map.Entry<String, ?> k: confs.entrySet()) {
            if (k.getValue() != null) {
                String value = k.getValue().toString();
                MqttConfiguration conf = g.fromJson(value, MqttConfiguration.class);
                res.add(conf);
            }
        }
        return res.toArray(new MqttConfiguration[0]);
    }

    private void saveConfiguration() {
        //SharedPreferences settings = getContext().getSharedPreferences(PERFS_MQTT_CONFS, Context.MODE_PRIVATE);
        //SharedPreferences.Editor editor = settings.edit();
        //editor.putInt("homeScore", YOUR_HOME_SCORE);
        //editor.apply();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mqttConfigurationSpinner = view.findViewById(R.id.mqttConfigurationSpinner);
        mqttConfigurationNameEditText = view.findViewById(R.id.mqttConfigurationNameEditText);
        mqttHostEditText = view.findViewById(R.id.mqttHostnameEditText);
        mqttPortEditText = view.findViewById(R.id.mqttPortEditText);
        pairButton = view.findViewById(R.id.pairButton);
        saveCheckbox = view.findViewById(R.id.saveCheckbox);
        addConfButton = view.findViewById(R.id.addConfButton);
        newConfigurationFrame = view.findViewById(R.id.newConfigurationFrame);
        noSavedConfTextView = view.findViewById(R.id.noSavedConfTextView);

        MqttConfiguration[] configurations = loadMqttConfigurations();
        adapter = new ArrayAdapter<MqttConfiguration>(getContext(), android.R.layout.simple_list_item_1, configurations);
        mqttConfigurationSpinner.setAdapter(adapter);
        if (configurations.length < 1) {
            noSavedConfTextView.setVisibility(View.VISIBLE);
        } else {
            noSavedConfTextView.setVisibility(View.GONE);
        }

        addConfButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newConfigurationFrame.setVisibility(View.VISIBLE);
            }
        });

        saveCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mqttConfigurationNameEditText.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
        });

        pairButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO
                /*
                // make sure the host/port is valid
                String hostnameStr = mqttHostEditText.getText().toString();
                mqttHostEditText.setError(hostnameStr.isEmpty() ? "Invalid mqtt host" : null);
                String portstr = mqttPortEditText.getText().toString();
                int port;
                try {
                    port = Integer.parseInt(portstr);
                    if (port<1 || port > 65535)
                        throw new NumberFormatException();
                    mqttPortEditText.setError(null);
                } catch (NumberFormatException e) {
                    mqttPortEditText.setError("The MQTT port is invalid");
                    return;
                }
                */

                NavHostFragment
                        .findNavController(MqttConfigFragment.this)
                        .navigate(R.id.action_mqttConfigFragment_to_PairFragment);
            }
        });
    }
}
