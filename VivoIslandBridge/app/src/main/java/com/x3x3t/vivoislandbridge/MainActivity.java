package com.x3x3t.vivoislandbridge;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private TextView status;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(36, 48, 36, 36);

        TextView title = new TextView(this);
        title.setText("Vivo Island Bridge 0.1.1\n\nТест медиасессии\n\nНажми кнопку ниже. Сначала приложение проверит разрешение на уведомления, затем запустит тестовый музыкальный сеанс.");
        title.setTextSize(18);
        layout.addView(title);

        status = new TextView(this);
        status.setText("\nСтатус: ожидание");
        status.setTextSize(15);
        layout.addView(status);

        Button start = new Button(this);
        start.setText("Запустить тестовую медиасессию");
        start.setOnClickListener(v -> startTest());
        layout.addView(start);

        Button stop = new Button(this);
        stop.setText("Остановить тест");
        stop.setOnClickListener(v -> {
            stopService(new Intent(this, BridgeService.class));
            status.setText("Статус: тест остановлен");
        });
        layout.addView(stop);
        setContentView(layout);
    }

    private void startTest() {
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            status.setText("Статус: нужно разрешение на уведомления");
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 10);
            return;
        }
        launchService();
    }

    private void launchService() {
        Intent intent = new Intent(this, BridgeService.class);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent); else startService(intent);
        status.setText("Статус: тестовая медиасессия запущена");
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 10) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchService();
            } else {
                status.setText("Статус: уведомления запрещены. Разреши их в настройках приложения и нажми запуск ещё раз.");
            }
        }
    }
}
