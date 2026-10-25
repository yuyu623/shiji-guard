package com.shiji.guard;

import android.content.Context;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 把本机统计整理成给模型看的简报。 */
public final class Insights {

    private Insights() {
    }

    public static String dailyPayload(Context c, Repo repo) {
        long now = System.currentTimeMillis();
        String day = TimeUtil.dayKey(now);
        StringBuilder sb = new StringBuilder();
        long screen = Engine.totalToday();
        long media = MediaTracker.totalToday();
        long total = screen + media;
        sb.append("日期：").append(TimeUtil.dayLabel(now)).append('\n');
        sb.append("现在：").append(TimeUtil.hourMinute(now)).append('\n');
        sb.append("今日总占用：").append(TimeUtil.fmtDuration(total))
                .append("（屏幕 ").append(TimeUtil.fmtDuration(screen))
                .append(" + 后台音频 ").append(TimeUtil.fmtDuration(media)).append("）\n");
        long yesterday = Usage.totalOnDay(TimeUtil.addDays(now, -1));
        sb.append("昨日全天：").append(TimeUtil.fmtDuration(yesterday)).append('\n');

        long weekSum = 0;
        for (int i = 1; i <= 7; i++) {
            weekSum += Usage.totalOnDay(TimeUtil.addDays(now, -i));
        }
        sb.append("近 7 日日均：").append(TimeUtil.fmtDuration(weekSum / 7)).append('\n');

        LinkedHashMap<String, Long> apps = Usage.appsOnDay(day);
        sb.append("今日应用（含后台）：").append(topList(c, apps, 5)).append('\n');
        LinkedHashMap<String, Long> mediaApps = repo.mediaAppsOnDay(day);
        if (!mediaApps.isEmpty()) {
            sb.append("后台音频：").append(topList(c, mediaApps, 5)).append('\n');
        }

        List<Repo.Rule> rules = repo.rules();
        if (!rules.isEmpty()) {
            sb.append("限额：");
            boolean first = true;
            for (Repo.Rule r : rules) {
                if (!r.enabled || r.limitMin <= 0) continue;
                if (!first) sb.append("；");
                first = false;
                long used = Usage.totalToday(r.pkg);
                sb.append(Apps.name(c, r.pkg)).append(' ')
                        .append(r.limitMin).append(" 分钟，已用 ").append(used / TimeUtil.MINUTE).append(" 分钟");
                if (used >= r.limitMin * TimeUtil.MINUTE) sb.append("（已超限）");
            }
            if (first) sb.append("无启用中的规则");
            sb.append('\n');
        }
        sb.append("今日被拦截：").append(repo.blocksOnDay(day)).append(" 次\n");
        sb.append("请写今日点评。");
        return sb.toString();
    }

    public static String weekPayload(Context c, Repo repo) {
        long now = System.currentTimeMillis();
        long start = TimeUtil.weekStart(now);
        long prevStart = TimeUtil.addDays(start, -7);
        long elapsed = now - start;
        long total = periodTotal(c, repo, start, now + 1);
        long prev = periodTotal(c, repo, prevStart, prevStart + elapsed);
        StringBuilder sb = new StringBuilder();
        sb.append("周期：").append(TimeUtil.weekRangeLabel(start)).append("（本周，尚未结束）\n");
        sb.append("本周累计：").append(TimeUtil.fmtDuration(total))
                .append("（含后台音频 ").append(TimeUtil.fmtDuration(MediaTracker.totalBetween(start, now + 1))).append("）\n");
        sb.append("上周同期：").append(TimeUtil.fmtDuration(prev)).append('\n');
        sb.append("每日：");
        LinkedHashMap<String, Long> daily = Usage.dailyTotals(start, now + 1);
        for (long d = start; d <= now; d = TimeUtil.addDays(d, 1)) {
            sb.append(TimeUtil.shortDay(d)).append(' ')
                    .append(TimeUtil.fmtCompact(daily.containsKey(TimeUtil.dayKey(d)) ? daily.get(TimeUtil.dayKey(d)) : 0L))
                    .append("；");
        }
        sb.append('\n');
        LinkedHashMap<String, Long> apps = Usage.appsBetween(start, now + 1, 6);
        sb.append("应用排名（含后台）：").append(topList(c, apps, 6)).append('\n');
        sb.append("本周被拦截：").append(weekBlocks(repo, start, now)).append(" 次\n");
        sb.append("请写本周点评。");
        return sb.toString();
    }

    public static String monthPayload(Context c, Repo repo) {
        long now = System.currentTimeMillis();
        long start = TimeUtil.monthStart(now);
        long prevStart = TimeUtil.monthStart(TimeUtil.addDays(start, -1));
        long elapsed = now - start;
        long total = periodTotal(c, repo, start, now + 1);
        long prev = periodTotal(c, repo, prevStart, Math.min(prevStart + elapsed, start));
        StringBuilder sb = new StringBuilder();
        sb.append("周期：").append(TimeUtil.monthLabel(now)).append("（本月，尚未结束）\n");
        sb.append("本月累计：").append(TimeUtil.fmtDuration(total))
                .append("（含后台音频 ").append(TimeUtil.fmtDuration(MediaTracker.totalBetween(start, now + 1))).append("）\n");
        sb.append("上月同期：").append(TimeUtil.fmtDuration(prev)).append('\n');
        LinkedHashMap<String, Long> daily = Usage.dailyTotals(start, now + 1);
        long best = 0;
        for (Long v : daily.values()) {
            if (v > best) best = v;
        }
        sb.append("单日最高：").append(TimeUtil.fmtDuration(best)).append('\n');
        LinkedHashMap<String, Long> apps = Usage.appsBetween(start, now + 1, 6);
        sb.append("应用排名（含后台）：").append(topList(c, apps, 6)).append('\n');
        sb.append("本月被拦截：").append(weekBlocks(repo, start, now)).append(" 次\n");
        sb.append("请写本月点评。");
        return sb.toString();
    }

    static long periodTotal(Context c, Repo repo, long from, long to) {
        long screen = repo.totalBetweenExcluding(from, to, Sys.excludedArray(c));
        long media = MediaTracker.totalBetween(from, to);
        return Math.max(0, screen + media);
    }

    private static long weekBlocks(Repo repo, long from, long to) {
        long sum = 0;
        for (long d = TimeUtil.dayStart(from); d < to; d = TimeUtil.addDays(d, 1)) {
            sum += repo.blocksOnDay(TimeUtil.dayKey(d));
        }
        return sum;
    }

    private static String topList(Context c, LinkedHashMap<String, Long> map, int limit) {
        if (map.isEmpty()) return "暂无记录";
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (Map.Entry<String, Long> e : map.entrySet()) {
            if (i >= limit) break;
            if (Sys.excluded(c, e.getKey())) continue;
            if (i > 0) sb.append("；");
            sb.append(Apps.name(c, e.getKey())).append(' ').append(TimeUtil.fmtCompact(e.getValue()));
            i++;
        }
        return sb.toString();
    }
}
