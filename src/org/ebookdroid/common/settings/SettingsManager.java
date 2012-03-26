package org.ebookdroid.common.settings;

import org.ebookdroid.common.settings.books.BookSettings;
import org.ebookdroid.common.settings.books.DBSettingsManager;
import org.ebookdroid.core.PageIndex;

import android.content.Context;
import android.content.SharedPreferences.Editor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.emdev.utils.listeners.ListenerProxy;

public class SettingsManager {

    private static Context ctx;

    private static DBSettingsManager db;

    private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private static AppSettings appSettings;

    private static final Map<String, BookSettings> bookSettings = new HashMap<String, BookSettings>();

    private static BookSettings current;

    private static ListenerProxy listeners = new ListenerProxy(ISettingsChangeListener.class);

    public static void init(final Context context) {
        if (ctx == null) {
            ctx = context;
            db = new DBSettingsManager(context);
            appSettings = new AppSettings(context);
        }
    }

    public static BookSettings init(final String fileName) {
        lock.writeLock().lock();
        try {
            getAppSettings().clearPseudoBookSettings();
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
                bs = new BookSettings(fileName);
                getAppSettings().fillBookSettings(bs);
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

    public static void clearCurrentBookSettings() {
        lock.writeLock().lock();
        try {
            getAppSettings().clearPseudoBookSettings();
            storeBookSettings();
            replaceCurrentBookSettings(null);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public static void removeCurrentBookSettings() {
        lock.writeLock().lock();
        try {
            getAppSettings().clearPseudoBookSettings();
            if (current != null) {
                bookSettings.remove(current.fileName);
                db.delete(current);
            }
            current = null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public static void clearAllRecentBookSettings() {
        lock.writeLock().lock();
        try {
            db.clearRecent();
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

    public static void deleteAllBookmarks() {
        lock.writeLock().lock();
        try {
            db.deleteAllBookmarks();
            bookSettings.clear();
            if (current != null) {
                current.bookmarks.clear();
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
            bookSettings.putAll(books);
            replaceCurrentBookSettings(books.get(fileName));
            return books;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public static void updateSearchBookQuery(final String searchQuery) {
        lock.writeLock().lock();
        try {
            final Editor edit = appSettings.prefs.edit();
            AppPreferences.SEARCH_BOOK_QUERY.setPreferenceValue(edit, searchQuery);
            edit.commit();
            final AppSettings oldSettings = appSettings;
            appSettings = new AppSettings(ctx);
            applyAppSettingsChanges(oldSettings, appSettings);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public static void updateTapProfiles(final String profiles) {
        lock.writeLock().lock();
        try {
            final Editor edit = appSettings.prefs.edit();
            AppPreferences.TAP_PROFILES.setPreferenceValue(edit, profiles);
            edit.commit();
            final AppSettings oldSettings = appSettings;
            appSettings = new AppSettings(ctx);
            applyAppSettingsChanges(oldSettings, appSettings);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public static void changeAutoScanDirs(final String dir, final boolean add) {
        lock.writeLock().lock();
        try {
            final Set<String> dirs = new HashSet<String>(appSettings.autoScanDirs);
            if (add && dirs.add(dir) || dirs.remove(dir)) {
                final Editor edit = appSettings.prefs.edit();
                AppPreferences.AUTO_SCAN_DIRS.setPreferenceValue(edit, dirs);
                edit.commit();
                final AppSettings oldSettings = appSettings;
                appSettings = new AppSettings(ctx);
                applyAppSettingsChanges(oldSettings, appSettings);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public static void updateKeysBinding(final String json) {
        lock.writeLock().lock();
        try {
            final Editor edit = appSettings.prefs.edit();
            AppPreferences.KEY_BINDINGS.setPreferenceValue(edit, json);
            edit.commit();
            final AppSettings oldSettings = appSettings;
            appSettings = new AppSettings(ctx);
            applyAppSettingsChanges(oldSettings, appSettings);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public static void toggleNightMode() {
        lock.writeLock().lock();
        try {
            if (current != null) {
                BookSettings olds = new BookSettings(current);

                current.nightMode = !current.nightMode;
                db.storeBookSettings(current);
                appSettings.updatePseudoBookSettings(current);

                applyBookSettingsChanges(olds, current, null);
            }
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

    public static void zoomChanged(final float zoom, boolean committed) {
        lock.readLock().lock();
        try {
            if (current != null) {
                current.setZoom(zoom);
                if (committed) {
                    db.storeBookSettings(current);
                }
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    public static void positionChanged(final float offsetX, final float offsetY) {
        lock.readLock().lock();
        try {
            if (current != null) {
                current.offsetX = offsetX;
                current.offsetY = offsetY;
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
            onBookSettingsChanged(appDiff);

        } finally {
            lock.writeLock().unlock();
        }
    }

    public static void onBookSettingsChanged(final AppSettings.Diff appDiff) {
        lock.writeLock().lock();
        try {
            if (current != null) {
                final BookSettings oldBS = current;
                current = new BookSettings(oldBS);
                appSettings.fillBookSettings(current);
                db.storeBookSettings(current);
                db.updateBookmarks(current);
                applyBookSettingsChanges(oldBS, current, appDiff);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public static void storeBookSettings() {
        lock.readLock().lock();
        try {
            if (current != null) {
                db.storeBookSettings(current);
                db.updateBookmarks(current);
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    public static AppSettings.Diff applyAppSettingsChanges(final AppSettings oldSettings, final AppSettings newSettings) {
        final AppSettings.Diff diff = new AppSettings.Diff(oldSettings, newSettings);
        final ISettingsChangeListener l = listeners.getListener();
        l.onAppSettingsChanged(oldSettings, newSettings, diff);
        return diff;
    }

    public static void applyBookSettingsChanges(final BookSettings oldSettings, final BookSettings newSettings,
            final AppSettings.Diff appDiff) {
        final BookSettings.Diff diff = new BookSettings.Diff(oldSettings, newSettings);
        final ISettingsChangeListener l = listeners.getListener();
        l.onBookSettingsChanged(oldSettings, newSettings, diff, appDiff);
    }

    public static void addListener(final ISettingsChangeListener l) {
        listeners.addListener(l);
    }

    public static void removeListener(final ISettingsChangeListener l) {
        listeners.removeListener(l);
    }
}
