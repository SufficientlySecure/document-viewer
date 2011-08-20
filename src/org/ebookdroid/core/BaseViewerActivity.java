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

import java.util.List;

public abstract class BaseViewerActivity extends Activity implements IViewerActivity, DecodingProgressListener,
        CurrentPageListener {

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

        frameLayout = createMainContainer();

        initActivity();

        initView("");
    }

    private void initActivity() {
        getSettings().applyAppSettings(this);
    }

    private void initView(final String password) {
        final DecodeService decodeService = createDecodeService();

        final Uri uri = getIntent().getData();
        try {
            final String fileName = PathFromUri.retrieve(getContentResolver(), uri);

            getSettings().init(fileName);

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

        getSettings().applyBookSettings(this);

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

        final BookSettings bs = getBookSettings();

        if (bs.getSinglePage()) {
            documentController = new SinglePageDocumentView(this);
        } else {
            documentController = new ContiniousDocumentView(this);
        }

        zoomModel.addEventListener(documentController);
        documentController.getView().setLayoutParams(
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));

        documentController.goToPage(bs.getSplitPages() ? bs.getCurrentViewPage() : bs.getCurrentDocPage());
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
    public void currentPageChanged(final int docPageIndex, final int viewPageIndex) {
        final int pageCount = documentModel.getPageCount();
        String prefix = "";

        if (pageCount > 0) {
            final String pageText = (viewPageIndex + 1) + "/" + pageCount;
            if (getAppSettings().getPageInTitle()) {
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
        getSettings().currentPageChanged(docPageIndex, viewPageIndex);
    }

    private void setWindowTitle() {
        currentFilename = getIntent().getData().getLastPathSegment();
        getWindow().setTitle(currentFilename);
    }

    @Override
    protected void onPostCreate(final Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setWindowTitle();
        if (documentModel != null) {
            currentPageChanged(getBookSettings().getCurrentDocPage(), getBookSettings().getCurrentViewPage());
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

    @Override
    public void onResume() {
        super.onResume();
        if (documentModel != null) {
            getSettings().onAppSettingsChanged(this);
        }
    }

    @Override
    protected void onDestroy() {
        if (documentModel != null) {
            documentModel.recycle();
            documentModel = null;
        }
        super.onDestroy();
    }

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
        if (getAppSettings().getFullScreen()) {
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
            case R.id.mainmenu_settings:
                final Intent i = new Intent(BaseViewerActivity.this, SettingsActivity.class);
                startActivity(i);
                return true;
            case R.id.mainmenu_nightmode:
                getAppSettings().switchNightMode();
                ((AbstractDocumentView) getView()).redrawView();
                return true;
        }
        return super.onOptionsItemSelected(item);
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
    public SettingsManager getSettings() {
        return SettingsManager.getInstance(this);
    }

    @Override
    public AppSettings getAppSettings() {
        return getSettings().getAppSettings();
    }

    @Override
    public BookSettings getBookSettings() {
        return getSettings().getBookSettings();
    }

    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            closeActivity();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    
    private void closeActivity()
    {
        //final Intent myIntent = new Intent(BaseViewerActivity.this, RecentActivity.class);
        //startActivity(myIntent);
        finish();
    }

}
