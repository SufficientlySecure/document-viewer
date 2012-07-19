package org.ebookdroid.common.notifications;

import org.ebookdroid.R;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.support.v4.app.NotificationCompat;
import android.util.SparseArray;

import java.util.concurrent.atomic.AtomicInteger;

import org.emdev.BaseDroidApp;

public class NotificationsManager {

    private static NotificationManager manager;

    private static final AtomicInteger SEQ = new AtomicInteger();

    private static final SparseArray<Notification> notifications = new SparseArray<Notification>();

    public static int createInfoNotification(String message) {
        return createInfoNotification(BaseDroidApp.context.getText(R.string.app_name).toString(), message);
    }

    public static int createInfoNotification(String title, String message) {
        NotificationCompat.Builder nb = new NotificationCompat.Builder(BaseDroidApp.context)
                .setSmallIcon(R.drawable.icon).setAutoCancel(true).setTicker(message).setContentText(message)
                .setWhen(System.currentTimeMillis()).setContentTitle(title).setDefaults(Notification.DEFAULT_ALL);

        Notification notification = nb.getNotification();
        int id = SEQ.getAndIncrement();
        getManager().notify(id, notification);
        notifications.put(id, notification);
        return id;
    }

    private static NotificationManager getManager() {
        if (manager == null) {
            manager = (NotificationManager) BaseDroidApp.context.getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return manager;
    }
}
