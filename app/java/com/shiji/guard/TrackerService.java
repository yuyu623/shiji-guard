package com.shiji.guard;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

public class TrackerService extends Service {

    public static final String ACTION_STOP = "com.shiji.guard.STOP_TRACKING";
    public static final String ACTION_RESTART = "com.shiji.guard.RESTART";
    private static final String CHANNEL = "tracker";
    private static final int NOTIF_ID = 4101;
    private static volatile boolean running;

    private Handler handler;
    private final Runnable refresh = new Runnable() {
        @Override
        public void run() {
            updateNotification();
            if (handler != null) handler.postDelayed(this, 30000);
        }
    };

    public static boolean isRunning() {
        return running;
    }

    public static void start(Context c) {
        try {
            Intent i = new Intent(c, TrackerService.class);
            if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i);
            else c.startService(i);
        } catch (Throwable ignored) {
        }
    }

    public static void stop(Context c) {
        try {
            c.stopService(new Intent(c, TrackerService.class));
        } catch (Throwable ignored) {
        }
    }

    public static void scheduleRestart(Context c) {
        try {
            AlarmManager am = (AlarmManager) c.getSystemService(ALARM_SERVICE);
            if (am == null) return;
            Intent i = new Intent(c, BootReceiver.class).setAction(ACTION_RESTART);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
            PendingIntent pi = PendingIntent.getBroadcast(c, 41, i, flags);
            am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    android.os.SystemClock.elapsedRealtime() + 5000, pi);
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
        try {
            startForeground(NOTIF_ID, buildNotification());
        } catch (Throwable t) {
            stopSelf();
            return;
        }
        running = true;
        Engine.acquire(this);
        MediaTracker.start(this);
        handler = new Handler(Looper.getMainLooper());
        handler.post(refresh);
        App.get().repo().prune();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            App.get().prefs().setTrackerEnabled(false);
            stopSelf();
            return START_NOT_STICKY;
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        running = false;
        if (handler != null) handler.removeCallbacks(refresh);
        Engine.release();
        super.onDestroy();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        scheduleRestart(this);
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createChannel() {
        try {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null && nm.getNotificationChannel(CHANNEL) == null) {
                NotificationChannel ch = new NotificationChannel(CHANNEL, getString(R.string.notif_channel), NotificationManager.IMPORTANCE_MIN);
                ch.setShowBadge(false);
                ch.setSound(null, null);
                ch.enableVibration(false);
                nm.createNotificationChannel(ch);
            }
        } catch (Throwable ignored) {
        }
    }

    private Notification buildNotification() {
        try {
            Intent i = new Intent(this, MainActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
            PendingIntent pi = PendingIntent.getActivity(this, 0, i, flags);
            Notification.Builder b = new Notification.Builder(this, CHANNEL)
                    .setSmallIcon(R.drawable.ic_stat)
                    .setContentTitle("时迹 · 正在记录")
                    .setContentText(notificationText())
                    .setContentIntent(pi)
                    .setOngoing(true)
                    .setShowWhen(false)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .setPriority(Notification.PRIORITY_MIN);
            return b.build();
        } catch (Throwable t) {
            Notification.Builder b = new Notification.Builder(this)
                    .setSmallIcon(R.drawable.ic_stat)
                    .setContentTitle("时迹");
            return b.build();
        }
    }

    private String notificationText() {
        String cur = Engine.currentPkg();
        long media = MediaTracker.totalToday();
        String base = "今日 " + TimeUtil.fmtDuration(Usage.totalTodayAll());
        if (media > 0) base += "（后台 " + TimeUtil.fmtDuration(media) + "）";
        if (cur == null || cur.isEmpty() || cur.equals(getPackageName())) return base;
        return base + " · " + Apps.name(this, cur);
    }

    private void updateNotification() {
        try {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null) nm.notify(NOTIF_ID, buildNotification());
        } catch (Throwable ignored) {
        }
    }
}
