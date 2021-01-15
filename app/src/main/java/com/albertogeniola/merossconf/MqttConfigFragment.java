package com.albertogeniola.merossconf;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
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
import java.util.List;
import java.util.Map;


public class MqttConfigFragment extends Fragment {
    private static String PERFS_MQTT_CONFS = "com.albertogeniola.merossconf.mqtt_shared_preferences";

    private View newConfigurationFrame;
    private PairActivity parentActivity;
    private EditText mqttConfigurationNameEditText;
    private EditText mqttHostEditText;
    private EditText mqttPortEditText;
    private Spinner mqttConfigurationSpinner;
    private Button pairButton;
    private CheckBox saveCheckbox;
    private ArrayAdapter<MqttConfiguration> adapter;
    private MqttConfiguration newMqttConfig = new MqttConfiguration("Add new...", null, -1);

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

    private ArrayList<MqttConfiguration> loadMqttConfigurations() {
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
        return res;
    }

    private void saveConfiguration(MqttConfiguration conf) {
        SharedPreferences settings = getContext().getSharedPreferences(PERFS_MQTT_CONFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        Gson g = new Gson();
        String json = g.toJson(conf);
        editor.putString(conf.getName(), json);
        editor.apply();
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
        newConfigurationFrame = view.findViewById(R.id.newConfigurationFrame);

        ArrayList<MqttConfiguration> configurations = loadMqttConfigurations();
        configurations.add(this.newMqttConfig);
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, configurations);
        mqttConfigurationSpinner.setAdapter(adapter);

        saveCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mqttConfigurationNameEditText.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
        });

        mqttConfigurationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                MqttConfiguration selection = (MqttConfiguration) parent.getAdapter().getItem(position);
                if (selection == newMqttConfig) {
                    mqttConfigurationNameEditText.setText("");
                    mqttConfigurationNameEditText.setEnabled(true);
                    mqttHostEditText.setText("");
                    mqttPortEditText.setText("");
                } else {
                    mqttConfigurationNameEditText.setText(selection.getName());
                    mqttConfigurationNameEditText.setEnabled(false);
                    mqttHostEditText.setText(selection.getHostname());
                    mqttPortEditText.setText(""+selection.getPort());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        pairButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // New item validation
                MqttConfiguration tmpConf = new MqttConfiguration();
                boolean save = saveCheckbox.isChecked();
                boolean error = false;
                if (mqttConfigurationSpinner.getSelectedItem() == newMqttConfig) {
                    // make sure the host is populated
                    String hostnameStr = mqttHostEditText.getText().toString();
                    if (hostnameStr.isEmpty()) {
                        error = true;
                        mqttHostEditText.setError("Invalid mqtt host");
                    } else {
                        mqttHostEditText.setError(null);
                        tmpConf.setHostname(hostnameStr);
                    }

                    // make sure the port is populated
                    try {
                        String portstr = mqttPortEditText.getText().toString();
                        int port = Integer.parseInt(portstr);
                        if (port<1 || port > 65535)
                            throw new NumberFormatException();
                        mqttPortEditText.setError(null);
                        tmpConf.setPort(port);

                    } catch (NumberFormatException e) {
                        error = true;
                        mqttPortEditText.setError("The MQTT port is invalid");
                    }

                    // Make sure the name is populated
                    String name = mqttConfigurationNameEditText.getText().toString();
                    if (save && (name.isEmpty() || name.trim().toLowerCase().compareTo("add new...")==0)) {
                        error = true;
                        mqttConfigurationNameEditText.setError("Invalid name");
                    } else {
                        mqttConfigurationNameEditText.setError(null);
                        tmpConf.setHostname(name);
                    }

                    if (error) {
                        return;
                    }

                    if (save) {
                        saveConfiguration(tmpConf);
                        adapter.insert(tmpConf,0);
                        adapter.notifyDataSetChanged();
                        mqttConfigurationSpinner.setSelection(0);
                    }

                    parentActivity.setTargetMqttConfig(tmpConf);
                } else {
                    MqttConfiguration tmp = (MqttConfiguration) mqttConfigurationSpinner.getSelectedItem();
                    parentActivity.setTargetMqttConfig(tmp);
                }

                NavHostFragment
                        .findNavController(MqttConfigFragment.this)
                        .navigate(R.id.action_mqttConfigFragment_to_PairFragment);
            }
        });
    }
}
