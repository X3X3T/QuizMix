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
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.lang.reflect.Method;
import java.util.ArrayList;

public final class OriginIslandPublisher {
    private static final String TAG = "VivoIslandBridge";
    private static final String CHANNEL_ID = "vib_origin_island_v2";
    private static final String NOTIFICATION_TAG = "VIVO_SUPERX_TAG";
    private static final int NOTIFICATION_ID = 6101;
    private static final String SCENE = "TRAIN";
    private static final int DISPLAYS = 65809;
    private static final Handler HANDLER = new Handler(Looper.getMainLooper());

    private OriginIslandPublisher() {}

    public static void publish(Context context, String sourcePackage, String title, String artist,
                               PendingIntent sourceClick, boolean playing) {
        try {
            if (!context.getSharedPreferences("vivo_bridge", Context.MODE_PRIVATE)
                    .getBoolean("bridge_enabled", false)) return;

            createChannel(context);
            registerScene(context);

            Icon sourceIcon = loadApplicationIcon(context, sourcePackage);
            String rightText = TextUtils.isEmpty(artist) ? title : artist + " · " + title;
            if (rightText.length() > 58) rightText = rightText.substring(0, 55) + "…";

            PendingIntent click = sourceClick;
            if (click == null) {
                Intent open = context.getPackageManager().getLaunchIntentForPackage(sourcePackage);
                if (open == null) open = new Intent(context, MainActivity.class);
                click = PendingIntent.getActivity(context, 6101, open,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            }

            // Build the same nested SuperX payload used by the reverse-engineered
            // OriginOS sender: operation=0, baseInfos, capsule, infos, shortInfos,
            // and the island's left/right template bundles.
            Bundle extras = new Bundle();
            extras.putInt("notification.superx.operation", 0);
            extras.putBoolean("notification.superx.showNotify", true);
            extras.putInt("notification.superx.template", 1);
            extras.putString("notification.superx.scene", SCENE);
            extras.putInt("notification.superx.displays", DISPLAYS);
            extras.putInt("notification.superx.changedRecord", 0);
            extras.putParcelable("notification.superx.clickResp", click);

            Bundle base = new Bundle();
            base.putCharSequence("notification.superx.baseInfos.title", title);
            base.putCharSequence("notification.superx.baseInfos.content", rightText);
            base.putParcelable("notification.superx.baseInfos.icon", sourceIcon);
            extras.putBundle("notification.superx.baseInfos", base);

            Bundle capsule = new Bundle();
            capsule.putInt("notification.superx.capsule.state", playing ? 1 : 0);
            capsule.putCharSequence("notification.superx.capsule.content", title);
            capsule.putParcelable("notification.superx.capsule.icon", sourceIcon);
            extras.putBundle("notification.superx.capsule", capsule);

            Bundle infos = new Bundle();
            infos.putString("notification.superx.infos.describe", title);
            infos.putString("notification.superx.infos.coreInfo", rightText);
            infos.putParcelable("notification.superx.infos.image", sourceIcon);
            extras.putBundle("notification.superx.infos", infos);

            Bundle shortInfos = new Bundle();
            shortInfos.putString("notification.superx.shortInfos.describeShort", title);
            shortInfos.putString("notification.superx.shortInfos.coreInfoShort", rightText);
            shortInfos.putParcelable("notification.superx.shortInfos.image", sourceIcon);
            extras.putBundle("notification.superx.shortInfos", shortInfos);

            Bundle island = new Bundle();
            island.putInt("island.superx.leftTemplate", 1);
            island.putInt("island.superx.rightTemplate", 4); // text + icon

            Bundle left = new Bundle();
            left.putString("island.superx.leftInfo.content", title);
            left.putParcelable("island.superx.leftInfo.icon", sourceIcon);
            island.putBundle("island.superx.leftInfo", left);

            Bundle right = new Bundle();
            right.putString("island.superx.rightInfo.content", rightText);
            right.putParcelable("island.superx.rightInfo.icon", sourceIcon);
            island.putBundle("island.superx.rightInfo", right);
            island.putBoolean("island.superx.forceShow", true);
            island.putBoolean("island.superx.forceShowCard", true);
            extras.putBundle("notification.superx.island", island);

            Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_media_play)
                    .setContentTitle(title)
                    .setContentText(rightText)
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
                Log.i(TAG, "SuperX notification posted for " + sourcePackage);
            }
        } catch (Throwable error) {
            Log.e(TAG, "Не удалось отправить карточку в Origin Island", error);
        }
    }

    public static void unmount(Context context) {
        try {
            createChannel(context);
            Bundle extras = new Bundle();
            extras.putInt("notification.superx.operation", 2);
            extras.putBoolean("notification.superx.showNotify", false);
            extras.putInt("notification.superx.template", 1);
            extras.putString("notification.superx.scene", SCENE);
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
                HANDLER.postDelayed(() -> manager.cancel(NOTIFICATION_TAG, NOTIFICATION_ID), 350);
            }
        } catch (Throwable error) {
            Log.w(TAG, "Не удалось завершить карточку Origin Island", error);
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

    private static void registerScene(Context context) {
        try {
            Method method = NotificationManager.class.getMethod(
                    "setSuperXInfosSceneList", java.util.List.class,
                    java.util.List.class, java.util.List.class, java.util.List.class);
            ArrayList<String> scenes = new ArrayList<>();
            scenes.add(SCENE);
            ArrayList<String> enabled = new ArrayList<>();
            enabled.add("true");
            ArrayList<String> packages = new ArrayList<>();
            packages.add(context.getPackageName());
            ArrayList<String> allowed = new ArrayList<>();
            allowed.add("true");
            method.invoke(context.getSystemService(Context.NOTIFICATION_SERVICE),
                    scenes, enabled, packages, allowed);
        } catch (Throwable error) {
            Log.w(TAG, "OriginOS scene registration unavailable", error);
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
