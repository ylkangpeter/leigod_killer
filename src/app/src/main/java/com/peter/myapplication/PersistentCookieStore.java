package com.peter.myapplication;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

public class PersistentCookieStore implements CookieJar {
    private final Map<String, List<Cookie>> cookieStore = new HashMap<>();

    @Override
    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
        // 获取当前主机对应的Cookies列表
        List<Cookie> existingCookies = cookieStore.get(url.host());
        if (existingCookies == null) {
            existingCookies = new ArrayList<>();
        }

        // 更新Cookies，如果有相同的Cookie则覆盖旧的
        for (Cookie cookie : cookies) {
            boolean replaced = false;
            for (int i = 0; i < existingCookies.size(); i++) {
                if (existingCookies.get(i).name().equals(cookie.name())) {
                    existingCookies.set(i, cookie);
                    replaced = true;
                    break;
                }
            }
            if (!replaced) {
                existingCookies.add(cookie);
            }
        }

        // 将更新后的Cookies存入cookieStore
        cookieStore.put(url.host(), existingCookies);
    }

    @Override
    public List<Cookie> loadForRequest(HttpUrl url) {
        // 返回当前主机对应的Cookies列表
        List<Cookie> cookies = cookieStore.get(url.host());
        return cookies != null ? new ArrayList<>(cookies) : new ArrayList<>();
    }
}
