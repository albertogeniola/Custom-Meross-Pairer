package com.albertogeniola.merosslib.model.protocol;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.UUID;

import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class Header implements Serializable {
    @SerializedName("from")
    private String from;
    @SerializedName("messageId")
    private String messageId;
    @SerializedName("method")
    private Method method;
    @SerializedName("namespace")
    private String namespace;
    @SerializedName("timestamp")
    private long timestamp;
    @SerializedName("sign")
    private String sign;

    public static Header BuildNew(String namespace, Method method, String from) {
        Header header = new Header();
        header.namespace = namespace;
        header.method = method;
        header.from = from;
        header.messageId = UUID.randomUUID().toString().replaceAll("-","");
        header.timestamp = new Date().getTime();
        return header;
    }

    public static Header BuildNew(String namespace, Method method) {
        return BuildNew(namespace, method, "http://10.10.10.1/config");
    }

    public void sign(String cloudKey) {
        String signature = null;
        StringBuilder signatureBuilder = new StringBuilder();
        signatureBuilder.append(messageId);
        signatureBuilder.append(cloudKey);
        signatureBuilder.append(timestamp);

        try {
            MessageDigest md = MessageDigest.getInstance("md5");
            md.update(signatureBuilder.toString().getBytes("utf8"));
            signature = toHex(md.digest());
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        this.sign = signature;
    }

    public static String toHex(byte[] bytes) {
        BigInteger bi = new BigInteger(1, bytes);
        return String.format("%0" + (bytes.length << 1) + "x", bi);
    }

}
