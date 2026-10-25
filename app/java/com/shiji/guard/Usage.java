package com.shiji.guard;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 屏幕使用 + 后台音频的统一口径。 */
public final class Usage {

    private Usage() {
    }

    public static long screenToday(String pkg) {
        return Engine.usageToday(pkg);
    }

    public static long mediaToday(String pkg) {
        return MediaTracker.usageToday(pkg);
    }

    public static long totalToday(String pkg) {
        return screenToday(pkg) + mediaToday(pkg);
    }

    public static long totalTodayAll() {
        return Engine.totalToday() + MediaTracker.totalToday();
    }

    public static long totalOnDay(long ts) {
        long start = TimeUtil.dayStart(ts);
        long end = TimeUtil.nextMidnight(start);
        return Engine.totalOnDay(ts) + MediaTracker.totalBetween(start, end);
    }

    public static long totalBetween(String pkg, long from, long to) {
        long screen = App.get().repo().usageBetween(pkg, from, to);
        if (pkg.equals(Engine.currentPkg()) && !Sys.excluded(App.get(), pkg)) {
            long since = Engine.currentSince();
            long s = Math.max(since, from);
            long e = Math.min(to, System.currentTimeMillis());
            if (e > s) screen += e - s;
        }
        return screen + MediaTracker.usageBetween(pkg, from, to);
    }

    public static LinkedHashMap<String, Long> appsOnDay(String day) {
        Repo repo = App.get().repo();
        Set<String> keys = new LinkedHashSet<>();
        keys.addAll(repo.appsOnDay(day).keySet());
        keys.addAll(repo.mediaAppsOnDay(day).keySet());
        boolean today = day.equals(TimeUtil.dayKey(System.currentTimeMillis()));
        if (today) {
            String cur = Engine.currentPkg();
            if (cur != null) keys.add(cur);
            keys.addAll(MediaTracker.activePackages());
        }
        List<Map.Entry<String, Long>> list = new ArrayList<>();
        for (String pkg : keys) {
            if (Sys.excluded(App.get(), pkg)) continue;
            long total;
            if (today) {
                total = totalToday(pkg);
            } else {
                total = repo.usageOnDay(pkg, day) + repo.mediaUsageOnDay(pkg, day);
            }
            if (total > 0) list.add(new AbstractMap.SimpleEntry<>(pkg, total));
        }
        list.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        LinkedHashMap<String, Long> out = new LinkedHashMap<>();
        for (Map.Entry<String, Long> e : list) out.put(e.getKey(), e.getValue());
        return out;
    }

    public static LinkedHashMap<String, Long> appsBetween(long from, long to, int limit) {
        Repo repo = App.get().repo();
        Set<String> keys = new LinkedHashSet<>();
        keys.addAll(repo.appsBetween(from, to, Math.max(limit, 5) * 2).keySet());
        keys.addAll(repo.mediaAppsBetween(from, to, Math.max(limit, 5) * 2).keySet());
        long now = System.currentTimeMillis();
        if (from <= now && to > now) {
            String cur = Engine.currentPkg();
            if (cur != null) keys.add(cur);
            keys.addAll(MediaTracker.activePackages());
        }
        List<Map.Entry<String, Long>> list = new ArrayList<>();
        for (String pkg : keys) {
            if (Sys.excluded(App.get(), pkg)) continue;
            long total = totalBetween(pkg, from, to);
            if (total > 0) list.add(new AbstractMap.SimpleEntry<>(pkg, total));
        }
        list.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        LinkedHashMap<String, Long> out = new LinkedHashMap<>();
        for (Map.Entry<String, Long> e : list) {
            if (out.size() >= limit) break;
            out.put(e.getKey(), e.getValue());
        }
        return out;
    }

    public static LinkedHashMap<String, Long> dailyTotals(long from, long to) {
        LinkedHashMap<String, Long> screen = App.get().repo()
                .dailyTotalsExcluding(from, to, Sys.excludedArray(App.get()));
        LinkedHashMap<String, Long> media = App.get().repo().mediaDailyTotals(from, to);
        LinkedHashMap<String, Long> out = new LinkedHashMap<>();
        Set<String> days = new LinkedHashSet<>();
        days.addAll(screen.keySet());
        days.addAll(media.keySet());
        for (String day : days) {
            long v = (screen.containsKey(day) ? screen.get(day) : 0L)
                    + (media.containsKey(day) ? media.get(day) : 0L);
            out.put(day, v);
        }
        long now = System.currentTimeMillis();
        if (from <= now && to > now) {
            String today = TimeUtil.dayKey(now);
            long add = MediaTracker.activeDelta();
            long since = Engine.currentSince();
            String cur = Engine.currentPkg();
            if (since > 0 && cur != null && !Sys.excluded(App.get(), cur)) {
                add += Math.max(0, now - Math.max(since, TimeUtil.dayStart(now)));
            }
            if (add > 0) out.put(today, (out.containsKey(today) ? out.get(today) : 0L) + add);
        }
        return out;
    }
}
