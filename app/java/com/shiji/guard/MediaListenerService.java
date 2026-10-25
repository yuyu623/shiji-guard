package com.shiji.guard;

import android.service.notification.NotificationListenerService;

/** 系统通知使用权入口；只用来读取 MediaSession，不读取通知内容。 */
public class MediaListenerService extends NotificationListenerService {

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        MediaTracker.start(this);
    }

    @Override
    public void onListenerDisconnected() {
        MediaTracker.stop();
        super.onListenerDisconnected();
    }

    @Override
    public void onDestroy() {
        MediaTracker.stop();
        super.onDestroy();
    }
}
