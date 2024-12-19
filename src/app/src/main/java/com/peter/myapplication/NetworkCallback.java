package com.peter.myapplication;

import java.io.IOException;

public interface NetworkCallback {

    void appendLog(String msg);

    void setToken(String token);

    void setBannerText(String text);

    void login() throws IOException;

    void info() throws IOException;
}