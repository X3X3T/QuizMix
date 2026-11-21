package com.x3x3t.vivoislandbridge;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public final class MessageIslandPublisher {
    private static final String TAG = "VivoIslandBridge";
    private static final String CHANNEL_ID = "vib_live_messages_v1";
    private static final int MESSAGE_ID = 7101;
    private static final int TEST_ID = 7102;
    private static final long DISPLAY_TIME_MS = 12000L;
    private static final String PROMOTED_EXTRA = "android.requestPromotedOngoing";
    private static final String SHORT_CRITICAL_EXTRA = "android.shortCriticalText";

    private MessageIslandPublisher() {}

    public static void publish(Context context, String sourcePackage, String appLabel,
                               String sender, String message, PendingIntent sourceClick) {
        if (!context.getSharedPreferences("vivo_bridge", Context.MODE_PRIVATE)
                .getBoolean("bridge_enabled", false)) return;

        post(context, MESSAGE_ID, sourcePackage, appLabel, sender, message, sourceClick);
    }

    public static void publishTest(Context context) {
        Intent open = new Intent(context, MainActivity.class);
        PendingIntent click = PendingIntent.getActivity(context, TEST_ID, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        post(context, TEST_ID, context.getPackageName(), "Vivo Island Bridge",
                "Проверка островка",
                "Если эта карточка появилась на Origin Island, стандартный Android Live Update работает.",
                click);
    }

    public static void cancelAll(Context context) {
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.cancel(MESSAGE_ID);
            manager.cancel(TEST_ID);
        }
    }

    private static void post(Context context, int id, String sourcePackage, String appLabel,
                             String sender, String message, PendingIntent sourceClick) {
        try {
            createChannel(context);

            String safeSender = clean(sender, 80);
            String safeMessage = clean(message, 500);
            if (TextUtils.isEmpty(safeSender)) safeSender = appLabel;
            if (TextUtils.isEmpty(safeMessage)) safeMessage = "Новое сообщение";

            PendingIntent click = sourceClick;
            if (click == null && !TextUtils.isEmpty(sourcePackage)) {
                Intent launch = context.getPackageManager().getLaunchIntentForPackage(sourcePackage);
                if (launch != null) {
                    click = PendingIntent.getActivity(context, id, launch,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                }
            }
            if (click == null) {
                Intent open = new Intent(context, MainActivity.class);
                click = PendingIntent.getActivity(context, id, open,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            }

            Bitmap largeIcon = loadIcon(context, sourcePackage);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_email)
                    .setLargeIcon(largeIcon)
                    .setContentTitle(safeSender)
                    .setContentText(safeMessage)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(safeMessage))
                    .setSubText(appLabel)
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                    .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                    .setContentIntent(click)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setSilent(true)
                    .setAutoCancel(false)
                    .setTimeoutAfter(DISPLAY_TIME_MS)
                    .setWhen(System.currentTimeMillis())
                    .setShowWhen(true);

            Notification notification = builder.build();
            if (notification.extras == null) notification.extras = new Bundle();

            // Android 16 Live Updates / promoted ongoing notifications.
            // This is the public AOSP path that OriginOS 6 can render; no SuperX
            // package spoofing, hidden API, or vendor-specific notification tag.
            notification.extras.putBoolean(PROMOTED_EXTRA, true);
            notification.extras.putString(SHORT_CRITICAL_EXTRA, shorten(safeSender, 7));
            notification.flags |= Notification.FLAG_ONGOING_EVENT;

            NotificationManager manager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.notify(id, notification);
                Log.i(TAG, "Promoted message notification posted: " + appLabel + " / " + safeSender);
            }
        } catch (Throwable error) {
            Log.e(TAG, "Не удалось показать сообщение на Origin Island", error);
        }
    }

    private static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null || manager.getNotificationChannel(CHANNEL_ID) != null) return;

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Сообщения на Origin Island", NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("Краткое отображение новых сообщений Telegram и AyuGram");
        channel.setShowBadge(false);
        channel.setSound(null, null);
        channel.enableVibration(false);
        channel.enableLights(false);
        manager.createNotificationChannel(channel);
    }

    private static Bitmap loadIcon(Context context, String packageName) {
        try {
            Drawable drawable = context.getPackageManager().getApplicationIcon(packageName);
            int size = (int) (48 * context.getResources().getDisplayMetrics().density);
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, size, size);
            drawable.draw(canvas);
            return bitmap;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String clean(String value, int maxLength) {
        if (value == null) return null;
        String result = value.replace((char) 0, ' ').trim();
        if (result.length() > maxLength) result = result.substring(0, maxLength - 1) + "…";
        return result;
    }

    private static String shorten(String value, int maxLength) {
        if (TextUtils.isEmpty(value)) return "Сообщ.";
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
