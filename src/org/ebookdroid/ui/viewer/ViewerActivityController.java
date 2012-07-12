package org.ebookdroid.ui.viewer;

import org.ebookdroid.CodecType;
import org.ebookdroid.R;
import org.ebookdroid.common.bitmaps.BitmapManager;
import org.ebookdroid.common.cache.CacheManager;
import org.ebookdroid.common.keysbinding.KeyBindingsDialog;
import org.ebookdroid.common.keysbinding.KeyBindingsManager;
import org.ebookdroid.common.log.EmergencyHandler;
import org.ebookdroid.common.log.LogContext;
import org.ebookdroid.common.settings.AppSettings;
import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.common.settings.books.BookSettings;
import org.ebookdroid.common.settings.books.Bookmark;
import org.ebookdroid.common.settings.listeners.IAppSettingsChangeListener;
import org.ebookdroid.common.settings.listeners.IBookSettingsChangeListener;
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
import org.ebookdroid.ui.settings.SettingsUI;
import org.ebookdroid.ui.viewer.dialogs.OutlineDialog;
import org.ebookdroid.ui.viewer.stubs.ActivityControllerStub;
import org.ebookdroid.ui.viewer.stubs.ViewContollerStub;
import org.ebookdroid.ui.viewer.views.SearchControls;
import org.ebookdroid.ui.viewer.views.ViewEffects;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.PointF;
import android.graphics.RectF;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.emdev.ui.actions.ActionController;
import org.emdev.ui.actions.ActionDialogBuilder;
import org.emdev.ui.actions.ActionEx;
import org.emdev.ui.actions.ActionMethod;
import org.emdev.ui.actions.ActionMethodDef;
import org.emdev.ui.actions.ActionTarget;
import org.emdev.ui.actions.IActionController;
import org.emdev.ui.actions.params.Constant;
import org.emdev.ui.actions.params.EditableValue;
import org.emdev.ui.progress.IProgressIndicator;
import org.emdev.ui.tasks.BaseAsyncTask;
import org.emdev.ui.uimanager.IUIManager;
import org.emdev.utils.LengthUtils;
import org.emdev.utils.StringUtils;
import org.emdev.utils.filesystem.PathFromUri;

