package com.x3x3t.vivoislandbridge;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.reflect.Method;

public class MainActivity extends Activity {
    private static final int REQUEST_NOTIFICATIONS = 10;
    private TextView status;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (24 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad * 2, pad, pad);

        TextView title = new TextView(this);
        title.setText("Vivo Island Bridge\n\nTelegram + AyuGram\n\nПриложение переносит новые сообщения из уведомлений Telegram и AyuGram в системный Origin Island через стандартные Android 16 Live Updates. Содержимое обрабатывается только на телефоне.");
        title.setTextSize(18);
        layout.addView(title);

        status = new TextView(this);
        status.setTextSize(15);
        status.setPadding(0, pad, 0, pad);
        layout.addView(status);

        Button access = new Button(this);
        access.setText("Разрешить доступ к уведомлениям");
        access.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
            } catch (Exception e) {
                status.setText("Не удалось открыть настройки доступа к уведомлениям.");
            }
        });
        layout.addView(access);

        Button enable = new Button(this);
        enable.setText("Включить Telegram и AyuGram");
        enable.setOnClickListener(v -> enableBridge());
        layout.addView(enable);

        Button test = new Button(this);
        test.setText("Тест островка (затем сверни приложение)");
        test.setOnClickListener(v -> postTest());
        layout.addView(test);

        Button disable = new Button(this);
        disable.setText("Остановить перенос");
        disable.setOnClickListener(v -> {
            getSharedPreferences("vivo_bridge", MODE_PRIVATE).edit()
                    .putBoolean("bridge_enabled", false).apply();
            MessageIslandPublisher.cancelAll(this);
            refreshStatus();
        });
        layout.addView(disable);

        setContentView(layout);
        refreshStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    private void enableBridge() {
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
            return;
        }
        getSharedPreferences("vivo_bridge", MODE_PRIVATE).edit()
                .putBoolean("bridge_enabled", true).apply();
        refreshStatus();
        if (!hasListenerAccess()) {
            try {
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
            } catch (Exception ignored) {
                status.setText("Открой настройки доступа к уведомлениям и разреши Vivo Island Bridge.");
            }
        }
    }

    private void postTest() {
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
            status.setText("Сначала разреши отправку уведомлений, затем нажми тест ещё раз.");
            return;
        }
        MessageIslandPublisher.publishTest(this);
        status.setText("Тест отправлен. Сверни приложение на главный экран и проверь Origin Island. Уведомление исчезнет примерно через 12 секунд.");
    }

    private boolean hasListenerAccess() {
        String enabled = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        if (enabled == null) return false;
        String component = new ComponentName(this, IslandCastListener.class).flattenToString();
        return enabled.contains(component);
    }

    private String promotionStatus() {
        if (Build.VERSION.SDK_INT < 36) return "Для стандартного Live Update нужен Android 16";
        try {
            NotificationManager manager = getSystemService(NotificationManager.class);
            Method method = NotificationManager.class.getMethod("canPostPromotedNotifications");
            Object result = method.invoke(manager);
            return Boolean.TRUE.equals(result)
                    ? "Android разрешает Live Updates"
                    : "Android сейчас не разрешает Live Updates";
        } catch (Throwable ignored) {
            return "Прошивка не раскрывает статус Live Updates";
        }
    }

    private void refreshStatus() {
        boolean enabled = getSharedPreferences("vivo_bridge", MODE_PRIVATE)
                .getBoolean("bridge_enabled", false);
        boolean listener = hasListenerAccess();
        boolean notifications = Build.VERSION.SDK_INT < 33 ||
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;

        String state;
        if (!notifications) {
            state = "Нужно разрешить приложению отправлять уведомления.";
        } else if (!listener) {
            state = "Нужен доступ к уведомлениям. Разреши его для Vivo Island Bridge в системных настройках.";
        } else if (!enabled) {
            state = "Доступ выдан. Нажми «Включить Telegram и AyuGram».";
        } else {
            state = "Включено. Ожидаю новые сообщения Telegram или AyuGram.";
        }

        status.setText("Статус: " + state + "\n\nLive Updates: " + promotionStatus());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NOTIFICATIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableBridge();
            } else {
                status.setText("Разрешение на уведомления не выдано.");
            }
        }
    }
}
