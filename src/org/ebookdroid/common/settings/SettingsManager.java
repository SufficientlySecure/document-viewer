package org.ebookdroid.common.settings;

import org.ebookdroid.common.log.LogContext;
import org.ebookdroid.common.settings.books.BookSettings;
import org.ebookdroid.common.settings.books.DBSettingsManager;
import org.ebookdroid.common.settings.listeners.IAppSettingsChangeListener;
import org.ebookdroid.common.settings.listeners.IBookSettingsChangeListener;
import org.ebookdroid.common.settings.listeners.ILibSettingsChangeListener;
import org.ebookdroid.common.settings.listeners.IOpdsSettingsChangeListener;
import org.ebookdroid.common.settings.listeners.IRecentBooksChangedListener;
import org.ebookdroid.common.settings.types.PageAlign;
import org.ebookdroid.core.PageIndex;
import org.ebookdroid.core.curl.PageAnimationType;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.emdev.utils.LengthUtils;
import org.emdev.utils.listeners.ListenerProxy;

public class SettingsManager {

    public static final LogContext LCTX = LogContext.ROOT.lctx("SettingsManager");

    static Context ctx;

    static SharedPreferences prefs;

    static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private static DBSettingsManager db;

    private static final Map<String, BookSettings> bookSettings = new HashMap<String, BookSettings>();

    private static BookSettings current;

    static ListenerProxy listeners = new ListenerProxy(IAppSettingsChangeListener.class,
            ILibSettingsChangeListener.class, IOpdsSettingsChangeListener.class, IBookSettingsChangeListener.class, IRecentBooksChangedListener.class);

    public static void init(final Context context) {
        if (ctx == null) {
            ctx = context;
            prefs = PreferenceManager.getDefaultSharedPreferences(context);
            db = new DBSettingsManager(context);

            AppSettings.init();
            LibSettings.init();
            OpdsSettings.init();
        }
    }

    public static BookSettings init(final String fileName, final Intent intent) {
        lock.writeLock().lock();
        try {
            AppSettings.clearPseudoBookSettings();

            boolean store = false;
            current = bookSettings.get(fileName);
            if (current == null) {
                current = db.getBookSettings(fileName);
                if (current == null) {
                    current = new BookSettings(fileName);
                    AppSettings.fillBookSettings(current);
                    store = true;
                }
                bookSettings.put(fileName, current);
            }

            if (intent != null) {
                current.persistent = Boolean.parseBoolean(LengthUtils.safeString(intent.getStringExtra("persistent"),
                        "true"));

                current.animationType = PageAnimationType.valueOf(LengthUtils.safeString(
                        intent.getStringExtra("animationType"), current.animationType.toString()));
                current.pageAlign = PageAlign.valueOf(LengthUtils.safeString(intent.getStringExtra("pageAlign"),
                        current.pageAlign.toString()));
                current.splitPages = Boolean.parseBoolean(LengthUtils.safeString(intent.getStringExtra("splitPages"),
                        Boolean.toString(current.splitPages)));
                current.cropPages = Boolean.parseBoolean(LengthUtils.safeString(intent.getStringExtra("cropPages"),
                        Boolean.toString(current.cropPages)));
                current.nightMode = Boolean.parseBoolean(LengthUtils.safeString(intent.getStringExtra("nightMode"),
                        Boolean.toString(current.nightMode)));

                final int pageIndex = Integer.parseInt(LengthUtils.safeString(intent.getStringExtra("pageIndex"),
                        Integer.toString(current.currentPage.viewIndex)));
                current.currentPage = new PageIndex(current.splitPages ? current.currentPage.docIndex : pageIndex,
                        pageIndex);

                store = current.persistent;
            }

            if (store) {
                db.storeBookSettings(current);
            }

            AppSettings.updatePseudoBookSettings(current);

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
            AppSettings.clearPseudoBookSettings();
            storeBookSettings();
            replaceCurrentBookSettings(null);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public static void removeCurrentBookSettings() {
        lock.writeLock().lock();
        try {
            AppSettings.clearPseudoBookSettings();
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

           if (current != null) {
                AppSettings.clearPseudoBookSettings();
                AppSettings.updatePseudoBookSettings(current);
            } else {
                AppSettings.clearPseudoBookSettings();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public static void removeBookFromRecents(String path) {
        lock.writeLock().lock();
        try {
            BookSettings bs = bookSettings.get(path);
           if (bs != null) {
               db.removeBookFromRecents(bs);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public static void deleteBookSettings(final BookSettings bs) {
        lock.writeLock().lock();
        try {
            db.delete(bs);
            bookSettings.remove(bs.fileName);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public static void deleteAllBookSettings() {
        lock.writeLock().lock();
        try {
            db.deleteAll();
            bookSettings.clear();

            if (current != null) {
                AppSettings.clearPseudoBookSettings();
                AppSettings.updatePseudoBookSettings(current);
            } else {
                AppSettings.clearPseudoBookSettings();
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

    public static void toggleNightMode() {
        lock.writeLock().lock();
        try {
            if (current != null) {
                final BookSettings olds = new BookSettings(current);
                current.nightMode = !current.nightMode;
                db.storeBookSettings(current);
                AppSettings.updatePseudoBookSettings(current);

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

    public static void zoomChanged(final float zoom, final boolean committed) {
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
            final AppSettings.Diff appDiff = AppSettings.onSettingsChanged();
            LibSettings.onSettingsChanged();
            OpdsSettings.onSettingsChanged();

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

                AppSettings.fillBookSettings(current);

                db.storeBookSettings(current);
                db.updateBookmarks(current);
                applyBookSettingsChanges(oldBS, current, appDiff);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public static BookSettings copyBookSettings(final File target, final BookSettings settings) {
        lock.writeLock().lock();
        try {
            final BookSettings bs = new BookSettings(target.getAbsolutePath(), settings);
            db.storeBookSettings(bs);
            db.updateBookmarks(bs);
            bookSettings.put(bs.fileName, bs);
            return bs;
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

    public static void applyBookSettingsChanges(final BookSettings oldSettings, final BookSettings newSettings,
            final AppSettings.Diff appDiff) {
        final BookSettings.Diff diff = new BookSettings.Diff(oldSettings, newSettings);
        final IBookSettingsChangeListener l = listeners.getListener();
        l.onBookSettingsChanged(oldSettings, newSettings, diff, appDiff);
    }

    public static void addListener(final Object l) {
        listeners.addListener(l);
    }

    public static void removeListener(final Object l) {
        listeners.removeListener(l);
    }

    public static void onRecentBooksChanged() {
        final IRecentBooksChangedListener l = listeners.getListener();
        l.onRecentBooksChanged();
    }
}
