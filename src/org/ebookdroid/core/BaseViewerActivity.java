package org.ebookdroid.core;

import org.ebookdroid.R;
import org.ebookdroid.core.events.CurrentPageListener;
import org.ebookdroid.core.events.DecodingProgressListener;
import org.ebookdroid.core.log.LogContext;
import org.ebookdroid.core.models.DecodingProgressModel;
import org.ebookdroid.core.models.DocumentModel;
import org.ebookdroid.core.models.ZoomModel;
import org.ebookdroid.core.multitouch.MultiTouchZoom;
import org.ebookdroid.core.settings.AppSettings;
import org.ebookdroid.core.settings.BookSettings;
import org.ebookdroid.core.settings.BookSettingsActivity;
import org.ebookdroid.core.settings.Bookmark;
import org.ebookdroid.core.settings.ISettingsChangeListener;
import org.ebookdroid.core.settings.SettingsActivity;
import org.ebookdroid.core.settings.SettingsManager;
import org.ebookdroid.core.utils.PathFromUri;
import org.ebookdroid.core.views.PageViewZoomControls;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.List;

public abstract class BaseViewerActivity extends Activity implements IViewerActivity, DecodingProgressListener,
        CurrentPageListener, ISettingsChangeListener {

    public static final LogContext LCTX = LogContext.ROOT.lctx("Core");

    private static final int DIALOG_GOTO = 0;

    private IDocumentViewController documentController;
    private Toast pageNumberToast;

    private ZoomModel zoomModel;
    private PageViewZoomControls zoomControls;

    private FrameLayout frameLayout;

    private DecodingProgressModel progressModel;

    private MultiTouchZoom multiTouchZoom;

    private DocumentModel documentModel;
    private String currentFilename;

    /**
     * Instantiates a new base viewer activity.
     */
    public BaseViewerActivity() {
        super();
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SettingsManager.addListener(this);

        frameLayout = createMainContainer();

        initActivity();

        initView("");
    }

    @Override
    protected void onDestroy() {
        if (documentModel != null) {
            documentModel.recycle();
            documentModel = null;
        }
        SettingsManager.removeListener(this);
        super.onDestroy();
    }

    private void initActivity() {
        SettingsManager.applyAppSettingsChanges(null, SettingsManager.getAppSettings());
    }

    private void initView(final String password) {
        final DecodeService decodeService = createDecodeService();

        final Uri uri = getIntent().getData();
        try {
            final String fileName = PathFromUri.retrieve(getContentResolver(), uri);

            SettingsManager.init(fileName);

            decodeService.open(fileName, password);

        } catch (final Exception e) {
            LCTX.e(e.getMessage(), e);
            final String msg = e.getMessage();

            if ("PDF needs a password!".equals(msg)) {
                askPassword();
            } else {
                showErrorDlg(msg);
            }
            return;
        }

        documentModel = new DocumentModel(decodeService);

        documentModel.addEventListener(this);

        zoomModel = new ZoomModel();

        initMultiTouchZoomIfAvailable();

        progressModel = new DecodingProgressModel();
        progressModel.addEventListener(this);

        SettingsManager.applyBookSettingsChanges(null, SettingsManager.getBookSettings(), null);

        setContentView(frameLayout);
        setProgressBarIndeterminateVisibility(false);
    }

    private void askPassword() {
        setContentView(R.layout.password);
        final Button ok = (Button) findViewById(R.id.pass_ok);
        ok.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                final EditText te = (EditText) findViewById(R.id.pass_req);
                initView(te.getText().toString());
            }
        });
        final Button cancel = (Button) findViewById(R.id.pass_cancel);
        cancel.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                closeActivity();
            }
        });
    }

    private void showErrorDlg(final String msg) {
        setContentView(R.layout.error);
        final TextView errortext = (TextView) findViewById(R.id.error_text);
        if (msg != null && msg.length() > 0) {
            errortext.setText(msg);
        } else {
            errortext.setText("Unexpected error occured!");
        }
        final Button cancel = (Button) findViewById(R.id.error_close);
        cancel.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                closeActivity();
            }
        });
    }

    private void initMultiTouchZoomIfAvailable() {
        try {
            multiTouchZoom = ((MultiTouchZoom) Class.forName("org.ebookdroid.core.multitouch.MultiTouchZoomImpl")
                    .getConstructor(ZoomModel.class).newInstance(zoomModel));
        } catch (final Exception e) {
            System.out.println("Multi touch zoom is not available: " + e);
        }
    }

    @Override
    public void createDocumentView() {
        if (documentController != null) {
            frameLayout.removeView(documentController.getView());
            zoomModel.removeEventListener(documentController);
        }

        final BookSettings bs = SettingsManager.getBookSettings();

        if (bs.getSinglePage()) {
            documentController = new SinglePageDocumentView(this);
        } else {
            documentController = new ContiniousDocumentView(this);
        }

        zoomModel.addEventListener(documentController);
        documentController.getView().setLayoutParams(
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));

        documentController.showDocument();

        frameLayout.removeView(getZoomControls());
        frameLayout.addView(documentController.getView());
        frameLayout.addView(getZoomControls());
    }

    @Override
    public void decodingProgressChanged(final int currentlyDecoding) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                try {
                    setProgressBarIndeterminateVisibility(true);
                    getWindow().setFeatureInt(Window.FEATURE_INDETERMINATE_PROGRESS,
                            currentlyDecoding == 0 ? 10000 : currentlyDecoding);
                } catch (final Throwable e) {
                }
            }
        });
    }

    @Override
    public void currentPageChanged(final PageIndex oldIndex, final PageIndex newIndex) {
        final int pageCount = documentModel.getPageCount();
        String prefix = "";

        if (pageCount > 0) {
            final String pageText = (newIndex.viewIndex + 1) + "/" + pageCount;
            if (SettingsManager.getAppSettings().getPageInTitle()) {
                prefix = "(" + pageText + ") ";
            } else {
                if (pageNumberToast != null) {
                    pageNumberToast.setText(pageText);
                } else {
                    pageNumberToast = Toast.makeText(this, pageText, 300);
                }
                pageNumberToast.setGravity(Gravity.TOP | Gravity.LEFT, 0, 0);
                pageNumberToast.show();
            }
        }

        getWindow().setTitle(prefix + currentFilename);
        SettingsManager.currentPageChanged(oldIndex, newIndex);
    }

    private void setWindowTitle() {
        currentFilename = getIntent().getData().getLastPathSegment();

        cleanupTitle();

        getWindow().setTitle(currentFilename);
    }

    /**
     * Cleanup title. Remove from title file extension and (...), [...]
     */
    private void cleanupTitle() {
        try {
            currentFilename = currentFilename.substring(0, currentFilename.lastIndexOf('.'));
            currentFilename = currentFilename.replaceAll("\\(.*\\)|\\[.*\\]", "");
        } catch (final IndexOutOfBoundsException e) {

        }
    }

    @Override
    protected void onPostCreate(final Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setWindowTitle();
        if (documentModel != null) {
            final BookSettings bs = SettingsManager.getBookSettings();
            if (bs != null) {
                currentPageChanged(PageIndex.NULL, bs.getCurrentPage());
            }
        }
    }

    private PageViewZoomControls getZoomControls() {
        if (zoomControls == null) {
            zoomControls = new PageViewZoomControls(this, zoomModel);
            zoomControls.setGravity(Gravity.RIGHT | Gravity.BOTTOM);
            zoomModel.addEventListener(zoomControls);
        }
        return zoomControls;
    }

    private FrameLayout createMainContainer() {
        return new FrameLayout(this);
    }

    protected abstract DecodeService createDecodeService();

    /**
     * Called on creation options menu
     *
     * @param menu
     *            the main menu
     * @return true, if successful
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {

        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainmenu, menu);
        return true;
    }

    @Override
    public boolean onMenuOpened(final int featureId, final Menu menu) {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        return super.onMenuOpened(featureId, menu);
    }

    @Override
    public void onOptionsMenuClosed(final Menu menu) {
        if (SettingsManager.getAppSettings().getFullScreen()) {
            getWindow()
                    .setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    private void showOutline() {
        final List<OutlineLink> outline = documentModel.getDecodeService().getOutline();
        if ((outline != null) && (outline.size() > 0)) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            final CharSequence[] items = outline.toArray(new CharSequence[outline.size()]);
            builder.setTitle("Outline");
            builder.setItems(items, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(final DialogInterface dialog, final int item) {
                    // Toast.makeText(getApplicationContext(), outline[item].getLink(),
                    // Toast.LENGTH_SHORT).show();
                    final String link = outline.get(item).getLink();
                    if (link.startsWith("#")) {
                        int pageNumber = 0;
                        try {
                            pageNumber = Integer.parseInt(link.substring(1).replace(" ", ""));
                        } catch (final Exception e) {
                            pageNumber = 0;
                        }
                        if (pageNumber < 1 || pageNumber > documentModel.getPageCount()) {
                            Toast.makeText(
                                    getApplicationContext(),
                                    "Page number out of range. Valid range: 1-"
                                            + documentModel.getDecodeService().getPageCount(), 2000).show();
                            return;
                        }
                        documentController.goToPage(pageNumber - 1);
                    } else if (link.startsWith("http:")) {
                        final Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setData(Uri.parse(link));
                        startActivity(i);
                    }
                }
            });
            final AlertDialog alert = builder.create();
            alert.show();
        } else {
            Toast.makeText(getApplicationContext(), "Document without Outline", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mainmenu_close:
                closeActivity();
                return true;
            case R.id.mainmenu_goto_page:
                showDialog(DIALOG_GOTO);
                return true;
            case R.id.mainmenu_zoom:
                zoomModel.toggleZoomControls();
                return true;
            case R.id.mainmenu_outline:
                showOutline();
                return true;
            case R.id.mainmenu_booksettings:
                final Intent bsa = new Intent(BaseViewerActivity.this, BookSettingsActivity.class);
                bsa.setData(Uri.fromFile(new File(SettingsManager.getBookSettings().getFileName())));
                startActivity(bsa);
                return true;
            case R.id.mainmenu_settings:
                final Intent i = new Intent(BaseViewerActivity.this, SettingsActivity.class);
                startActivity(i);
                return true;
            case R.id.mainmenu_nightmode:
                SettingsManager.getAppSettings().switchNightMode();
                getDocumentController().redrawView();
                return true;
            case R.id.mainmenu_bookmark:
                addBookmark();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void addBookmark() {
        final int page = getDocumentModel().getCurrentViewPageIndex();

        final String message = getString(R.string.add_bookmark_name);

        final EditText input = new EditText(this);
        input.setText(getString(R.string.text_page) + " " + (page + 1));

        new AlertDialog.Builder(this).setTitle(R.string.menu_add_bookmark).setMessage(message).setView(input)
                .setPositiveButton(R.string.password_ok, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(final DialogInterface dialog, final int whichButton) {
                        final Editable value = input.getText();
                        final BookSettings bs = SettingsManager.getBookSettings();
                        bs.getBookmarks().add(new Bookmark(getDocumentModel().getCurrentIndex(), value.toString()));
                        SettingsManager.edit(bs).commit();
                    }
                }).setNegativeButton(R.string.password_cancel, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(final DialogInterface dialog, final int whichButton) {
                    }
                }).show();
    }

    @Override
    protected Dialog onCreateDialog(final int id) {
        switch (id) {
            case DIALOG_GOTO:
                return new GoToPageDialog(this);
        }
        return null;
    }

    /**
     * Gets the zoom model.
     *
     * @return the zoom model
     */
    @Override
    public ZoomModel getZoomModel() {
        return zoomModel;
    }

    /**
     * Gets the multi touch zoom.
     *
     * @return the multi touch zoom
     */
    @Override
    public MultiTouchZoom getMultiTouchZoom() {
        return multiTouchZoom;
    }

    @Override
    public DecodeService getDecodeService() {
        return documentModel.getDecodeService();
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
    public IDocumentViewController getDocumentController() {
        return documentController;
    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public View getView() {
        return documentController.getView();
    }

    @Override
    public Activity getActivity() {
        return this;
    }

    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            closeActivity();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void closeActivity() {
        SettingsManager.clearCurrentBookSettings();
        finish();
    }

    @Override
    public void onAppSettingsChanged(final AppSettings oldSettings, final AppSettings newSettings,
            final AppSettings.Diff diff) {
        if (diff.isRotationChanged()) {
            setRequestedOrientation(newSettings.getRotation().getOrientation());
        }

        if (diff.isFullScreenChanged()) {
            final Window window = getWindow();
            if (newSettings.getFullScreen()) {
                window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }
        }

        if (diff.isShowTitleChanged() && diff.isFirstTime()) {
            final Window window = getWindow();
            try {
                if (!newSettings.getShowTitle()) {
                    window.requestFeature(Window.FEATURE_NO_TITLE);
                } else {
                    // Android 3.0+ you need both progress!!!
                    window.requestFeature(Window.FEATURE_PROGRESS);
                    window.requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
                    setProgressBarIndeterminate(true);
                }
            } catch (final Throwable th) {
                LCTX.e("Error on requestFeature call: " + th.getMessage());
            }
        }
        final IDocumentViewController dc = getDocumentController();
        if (dc != null) {
            if (diff.isKeepScreenOnChanged()) {
                dc.getView().setKeepScreenOn(newSettings.isKeepScreenOn());
            }
        }

    }

    @Override
    public void onBookSettingsChanged(final BookSettings oldSettings, final BookSettings newSettings,
            final BookSettings.Diff diff, final AppSettings.Diff appDiff) {

        boolean redrawn = false;
        if (diff.isSinglePageChanged() || diff.isSplitPagesChanged()) {
            redrawn = true;
            createDocumentView();
        }

        if (diff.isZoomChanged() && diff.isFirstTime()) {
            redrawn = true;
            getZoomModel().setZoom(newSettings.getZoom());
        }

        final IDocumentViewController dc = getDocumentController();
        if (dc != null) {

            if (diff.isPageAlignChanged()) {
                dc.setAlign(newSettings.getPageAlign());
            }

            if (diff.isAnimationTypeChanged()) {
                dc.updateAnimationType();
            }

            if (!redrawn && appDiff != null) {
                if (appDiff.isMaxImageSizeChanged() || appDiff.isPagesInMemoryChanged() || appDiff.isLowMemoryChanged()) {
                    dc.updateMemorySettings();
                }
            }
        }

        final DocumentModel dm = getDocumentModel();
        if (dm != null) {
            currentPageChanged(PageIndex.NULL, dm.getCurrentIndex());
        }
    }

}
