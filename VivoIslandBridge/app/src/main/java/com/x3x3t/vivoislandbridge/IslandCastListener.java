package com.x3x3t.vivoislandbridge;

import android.app.Notification;
import android.app.PendingIntent;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class IslandCastListener extends NotificationListenerService {
    private static final String TAG = "VivoIslandBridge";
    private static final Map<String, String> LAST_CONTENT = new ConcurrentHashMap<>();

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || sbn.getNotification() == null) return;
        if (!getSharedPreferences("vivo_bridge", MODE_PRIVATE)
                .getBoolean("bridge_enabled", false)) return;

        String sourcePackage = sbn.getPackageName();
        if (TextUtils.isEmpty(sourcePackage) || getPackageName().equals(sourcePackage)) return;
        if (!isTelegramFamily(sourcePackage)) return;

        Notification notification = sbn.getNotification();
        if ((notification.flags & Notification.FLAG_ONGOING_EVENT) != 0) return;

        // Group summaries usually contain only an unread-count summary. Their child
        // notifications carry the sender and message, so forwarding both would duplicate it.
        if ((notification.flags & Notification.FLAG_GROUP_SUMMARY) != 0) return;

        Bundle extras = notification.extras;
        if (extras == null) return;

        MessageData data = extractMessage(sourcePackage, extras);
        if (data == null) return;

        String key = sbn.getKey();
        String fingerprint = data.sender + "\n" + data.message;
        String previous = LAST_CONTENT.put(key, fingerprint);
        if (fingerprint.equals(previous)) return;

        String appLabel = getAppLabel(sourcePackage);
        PendingIntent click = notification.contentIntent;
        MessageIslandPublisher.publish(
                this, sourcePackage, appLabel, data.sender, data.message, click);

        Log.i(TAG, "Forwarded message notification from " + sourcePackage);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        if (sbn != null) LAST_CONTENT.remove(sbn.getKey());
    }

    private boolean isTelegramFamily(String packageName) {
        String p = packageName.toLowerCase();
        if (p.equals("org.telegram.messenger") ||
                p.equals("org.telegram.messenger.web") ||
                p.equals("com.radolyn.ayugram") ||
                p.contains("ayugram") ||
                p.contains("telegram")) {
            return true;
        }

        try {
            CharSequence label = getPackageManager().getApplicationLabel(
                    getPackageManager().getApplicationInfo(packageName, 0));
            String name = label == null ? "" : label.toString().toLowerCase();
            return name.contains("telegram") || name.contains("ayugram") || name.contains("аюграм");
        } catch (Exception ignored) {
            return false;
        }
    }

    private String getAppLabel(String packageName) {
        try {
            CharSequence label = getPackageManager().getApplicationLabel(
                    getPackageManager().getApplicationInfo(packageName, 0));
            String name = label == null ? packageName : label.toString();
            String normalized = name.toLowerCase();
            if (normalized.contains("ayu") || normalized.contains("аю")) return "AyuGram";
            return "Telegram";
        } catch (Exception ignored) {
            return packageName.toLowerCase().contains("ayu") ? "AyuGram" : "Telegram";
        }
    }

    private MessageData extractMessage(String packageName, Bundle extras) {
        String appLabel = getAppLabel(packageName);
        String title = value(extras.getCharSequence(Notification.EXTRA_TITLE));
        String bigTitle = value(extras.getCharSequence(Notification.EXTRA_TITLE_BIG));
        String body = value(extras.getCharSequence(Notification.EXTRA_BIG_TEXT));
        if (TextUtils.isEmpty(body)) body = value(extras.getCharSequence(Notification.EXTRA_TEXT));

        String conversation = value(extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE));
        String messageSender = null;
        String messageText = null;

        try {
            Parcelable[] messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES);
            if (messages != null && messages.length > 0) {
                for (int i = messages.length - 1; i >= 0; i--) {
                    if (!(messages[i] instanceof Bundle)) continue;
                    Bundle message = (Bundle) messages[i];
                    messageText = value(message.getCharSequence("text"));
                    messageSender = personName(message.get("sender_person"));
                    if (TextUtils.isEmpty(messageSender)) {
                        messageSender = personName(message.get("sender"));
                    }
                    if (!TextUtils.isEmpty(messageText) || !TextUtils.isEmpty(messageSender)) break;
                }
            }
        } catch (Throwable error) {
            Log.w(TAG, "Не удалось разобрать MessagingStyle уведомления", error);
        }

        if (!TextUtils.isEmpty(messageText)) body = messageText;

        String sender = !TextUtils.isEmpty(bigTitle) ? bigTitle : title;
        if (isGenericTitle(sender, appLabel)) {
            sender = !TextUtils.isEmpty(messageSender) ? messageSender : conversation;
        }
        if (TextUtils.isEmpty(sender)) sender = messageSender;
        if (TextUtils.isEmpty(sender)) sender = conversation;
        if (TextUtils.isEmpty(sender)) sender = appLabel;

        if (TextUtils.isEmpty(body)) {
            // A few Telegram builds expose a structured MessagingStyle array only.
            body = messageText;
        }
        if (TextUtils.isEmpty(body)) return null;

        String lowerBody = body.toLowerCase();
        if (lowerBody.contains("keep-alive service") ||
                lowerBody.contains("checking for messages") ||
                lowerBody.equals("connecting…") ||
                lowerBody.equals("connecting...")) {
            return null;
        }

        return new MessageData(trim(sender, 80), trim(body, 500));
    }

    private boolean isGenericTitle(String title, String appLabel) {
        if (TextUtils.isEmpty(title)) return true;
        String t = title.trim().toLowerCase();
        return t.equals(appLabel.toLowerCase()) ||
                t.equals("telegram") || t.equals("ayugram") || t.equals("аюграм") ||
                t.equals("new messages") || t.equals("новые сообщения");
    }

    private String personName(Object person) {
        if (person == null) return null;
        if (person instanceof CharSequence) return person.toString();
        if (person instanceof Bundle) {
            Bundle bundle = (Bundle) person;
            String name = value(bundle.getCharSequence("name"));
            if (!TextUtils.isEmpty(name)) return name;
        }
        try {
            Object name = person.getClass().getMethod("getName").invoke(person);
            return name == null ? null : name.toString();
        } catch (Exception ignored) {
            return null;
        }
    }

    private String value(CharSequence text) {
        return text == null ? null : text.toString().trim();
    }

    private String trim(String value, int max) {
        if (value == null) return "";
        String cleaned = value.trim();
        return cleaned.length() <= max ? cleaned : cleaned.substring(0, max - 1) + "…";
    }

    private static final class MessageData {
        final String sender;
        final String message;

        MessageData(String sender, String message) {
            this.sender = sender;
            this.message = message;
        }
    }
}
