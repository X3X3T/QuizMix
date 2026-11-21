package com.x3x3t.vivoislandbridge;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

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
        title.setText("Vivo Island Bridge 0.2\n\nПеренос VK Музыки на Origin Island\n\nПриложение читает уведомления VK Музыки и передаёт название трека в системный формат OriginOS. Оно не подменяет Spotify или системный плеер.");
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
        enable.setText("Включить перенос на островок");
        enable.setOnClickListener(v -> enableBridge());
        layout.addView(enable);

        Button disable = new Button(this);
        disable.setText("Остановить перенос");
        disable.setOnClickListener(v -> {
            getPreferences(MODE_PRIVATE).edit().putBoolean("bridge_enabled", false).apply();
            OriginIslandPublisher.unmount(this);
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
        getPreferences(MODE_PRIVATE).edit().putBoolean("bridge_enabled", true).apply();
        refreshStatus();
        if (!hasListenerAccess()) {
            try {
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
            } catch (Exception ignored) {
                status.setText("Открой настройки доступа к уведомлениям и разреши Vivo Island Bridge.");
            }
        }
    }

    private boolean hasListenerAccess() {
        String enabled = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        if (enabled == null) return false;
        String component = new ComponentName(this, IslandCastListener.class).flattenToString();
        return enabled.contains(component);
    }

    private void refreshStatus() {
        boolean enabled = getPreferences(MODE_PRIVATE).getBoolean("bridge_enabled", false);
        boolean listener = hasListenerAccess();
        boolean notifications = Build.VERSION.SDK_INT < 33 ||
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;

        if (!notifications) {
            status.setText("Статус: нужно разрешить приложению отправлять уведомления.");
        } else if (!listener) {
            status.setText("Статус: нужен доступ к уведомлениям. Разреши его для Vivo Island Bridge в системных настройках.");
        } else if (!enabled) {
            status.setText("Статус: доступ выдан. Нажми «Включить перенос на островок».");
        } else {
            status.setText("Статус: включено. Ожидаю уведомление VK Музыки. Само приложение можно свернуть.");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NOTIFICATIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableBridge();
            } else {
                status.setText("Статус: разрешение на уведомления не выдано.");
            }
        }
    }
}
