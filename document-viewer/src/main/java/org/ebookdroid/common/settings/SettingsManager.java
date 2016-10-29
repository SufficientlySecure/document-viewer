package org.ebookdroid.common.settings;

import org.ebookdroid.common.settings.books.BookSettings;
import org.ebookdroid.common.settings.books.DBSettingsManager;
import org.ebookdroid.common.settings.listeners.IAppSettingsChangeListener;
import org.ebookdroid.common.settings.listeners.IBackupSettingsChangeListener;
import org.ebookdroid.common.settings.listeners.IBookSettingsChangeListener;
import org.ebookdroid.common.settings.listeners.ILibSettingsChangeListener;
import org.ebookdroid.common.settings.listeners.IOpdsSettingsChangeListener;
import org.ebookdroid.common.settings.listeners.IRecentBooksChangedListener;
import org.ebookdroid.common.settings.types.DocumentViewMode;
import org.ebookdroid.common.settings.types.PageAlign;
import org.ebookdroid.common.settings.types.RotationType;
import org.ebookdroid.core.PageIndex;
import org.ebookdroid.core.curl.PageAnimationType;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.emdev.common.log.LogContext;
import org.emdev.common.log.LogManager;
import org.emdev.utils.FileUtils;
import org.emdev.utils.LengthUtils;
import org.emdev.utils.concurrent.Flag;
import org.emdev.utils.listeners.ListenerProxy;

public class SettingsManager {

    public static final LogContext LCTX = LogManager.root().lctx("SettingsManager", false);

    public static final int INITIAL_FONTS = 1 << 0;

    private static final String INITIAL_FLAGS = "initial_flags";

    static Context ctx;

    static SharedPreferences prefs;

    static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private static DBSettingsManager db;

    private static final Map<String, BookSettings> bookSettings = new HashMap<String, BookSettings>();

    static ListenerProxy listeners = new ListenerProxy(IAppSettingsChangeListener.class,
            IBackupSettingsChangeListener.class, ILibSettingsChangeListener.class, IOpdsSettingsChangeListener.class,
            IBookSettingsChangeListener.class, IRecentBooksChangedListener.class);

    private static BookSettingsUpdate updateThread;

    public static void init(final Context context) {
        if (ctx == null) {
            ctx = context;
            prefs = PreferenceManager.getDefaultSharedPreferences(context);
            db = new DBSettingsManager(context);

            AppSettings.init();
            BackupSettings.init();
            LibSettings.init();
            OpdsSettings.init();

            updateThread = new BookSettingsUpdate();
            updateThread.start();
        }
    }

    public static void onTerminate() {
        updateThread.run.clear();
        try {
            updateThread.join();
        } catch (final InterruptedException e) {
            Thread.interrupted();
        }
    }

