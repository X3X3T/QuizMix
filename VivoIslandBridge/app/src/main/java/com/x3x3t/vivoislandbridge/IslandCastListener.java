package com.x3x3t.vivoislandbridge;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;

import java.util.List;

public class IslandCastListener extends NotificationListenerService {
    private static final String TAG = "VivoIslandBridge";
    private String activePackage;

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || sbn.getNotification() == null) return;
        String sourcePackage = sbn.getPackageName();
        if (getPackageName().equals(sourcePackage)) return;
        if (!isVkPackage(sourcePackage)) return;

        Notification notification = sbn.getNotification();
        Bundle extras = notification.extras;
        if (extras == null || !isMediaNotification(notification, extras)) return;

        String title = null;
        String artist = null;
        boolean playing = true;

        try {
            MediaSessionManager manager = getSystemService(MediaSessionManager.class);
            if (manager != null) {
                List<MediaController> controllers =
                        manager.getActiveSessions(new ComponentName(this, IslandCastListener.class));
                for (MediaController controller : controllers) {
                    if (sourcePackage.equals(controller.getPackageName())) {
                        MediaMetadata metadata = controller.getMetadata();
                        if (metadata != null) {
                            title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
                            artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
                            if (TextUtils.isEmpty(artist)) {
                                artist = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST);
                            }
                        }
                        PlaybackState state = controller.getPlaybackState();
                        if (state != null) {
                            playing = state.getState() == PlaybackState.STATE_PLAYING ||
                                    state.getState() == PlaybackState.STATE_BUFFERING;
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Не удалось прочитать активную медиасессию", e);
        }

        if (TextUtils.isEmpty(title)) title = stringValue(extras.getCharSequence(Notification.EXTRA_TITLE));
        if (TextUtils.isEmpty(artist)) artist = stringValue(extras.getCharSequence(Notification.EXTRA_TEXT));
        CharSequence bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
        if (!TextUtils.isEmpty(bigText) && (TextUtils.isEmpty(artist) || bigText.toString().length() > artist.length())) {
            artist = bigText.toString();
        }

        if (TextUtils.isEmpty(title) && TextUtils.isEmpty(artist)) return;
        if (TextUtils.isEmpty(title)) title = artist;
        if (TextUtils.isEmpty(artist)) artist = "VK Музыка";

        PendingIntent click = notification.contentIntent;
        activePackage = sourcePackage;
        if (getPreferences(MODE_PRIVATE).getBoolean("bridge_enabled", false)) {
            OriginIslandPublisher.publish(this, sourcePackage, title, artist, click, playing);
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        if (sbn == null) return;
        if (activePackage != null && activePackage.equals(sbn.getPackageName())) {
            activePackage = null;
            OriginIslandPublisher.unmount(this);
        }
    }

    private boolean isMediaNotification(Notification notification, Bundle extras) {
        if (Notification.CATEGORY_TRANSPORT.equals(notification.category)) return true;
        return extras.containsKey(Notification.EXTRA_MEDIA_SESSION) ||
                extras.containsKey(Notification.EXTRA_MEDIA_SESSION) ||
                !TextUtils.isEmpty(extras.getCharSequence(Notification.EXTRA_TITLE));
    }

    private boolean isVkPackage(String packageName) {
        if (packageName == null) return false;
        String p = packageName.toLowerCase();
        if (p.contains("vkontakte") || p.contains(".vk") || p.startsWith("vk.")) return true;
        try {
            CharSequence label = getPackageManager().getApplicationLabel(
                    getPackageManager().getApplicationInfo(packageName, 0));
            String name = label == null ? "" : label.toString().toLowerCase();
            return name.contains("vk") || name.contains("вк") || name.contains("музык");
        } catch (Exception ignored) {
            return false;
        }
    }

    private String stringValue(CharSequence value) {
        return value == null ? null : value.toString();
    }
}
