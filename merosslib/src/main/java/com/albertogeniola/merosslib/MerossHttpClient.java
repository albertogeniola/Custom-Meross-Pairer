package com.albertogeniola.merosslib;

import com.albertogeniola.merosslib.model.http.ApiCredentials;
import com.albertogeniola.merosslib.model.http.ApiResponse;
import com.albertogeniola.merosslib.model.http.DeviceInfo;
import com.albertogeniola.merosslib.model.http.ErrorCodes;
import com.albertogeniola.merosslib.model.http.LoginResponseData;
import com.albertogeniola.merosslib.model.http.exceptions.HttpApiException;
import com.albertogeniola.merosslib.model.http.exceptions.HttpApiInvalidCredentialsException;
import com.albertogeniola.merosslib.model.http.exceptions.HttpApiTokenException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import jdk.nashorn.internal.parser.TokenType;
import lombok.NonNull;
import lombok.SneakyThrows;

import java.util.logging.Logger;

import javax.net.SocketFactory;


public class MerossHttpClient implements Serializable {
    // Static attributes
    private final static Logger l = Logger.getLogger(MerossHttpClient.class.getName());

    private static final Gson g = new GsonBuilder().disableHtmlEscaping().create();
    private static final String NOONCE_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOGIN_PATH = "/v1/Auth/Login";
    private static final String DEVICE_LIST = "/v1/Device/devList";
    private static final String LOGOUT_PATH = "/v1/Profile/logout";
    private static final String SECRET = "23x17ahWarFH6w29";
    private static final HashMap<String, Object> DEFAULT_PARAMS = new HashMap<>();

    // Class attributes
    private ApiCredentials mCredentials;
    private OkHttpClient mClient;

    public MerossHttpClient() {
        this(null, new OkHttpClient());
    }
    public void setSocketFactory(SocketFactory socketFactory) {
        mClient.setSocketFactory(socketFactory);
    }
    public MerossHttpClient(ApiCredentials creds) {
        this(creds, new OkHttpClient());
    }

    private MerossHttpClient(ApiCredentials creds, OkHttpClient client) {
        this.mCredentials = creds;
        this.mClient = client;
        this.mClient.setConnectTimeout(10, TimeUnit.SECONDS);
        this.mClient.setReadTimeout(10, TimeUnit.SECONDS);
    }

    @SneakyThrows(UnsupportedEncodingException.class)
    public void login(String apiUrl, String username, String password) throws IOException, HttpApiException, HttpApiInvalidCredentialsException {
        HashMap<String, Object> data = new HashMap<>();
        data.put("email", username);
        data.put("password", password);
        LoginResponseData result = authenticatedPost( apiUrl+LOGIN_PATH, data, null, LoginResponseData.class);

        this.mCredentials = new ApiCredentials(
                apiUrl,
                result.getToken(),
                result.getUserId(),
                result.getEmail(),
                result.getKey(),
                new Date()
        );
    }

    public List<DeviceInfo> listDevices() throws IOException, HttpApiException, HttpApiInvalidCredentialsException {
        HashMap<String, Object> data = new HashMap<>();
        TypeToken<?> typeToken = TypeToken.getParameterized(List.class, DeviceInfo.class);
        List<DeviceInfo> devices = authenticatedPost( mCredentials.getApiServer()+DEVICE_LIST, data, this.mCredentials.getToken(), typeToken.getType());
        return devices;
    }

    public void logout() throws HttpApiInvalidCredentialsException, HttpApiException, IOException {
        if (mCredentials == null) {
            throw new IllegalStateException("Invalid logout operation: this client is not logged in.");
        }
        authenticatedPost(mCredentials.getApiServer()+LOGOUT_PATH, null, mCredentials.getToken(), Object.class);
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
    private <T> T authenticatedPost(@NonNull String url, HashMap<String, Object> data, String httpToken, Type dataType) throws IOException, HttpApiException, HttpApiInvalidCredentialsException {

        String nonce = generateNonce(16);
        long timestampMillis = new Date().getTime();
        String params = new String(Base64.encodeBase64(g.toJson(data == null ? DEFAULT_PARAMS : data ).getBytes("utf8")), "utf8");

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

        String requestData = g.toJson(payload);
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .addHeader("Authorization",  httpToken == null ? "Basic" : "Basic " + httpToken)
                .addHeader("vender", "Meross")
                .addHeader("AppVersion", "1.3.0")
                .addHeader("AppLanguage", "EN")
                .addHeader("User-Agent", "okhttp/3.6.0")
                .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestData.getBytes("utf8")))
                .build();

        l.fine("HTTP Request, METHOD:" + request.method().toString() +", URL: " + request.urlString() + ", HEADERS: "+ request.headers() +", DATA:" +requestData);
        Response response = mClient.newCall(request).execute();
        String strdata = response.body().string();
        l.fine("HTTP Response, STATUS_CODE: "+response.code()+", HEADERS: "+response.headers() + ", BODY: "+strdata);
        if (response.code() != 200) {
            l.severe("Bad HTTP Response code: " + response.code() );
        }

        TypeToken<?> token = TypeToken.getParameterized(ApiResponse.class, dataType);
        ApiResponse<T> responseData = g.fromJson(strdata, token.getType());

        switch (responseData.getApiStatus()) {
            case CODE_NO_ERROR:
                return responseData.getData();
            case CODE_WRONG_CREDENTIALS:
            case CODE_UNEXISTING_ACCOUNT:
                throw new HttpApiInvalidCredentialsException(responseData.getApiStatus());
            case CODE_TOKEN_ERROR:
            case CODE_TOKEN_EXPIRED:
            case CODE_TOKEN_INVALID:
            case CODE_TOO_MANY_TOKENS:
                throw new HttpApiTokenException(responseData.getApiStatus());
            default:
                l.severe("API Code was unknown. Passing CODE_ERROR_GENERIC to the handler.");
                throw new HttpApiException(responseData.getApiStatus() == null ? ErrorCodes.CODE_ERROR_GENERIC : responseData.getApiStatus());
        }
    }

    public ApiCredentials getCredentials() {
        return mCredentials;
    }
}
