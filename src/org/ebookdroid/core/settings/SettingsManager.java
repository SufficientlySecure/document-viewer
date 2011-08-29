package org.ebookdroid.core.settings;

import org.ebookdroid.core.PageIndex;

import android.content.Context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SettingsManager {

    private static Context ctx;

    private static DBHelper db;

    private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private static AppSettings appSettings;

    private static final Map<String, BookSettings> bookSettings = new HashMap<String, BookSettings>();

    private static BookSettings current;

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
            current = getBookSettingsImpl(fileName, true);
            getAppSettings().updatePseudoBookSettings(current);

            return current;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public static BookSettings getBookSettings(final String fileName) {
        lock.writeLock().lock();
        try {
            BookSettings bs = bookSettings.get(fileName);
            if (bs == null) {
                bs = db.getBookSettings(fileName);
            }
            if (current == null) {
                current = bs;
            }
            return bs;
        } finally {
            lock.writeLock().unlock();
        }
    }

    private static BookSettings getBookSettingsImpl(final String fileName, final boolean createOnDemand) {
        BookSettings bs = bookSettings.get(fileName);
        if (bs == null) {
            bs = db.getBookSettings(fileName);
            if (bs == null) {
                bs = new BookSettings(fileName, getAppSettings());
                db.storeBookSettings(bs);
            }
            bookSettings.put(fileName, bs);
        }
        return bs;
    }

    private static void replaceCurrentBookSettings(final BookSettings newBS) {
        if (current != null) {
            bookSettings.remove(current.fileName);
        }
        current = newBS;
        if (current != null) {
            bookSettings.put(current.fileName, current);
        }
    }

    public static BookSettingsEditor edit(final BookSettings bs) {
        return new BookSettingsEditor(bs);
    }

    public static void clearCurrentBookSettings() {
        lock.writeLock().lock();
        try {
            getAppSettings().clearPseudoBookSettings();
            replaceCurrentBookSettings(null);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public static void deleteAllBookSettings() {
        lock.writeLock().lock();
        try {
            db.deleteAll();
            bookSettings.clear();

            final AppSettings apps = getAppSettings();
            if (current != null) {
                apps.clearPseudoBookSettings();
                apps.updatePseudoBookSettings(current);
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
            return current;
        } finally {
            lock.readLock().unlock();
        }
    }

    public static BookSettings getRecentBook() {
        lock.readLock().lock();
        try {
            if (current != null) {
                return current;
            }
            final Map<String, BookSettings> books = db.getBookSettings(false);
            final BookSettings bs = books.isEmpty() ? null : books.values().iterator().next();
            if (bs != null) {
                bookSettings.put(bs.fileName, bs);
            }
            return bs;
        } finally {
            lock.readLock().unlock();
        }
    }

    public static Map<String, BookSettings> getAllBooksSettings() {
        lock.writeLock().lock();
        try {
            final String fileName = current != null ? current.fileName : null;
            final Map<String, BookSettings> books = db.getBookSettings(true);
            bookSettings.clear();
            books.putAll(books);
            replaceCurrentBookSettings(books.get(fileName));
            return books;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public static void currentPageChanged(final PageIndex oldIndex, final PageIndex newIndex) {
        lock.readLock().lock();
        try {
            if (current != null) {
                current.currentPageChanged(oldIndex, newIndex);
                db.storeBookSettings(current);
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    public static void zoomChanged(final float zoom) {
        lock.readLock().lock();
        try {
            if (current != null) {
                current.setZoom(zoom);
                db.storeBookSettings(current);
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

            final AppSettings.Diff appDiff = applyAppSettingsChanges(oldSettings, appSettings);
            onBookSettingsChanged(current, appDiff);

        } finally {
            lock.writeLock().unlock();
        }
    }

    static void onBookSettingsChanged(final BookSettings bs, final AppSettings.Diff appDiff) {
        lock.writeLock().lock();
        try {
            if (current != null) {
                if (current == bs) {
                    final BookSettings oldBS = new BookSettings(current);
                    appSettings.fillBookSettings(current);
                    db.storeBookSettings(current);

                    applyBookSettingsChanges(oldBS, current, appDiff);
                } else {
                    appSettings.fillBookSettings(current);
                    db.storeBookSettings(current);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }

    }

    public static AppSettings.Diff applyAppSettingsChanges(final AppSettings oldSettings, final AppSettings newSettings) {
        final AppSettings.Diff diff = new AppSettings.Diff(oldSettings, newSettings);
        for (final ISettingsChangeListener l : listeners) {
            l.onAppSettingsChanged(oldSettings, newSettings, diff);
        }
        return diff;
    }

    public static void applyBookSettingsChanges(final BookSettings oldSettings, final BookSettings newSettings,
            final AppSettings.Diff appDiff) {
        if (newSettings == null) {
            return;
        }
        final BookSettings.Diff diff = new BookSettings.Diff(oldSettings, newSettings);
        for (final ISettingsChangeListener l : listeners) {
            l.onBookSettingsChanged(oldSettings, newSettings, diff, appDiff);
        }

    }

    public static void addListener(final ISettingsChangeListener l) {
        listeners.add(l);
    }

    public static void removeListener(final ISettingsChangeListener l) {
        listeners.remove(l);
    }

    public static class BookSettingsEditor {

        final BookSettings bookSettings;

        BookSettingsEditor(final BookSettings bs) {
            this.bookSettings = bs;
            if (bookSettings != null) {
                getAppSettings().updatePseudoBookSettings(bookSettings);
            }
        }

        public void commit() {
            if (bookSettings != null) {
                onBookSettingsChanged(bookSettings, null);
            }
        }

        public void rollback() {
            getAppSettings().clearPseudoBookSettings();
        }
    }
}
