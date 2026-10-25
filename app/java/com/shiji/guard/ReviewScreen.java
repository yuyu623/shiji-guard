package com.shiji.guard;

import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.LinkedHashMap;
import java.util.Map;

public class ReviewScreen extends Screen {

    private boolean month;
    private String lastSig = "";
    private boolean aiLoading;
    private boolean autoTried;
    private String aiError;

    public ReviewScreen(MainActivity a) {
        super(a);
    }

    @Override
    public void refresh() {
        long now = System.currentTimeMillis();
        Repo repo = App.get().repo();
        long start = month ? TimeUtil.monthStart(now) : TimeUtil.weekStart(now);
        long end = now + 1;
        long prevStart = month ? TimeUtil.monthStart(TimeUtil.addDays(start, -1))
                : TimeUtil.addDays(start, -7);
        long prevEnd = Math.min(prevStart + (now - start), start);
        long screenTotal = repo.totalBetweenExcluding(start, end, Sys.excludedArray(ctx));
        long mediaTotal = MediaTracker.totalBetween(start, end);
        long total = screenTotal + mediaTotal;
        long prevScreen = repo.totalBetweenExcluding(prevStart, prevEnd, Sys.excludedArray(ctx));
        long prevMedia = MediaTracker.totalBetween(prevStart, prevEnd);
        long prev = prevScreen + prevMedia;
        LinkedHashMap<String, Long> apps = Usage.appsBetween(start, end, 10);
        LinkedHashMap<String, Long> daily = Usage.dailyTotals(start, end);
        AiStyle.Profile profile = AiStyle.current(App.get().prefs());
        String aiKey = AiStyle.cacheKey(
                (month ? "m:" : "w:") + (month ? TimeUtil.monthKey(now) : TimeUtil.dayKey(start)), profile);
        String ai = repo.aiGet(aiKey);
        String payload = month ? Insights.monthPayload(ctx, repo) : Insights.weekPayload(ctx, repo);
        if (ai == null && !aiLoading && !autoTried && AiStyle.canAuto(profile, App.get().prefs())) {
            autoTried = true;
            aiLoading = true;
            AiHelper.generate(aiKey, payload, false, false, profile, aiCallback());
        }

        String sig = total + "|" + screenTotal + "|" + mediaTotal + "|" + prev + "|" + apps + "|" + ai
                + "|" + aiLoading + "|" + aiError + "|" + month
                + "|" + profile.signature() + "|" + TimeUtil.hourMinute(now);
        if (sig.equals(lastSig)) return;
        lastSig = sig;

        int y = scroll.getScrollY();
        clear();

        add(Ui.label(ctx, "回顾"));
        add(Ui.space(ctx, 26));

        LinearLayout tabs = Ui.row(ctx);
        tabs.addView(tab("本周", !month));
        tabs.addView(tab("本月", month));
        tabs.addView(spacer());
        add(tabs);
        add(Ui.space(ctx, 30));

        String title = month ? TimeUtil.monthLabel(now) : TimeUtil.weekRangeLabel(start);
        add(Ui.tv(ctx, title, 11.5f, Ui.FAINT));
        add(Ui.space(ctx, 12));
        TextView big = Ui.light(ctx, TimeUtil.fmtBig(total), 37, Ui.TEXT);
        big.setFontFeatureSettings("tnum");
        add(big);
        if (mediaTotal > 0) {
            add(Ui.space(ctx, 7));
            add(Ui.tv(ctx, "屏幕 " + TimeUtil.fmtDuration(screenTotal)
                    + " · 后台音频 " + TimeUtil.fmtDuration(mediaTotal), 11.5f, Ui.FAINT));
        }
        add(Ui.space(ctx, 10));
        add(Ui.tv(ctx, compare(total, prev), 12.5f, total < prev ? Ui.ACCENT : Ui.DIM));

        add(Ui.space(ctx, 30));
        int days = month ? TimeUtil.dayOfMonth(now) : 7;
        long[] values = new long[days];
        String[] labels = new String[days];
        long peak = 0;
        String[] week = {"一", "二", "三", "四", "五", "六", "日"};
        for (int i = 0; i < days; i++) {
            long d = TimeUtil.addDays(start, i);
            Long v = daily.get(TimeUtil.dayKey(d));
            values[i] = v == null ? 0 : v;
            peak = Math.max(peak, values[i]);
            labels[i] = month ? String.valueOf(i + 1) : week[i];
        }
        BarChart chart = new BarChart(ctx);
        chart.setData(values, labels, month ? days - 1 : todayIndex(now));
        add(chart);
        add(Ui.space(ctx, 8));
        LinearLayout chartFoot = Ui.row(ctx);
        chartFoot.addView(Ui.tv(ctx, month ? "每日总占用" : "周一至周日", 10.5f, Ui.FAINT));
        chartFoot.addView(spacer());
        chartFoot.addView(Ui.tv(ctx, "单日最高 " + TimeUtil.fmtCompact(peak), 10.5f, Ui.FAINT));
        add(chartFoot);

        add(Ui.space(ctx, 34));
        LinearLayout section = Ui.row(ctx);
        section.addView(Ui.section(ctx, "应用排行"));
        section.addView(spacer());
        section.addView(Ui.tv(ctx, apps.isEmpty() ? "暂无" : "按时长", 10.5f, Ui.FAINT));
        add(section);
        add(Ui.space(ctx, 12));

        if (apps.isEmpty()) {
            TextView empty = Ui.tv(ctx, "这个周期还没有记录。", 13, Ui.FAINT);
            add(empty);
        } else {
            long max = apps.values().iterator().next();
            int i = 0;
            for (Map.Entry<String, Long> e : apps.entrySet()) {
                if (i >= 6) break;
                long media = MediaTracker.usageBetween(e.getKey(), start, end);
                add(rankRow(e.getKey(), e.getValue(), max, i == 0, media));
                i++;
            }
        }

        add(Ui.space(ctx, 34));
        add(aiCard(aiKey, ai, payload, now, profile));
        add(Ui.space(ctx, 22));
    }

