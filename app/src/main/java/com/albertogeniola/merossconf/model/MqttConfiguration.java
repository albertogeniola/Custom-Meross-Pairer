package com.albertogeniola.merossconf.model;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class MqttConfiguration {
    private String name;
    private String hostname;
    private int port;

    public void validate() throws ValidationError {
        if (name == null || name.isEmpty()) {
            throw new ValidationError("Name is empty or invalid.");
        }

        if (hostname == null || hostname.isEmpty()) {
            throw new ValidationError("Hostname is empty or invalid.");
        }

        if (port > 65535 || port < 1) {
            throw new ValidationError("Port is invalid.");
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
