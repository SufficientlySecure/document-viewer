package org.ebookdroid.ui.viewer;

import org.ebookdroid.EBookDroidApp;
import org.ebookdroid.R;
import org.ebookdroid.common.settings.AppSettings;
import org.ebookdroid.common.settings.types.ToastPosition;
import org.ebookdroid.common.touch.TouchManagerView;
import org.ebookdroid.ui.viewer.dialogs.GoToPageDialog;
import org.ebookdroid.ui.viewer.views.ManualCropView;
import org.ebookdroid.ui.viewer.views.PageViewZoomControls;
import org.ebookdroid.ui.viewer.views.SearchControls;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.util.concurrent.atomic.AtomicLong;

import org.emdev.common.log.LogContext;
import org.emdev.common.log.LogManager;
import org.emdev.ui.AbstractActionActivity;
import org.emdev.ui.actions.ActionMethodDef;
import org.emdev.ui.actions.ActionTarget;
import org.emdev.ui.uimanager.IUIManager;
import org.emdev.utils.LayoutUtils;
import org.emdev.utils.LengthUtils;

@ActionTarget(
// action list
actions = {
// start
@ActionMethodDef(id = R.id.mainmenu_about, method = "showAbout")
// finish
})
public class ViewerActivity extends AbstractActionActivity<ViewerActivity, ViewerActivityController> {

    private static final int DIALOG_GOTO = 0;

    public static final DisplayMetrics DM = new DisplayMetrics();

    private static final AtomicLong SEQ = new AtomicLong();

    final LogContext LCTX;

    IView view;

    private Toast pageNumberToast;

    private Toast zoomToast;

    private PageViewZoomControls zoomControls;

    private SearchControls searchControls;

    private FrameLayout frameLayout;

    private TouchManagerView touchView;

    private boolean menuClosedCalled;

    private ManualCropView cropControls;

    /**
     * Instantiates a new base viewer activity.
     */
    public ViewerActivity() {
        super();
        LCTX = LogManager.root().lctx(this.getClass().getSimpleName(), true).lctx("" + SEQ.getAndIncrement(), true);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.emdev.ui.AbstractActionActivity#createController()
     */
    @Override
    protected ViewerActivityController createController() {
        return new ViewerActivityController(this);
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);
        if (LCTX.isDebugEnabled()) {
            LCTX.d("onNewIntent(): " + intent);
        }
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        if (LCTX.isDebugEnabled()) {
            LCTX.d("onCreate(): " + getIntent());
        }

        restoreController();
        getController().beforeCreate(this);

        super.onCreate(savedInstanceState);

        getWindowManager().getDefaultDisplay().getMetrics(DM);
        LCTX.i("XDPI=" + DM.xdpi + ", YDPI=" + DM.ydpi);

        frameLayout = new FrameLayout(this);

        view = AppSettings.current().viewType.create(getController());
        this.registerForContextMenu(view.getView());

        LayoutUtils.fillInParent(frameLayout, view.getView());

        frameLayout.addView(view.getView());
        frameLayout.addView(getZoomControls());
        frameLayout.addView(getManualCropControls());
        frameLayout.addView(getSearchControls());
        frameLayout.addView(getTouchView());

        getController().afterCreate();

        setContentView(frameLayout);
    }

    @Override
    protected void onResume() {
        if (LCTX.isDebugEnabled()) {
            LCTX.d("onResume()");
        }

        getController().beforeResume();

        super.onResume();
        IUIManager.instance.onResume(this);

        getController().afterResume();
    }

    @Override
    protected void onPause() {
        if (LCTX.isDebugEnabled()) {
            LCTX.d("onPause(): " + isFinishing());
        }

        getController().beforePause();

        super.onPause();
        IUIManager.instance.onPause(this);

        getController().afterPause();
    }

    @Override
    public void onWindowFocusChanged(final boolean hasFocus) {
        if (hasFocus && this.view != null) {
            IUIManager.instance.setFullScreenMode(this, this.view.getView(), AppSettings.current().fullScreen);
        }
    }

    @Override
    protected void onDestroy() {
        final boolean finishing = isFinishing();
        if (LCTX.isDebugEnabled()) {
            LCTX.d("onDestroy(): " + finishing);
        }

        getController().beforeDestroy(finishing);
        super.onDestroy();
        getController().afterDestroy(finishing);

        EBookDroidApp.onActivityClose(finishing);
    }

