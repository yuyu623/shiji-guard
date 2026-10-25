package com.shiji.guard;

import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

public class RulesScreen extends Screen {

    public RulesScreen(MainActivity a) {
        super(a);
    }

    @Override
    public void refresh() {
        long now = System.currentTimeMillis();
        String day = TimeUtil.dayKey(now);
        Repo repo = App.get().repo();
        int y = scroll.getScrollY();
        clear();

        add(Ui.label(ctx, "限制"));
        add(Ui.space(ctx, 26));
        add(Ui.light(ctx, "到点即止", 30, Ui.TEXT));
        add(Ui.space(ctx, 14));
        TextView desc = Ui.tv(ctx, "为应用设定每日额度。达到后覆盖应用并返回桌面，重新打开会再次拦截，直到次日零点。",
                13, Ui.DIM);
        desc.setLineSpacing(Ui.dp(ctx, 7), 1.2f);
        add(desc);

        add(Ui.space(ctx, 26));
        boolean on = Permissions.accessibility(ctx);
        LinearLayout status = Ui.row(ctx);
        status.setPadding(Ui.dp(ctx, 14), Ui.dp(ctx, 14), Ui.dp(ctx, 14), Ui.dp(ctx, 14));
        status.setBackground(Ui.rounded(on ? Ui.OK_BG : Ui.DANGER_BG, Ui.dp(ctx, 16)));
        status.addView(Ui.dot(ctx, on ? Ui.ACCENT : Ui.DANGER, 7),
                new LinearLayout.LayoutParams(Ui.dp(ctx, 7), Ui.dp(ctx, 7)));
        LinearLayout stCol = Ui.col(ctx);
        stCol.setPadding(Ui.dp(ctx, 12), 0, 0, 0);
        stCol.addView(Ui.tv(ctx, on ? "限额拦截已生效" : "无障碍服务未开启", 12.5f, on ? Ui.TEXT : Ui.DANGER_SOFT));
        stCol.addView(Ui.space(ctx, 5));
        stCol.addView(Ui.tv(ctx, on ? "时迹会在应用达到额度时接管屏幕" : "未开启时，达到额度也不会拦截",
                11, Ui.FAINT));
        status.addView(stCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        status.addView(Ui.tv(ctx, "设置", 11.5f, on ? Ui.DIM : Ui.ACCENT));
        Ui.click(status, () -> Permissions.openAccessibility(act));
        add(status);

        add(Ui.space(ctx, 34));
        LinearLayout head = Ui.row(ctx);
        head.addView(Ui.section(ctx, "限额应用"));
        head.addView(spacer());
        head.addView(Ui.tv(ctx, "每日重置", 10.5f, Ui.FAINT));
        add(head);
        add(Ui.space(ctx, 12));

        List<Repo.Rule> rules = repo.rules();
        rules.sort((a, b) -> Long.compare(Usage.totalToday(b.pkg), Usage.totalToday(a.pkg)));

        if (rules.isEmpty()) {
            TextView empty = Ui.tv(ctx, "还没有设置任何限额。先从最想控制的应用开始。", 13, Ui.FAINT);
            empty.setLineSpacing(Ui.dp(ctx, 6), 1.25f);
            add(empty);
            add(Ui.space(ctx, 18));
        } else {
            for (int i = 0; i < rules.size(); i++) {
                add(ruleRow(rules.get(i)));
                if (i < rules.size() - 1) add(Ui.hline(ctx, Ui.LINE));
            }
            add(Ui.space(ctx, 18));
        }

        TextView add = Ui.button(ctx, "＋  添加应用", false);
        Ui.click(add, () -> Dialogs.appPicker(ctx, () -> {
            refresh();
            act.refreshNow();
        }));
        add(add);

        add(Ui.space(ctx, 28));
        TextView note = Ui.tv(ctx, "关闭开关只会暂停拦截，统计仍会继续。后台音频也计入额度，到点会暂停播放；额度用完后无法在应用内解除，只能等到零点重置。",
                11, Ui.FAINT);
        note.setLineSpacing(Ui.dp(ctx, 6), 1.2f);
        add(note);

        scroll.post(() -> scroll.scrollTo(0, y));
    }

    private View ruleRow(Repo.Rule rule) {
        long used = Usage.totalToday(rule.pkg);
        long media = MediaTracker.usageToday(rule.pkg);
        long limit = rule.limitMin * TimeUtil.MINUTE;
        boolean over = used >= limit;

        LinearLayout wrap = Ui.col(ctx);
        wrap.setPadding(0, Ui.dp(ctx, 11), 0, Ui.dp(ctx, 13));
        LinearLayout row = Ui.row(ctx);
        row.addView(Ui.icon(ctx, rule.pkg, 34));

        LinearLayout mid = Ui.col(ctx);
        mid.setPadding(Ui.dp(ctx, 13), 0, Ui.dp(ctx, 8), 0);
        TextView name = Ui.tv(ctx, Apps.name(ctx, rule.pkg), 13.5f, rule.enabled ? Ui.TEXT : Ui.FAINT);
        mid.addView(name);
        mid.addView(Ui.space(ctx, 5));
        String sub = "今日 " + TimeUtil.fmtCompact(used) + " ／ 限额 " + TimeUtil.fmtCompact(limit);
        if (media > 0) sub = "今日 " + TimeUtil.fmtCompact(used) + "（后台 " + TimeUtil.fmtCompact(media) + "）／ 限额 " + TimeUtil.fmtCompact(limit);
        mid.addView(Ui.tv(ctx, sub, 11, over ? Ui.DANGER : Ui.FAINT));
        row.addView(mid, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Toggle sw = new Toggle(ctx);
        sw.setChecked(rule.enabled);
        sw.setListener(checked -> {
            App.get().repo().setRuleEnabled(rule.pkg, checked);
            lastRefresh();
        });
        row.addView(sw);
        wrap.addView(row);

        LineProgress bar = new LineProgress(ctx);
        bar.setProgress(used / (float) Math.max(1, limit));
        bar.setColor(over ? Ui.DANGER : Ui.ACCENT);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(ctx, 3));
        lp.topMargin = Ui.dp(ctx, 11);
        wrap.addView(bar, lp);

        Ui.click(wrap, () -> Dialogs.limit(ctx, rule.pkg, () -> {
            refresh();
            act.refreshNow();
        }));
        return wrap;
    }

    private void lastRefresh() {
        refresh();
    }

    private View spacer() {
        View v = new View(ctx);
        v.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));
        return v;
    }
}
