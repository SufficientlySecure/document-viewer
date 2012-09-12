package org.ebookdroid.common.notifications;

import org.ebookdroid.R;

import android.annotation.TargetApi;
import android.app.Notification;
import android.content.Intent;

@TargetApi(3)
class OldestNotificationManager extends AbstractNotificationManager {

    @Override
    public int notify(final CharSequence title, final CharSequence message, final Intent intent) {
        final String text = title + ": " + message;

        final Notification notification = new Notification(R.drawable.icon, text, System.currentTimeMillis());
        final int id = SEQ.getAndIncrement();

        getManager().notify(id, notification);
        return id;
    }
}
