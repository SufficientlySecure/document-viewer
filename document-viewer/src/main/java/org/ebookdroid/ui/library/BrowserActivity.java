package org.ebookdroid.ui.library;

import org.emdev.ui.uimanager.UIManagerAppCompat;
import org.sufficientlysecure.viewer.R;
import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.common.settings.books.BookSettings;
import org.ebookdroid.common.settings.books.Bookmark;
import org.ebookdroid.core.PageIndex;
import org.ebookdroid.ui.library.views.FileBrowserView;

import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.ViewFlipper;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.emdev.ui.AbstractActionActivity;
import org.emdev.ui.actions.ActionMenuHelper;
import org.emdev.utils.LayoutUtils;
import org.emdev.utils.LengthUtils;

public class BrowserActivity extends AbstractActionActivity<BrowserActivity, BrowserActivityController> {

    private static final String CURRENT_DIRECTORY = "currentDirectory";

    ViewFlipper viewflipper;

    public BrowserActivity() {
        super(false, ON_CREATE);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.emdev.ui.AbstractActionActivity#createController()
     */
    @Override
    protected BrowserActivityController createController() {
        return new BrowserActivityController(this);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.emdev.ui.AbstractActionActivity#onCreateImpl(android.os.Bundle)
     */
    @Override
    protected void onCreateImpl(final Bundle savedInstanceState) {
        setContentView(R.layout.browser);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final BrowserActivityController c = getController();

        viewflipper = (ViewFlipper) findViewById(R.id.browserflip);
        viewflipper.addView(LayoutUtils.fillInParent(viewflipper, new FileBrowserView(c, c.adapter)));
    }

    /**
     * {@inheritDoc}
     *
     * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
     */
    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(CURRENT_DIRECTORY, getController().adapter.getCurrentDirectory().getAbsolutePath());
    }

    /**
     * {@inheritDoc}
     *
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.browsermenu, menu);
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.emdev.ui.AbstractActionActivity#updateMenuItems(android.view.Menu)
     */
    @Override
    protected void updateMenuItems(final Menu optionsMenu) {

        final File dir = getController().adapter.getCurrentDirectory();
        final boolean hasParent = dir != null ? dir.getParentFile() != null : false;

        ActionMenuHelper.setMenuItemEnabled(optionsMenu, hasParent, R.id.browserupfolder,
                R.drawable.browser_actionbar_nav_up_enabled, R.drawable.browser_actionbar_nav_up_disabled);
    }

    void setTitle(final File dir) {
        final String path = dir.getAbsolutePath();
        setTitle(path);
        UIManagerAppCompat.invalidateOptionsMenu(this);
    }

    /**
     * {@inheritDoc}
     *
     * @see android.app.Activity#onKeyUp(int, android.view.KeyEvent)
     */
    @Override
    public boolean onKeyUp(final int keyCode, final KeyEvent event) {
        if (getController().onKeyUp(keyCode, event)) {
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    public void showProgress(final boolean show) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                UIManagerAppCompat.setProgressSpinnerVisible(BrowserActivity.this, show);
            }
        });
    }

    /**
     * {@inheritDoc}
     *
     * @see android.app.Activity#onCreateContextMenu(android.view.ContextMenu, android.view.View, android.view.ContextMenu.ContextMenuInfo)
     */
    @Override
    public void onCreateContextMenu(final ContextMenu menu, final View v, final ContextMenuInfo menuInfo) {
        final Object source = getContextMenuSource(v, menuInfo);

        if (source instanceof File) {
            final File node = (File) source;
            final String path = node.getAbsolutePath();

            if (node.isDirectory()) {
                createFolderMenu(menu, path);
            } else {
                createFileMenu(menu, path);
            }
        }

        ActionMenuHelper.setMenuSource(getController(), menu, source);
    }

    protected Object getContextMenuSource(final View v, final ContextMenuInfo menuInfo) {
        Object source = null;

        if (menuInfo instanceof AdapterContextMenuInfo) {
            final AbsListView list = (AbsListView) v;
            final AdapterContextMenuInfo mi = (AdapterContextMenuInfo) menuInfo;
            source = list.getAdapter().getItem(mi.position);
        } else if (menuInfo instanceof ExpandableListContextMenuInfo) {
            final ExpandableListView list = (ExpandableListView) v;
            final ExpandableListAdapter adapter = list.getExpandableListAdapter();
            final ExpandableListContextMenuInfo mi = (ExpandableListContextMenuInfo) menuInfo;
            final long pp = mi.packedPosition;
            final int group = ExpandableListView.getPackedPositionGroup(pp);
            final int child = ExpandableListView.getPackedPositionChild(pp);
            if (child >= 0) {
                source = adapter.getChild(group, child);
            } else {
                source = adapter.getGroup(group);
            }
        }
        return source;
    }

    protected void createFileMenu(final ContextMenu menu, final String path) {
        final BookSettings bs = SettingsManager.getBookSettings(path);
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.book_menu, menu);
        menu.setHeaderTitle(path);
        menu.findItem(R.id.bookmenu_recentgroup).setVisible(bs != null);
        menu.findItem(R.id.bookmenu_openbookshelf).setVisible(false);
        menu.findItem(R.id.bookmenu_openbookfolder).setVisible(false);

        final MenuItem om = menu.findItem(R.id.bookmenu_open);
        final SubMenu osm = om != null ? om.getSubMenu() : null;
        if (osm == null) {
            return;
        }
        osm.clear();

        final List<Bookmark> list = new ArrayList<Bookmark>();
        list.add(new Bookmark(true, getString(R.string.bookmark_start), PageIndex.FIRST, 0, 0));
        list.add(new Bookmark(true, getString(R.string.bookmark_end), PageIndex.LAST, 0, 1));
        if (bs != null) {
            if (LengthUtils.isNotEmpty(bs.bookmarks)) {
                list.addAll(bs.bookmarks);
            }
            list.add(new Bookmark(true, getString(R.string.bookmark_current), bs.currentPage, bs.offsetX, bs.offsetY));
        }

        Collections.sort(list);
        for (final Bookmark b : list) {
            addBookmarkMenuItem(osm, b);
        }
    }

    protected void addBookmarkMenuItem(final Menu menu, final Bookmark b) {
        final MenuItem bmi = menu.add(R.id.actions_goToBookmarkGroup, R.id.actions_goToBookmark, Menu.NONE, b.name);
        bmi.setIcon(R.drawable.viewer_menu_bookmark);
        ActionMenuHelper.setMenuItemExtra(bmi, "bookmark", b);
    }

    protected void createFolderMenu(final ContextMenu menu, final String path) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.library_menu, menu);
        menu.setHeaderTitle(path);
    }
}
