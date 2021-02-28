package com.albertogeniola.merosslib;

import com.albertogeniola.merosslib.model.http.ApiCredentials;
import com.albertogeniola.merosslib.model.http.ApiResponse;
import com.albertogeniola.merosslib.model.http.ErrorCodes;
import com.albertogeniola.merosslib.model.http.exceptions.HttpApiException;
import com.albertogeniola.merosslib.model.http.exceptions.HttpInvalidCredentials;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.apache.commons.codec.binary.Base64;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import javax.xml.bind.DatatypeConverter;

import lombok.Getter;
import lombok.SneakyThrows;


public class MerossHttpClient implements Serializable {
    private OkHttpClient client;

    @Getter
    private ApiCredentials creds;

    private static final Gson g = new Gson();
    private static final String NOONCE_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOGIN_PATH = "/v1/Auth/Login";
    private static final String SECRET = "23x17ahWarFH6w29";

    private MerossHttpClient(ApiCredentials creds, OkHttpClient client) {
        this.creds = creds;
        this.client = client;
        this.client.setConnectTimeout(10, TimeUnit.SECONDS);
        this.client.setReadTimeout(10, TimeUnit.SECONDS);
    }

    @SneakyThrows(UnsupportedEncodingException.class)
    public static MerossHttpClient getByUserAndPassword(String apiUrl, String username, String password) throws IOException, HttpApiException, HttpInvalidCredentials {
        Gson g = new Gson();
        OkHttpClient client = new OkHttpClient();
        HashMap<String, Object> data = new HashMap<>();
        data.put("email", username);
        data.put("password", password);
        Map<String, Object> result = authenticatedPost(client, apiUrl+LOGIN_PATH, data, null);

        ApiCredentials creds = new ApiCredentials(
                apiUrl,
                result.get("token").toString(),
                result.get("userid").toString(),
                result.get("email").toString(),
                result.get("key").toString(),
                new Date()
        );
        return new MerossHttpClient(creds, client);
    }

    private static String generateNonce(int targetStringLength) {
        StringBuilder result = new StringBuilder(targetStringLength);
        Random random = new Random();
        for (int i=0;i<targetStringLength;i++) {
            char randomChar = NOONCE_ALPHABET.charAt(random.nextInt(NOONCE_ALPHABET.length()));
            result.append(randomChar);
        }
        return result.toString();
    }

    private static String toHexString(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();

        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }

        return hexString.toString();
    }

    @SneakyThrows({UnsupportedEncodingException.class, NoSuchAlgorithmException.class})
    private static Map<String, Object> authenticatedPost(OkHttpClient client, String url, HashMap<String, Object> data, String httpToken) throws IOException, HttpApiException, HttpInvalidCredentials {

        String nonce = generateNonce(16);
        long timestampMillis = new Date().getTime();
        String params = new String(Base64.encodeBase64(g.toJson(data).getBytes("utf8")), "utf8");

        // Generate the md5-hash (called signature)
        MessageDigest m = MessageDigest.getInstance("md5");
        String dataToSign = SECRET + timestampMillis + nonce + params;
        m.update(dataToSign.getBytes("utf8"));
        String md5hash = toHexString(m.digest());

        HashMap<String, Object> payload = new HashMap<>();
        payload.put("params", params);
        payload.put("sign", md5hash);
        payload.put("timestamp", timestampMillis);
        payload.put("nonce", nonce);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization",  httpToken == null ? "Basic" : "Basic " + httpToken)
                .addHeader("vender", "Meross")
                .addHeader("AppVersion", "1.3.0")
                .addHeader("AppLanguage", "EN")
                .addHeader("User-Agent", "okhttp/3.6.0")
                .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), g.toJson(payload).getBytes("utf8")))
                .build();
        Response response = client.newCall(request).execute();
        if (response.code() != 200) {
            String responseString = response.body().string();
            throw new IOException("Failed request to API. Response code: " + response.code() + ". Response data: "+ responseString);
        }

        String strdata = response.body().string();
        ApiResponse responseData = g.fromJson(strdata, ApiResponse.class);

        switch (responseData.getApiStatus()) {
            case CODE_NO_ERROR:
                return responseData.getData();
            case CODE_WRONG_CREDENTIALS:
            case CODE_UNEXISTING_ACCOUNT:
                throw new HttpInvalidCredentials();
            default:
                throw new HttpApiException(responseData.getApiStatus());
        }
    }


}