    public static BookSettings create(final long ownerId, final String fileName, final boolean temporaryBook,
            final Intent intent) {
        lock.writeLock().lock();
        try {
            boolean created = false;
            BookSettings current = getBookSettingsImpl(fileName);
            if (current == null) {
                created = true;
                current = new BookSettings(fileName);
                AppSettings.setDefaultSettings(current);
                if (temporaryBook) {
                    current.persistent = false;
                }
            }
            bookSettings.put(current.fileName, current);

            if (intent != null) {
                current.persistent = Boolean.parseBoolean(LengthUtils.safeString(intent.getStringExtra("persistent"),
                        "true"));

                current.viewMode = DocumentViewMode.valueOf((LengthUtils.safeString(intent.getStringExtra("viewMode"),
                        current.viewMode.toString())));
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

                if (intent.hasExtra("pageIndex")) {
                    final int pageIndex = Integer.parseInt(LengthUtils.safeString(intent.getStringExtra("pageIndex"),
                            Integer.toString(current.currentPage.viewIndex)));
                    current.currentPage = new PageIndex(current.splitPages ? current.currentPage.docIndex : pageIndex,
                            pageIndex);

                    current.offsetX = Float.parseFloat(LengthUtils.safeString(intent.getStringExtra("offsetX"), "0"));
                    current.offsetY = Float.parseFloat(LengthUtils.safeString(intent.getStringExtra("offsetY"), "0"));
                }
            }

            if (created && current.persistent) {
                db.storeBookSettings(current);
            }

            return current;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public static boolean hasOpenedBooks() {
        lock.readLock().lock();
        try {
            return !bookSettings.isEmpty();
        } finally {
            lock.readLock().unlock();
        }
    }

    public static BookSettings getBookSettings(final String fileName) {
        lock.writeLock().lock();
        try {
            return getBookSettingsImpl(fileName);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private static BookSettings getBookSettingsImpl(final String fileName) {
        BookSettings bs = loadBookSettingsImpl(fileName);
        if (bs == null) {
            final String mpath = FileUtils.invertMountPrefix(fileName);
            final File f = mpath != null ? new File(mpath) : null;
            if (f != null && f.exists()) {
                bs = loadBookSettingsImpl(mpath);
            }
        }
        return bs;
    }

    private static BookSettings loadBookSettingsImpl(final String fileName) {
        BookSettings bs = bookSettings.get(fileName);
        if (bs == null) {
            bs = db.getBookSettings(fileName);
        }
        return bs;
    }

    public static void releaseBookSettings(final long ownerId, final BookSettings current) {
        if (current == null || !current.persistent) {
            return;
        }
        lock.writeLock().lock();
        try {
            storeBookSettings(current);
            bookSettings.remove(current.fileName);
        } finally {
            lock.writeLock().unlock();
        }
        onRecentBooksChanged();
    }

    public static void clearAllRecentBookSettings() {
        lock.writeLock().lock();
        try {
            db.clearRecent();
            for (final BookSettings current : bookSettings.values()) {
                current.persistent = false;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public static void removeBookFromRecents(final String path) {
        lock.writeLock().lock();
        try {
            final BookSettings bs = db.getBookSettings(path);
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
            bs.persistent = false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public static void deleteAllBookSettings() {
        lock.writeLock().lock();
        try {
            db.deleteAll();
            for (final BookSettings current : bookSettings.values()) {
                current.persistent = false;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public static void deleteAllBookmarks() {
        lock.writeLock().lock();
        try {
            db.deleteAllBookmarks();
            for (final BookSettings current : bookSettings.values()) {
                current.bookmarks.clear();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public static BookSettings getRecentBook() {
        lock.readLock().lock();
        try {
            final Map<String, BookSettings> books = db.getRecentBooks(false);
            return books.isEmpty() ? null : books.values().iterator().next();
        } finally {
            lock.readLock().unlock();
        }
    }

    public static Map<String, BookSettings> getRecentBooks() {
        lock.writeLock().lock();
        try {
            final Map<String, BookSettings> books = db.getRecentBooks(true);
            books.putAll(bookSettings);
            return books;
        } finally {
            lock.writeLock().unlock();
        }
    }

    // TODO: Factor out common code from these

    public static void setBookRotation(final BookSettings current, final RotationType mode) {
        if (current == null) {
            return;
        }
        lock.writeLock().lock();
        try {
            final BookSettings olds = new BookSettings(current);
            current.rotation = mode;
            current.lastChanged = System.currentTimeMillis();
            db.storeBookSettings(current);
            applyBookSettingsChanges(olds, current);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public static void toggleNightMode(final BookSettings current) {
        if (current == null) {
            return;
        }
        lock.writeLock().lock();
        try {
            final BookSettings olds = new BookSettings(current);
            current.nightMode = !current.nightMode;
            current.lastChanged = System.currentTimeMillis();
            db.storeBookSettings(current);
            applyBookSettingsChanges(olds, current);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public static void toggleSplitPages(final BookSettings current) {
        if (current == null) {
            return;
        }
        lock.writeLock().lock();
        try {
            final BookSettings olds = new BookSettings(current);
            current.splitPages = !current.splitPages;
            current.lastChanged = System.currentTimeMillis();
            db.storeBookSettings(current);
            applyBookSettingsChanges(olds, current);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public static void toggleCropPages(final BookSettings current) {
        if (current == null) {
            return;
        }
        lock.writeLock().lock();
        try {
            final BookSettings olds = new BookSettings(current);
            current.cropPages = !current.cropPages;
            current.lastChanged = System.currentTimeMillis();
            db.storeBookSettings(current);
            applyBookSettingsChanges(olds, current);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public static void toggleSinglePage(final BookSettings current) {
        if (current == null) {
            return;
        }
        lock.writeLock().lock();
        try {
            final BookSettings olds = new BookSettings(current);
            current.viewMode = (current.viewMode == DocumentViewMode.SINGLE_PAGE ? DocumentViewMode.VERTICALL_SCROLL : DocumentViewMode.SINGLE_PAGE);
            current.lastChanged = System.currentTimeMillis();
            db.storeBookSettings(current);
            applyBookSettingsChanges(olds, current);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public static void currentPageChanged(final BookSettings current, final PageIndex oldIndex, final PageIndex newIndex) {
        if (current == null) {
            return;
        }
        lock.writeLock().lock();
        try {
            current.currentPageChanged(oldIndex, newIndex);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public static void zoomChanged(final BookSettings current, final float zoom, final boolean committed) {
        if (current == null) {
            return;
        }
        lock.writeLock().lock();
        try {
            current.setZoom(zoom, committed);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public static void positionChanged(final BookSettings current, final float offsetX, final float offsetY) {
        if (current == null) {
            return;
        }
        lock.writeLock().lock();
        try {
            current.positionChanged(offsetX, offsetY);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public static void onSettingsChanged() {
        lock.writeLock().lock();
        try {
            AppSettings.onSettingsChanged();
            BackupSettings.onSettingsChanged();
            LibSettings.onSettingsChanged();
            OpdsSettings.onSettingsChanged();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public static void onBookSettingsActivityCreated(final BookSettings current) {
        if (current == null) {
            return;
        }
        AppSettings.updatePseudoBookSettings(current);
    }

    public static void onBookSettingsActivityClosed(final BookSettings current) {
        if (current == null) {
            return;
        }
        lock.writeLock().lock();
        try {
            final BookSettings oldBS = new BookSettings(current);
            AppSettings.fillBookSettings(current);
            if (current.persistent) {
                db.storeBookSettings(current);
            }
            applyBookSettingsChanges(oldBS, current);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public static BookSettings copyBookSettings(final File target, final BookSettings settings) {
        lock.writeLock().lock();
        try {
            final BookSettings bs = new BookSettings(target.getAbsolutePath(), settings);
            db.storeBookSettings(bs);
            return bs;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public static void storeBookSettings(final BookSettings current) {
        if (current == null) {
            return;
        }
        lock.readLock().lock();
        try {
            db.storeBookSettings(current);
        } finally {
            lock.readLock().unlock();
        }
    }

    public static void applyBookSettingsChanges(final BookSettings oldSettings, final BookSettings newSettings) {

        final BookSettings.Diff diff = new BookSettings.Diff(oldSettings, newSettings);
        final IBookSettingsChangeListener l = listeners.getListener();
        l.onBookSettingsChanged(oldSettings, newSettings, diff);
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

    public static boolean isInitialFlagsSet(final int flag) {
        if (prefs.contains(INITIAL_FLAGS)) {
            return (prefs.getInt(INITIAL_FLAGS, 0) & flag) == flag;
        }
        return false;
    }

    public static void setInitialFlags(final int flag) {
        final int old = prefs.getInt(INITIAL_FLAGS, 0);
        prefs.edit().putInt(INITIAL_FLAGS, old | flag).commit();
    }

    private static class BookSettingsUpdate extends Thread {

        private static final int INACTIVITY_PERIOD = 3000;

        final Flag run = new Flag(true);

        final List<BookSettings> list = new ArrayList<BookSettings>();

        @Override
        public void run() {
            LCTX.i("BookSettingsUpdate thread started");

            while (run.waitFor(TimeUnit.MILLISECONDS, INACTIVITY_PERIOD)) {
                boolean stored = false;
                lock.writeLock().lock();
                try {
                    list.clear();
                    final long now = run.get() ? System.currentTimeMillis() : 0;
                    for (final BookSettings current : bookSettings.values()) {
                        if (current.persistent && current.lastUpdated < current.lastChanged
                                && now - current.lastChanged >= INACTIVITY_PERIOD) {
                            list.add(current);
                        }
                    }
                    if (!list.isEmpty()) {
                        stored |= db.storeBookSettings(list);
                    }
                } catch (final Throwable th) {
                    LCTX.e("BookSettingsUpdate thread error: ", th);
                } finally {
                    lock.writeLock().unlock();
                }
                if (stored) {
                    onRecentBooksChanged();
                }
            }
            LCTX.i("BookSettingsUpdate thread finished");
        }
    }

}
