package org.ebookdroid.core.settings;

import org.ebookdroid.core.IDocumentViewController;
import org.ebookdroid.core.IViewerActivity;
import org.ebookdroid.core.events.CurrentPageListener;
import org.ebookdroid.core.models.DocumentModel;

import android.content.Context;
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

    public void zoomChanged(final float zoom) {
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

    public void applyAppSettings(final IViewerActivity base) {
        lock.writeLock().lock();
        try {
            appSettings = new AppSettings(base.getContext());
            applyAppSettingsChanges(base, null, appSettings);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void applyBookSettings(final IViewerActivity base) {
        lock.readLock().lock();
        try {
            applyBookSettingsChanges(base, null, bookSettings);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void onAppSettingsChanged(final IViewerActivity base) {
        lock.writeLock().lock();
        try {
            final AppSettings oldSettings = appSettings;
            appSettings = new AppSettings(base.getContext());

            applyAppSettingsChanges(base, oldSettings, appSettings);

            final BookSettings oldBS = bookSettings;
            if (oldBS != null) {
                bookSettings = new BookSettings(oldBS, appSettings);
                db.storeBookSettings(bookSettings);

                applyBookSettingsChanges(base, oldBS, bookSettings);
            } else {
                appSettings.clearPseudoBookSettings();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    protected void applyAppSettingsChanges(final IViewerActivity base, final AppSettings oldSettings,
            final AppSettings newSettings) {
        final AppSettings.Diff diff = new AppSettings.Diff(oldSettings, newSettings);

        if (diff.isRotationChanged()) {
            base.getActivity().setRequestedOrientation(newSettings.getRotation().getOrientation());
        }

        if (diff.isFullScreenChanged()) {
            final Window window = base.getActivity().getWindow();
            if (newSettings.getFullScreen()) {
                window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }
        }

        if (diff.isShowTitleChanged() && diff.isFirstTime()) {
            final Window window = base.getActivity().getWindow();
            if (!getAppSettings().getShowTitle()) {
                window.requestFeature(Window.FEATURE_NO_TITLE);
            } else {
                // Android 3.0+ you need both progress!!!
                window.requestFeature(Window.FEATURE_PROGRESS);
                window.requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
                base.getActivity().setProgressBarIndeterminate(true);
            }
        }
        final IDocumentViewController dc = base.getDocumentController();
        if (dc != null) {
            if (diff.isKeepScreenOnChanged()) {
                dc.getView().setKeepScreenOn(newSettings.isKeepScreenOn());
            }
        }        
    }

    protected void applyBookSettingsChanges(final IViewerActivity base, final BookSettings oldSettings,
            final BookSettings newSettings) {
        if (newSettings == null) {
            return;
        }
        final BookSettings.Diff diff = new BookSettings.Diff(oldSettings, newSettings);

        if (diff.isSinglePageChanged()) {
            base.createDocumentView();
        }

        if (diff.isZoomChanged() && diff.isFirstTime()) {
            base.getZoomModel().setZoom(newSettings.getZoom());
        }

        final IDocumentViewController dc = base.getDocumentController();
        if (dc != null) {

            if (diff.isPageAlignChanged()) {
                dc.setAlign(newSettings.getPageAlign());
            }

            if (diff.isAnimationTypeChanged()) {
                dc.updateAnimationType();
            }
            
        }

        final DocumentModel dm = base.getDocumentModel();
        currentPageChanged(dm.getCurrentDocPageIndex(), dm.getCurrentViewPageIndex());
    }
}
