package com.shiji.guard;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class Dialogs {

    private static final int[] LIMITS = {0, 1, 5, 15, 30, 45, 60, 90, 120};

    private Dialogs() {
    }

    public static void limit(Context c, String pkg, Runnable after) {
        Repo.Rule rule = App.get().repo().rule(pkg);
        final int[] sel = {rule == null ? 0 : rule.limitMin};
        LinearLayout card = card(c);
        LinearLayout head = Ui.row(c);
        head.addView(Ui.icon(c, pkg, 42));
        LinearLayout names = Ui.col(c);
        names.setPadding(Ui.dp(c, 14), 0, 0, 0);
        TextView name = Ui.tv(c, Apps.name(c, pkg), 16, Ui.TEXT);
        names.addView(name);
        names.addView(Ui.space(c, 5));
        names.addView(Ui.tv(c, "每天允许使用多久", 12, Ui.DIM));
        head.addView(names, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        card.addView(head);
        card.addView(Ui.space(c, 22));

        LinearLayout grid = Ui.col(c);
        card.addView(grid);
        fillGrid(c, grid, sel);
        card.addView(Ui.space(c, 22));

        LinearLayout buttons = Ui.row(c);
        if (rule != null) {
            TextView remove = Ui.button(c, "移除规则", false);
            remove.setOnClickListener(v -> {
                App.get().repo().deleteRule(pkg);
                dismissDialog(card);
                after.run();
            });
            buttons.addView(remove, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            View gap = new View(c);
            buttons.addView(gap, new LinearLayout.LayoutParams(Ui.dp(c, 10), 1));
        }
        TextView save = Ui.button(c, "保存", true);
        save.setOnClickListener(v -> {
            App.get().repo().setRule(pkg, sel[0], sel[0] > 0);
            dismissDialog(card);
            after.run();
        });
        buttons.addView(save, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        card.addView(buttons);

        show(c, card);
    }

    private static void fillGrid(Context c, LinearLayout grid, int[] sel) {
        grid.removeAllViews();
        for (int rowStart = 0; rowStart < LIMITS.length; rowStart += 3) {
            LinearLayout row = Ui.row(c);
            for (int j = 0; j < 3; j++) {
                int idx = rowStart + j;
                if (idx >= LIMITS.length) {
                    View filler = new View(c);
                    row.addView(filler, new LinearLayout.LayoutParams(0, 1, 1f));
                    continue;
                }
                int value = LIMITS[idx];
                TextView chip = Ui.chip(c, value == 0 ? "不限" : value + " 分", sel[0] == value);
                chip.setOnClickListener(v -> {
                    sel[0] = value;
                    fillGrid(c, grid, sel);
                });
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                lp.setMargins(Ui.dp(c, 4), Ui.dp(c, 4), Ui.dp(c, 4), Ui.dp(c, 4));
                row.addView(chip, lp);
            }
            grid.addView(row);
        }
    }

    public static void appPicker(Context c, Runnable after) {
        LinearLayout card = card(c);
        card.addView(Ui.tv(c, "添加应用", 18, Ui.TEXT));
        card.addView(Ui.space(c, 10));
        card.addView(Ui.tv(c, "选择一个应用，为它设定每日使用上限", 12.5f, Ui.DIM));
        card.addView(Ui.space(c, 18));

        EditText search = new EditText(c);
        search.setHint("搜索应用");
        search.setTextSize(13);
        search.setTextColor(Ui.TEXT);
        search.setHintTextColor(Ui.FAINT);
        search.setSingleLine(true);
        search.setPadding(Ui.dp(c, 14), Ui.dp(c, 11), Ui.dp(c, 14), Ui.dp(c, 11));
        search.setBackground(Ui.rounded(Ui.FIELD, Ui.dp(c, 12)));
        card.addView(search);
        card.addView(Ui.space(c, 12));

        ScrollView sc = new ScrollView(c);
        sc.setVerticalScrollBarEnabled(false);
        LinearLayout list = Ui.col(c);
        sc.addView(list);
        card.addView(sc, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(c, 360)));

        long now = System.currentTimeMillis();
        Map<String, Long> usage = App.get().repo().appsOnDay(TimeUtil.dayKey(now));
        List<Apps.Info> apps = Apps.launchable(c);
        apps.sort((a, b) -> {
            long ua = usage.containsKey(a.pkg) ? usage.get(a.pkg) : 0L;
            long ub = usage.containsKey(b.pkg) ? usage.get(b.pkg) : 0L;
            if (ua != ub) return Long.compare(ub, ua);
            return a.label.compareToIgnoreCase(b.label);
        });

        final Dialog[] holder = new Dialog[1];
        List<View> rows = new ArrayList<>();
        for (Apps.Info info : apps) {
            LinearLayout row = Ui.row(c);
            row.setPadding(0, Ui.dp(c, 9), 0, Ui.dp(c, 9));
            row.addView(Ui.icon(c, info.pkg, 34));
            LinearLayout mid = Ui.col(c);
            mid.setPadding(Ui.dp(c, 12), 0, Ui.dp(c, 8), 0);
            mid.addView(Ui.tv(c, info.label, 14, Ui.TEXT));
            boolean hasRule = App.get().repo().rule(info.pkg) != null;
            long used = usage.containsKey(info.pkg) ? usage.get(info.pkg) : 0L;
            String sub = hasRule ? "已设限额" : (used > 0 ? "今日 " + TimeUtil.fmtCompact(used) : "");
            if (!sub.isEmpty()) {
                mid.addView(Ui.space(c, 4));
                mid.addView(Ui.tv(c, sub, 11, hasRule ? Ui.ACCENT : Ui.FAINT));
            }
            row.addView(mid, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            if (used > 0) {
                TextView time = Ui.tv(c, TimeUtil.fmtCompact(used), 12, Ui.DIM);
                time.setFontFeatureSettings("tnum");
                row.addView(time);
            }
            row.setOnClickListener(v -> {
                if (holder[0] != null) holder[0].dismiss();
                limit(c, info.pkg, after);
            });
            list.addView(row);
            list.addView(Ui.hline(c));
            rows.add(row);
            row.setTag(info.label.toLowerCase(Locale.ROOT) + " " + info.pkg.toLowerCase(Locale.ROOT));
        }

        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int st, int cnt, int aft) {
            }

            @Override
            public void onTextChanged(CharSequence s, int st, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String q = s.toString().trim().toLowerCase(Locale.ROOT);
                for (View row : rows) {
                    Object tag = row.getTag();
                    boolean show = q.isEmpty() || (tag != null && tag.toString().contains(q));
                    row.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            }
        });

        Dialog d = show(c, card);
        holder[0] = d;
    }

    public static void confirm(Context c, String title, String message, Runnable ok) {
        LinearLayout card = card(c);
        card.addView(Ui.tv(c, title, 18, Ui.TEXT));
        card.addView(Ui.space(c, 10));
        TextView msg = Ui.tv(c, message, 13, Ui.DIM);
        msg.setLineSpacing(Ui.dp(c, 5), 1.25f);
        card.addView(msg);
        card.addView(Ui.space(c, 22));
        LinearLayout buttons = Ui.row(c);
        TextView cancel = Ui.button(c, "取消", false);
        TextView yes = Ui.button(c, "确定", true);
        cancel.setOnClickListener(v -> dismissDialog(card));
        yes.setOnClickListener(v -> {
            dismissDialog(card);
            ok.run();
        });
        buttons.addView(cancel, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        View gap = new View(c);
        buttons.addView(gap, new LinearLayout.LayoutParams(Ui.dp(c, 10), 1));
        buttons.addView(yes, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        card.addView(buttons);
        show(c, card);
    }

    private static LinearLayout card(Context c) {
        LinearLayout card = Ui.col(c);
        card.setBackground(Ui.rounded(Ui.DIALOG, Ui.dp(c, 24)));
        card.setPadding(Ui.dp(c, 20), Ui.dp(c, 20), Ui.dp(c, 20), Ui.dp(c, 20));
        return card;
    }

    private static Dialog show(Context c, LinearLayout card) {
        Dialog d = new Dialog(c);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        d.setContentView(card);
        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            w.setDimAmount(0.72f);
        }
        d.setCanceledOnTouchOutside(true);
        d.show();
        card.setTag(d);
        if (w != null) {
            int width = c.getResources().getDisplayMetrics().widthPixels - Ui.dp(c, 44);
            w.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT);
            w.setGravity(Gravity.CENTER);
        }
        return d;
    }

    private static void dismissDialog(LinearLayout card) {
        try {
            Dialog d = (Dialog) card.getTag();
            if (d != null) d.dismiss();
        } catch (Throwable ignored) {
        }
    }
}
