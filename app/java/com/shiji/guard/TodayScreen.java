package com.shiji.guard;

import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TodayScreen extends Screen {

    private String lastSig = "";
    private boolean aiLoading;
    private boolean autoTried;
    private String aiError;

    public TodayScreen(MainActivity a) {
        super(a);
    }

    @Override
    public void refresh() {
        long now = System.currentTimeMillis();
        String day = TimeUtil.dayKey(now);
        Repo repo = App.get().repo();
        long screenTotal = Engine.totalToday();
        long mediaTotal = MediaTracker.totalToday();
        long total = screenTotal + mediaTotal;
        long prev = Usage.totalOnDay(TimeUtil.addDays(now, -1));
        LinkedHashMap<String, Long> apps = Usage.appsOnDay(day);
        int blocks = repo.blocksOnDay(day);
        AiStyle.Profile profile = AiStyle.current(App.get().prefs());
        String aiKey = AiStyle.cacheKey("d:" + day, profile);
        String ai = repo.aiGet(aiKey);
        String payload = dailyPayloadSafe();
        if (ai == null && !aiLoading && !autoTried && AiStyle.canAuto(profile, App.get().prefs())) {
            autoTried = true;
            aiLoading = true;
            AiHelper.generate(aiKey, payload, true, false, profile, aiCallback());
        }
        boolean usageOn = Permissions.usage(ctx);
        boolean tracking = Engine.running();
        String sig = total / 60000 + "|" + screenTotal / 60000 + "|" + mediaTotal / 60000
                + "|" + apps + "|" + blocks + "|" + ai + "|" + aiLoading
                + "|" + aiError + "|" + usageOn + "|" + tracking + "|" + profile.signature()
                + "|" + TimeUtil.hourMinute(now);
        if (sig.equals(lastSig)) return;
        lastSig = sig;

        int y = scroll.getScrollY();
        clear();

        LinearLayout head = Ui.row(ctx);
        TextView brand = Ui.tv(ctx, "时迹", 11, Ui.ACCENT);
        brand.setLetterSpacing(0.34f);
        head.addView(brand);
        head.addView(spacer());
        head.addView(Ui.dot(ctx, tracking ? Ui.ACCENT : Ui.FAINT, 6),
                new LinearLayout.LayoutParams(Ui.dp(ctx, 6), Ui.dp(ctx, 6)));
        Ui.margins(head.getChildAt(head.getChildCount() - 1), 0, 0, 6, 0);
        head.addView(Ui.tv(ctx, tracking ? "记录中" : "未在记录", 11, tracking ? Ui.DIM : Ui.DANGER));
        add(head);
        add(Ui.space(ctx, 32));

        add(Ui.tv(ctx, TimeUtil.dayLabel(now), 12.5f, Ui.DIM));
        add(Ui.space(ctx, 12));
        add(Ui.tv(ctx, "总占用", 10.5f, Ui.FAINT));
        add(Ui.space(ctx, 6));
        TextView big = Ui.light(ctx, TimeUtil.fmtBig(total), 39, Ui.TEXT);
        big.setFontFeatureSettings("tnum");
        add(big);
        if (mediaTotal > 0) {
            add(Ui.space(ctx, 8));
            add(Ui.tv(ctx, "屏幕 " + TimeUtil.fmtDuration(screenTotal)
                    + " · 后台音频 " + TimeUtil.fmtDuration(mediaTotal), 11.5f, Ui.FAINT));
        }
        add(Ui.space(ctx, 10));
        add(Ui.tv(ctx, compareText(total, prev), 12.5f,
                total < prev ? Ui.ACCENT : Ui.DIM));

        if (!usageOn) {
            add(Ui.space(ctx, 24));
            LinearLayout banner = Ui.row(ctx);
            banner.setPadding(Ui.dp(ctx, 14), Ui.dp(ctx, 12), Ui.dp(ctx, 14), Ui.dp(ctx, 12));
            banner.setBackground(Ui.rounded(Ui.OK_BG, Ui.dp(ctx, 14)));
            banner.addView(Ui.dot(ctx, Ui.ACCENT, 6), new LinearLayout.LayoutParams(Ui.dp(ctx, 6), Ui.dp(ctx, 6)));
            TextView txt = Ui.tv(ctx, "使用情况访问未开启，无法统计 · 去开启", 12.5f, Ui.ACCENT);
            txt.setPadding(Ui.dp(ctx, 10), 0, 0, 0);
            banner.addView(txt, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            Ui.click(banner, () -> Permissions.openUsage(act));
            add(banner);
        }

        if (!Permissions.notificationAccess(ctx)) {
            add(Ui.space(ctx, 12));
            LinearLayout banner = Ui.row(ctx);
            banner.setPadding(Ui.dp(ctx, 14), Ui.dp(ctx, 12), Ui.dp(ctx, 14), Ui.dp(ctx, 12));
            banner.setBackground(Ui.rounded(Ui.FIELD, Ui.dp(ctx, 14)));
            banner.addView(Ui.dot(ctx, Ui.FAINT, 6),
                    new LinearLayout.LayoutParams(Ui.dp(ctx, 6), Ui.dp(ctx, 6)));
            TextView txt = Ui.tv(ctx, "后台音频统计未开启 · 去开启", 12.5f, Ui.TEXT);
            txt.setPadding(Ui.dp(ctx, 10), 0, 0, 0);
            banner.addView(txt, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            Ui.click(banner, () -> Permissions.openNotificationAccess(act));
            add(banner);
        }

        add(Ui.space(ctx, 30));
        DayStrip strip = new DayStrip(ctx);
        strip.setData(TimeUtil.dayStart(now), repo.sessionsOnDay(day));
        add(strip);
        add(Ui.space(ctx, 7));
        LinearLayout scale = Ui.row(ctx);
        String[] ticks = {"00", "06", "12", "18", "24"};
        for (int i = 0; i < ticks.length; i++) {
            TextView t = Ui.tv(ctx, ticks[i], 9.5f, Ui.FAINT);
            if (i == 0) t.setGravity(Gravity.START);
            else if (i == ticks.length - 1) t.setGravity(Gravity.END);
            else t.setGravity(Gravity.CENTER);
            scale.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }
        add(scale);

        add(Ui.space(ctx, 34));
        LinearLayout sectionRow = Ui.row(ctx);
        sectionRow.addView(Ui.section(ctx, "使用最多"));
        sectionRow.addView(spacer());
        sectionRow.addView(Ui.tv(ctx, apps.isEmpty() ? "暂无" : "前 " + Math.min(apps.size(), 6), 10.5f, Ui.FAINT));
        add(sectionRow);
        add(Ui.space(ctx, 12));

        List<String> chosen = new ArrayList<>();
        for (Map.Entry<String, Long> e : apps.entrySet()) {
            if (e.getValue() < TimeUtil.MINUTE) continue;
            chosen.add(e.getKey());
            if (chosen.size() >= 6) break;
        }
        if (chosen.isEmpty() && !apps.isEmpty()) {
            chosen.add(apps.keySet().iterator().next());
        }

        if (chosen.isEmpty()) {
            TextView empty = Ui.tv(ctx, usageOn
                            ? "还没有记录。保持「持续记录」开启，几分钟后回来看看。"
                            : "开启「使用情况访问」后，这里会出现每个应用的时长。",
                    13, Ui.FAINT);
            empty.setLineSpacing(Ui.dp(ctx, 6), 1.25f);
            add(empty);
        } else {
            for (int i = 0; i < chosen.size(); i++) {
                String pkg = chosen.get(i);
                Repo.Rule rule = repo.rule(pkg);
                add(appRow(pkg, Usage.totalToday(pkg), Usage.screenToday(pkg), Usage.mediaToday(pkg), rule));
                if (i < chosen.size() - 1) {
                    View line = Ui.hline(ctx, Ui.LINE);
                    add(line);
                }
            }
        }

        add(Ui.space(ctx, 34));
        add(aiCard(aiKey, ai, now, payload, profile));

        add(Ui.space(ctx, 26));
        if (blocks > 0) {
            TextView b = Ui.tv(ctx, "今天被拦截 " + blocks + " 次", 11, Ui.FAINT);
            add(b);
            add(Ui.space(ctx, 10));
        }
        add(Ui.tv(ctx, "数据只保存在本机 · 生成点评时才会上传汇总", 10.5f, Ui.FAINT));

        scroll.post(() -> scroll.scrollTo(0, y));
    }

    private View appRow(String pkg, long used, long screen, long media, Repo.Rule rule) {
        long limit = rule == null ? 0 : rule.limitMin * TimeUtil.MINUTE;
        boolean over = rule != null && limit > 0 && used >= limit;

        LinearLayout wrap = Ui.col(ctx);
        wrap.setPadding(0, Ui.dp(ctx, 10), 0, Ui.dp(ctx, 10));
        LinearLayout row = Ui.row(ctx);
        row.addView(Ui.icon(ctx, pkg, 34));

        LinearLayout mid = Ui.col(ctx);
        mid.setPadding(Ui.dp(ctx, 13), 0, Ui.dp(ctx, 10), 0);
        TextView name = Ui.tv(ctx, Apps.name(ctx, pkg), 13.5f, Ui.TEXT);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        mid.addView(name);
        if (media > 0) {
            mid.addView(Ui.space(ctx, 5));
            mid.addView(Ui.tv(ctx, "屏幕 " + TimeUtil.fmtCompact(screen) + " · 后台 " + TimeUtil.fmtCompact(media),
                    11, over ? Ui.DANGER : Ui.FAINT));
        }
        if (rule != null && rule.limitMin > 0) {
            mid.addView(Ui.space(ctx, 5));
            TextView sub = Ui.tv(ctx, "限额 " + TimeUtil.fmtCompact(limit) + " · 已用 " + TimeUtil.fmtCompact(used),
                    11, over ? Ui.DANGER : Ui.FAINT);
            mid.addView(sub);
        }
        row.addView(mid, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView time = Ui.tv(ctx, TimeUtil.fmtCompact(used), 13, over ? Ui.DANGER : Ui.DIM);
        time.setFontFeatureSettings("tnum");
        row.addView(time);
        wrap.addView(row);

        if (rule != null && rule.limitMin > 0) {
            LineProgress bar = new LineProgress(ctx);
            bar.setProgress(used / (float) Math.max(1, limit));
            bar.setColor(over ? Ui.DANGER : Ui.ACCENT);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(ctx, 3));
            lp.topMargin = Ui.dp(ctx, 11);
            wrap.addView(bar, lp);
        }

        Ui.click(wrap, () -> Dialogs.limit(ctx, pkg, () -> {
            lastSig = "";
            refresh();
        }));
        return wrap;
    }

    private View aiCard(String cacheKey, String ai, long now, String payload, AiStyle.Profile profile) {
        LinearLayout card = Ui.col(ctx);
        card.setBackground(Ui.rounded(Ui.SURFACE, Ui.dp(ctx, 20)));
        card.setPadding(Ui.dp(ctx, 18), Ui.dp(ctx, 17), Ui.dp(ctx, 18), Ui.dp(ctx, 17));

        LinearLayout top = Ui.row(ctx);
        top.addView(Ui.tv(ctx, "AI 点评", 11.5f, Ui.ACCENT));
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
            body = "让 AI 按当前风格，写一段今天的使用观察。";
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
        AiHelper.generate(cacheKey, payload, true, force, profile, aiCallback());
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

    private String dailyPayloadSafe() {
        try {
            return Insights.dailyPayload(ctx, App.get().repo());
        } catch (Throwable t) {
            return "请写今日点评。";
        }
    }

    private String compareText(long total, long prev) {
        if (prev <= 0) {
            return total > 0 ? "昨天没有记录，今天是新的开始" : "今天还没有使用记录";
        }
        if (prev < 5 * TimeUtil.MINUTE) return "昨日记录不足，暂不比较";
        long diff = total - prev;
        long pct = Math.round(Math.abs(diff) * 100.0 / prev);
        if (pct == 0) return "与昨日持平";
        if (pct > 999) return "较昨日 " + (diff > 0 ? "大幅增加" : "大幅减少");
        return "较昨日 " + (diff > 0 ? "↑ " : "↓ ") + pct + "%";
    }

    private View spacer() {
        View v = new View(ctx);
        v.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));
        return v;
    }
}
