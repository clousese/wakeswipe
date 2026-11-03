package com.nm.wakeswipe;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE);
        if (!prefs.getBoolean(MainActivity.KEY_RUNNING, false)) return;
        WakeReceiver.schedule(context, prefs.getInt(MainActivity.KEY_INTERVAL_MIN, MainActivity.DEFAULT_INTERVAL_MIN));
    }
}
