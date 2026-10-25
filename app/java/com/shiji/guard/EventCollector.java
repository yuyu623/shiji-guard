package com.shiji.guard;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;

import java.util.ArrayList;
import java.util.List;

/**
 * 从系统 UsageEvents 事件流还原「应用前台区间」。
 * 只依赖 RESUMED 事件与亮灭屏事件，避免同一应用内 Activity 切换造成碎片。
 */
public class EventCollector {

    private final UsageStatsManager usm;
    private final Prefs prefs;
    private final Repo repo;
    private final List<Repo.Sess> pending = new ArrayList<>();

    private long lastTs;
    private String curPkg;
    private long curStart;

    public EventCollector(Context c, Prefs p, Repo r) {
        usm = (UsageStatsManager) c.getSystemService(Context.USAGE_STATS_SERVICE);
        prefs = p;
        repo = r;
        lastTs = p.lastEventTs();
        curPkg = p.openPkg();
        curStart = p.openStart();
        if (curPkg != null && curStart > 0) {
            long now = System.currentTimeMillis();
            pending.add(new Repo.Sess(curPkg, curStart, Math.min(now, curStart + 12 * TimeUtil.HOUR)));
            prefs.clearOpen();
        }
        curPkg = null;
        curStart = 0;
        long now = System.currentTimeMillis();
        if (lastTs <= 0 || lastTs > now) lastTs = now - 7 * TimeUtil.DAY;
        flush();
    }

    public void poll() {
        if (usm == null) return;
        long now = System.currentTimeMillis();
        long cursor = lastTs + 1;
        if (cursor <= now) {
            long chunk = 6 * TimeUtil.HOUR;
            int guard = 0;
            while (cursor <= now && guard++ < 48) {
                long end = Math.min(now, cursor + chunk);
                long maxTs = queryRange(cursor, end);
                if (maxTs > lastTs) lastTs = maxTs;
                if (end >= now) break;
                cursor = end + 1;
            }
            // 不要把游标留在很久以前：系统事件通常即时到达，保留 5 秒容差即可。
            long floor = now - 5000;
            if (lastTs < floor) lastTs = floor;
        }
        prefs.setLastEventTs(lastTs);
        prefs.setOpen(curPkg, curStart);
        flush();
    }

    private long queryRange(long from, long to) {
        long max = 0;
        try {
            UsageEvents events = usm.queryEvents(from, to);
            if (events == null) return 0;
            UsageEvents.Event e = new UsageEvents.Event();
            while (events.hasNextEvent()) {
                events.getNextEvent(e);
                handle(e);
                if (e.getTimeStamp() > max) max = e.getTimeStamp();
            }
        } catch (Throwable ignored) {
        }
        return max;
    }

    private void handle(UsageEvents.Event e) {
        int type = e.getEventType();
        long ts = e.getTimeStamp();
        if (type == UsageEvents.Event.ACTIVITY_RESUMED) {
            String pkg = e.getPackageName();
            if (pkg == null || pkg.isEmpty()) return;
            if (pkg.equals(curPkg)) return;
            closeAt(ts);
            curPkg = pkg;
            curStart = ts;
        } else if (type == UsageEvents.Event.SCREEN_NON_INTERACTIVE
                || type == UsageEvents.Event.DEVICE_SHUTDOWN) {
            closeAt(ts);
        }
    }

    private void closeAt(long ts) {
        if (curPkg == null) return;
        if (ts > curStart) pending.add(new Repo.Sess(curPkg, curStart, ts));
        curPkg = null;
        curStart = 0;
        prefs.clearOpen();
    }

    private void flush() {
        if (pending.isEmpty()) return;
        List<Repo.Sess> copy = new ArrayList<>(pending);
        pending.clear();
        try {
            repo.addSessions(copy);
        } catch (Throwable ignored) {
        }
    }

    public void closeNow() {
        closeAt(System.currentTimeMillis());
        flush();
    }

    public String currentPkg() {
        return curPkg;
    }

    public long currentSince() {
        return curStart;
    }
}
