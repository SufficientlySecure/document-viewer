package org.ebookdroid.ui.viewer;

import org.ebookdroid.CodecType;
import org.ebookdroid.EBookDroidApp;
import org.emdev.ui.uimanager.UIManagerAppCompat;
import org.sufficientlysecure.viewer.R;
import org.ebookdroid.common.bitmaps.BitmapManager;
import org.ebookdroid.common.bitmaps.ByteBufferManager;
import org.ebookdroid.common.cache.CacheManager;
import org.ebookdroid.common.keysbinding.KeyBindingsDialog;
import org.ebookdroid.common.keysbinding.KeyBindingsManager;
import org.ebookdroid.common.settings.AppSettings;
import org.ebookdroid.common.settings.BackupSettings;
import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.common.settings.books.BookSettings;
import org.ebookdroid.common.settings.books.Bookmark;
import org.ebookdroid.common.settings.listeners.IAppSettingsChangeListener;
import org.ebookdroid.common.settings.listeners.IBookSettingsChangeListener;
import org.ebookdroid.common.settings.types.BookRotationType;
import org.ebookdroid.common.settings.types.DocumentViewMode;
import org.ebookdroid.common.touch.TouchManager;
import org.ebookdroid.core.DecodeService;
import org.ebookdroid.core.NavigationHistory;
import org.ebookdroid.core.Page;
import org.ebookdroid.core.PageIndex;
import org.ebookdroid.core.ViewState;
import org.ebookdroid.core.codec.OutlineLink;
import org.ebookdroid.core.events.CurrentPageListener;
import org.ebookdroid.core.events.DecodingProgressListener;
import org.ebookdroid.core.models.DecodingProgressModel;
import org.ebookdroid.core.models.DocumentModel;
import org.ebookdroid.core.models.SearchModel;
import org.ebookdroid.core.models.ZoomModel;
import org.ebookdroid.droids.mupdf.codec.exceptions.MuPdfPasswordException;
import org.ebookdroid.ui.library.dialogs.FolderDlg;
import org.ebookdroid.ui.settings.SettingsUI;
import org.ebookdroid.ui.viewer.dialogs.GoToPageDialog;
import org.ebookdroid.ui.viewer.dialogs.OutlineDialog;
import org.ebookdroid.ui.viewer.stubs.ActivityControllerStub;
import org.ebookdroid.ui.viewer.stubs.ViewContollerStub;
import org.ebookdroid.ui.viewer.views.ManualCropView;
import org.ebookdroid.ui.viewer.views.SearchControls;
import org.ebookdroid.ui.viewer.views.ViewEffects;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.graphics.PointF;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.AnyThread;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.emdev.BaseDroidApp;
import org.emdev.common.android.AndroidVersion;
import org.emdev.common.backup.BackupManager;
import org.emdev.common.content.ContentScheme;
import org.emdev.common.filesystem.PathFromUri;
import org.emdev.common.log.LogManager;
import org.emdev.ui.AbstractActivityController;
import org.emdev.ui.actions.ActionDialogBuilder;
import org.emdev.ui.actions.ActionEx;
import org.emdev.ui.actions.ActionMethod;
import org.emdev.ui.actions.IActionController;
import org.emdev.ui.actions.params.EditableValue;
import org.emdev.ui.actions.params.EditableValue.PasswordEditable;
import org.emdev.ui.progress.IProgressIndicator;
import org.emdev.ui.tasks.AsyncTask;
import org.emdev.ui.tasks.AsyncTaskExecutor;
import org.emdev.ui.tasks.BaseAsyncTask;
import org.emdev.ui.uimanager.IUIManager;
import org.emdev.utils.FileUtils;
import org.emdev.utils.LengthUtils;
import org.emdev.utils.StringUtils;

