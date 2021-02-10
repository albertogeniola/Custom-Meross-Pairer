package com.albertogeniola.merossconf;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.albertogeniola.merossconf.model.MqttConfiguration;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;


public class MqttConfigFragment extends Fragment {
    private PairActivity parentActivity;
    private TextInputLayout mqttConfigurationNameEditText;
    private TextInputLayout mqttHostEditText;
    private TextInputLayout mqttPortEditText;
    private Spinner mqttConfigurationSpinner;
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

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mqttConfigurationSpinner = view.findViewById(R.id.mqttConfigurationSpinner);
        mqttConfigurationNameEditText = view.findViewById(R.id.mqttConfigurationNameEditText);
        mqttHostEditText = view.findViewById(R.id.mqttHostnameEditText);
        mqttPortEditText = view.findViewById(R.id.mqttPortEditText);
        Button pairButton = view.findViewById(R.id.pairButton);
        saveCheckbox = view.findViewById(R.id.saveCheckbox);

        List<MqttConfiguration> configurations = AndroidPreferencesManager.loadAllMqttConfigurations(getContext());
        configurations.add(this.newMqttConfig);
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, configurations);
        saveCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mqttConfigurationNameEditText.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
        });
        mqttConfigurationSpinner.setAdapter(adapter);

        mqttConfigurationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                MqttConfiguration selection = (MqttConfiguration) parent.getAdapter().getItem(position);
                if (selection == newMqttConfig) {
                    mqttConfigurationNameEditText.getEditText().setText("");
                    mqttConfigurationNameEditText.setEnabled(true);
                    mqttHostEditText.getEditText().setText("");
                    mqttHostEditText.setEnabled(true);
                    mqttPortEditText.getEditText().setText("");
                    mqttPortEditText.setEnabled(true);
                    saveCheckbox.setVisibility(View.VISIBLE);
                } else {
                    mqttConfigurationNameEditText.getEditText().setText(selection.getName());
                    mqttConfigurationNameEditText.setEnabled(false);
                    mqttHostEditText.getEditText().setText(selection.getHostname());
                    mqttHostEditText.setEnabled(false);
                    mqttPortEditText.getEditText().setText(""+selection.getPort());
                    mqttPortEditText.setEnabled(false);
                    saveCheckbox.setVisibility(View.GONE);
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
                    String hostnameStr = mqttHostEditText.getEditText().getText().toString();
                    if (hostnameStr.isEmpty()) {
                        error = true;
                        mqttHostEditText.setError("Invalid mqtt host");
                    } else {
                        mqttHostEditText.setError(null);
                        tmpConf.setHostname(hostnameStr);
                    }

                    // make sure the port is populated
                    try {
                        String portstr = mqttPortEditText.getEditText().getText().toString();
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
                    String name = mqttConfigurationNameEditText.getEditText().getText().toString();
                    if (save && (name.isEmpty() || name.trim().toLowerCase().compareTo("add new...")==0)) {
                        error = true;
                        mqttConfigurationNameEditText.setError("Invalid name");
                    } else {
                        mqttConfigurationNameEditText.setError(null);
                        tmpConf.setName(name);
                    }

                    if (error) {
                        return;
                    }

                    if (save) {
                        AndroidPreferencesManager.storeNewMqttConfiguration(getContext(), tmpConf);
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
