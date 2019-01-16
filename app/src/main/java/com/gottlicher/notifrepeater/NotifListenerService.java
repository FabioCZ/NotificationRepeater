package com.gottlicher.notifrepeater;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
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
    public void onListenerConnected() {

        Log.d (TAG, "Connected");
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn, RankingMap rankingMap) {
        Log.d (TAG, "Removed" + sbn.getPackageName());

    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn, RankingMap rankingMap, int reason) {
        Log.d (TAG, "Removed" + sbn.getPackageName() + "reason" + reason);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.d (TAG, "Removed" + sbn.getPackageName());
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Log.d (TAG, "New notif from " + sbn.getPackageName());

        postRepeatNotif(sbn);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn, RankingMap rankingMap) {
        Log.d (TAG, "New notif from " + sbn.getPackageName());
        postRepeatNotif(sbn);
    }


    void postRepeatNotif (StatusBarNotification sbn) {

        int iconRes = 0;
        if (sbn.getPackageName().equals(GOOGLE_VOICE_PACKAGE) ) {
            iconRes = R.drawable.ic_perm_phone_msg_black_24dp;
        } else if (sbn.getPackageName().equals(DISCORD_PACKAGE)) {
            iconRes = R.drawable.ic_textsms_black_24dp;
        } else {
            return;
        }

        final PackageManager pm = getApplicationContext().getPackageManager();
        ApplicationInfo ai;
        try {
            ai = pm.getApplicationInfo(sbn.getPackageName(), 0);
        } catch (final PackageManager.NameNotFoundException e) {
            ai = null;
        }
        final String applicationName = (String) (ai != null ? pm.getApplicationLabel(ai) : "(unknown)");

        createNotificationChannel();

        CharSequence text = sbn.getNotification().extras.getCharSequence("android.text", "Failed to parse");
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(iconRes)
                .setContentTitle(applicationName + " " + sbn.getNotification().extras.getString("android.title"))
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
