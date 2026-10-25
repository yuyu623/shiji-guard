package com.shiji.guard;

import android.app.job.JobParameters;
import android.app.job.JobService;

public class FrameJobService extends JobService {

    public static final int JOB_ID = 4100;

    @Override
    public boolean onStartJob(JobParameters params) {
        if (App.get().prefs().trackerEnabled() && !TrackerService.isRunning()) {
            TrackerService.start(this);
        }
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }
}
