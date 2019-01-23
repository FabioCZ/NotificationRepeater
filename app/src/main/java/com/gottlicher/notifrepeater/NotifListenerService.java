package com.gottlicher.notifrepeater;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.Parcel;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.RemoteInput;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            requestRebind(ComponentName.createRelative(this.getApplicationContext().getPackageName(), "NotifListenerService"));
        }
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

        postNotification(this,
                appLauncherName + ": " + title,
                text.toString(),
                iconRes,
                sbn.getNotification().contentIntent,
                sbn.getId(),
                sbn.getNotification().visibility,
                extractActions(sbn));
        localBroadcastManager.sendBroadcast(new Intent(RELOAD_MESSAGE));
    }

    List<NotificationCompat.Action> extractActions(StatusBarNotification sbn)
    {
        ArrayList<NotificationCompat.Action> actions = new ArrayList<>();
        if (!MainActivity.isNotificationsActionsEnabled(getApplicationContext())) {
            return  actions;
        }

        int ct = NotificationCompat.getActionCount(sbn.getNotification());
        for (int i = 0; i < ct; i++) {
            try {
                NotificationCompat.Action action = NotificationCompat.getAction(sbn.getNotification(), i);
                if (action == null) {
                    continue;
                }
                PendingIntent origPI = action.getActionIntent();

                Intent newIntent = new Intent(getApplicationContext(), NotifHandlerBroadcastReceiver.class);
                newIntent.putExtra(NotifHandlerBroadcastReceiver.EXTRA_ORIG_PENDING_INTENT, origPI);
                newIntent.putExtra(NotifHandlerBroadcastReceiver.EXTRA_NOTIF_TO_DISMISS, sbn.getId());

                PendingIntent newPI = PendingIntent.getBroadcast(getApplicationContext(),  sbn.getId(), newIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                NotificationCompat.Action.Builder builder = new NotificationCompat.Action.Builder(action.getIcon(), action.getTitle(), newPI);

                RemoteInput[] remoteInputs = action.getRemoteInputs();
                if (remoteInputs != null) {
                    for (RemoteInput ri : remoteInputs) {
                        builder.addRemoteInput(ri);
                    }
                }
                actions.add(builder.build());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return actions;
    }

    public static void postNotification(Context context, String title, String text, int icon, PendingIntent clickIntent, int id, int visibility, List<NotificationCompat.Action> actions)
    {
        NotificationCompat.Builder mBuilder;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            mBuilder = new NotificationCompat.Builder(context, CHANNEL_ID);
        } else {
            mBuilder = new NotificationCompat.Builder(context);
        }

        mBuilder.setSmallIcon(icon)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(clickIntent)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(visibility)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text));

        for (NotificationCompat.Action a : actions) {
            mBuilder.addAction(a);
        }

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(id, mBuilder.build());

    }
}
