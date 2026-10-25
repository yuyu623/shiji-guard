package com.shiji.guard;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

/** 统计口径：排除桌面、系统 UI 与时迹自身。 */
public final class Sys {

    private static String sLauncher;

    private Sys() {
    }

    public static String launcher(Context c) {
        if (sLauncher == null) {
            try {
                Intent i = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
                ResolveInfo ri = c.getPackageManager().resolveActivity(i, PackageManager.MATCH_DEFAULT_ONLY);
                sLauncher = ri != null && ri.activityInfo != null ? ri.activityInfo.packageName : "com.android.launcher";
            } catch (Throwable t) {
                sLauncher = "com.android.launcher";
            }
        }
        return sLauncher;
    }

    public static boolean excluded(Context c, String pkg) {
        if (pkg == null || pkg.isEmpty()) return true;
        if (pkg.equals(c.getPackageName())) return true;
        if (pkg.equals("com.android.systemui")) return true;
        String l = launcher(c);
        return l != null && !l.isEmpty() && pkg.equals(l);
    }

    public static String[] excludedArray(Context c) {
        return new String[]{c.getPackageName(), "com.android.systemui", launcher(c)};
    }
}
