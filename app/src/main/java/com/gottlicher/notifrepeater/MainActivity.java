package com.gottlicher.notifrepeater;

import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.provider.Settings;
import android.support.annotation.NonNull;
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

import org.w3c.dom.Text;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

// Dialog code from: https://github.com/Chagall/notification-listener-service-example
public class MainActivity extends AppCompatActivity {

    private static final String ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners";
    private static final String ACTION_NOTIFICATION_LISTENER_SETTINGS = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";


    private AlertDialog enableNotificationListenerAlertDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(!isNotificationServiceEnabled()){
            buildNotificationServiceAlertDialog().show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        populatePastAppsList();
    }

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
        alertDialogBuilder.setTitle(R.string.notification_listener_service);
        alertDialogBuilder.setMessage(R.string.notification_listener_service_explanation);
        alertDialogBuilder.setPositiveButton(R.string.yes,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        startActivity(new Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS));
                    }
                });
        alertDialogBuilder.setNegativeButton(R.string.no,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // If you choose to not enable the notification listener
                        // the app. will not work as expected
                    }
                });
        return(alertDialogBuilder.create());
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
                R.drawable.ic_textsms_black_24dp
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

            pastAppsHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int currIndex = indexOfIcon(ai.imageRes);
                    int nextIndex = currIndex >= icons.length - 1 ? 0 : currIndex + 1;
                    int newRes = icons[nextIndex];
                    ai.imageRes = newRes;
                    finalHolder.icon.setImageResource(newRes);
                    AppHistoryHelper.updateIcon(MainActivity.this, ai.packageName, newRes);
                }
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
