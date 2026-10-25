package com.shiji.guard;

import android.accessibilityservice.AccessibilityService;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * 前台应用识别 + 到点拦截。
 * 达到每日限额后：全屏覆盖 + 返回桌面；用户再次打开时会被再次拦截。
 */
public class GuardService extends AccessibilityService {

    private static final String TAG = "ShijiGuard";
    public static volatile boolean connected;
    private static GuardService sInstance;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private WindowManager wm;
    private View overlay;
    private View keepAlive;
    private String overlayPkg;
    private String fgPkg;
    private long suppressUntil;
    private int beats;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            try {
                refreshForegroundFromWindow();
                if (fgPkg != null) check(fgPkg);
                if (++beats % 5 == 0) Log.d(TAG, "tick fg=" + fgPkg);
            } catch (Throwable ignored) {
            }
            handler.postDelayed(this, 2000);
        }
    };

    public static boolean isConnected() {
        return connected;
    }

    public static GuardService get() {
        return sInstance;
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        connected = true;
        sInstance = this;
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        addKeepAlive();
        try {
            Engine.acquire(this);
        } catch (Throwable ignored) {
        }
        if (App.get().prefs().trackerEnabled() && !TrackerService.isRunning()) {
            TrackerService.start(this);
        }
        handler.postDelayed(tick, 1200);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        int type = event.getEventType();
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && type != AccessibilityEvent.TYPE_WINDOWS_CHANGED) return;
        CharSequence p = event.getPackageName();
        if (p == null) return;
        String pkg = p.toString();
        if (pkg.equals(getPackageName())) return;
        if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            fgPkg = pkg;
            Engine.poke();
            MediaTracker.noteForeground(pkg);
            Log.d(TAG, "window " + pkg + " used=" + Usage.totalToday(pkg));
        }
        check(pkg);
    }

    /**
     * 一个 1 像素、不可触摸的常驻窗口。HyperOS 会冻结完全不可见的后台进程，
     * 保留一个可见窗口可以避免守护逻辑被冻结，且用户无感知。
     */
    private void addKeepAlive() {
        try {
            if (keepAlive != null || wm == null) return;
            View v = new View(this);
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                    1, 1,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT);
            lp.gravity = Gravity.TOP | Gravity.START;
            lp.x = 0;
            lp.y = 0;
            lp.alpha = 0.01f;
            wm.addView(v, lp);
            keepAlive = v;
            Log.d(TAG, "keepalive added");
        } catch (Throwable t) {
            Log.e(TAG, "keepalive failed", t);
        }
    }

    private void removeKeepAlive() {
        if (keepAlive != null) {
            try {
                wm.removeView(keepAlive);
            } catch (Throwable ignored) {
            }
            keepAlive = null;
        }
    }

    /** 事件流偶尔会断（重装、重连），主动读取当前窗口作为兜底。 */
    private void refreshForegroundFromWindow() {
        try {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root == null) return;
            CharSequence p = root.getPackageName();
            if (p != null) {
                String s = p.toString();
                if (!s.equals(getPackageName())) {
                    if (!s.equals(fgPkg)) Log.d(TAG, "active window " + s);
                    fgPkg = s;
                }
            }
            root.recycle();
        } catch (Throwable ignored) {
        }
    }

    private void check(String pkg) {
        if (pkg == null || pkg.equals(getPackageName())) return;
        if (!App.get().prefs().trackerEnabled()) return;
        if (System.currentTimeMillis() < suppressUntil) return;
        if (overlay != null) {
            if (overlay.isAttachedToWindow()) return;
            Log.w(TAG, "stale overlay cleared");
            overlay = null;
            overlayPkg = null;
        }
        Repo.Rule r = App.get().repo().rule(pkg);
        if (r == null || !r.enabled || r.limitMin <= 0) return;
        long used = Usage.totalToday(pkg);
        long limit = r.limitMin * TimeUtil.MINUTE;
        if (used >= limit) {
            Log.d(TAG, "block " + pkg + " used=" + used + " limit=" + limit);
            block(pkg, used, limit);
        }
    }

    private void block(String pkg, long used, long limit) {
        if (overlay != null) dismiss();
        Ui.applyTheme(this);
        overlayPkg = pkg;
        try {
            App.get().repo().logBlock(pkg);
        } catch (Throwable ignored) {
        }
        try {
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) am.killBackgroundProcesses(pkg);
        } catch (Throwable ignored) {
        }

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Ui.BG);
        root.setClickable(true);
        root.setFocusableInTouchMode(true);
        root.setOnKeyListener((v, keyCode, event) -> keyCode == KeyEvent.KEYCODE_BACK);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = dp(30);
        col.setPadding(pad, 0, pad, 0);
        FrameLayout.LayoutParams colLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        colLp.gravity = Gravity.CENTER;
        root.addView(col, colLp);

        col.addView(label("时迹 · 每日限额", 11, Ui.DIM, 0.24f));
        col.addView(space(22));
        col.addView(label(Apps.name(this, pkg), 14, Ui.ACCENT, 0.02f));
        col.addView(space(12));
        col.addView(label("今日额度已用完", 32, Ui.TEXT, 0.0f));
        col.addView(space(18));
        col.addView(label("已用 " + TimeUtil.fmtDuration(used) + " / 限额 " + r2(limit), 14, Ui.DIM, 0.0f));
        col.addView(space(6));
        col.addView(label("明天 00:00 重置", 12, Ui.FAINT, 0.0f));
        col.addView(space(44));

        TextView btn = new TextView(this);
        btn.setText("返回桌面");
        btn.setTextSize(14);
        btn.setTextColor(Ui.BG);
        btn.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(dp(44), dp(15), dp(44), dp(15));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Ui.ACCENT);
        bg.setCornerRadius(dp(100));
        btn.setBackground(bg);
        btn.setOnClickListener(v -> {
            dismiss();
            performGlobalAction(GLOBAL_ACTION_HOME);
        });
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        col.addView(btn, btnLp);

        col.addView(space(30));
        col.addView(label("手机被时迹暂时接管，避免你一时手滑", 11, Ui.FAINT, 0.0f));

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        try {
            wm.addView(root, lp);
            overlay = root;
            Log.d(TAG, "overlay added for " + pkg);
        } catch (Throwable t) {
            Log.e(TAG, "overlay add failed", t);
            overlay = null;
            overlayPkg = null;
            return;
        }
        handler.postDelayed(() -> {
            try {
                performGlobalAction(GLOBAL_ACTION_HOME);
            } catch (Throwable ignored) {
            }
        }, 120);
        final View shown = root;
        handler.postDelayed(() -> {
            if (overlay == shown) dismiss();
        }, 12000);
    }

    private TextView label(String text, float sp, int color, float spacing) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setIncludeFontPadding(false);
        t.setGravity(Gravity.CENTER);
        t.setLetterSpacing(spacing);
        t.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        return t;
    }

    private View space(int heightDp) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(heightDp)));
        return v;
    }

    private String r2(long limitMs) {
        long minutes = limitMs / TimeUtil.MINUTE;
        if (minutes % 60 == 0) return (minutes / 60) + " 小时";
        return TimeUtil.fmtDuration(limitMs);
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private void dismiss() {
        if (overlay != null) {
            try {
                wm.removeView(overlay);
            } catch (Throwable ignored) {
            }
            Log.d(TAG, "overlay removed");
        }
        overlay = null;
        overlayPkg = null;
        suppressUntil = System.currentTimeMillis() + 1500;
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onDestroy() {
        connected = false;
        sInstance = null;
        handler.removeCallbacks(tick);
        if (overlay != null) {
            try {
                wm.removeView(overlay);
            } catch (Throwable ignored) {
            }
            overlay = null;
        }
        removeKeepAlive();
        Engine.release();
        super.onDestroy();
    }
}
