package com.albertogeniola.merosslib.model.protocol;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class Message implements Serializable {
    @SerializedName("header")
    private Header header;

    public void sign(String cloudKey) {
        this.header.sign(cloudKey);
    }
}

