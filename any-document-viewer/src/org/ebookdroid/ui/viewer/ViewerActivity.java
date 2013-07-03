package org.ebookdroid.ui.viewer;

import org.ebookdroid.R;
import org.ebookdroid.common.settings.AppSettings;
import org.ebookdroid.common.settings.books.BookSettings;
import org.ebookdroid.common.settings.books.Bookmark;
import org.ebookdroid.common.settings.types.BookRotationType;
import org.ebookdroid.common.settings.types.ToastPosition;
import org.ebookdroid.common.touch.TouchManagerView;
import org.ebookdroid.core.DecodeService;
import org.ebookdroid.core.codec.CodecFeatures;
import org.ebookdroid.ui.viewer.stubs.ViewStub;
import org.ebookdroid.ui.viewer.viewers.GLView;
import org.ebookdroid.ui.viewer.views.ManualCropView;
import org.ebookdroid.ui.viewer.views.PageViewZoomControls;
import org.ebookdroid.ui.viewer.views.SearchControls;

import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import org.emdev.common.android.AndroidVersion;
import org.emdev.ui.AbstractActionActivity;
import org.emdev.ui.actions.ActionDialogBuilder;
import org.emdev.ui.actions.ActionMenuHelper;
import org.emdev.ui.gl.GLConfiguration;
import org.emdev.ui.uimanager.IUIManager;
import org.emdev.utils.LayoutUtils;
import org.emdev.utils.LengthUtils;

public class ViewerActivity extends AbstractActionActivity<ViewerActivity, ViewerActivityController> {

    public static final DisplayMetrics DM = new DisplayMetrics();

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
        super(false, ON_CREATE, ON_RESUME, ON_PAUSE, ON_DESTROY);
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