    private View tab(String text, boolean active) {
        LinearLayout item = Ui.col(ctx);
        item.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView t = Ui.tv(ctx, text, 12.5f, active ? Ui.TEXT : Ui.FAINT);
        t.setGravity(Gravity.CENTER);
        item.addView(t, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        View line = new View(ctx);
        line.setBackgroundColor(active ? Ui.ACCENT : 0x00000000);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(ctx, 18), Ui.dp(ctx, 2));
        lp.topMargin = Ui.dp(ctx, 7);
        item.addView(line, lp);
        Ui.click(item, () -> {
            if (month == text.equals("本月")) return;
            month = text.equals("本月");
            lastSig = "";
            aiError = null;
            autoTried = false;
            refresh();
        });
        LinearLayout.LayoutParams itemLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        itemLp.rightMargin = Ui.dp(ctx, 22);
        item.setLayoutParams(itemLp);
        return item;
    }

    private View rankRow(String pkg, long ms, long max, boolean top, long media) {
        LinearLayout wrap = Ui.col(ctx);
        wrap.setPadding(0, Ui.dp(ctx, 10), 0, Ui.dp(ctx, 12));
        LinearLayout row = Ui.row(ctx);
        row.addView(Ui.icon(ctx, pkg, 30));
        LinearLayout mid = Ui.col(ctx);
        mid.setPadding(Ui.dp(ctx, 12), 0, Ui.dp(ctx, 10), 0);
        TextView name = Ui.tv(ctx, Apps.name(ctx, pkg), 13, Ui.TEXT);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        mid.addView(name);
        if (media > 0) {
            mid.addView(Ui.space(ctx, 4));
            mid.addView(Ui.tv(ctx, "含后台 " + TimeUtil.fmtCompact(media), 10, Ui.FAINT));
        }
        row.addView(mid, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView time = Ui.tv(ctx, TimeUtil.fmtCompact(ms), 12.5f, top ? Ui.ACCENT : Ui.DIM);
        time.setFontFeatureSettings("tnum");
        row.addView(time);
        wrap.addView(row);
        LineProgress bar = new LineProgress(ctx);
        bar.setProgress(ms / (float) Math.max(1, max));
        bar.setColor(top ? Ui.ACCENT : Ui.BAR);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(ctx, 2));
        lp.topMargin = Ui.dp(ctx, 10);
        wrap.addView(bar, lp);
        return wrap;
    }

