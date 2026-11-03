package com.nm.wakeswipe;

import android.app.Activity;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    static final String PREFS = "wakeswipe";
    static final String KEY_INTERVAL_MIN = "interval_min";
    static final String KEY_RUNNING = "running";
    static final int DEFAULT_INTERVAL_MIN = 60;

    private EditText intervalInput;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(48, 48, 48, 48);
        root.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView title = new TextView(this);
        title.setText("定时亮屏上滑");
        title.setTextSize(22);
        root.addView(title);

        TextView hint = new TextView(this);
        hint.setText("到点亮屏并上滑到桌面。默认每 60 分钟一次，可改间隔。");
        hint.setPadding(0, 24, 0, 24);
        root.addView(hint);

        TextView label = new TextView(this);
        label.setText("间隔（分钟）：");
        root.addView(label);

        intervalInput = new EditText(this);
        intervalInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        intervalInput.setText(String.valueOf(prefs.getInt(KEY_INTERVAL_MIN, DEFAULT_INTERVAL_MIN)));
        root.addView(intervalInput);

        Button startBtn = new Button(this);
        startBtn.setText("开始定时");
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { startSchedule(); }
        });
        root.addView(startBtn);

        Button stopBtn = new Button(this);
        stopBtn.setText("停止定时");
        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { stopSchedule(); }
        });
        root.addView(stopBtn);

        Button testBtn = new Button(this);
        testBtn.setText("立即测试一次（亮屏+上滑）");
        testBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, WakeActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
            }
        });
        root.addView(testBtn);

        Button accBtn = new Button(this);
        accBtn.setText("开启无障碍权限（必须，否则滑不动）");
        accBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            }
        });
        root.addView(accBtn);

        Button batteryBtn = new Button(this);
        batteryBtn.setText("忽略电池优化（必须，否则到点不执行）");
        batteryBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                try {
                    PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                    if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                        Intent i = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                        i.setData(Uri.parse("package:" + getPackageName()));
                        startActivity(i);
                    } else {
                        Toast.makeText(MainActivity.this, "已忽略电池优化", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
                }
            }
        });
        root.addView(batteryBtn);

        Button alarmBtn = new Button(this);
        alarmBtn.setText("允许精确闹钟（Android 12 需要）");
        alarmBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= 31) {
                    AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                    if (am != null && !am.canScheduleExactAlarms()) {
                        startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM));
                    } else {
                        Toast.makeText(MainActivity.this, "已允许精确闹钟", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        root.addView(alarmBtn);

        statusText = new TextView(this);
        statusText.setPadding(0, 32, 0, 0);
        root.addView(statusText);

        setContentView(root);
        refreshStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    private void refreshStatus() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        boolean running = prefs.getBoolean(KEY_RUNNING, false);
        int min = prefs.getInt(KEY_INTERVAL_MIN, DEFAULT_INTERVAL_MIN);
        String acc = SwipeService.isEnabled(this) ? "已开启" : "未开启（需手动开）";
        statusText.setText("状态：" + (running ? "运行中，每 " + min + " 分钟一次" : "已停止")
                + "\n无障碍权限：" + acc);
    }

    private void startSchedule() {
        int min;
        try {
            min = Integer.parseInt(intervalInput.getText().toString().trim());
        } catch (Exception e) {
            Toast.makeText(this, "间隔请填数字", Toast.LENGTH_SHORT).show();
            return;
        }
        if (min < 1) min = 1;
        if (min > 1440) min = 1440;
        intervalInput.setText(String.valueOf(min));
        if (!SwipeService.isEnabled(this)) {
            Toast.makeText(this, "请先开启无障碍权限，否则滑不动", Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        }
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        prefs.edit().putInt(KEY_INTERVAL_MIN, min).putBoolean(KEY_RUNNING, true).apply();
        WakeReceiver.schedule(this, min);
        refreshStatus();
        Toast.makeText(this, "已开始，每 " + min + " 分钟亮屏上滑一次", Toast.LENGTH_SHORT).show();
    }

    private void stopSchedule() {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(KEY_RUNNING, false).apply();
        WakeReceiver.cancel(this);
        refreshStatus();
        Toast.makeText(this, "已停止", Toast.LENGTH_SHORT).show();
    }
}