    protected IView createView() {
        return AppSettings.current().viewType.create(getController());
    }

    public TouchManagerView getTouchView() {
        if (touchView == null) {
            touchView = new TouchManagerView(getController());
        }
        return touchView;
    }

    public void currentPageChanged(final String pageText, final String bookTitle) {
        if (LengthUtils.isEmpty(pageText)) {
            return;
        }

        final AppSettings app = AppSettings.current();
        if (IUIManager.instance.isTitleVisible(this) && app.pageInTitle) {
            getWindow().setTitle("(" + pageText + ") " + bookTitle);
            return;
        }

        if (app.pageNumberToastPosition == ToastPosition.Invisible) {
            return;
        }
        if (pageNumberToast != null) {
            pageNumberToast.setText(pageText);
        } else {
            pageNumberToast = Toast.makeText(this, pageText, Toast.LENGTH_SHORT);
        }

        pageNumberToast.setGravity(app.pageNumberToastPosition.position, 0, 0);
        pageNumberToast.show();
    }

    public void zoomChanged(final float zoom) {
        if (getZoomControls().isShown()) {
            return;
        }

        final AppSettings app = AppSettings.current();

        if (app.zoomToastPosition == ToastPosition.Invisible) {
            return;
        }

        final String zoomText = String.format("%.2f", zoom) + "x";

        if (zoomToast != null) {
            zoomToast.setText(zoomText);
        } else {
            zoomToast = Toast.makeText(this, zoomText, Toast.LENGTH_SHORT);
        }

        zoomToast.setGravity(app.zoomToastPosition.position, 0, 0);
        zoomToast.show();
    }

    @Override
    protected void onPostCreate(final Bundle savedInstanceState) {
        getController().beforePostCreate();
        super.onPostCreate(savedInstanceState);
        getController().afterPostCreate();
    }

    public PageViewZoomControls getZoomControls() {
        if (zoomControls == null) {
            zoomControls = new PageViewZoomControls(this, getController().getZoomModel());
            zoomControls.setGravity(Gravity.RIGHT | Gravity.BOTTOM);
        }
        return zoomControls;
    }

    public SearchControls getSearchControls() {
        if (searchControls == null) {
            searchControls = new SearchControls(this);
        }
        return searchControls;
    }

    public ManualCropView getManualCropControls() {
        if (cropControls == null) {
            cropControls = new ManualCropView(getController());
        }
        return cropControls;
    }

    @Override
    public void onCreateContextMenu(final ContextMenu menu, final View v, final ContextMenuInfo menuInfo) {
        menu.setHeaderTitle(R.string.app_name);
        menu.setHeaderIcon(R.drawable.icon);
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainmenu, menu);
    }

    /**
     * {@inheritDoc}
     *
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainmenu, menu);
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @see android.app.Activity#onMenuOpened(int, android.view.Menu)
     */
    @Override
    public boolean onMenuOpened(final int featureId, final Menu menu) {
        view.changeLayoutLock(true);
        IUIManager.instance.onMenuOpened(this);
        return super.onMenuOpened(featureId, menu);
    }

    /**
     * {@inheritDoc}
     *
     * @see android.app.Activity#onPanelClosed(int, android.view.Menu)
     */
    @Override
    public void onPanelClosed(final int featureId, final Menu menu) {
        menuClosedCalled = false;
        super.onPanelClosed(featureId, menu);
        if (!menuClosedCalled) {
            onOptionsMenuClosed(menu);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see android.app.Activity#onOptionsMenuClosed(android.view.Menu)
     */
    @Override
    public void onOptionsMenuClosed(final Menu menu) {
        menuClosedCalled = true;
        IUIManager.instance.onMenuClosed(this);
        view.changeLayoutLock(false);
    }

    @Override
    protected Dialog onCreateDialog(final int id) {
        switch (id) {
            case DIALOG_GOTO:
                return new GoToPageDialog(getController());
        }
        return null;
    }

    @Override
    public final boolean dispatchKeyEvent(final KeyEvent event) {
        view.checkFullScreenMode();
        if (getController().dispatchKeyEvent(event)) {
            return true;
        }

        return super.dispatchKeyEvent(event);
    }

    public void showToastText(final int duration, final int resId, final Object... args) {
        Toast.makeText(getApplicationContext(), getResources().getString(resId, args), duration).show();
    }

}
