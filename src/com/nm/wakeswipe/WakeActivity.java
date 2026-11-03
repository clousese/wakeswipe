package com.nm.wakeswipe;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.view.WindowManager;
import android.widget.Toast;

public class WakeActivity extends Activity {
    private PowerManager.WakeLock wakeLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        try {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                wakeLock = pm.newWakeLock(
                        PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                        "wakeswipe:wake");
                wakeLock.acquire(10000);
            }
        } catch (Exception ignored) {
        }
        // 等屏幕真正亮起再滑，避免屏还没亮手势丢了。
        new Handler().postDelayed(new Runnable() {
            @Override public void run() {
                boolean ok = SwipeService.swipeUpToDesktop();
                if (!ok) {
                    Toast.makeText(WakeActivity.this, "未开启无障碍权限，无法滑动", Toast.LENGTH_LONG).show();
                }
                finish();
            }
        }, 1200);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        } catch (Exception ignored) {
        }
    }
}
