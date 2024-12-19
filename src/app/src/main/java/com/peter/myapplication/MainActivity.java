package com.peter.myapplication;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CheckBox;
import android.text.InputType;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements NetworkCallback {

    private EditText usernameEditText;
    private EditText passwordEditText;
    private Button pauseButton;
    private Button testButton;

    private TextView statusText;
    private TextView htmlText;
    private ScrollView scrollView;

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 绑定UI元素
        usernameEditText = findViewById(R.id.username);
        passwordEditText = findViewById(R.id.password);
        pauseButton = findViewById(R.id.btn_pause);
        testButton = findViewById(R.id.btn_test);
        statusText = findViewById(R.id.status_text);
        htmlText = findViewById(R.id.html_text);
        scrollView = findViewById(R.id.scrollView);

        sharedPreferences = getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        editor = sharedPreferences.edit();

        CheckBox showPasswordCheckBox = findViewById(R.id.show_password);

        showPasswordCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // 显示密码
                passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            } else {
                // 隐藏密码
                passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            }
            // 将光标移到文本末尾
            passwordEditText.setSelection(passwordEditText.getText().length());
        });

        // 加载保存的值
        loadSavedData();

        // start checking
        initLog();

        // 设置保存按钮点击事件
        pauseButton.setOnClickListener(v -> pause());

        // 设置测试按钮点击事件，执行网络请求
        testButton.setOnClickListener(v -> {
            try {
                saveData();
                login();
                info();
            } catch (IOException e) {
                appendLog(e.getMessage());
                throw new RuntimeException(e);
            }
        });

        // 尝试获取用户信息
        try {
            info();
        } catch (IOException e) {
            appendLog(e.getMessage());
            throw new RuntimeException(e);
        }

        // 检查是否来自 Widget 的点击事件
        if (MyAppWidgetProvider.ACTION_PAUSE.equals(getIntent().getAction())) {
            appendLog("----pause by widget----");
            pause();
            updateWidget();
        } else if (MyAppWidgetProvider.ACTION_UPDATE.equals(getIntent().getAction())) {
            appendLog("----timer update by widget----");
            try {
                info();
                updateWidget();
            } catch (IOException e) {
                appendLog(e.getMessage());
            }
        }
    }

    private void updateWidget() {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(this, MyAppWidgetProvider.class));
        for (int appWidgetId : appWidgetIds) {
            MyAppWidgetProvider.updateAppWidget(this, appWidgetManager, appWidgetId);
        }
    }
    // 保存数据到SharedPreferences
    private void saveData() {
        String username = usernameEditText.getText().toString();
        String password = passwordEditText.getText().toString();

        editor.putString("keyUsername", username);
        editor.putString("keyPassword", password);
        editor.apply(); // 异步保存数据

        Toast.makeText(this, "数据已保存", Toast.LENGTH_SHORT).show();
    }

    // 从SharedPreferences中加载数据
    private void loadSavedData() {
        SharedPreferences sharedPreferences = getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        String uName = sharedPreferences.getString("keyUsername", "");
        String pwd = sharedPreferences.getString("keyPassword", "");
        String token = sharedPreferences.getString("token", "");

        // 将保存的值设置到输入框
        usernameEditText.setText(uName);
        passwordEditText.setText(pwd);
    }

    // 执行网络请求
    private void pause() {
        String token = sharedPreferences.getString("token", "");
        // pause with token
        appendLog(String.format("----stop with token: %s----", token));
        try {
            HttpUtil.pause(token, this);
            info();
        } catch (Exception e) {
            appendLog("Exception in pause: " + e.getMessage());
        }
    }

    @Override
    public void login() throws IOException {
        appendLog("----logining----");
        HttpUtil.login(usernameEditText.getText().toString(), passwordEditText.getText().toString(), this);
        appendLog("----login done----");
    }

    @Override
    public void info() throws IOException {
        String token = sharedPreferences.getString("token", "");
        appendLog(String.format("----get info: %s----", token));
        HttpUtil.getInfo(token, this);
        appendLog("----get info done----");
    }

    private void initLog() {
        Spanned initialSpannedLog;
        initialSpannedLog = Html.fromHtml("initializing...<br>", Html.FROM_HTML_MODE_COMPACT);
        htmlText.setText(initialSpannedLog);
    }

    @Override
    public void appendLog(String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // 添加新的日志内容
                Spanned spannedLog;
                spannedLog = Html.fromHtml(msg + "<br>", Html.FROM_HTML_MODE_COMPACT);
                htmlText.append(spannedLog);
                htmlText.append("\n"); // 添加换行符，便于区分不同日志
                scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
            }
        });

    }

    @Override
    public void setToken(String token) {
        editor.putString("token", token);
        editor.apply(); // 异步保存数据
    }

    @Override
    public void setBannerText(String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (text.indexOf("未暂停") > 0) {
                    statusText.setTextColor(0xFFE91E63);
                } else {
                    statusText.setTextColor(0xFF0E9014);
                }
                statusText.setText(text);
            }
        });
    }
}