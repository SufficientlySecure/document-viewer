package org.ebookdroid.core.settings;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SettingsManager {

    private static Context ctx;

    private static DBHelper db;

    private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private static AppSettings appSettings;

    private static BookSettings bookSettings;

    private static List<ISettingsChangeListener> listeners = new ArrayList<ISettingsChangeListener>();

    public static void init(final Context context) {
        if (ctx == null) {
            ctx = context;
            db = new DBHelper(context);
            appSettings = new AppSettings(context);
        }
    }

    public static BookSettings init(final String fileName) {
        lock.writeLock().lock();
        try {
            BookSettings bs = getBookSettings(fileName);
            if (bs == null) {
                bs = new BookSettings(fileName, getAppSettings());
                db.storeBookSettings(bs);
            }
            bookSettings = bs;
            getAppSettings().updatePseudoBookSettings(bs);

            return bs;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public static BookSettings getBookSettings(final String fileName) {
        return db.getBookSettings(fileName);
    }

    public static void clearCurrentBookSettings() {
        lock.writeLock().lock();
        try {
            getAppSettings().clearPseudoBookSettings();
            bookSettings = null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public static void deleteAllBookSettings() {
        lock.writeLock().lock();
        try {
            db.deleteAll();
            final BookSettings oldBS = getBookSettings();
            final AppSettings apps = getAppSettings();
            if (oldBS != null) {
                apps.clearPseudoBookSettings();
                final BookSettings newBS = new BookSettings(oldBS, apps);
                apps.updatePseudoBookSettings(newBS);
                bookSettings = newBS;
            } else {
                apps.clearPseudoBookSettings();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public static AppSettings getAppSettings() {
        lock.readLock().lock();
        try {
            return appSettings;
        } finally {
            lock.readLock().unlock();
        }
    }

    public static BookSettings getBookSettings() {
        lock.readLock().lock();
        try {
            return bookSettings;
        } finally {
            lock.readLock().unlock();
        }
    }

    public static BookSettings getRecentBook() {
        final Map<String, BookSettings> bs = db.getBookSettings(false);
        return bs.isEmpty() ? null : bs.values().iterator().next();
    }

    public static Map<String, BookSettings> getAllBooksSettings() {
        return db.getBookSettings(true);
    }

    public static void currentPageChanged(final int docPageIndex, final int viewPageIndex) {
        lock.readLock().lock();
        try {
            bookSettings.currentPageChanged(docPageIndex, viewPageIndex);
            db.storeBookSettings(bookSettings);
        } finally {
            lock.readLock().unlock();
        }
    }

    public static void zoomChanged(final float zoom) {
        lock.readLock().lock();
        try {
            if (bookSettings != null) {
                bookSettings.setZoom(zoom);
                db.storeBookSettings(bookSettings);
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    public static void onSettingsChanged() {
        lock.writeLock().lock();
        try {
            final AppSettings oldSettings = appSettings;
            appSettings = new AppSettings(ctx);

            applyAppSettingsChanges(oldSettings, appSettings);

            final BookSettings oldBS = bookSettings;
            if (oldBS != null) {
                bookSettings = new BookSettings(oldBS, appSettings);
                db.storeBookSettings(bookSettings);

                applyBookSettingsChanges(oldBS, bookSettings);

            } else {
                appSettings.clearPseudoBookSettings();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public static void applyAppSettingsChanges(final AppSettings oldSettings, final AppSettings newSettings) {
        final AppSettings.Diff diff = new AppSettings.Diff(oldSettings, newSettings);
        for (ISettingsChangeListener l : listeners) {
            l.onAppSettingsChanged(oldSettings, newSettings, diff);
        }
    }

    public static void applyBookSettingsChanges(final BookSettings oldSettings, final BookSettings newSettings) {
        if (newSettings == null) {
            return;
        }
        final BookSettings.Diff diff = new BookSettings.Diff(oldSettings, newSettings);
        for (ISettingsChangeListener l : listeners) {
            l.onBookSettingsChanged(oldSettings, newSettings, diff);
        }

    }

    public static void addListener(ISettingsChangeListener l) {
        listeners.add(l);
    }

    public static void removeListener(ISettingsChangeListener l) {
        listeners.remove(l);
    }

}