    /**
     * {@inheritDoc}
     * 
     * @see android.app.Activity#onNewIntent(android.content.Intent)
     */
    @Override
    protected void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);
        if (LCTX.isDebugEnabled()) {
            LCTX.d("onNewIntent(): " + intent);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.emdev.ui.AbstractActionActivity#onCreateImpl(android.os.Bundle)
     */
    @Override
    protected void onCreateImpl(final Bundle savedInstanceState) {
        getWindowManager().getDefaultDisplay().getMetrics(DM);
        LCTX.i("XDPI=" + DM.xdpi + ", YDPI=" + DM.ydpi);

        frameLayout = new FrameLayout(this);

        view = ViewStub.STUB;

        try {
            GLConfiguration.checkConfiguration();

            view = new GLView(getController());
            this.registerForContextMenu(view.getView());

            LayoutUtils.fillInParent(frameLayout, view.getView());

            frameLayout.addView(view.getView());
            frameLayout.addView(getZoomControls());
            frameLayout.addView(getManualCropControls());
            frameLayout.addView(getSearchControls());
            frameLayout.addView(getTouchView());

        } catch (final Throwable th) {
            final ActionDialogBuilder builder = new ActionDialogBuilder(this, getController());
            builder.setTitle(R.string.error_dlg_title);
            builder.setMessage(th.getMessage());
            builder.setPositiveButton(R.string.error_close, R.id.mainmenu_close);
            builder.show();
        }

        setContentView(frameLayout);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.emdev.ui.AbstractActionActivity#onResumeImpl()
     */
    @Override
    protected void onResumeImpl() {
        IUIManager.instance.onResume(this);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.emdev.ui.AbstractActionActivity#onPauseImpl(boolean)
     */
    @Override
    protected void onPauseImpl(final boolean finishing) {
        IUIManager.instance.onPause(this);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.emdev.ui.AbstractActionActivity#onDestroyImpl(boolean)
     */
    @Override
    protected void onDestroyImpl(final boolean finishing) {
        view.onDestroy();
    }

    /**
     * {@inheritDoc}
     * 
     * @see android.app.Activity#onWindowFocusChanged(boolean)
     */
    @Override
    public void onWindowFocusChanged(final boolean hasFocus) {
        if (hasFocus && this.view != null) {
            IUIManager.instance.setFullScreenMode(this, this.view.getView(), AppSettings.current().fullScreen);
        }
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

    /**
     * {@inheritDoc}
     * 
     * @see android.app.Activity#onCreateContextMenu(android.view.ContextMenu, android.view.View,
     *      android.view.ContextMenu.ContextMenuInfo)
     */
    @Override
    public void onCreateContextMenu(final ContextMenu menu, final View v, final ContextMenuInfo menuInfo) {
        menu.clear();
        menu.setHeaderTitle(R.string.app_name);
        menu.setHeaderIcon(R.drawable.application_icon);
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainmenu_context, menu);
        updateMenuItems(menu);
    }

    /**
     * {@inheritDoc}
     * 
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        menu.clear();

        final MenuInflater inflater = getMenuInflater();

        if (hasNormalMenu()) {
            inflater.inflate(R.menu.mainmenu, menu);
        } else {
            inflater.inflate(R.menu.mainmenu_context, menu);
        }

        return true;
    }

    protected boolean hasNormalMenu() {
        return AndroidVersion.lessThan4x || IUIManager.instance.isTabletUi(this) || AppSettings.current().showTitle;
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
     * @see org.emdev.ui.AbstractActionActivity#updateMenuItems(android.view.Menu)
     */
    @Override
    protected void updateMenuItems(final Menu menu) {
        final AppSettings as = AppSettings.current();

        ActionMenuHelper.setMenuItemChecked(menu, as.fullScreen, R.id.mainmenu_fullscreen);

        if (!AndroidVersion.lessThan3x) {
            ActionMenuHelper.setMenuItemChecked(menu, as.showTitle, R.id.mainmenu_showtitle);
        } else {
            ActionMenuHelper.setMenuItemVisible(menu, false, R.id.mainmenu_showtitle);
        }

        ActionMenuHelper
                .setMenuItemChecked(menu, getZoomControls().getVisibility() == View.VISIBLE, R.id.mainmenu_zoom);

        final BookSettings bs = getController().getBookSettings();
        if (bs == null) {
            return;
        }

        ActionMenuHelper.setMenuItemChecked(menu, bs.rotation == BookRotationType.PORTRAIT,
                R.id.mainmenu_force_portrait);
        ActionMenuHelper.setMenuItemChecked(menu, bs.rotation == BookRotationType.LANDSCAPE,
                R.id.mainmenu_force_landscape);
        ActionMenuHelper.setMenuItemChecked(menu, bs.nightMode, R.id.mainmenu_nightmode);
        ActionMenuHelper.setMenuItemChecked(menu, bs.cropPages, R.id.mainmenu_croppages);
        ActionMenuHelper.setMenuItemChecked(menu, bs.splitPages, R.id.mainmenu_splitpages,
                R.drawable.viewer_menu_split_pages, R.drawable.viewer_menu_split_pages_off);

        final DecodeService ds = getController().getDecodeService();

        final boolean cropSupported = ds.isFeatureSupported(CodecFeatures.FEATURE_CROP_SUPPORT);
        ActionMenuHelper.setMenuItemVisible(menu, cropSupported, R.id.mainmenu_croppages);
        ActionMenuHelper.setMenuItemVisible(menu, cropSupported, R.id.mainmenu_crop);

        final boolean splitSupported = ds.isFeatureSupported(CodecFeatures.FEATURE_SPLIT_SUPPORT);
        ActionMenuHelper.setMenuItemVisible(menu, splitSupported, R.id.mainmenu_splitpages);

        final MenuItem navMenu = menu.findItem(R.id.mainmenu_nav_menu);
        if (navMenu != null) {
            final SubMenu subMenu = navMenu.getSubMenu();
            subMenu.removeGroup(R.id.actions_goToBookmarkGroup);
            if (AppSettings.current().showBookmarksInMenu && LengthUtils.isNotEmpty(bs.bookmarks)) {
                for (final Bookmark b : bs.bookmarks) {
                    addBookmarkMenuItem(subMenu, b);
                }
            }
        }

    }

    protected void addBookmarkMenuItem(final Menu menu, final Bookmark b) {
        final MenuItem bmi = menu.add(R.id.actions_goToBookmarkGroup, R.id.actions_goToBookmark, Menu.NONE, b.name);
        bmi.setIcon(R.drawable.viewer_menu_bookmark);
        ActionMenuHelper.setMenuItemExtra(bmi, "bookmark", b);
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

    /**
     * {@inheritDoc}
     * 
     * @see android.app.Activity#dispatchKeyEvent(android.view.KeyEvent)
     */
    @Override
    public final boolean dispatchKeyEvent(final KeyEvent event) {
        view.checkFullScreenMode();
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_MENU) {
            if (!hasNormalMenu()) {
                getController().getOrCreateAction(R.id.actions_openOptionsMenu).run();
                return true;
            }
        }

        if (getController().dispatchKeyEvent(event)) {
            return true;
        }

        return super.dispatchKeyEvent(event);
    }

    public void showToastText(final int duration, final int resId, final Object... args) {
        Toast.makeText(getApplicationContext(), getResources().getString(resId, args), duration).show();
    }

}
