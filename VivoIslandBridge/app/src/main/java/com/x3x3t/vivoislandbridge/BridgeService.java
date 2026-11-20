package com.x3x3t.vivoislandbridge;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

public class BridgeService extends Service {
    private static final String CHANNEL = "vib_media";
    private MediaSessionCompat session;

    @Override public void onCreate() {
        super.onCreate();
        createChannel();
        session = new MediaSessionCompat(this, "VivoIslandBridge");
        session.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        session.setMetadata(new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, "Vivo Island Bridge — тест")
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "Проверка OriginOS")
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, 210000).build());
        session.setPlaybackState(new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
                .setState(PlaybackStateCompat.STATE_PLAYING, 0, 1f).build());
        session.setActive(true);
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
        Notification n = new NotificationCompat.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle("Vivo Island Bridge — тест")
                .setContentText("Проверка отображения медиасессии")
                .setContentIntent(pi).setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle().setMediaSession(session.getSessionToken()))
                .build();
        startForeground(7, n);
    }
    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel c = new NotificationChannel(CHANNEL, "Тест медиасессии", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(c);
        }
    }
    @Override public int onStartCommand(Intent intent, int flags, int startId) { return START_STICKY; }
    @Override public void onDestroy() { if (session != null) { session.setActive(false); session.release(); } stopForeground(true); super.onDestroy(); }
    @Override public IBinder onBind(Intent intent) { return null; }
}
