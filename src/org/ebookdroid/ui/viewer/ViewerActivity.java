package org.ebookdroid.ui.viewer;

import org.ebookdroid.R;
import org.ebookdroid.common.log.LogContext;
import org.ebookdroid.common.settings.AppSettings;
import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.common.settings.types.ToastPosition;
import org.ebookdroid.common.touch.TouchManagerView;
import org.ebookdroid.ui.viewer.dialogs.GoToPageDialog;
import org.ebookdroid.ui.viewer.views.PageViewZoomControls;
import org.ebookdroid.ui.viewer.views.ViewEffects;

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

import org.emdev.ui.AbstractActionActivity;
import org.emdev.ui.actions.ActionEx;
import org.emdev.ui.actions.ActionMethod;
import org.emdev.ui.actions.ActionMethodDef;
import org.emdev.ui.actions.ActionTarget;
import org.emdev.ui.actions.IActionController;
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
public class ViewerActivity extends AbstractActionActivity {

    private static final int DIALOG_GOTO = 0;

    public static final DisplayMetrics DM = new DisplayMetrics();

    private static final AtomicLong SEQ = new AtomicLong();

    final LogContext LCTX;

    IView view;

    private Toast pageNumberToast;

    private Toast zoomToast;

    private PageViewZoomControls zoomControls;

    private FrameLayout frameLayout;

    private TouchManagerView touchView;

    private boolean menuClosedCalled;

    private ViewerActivityController controller;

    /**
     * Instantiates a new base viewer activity.
     */
    public ViewerActivity() {
        super();
        LCTX = LogContext.ROOT.lctx(this.getClass().getSimpleName(), true).lctx("" + SEQ.getAndIncrement(), true);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.emdev.ui.AbstractActionActivity#createController()
     */
    @Override
    protected IActionController<? extends AbstractActionActivity> createController() {
        if (controller == null) {
            controller = new ViewerActivityController(this);
        }
        return controller;
    }

    @Override
    protected void onNewIntent(Intent intent) {
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

        Object last = this.getLastNonConfigurationInstance();
        if (last instanceof ViewerActivityController) {
            this.controller = (ViewerActivityController) last;
        } else {
            this.controller = new ViewerActivityController(this);
        }

        this.controller.beforeCreate(this);

        super.onCreate(savedInstanceState);

        getWindowManager().getDefaultDisplay().getMetrics(DM);
        LCTX.i("XDPI=" + DM.xdpi + ", YDPI=" + DM.ydpi);

        frameLayout = new FrameLayout(this);

        view = SettingsManager.getAppSettings().viewType.create(controller);
        this.registerForContextMenu(view.getView());

        LayoutUtils.fillInParent(frameLayout, view.getView());

        frameLayout.addView(view.getView());
        frameLayout.addView(getZoomControls());
        frameLayout.addView(getTouchView());

        this.controller.afterCreate();

        setContentView(frameLayout);
    }

    @Override
    protected void onResume() {
        if (LCTX.isDebugEnabled()) {
            LCTX.d("onResume()");
        }

        this.controller.beforeResume();

        super.onResume();
        IUIManager.instance.onResume(this);

        this.controller.afterResume();
    }

    @Override
    protected void onPause() {
        if (LCTX.isDebugEnabled()) {
            LCTX.d("onPause(): " + isFinishing());
        }

        this.controller.beforePause();

        super.onPause();
        IUIManager.instance.onPause(this);

        this.controller.afterPause();
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return controller;
    }

    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus && this.view != null) {
            IUIManager.instance.setFullScreenMode(this, this.view.getView(), SettingsManager.getAppSettings().fullScreen);
        }
    }

    @Override
    protected void onDestroy() {
        if (LCTX.isDebugEnabled()) {
            LCTX.d("onDestroy(): " + isFinishing());
        }

        controller.beforeDestroy();
        super.onDestroy();
        controller.afterDestroy();
    }

    protected IView createView() {
        return SettingsManager.getAppSettings().viewType.create(controller);
    }

    public TouchManagerView getTouchView() {
        if (touchView == null) {
            touchView = new TouchManagerView(controller);
        }
        return touchView;
    }

    public void currentPageChanged(final String pageText, final String bookTitle) {
        if (LengthUtils.isEmpty(pageText)) {
            return;
        }

        AppSettings app = SettingsManager.getAppSettings();
        if (app.showTitle && app.pageInTitle) {
            getWindow().setTitle("(" + pageText + ") " + bookTitle);
            return;
        }

        if (app.pageNumberToastPosition == ToastPosition.Invisible) {
            return;
        }
        if (pageNumberToast != null) {
            pageNumberToast.setText(pageText);
        } else {
            pageNumberToast = Toast.makeText(this, pageText, 0);
        }

        pageNumberToast.setGravity(app.pageNumberToastPosition.position, 0, 0);
        pageNumberToast.show();
    }

    public void zoomChanged(final float zoom) {
        if (getZoomControls().isShown()) {
            return;
        }

        AppSettings app = SettingsManager.getAppSettings();

        if (app.zoomToastPosition == ToastPosition.Invisible) {
            return;
        }

        String zoomText = String.format("%.2f", zoom) + "x";

        if (zoomToast != null) {
            zoomToast.setText(zoomText);
        } else {
            zoomToast = Toast.makeText(this, zoomText, 0);
        }

        zoomToast.setGravity(app.zoomToastPosition.position, 0, 0);
        zoomToast.show();
    }

    @Override
    protected void onPostCreate(final Bundle savedInstanceState) {
        controller.beforePostCreate();
        super.onPostCreate(savedInstanceState);
        controller.afterPostCreate();
    }

    public PageViewZoomControls getZoomControls() {
        if (zoomControls == null) {
            zoomControls = new PageViewZoomControls(this, controller.getZoomModel());
            zoomControls.setGravity(Gravity.RIGHT | Gravity.BOTTOM);
        }
        return zoomControls;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
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
                return new GoToPageDialog(controller);
        }
        return null;
    }

    @ActionMethod(ids = { R.id.mainmenu_zoom, R.id.actions_toggleTouchManagerView })
    public void toggleControls(final ActionEx action) {
        final View view = action.getParameter("view");
        ViewEffects.toggleControls(view);
    }

    @Override
    public final boolean dispatchKeyEvent(final KeyEvent event) {
        if (controller.dispatchKeyEvent(event)) {
            return true;
        }

        return super.dispatchKeyEvent(event);
    }

    public void showToastText(int duration, int resId, Object... args) {
        Toast.makeText(getApplicationContext(), getResources().getString(resId, args), duration).show();
    }

}
