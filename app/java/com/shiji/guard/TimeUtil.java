package com.shiji.guard;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public final class TimeUtil {

    public static final long MINUTE = 60 * 1000L;
    public static final long HOUR = 60 * MINUTE;
    public static final long DAY = 24 * HOUR;

    private TimeUtil() {
    }

    public static Calendar cal(long ts) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ts);
        return c;
    }

    public static long dayStart(long ts) {
        Calendar c = cal(ts);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    public static long nextMidnight(long ts) {
        Calendar c = cal(dayStart(ts));
        c.add(Calendar.DAY_OF_MONTH, 1);
        return c.getTimeInMillis();
    }

    public static long addDays(long ts, int days) {
        Calendar c = cal(ts);
        c.add(Calendar.DAY_OF_MONTH, days);
        return c.getTimeInMillis();
    }

    public static long weekStart(long ts) {
        Calendar c = cal(dayStart(ts));
        int dow = c.get(Calendar.DAY_OF_WEEK);
        int back = (dow + 5) % 7;
        c.add(Calendar.DAY_OF_MONTH, -back);
        return c.getTimeInMillis();
    }

    public static long monthStart(long ts) {
        Calendar c = cal(dayStart(ts));
        c.set(Calendar.DAY_OF_MONTH, 1);
        return c.getTimeInMillis();
    }

    public static String dayKey(long ts) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(ts));
    }

    public static String monthKey(long ts) {
        return new SimpleDateFormat("yyyy-MM", Locale.US).format(new Date(ts));
    }

    public static String hourMinute(long ts) {
        return new SimpleDateFormat("HH:mm", Locale.CHINA).format(new Date(ts));
    }

    public static String dayLabel(long ts) {
        return new SimpleDateFormat("M 月 d 日 EEEE", Locale.CHINA).format(new Date(ts));
    }

    public static String shortDay(long ts) {
        return new SimpleDateFormat("M.d", Locale.US).format(new Date(ts));
    }

    public static String weekRangeLabel(long weekStart) {
        Calendar c = cal(weekStart);
        String a = shortDay(weekStart);
        c.add(Calendar.DAY_OF_MONTH, 6);
        return a + " — " + shortDay(c.getTimeInMillis());
    }

    public static String monthLabel(long ts) {
        return new SimpleDateFormat("yyyy 年 M 月", Locale.CHINA).format(new Date(ts));
    }

    public static int hourOfDay(long ts) {
        return cal(ts).get(Calendar.HOUR_OF_DAY);
    }

    public static int dayOfMonth(long ts) {
        return cal(ts).get(Calendar.DAY_OF_MONTH);
    }

    public static String fmtDuration(long ms) {
        if (ms < 0) ms = 0;
        long totalMinutes = ms / MINUTE;
        long h = totalMinutes / 60;
        long m = totalMinutes % 60;
        if (h > 0) {
            return h + " 小时 " + (m < 10 ? "0" + m : String.valueOf(m)) + " 分";
        }
        if (m > 0) return m + " 分钟";
        long s = ms / 1000L;
        if (s > 0) return s + " 秒";
        return "0 分钟";
    }

    public static String fmtCompact(long ms) {
        long totalMinutes = Math.max(0, ms) / MINUTE;
        long h = totalMinutes / 60;
        long m = totalMinutes % 60;
        if (h > 0) return h + "h" + (m < 10 ? "0" + m : String.valueOf(m)) + "m";
        return m + "m";
    }

    public static String fmtBig(long ms) {
        long totalMinutes = Math.max(0, ms) / MINUTE;
        long h = totalMinutes / 60;
        long m = totalMinutes % 60;
        if (h > 0) return h + " 小时 " + (m < 10 ? "0" + m : String.valueOf(m)) + " 分";
        return m + " 分钟";
    }

    public static String percentDelta(long now, long before) {
        if (before <= 0) return now > 0 ? "较上期新增" : "暂无对比";
        long diff = now - before;
        long pct = Math.round(Math.abs(diff) * 100.0 / before);
        if (pct == 0) return "较上期持平";
        return "较上期 " + (diff > 0 ? "↑ " : "↓ ") + pct + "%";
    }
}
