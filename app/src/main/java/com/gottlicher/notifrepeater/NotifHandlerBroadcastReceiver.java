package com.gottlicher.notifrepeater;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationManagerCompat;

public class NotifHandlerBroadcastReceiver extends BroadcastReceiver {
    public static final String EXTRA_ORIG_PENDING_INTENT = "EXTRA_ORIG_PENDING_INTENT";
    public static final String EXTRA_NOTIF_TO_DISMISS = "EXTRA_NOTIF_TO_DISMISS";
    @Override
    public void onReceive(Context context, Intent intent) {
        int idToDismiss = intent.getIntExtra(EXTRA_NOTIF_TO_DISMISS, 0);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancel(idToDismiss);

        PendingIntent pi = intent.getParcelableExtra(EXTRA_ORIG_PENDING_INTENT);
        try {
            pi.send(context, 0, intent);
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
    }
}
