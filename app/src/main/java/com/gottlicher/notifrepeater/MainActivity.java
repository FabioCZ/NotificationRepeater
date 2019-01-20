package com.gottlicher.notifrepeater;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

// Dialog code from: https://github.com/Chagall/notification-listener-service-example
public class MainActivity extends AppCompatActivity {

    private static final String ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners";
    private static final String ACTION_NOTIFICATION_LISTENER_SETTINGS = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";
    private static final int TEST_NOTIF_CODE = 1;
    private static final String TEST_NOTIF_EXTRA = "TEST_NOTIF";

    BroadcastReceiver reloadBroadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        reloadBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                populatePastAppsList();
            }
        };

        createNotificationChannel();

        if(!isNotificationServiceEnabled()){
            buildNotificationServiceAlertDialog().show();
        }

        findViewById(R.id.app_settings_btn).setOnClickListener((View v) -> openAppSettings());
        findViewById(R.id.test_notif_btn).setOnClickListener((View v) -> showTestNotification());
    }

    @Override
    protected void onResume() {
        super.onResume();
        populatePastAppsList();
    }

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(this).registerReceiver((reloadBroadcastReceiver),
                new IntentFilter(NotifListenerService.RELOAD_MESSAGE));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.hasExtra(TEST_NOTIF_EXTRA)){
            Toast.makeText(this, R.string.test_notif_received,Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(reloadBroadcastReceiver);
        super.onStop();
    }

    //Inefficient, I know, but it doesn't need to be
    private void populatePastAppsList(){
        RecyclerView list = findViewById(R.id.apps_history_list);
        list.setHasFixedSize(true);
        RecyclerView.LayoutManager lm = new LinearLayoutManager(this);
        list.setLayoutManager(lm);
        RecyclerView.Adapter ad = new PastAppsAdapter(AppHistoryHelper.getOccurences (this));
        list.setAdapter(ad);
    }

    private boolean isNotificationServiceEnabled(){
        String pkgName = getPackageName();
        final String flat = Settings.Secure.getString(getContentResolver(),
                ENABLED_NOTIFICATION_LISTENERS);
        if (!TextUtils.isEmpty(flat)) {
            final String[] names = flat.split(":");
            for (int i = 0; i < names.length; i++) {
                final ComponentName cn = ComponentName.unflattenFromString(names[i]);
                if (cn != null) {
                    if (TextUtils.equals(pkgName, cn.getPackageName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private AlertDialog buildNotificationServiceAlertDialog(){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle(R.string.notification_listener_service)
                        .setMessage(R.string.notification_listener_service_explanation)
                        .setPositiveButton(R.string.yes, (dialog, id) -> startActivity(new Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS)))
                        .setNegativeButton(R.string.no,  (dialog, id) -> Toast.makeText(this, R.string.the_app_will_not_work, Toast.LENGTH_LONG).show());
        return alertDialogBuilder.create();
    }

    private void createNotificationChannel() {
        CharSequence name = getString(R.string.notif_channel_name);
        String description = getString(R.string.notif_channel_name);
        int importance = NotificationManager.IMPORTANCE_HIGH;
        NotificationChannel channel = new NotificationChannel(NotifListenerService.CHANNEL_ID, name, importance);
        channel.setDescription(description);
        channel.enableLights(true);
        channel.enableVibration(true);

        // Register the channel with the system; you can't change the importance
        // or other notification behaviors after this
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }

    private void openAppSettings ()
    {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }

    private void showTestNotification ()
    {
        Intent intent = new Intent(this,MainActivity.class);
        intent.putExtra(TEST_NOTIF_EXTRA, TEST_NOTIF_EXTRA);
        PendingIntent pi =  PendingIntent.getActivity(this, TEST_NOTIF_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotifListenerService.postNotification(this, "Repeated test notification", "Did you ever hear the tragedy of Darth Plagueis The Wise?", R.drawable.ic_notifications_black_24dp, pi, 123);
    }

    public static class PastAppsHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView nameTxt;
        public TextView ctTxt;
        public ImageView icon;

        public PastAppsHolder(LinearLayout v) {
            super(v);
            nameTxt = v.findViewById(R.id.app_name);
            ctTxt = v.findViewById(R.id.app_count);
            icon = v.findViewById(R.id.app_icon);
        }
    }
    public class PastAppsAdapter extends RecyclerView.Adapter<PastAppsHolder> {
        private ArrayList<AppHistoryHelper.AppInfo> mDataset;

        private int[] icons = {
                R.drawable.ic_note_black_24dp,
                R.drawable.ic_notifications_black_24dp,
                R.drawable.ic_perm_phone_msg_black_24dp,
                R.drawable.ic_phone_black_24dp,
                R.drawable.ic_question_answer_black_24dp,
                R.drawable.ic_textsms_black_24dp,
                R.drawable.ic_account_circle_black_24dp,
                R.drawable.ic_assignment_late_black_24dp,
                R.drawable.ic_chat_black_24dp,
                R.drawable.ic_email_black_24dp
        };

        // Provide a suitable constructor (depends on the kind of dataset)
        public PastAppsAdapter(ArrayList<AppHistoryHelper.AppInfo> dataSet) {
            mDataset = dataSet;
        }

        // Create new views (invoked by the layout manager)
        @Override
        public PastAppsHolder onCreateViewHolder(ViewGroup parent,
                                                         int viewType) {
            // create a new view
            LinearLayout v = (LinearLayout) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.past_apps_view_holder, parent, false);
            PastAppsHolder vh = new PastAppsHolder(v);

            return vh;
        }

        @Override
        public void onBindViewHolder(@NonNull PastAppsHolder pastAppsHolder, int i) {
            final AppHistoryHelper.AppInfo ai = mDataset.get(i);
            final PastAppsHolder finalHolder = pastAppsHolder;
            pastAppsHolder.icon.setImageResource(ai.imageRes);
            pastAppsHolder.nameTxt.setText(ai.getAppLauncherName(MainActivity.this));
            pastAppsHolder.ctTxt.setText(ai.ct + " Errors");

            pastAppsHolder.itemView.setOnClickListener(v -> {
                int currIndex = indexOfIcon(ai.imageRes);
                int nextIndex = currIndex >= icons.length - 1 ? 0 : currIndex + 1;
                int newRes = icons[nextIndex];
                ai.imageRes = newRes;
                finalHolder.icon.setImageResource(newRes);
                AppHistoryHelper.updateIcon(MainActivity.this, ai.packageName, newRes);
            });
        }

        int indexOfIcon (int resId){
            for (int i = 0; i < icons.length; i++) {
                if (icons[i] == resId) {
                    return i;
                }
            }
            return 0;
        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return mDataset.size();
        }
    }
}
