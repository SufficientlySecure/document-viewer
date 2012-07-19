package org.ebookdroid.common.notifications;

import org.ebookdroid.R;

import android.app.NotificationManager;
import android.content.Context;

import java.util.concurrent.atomic.AtomicInteger;

import org.emdev.BaseDroidApp;

abstract class AbstractNotificationManager implements INotificationManager {

    private NotificationManager manager;

    protected final AtomicInteger SEQ = new AtomicInteger();

    @Override
    public int notify(final int messageId) {
        final CharSequence title = BaseDroidApp.context.getText(R.string.app_name);
        final CharSequence message = BaseDroidApp.context.getText(messageId);
        return notify(title, message);
    }

    @Override
    public int notify(final int titleId, final int messageId) {
        final CharSequence title = BaseDroidApp.context.getText(titleId);
        final CharSequence message = BaseDroidApp.context.getText(messageId);
        return notify(title, message);
    }

    @Override
    public int notify(final int titleId, final CharSequence message) {
        final CharSequence title = BaseDroidApp.context.getText(titleId);
        return notify(title, message);
    }

    @Override
    public int notify(final CharSequence message) {
        return notify(BaseDroidApp.context.getText(R.string.app_name), message);
    }

    protected NotificationManager getManager() {
        if (manager == null) {
            manager = (NotificationManager) BaseDroidApp.context.getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return manager;
    }
}
