package com.shiji.guard;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

/**
 * 单例统计引擎：同时被前台记录服务和无障碍服务持有，
 * 只要任一服务存活就持续记录，避免进程被杀后统计中断。
 */
public final class Engine {

    private static Engine sInstance;

    private final EventCollector collector;
    private final Handler handler;
    private final Runnable loop;
    private final Context appCtx;
    private int refs;

    private Engine(Context c) {
        appCtx = c.getApplicationContext();
        collector = new EventCollector(c.getApplicationContext(), App.get().prefs(), App.get().repo());
        handler = new Handler(Looper.getMainLooper());
        loop = new Runnable() {
            @Override
            public void run() {
                try {
                    collector.poll();
                } catch (Throwable ignored) {
                }
                handler.postDelayed(this, 10000);
            }
        };
        handler.post(loop);
    }

    public static synchronized void acquire(Context c) {
        if (sInstance == null) {
            sInstance = new Engine(c);
        }
        sInstance.refs++;
    }

    public static synchronized void release() {
        if (sInstance == null) return;
        sInstance.refs--;
        if (sInstance.refs <= 0) {
            sInstance.handler.removeCallbacks(sInstance.loop);
            sInstance.collector.closeNow();
            sInstance = null;
        }
    }

    public static boolean running() {
        return sInstance != null;
    }

    public static void poke() {
        Engine e = sInstance;
        if (e == null) return;
        e.handler.removeCallbacks(e.loop);
        e.handler.post(e.loop);
    }

    public static String currentPkg() {
        Engine e = sInstance;
        return e == null ? null : e.collector.currentPkg();
    }

    public static long currentSince() {
        Engine e = sInstance;
        return e == null ? 0L : e.collector.currentSince();
    }

    /** 今日某应用累计时长，含正在进行、尚未落库的当前会话。 */
    public static long usageToday(String pkg) {
        if (pkg == null) return 0L;
        long now = System.currentTimeMillis();
        long ms = App.get().repo().usageOnDay(pkg, TimeUtil.dayKey(now));
        String cur = currentPkg();
        long since = currentSince();
        if (pkg.equals(cur) && since > 0) {
            ms += Math.max(0, now - Math.max(since, TimeUtil.dayStart(now)));
        }
        return ms;
    }

    /** 今日累计时长，排除桌面、系统 UI 与时迹自身。 */
    public static long totalToday() {
        long now = System.currentTimeMillis();
        long start = TimeUtil.dayStart(now);
        Repo repo = App.get().repo();
        Context ctx = App.get();
        long ms = repo.totalBetweenExcluding(start, now + 1, Sys.excludedArray(ctx));
        String cur = currentPkg();
        long since = currentSince();
        if (cur != null && since > 0 && !Sys.excluded(ctx, cur)) {
            ms += Math.max(0, now - Math.max(since, start));
        }
        return Math.max(0, ms);
    }

    public static long totalOnDay(long ts) {
        long start = TimeUtil.dayStart(ts);
        return App.get().repo().totalBetweenExcluding(start, TimeUtil.nextMidnight(start),
                Sys.excludedArray(App.get()));
    }
}