public class ViewerActivityController extends AbstractActivityController<ViewerActivity> implements
        IActivityController, DecodingProgressListener, CurrentPageListener, IAppSettingsChangeListener,
        IBookSettingsChangeListener {

    private final AtomicReference<IViewController> ctrl = new AtomicReference<IViewController>(ViewContollerStub.STUB);

    private ZoomModel zoomModel;

    private DecodingProgressModel progressModel;

    private DocumentModel documentModel;

    private SearchModel searchModel;

    private String bookTitle;

    private ContentScheme scheme;

    private CodecType codecType;

    private final Intent intent;

    private String m_fileName;

    private final NavigationHistory history;

    private String currentSearchPattern;

    private BookSettings bookSettings;

    private final AsyncTaskExecutor executor;

    /**
     * Instantiates a new base viewer activity.
     */
    public ViewerActivityController(final ViewerActivity activity) {
        super(activity, BEFORE_CREATE, BEFORE_RECREATE, AFTER_CREATE, ON_POST_CREATE, ON_DESTROY);

        intent = activity.getIntent();

        history = new NavigationHistory(this);

        executor = new AsyncTaskExecutor(256, 1, 5, 1, "BookExecutor-" + id);

        SettingsManager.addListener(this);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.emdev.ui.AbstractActivityController#beforeCreate(android.app.Activity)
     */
    @Override
    public void beforeCreate(final ViewerActivity activity) {
        final AppSettings newSettings = AppSettings.current();

        activity.setRequestedOrientation(newSettings.rotation.getOrientation());

        TouchManager.loadFromSettings(newSettings);
        KeyBindingsManager.loadFromSettings(newSettings);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.emdev.ui.AbstractActivityController#beforeRecreate(android.app.Activity)
     */
    @Override
    public void beforeRecreate(final ViewerActivity activity) {
        final AppSettings newSettings = AppSettings.current();

        activity.setRequestedOrientation(newSettings.rotation.getOrientation());

        TouchManager.loadFromSettings(newSettings);
        KeyBindingsManager.loadFromSettings(newSettings);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.emdev.ui.AbstractActivityController#afterCreate(android.app.Activity, boolean)
     */
    @Override
    public void afterCreate(final ViewerActivity activity, final boolean recreated) {

        final AppSettings appSettings = AppSettings.current();

        IUIManager.instance.setFullScreenMode(activity, getManagedComponent().view.getView(), appSettings.fullScreen);
        UIManagerAppCompat.setToolbarVisible(activity, appSettings.getShowTitle());

        createAction(R.id.mainmenu_crop).putValue("view", activity.getManualCropControls()).putValue("mode",
                DocumentViewMode.SINGLE_PAGE);
        createAction(R.id.mainmenu_zoom).putValue("view", activity.getZoomControls());
        createAction(R.id.mainmenu_search).putValue("view", activity.getSearchControls());
        createAction(R.id.actions_toggleTouchManagerView).putValue("view", activity.getTouchView());
        createAction(R.id.mainmenu_force_portrait).putValue("mode", BookRotationType.PORTRAIT);
        createAction(R.id.mainmenu_force_landscape).putValue("mode", BookRotationType.LANDSCAPE);

        if (recreated) {
            return;
        }

        documentModel = ActivityControllerStub.DM_STUB;
        searchModel = new SearchModel(this);

        if (intent == null) {
            showErrorDlg(R.string.msg_bad_intent, intent);
            return;
        }

        final Uri data = intent.getData();
        if (data == null) {
            showErrorDlg(R.string.msg_no_intent_data, intent);
            return;
        }

        scheme = ContentScheme.getScheme(intent);
        if (scheme == ContentScheme.UNKNOWN) {
            showErrorDlg(R.string.msg_bad_intent, intent);
            return;
        }

        bookTitle = scheme.getResourceName(activity.getContentResolver(), data);
        codecType = CodecType.getByUri(bookTitle);

        if (codecType == null) {
            bookTitle = ContentScheme.getDefaultResourceName(data, "");
            codecType = CodecType.getByUri(bookTitle);
        }
        bookTitle = StringUtils.cleanupTitle(bookTitle);

        if (codecType == null) {
            final String type = intent.getType();
            LCTX.i("Book mime type: " + type);
            if (LengthUtils.isNotEmpty(type)) {
                codecType = CodecType.getByMimeType(type);
            }
        }

        LCTX.i("Book codec type: " + codecType);
        LCTX.i("Book title: " + bookTitle);
        if (codecType == null) {
            showErrorDlg(R.string.msg_unknown_intent_data_type, data);
            return;
        }

        documentModel = new DocumentModel(codecType);
        documentModel.addListener(ViewerActivityController.this);
        progressModel = new DecodingProgressModel();
        progressModel.addListener(ViewerActivityController.this);

        final Uri uri = data;
        if (scheme.temporary) {
            m_fileName = scheme.key;
            CacheManager.clear(scheme.key);
        } else {
            m_fileName = PathFromUri.retrieve(activity.getContentResolver(), uri);
        }

        bookSettings = SettingsManager.create(id, m_fileName, scheme.temporary, intent);
        SettingsManager.applyBookSettingsChanges(null, bookSettings);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.emdev.ui.AbstractActivityController#onPostCreate(android.os.Bundle, boolean)
     */
    @Override
    public void onPostCreate(final Bundle savedInstanceState, final boolean recreated) {
        setWindowTitle();
        if (!recreated && documentModel != ActivityControllerStub.DM_STUB) {
            startDecoding("");
        }
    }

    public void startDecoding(final String password) {
        final BookLoadTask loadTask = new BookLoadTask(password);
        if (codecType != null && codecType.useCustomFonts ) {
            EBookDroidApp.checkInstalledFonts(getContext());
        }
        loadTask.run();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.emdev.ui.AbstractActivityController#onDestroy(boolean)
     */
    @Override
    public void onDestroy(final boolean finishing) {
        if (finishing) {
            if (BackupSettings.current().backupOnBookClose) {
                BackupManager.backup();
            }
            if (documentModel != null) {
                documentModel.recycle();
            }
            if (scheme != null && scheme.temporary) {
                CacheManager.clear(scheme.key);
            }
            SettingsManager.removeListener(this);
            BitmapManager.clear("on finish");
            ByteBufferManager.clear("on finish");
        }
    }

    public void askPassword(final String fileName, final int promtId) {
        final EditText input = new AppCompatEditText(getManagedComponent());
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        final ActionDialogBuilder builder = new ActionDialogBuilder(getManagedComponent(), this);
        builder.setTitle(fileName).setMessage(promtId).setView(input);
        builder.setPositiveButton(R.id.actions_redecodingWithPassword, new EditableValue("input", input));
        builder.setNegativeButton(R.id.mainmenu_close).show();
    }

    public void showErrorDlg(final int msgId, final Object... args) {
        final ActionDialogBuilder builder = new ActionDialogBuilder(getManagedComponent(), this);

        builder.setTitle(R.string.error_dlg_title);
        builder.setMessage(msgId, args);

        builder.setPositiveButton(R.string.error_close, R.id.mainmenu_close);
        builder.show();
    }

    @ActionMethod(ids = R.id.actions_redecodingWithPassword)
    public void redecodingWithPassword(final ActionEx action) {
        final PasswordEditable value = action.getParameter("input");
        final String password = value.getPassword();
        startDecoding(password);
    }

    protected IViewController switchDocumentController(final BookSettings bs) {
        if (bs != null) {
            try {
                final IViewController newDc = bs.viewMode.create(this);
                if (newDc != null) {
                    final IViewController oldDc = ctrl.getAndSet(newDc);
                    getZoomModel().removeListener(oldDc);
                    getZoomModel().addListener(newDc);
                    return ctrl.get();
                }
            } catch (final Throwable e) {
                LCTX.e("Unexpected error: ", e);
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.events.DecodingProgressListener#decodingProgressChanged(int)
     */
    @Override
    public void decodingProgressChanged(final int currentlyDecoding) {
        final Runnable r = new Runnable() {

            @Override
            public void run() {
                final ViewerActivity activity = getManagedComponent();
                UIManagerAppCompat.setProgressSpinnerVisible(activity, currentlyDecoding > 0);
            }
        };

        getView().post(r);
    }

    public String getPageNumberString() {
        final int pageCount = documentModel.getPageCount();
        PageIndex currentIndex = documentModel.getCurrentIndex();
        String pageText = "";
        if (pageCount > 0) {
            final int offset = bookSettings != null ? bookSettings.firstPageOffset : 1;
            if (offset == 1) {
                pageText = (currentIndex.viewIndex + 1) + "/" + pageCount;
            } else {
                pageText = offset + "/" + (currentIndex.viewIndex + offset) + "/" + (pageCount - 1 + offset);
            }
        }
        return pageText;
    }

    public String getWindowTitle() {
        final AppSettings app = AppSettings.current();
        if (app.pageInTitle) {
            return "(" + getPageNumberString()  + ") " + bookTitle;
        } else {
            return bookTitle;
        }
    }

    public void setWindowTitle() {
        getManagedComponent().getSupportActionBar().setTitle(getWindowTitle());
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.events.CurrentPageListener#currentPageChanged(org.ebookdroid.core.PageIndex,
     *      org.ebookdroid.core.PageIndex)
     */
    @Override
    public void currentPageChanged(final PageIndex oldIndex, final PageIndex newIndex) {
        final Runnable r = new Runnable() {

            @Override
            public void run() {
                setWindowTitle();
                getManagedComponent().currentPageChanged(getPageNumberString());
                SettingsManager.currentPageChanged(bookSettings, oldIndex, newIndex);
            }
        };

        getView().post(r);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IActivityController#runOnUiThread(java.lang.Runnable)
     */
    @Override
    public void runOnUiThread(final Runnable r) {
        final FutureTask<Object> task = new FutureTask<Object>(r, null);

        try {
            getActivity().runOnUiThread(task);
            task.get();
        } catch (final InterruptedException ex) {
            Thread.interrupted();
        } catch (final ExecutionException ex) {
            ex.printStackTrace();
        } catch (final Throwable th) {
            th.printStackTrace();
        }
    }

    @ActionMethod(ids = R.id.actions_openOptionsMenu)
    public void openOptionsMenu(final ActionEx action) {
        UIManagerAppCompat.openOptionsMenu(getManagedComponent(), getManagedComponent().view.getView());
    }

    @ActionMethod(ids = R.id.actions_gotoOutlineItem)
    public void gotoOutlineItem(final ActionEx action) {
        final OutlineLink link = action.getParameter(IActionController.ADAPTER_SELECTED_ITEM_PROPERTY);
        if (link == null) {
            return;
        }

        if (link.targetPage != -1) {
            final int pageCount = documentModel.decodeService.getPageCount();
            if (link.targetPage < 1 || link.targetPage > pageCount) {
                getManagedComponent().showToastText(2000, R.string.error_page_out_of_rande, pageCount);
            } else {
                getDocumentController().goToLink(link.targetPage - 1, link.targetRect,
                        AppSettings.current().storeOutlineGotoHistory);
            }
            return;
        }

        if (link.targetUrl != null) {
            final Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(link.targetUrl));
            getManagedComponent().startActivity(i);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IActivityController#jumpToPage(int, float, float)
     */
    @Override
    public void jumpToPage(final int viewIndex, final float offsetX, final float offsetY, final boolean addToHistory) {
        if (addToHistory) {
            history.update();
        }
        getDocumentController().goToPage(viewIndex, offsetX, offsetY);
    }

    @ActionMethod(ids = R.id.mainmenu_outline)
    public void showOutline(final ActionEx action) {
        final List<OutlineLink> outline = documentModel.decodeService.getOutline();
        if ((outline != null) && (outline.size() > 0)) {
            final OutlineDialog dlg = new OutlineDialog(this, outline);
            dlg.show();
        } else {
            getManagedComponent().showToastText(Toast.LENGTH_SHORT, R.string.outline_missed);
        }
    }

    @ActionMethod(ids = { R.id.actions_doSearch, R.id.actions_doSearchBack })
    public final void doSearch(final ActionEx action) {
        final Editable value = action.getParameter("input");
        final String newPattern = (value != null ? value.toString() : LengthUtils.toString(action.getParameter("text")));
        final String oldPattern = currentSearchPattern;

        currentSearchPattern = newPattern;

        executor.execute(new SearchTask(), newPattern, oldPattern, (String) action.getParameter("forward"));
    }

    @ActionMethod(ids = R.id.mainmenu_goto_page)
    public void showGotoDialog(final ActionEx action) {
        final GoToPageDialog dlg = new GoToPageDialog(this);
        dlg.show();
    }

    @ActionMethod(ids = R.id.mainmenu_booksettings)
    public void showBookSettings(final ActionEx action) {
        SettingsUI.showBookSettings(getManagedComponent(), bookSettings.fileName);
    }

    @ActionMethod(ids = R.id.mainmenu_settings)
    public void showAppSettings(final ActionEx action) {
        SettingsUI.showAppSettings(getManagedComponent(), bookSettings.fileName);
    }

    @ActionMethod(ids = R.id.mainmenu_fullscreen)
    public void toggleFullScreen(final ActionEx action) {
        AppSettings.toggleFullScreen();
    }

    @ActionMethod(ids = { R.id.mainmenu_force_portrait, R.id.mainmenu_force_landscape })
    public void forceOrientation(final ActionEx action) {
        final BookRotationType mode = action.getParameter("mode");
        if (bookSettings.rotation == mode) {
            SettingsManager.setBookRotation(bookSettings, BookRotationType.UNSPECIFIED);
        } else {
            SettingsManager.setBookRotation(bookSettings, mode);
        }
    }

    @ActionMethod(ids = R.id.mainmenu_nightmode)
    public void toggleNightMode(final ActionEx action) {
        SettingsManager.toggleNightMode(bookSettings);
    }

    @ActionMethod(ids = R.id.mainmenu_splitpages)
    public void toggleSplitPages(final ActionEx action) {
        SettingsManager.toggleSplitPages(bookSettings);
    }

    @ActionMethod(ids = R.id.mainmenu_croppages)
    public void toggleCropPages(final ActionEx action) {
        SettingsManager.toggleCropPages(bookSettings);
    }

    @ActionMethod(ids = R.id.mainmenu_thumbnail)
    public void setCurrentPageAsThumbnail(final ActionEx action) {
        final Page page = documentModel.getCurrentPageObject();
        if (page != null) {
            documentModel.createBookThumbnail(bookSettings, page, true, false);
        }
    }

    @ActionMethod(ids = R.id.mainmenu_singlepage)
    public void toggleSinglePage(final ActionEx action) {
        SettingsManager.toggleSinglePage(bookSettings);
    }

    @ActionMethod(ids = R.id.mainmenu_bookmark)
    public void showBookmarkDialog(final ActionEx action) {
        final int page = documentModel.getCurrentViewPageIndex();

        final String message = getManagedComponent().getString(R.string.add_bookmark_name);

        final BookSettings bs = getBookSettings();
        final int offset = bs != null ? bs.firstPageOffset : 1;

        final EditText input = (EditText) LayoutInflater.from(getManagedComponent()).inflate(R.layout.bookmark_edit,
                null);
        input.setText(getManagedComponent().getString(R.string.text_page) + " " + (page + offset));
        input.selectAll();

        final ActionDialogBuilder builder = new ActionDialogBuilder(getManagedComponent(), this);
        builder.setTitle(R.string.menu_add_bookmark).setMessage(message).setView(input);
        builder.setPositiveButton(R.id.actions_addBookmark, new EditableValue("input", input));
        builder.setNegativeButton().show();
    }

    @ActionMethod(ids = R.id.actions_addBookmark)
    public void addBookmark(final ActionEx action) {
        final Editable value = action.getParameter("input");
        final String name = value.toString();
        final Page page = documentModel.getCurrentPageObject();
        if (page != null) {
            final ViewState state = ViewState.get(getDocumentController());
            final PointF pos = state.getPositionOnPage(page);
            bookSettings.bookmarks.add(new Bookmark(name, documentModel.getCurrentIndex(), pos.x, pos.y));
            Collections.sort(bookSettings.bookmarks);
            SettingsManager.storeBookSettings(bookSettings);
            UIManagerAppCompat.invalidateOptionsMenu(getManagedComponent());
            state.release();
        }
    }

    @ActionMethod(ids = R.id.actions_goToBookmark)
    public void goToBookmark(final ActionEx action) {
        final Bookmark b = action.getParameter("bookmark");
        if (b == null) {
            return;
        }
        final Page actualPage = b.page.getActualPage(getDocumentModel(), bookSettings);
        if (actualPage != null) {
            jumpToPage(actualPage.index.viewIndex, b.offsetX, b.offsetY, AppSettings.current().storeGotoHistory);
        }
    }

    @ActionMethod(ids = R.id.actions_keyBindings)
    public void showKeyBindingsDialog(final ActionEx action) {
        final KeyBindingsDialog dlg = new KeyBindingsDialog(this);
        dlg.show();
    }

    /**
     * Gets the zoom model.
     *
     * @return the zoom model
     */
    @Override
    public ZoomModel getZoomModel() {
        if (zoomModel == null) {
            zoomModel = new ZoomModel();
        }
        return zoomModel;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IActivityController#getDecodeService()
     */
    @Override
    public DecodeService getDecodeService() {
        return documentModel != null ? documentModel.decodeService : null;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IActivityController#getDecodingProgressModel()
     */
    @Override
    public DecodingProgressModel getDecodingProgressModel() {
        return progressModel;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IActivityController#getDocumentModel()
     */
    @Override
    public DocumentModel getDocumentModel() {
        return documentModel;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IActivityController#getSearchModel()
     */
    @Override
    public final SearchModel getSearchModel() {
        return searchModel;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IActivityController#getDocumentController()
     */
    @Override
    public final IViewController getDocumentController() {
        return ctrl.get();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IActivityController#getView()
     */
    @Override
    @AnyThread
    public final IView getView() {
        return getManagedComponent().view;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IActivityController#getBookSettings()
     */
    @Override
    public final BookSettings getBookSettings() {
        return bookSettings;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IActivityController#getActionController()
     */
    @Override
    public final IActionController<?> getActionController() {
        return this;
    }

    @ActionMethod(ids = { R.id.mainmenu_zoom, R.id.actions_toggleTouchManagerView, R.id.mainmenu_search,
            R.id.mainmenu_crop })
    public void toggleControls(final ActionEx action) {
        final View view = action.getParameter("view");
        final DocumentViewMode mode = action.getParameter("mode");
        if (mode != null && bookSettings != null && bookSettings.viewMode != mode) {

            final ActionDialogBuilder builder = new ActionDialogBuilder(getContext(), this);
            builder.setTitle(android.R.string.dialog_alert_title);
            builder.setMessage(R.string.error_invalid_view_mode, mode.getResValue());
            builder.setNegativeButton();
            builder.show();
            return;
        }
        ViewEffects.toggleControls(view);
        if (view instanceof ManualCropView) {
            final ManualCropView mcv = (ManualCropView) view;
            if (mcv.getVisibility() == View.VISIBLE) {
                mcv.initControls();
            }
        }
        UIManagerAppCompat.invalidateOptionsMenu(getManagedComponent());
    }

    public final boolean dispatchKeyEvent(final KeyEvent event) {
	if (event.isCanceled()) {
	    return false;
	}

        final int action = event.getAction();
        final int keyCode = event.getKeyCode();

        if (getManagedComponent().getSearchControls().getVisibility() == View.VISIBLE) {
            if (action == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
                toggleControls(getAction(R.id.mainmenu_search));
                return true;
            }
            return false;
        }

        if (getDocumentController().dispatchKeyEvent(event)) {
            return true;
        }

        if (action == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
	    if (getManagedComponent().getTouchView().isShown()) {
		ViewEffects.toggleControls(getManagedComponent().getTouchView());
	    } else if (getManagedComponent().getManualCropControls().isShown()) {
		ViewEffects.toggleControls(getManagedComponent().getManualCropControls());
	    } else {

		if (history.goBack()) {
		    return true;
		}

		if (AppSettings.current().confirmClose) {
		    final ActionDialogBuilder builder = new ActionDialogBuilder(getManagedComponent(), this);
		    builder.setTitle(R.string.confirmclose_title);
		    builder.setMessage(R.string.confirmclose_msg);
		    builder.setPositiveButton(R.id.mainmenu_close);
		    builder.setNegativeButton().show();
		} else {
		    getOrCreateAction(R.id.mainmenu_close).run();
		}
	    }
	    return true;
        }
        return false;
    }

    private String getShareMimeType() {
        return codecType.mimeTypes.get(0);
    }

    private Uri getShareURI() {
        final String mimeType = getShareMimeType();
        Uri.Builder builder = new Uri.Builder();
        Uri uri;
        if (scheme.temporary) {
            builder.scheme("content");
            builder.authority("org.ebookdroid.document");
            builder.path(m_fileName);
            builder.query(mimeType);
            uri = builder.build();
        } else {
            builder.scheme("file");
            builder.path(m_fileName);
            uri = builder.build();
        }
        return uri;
    }

    @ActionMethod(ids = R.id.mainmenu_share)
    public void shareDocument(final ActionEx action) {
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        share.putExtra(Intent.EXTRA_STREAM, getShareURI());
        share.setType(getShareMimeType());

        getManagedComponent().startActivity(Intent.createChooser(share, getManagedComponent().getString(R.string.menu_share)));
    }

    @ActionMethod(ids = R.id.mainmenu_openwith)
    public void openWith(final ActionEx action) {
        Intent openwith = new Intent(Intent.ACTION_VIEW);
        openwith.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        openwith.setDataAndType(getShareURI(), getShareMimeType());

        getManagedComponent().startActivity(Intent.createChooser(openwith, getManagedComponent().getString(R.string.menu_openwith)));
    }

    @ActionMethod(ids = R.id.mainmenu_close)
    public void closeActivity(final ActionEx action) {
        if (scheme == null || !scheme.promptForSave) {
            getOrCreateAction(R.id.actions_doClose).run();
            return;
        }

        // TODO: These two lines appear to do nothing, "save" value is never used.
        getOrCreateAction(R.id.actions_doSaveAndClose).putValue("save", Boolean.TRUE);
        getOrCreateAction(R.id.actions_doClose).putValue("save", Boolean.FALSE);

        final ActionDialogBuilder builder = new ActionDialogBuilder(getManagedComponent(), this);
        builder.setTitle(R.string.confirmclose_title);
        builder.setMessage(R.string.confirmsave_msg);
        builder.setPositiveButton(R.string.confirmsave_yes_btn, R.id.actions_showSaveDlg);
        builder.setNegativeButton(R.string.confirmsave_no_btn, R.id.actions_doClose);
        builder.show();
    }

    @ActionMethod(ids = android.R.id.home)
    public void navigateUp(final ActionEx action) {
        // Set a flag so that R.id.actions_doClose actually performs an "up" action instead of just closing
        getOrCreateAction(R.id.actions_doClose).putValue("up", Boolean.TRUE);

        // Show the save prompt if needed, then runs R.id.actions_doClose
        getOrCreateAction(R.id.mainmenu_close).run();
    }

    @ActionMethod(ids = R.id.actions_showSaveDlg)
    public void showSaveDlg(final ActionEx action) {
        final FolderDlg dlg = new FolderDlg(this);
        dlg.show(BaseDroidApp.EXT_STORAGE, R.string.confirmclose_title, R.id.actions_doSaveAndClose, R.id.actions_doClose);
    }

    @ActionMethod(ids = R.id.actions_doSaveAndClose)
    public void doSaveAndClose(final ActionEx action) {
        final File targetFolder = action.getParameter(FolderDlg.SELECTED_FOLDER);
        final File source = new File(m_fileName);
        final File target = new File(targetFolder, source.getName());

        try {
            FileUtils.copy(source, target);
            SettingsManager.copyBookSettings(target, bookSettings);
            CacheManager.copy(source.getAbsolutePath(), target.getAbsolutePath(), true);
        } catch (final IOException ex) {
            ex.printStackTrace();
        }

        doClose(action);
    }

    @ActionMethod(ids = R.id.actions_doClose)
    public void doClose(final ActionEx action) {
        if (documentModel != null) {
            documentModel.recycle();
        }
        if (scheme != null && scheme.temporary) {
            CacheManager.clear(m_fileName);
        }
        SettingsManager.releaseBookSettings(id, bookSettings);

        if (getOrCreateAction(R.id.actions_doClose).getParameter("up", Boolean.FALSE).booleanValue()) {
            goUp();
        } else {
            getManagedComponent().finish();
        }
    }

    private void goUp() {
        // Implementation of the up button from http://developer.android.com/training/implementing-navigation/ancestral.html
        // isTaskRoot() check works around a bug where pressing "up" does nothing when the viewer is launched
        // by tapping on a pdf download notification in the notification list.
        // see: http://stackoverflow.com/questions/19999619/navutils-navigateupto-does-not-start-any-activity
        Activity activity = getActivity();
        Intent upIntent = NavUtils.getParentActivityIntent(activity);
        if (NavUtils.shouldUpRecreateTask(activity, upIntent) || activity.isTaskRoot()) {
            // e.g., this is the case when opening a pdf from the Downloads app and pressing the up button:
            // the ViewerActivity is running in the Downloads task, so the following will start a new task
            // to open the document-viewer library in.
            TaskStackBuilder.create(activity)
                    .addNextIntentWithParentStack(upIntent)
                    .startActivities();

            getActivity().finish();
        } else {
            // Restart the existing instance of the RecentActivity rather than starting a new one   
            upIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            NavUtils.navigateUpTo(activity, upIntent);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.common.settings.listeners.IAppSettingsChangeListener#onAppSettingsChanged(org.ebookdroid.common.settings.AppSettings,
     *      org.ebookdroid.common.settings.AppSettings, org.ebookdroid.common.settings.AppSettings.Diff)
     */
    @Override
    public void onAppSettingsChanged(final AppSettings oldSettings, final AppSettings newSettings,
            final AppSettings.Diff diff) {
        final ViewerActivity activity = getManagedComponent();
        if (diff.isRotationChanged()) {
            if (bookSettings != null) {
                activity.setRequestedOrientation(bookSettings.getOrientation(newSettings));
            } else {
                activity.setRequestedOrientation(newSettings.rotation.getOrientation());
            }
        }

        if (diff.isFullScreenChanged()) {
            IUIManager.instance.setFullScreenMode(activity, activity.view.getView(), newSettings.fullScreen);
            UIManagerAppCompat.setToolbarVisible(activity, newSettings.getShowTitle());
            setWindowTitle();
        }

        if (diff.isKeepScreenOnChanged()) {
            activity.view.getView().setKeepScreenOn(newSettings.keepScreenOn);
        }

        if (diff.isTapConfigChanged()) {
            TouchManager.loadFromSettings(newSettings);
        }

        if (diff.isKeyBindingChanged()) {
            KeyBindingsManager.loadFromSettings(newSettings);
        }

        if (diff.isPagesInMemoryChanged()) {
            getDocumentController().updateMemorySettings();
        }

        UIManagerAppCompat.invalidateOptionsMenu(getManagedComponent());
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.common.settings.listeners.IBookSettingsChangeListener#onBookSettingsChanged(org.ebookdroid.common.settings.books.BookSettings,
     *      org.ebookdroid.common.settings.books.BookSettings, org.ebookdroid.common.settings.books.BookSettings.Diff)
     */
    @Override
    public void onBookSettingsChanged(final BookSettings oldSettings, final BookSettings newSettings,
            final BookSettings.Diff diff) {
        if (newSettings == null) {
            return;
        }

        boolean redrawn = false;
        if (diff.isViewModeChanged() || diff.isSplitPagesChanged() || diff.isCropPagesChanged() || diff.isRTLChanged()) {
            redrawn = true;
            final IViewController newDc = switchDocumentController(newSettings);
            if (!diff.isFirstTime() && newDc != null) {
                newDc.init(null);
                newDc.show();
            }
        }

        if (diff.isRotationChanged()) {
            getManagedComponent().setRequestedOrientation(newSettings.getOrientation(AppSettings.current()));
        }

        if (diff.isFirstTime()) {
            getZoomModel().initZoom(newSettings.getZoom());
        }

        final IViewController dc = getDocumentController();

        if (!redrawn && (diff.isEffectsChanged())) {
            redrawn = true;
            dc.toggleRenderingEffects();
        }

        if (!redrawn && diff.isPageAlignChanged()) {
            dc.setAlign(newSettings.pageAlign);
        }

        if (diff.isAnimationTypeChanged()) {
            dc.updateAnimationType();
        }

        currentPageChanged(PageIndex.NULL, documentModel.getCurrentIndex());

        UIManagerAppCompat.invalidateOptionsMenu(getManagedComponent());
    }

    final class BookLoadTask extends BaseAsyncTask<String, Throwable> implements Runnable, IProgressIndicator {

        private final String m_password;

        public BookLoadTask(final String password) {
            super(getManagedComponent(), R.string.msg_loading, false);
            m_password = password;
        }

        @Override
        public void run() {
            executor.execute(this, "");
        }

        @Override
        protected Throwable doInBackground(final String... params) {
            LCTX.d("BookLoadTask.doInBackground(): start");
            try {
                final File cached = scheme.loadToCache(intent.getData(), codecType.getDefaultExtension(), this);
                if (cached != null) {
                    m_fileName = cached.getAbsolutePath();
                    setProgressDialogMessage(startProgressStringId);
                }
                getView().waitForInitialization();
                documentModel.open(m_fileName, m_password);
                getDocumentController().init(this);
                return null;
            } catch (final MuPdfPasswordException pex) {
                LCTX.i(pex.getMessage());
                return pex;
            } catch (final Exception e) {
                LCTX.e(e.getMessage(), e);
                return e;
            } catch (final Throwable th) {
                LCTX.e("BookLoadTask.doInBackground(): Unexpected error", th);
                return th;
            } finally {
                LCTX.d("BookLoadTask.doInBackground(): finish");
            }
        }

        @Override
        protected void onPostExecute(Throwable result) {
            LCTX.d("BookLoadTask.onPostExecute(): start");
            try {
                if (result == null) {
                    try {
                        getDocumentController().show();

                        final DocumentModel dm = getDocumentModel();
                        currentPageChanged(PageIndex.NULL, dm.getCurrentIndex());

                    } catch (final Throwable th) {
                        result = th;
                    }
                }

                super.onPostExecute(result);

                if (result instanceof MuPdfPasswordException) {
                    final MuPdfPasswordException pex = (MuPdfPasswordException) result;
                    final int promptId = pex.isWrongPasswordEntered() ? R.string.msg_wrong_password
                            : R.string.msg_password_required;

                    askPassword(m_fileName, promptId);

                } else if (result != null) {
                    final String msg = result.getMessage();
                    LogManager.onUnexpectedError(result);
                    showErrorDlg(R.string.msg_unexpected_error, msg);
                }
            } catch (final Throwable th) {
                LCTX.e("BookLoadTask.onPostExecute(): Unexpected error", th);
                LogManager.onUnexpectedError(result);
            } finally {
                LCTX.d("BookLoadTask.onPostExecute(): finish");
            }
        }

        @Override
        public void setProgressDialogMessage(final int resourceID, final Object... args) {
            publishProgress(getManagedComponent().getString(resourceID, args));
        }
    }

    final class SearchTask extends AsyncTask<String, String, RectF> implements SearchModel.ProgressCallback,
            OnCancelListener {

        private ProgressDialog progressDialog;
        private final AtomicBoolean continueFlag = new AtomicBoolean(true);
        private String pattern;
        private Page targetPage = null;

        @Override
        public void onCancel(final DialogInterface dialog) {
            documentModel.decodeService.stopSearch(pattern);
            continueFlag.set(false);
        }

        @Override
        public void searchStarted(final int pageIndex) {
            final int offset = bookSettings != null ? bookSettings.firstPageOffset : 1;
            publishProgress(getManagedComponent().getResources().getString(R.string.msg_search_text_on_page,
                    pageIndex + offset));
        }

        @Override
        public void searchFinished(final int pageIndex) {
        }

        @Override
        protected RectF doInBackground(final String... params) {
            try {
                final int length = LengthUtils.length(params);

                pattern = length > 0 ? params[0] : null;
                final boolean forward = length >= 3 ? Boolean.parseBoolean(params[2]) : true;

                searchModel.setPattern(pattern);

                final RectF current = forward ? searchModel.moveToNext(this) : searchModel.moveToPrev(this);
                targetPage = searchModel.getCurrentPage();
                if (LCTX.isDebugEnabled()) {
                    LCTX.d("SearchTask.doInBackground(): " + targetPage + " " + current);
                }
                return current;

            } catch (final Throwable th) {
                th.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(final RectF result) {
            if (progressDialog != null) {
                try {
                    progressDialog.dismiss();
                } catch (final Throwable th) {
                }
            }
            if (result != null) {
                final RectF newRect = new RectF(result);
                final SearchControls sc = getManagedComponent().getSearchControls();
                final int controlsHeight = 3 + sc.getActualHeight();
                final float pageHeight = targetPage.getBounds(getZoomModel().getZoom()).height();
                newRect.offset(0, -(controlsHeight / pageHeight));
                getDocumentController().goToLink(targetPage.index.docIndex, newRect,
                        AppSettings.current().storeSearchGotoHistory);
            } else {
                Toast.makeText(getManagedComponent(), R.string.msg_no_text_found, Toast.LENGTH_LONG).show();
            }
            getDocumentController().redrawView();
        }

        @Override
        protected void onProgressUpdate(final String... values) {
            final int length = LengthUtils.length(values);
            if (length == 0) {
                return;
            }
            final String last = values[length - 1];
            if (progressDialog == null || !progressDialog.isShowing()) {
                progressDialog = ProgressDialog.show(getManagedComponent(), "", last, true);
                progressDialog.setCancelable(true);
                progressDialog.setCanceledOnTouchOutside(true);
                progressDialog.setOnCancelListener(this);
            } else {
                progressDialog.setMessage(last);
            }
        }
    }
}
