package org.ebookdroid.core.settings;

import org.ebookdroid.core.IDocumentViewController;
import org.ebookdroid.core.IViewerActivity;
import org.ebookdroid.core.events.CurrentPageListener;
import org.ebookdroid.core.models.DocumentModel;

import android.content.Context;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SettingsManager implements CurrentPageListener {

    private static SettingsManager instance;

    private final DBHelper db;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private AppSettings appSettings;

    private BookSettings bookSettings;

    private SettingsManager(final Context context) {
        db = new DBHelper(context);
        appSettings = new AppSettings(context);
    }

    public static SettingsManager getInstance(final Context context) {
        if (instance == null) {
            instance = new SettingsManager(context);
        }
        return instance;
    }

    public BookSettings init(final String fileName) {
        lock.writeLock().lock();
        try {
            BookSettings bs = db.getBookSettings(fileName);
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

    public void clearCurrentBookSettings() {
        lock.writeLock().lock();
        try {
            getAppSettings().clearPseudoBookSettings();
            bookSettings = null;
            Log.d("SettingsManager", "", new Exception("Book settings cleared"));
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void deleteAllBookSettings() {
        lock.writeLock().lock();
        try {
            db.deleteAll();
            final BookSettings oldBS = getBookSettings();
            final AppSettings apps = getAppSettings();
            if (oldBS != null) {
                apps.clearPseudoBookSettings();
                final BookSettings newBS = new BookSettings(oldBS.getFileName(), apps);
                apps.updatePseudoBookSettings(newBS);
                bookSettings = newBS;
            } else {
                apps.clearPseudoBookSettings();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public AppSettings getAppSettings() {
        lock.readLock().lock();
        try {
            return appSettings;
        } finally {
            lock.readLock().unlock();
        }
    }

    public BookSettings getBookSettings() {
        lock.readLock().lock();
        try {
            if (bookSettings == null) {
                Log.e("SettingsManager", "", new Exception("No book settings defined"));
            }
            return bookSettings;
        } finally {
            lock.readLock().unlock();
        }
    }

    public Map<String, BookSettings> getAllBooksSettings() {
        return db.getBookSettings();
    }

    @Override
    public void currentPageChanged(final int docPageIndex, final int viewPageIndex) {
        lock.readLock().lock();
        try {
            bookSettings.currentPageChanged(docPageIndex, viewPageIndex);
            db.storeBookSettings(bookSettings);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void onAppSettingsChanged(final IViewerActivity base) {
        lock.writeLock().lock();
        try {
            final AppSettings oldSettings = appSettings;
            appSettings = new AppSettings(base.getContext());

            setUseAnimation(base, oldSettings, appSettings);
            setOrientation(base, oldSettings, appSettings);
            setFullScreen(base, oldSettings, appSettings);

            final BookSettings oldBS = bookSettings;
            if (oldBS != null) {
                bookSettings = new BookSettings(oldBS.getFileName(), null);
                appSettings.fillBookSettings(bookSettings);
                db.storeBookSettings(bookSettings);

                setDocumentView(base, oldBS, bookSettings);
                setAlign(base, oldBS, bookSettings);

                final DocumentModel dm = base.getDocumentModel();
                currentPageChanged(dm.getCurrentDocPageIndex(), dm.getCurrentViewPageIndex());
            } else {
                appSettings.clearPseudoBookSettings();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    protected void setUseAnimation(final IViewerActivity base, final AppSettings oldSettings,
            final AppSettings newSettings) {
        final IDocumentViewController dc = base.getDocumentController();
        if (dc == null) {
            return;
        }
        if (oldSettings.getAnimationType() != newSettings.getAnimationType()) {
            dc.updateAnimationType();
        }
    }

    protected void setDocumentView(final IViewerActivity base, final BookSettings oldSettings,
            final BookSettings newSettings) {
        if (oldSettings.getSinglePage() != newSettings.getSinglePage()) {
            base.createDocumentView();
        }
    }

    protected void setAlign(final IViewerActivity base, final BookSettings oldSettings, final BookSettings newSettings) {
        final IDocumentViewController dc = base.getDocumentController();
        if (dc == null) {
            return;
        }
        if (oldSettings.getPageAlign() != newSettings.getPageAlign()) {
            dc.setAlign(newSettings.getPageAlign());
        }
    }

    protected void setOrientation(final IViewerActivity base, final AppSettings oldSettings,
            final AppSettings newSettings) {
        if (oldSettings.getRotation() != newSettings.getRotation()) {
            base.getActivity().setRequestedOrientation(newSettings.getRotation().getOrientation());
        }
    }

    protected void setFullScreen(final IViewerActivity base, final AppSettings oldSettings,
            final AppSettings newSettings) {
        if (oldSettings.getFullScreen() != newSettings.getFullScreen()) {
            final Window window = base.getActivity().getWindow();
            if (newSettings.getFullScreen()) {
                window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }
        }
    }

}
