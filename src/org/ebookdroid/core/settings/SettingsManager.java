package org.ebookdroid.core.settings;

import org.ebookdroid.core.IDocumentViewController;
import org.ebookdroid.core.IViewerActivity;
import org.ebookdroid.core.events.CurrentPageListener;
import org.ebookdroid.core.models.DocumentModel;

import android.content.Context;
import android.view.Window;
import android.view.WindowManager;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class SettingsManager implements CurrentPageListener {

    private static SettingsManager instance;

    private final DBHelper db;

    private final AtomicReference<AppSettings> appSettings = new AtomicReference<AppSettings>();

    private final AtomicReference<BookSettings> bookSettings = new AtomicReference<BookSettings>();

    private SettingsManager(final Context context) {
        db = new DBHelper(context);
        appSettings.set(new AppSettings(context));
    }

    public static SettingsManager getInstance(Context context) {
        if (instance == null) {
            instance = new SettingsManager(context);
        }
        return instance;
    }

    public BookSettings init(final String fileName) {
        BookSettings bs = db.getBookSettings(fileName);
        if (bs == null) {
            bs = new BookSettings(fileName, getAppSettings());
            db.storeBookSettings(bs);
        }

        bookSettings.set(bs);
        getAppSettings().updateBookSettings(bs);

        return bs;
    }

    public void clearBookSettings() {
        getAppSettings().clearBookSettings();
        bookSettings.set(null);
    }

    public void clearAllBookSettings() {
        getAppSettings().clearBookSettings();
        db.deleteAll();
        BookSettings oldBS = getBookSettings();
        if (oldBS != null) {
            BookSettings newBS = new BookSettings(oldBS.getFileName(), getAppSettings());
            getAppSettings().updateBookSettings(newBS);
            bookSettings.set(newBS);
        }
    }

    public AppSettings getAppSettings() {
        return appSettings.get();
    }

    public BookSettings getBookSettings() {
        return bookSettings.get();
    }

    public Map<String, BookSettings> getAllBooksSettings() {
        return db.getBookSettings();
    }

    @Override
    public void currentPageChanged(final int docPageIndex, final int viewPageIndex) {
        final BookSettings bs = getBookSettings();
        bs.currentPageChanged(docPageIndex, viewPageIndex);
        db.storeBookSettings(bs);
    }

    public void onAppSettingsChanged(IViewerActivity base) {
        final AppSettings newSettings = new AppSettings(base.getContext());
        final AppSettings oldSettings = appSettings.getAndSet(newSettings);

        setUseAnimation(base, oldSettings, newSettings);
        setOrientation(base, oldSettings, newSettings);
        setFullScreen(base, oldSettings, newSettings);

        BookSettings oldBS = getBookSettings();
        if (oldBS != null) {
            final BookSettings newBS = new BookSettings(oldBS.getFileName(), null);
            newSettings.fillBookSettings(newBS);
            bookSettings.getAndSet(newBS);
            db.storeBookSettings(newBS);

            setDocumentView(base, oldBS, newBS);
            setAlign(base, oldBS, newBS);

            final DocumentModel dm = base.getDocumentModel();
            currentPageChanged(dm.getCurrentDocPageIndex(), dm.getCurrentViewPageIndex());
        }
    }

    /**
     * Checks current type of document view and recreates if needed
     */
    private void setUseAnimation(IViewerActivity base, final AppSettings oldSettings, final AppSettings newSettings) {
        final IDocumentViewController dc = base.getDocumentController();
        if (dc == null) {
            return;
        }
        if (oldSettings.getUseAnimation() != newSettings.getUseAnimation()) {
            dc.updateUseAnimation();
        }
    }

    private void setDocumentView(IViewerActivity base, final BookSettings oldSettings, final BookSettings newSettings) {
        if (oldSettings.getSinglePage() != newSettings.getSinglePage()) {
            base.createDocumentView();
        }
    }

    protected void setAlign(IViewerActivity base, final BookSettings oldSettings, final BookSettings newSettings) {
        final IDocumentViewController dc = base.getDocumentController();
        if (dc == null) {
            return;
        }
        if (oldSettings.getPageAlign() != newSettings.getPageAlign()) {
            dc.setAlign(newSettings.getPageAlign());
        }
    }

    protected void setOrientation(IViewerActivity base, final AppSettings oldSettings, final AppSettings newSettings) {
        if (oldSettings.getRotation() != newSettings.getRotation()) {
            base.getActivity().setRequestedOrientation(newSettings.getRotation().getOrientation());
        }
    }

    protected void setFullScreen(IViewerActivity base, final AppSettings oldSettings, final AppSettings newSettings) {
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
