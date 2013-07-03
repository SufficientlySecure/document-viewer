package org.ebookdroid.common.notifications;

import android.content.Intent;

import org.emdev.common.android.AndroidVersion;

public interface INotificationManager {

    INotificationManager instance =
    /* CompatibilityNotificationManager for Android 1.6 - 2.x */
    AndroidVersion.lessThan3x ? new CompatibilityNotificationManager() :
    /* ModernNotificationManager for Android 3.0+ */
    new ModernNotificationManager();

    int notify(final CharSequence title, final CharSequence message, final Intent intent);

    int notify(final CharSequence message);

    int notify(final int titleId, final CharSequence message, final Intent intent);

    int notify(final int titleId, final int messageId);

    int notify(final int messageId);

}
