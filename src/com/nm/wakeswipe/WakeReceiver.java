package com.nm.wakeswipe;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

public class WakeReceiver extends BroadcastReceiver {
    static final String ACTION_WAKE = "com.nm.wakeswipe.ACTION_WAKE";

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE);
        if (!prefs.getBoolean(MainActivity.KEY_RUNNING, false)) return;
        int min = prefs.getInt(MainActivity.KEY_INTERVAL_MIN, MainActivity.DEFAULT_INTERVAL_MIN);
        // 先约下一次，再亮屏滑动，保证即使本次失败下次还会来。
        schedule(context, min);
        Intent i = new Intent(context, WakeActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(i);
    }

    static void schedule(Context context, int minutes) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        Intent intent = new Intent(context, WakeReceiver.class);
        intent.setAction(ACTION_WAKE);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, flags);
        long trigger = System.currentTimeMillis() + minutes * 60L * 1000L;
        try {
            if (Build.VERSION.SDK_INT >= 31 && am.canScheduleExactAlarms()) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi);
            } else if (Build.VERSION.SDK_INT >= 23) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi);
            } else {
                am.set(AlarmManager.RTC_WAKEUP, trigger, pi);
            }
        } catch (SecurityException e) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi);
        }
    }

    static void cancel(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        Intent intent = new Intent(context, WakeReceiver.class);
        intent.setAction(ACTION_WAKE);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, flags);
        am.cancel(pi);
    }
}
