package com.shiji.guard;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;

public final class Permissions {

    private Permissions() {
    }

    public static boolean usage(Context c) {
        try {
            android.app.AppOpsManager aom = (android.app.AppOpsManager) c.getSystemService(Context.APP_OPS_SERVICE);
            if (aom == null) return false;
            int mode;
            if (Build.VERSION.SDK_INT >= 29) {
                mode = aom.unsafeCheckOpNoThrow("android:get_usage_stats", android.os.Process.myUid(), c.getPackageName());
            } else {
                mode = aom.checkOpNoThrow("android:get_usage_stats", android.os.Process.myUid(), c.getPackageName());
            }
            return mode == android.app.AppOpsManager.MODE_ALLOWED;
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean accessibility(Context c) {
        try {
            int enabled = Settings.Secure.getInt(c.getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED, 0);
            if (enabled != 1) return false;
            String list = Settings.Secure.getString(c.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (list == null) return false;
            TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
            splitter.setString(list);
            while (splitter.hasNext()) {
                String s = splitter.next();
                if (s != null && s.contains(c.getPackageName()) && s.contains("GuardService")) return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    public static boolean overlay(Context c) {
        try {
            return Settings.canDrawOverlays(c);
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean notifications(Context c) {
        try {
            android.app.NotificationManager nm = (android.app.NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            return nm != null && nm.areNotificationsEnabled();
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean battery(Context c) {
        try {
            PowerManager pm = (PowerManager) c.getSystemService(Context.POWER_SERVICE);
            return pm != null && pm.isIgnoringBatteryOptimizations(c.getPackageName());
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean notificationAccess(Context c) {
        try {
            android.app.NotificationManager nm =
                    (android.app.NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            return nm != null && nm.isNotificationListenerAccessGranted(
                    new ComponentName(c, MediaListenerService.class));
        } catch (Throwable t) {
            return false;
        }
    }

    public static void openNotificationAccess(Context c) {
        launch(c, new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
    }

    public static boolean postNotificationsGranted(Context c) {
        if (Build.VERSION.SDK_INT < 33) return true;
        return c.checkSelfPermission("android.permission.POST_NOTIFICATIONS") == PackageManager.PERMISSION_GRANTED;
    }

    public static void openUsage(Context c) {
        launch(c, new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
    }

    public static void openAccessibility(Context c) {
        launch(c, new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
    }

    public static void openOverlay(Context c) {
        launch(c, new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + c.getPackageName())));
    }

    public static void openNotifications(Context c) {
        if (Build.VERSION.SDK_INT >= 26) {
            Intent i = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, c.getPackageName());
            launch(c, i);
        } else {
            launch(c, new Intent(Settings.ACTION_SETTINGS));
        }
    }

    public static void openBattery(Context c) {
        try {
            Intent i = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + c.getPackageName()));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);
        } catch (Throwable t) {
            launch(c, new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
        }
    }

    public static void openAppDetails(Context c) {
        launch(c, new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + c.getPackageName())));
    }

    public static void openAutostart(Context c) {
        try {
            Intent i = new Intent();
            i.setComponent(new ComponentName("com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);
        } catch (Throwable t) {
            openAppDetails(c);
        }
    }

    private static void launch(Context c, Intent i) {
        try {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);
        } catch (Throwable ignored) {
        }
    }
}
