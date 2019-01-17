package com.gottlicher.notifrepeater;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.text.SpannableString;
import android.util.Log;

public class NotifListenerService extends NotificationListenerService {

    final String GOOGLE_VOICE_PACKAGE = "com.google.android.apps.googlevoice";
    final String DISCORD_PACKAGE = "com.discord";

    final String CHANNEL_ID = "Forwaded";
    final String TAG = "NOTIF";


    @Override
    public IBinder onBind(Intent intent) {
        Log.d (TAG, "Service bound");
        return super.onBind(intent);
    }

    @Override
    public void onListenerConnected() {

        Log.d (TAG, "Connected");
    }

    @Override
    public void onListenerDisconnected() {
        Log.d(TAG, "Disconnected");
        requestRebind(ComponentName.createRelative(this.getApplicationContext().getPackageName(), "NotifListenerService"));
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn, RankingMap rankingMap) {
        Log.d (TAG, "Removed" + sbn.getPackageName());
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn, RankingMap rankingMap, int reason) {
        if (reason != REASON_ERROR) {
            Log.d (TAG, "Removed " + sbn.getPackageName() + " due to reason" + reason);
            return;
        }
        Log.d (TAG, "Removed " + sbn.getPackageName() + " due to REASON_ERROR");

        AppHistoryHelper.addNotificationOccurence(getApplicationContext(), sbn.getPackageName());
        postRepeatNotif(sbn);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.d (TAG, "Removed" + sbn.getPackageName());
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Log.d (TAG, "New notif from " + sbn.getPackageName());
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn, RankingMap rankingMap) {
        Log.d (TAG, "New notif from " + sbn.getPackageName());
    }

    void postRepeatNotif (StatusBarNotification sbn) {

        int iconRes = AppHistoryHelper.getIconFor(getApplicationContext(), sbn.getPackageName());
        String appLauncherName = AppHistoryHelper.getAppNameFromPackage(getApplicationContext(), sbn.getPackageName());
        createNotificationChannel();

        CharSequence title = sbn.getNotification().extras.getCharSequence("android.title", "Failed to parse");
        CharSequence text = sbn.getNotification().extras.getCharSequence("android.text", "Failed to parse");

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(iconRes)
                .setContentTitle(appLauncherName + ": " + title.toString())
                .setContentText(text.toString())
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(sbn.getNotification().contentIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(sbn.getId(), mBuilder.build());
    }

    private void createNotificationChannel() {
        CharSequence name = getString(R.string.notif_channel_name);
        String description = getString(R.string.notif_channel_name);
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
        channel.setDescription(description);
        // Register the channel with the system; you can't change the importance
        // or other notification behaviors after this
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);

    }
}
