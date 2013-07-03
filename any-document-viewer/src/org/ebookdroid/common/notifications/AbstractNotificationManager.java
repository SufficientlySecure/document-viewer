package org.ebookdroid.common.notifications;

import org.ebookdroid.EBookDroidApp;
import org.ebookdroid.R;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.concurrent.atomic.AtomicInteger;

import org.emdev.BaseDroidApp;

abstract class AbstractNotificationManager implements INotificationManager {

    private NotificationManager manager;

    protected final AtomicInteger SEQ = new AtomicInteger();

    @Override
    public int notify(final int messageId) {
        final CharSequence title = BaseDroidApp.context.getText(R.string.app_name);
        final CharSequence message = BaseDroidApp.context.getText(messageId);
        return notify(title, message, null);
    }

    @Override
    public int notify(final int titleId, final int messageId) {
        final CharSequence title = BaseDroidApp.context.getText(titleId);
        final CharSequence message = BaseDroidApp.context.getText(messageId);
        return notify(title, message, null);
    }

    @Override
    public int notify(final int titleId, final CharSequence message, final Intent intent) {
        final CharSequence title = BaseDroidApp.context.getText(titleId);
        return notify(title, message, intent);
    }

    @Override
    public int notify(final CharSequence message) {
        return notify(BaseDroidApp.context.getText(R.string.app_name), message, null);
    }

    protected PendingIntent getIntent(final Intent intent) {
        return intent != null ? PendingIntent.getActivity(EBookDroidApp.context, 0, intent, 0) : getDefaultIntent();
    }

    protected PendingIntent getDefaultIntent() {
        return PendingIntent.getActivity(BaseDroidApp.context, 0, new Intent(), 0);
    }

    protected NotificationManager getManager() {
        if (manager == null) {
            manager = (NotificationManager) BaseDroidApp.context.getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return manager;
    }
}
