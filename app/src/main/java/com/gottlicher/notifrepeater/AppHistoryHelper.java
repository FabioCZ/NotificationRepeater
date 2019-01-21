package com.gottlicher.notifrepeater;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class AppHistoryHelper {
    private final static String APPS_PREF_NAME = "APPS_PREF_NAME";

    public static class AppInfo {
        public String packageName;
        public int imageRes;
        public int ct;

        AppInfo(String packageName, int imageRes, int ct) {
            this.packageName = packageName;
            this.imageRes = imageRes;
            this.ct = ct;
        }

        public String getAppLauncherName (Context context) {
            return getAppNameFromPackage(context, packageName);
        }
    }


    private static Gson gson = new Gson();
    public static void addNotificationOccurence (Context context, String appName)
    {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        String appsJson = sharedPrefs.getString(APPS_PREF_NAME, "");

        Type collType = new TypeToken<ArrayList<AppInfo>>(){}.getType();
        ArrayList<AppInfo> appList = gson.fromJson(appsJson, collType);

        if (appList == null) {
            appList = new ArrayList<>();
        }

        boolean found = false;
        for(AppInfo ai : appList) {
            if(ai.packageName.equals(appName)) {
                ai.ct++;
                found = true;
            }
        }

        if(!found) {
            appList.add(new AppInfo(appName, R.drawable.ic_notifications_black_24dp, 1));
        }

        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(APPS_PREF_NAME, gson.toJson(appList));
        editor.apply();
    }

    public static ArrayList<AppInfo> getOccurrences(Context context)
    {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        String appsJson = sharedPrefs.getString(APPS_PREF_NAME, "");

        Type collType = new TypeToken<ArrayList<AppInfo>>(){}.getType();
        ArrayList<AppInfo> appList = gson.fromJson(appsJson, collType);
        if (appList == null) {
            appList = new ArrayList<>();
        }
        return appList;
    }

    public static int getIconFor (Context context, String appName)
    {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        String appsJson = sharedPrefs.getString(APPS_PREF_NAME, "");

        Type collType = new TypeToken<ArrayList<AppInfo>>(){}.getType();
        ArrayList<AppInfo> appList = gson.fromJson(appsJson, collType);

        for(AppInfo ai : appList) {
            if(ai.packageName.equals(appName)) {
                return  ai.imageRes;
            }
        }
        return  R.drawable.ic_notifications_black_24dp;
    }

    public static void updateIcon (Context context, String packageName, int newIcon)
    {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        String appsJson = sharedPrefs.getString(APPS_PREF_NAME, "");

        Type collType = new TypeToken<ArrayList<AppInfo>>(){}.getType();
        ArrayList<AppInfo> appList = gson.fromJson(appsJson, collType);

        if (appList == null) {
            appList = new ArrayList<>();
        }

        boolean found = false;
        for(AppInfo ai : appList) {
            if(ai.packageName.equals(packageName)) {
                ai.imageRes = newIcon;
            }
        }

        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(APPS_PREF_NAME, gson.toJson(appList));
        editor.apply();
    }


    public static String getAppNameFromPackage (Context context, String pkg)
    {
        final PackageManager pm = context.getPackageManager();
        ApplicationInfo ai;
        try {
            ai = pm.getApplicationInfo(pkg, 0);
        } catch (final PackageManager.NameNotFoundException e) {
            ai = null;
        }
        return (String) (ai != null ? pm.getApplicationLabel(ai) : "(unknown)");
    }
}
