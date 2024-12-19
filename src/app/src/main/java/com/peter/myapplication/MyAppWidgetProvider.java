package com.peter.myapplication;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;
import android.app.AlarmManager;


public class MyAppWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_PAUSE = "com.peter.myapplication.ACTION_PAUSE";

    public static final String ACTION_UPDATE = "com.peter.myapplication.ACTION_UPDATE";

    private static final int UPDATE_INTERVAL_MS = 3600000;

    public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

        // 更新Widget的视图，例如更新TextView的文本
        views.setTextViewText(R.id.widget_text, "Updated Value!");
        // 更新Widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // 为每个 widget 更新 UI
        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

            // 创建一个 Intent，触发 ACTION_PAUSE 事件
            Intent intent = new Intent(context, MyAppWidgetProvider.class);
            intent.setAction(ACTION_PAUSE);

            // 将 Intent 包装在 PendingIntent 中，并添加 FLAG_IMMUTABLE 标志
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            // 设置按钮点击事件
            views.setOnClickPendingIntent(R.id.close_button, pendingIntent);

            // 更新 widget
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
        // 设置定时更新
//        setAlarm(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        // 检查是否是 ACTION_PAUSE 的事件
        if (ACTION_PAUSE.equals(intent.getAction())) {
            // 在这里处理按钮点击后的逻辑，不启动 Activity
            pause(context); // 调用 MainActivity 中的静态方法
            // 更新按钮颜色
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
            // 如果要动态改变颜色，你可以创建一个新的 shape Drawable，设置颜色后应用
            views.setInt(R.id.close_button, "setBackgroundResource", R.drawable.rounded_button_background_clicked);

            // 获取所有实例的 Widget，并更新它们
            ComponentName widget = new ComponentName(context, MyAppWidgetProvider.class);
            appWidgetManager.updateAppWidget(widget, views);
        }
    }

    private void pause(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("sharedPrefs", context.MODE_PRIVATE);
        String token = sharedPreferences.getString("token", "");

        try {
            HttpUtil.pause(token, null);
        } catch (Exception e) {
            e.printStackTrace();//FIXME
        }
    }

    private void setAlarm(Context context) {
        Intent intent = new Intent(context, MyAppWidgetProvider.class);
        intent.setAction(ACTION_UPDATE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.RTC, System.currentTimeMillis(), UPDATE_INTERVAL_MS, pendingIntent);
    }

}