    private View aiCard(String cacheKey, String ai, String payload, long now, AiStyle.Profile profile) {
        LinearLayout card = Ui.col(ctx);
        card.setBackground(Ui.rounded(Ui.SURFACE, Ui.dp(ctx, 20)));
        card.setPadding(Ui.dp(ctx, 18), Ui.dp(ctx, 17), Ui.dp(ctx, 18), Ui.dp(ctx, 17));
        LinearLayout top = Ui.row(ctx);
        top.addView(Ui.tv(ctx, month ? "AI 月评" : "AI 周评", 11.5f, Ui.ACCENT));
        TextView badge = Ui.tv(ctx, profile.badge(), 10, Ui.FAINT);
        badge.setPadding(Ui.dp(ctx, 9), 0, 0, 0);
        top.addView(badge);
        top.addView(spacer());
        TextView action = Ui.tv(ctx, aiLoading ? "生成中" : (ai == null ? "生成" : "换一个"),
                11.5f, aiLoading ? Ui.FAINT : (ai == null ? Ui.ACCENT : Ui.DIM));
        if (!aiLoading) {
            Ui.click(action, () -> generate(cacheKey, payload, profile, ai != null));
        }
        top.addView(action);
        card.addView(top);
        card.addView(Ui.space(ctx, 13));

        String body;
        int color;
        if (aiLoading) {
            body = "正在按「" + profile.name() + "」重写…";
            color = Ui.FAINT;
        } else if (ai != null) {
            body = ai;
            color = Ui.BODY;
        } else if (aiError != null) {
            body = aiError;
            color = Ui.DANGER;
        } else {
            body = month ? "让 AI 按当前风格，从月度结构里写一段观察。" : "让 AI 按当前风格，回看这一周。";
            color = Ui.FAINT;
        }
        TextView text = Ui.tv(ctx, body, 13.5f, color);
        text.setLineSpacing(Ui.dp(ctx, 7), 1.18f);
        card.addView(text);
        if (ai != null && !aiLoading) {
            long ts = App.get().repo().aiTs(cacheKey);
            card.addView(Ui.space(ctx, 12));
            card.addView(Ui.tv(ctx, "更新于 " + TimeUtil.hourMinute(ts), 10, Ui.FAINT));
        }
        return card;
    }

    private void generate(String cacheKey, String payload, AiStyle.Profile profile, boolean force) {
        if (aiLoading) return;
        autoTried = true;
        aiLoading = true;
        aiError = null;
        lastSig = "";
        refresh();
        AiHelper.generate(cacheKey, payload, false, force, profile, aiCallback());
    }

    private AiHelper.Callback aiCallback() {
        return new AiHelper.Callback() {
            @Override
            public void onText(String text) {
                aiLoading = false;
                aiError = null;
                lastSig = "";
                refresh();
            }

            @Override
            public void onError(String message) {
                aiLoading = false;
                aiError = message;
                lastSig = "";
                refresh();
            }
        };
    }

    private String compare(long total, long prev) {
        if (prev <= 0) return total > 0 ? "上一周期没有记录" : "还没有足够的数据";
        if (prev < 10 * TimeUtil.MINUTE) return "上一周期数据不足，暂不比较";
        long diff = total - prev;
        long pct = Math.round(Math.abs(diff) * 100.0 / prev);
        if (pct == 0) return "与上一周期持平";
        if (pct > 999) return (month ? "较上月同期 " : "较上周同期 ") + (diff > 0 ? "大幅增加" : "大幅减少");
        return (month ? "较上月同期 " : "较上周同期 ") + (diff > 0 ? "↑ " : "↓ ") + pct + "%";
    }

    private int todayIndex(long now) {
        long start = TimeUtil.weekStart(now);
        long diff = now - start;
        int idx = (int) (diff / TimeUtil.DAY);
        return Math.max(0, Math.min(6, idx));
    }

    private View spacer() {
        View v = new View(ctx);
        v.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));
        return v;
    }
}
