package com.albertogeniola.merossconf.model;

import androidx.annotation.Nullable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WifiLocationStatus {
    private Boolean wifiEnabledOrEnabling;
    private Boolean locationEnabledOrEnabling;

    public WifiLocationStatus(@Nullable Boolean wifiEnabledOrEnabling, @Nullable Boolean locationEnabledOrEnabling) {
        this.wifiEnabledOrEnabling = wifiEnabledOrEnabling;
        this.locationEnabledOrEnabling = locationEnabledOrEnabling;
    }
}
