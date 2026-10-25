package com.shiji.guard;

import android.app.Application;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class App extends Application {

    private static App sInstance;
    private Repo repo;
    private Prefs prefs;
    private final ExecutorService io = Executors.newFixedThreadPool(2);
    private final Handler main = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        prefs = new Prefs(this);
        repo = new Repo(this);
        Ui.applyTheme(this);
        scheduleKeepAliveJob();
    }

    public static App get() {
        return sInstance;
    }

    public Repo repo() {
        return repo;
    }

    public Prefs prefs() {
        return prefs;
    }

    public void io(Runnable r) {
        io.execute(r);
    }

    public void ui(Runnable r) {
        main.post(r);
    }

    private void scheduleKeepAliveJob() {
        try {
            JobScheduler js = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
            if (js == null || js.getPendingJob(FrameJobService.JOB_ID) != null) return;
            JobInfo job = new JobInfo.Builder(FrameJobService.JOB_ID, new ComponentName(this, FrameJobService.class))
                    .setPeriodic(15 * 60 * 1000L, 5 * 60 * 1000L)
                    .setPersisted(true)
                    .build();
            js.schedule(job);
        } catch (Throwable ignored) {
        }
    }
}
