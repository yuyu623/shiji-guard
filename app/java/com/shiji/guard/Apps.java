package com.shiji.guard;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Apps {

    public static class Info {
        public final String pkg;
        public final String label;
        public Info(String pkg, String label) {
            this.pkg = pkg;
            this.label = label;
        }
    }

    private static final Map<String, String> NAMES = new HashMap<>();
    private static final Map<String, Bitmap> ICONS = new HashMap<>();

    private Apps() {
    }

    public static String name(Context c, String pkg) {
        if (pkg == null) return "";
        String cached = NAMES.get(pkg);
        if (cached != null) return cached;
        String label;
        try {
            PackageManager pm = c.getPackageManager();
            label = pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
        } catch (Throwable t) {
            label = pkg;
            int dot = pkg.lastIndexOf('.');
            if (dot >= 0 && dot < pkg.length() - 1) label = pkg.substring(dot + 1);
        }
        NAMES.put(pkg, label);
        return label;
    }

    public static Bitmap icon(Context c, String pkg, int px) {
        String key = pkg + "@" + px;
        Bitmap cached = ICONS.get(key);
        if (cached != null) return cached;
        Bitmap out = null;
        try {
            PackageManager pm = c.getPackageManager();
            Drawable d = pm.getApplicationIcon(pkg);
            Bitmap raw = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888);
            Canvas cv = new Canvas(raw);
            d.setBounds(0, 0, px, px);
            d.draw(cv);
            out = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888);
            Canvas cv2 = new Canvas(out);
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setShader(new BitmapShader(raw, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
            cv2.drawRoundRect(new RectF(0, 0, px, px), px * 0.26f, px * 0.26f, p);
        } catch (Throwable ignored) {
        }
        if (out == null) {
            out = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888);
            Canvas cv = new Canvas(out);
            cv.drawColor(0xFF262A30);
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setColor(0xFFCDF564);
            cv.drawCircle(px / 2f, px / 2f, px * 0.12f, p);
        }
        ICONS.put(key, out);
        return out;
    }

    public static List<Info> launchable(Context c) {
        PackageManager pm = c.getPackageManager();
        Intent i = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> list = pm.queryIntentActivities(i, 0);
        Map<String, Info> map = new LinkedHashMap<>();
        String self = c.getPackageName();
        for (ResolveInfo ri : list) {
            if (ri.activityInfo == null || ri.activityInfo.packageName == null) continue;
            String pkg = ri.activityInfo.packageName;
            if (pkg.equals(self) || map.containsKey(pkg)) continue;
            String label;
            try {
                label = ri.loadLabel(pm).toString();
            } catch (Throwable t) {
                label = pkg;
            }
            map.put(pkg, new Info(pkg, label));
        }
        List<Info> out = new ArrayList<>(map.values());
        out.sort((a, b) -> a.label.compareToIgnoreCase(b.label));
        return out;
    }
}