@ActionTarget(
// action list
actions = {
        // start
        @ActionMethodDef(id = R.id.actions_redecodingWithPassword, method = "redecodingWithPassword"),
        @ActionMethodDef(id = R.id.actions_openOptionsMenu, method = "openOptionsMenu"),
        @ActionMethodDef(id = R.id.actions_gotoOutlineItem, method = "gotoOutlineItem"),
        @ActionMethodDef(id = R.id.mainmenu_outline, method = "showOutline"),
        @ActionMethodDef(id = R.id.actions_doSearch, method = "doSearch"),
        @ActionMethodDef(id = R.id.actions_doSearchBack, method = "doSearch"),
        @ActionMethodDef(id = R.id.mainmenu_goto_page, method = "showDialog"),
        @ActionMethodDef(id = R.id.mainmenu_booksettings, method = "showBookSettings"),
        @ActionMethodDef(id = R.id.mainmenu_settings, method = "showAppSettings"),
        @ActionMethodDef(id = R.id.mainmenu_nightmode, method = "toggleNightMode"),
        @ActionMethodDef(id = R.id.mainmenu_thumbnail, method = "setCurrentPageAsThumbnail"),
        @ActionMethodDef(id = R.id.mainmenu_bookmark, method = "showBookmarkDialog"),
        @ActionMethodDef(id = R.id.actions_addBookmark, method = "addBookmark"),
        @ActionMethodDef(id = R.id.actions_keyBindings, method = "showKeyBindingsDialog"),
        @ActionMethodDef(id = R.id.mainmenu_zoom, method = "toggleControls"),
        @ActionMethodDef(id = R.id.actions_toggleTouchManagerView, method = "toggleControls"),
        @ActionMethodDef(id = R.id.mainmenu_search, method = "toggleControls"),
        @ActionMethodDef(id = R.id.mainmenu_close, method = "closeActivity")
// finish
})
public class ViewerActivityController extends ActionController<ViewerActivity> implements IActivityController,
        DecodingProgressListener, CurrentPageListener, IAppSettingsChangeListener, IBookSettingsChangeListener {

    private static final String E_MAIL_ATTACHMENT = "[E-mail Attachment]";

    private static final int DIALOG_GOTO = 0;

    private static final AtomicLong SEQ = new AtomicLong();

    private final LogContext LCTX;

    private final AtomicReference<IViewController> ctrl = new AtomicReference<IViewController>(ViewContollerStub.STUB);

    private ZoomModel zoomModel;

    private DecodingProgressModel progressModel;

    private DocumentModel documentModel;

    private SearchModel searchModel;

    private String bookTitle;

    private boolean temporaryBook;

    private CodecType codecType;

    private final Intent intent;

    private int loadingCount = 0;

    private String m_fileName;

    private final NavigationHistory history;

    private String currentSearchPattern;

    /**
     * Instantiates a new base viewer activity.
     */
    public ViewerActivityController(final ViewerActivity activity) {
        super(activity);
        LCTX = LogContext.ROOT.lctx("Controller", true).lctx("" + SEQ.getAndIncrement(), true);
        this.intent = activity.getIntent();
        SettingsManager.addListener(this);

        history = new NavigationHistory(this);
    }

    public void beforeCreate(final ViewerActivity activity) {
        if (LCTX.isDebugEnabled()) {
            LCTX.d("beforeCreate(): " + activity.LCTX);
        }
        if (getManagedComponent() != activity) {
            setManagedComponent(activity);
        }

        final AppSettings newSettings = AppSettings.current();

        activity.setRequestedOrientation(newSettings.rotation.getOrientation());

        TouchManager.loadFromSettings(newSettings);
        KeyBindingsManager.loadFromSettings(newSettings);

        BitmapManager.setPartSize(1 << newSettings.bitmapSize);
        BitmapManager.setUseEarlyRecycling(newSettings.useEarlyRecycling);
    }

    public void afterCreate() {
        if (LCTX.isDebugEnabled()) {
            LCTX.d("afterCreate()");
        }

        final ViewerActivity activity = getManagedComponent();
        final AppSettings appSettings = AppSettings.current();

        IUIManager.instance.setFullScreenMode(activity, getManagedComponent().view.getView(), appSettings.fullScreen);
        IUIManager.instance.setTitleVisible(activity, appSettings.showTitle);

        createAction(R.id.mainmenu_goto_page, new Constant("dialogId", DIALOG_GOTO));
        createAction(R.id.mainmenu_zoom).putValue("view", activity.getZoomControls());
        createAction(R.id.mainmenu_search).putValue("view", activity.getSearchControls());
        createAction(R.id.actions_toggleTouchManagerView).putValue("view", activity.getTouchView());

        if (++loadingCount == 1) {
            documentModel = ActivityControllerStub.DM_STUB;
            searchModel = new SearchModel(this);

            if (intent == null) {
                showErrorDlg("Bad intent or scheme:\n" + intent);
                return;
            }
            final String scheme = intent.getScheme();
            if (LengthUtils.isEmpty(scheme)) {
                showErrorDlg("Bad intent or scheme:\n" + intent);
                return;
            }
            final Uri data = intent.getData();
            if (data == null) {
                showErrorDlg("No intent data:\n" + intent);
                return;
            }
            if (scheme.equals("content")) {
                try {
                    final Cursor c = activity.getContentResolver().query(data, null, null, null, null);
                    c.moveToFirst();
                    final int fileNameColumnId = c.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
                    if (fileNameColumnId >= 0) {
                        final String attachmentFileName = c.getString(fileNameColumnId);
                        bookTitle = LengthUtils.safeString(attachmentFileName, E_MAIL_ATTACHMENT);
                        codecType = CodecType.getByUri(attachmentFileName);
                    } else {
                        if (LCTX.isDebugEnabled()) {
                            LCTX.d("No attachment file name returned");
                        }
                    }
                } catch (final Throwable th) {
                    LCTX.e("Unexpected error: ", th);
                }
            }
            if (codecType == null) {
                bookTitle = LengthUtils.safeString(data.getLastPathSegment(), E_MAIL_ATTACHMENT);
                codecType = CodecType.getByUri(data.toString());
                if (codecType == null) {
                    final String type = intent.getType();
                    LCTX.i("Book mime type: " + type);
                    if (LengthUtils.isNotEmpty(type)) {
                        codecType = CodecType.getByMimeType(type);
                    }
                }
            }

            LCTX.i("Book codec type: " + codecType);
            LCTX.i("Book title: " + bookTitle);
            if (codecType == null) {
                showErrorDlg("Unknown intent data type:\n" + data);
                return;
            }

            documentModel = new DocumentModel(codecType);
            documentModel.addListener(ViewerActivityController.this);
            progressModel = new DecodingProgressModel();
            progressModel.addListener(ViewerActivityController.this);

            final Uri uri = data;
            m_fileName = "";

            if (scheme.equals("content")) {
                temporaryBook = true;
                m_fileName = E_MAIL_ATTACHMENT;
                CacheManager.clear(m_fileName);
            } else {
                m_fileName = PathFromUri.retrieve(activity.getContentResolver(), uri);
            }

            SettingsManager.init(m_fileName, intent);
            SettingsManager.applyBookSettingsChanges(null, SettingsManager.getBookSettings(), null);
        }
    }

    public void beforePostCreate() {
        if (LCTX.isDebugEnabled()) {
            LCTX.d("beforePostCreate()");
        }
    }

    public void afterPostCreate() {
        if (LCTX.isDebugEnabled()) {
            LCTX.d("afterPostCreate()");
        }
        setWindowTitle();
        if (loadingCount == 1 && documentModel != ActivityControllerStub.DM_STUB) {
            startDecoding(m_fileName, "");
        }
    }

    public void startDecoding(final String fileName, final String password) {
        getManagedComponent().view.getView().post(new BookLoadTask(fileName, password));
    }

    public void beforeResume() {
        if (LCTX.isDebugEnabled()) {
            LCTX.d("beforeResume()");
        }
    }

    public void afterResume() {
        if (LCTX.isDebugEnabled()) {
            LCTX.d("afterResume()");
        }
    }

    public void beforePause() {
        if (LCTX.isDebugEnabled()) {
            LCTX.d("beforePause()");
        }
    }

    public void afterPause() {
        if (LCTX.isDebugEnabled()) {
            LCTX.d("afterPause()");
        }
        SettingsManager.storeBookSettings();
    }

    public void beforeDestroy() {
        final boolean finishing = getManagedComponent().isFinishing();
        if (LCTX.isDebugEnabled()) {
            LCTX.d("beforeDestroy(): " + finishing);
        }
        if (finishing) {
            getManagedComponent().view.onDestroy();
            if (documentModel != null) {
                documentModel.recycle();
            }
            if (temporaryBook) {
                CacheManager.clear(E_MAIL_ATTACHMENT);
            }
            SettingsManager.removeListener(this);
        }
    }

    public void afterDestroy() {
        if (LCTX.isDebugEnabled()) {
            LCTX.d("afterDestroy()");
        }
        getDocumentController().onDestroy();
    }

    public void askPassword(final String fileName, final String promt) {
        final EditText input = new EditText(getManagedComponent());
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        final ActionDialogBuilder builder = new ActionDialogBuilder(getManagedComponent(), this);
        builder.setTitle(fileName).setMessage(promt).setView(input);
        builder.setPositiveButton(R.id.actions_redecodingWithPassword, new EditableValue("input", input), new Constant(
                "fileName", fileName));
        builder.setNegativeButton(R.id.mainmenu_close).show();
    }

    public void showErrorDlg(final String msg) {
        final ActionDialogBuilder builder = new ActionDialogBuilder(getManagedComponent(), this);
        builder.setTitle("Application error").setMessage(LengthUtils.safeString(msg, "Unexpected error occured!"));
        builder.setPositiveButton(R.string.error_close, R.id.mainmenu_close);
        builder.show();
    }

    @ActionMethod(ids = R.id.actions_redecodingWithPassword)
    public void redecodingWithPassword(final ActionEx action) {
        final Editable value = action.getParameter("input");
        final String password = value.toString();
        final String fileName = action.getParameter("fileName");
        startDecoding(fileName, password);
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

    @Override
    public void decodingProgressChanged(final int currentlyDecoding) {
        final ViewerActivity activity = getManagedComponent();
        activity.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                try {
                    activity.setProgressBarIndeterminateVisibility(currentlyDecoding > 0);
                    activity.getWindow().setFeatureInt(Window.FEATURE_INDETERMINATE_PROGRESS,
                            currentlyDecoding == 0 ? 10000 : currentlyDecoding);
                } catch (final Throwable e) {
                }
            }
        });
    }

    @Override
    public void currentPageChanged(final PageIndex oldIndex, final PageIndex newIndex) {
        final int pageCount = documentModel.getPageCount();
        String pageText = "";
        if (pageCount > 0) {
            pageText = (newIndex.viewIndex + 1) + "/" + pageCount;
        }
        getManagedComponent().currentPageChanged(pageText, bookTitle);
        SettingsManager.currentPageChanged(oldIndex, newIndex);
    }

    public void setWindowTitle() {
        bookTitle = StringUtils.cleanupTitle(bookTitle);
        getManagedComponent().getWindow().setTitle(bookTitle);
    }

    @ActionMethod(ids = R.id.actions_openOptionsMenu)
    public void openOptionsMenu(final ActionEx action) {
        IUIManager.instance.openOptionsMenu(getManagedComponent(), getManagedComponent().view.getView());
    }

    @ActionMethod(ids = R.id.actions_gotoOutlineItem)
    public void gotoOutlineItem(final ActionEx action) {
        final OutlineLink link = action.getParameter(IActionController.ADAPTER_SELECTED_ITEM_PROPERTY);
        if (link == null) {
            return;
        }

        if (link.targetPage != -1) {
            final int pageCount = documentModel.getDecodeService().getPageCount();
            if (link.targetPage < 1 || link.targetPage > pageCount) {
                getManagedComponent().showToastText(2000, R.string.error_page_out_of_rande, pageCount);
            } else {
                getDocumentController().goToLink(link.targetPage - 1, link.targetRect, AppSettings.current().storeOutlineGotoHistory);
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
        final List<OutlineLink> outline = documentModel.getDecodeService().getOutline();
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
        new SearchTask().execute(newPattern, oldPattern, (String) action.getParameter("forward"));
    }

    @ActionMethod(ids = R.id.mainmenu_goto_page)
    public void showDialog(final ActionEx action) {
        final Integer dialogId = action.getParameter("dialogId");
        getManagedComponent().showDialog(dialogId);
    }

    @ActionMethod(ids = R.id.mainmenu_booksettings)
    public void showBookSettings(final ActionEx action) {
        SettingsUI.showBookSettings(getManagedComponent(), SettingsManager.getBookSettings().fileName);
    }

    @ActionMethod(ids = R.id.mainmenu_settings)
    public void showAppSettings(final ActionEx action) {
        SettingsUI.showAppSettings(getManagedComponent());
    }

    @ActionMethod(ids = R.id.mainmenu_nightmode)
    public void toggleNightMode(final ActionEx action) {
        SettingsManager.toggleNightMode();
    }

    @ActionMethod(ids = R.id.mainmenu_thumbnail)
    public void setCurrentPageAsThumbnail(final ActionEx action) {
        final Page page = documentModel.getCurrentPageObject();
        if (page != null) {
            documentModel.createBookThumbnail(SettingsManager.getBookSettings(), page, true);
        }
    }

    @ActionMethod(ids = R.id.mainmenu_bookmark)
    public void showBookmarkDialog(final ActionEx action) {
        final int page = documentModel.getCurrentViewPageIndex();

        final String message = getManagedComponent().getString(R.string.add_bookmark_name);

        final EditText input = new EditText(getManagedComponent());
        input.setText(getManagedComponent().getString(R.string.text_page) + " " + (page + 1));
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
        final BookSettings bs = SettingsManager.getBookSettings();
        final Page page = getDocumentModel().getCurrentPageObject();
        if (page != null) {
            final ViewState state = new ViewState(getDocumentController());
            final PointF pos = state.getPositionOnPage(page);
            bs.bookmarks.add(new Bookmark(name, getDocumentModel().getCurrentIndex(), pos.x, pos.y));
            SettingsManager.storeBookSettings();
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

    @Override
    public DecodeService getDecodeService() {
        return documentModel != null ? documentModel.getDecodeService() : null;
    }

    /**
     * Gets the decoding progress model.
     * 
     * @return the decoding progress model
     */
    @Override
    public DecodingProgressModel getDecodingProgressModel() {
        return progressModel;
    }

    @Override
    public DocumentModel getDocumentModel() {
        return documentModel;
    }

    @Override
    public SearchModel getSearchModel() {
        return searchModel;
    }

    @Override
    public IViewController getDocumentController() {
        return ctrl.get();
    }

    @Override
    public Context getContext() {
        return getManagedComponent();
    }

    @Override
    public IView getView() {
        return getManagedComponent().view;
    }

    @Override
    public Activity getActivity() {
        return getManagedComponent();
    }

    @Override
    public IActionController<?> getActionController() {
        return this;
    }

    @ActionMethod(ids = { R.id.mainmenu_zoom, R.id.actions_toggleTouchManagerView, R.id.mainmenu_search })
    public void toggleControls(final ActionEx action) {
        final View view = action.getParameter("view");
        ViewEffects.toggleControls(view);
    }

    public final boolean dispatchKeyEvent(final KeyEvent event) {
        if (getDocumentController().dispatchKeyEvent(event)) {
            return true;
        }

        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_BACK:
                    if (event.getRepeatCount() == 0) {
                        if (getManagedComponent().getTouchView().isShown()) {
                            ViewEffects.toggleControls(getManagedComponent().getTouchView());
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
                    }
                    return true;
                default:
                    return false;
            }
        }
        return false;
    }

    @ActionMethod(ids = R.id.mainmenu_close)
    public void closeActivity(final ActionEx action) {
        if (documentModel != null) {
            documentModel.recycle();
        }
        if (temporaryBook) {
            CacheManager.clear(E_MAIL_ATTACHMENT);
            SettingsManager.removeCurrentBookSettings();
        } else {
            SettingsManager.clearCurrentBookSettings();
        }
        getManagedComponent().finish();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.common.settings.listeners.ISettingsChangeListener#onAppSettingsChanged(org.ebookdroid.common.settings.AppSettings,
     *      org.ebookdroid.common.settings.AppSettings, org.ebookdroid.common.settings.AppSettings.Diff)
     */
    @Override
    public void onAppSettingsChanged(final AppSettings oldSettings, final AppSettings newSettings,
            final AppSettings.Diff diff) {
        final ViewerActivity activity = getManagedComponent();
        if (diff.isRotationChanged()) {
            activity.setRequestedOrientation(newSettings.rotation.getOrientation());
        }

        if (diff.isFullScreenChanged()) {
            IUIManager.instance.setFullScreenMode(activity, activity.view.getView(), newSettings.fullScreen);
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

        BitmapManager.setPartSize(1 << newSettings.bitmapSize);
        BitmapManager.setUseEarlyRecycling(newSettings.useEarlyRecycling);
        BitmapManager.setUseBitmapHack(newSettings.useBitmapHack);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.common.settings.listeners.ISettingsChangeListener#onBookSettingsChanged(org.ebookdroid.common.settings.books.BookSettings,
     *      org.ebookdroid.common.settings.books.BookSettings, org.ebookdroid.common.settings.books.BookSettings.Diff,
     *      org.ebookdroid.common.settings.AppSettings.Diff)
     */
    @Override
    public void onBookSettingsChanged(final BookSettings oldSettings, final BookSettings newSettings,
            final BookSettings.Diff diff, final AppSettings.Diff appDiff) {
        if (newSettings == null) {
            return;
        }

        boolean redrawn = false;
        if (diff.isViewModeChanged() || diff.isSplitPagesChanged() || diff.isCropPagesChanged()) {
            redrawn = true;
            final IViewController newDc = switchDocumentController(newSettings);
            if (!diff.isFirstTime() && newDc != null) {
                newDc.init(null);
                newDc.show();
            }
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

        if (!redrawn && appDiff != null) {
            if (appDiff.isPagesInMemoryChanged()) {
                dc.updateMemorySettings();
            }
        }

        currentPageChanged(PageIndex.NULL, documentModel.getCurrentIndex());
    }

    final class BookLoadTask extends BaseAsyncTask<String, Throwable> implements IProgressIndicator, Runnable {

        private String m_fileName;
        private final String m_password;

        public BookLoadTask(final String fileName, final String password) {
            super(getManagedComponent(), R.string.msg_loading, false);
            m_fileName = fileName;
            m_password = password;
        }

        @Override
        public void run() {
            execute(" ");
        }

        @Override
        protected Throwable doInBackground(final String... params) {
            LCTX.d("BookLoadTask.doInBackground(): start");
            try {
                if (intent.getScheme().equals("content")) {
                    final File tempFile = CacheManager.createTempFile(intent.getData());
                    m_fileName = tempFile.getAbsolutePath();
                }
                getView().waitForInitialization();
                documentModel.open(m_fileName, m_password);
                getDocumentController().init(this);
                return null;
            } catch (MuPdfPasswordException pex) {
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
                    MuPdfPasswordException pex = (MuPdfPasswordException) result;
                    askPassword(m_fileName, pex.isWrongPasswordEntered() ? "Wrong password given..." : "Enter password...");
                } else if (result != null) {
                    final String msg = result.getMessage();
                    EmergencyHandler.onUnexpectedError(result);
                    showErrorDlg(msg);
                }
            } catch (final Throwable th) {
                LCTX.e("BookLoadTask.onPostExecute(): Unexpected error", th);
                EmergencyHandler.onUnexpectedError(result);
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
        protected void onPreExecute() {
            // onProgressUpdate("Searching...");
        }

        @Override
        public void onCancel(final DialogInterface dialog) {
            documentModel.getDecodeService().stopSearch(pattern);
            continueFlag.set(false);
        }

        @Override
        public void searchStarted(int pageIndex) {
            publishProgress("Searching on page " + (pageIndex + 1) + "...");
        }

        @Override
        public void searchFinished(int pageIndex) {
        }

        @Override
        protected RectF doInBackground(final String... params) {
            try {
                final int length = LengthUtils.length(params);

                pattern = length > 0 ? params[0] : null;
                final boolean forward = length >= 3 ? Boolean.parseBoolean(params[2]) : true;

                searchModel.setPattern(pattern);

                RectF current = forward ? searchModel.moveToNext(this) : searchModel.moveToPrev(this);
                targetPage = searchModel.getCurrentPage();
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
                getDocumentController().goToLink(targetPage.index.docIndex, newRect, AppSettings.current().storeSearchGotoHistory);
            } else {
                Toast.makeText(getManagedComponent(), "Text not found", Toast.LENGTH_SHORT).show();
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
