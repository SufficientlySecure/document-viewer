package org.ebookdroid.common.notifications;

import org.ebookdroid.R;

import android.annotation.TargetApi;
import android.app.Notification;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import org.emdev.BaseDroidApp;

@TargetApi(4)
class CompatibilityNotificationManager extends AbstractNotificationManager {

    @Override
    public int notify(final CharSequence title, final CharSequence message, final Intent intent) {
        try {
            final NotificationCompat.Builder nb = new NotificationCompat.Builder(BaseDroidApp.context);

            nb.setSmallIcon(R.drawable.application_icon);
            nb.setAutoCancel(true);
            nb.setWhen(System.currentTimeMillis());
            nb.setDefaults(Notification.DEFAULT_ALL & (~Notification.DEFAULT_VIBRATE));

            nb.setContentIntent(getIntent(intent));

            nb.setContentTitle(title);
            nb.setTicker(message);
            nb.setContentText(message);

            final Notification notification = nb.getNotification();
            final int id = SEQ.getAndIncrement();
            getManager().notify(id, notification);

            return id;
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return 0;
    }
}
