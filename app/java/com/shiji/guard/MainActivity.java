package com.shiji.guard;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    private FrameLayout content;
    private final TextView[] tabLabels = new TextView[4];
    private final View[] tabDots = new View[4];
    private Screen screen;
    private int tab = -1;

    private final Handler ticker = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (screen != null) screen.refresh();
            ticker.postDelayed(this, 5000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Ui.applyTheme(this);
        setTheme(Ui.isLight() ? R.style.AppTheme_Light : R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(buildRoot());
        applySystemBars();

        if (Build.VERSION.SDK_INT >= 33 && !Permissions.postNotificationsGranted(this)) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 11);
        }
        if (App.get().prefs().trackerEnabled()) TrackerService.start(this);
        setTab(savedInstanceState != null ? savedInstanceState.getInt("tab", 0) : 0);
    }

    private void applySystemBars() {
        try {
            getWindow().setStatusBarColor(Ui.BG);
            getWindow().setNavigationBarColor(Ui.BG);
            int flags = getWindow().getDecorView().getSystemUiVisibility();
            int lightFlags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            if (Ui.isLight()) flags |= lightFlags;
            else flags &= ~lightFlags;
            getWindow().getDecorView().setSystemUiVisibility(flags);
        } catch (Throwable ignored) {
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt("tab", tab);
        super.onSaveInstanceState(outState);
    }

    private View buildRoot() {
        LinearLayout root = Ui.col(this);
        root.setBackgroundColor(Ui.BG);
        content = new FrameLayout(this);
        content.setBackgroundColor(Ui.BG);
        root.addView(content, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        root.addView(Ui.hline(this));

        LinearLayout nav = Ui.row(this);
        nav.setPadding(0, Ui.dp(this, 9), 0, Ui.dp(this, 11));
        String[] labels = {"今日", "回顾", "限制", "设置"};
        for (int i = 0; i < labels.length; i++) {
            LinearLayout item = Ui.col(this);
            item.setGravity(Gravity.CENTER);
            TextView label = Ui.tv(this, labels[i], 11.5f, Ui.DIM);
            label.setLetterSpacing(0.08f);
            label.setGravity(Gravity.CENTER);
            View dot = Ui.dot(this, Ui.ACCENT, 4);
            dot.setVisibility(View.INVISIBLE);
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(Ui.dp(this, 4), Ui.dp(this, 4));
            dlp.topMargin = Ui.dp(this, 7);
            item.addView(label);
            item.addView(dot, dlp);
            final int idx = i;
            Ui.click(item, () -> setTab(idx));
            nav.addView(item, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            tabLabels[i] = label;
            tabDots[i] = dot;
        }
        root.addView(nav);
        return root;
    }

    public void setTab(int idx) {
        tab = idx;
        switch (idx) {
            case 0:
                screen = new TodayScreen(this);
                break;
            case 1:
                screen = new ReviewScreen(this);
                break;
            case 2:
                screen = new RulesScreen(this);
                break;
            default:
                screen = new SettingsScreen(this);
                break;
        }
        content.removeAllViews();
        content.addView(screen.view(), new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        for (int i = 0; i < 4; i++) {
            tabLabels[i].setTextColor(i == idx ? Ui.TEXT : Ui.DIM);
            tabDots[i].setVisibility(i == idx ? View.VISIBLE : View.INVISIBLE);
        }
        screen.refresh();
    }

    public void refreshNow() {
        if (screen != null) {
            screen.refresh();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (screen != null) screen.refresh();
        if (App.get().prefs().trackerEnabled() && !TrackerService.isRunning()) {
            TrackerService.start(this);
        }
        ticker.removeCallbacks(tick);
        ticker.postDelayed(tick, 5000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        ticker.removeCallbacks(tick);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ticker.removeCallbacks(tick);
    }
}
