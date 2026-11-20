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
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(36, 48, 36, 36);
        TextView title = new TextView(this);
        title.setText("Vivo Island Bridge 0.1.0\n\nТест медиасессии\n\nНажми кнопку ниже. Приложение создаст тестовый музыкальный сеанс. Проверь, появится ли он в Origin Player / Origin Island.");
        title.setTextSize(18);
        layout.addView(title);
        Button start = new Button(this);
        start.setText("Запустить тестовую медиасессию");
        start.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 10);
            }
            Intent intent = new Intent(this, BridgeService.class);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent); else startService(intent);
        });
        layout.addView(start);
        Button stop = new Button(this);
        stop.setText("Остановить тест");
        stop.setOnClickListener(v -> stopService(new Intent(this, BridgeService.class)));
        layout.addView(stop);
        setContentView(layout);
    }
}
