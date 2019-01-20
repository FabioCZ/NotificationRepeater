package com.gottlicher.notifrepeater;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class NotifListenerService extends NotificationListenerService {

    final static String RELOAD_MESSAGE = "com.gottlicher.notifrepeater.reloadData";
    final static String CHANNEL_ID = "Forwaded";
    final String TAG = "NOTIF";

    private LocalBroadcastManager localBroadcastManager;

    @Override
    public IBinder onBind(Intent intent) {
        Log.d (TAG, "Service bound");
        return super.onBind(intent);
    }

    @Override
    public void onListenerConnected() {
        Log.d (TAG, "Connected");
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
    }

    @Override
    public void onListenerDisconnected() {
        Log.d(TAG, "Disconnected");
        requestRebind(ComponentName.createRelative(this.getApplicationContext().getPackageName(), "NotifListenerService"));
        localBroadcastManager = null;
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
        postRepeatNotification(sbn);
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

    void postRepeatNotification(StatusBarNotification sbn) {

        int iconRes = AppHistoryHelper.getIconFor(getApplicationContext(), sbn.getPackageName());
        String appLauncherName = AppHistoryHelper.getAppNameFromPackage(getApplicationContext(), sbn.getPackageName());

        CharSequence title = sbn.getNotification().extras.getCharSequence("android.title", "Failed to parse");
        CharSequence text = sbn.getNotification().extras.getCharSequence("android.text", "Failed to parse");

        postNotification(this, appLauncherName + ": " + title, text.toString(), iconRes, sbn.getNotification().contentIntent, sbn.getId());
        localBroadcastManager.sendBroadcast(new Intent(RELOAD_MESSAGE));
    }

    public static void postNotification(Context context, String title, String text, int icon, PendingIntent clickIntent, int id)
    {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(icon)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(clickIntent)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(id, mBuilder.build());

    }
}
