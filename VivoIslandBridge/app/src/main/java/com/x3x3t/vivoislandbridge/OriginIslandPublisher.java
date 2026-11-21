package com.x3x3t.vivoislandbridge;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.Rect;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.VectorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.ScaleDrawable;
import android.graphics.drawable.RippleDrawable;
import android.content.pm.PackageManager;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.util.ArrayList;
import java.util.Collections;

public final class OriginIslandPublisher {
    private static final String TAG = "VivoIslandBridge";
    private static final String CHANNEL_ID = "vib_origin_island_v1";
    private static final String NOTIFICATION_TAG = "VIVO_SUPERX_TAG";
    private static final int NOTIFICATION_ID = 6101;
    private static boolean cardPosted = false;
    private static int changeRecord = 1;
    private static final Handler HANDLER = new Handler(Looper.getMainLooper());

    private OriginIslandPublisher() {}

    public static void publish(Context context, String sourcePackage, String title, String artist,
                               PendingIntent sourceClick, boolean playing) {
        try {
            if (!context.getSharedPreferences("vivo_bridge", Context.MODE_PRIVATE)
                    .getBoolean("bridge_enabled", false)) return;

            createChannel(context);
            Icon appIcon = loadApplicationIcon(context, sourcePackage);
            String line = TextUtils.isEmpty(artist) ? title : artist + " · " + title;
            if (line.length() > 58) line = line.substring(0, 55) + "…";

            PendingIntent click = sourceClick;
            if (click == null) {
                Intent open = new Intent(context, MainActivity.class);
                click = PendingIntent.getActivity(context, 6101, open,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            }

            Bundle extras = new Bundle();
            extras.putInt("notification.superx.operation", cardPosted ? 1 : 0);
            extras.putBoolean("notification.superx.showNotify", true);
            extras.putInt("notification.superx.template", 4);
            extras.putString("notification.superx.scene", "TRAIN");
            extras.putInt("notification.superx.displays", 65809);
            extras.putInt("notification.superx.changedRecord", ++changeRecord);
            extras.putInt("notification.superx.changeRecord", changeRecord);
            extras.putParcelable("notification.superx.clickResp", click);

            Bundle base = new Bundle();
            base.putParcelable("notification.superx.baseInfos.icon", appIcon);
            base.putCharSequence("notification.superx.baseInfos.title", artist);
            base.putCharSequence("notification.superx.baseInfos.content", title);
            extras.putBundle("notification.superx.baseInfos", base);

            Bundle shortInfo = new Bundle();
            shortInfo.putParcelable("notification.superx.shortInfos.icon", appIcon);
            shortInfo.putCharSequence("notification.superx.shortInfos.title", artist);
            shortInfo.putCharSequence("notification.superx.shortInfos.content", title);
            extras.putBundle("notification.superx.shortInfos", shortInfo);

            Bundle capsule = new Bundle();
            capsule.putParcelable("notification.superx.capsule.icon", appIcon);
            capsule.putCharSequence("notification.superx.capsule.content", "VK Музыка");
            capsule.putInt("notification.superx.capsule.state", playing ? 1 : 0);
            extras.putBundle("notification.superx.capsule", capsule);

            Bundle island = new Bundle();
            island.putInt("island.superx.leftTemplate", 1);
            island.putInt("island.superx.rightTemplate", 1);
            Bundle left = new Bundle();
            left.putParcelable("island.superx.leftInfo.icon", appIcon);
            left.putCharSequence("island.superx.leftInfo.text", line);
            Bundle right = new Bundle();
            ArrayList<String> waveColors = new ArrayList<>();
            waveColors.add("#FFFFFFFF");
            right.putStringArrayList("island.superx.rightInfo.waveColor", waveColors);
            right.putInt("island.superx.rightInfo.waveState", playing ? 2 : 1);
            island.putBundle("island.superx.leftInfo", left);
            island.putBundle("island.superx.rightInfo", right);
            island.putBoolean("island.superx.forceShow", true);
            island.putBoolean("island.superx.forceShowCard", true);
            island.putInt("island.superx.clickType", 1);
            island.putInt("island.superx.showTime", 180);
            extras.putBundle("notification.superx.island", island);

            Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_media_play)
                    .setContentTitle(title)
                    .setContentText(artist)
                    .setContentIntent(click)
                    .setCategory(Notification.CATEGORY_TRANSPORT)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setExtras(extras)
                    .build();

            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.notify(NOTIFICATION_TAG, NOTIFICATION_ID, notification);
                cardPosted = true;
            }
        } catch (Throwable error) {
            Log.e(TAG, "Не удалось отправить карточку в Origin Island", error);
        }
    }

    public static void unmount(Context context) {
        if (!cardPosted) return;
        try {
            createChannel(context);
            Bundle extras = new Bundle();
            extras.putInt("notification.superx.operation", 2);
            extras.putBoolean("notification.superx.showNotify", false);
            extras.putInt("notification.superx.template", 4);
            extras.putString("notification.superx.scene", "TRAIN");
            extras.putInt("notification.superx.changedRecord", Integer.MAX_VALUE);
            extras.putInt("notification.superx.changeRecord", Integer.MAX_VALUE);

            Intent open = new Intent(context, MainActivity.class);
            PendingIntent click = PendingIntent.getActivity(context, 6102, open,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            extras.putParcelable("notification.superx.clickResp", click);

            Notification end = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_media_play)
                    .setContentTitle("VK Музыка")
                    .setContentText("Воспроизведение завершено")
                    .setContentIntent(click)
                    .setOnlyAlertOnce(true)
                    .setExtras(extras)
                    .build();

            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.notify(NOTIFICATION_TAG, NOTIFICATION_ID, end);
                HANDLER.postDelayed(() -> {
                    try {
                        manager.cancel(NOTIFICATION_TAG, NOTIFICATION_ID);
                    } catch (Exception ignored) {}
                }, 350);
            }
        } catch (Throwable error) {
            Log.w(TAG, "Не удалось завершить карточку Origin Island", error);
        } finally {
            cardPosted = false;
        }
    }

    private static void createChannel(Context context) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null || Build.VERSION.SDK_INT < 26) return;
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "VK Музыка — Origin Island", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Карточка текущего трека на системном островке Vivo");
            channel.setShowBadge(false);
            manager.createNotificationChannel(channel);
        }
    }

    private static Icon loadApplicationIcon(Context context, String packageName) {
        try {
            Drawable drawable = context.getPackageManager().getApplicationIcon(packageName);
            int size = (int) (48 * context.getResources().getDisplayMetrics().density);
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, size, size);
            drawable.draw(canvas);
            return Icon.createWithBitmap(bitmap);
        } catch (Exception ignored) {
            return Icon.createWithResource(context, android.R.drawable.ic_media_play);
        }
    }
}
