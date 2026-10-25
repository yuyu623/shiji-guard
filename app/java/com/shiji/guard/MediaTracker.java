package com.shiji.guard;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 后台音频统计：只在应用处于后台/息屏、且媒体会话仍在播放时累计。
 * 需要通知使用权（MediaSessionManager 的限制）。
 */
public final class MediaTracker {

    private static final String TAG = "ShijiMedia";
    private static MediaTracker sInstance;

    private final Context ctx;
    private final Prefs prefs;
    private final Repo repo;
    private final MediaSessionManager manager;
    private final ComponentName listener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Map<String, Long> active = new HashMap<>();
    private final Map<String, Long> lastPause = new HashMap<>();
    private final PowerManager.WakeLock wakeLock;

    private BroadcastReceiver screenReceiver;
    private String foregroundPkg;
    private long foregroundAt;
    private boolean started;

    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                poll();
            } catch (Throwable t) {
                Log.w(TAG, "poll failed", t);
            }
            if (started) handler.postDelayed(this, 4000);
        }
    };

    private MediaTracker(Context c) {
        ctx = c.getApplicationContext();
        prefs = App.get().prefs();
        repo = App.get().repo();
        manager = (MediaSessionManager) ctx.getSystemService(Context.MEDIA_SESSION_SERVICE);
        listener = new ComponentName(ctx, MediaListenerService.class);
        PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
        wakeLock = pm == null ? null : pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "shiji:media");
        if (wakeLock != null) wakeLock.setReferenceCounted(false);

        recoverOpenSessions();
        registerScreenReceiver();
        started = true;
        handler.post(pollRunnable);
        Log.d(TAG, "started, open=" + prefs.openMedia().size());
    }

    public static synchronized void start(Context c) {
        if (sInstance != null) return;
        if (!Permissions.notificationAccess(c)) {
            Log.d(TAG, "no notification access");
            return;
        }
        try {
            sInstance = new MediaTracker(c);
        } catch (Throwable t) {
            sInstance = null;
            Log.w(TAG, "start failed", t);
        }
    }

    public static synchronized void stop() {
        MediaTracker t = sInstance;
        sInstance = null;
        if (t != null) t.stopInternal();
    }

    public static boolean isRunning() {
        return sInstance != null;
    }

    public static void noteForeground(String pkg) {
        MediaTracker t = sInstance;
        if (t == null || pkg == null) return;
        t.foregroundPkg = pkg;
        t.foregroundAt = System.currentTimeMillis();
    }

    public static long usageToday(String pkg) {
        MediaTracker t = sInstance;
        long now = System.currentTimeMillis();
        String day = TimeUtil.dayKey(now);
        long ms = App.get().repo().mediaUsageOnDay(pkg, day);
        if (t != null) {
            Long start = t.active.get(pkg);
            if (start != null) {
                ms += Math.max(0, now - Math.max(start, TimeUtil.dayStart(now)));
            }
        }
        return ms;
    }

    public static long totalToday() {
        MediaTracker t = sInstance;
        long now = System.currentTimeMillis();
        long dayStart = TimeUtil.dayStart(now);
        long ms = App.get().repo().mediaDayTotal(TimeUtil.dayKey(now));
        if (t != null) {
            for (Long start : t.active.values()) {
                ms += Math.max(0, now - Math.max(start, dayStart));
            }
        }
        return Math.max(0, ms);
    }

    public static long usageBetween(String pkg, long from, long to) {
        MediaTracker t = sInstance;
        long ms = App.get().repo().mediaUsageBetween(pkg, from, to);
        if (t != null) {
            Long start = t.active.get(pkg);
            if (start != null) {
                long s = Math.max(start, from);
                long e = Math.min(to, System.currentTimeMillis());
                if (e > s) ms += e - s;
            }
        }
        return ms;
    }

    public static long totalBetween(long from, long to) {
        MediaTracker t = sInstance;
        long ms = App.get().repo().mediaTotalBetween(from, to);
        if (t != null) {
            long now = System.currentTimeMillis();
            for (Long start : t.active.values()) {
                long s = Math.max(start, from);
                long e = Math.min(to, now);
                if (e > s) ms += e - s;
            }
        }
        return ms;
    }

    public static Set<String> activePackages() {
        MediaTracker t = sInstance;
        return t == null ? new HashSet<>() : new HashSet<>(t.active.keySet());
    }

    public static long activeDelta() {
        MediaTracker t = sInstance;
        if (t == null) return 0;
        long now = System.currentTimeMillis();
        long ms = 0;
        for (Long start : t.active.values()) ms += Math.max(0, now - start);
        return ms;
    }

    private void poll() {
        long now = System.currentTimeMillis();
        if (!started) return;
        if (!prefs.trackerEnabled()) {
            closeAll(now);
            updateWakeLock(false);
            return;
        }
        if (manager == null) return;

        List<MediaController> sessions;
        try {
            sessions = manager.getActiveSessions(listener);
        } catch (Throwable t) {
            Log.w(TAG, "getActiveSessions failed", t);
            return;
        }
        if (sessions == null) sessions = new ArrayList<>();

        boolean screenOn = isInteractive();
        String fg = foreground();

        Map<String, List<MediaController>> playing = new HashMap<>();
        for (MediaController c : sessions) {
            if (c == null) continue;
            PlaybackState ps;
            try {
                ps = c.getPlaybackState();
            } catch (Throwable t) {
                continue;
            }
            if (ps == null || ps.getState() != PlaybackState.STATE_PLAYING) continue;
            String pkg;
            try {
                pkg = c.getPackageName();
            } catch (Throwable t) {
                continue;
            }
            if (pkg == null || pkg.equals(ctx.getPackageName())) continue;
            List<MediaController> list = playing.get(pkg);
            if (list == null) {
                list = new ArrayList<>();
                playing.put(pkg, list);
            }
            list.add(c);
        }

        for (String pkg : new ArrayList<>(active.keySet())) {
            if (!playing.containsKey(pkg)) close(pkg, now);
        }

        for (Map.Entry<String, List<MediaController>> e : playing.entrySet()) {
            String pkg = e.getKey();
            if (limitExceeded(pkg)) {
                close(pkg, now);
                pause(pkg, e.getValue());
                continue;
            }
            boolean background = !screenOn || !pkg.equals(fg);
            if (background) {
                if (!active.containsKey(pkg)) {
                    active.put(pkg, now);
                    prefs.setOpenMedia(pkg, now);
                    Log.d(TAG, "bg start " + pkg);
                }
            } else if (active.containsKey(pkg)) {
                close(pkg, now);
            }
        }

        updateWakeLock(!active.isEmpty());
    }

    private boolean limitExceeded(String pkg) {
        Repo.Rule r = repo.rule(pkg);
        if (r == null || !r.enabled || r.limitMin <= 0) return false;
        return Usage.totalToday(pkg) >= r.limitMin * TimeUtil.MINUTE;
    }

    private void pause(String pkg, List<MediaController> controllers) {
        long now = System.currentTimeMillis();
        Long last = lastPause.get(pkg);
        if (last != null && now - last < 3000) return;
        lastPause.put(pkg, now);
        for (MediaController c : controllers) {
            try {
                MediaController.TransportControls tc = c.getTransportControls();
                if (tc != null) {
                    tc.pause();
                    Log.d(TAG, "paused " + pkg);
                }
            } catch (Throwable t) {
                Log.w(TAG, "pause failed " + pkg, t);
            }
        }
    }

    private boolean isInteractive() {
        try {
            PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
            return pm != null && pm.isInteractive();
        } catch (Throwable t) {
            return true;
        }
    }

    private String foreground() {
        long now = System.currentTimeMillis();
        if (foregroundPkg != null && now - foregroundAt < 15000) return foregroundPkg;
        return Engine.currentPkg();
    }

    private void recoverOpenSessions() {
        long now = System.currentTimeMillis();
        Map<String, Long> open = prefs.openMedia();
        for (Map.Entry<String, Long> e : open.entrySet()) {
            String pkg = e.getKey();
            long start = e.getValue();
            if (pkg != null && start > 0 && start < now) {
                try {
                    repo.addMediaSession(pkg, start, Math.min(now, start + 12 * TimeUtil.HOUR));
                } catch (Throwable ignored) {
                }
            }
            prefs.clearOpenMedia(pkg);
        }
    }

    private void close(String pkg, long end) {
        Long start = active.remove(pkg);
        if (start != null && end > start) {
            try {
                repo.addMediaSession(pkg, start, end);
                Log.d(TAG, "bg end " + pkg + " " + (end - start) + "ms");
            } catch (Throwable ignored) {
            }
        }
        prefs.clearOpenMedia(pkg);
    }

    private void closeAll(long end) {
        for (String pkg : new ArrayList<>(active.keySet())) close(pkg, end);
    }

    private void updateWakeLock(boolean on) {
        if (wakeLock == null) return;
        try {
            if (on && !wakeLock.isHeld()) {
                wakeLock.acquire();
                Log.d(TAG, "wake lock held");
            } else if (!on && wakeLock.isHeld()) {
                wakeLock.release();
                Log.d(TAG, "wake lock released");
            }
        } catch (Throwable t) {
            Log.w(TAG, "wake lock failed", t);
        }
    }

    private void registerScreenReceiver() {
        try {
            screenReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context c, Intent intent) {
                    handler.removeCallbacks(pollRunnable);
                    handler.post(pollRunnable);
                }
            };
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_SCREEN_ON);
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            if (Build.VERSION.SDK_INT >= 33) {
                ctx.registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                ctx.registerReceiver(screenReceiver, filter);
            }
        } catch (Throwable t) {
            Log.w(TAG, "register screen failed", t);
            screenReceiver = null;
        }
    }

    private void stopInternal() {
        started = false;
        handler.removeCallbacks(pollRunnable);
        closeAll(System.currentTimeMillis());
        updateWakeLock(false);
        if (screenReceiver != null) {
            try {
                ctx.unregisterReceiver(screenReceiver);
            } catch (Throwable ignored) {
            }
            screenReceiver = null;
        }
        Log.d(TAG, "stopped");
    }
}
