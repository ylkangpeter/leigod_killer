package com.peter.myapplication;

import androidx.annotation.NonNull;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

public class HttpUtil {

    private static final String LOGIN_URL = "https://webapi.leigod.com/api/auth/login/v1";
    private static final String PAUSE_URL = "https://webapi.leigod.com/api/user/pause";
    private static final String INFO_URL = "https://webapi.leigod.com/api/user/info";

    private static HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();

    static {
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY); // BODY, HEADER, BASIC, NONE
    }

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    public static void login(String name, String pwd, NetworkCallback activity) throws IOException {
        RequestBody loginBody = RequestBody.create(
                MediaType.parse("application/json; charset=UTF-8"), generateLoginData(name, md5(pwd))
        );
        Request loginRequest = buildRequest(LOGIN_URL, loginBody);

        client.newCall(loginRequest).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                } else {
                    String responseBody = response.body().string();
                    activity.appendLog("Request: " + LOGIN_URL + "\n" + decodeUnicode(responseBody));
                    // token
                    JsonObject jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();
                    int code = jsonObject.get("code").getAsInt();
                    if (code == 0) {
                        String token = jsonObject.getAsJsonObject("data").getAsJsonObject("login_info").get("account_token").getAsString();
                        activity.setToken(token);
                        activity.appendLog("save token: " + token);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                activity.appendLog("login failed: " + e.getMessage());
            }
        });
    }

    /*
        return string[important_info, all_info]
     */
    public static void getInfo(String token, NetworkCallback activity) throws IOException {
        postByToken(token, INFO_URL, activity, true);
    }

    public static void pause(String token, NetworkCallback activity) throws IOException {
        postByToken(token, PAUSE_URL, activity, false);
    }

    private static void postByToken(String accountToken, String url, NetworkCallback activity, boolean updateInfo) throws IOException {
        RequestBody infoBody = RequestBody.create(MediaType.parse("application/json; charset=UTF-8"),
                "{\"account_token\":\"" + accountToken + "\",\"lang\":\"zh_CN\"}"
        );
        Request req = buildRequest(url, infoBody);
        client.newCall(req).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                } else {
                    if (activity != null) {
                        String responseBody = decodeUnicode(response.body().string());
                        activity.appendLog("Request by token Success: " + String.format("%s ||| token:  %s", url, accountToken) + "\nInfo Response: " + responseBody);
                        JsonObject jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();

                        int code = jsonObject.get("code").getAsInt();
                        if (code == 0) {
                            if (updateInfo) {
                                activity.appendLog("update info");
                                JsonObject detail = jsonObject.getAsJsonObject("data");
                                String pauseStatus = detail.get("pause_status").toString();
                                String mobile = detail.get("mobile").toString();
                                String remainCredits = detail.get("expiry_time").toString();
                                activity.setBannerText(String.format("--%s-- --%s-- --余额: %s--", pauseStatus, mobile, remainCredits));
                            }
                        } else {
                            activity.appendLog("token or other errors. regenerate token by click TEST.");
                        }
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                activity.appendLog("Request by token failed: " + url + " ||| " + e.getMessage());
            }
        });
    }

    private static Request buildRequest(String url, RequestBody body) {
        Request req = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) " +
                        "AppleWebKit/537" +
                        ".36 (KHTML, like Gecko) Chrome/76.0.3803.0 Mobile Safari/537.36")
                .addHeader("Accept", "application/json, text/javascript, */*; q=0.01")
                .addHeader("Accept-Language", "zh-CN,zh;q=0.9")
                .addHeader("DNT", "1")
                .addHeader("Connection", "keep-alive")
                .addHeader("Content-Type", "application/json; charset=UTF-8")
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .build();
        return req;
    }

    private static String generateLoginData(String name, String pwd) {
        long ts = System.currentTimeMillis() / 1000;
        String signStr = String.format("account_token=null&country_code=86&lang=zh_CN&mobile_num=%s&os_type=4" +
                        "&password=%s&region_code=1&src_channel=guanwang&ts=%d&username=%s&key" +
                        "=5C5A639C20665313622F51E93E3F2783",
                name, pwd, ts, name);
        String sign = md5(signStr);

        return String.format("{\"os_type\":4,\"password\":\"%s\",\"mobile_num\":\"%s\",\"src_channel\":\"guanwang\"," +
                        "\"country_code\":86,\"username\":\"%s\",\"lang\":\"zh_CN\",\"region_code\":1," +
                        "\"account_token\":null,\"ts\":%d,\"sign\":\"%s\"}",
                pwd, name, name, ts, sign);
    }

    private static String md5(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static String decodeUnicode(String unicodeString) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < unicodeString.length()) {
            char c = unicodeString.charAt(i++);
            if (c == '\\' && i < unicodeString.length() && unicodeString.charAt(i) == 'u') {
                // 读取 Unicode 编码
                String hex = unicodeString.substring(i + 1, i + 5);
                sb.append((char) Integer.parseInt(hex, 16));
                i += 5;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static void logger(String msg) {
        System.out.println(msg);
    }
}
