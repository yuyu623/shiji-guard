package com.shiji.guard;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class Ui {

    public static int BG = 0xFF07080A;
    public static int SURFACE = 0xFF111216;
    public static int FIELD = 0xFF17181C;
    public static int LINE = 0xFF212329;
    public static int TEXT = 0xFFEDEEF1;
    public static int DIM = 0xFF7C7F86;
    public static int FAINT = 0xFF4D5057;
    public static int ACCENT = 0xFFCDF564;
    public static int DANGER = 0xFFFF7D6E;
    public static int TRACK = 0xFF1E2025;
    public static int BAR = 0xFF34373E;
    public static int BAR_EMPTY = 0xFF191B20;
    public static int BODY = 0xFFD7D9DE;
    public static int OK = 0xFF9AA06F;
    public static int OK_BG = 0xFF12160D;
    public static int DANGER_SOFT = 0xFFFFB4AA;
    public static int DANGER_BG = 0xFF1A1111;
    public static int DIALOG = 0xFF15161A;
    public static int NOW = 0x99FFFFFF;

    private static boolean sLight;

    private Ui() {
    }

    public static boolean isLight() {
        return sLight;
    }

    /** 根据「跟随系统 / 浅色 / 深色」设置刷新静态调色板。 */
    public static void applyTheme(Context c) {
        int mode = 0;
        try {
            mode = App.get().prefs().themeMode();
        } catch (Throwable ignored) {
        }
        boolean light;
        if (mode == 1) {
            light = true;
        } else if (mode == 2) {
            light = false;
        } else {
            int ui = c.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            light = ui != Configuration.UI_MODE_NIGHT_YES;
        }
        sLight = light;
        if (light) {
            BG = 0xFFF5F5EF;
            SURFACE = 0xFFFFFFFF;
            FIELD = 0xFFEFEFE8;
            LINE = 0xFFE2E2DA;
            TEXT = 0xFF151613;
            DIM = 0xFF6C6F68;
            FAINT = 0xFF9B9E96;
            ACCENT = 0xFF4F6B00;
            DANGER = 0xFFB3261E;
            TRACK = 0xFFE7E7DF;
            BAR = 0xFFC9CBC3;
            BAR_EMPTY = 0xFFEDEDE5;
            BODY = 0xFF2C2D29;
            OK = 0xFF4F6B00;
            OK_BG = 0xFFF0F4E2;
            DANGER_SOFT = 0xFF8C1D18;
            DANGER_BG = 0xFFFBEAE7;
            DIALOG = 0xFFFFFFFF;
            NOW = 0x99000000;
        } else {
            BG = 0xFF07080A;
            SURFACE = 0xFF111216;
            FIELD = 0xFF17181C;
            LINE = 0xFF212329;
            TEXT = 0xFFEDEEF1;
            DIM = 0xFF7C7F86;
            FAINT = 0xFF4D5057;
            ACCENT = 0xFFCDF564;
            DANGER = 0xFFFF7D6E;
            TRACK = 0xFF1E2025;
            BAR = 0xFF34373E;
            BAR_EMPTY = 0xFF191B20;
            BODY = 0xFFD7D9DE;
            OK = 0xFF9AA06F;
            OK_BG = 0xFF12160D;
            DANGER_SOFT = 0xFFFFB4AA;
            DANGER_BG = 0xFF1A1111;
            DIALOG = 0xFF15161A;
            NOW = 0x99FFFFFF;
        }
    }

    public static int dp(Context c, float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, c.getResources().getDisplayMetrics()));
    }

    public static TextView tv(Context c, CharSequence s, float sp, int color) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setIncludeFontPadding(false);
        t.setLineSpacing(dp(c, 5), 1.12f);
        return t;
    }

    public static TextView light(Context c, CharSequence s, float sp, int color) {
        TextView t = tv(c, s, sp, color);
        t.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        return t;
    }

    public static TextView label(Context c, String s) {
        TextView t = tv(c, s, 10.5f, DIM);
        t.setLetterSpacing(0.22f);
        return t;
    }

    public static TextView section(Context c, String s) {
        TextView t = tv(c, s, 11, FAINT);
        t.setLetterSpacing(0.16f);
        return t;
    }

    public static TextView tnum(TextView t) {
        t.setFontFeatureSettings("tnum");
        return t;
    }

    public static LinearLayout col(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    public static LinearLayout row(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    public static View space(Context c, int h) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(c, h)));
        return v;
    }

    public static View vline(Context c) {
        View v = new View(c);
        v.setBackgroundColor(0x22FFFFFF);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(c, 14)));
        return v;
    }

    public static View hline(Context c) {
        return hline(c, LINE);
    }

    public static View hline(Context c, int color) {
        View v = new View(c);
        v.setBackgroundColor(color);
        v.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(c, 0.8f))));
        return v;
    }

    public static View dot(Context c, int color, int sizeDp) {
        View v = new View(c);
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(color);
        v.setBackground(g);
        int s = dp(c, sizeDp);
        v.setLayoutParams(new LinearLayout.LayoutParams(s, s));
        return v;
    }

    public static GradientDrawable rounded(int color, float radiusPx) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radiusPx);
        return g;
    }

    public static TextView chip(Context c, String text, boolean selected) {
        TextView t = tv(c, text, 13, selected ? BG : TEXT);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(c, 10), dp(c, 10), dp(c, 10), dp(c, 10));
        GradientDrawable g = rounded(selected ? ACCENT : FIELD, dp(c, 100));
        if (!selected) g.setStroke(Math.max(1, dp(c, 1)), LINE);
        t.setBackground(g);
        return t;
    }

    public static TextView button(Context c, String text, boolean primary) {
        TextView t = tv(c, text, 13.5f, primary ? BG : TEXT);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(c, 18), dp(c, 13), dp(c, 18), dp(c, 13));
        t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        GradientDrawable g = rounded(primary ? ACCENT : 0x00000000, dp(c, 100));
        if (!primary) g.setStroke(Math.max(1, dp(c, 1)), LINE);
        t.setBackground(g);
        return t;
    }

    public static ImageView icon(Context c, String pkg, int dpSize) {
        ImageView iv = new ImageView(c);
        int px = dp(c, dpSize);
        iv.setLayoutParams(new LinearLayout.LayoutParams(px, px));
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        iv.setImageBitmap(Apps.icon(c, pkg, px));
        return iv;
    }

    public static void click(View v, Runnable r) {
        v.setOnClickListener(x -> r.run());
    }

    public static void margins(View v, int l, int t, int r, int b) {
        ViewGroup.LayoutParams lp = v.getLayoutParams();
        if (!(lp instanceof LinearLayout.LayoutParams)) {
            lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        LinearLayout.LayoutParams llp = (LinearLayout.LayoutParams) lp;
        Context c = v.getContext();
        llp.setMargins(dp(c, l), dp(c, t), dp(c, r), dp(c, b));
        v.setLayoutParams(llp);
    }

    public static void weight(View v, float w) {
        ViewGroup.LayoutParams lp = v.getLayoutParams();
        if (lp instanceof LinearLayout.LayoutParams) {
            ((LinearLayout.LayoutParams) lp).weight = w;
            ((LinearLayout.LayoutParams) lp).width = 0;
            v.setLayoutParams(lp);
        }
    }

    public static void padding(View v, int l, int t, int r, int b) {
        Context c = v.getContext();
        v.setPadding(dp(c, l), dp(c, t), dp(c, r), dp(c, b));
    }
}
