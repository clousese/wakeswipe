package com.nm.wakeswipe;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.graphics.Path;
import android.graphics.Point;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Display;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;

public class SwipeService extends AccessibilityService {
    private static SwipeService instance;

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (instance == this) instance = null;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
    }

    static boolean isEnabled(Context context) {
        String enabled = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (TextUtils.isEmpty(enabled)) return false;
        String me = context.getPackageName() + "/" + SwipeService.class.getName();
        for (String s : enabled.split(":")) {
            if (me.equals(s)) return true;
        }
        return false;
    }

    /** 从屏幕下方 85% 处上滑到 25% 处，即上滑解锁到桌面。 */
    static boolean swipeUpToDesktop() {
        SwipeService svc = instance;
        if (svc == null) return false;
        try {
            WindowManager wm = (WindowManager) svc.getSystemService(Context.WINDOW_SERVICE);
            if (wm == null) return false;
            Point size = new Point();
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Rect bounds = wm.getCurrentWindowMetrics().getBounds();
                size.x = bounds.width();
                size.y = bounds.height();
            } else {
                Display d = wm.getDefaultDisplay();
                d.getRealSize(size);
            }
            float x = size.x / 2f;
            Path path = new Path();
            path.moveTo(x, size.y * 0.85f);
            path.lineTo(x, size.y * 0.25f);
            GestureDescription.StrokeDescription stroke =
                    new GestureDescription.StrokeDescription(path, 0, 350);
            GestureDescription gesture = new GestureDescription.Builder().addStroke(stroke).build();
            return svc.dispatchGesture(gesture, null, null);
        } catch (Exception e) {
            return false;
        }
    }
}
