package org.ebookdroid.ui.library;

import org.ebookdroid.R;
import org.ebookdroid.common.settings.LibSettings;
import org.ebookdroid.common.settings.books.BookSettings;
import org.ebookdroid.common.settings.books.Bookmark;
import org.ebookdroid.core.PageIndex;
import org.ebookdroid.ui.library.adapters.BookNode;
import org.ebookdroid.ui.library.adapters.BookShelfAdapter;
import org.ebookdroid.ui.library.adapters.BooksAdapter;
import org.ebookdroid.ui.library.adapters.LibraryAdapter;
import org.ebookdroid.ui.library.adapters.RecentAdapter;
import org.ebookdroid.ui.library.views.BookcaseView;
import org.ebookdroid.ui.library.views.LibraryView;
import org.ebookdroid.ui.library.views.RecentBooksView;

import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
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
import android.widget.ImageView;
import android.widget.ViewFlipper;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.emdev.BaseDroidApp;
import org.emdev.common.android.AndroidVersion;
import org.emdev.common.filesystem.MediaManager;
import org.emdev.ui.AbstractActionActivity;
import org.emdev.ui.actions.ActionMenuHelper;
import org.emdev.ui.uimanager.IUIManager;
import org.emdev.utils.FileUtils;
import org.emdev.utils.LengthUtils;

public class RecentActivity extends AbstractActionActivity<RecentActivity, RecentActivityController> {

    public static final int VIEW_RECENT = 0;
    public static final int VIEW_LIBRARY = 1;

    private ViewFlipper viewflipper;

    ImageView libraryButton;
    BookcaseView bookcaseView;
    RecentBooksView recentBooksView;
    LibraryView libraryView;

    public RecentActivity() {
        super(true, ON_CREATE, ON_RESUME);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.emdev.ui.AbstractActionActivity#createController()
     */
    @Override
    protected RecentActivityController createController() {
        return new RecentActivityController(this);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.emdev.ui.AbstractActionActivity#onCreateImpl(android.os.Bundle)
     */
    @Override
    protected void onCreateImpl(final Bundle savedInstanceState) {
        IUIManager.instance.setTitleVisible(this, !AndroidVersion.lessThan3x, true);

        setContentView(R.layout.recent);

        if (AndroidVersion.lessThan3x) {
            // Old layout with custom title bar
            libraryButton = (ImageView) findViewById(R.id.recent_showlibrary);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.emdev.ui.AbstractActionActivity#onResumeImpl()
     */
    @Override
    protected void onResumeImpl() {
        IUIManager.instance.invalidateOptionsMenu(this);
    }

    /**
     * {@inheritDoc}
     *
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.recentmenu, menu);
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.emdev.ui.AbstractActionActivity#updateMenuItems(android.view.Menu)
     */
    @Override
    protected void updateMenuItems(final Menu menu) {
        final LibSettings ls = LibSettings.current();
        if (!ls.useBookcase) {
            final int viewMode = getViewMode();
            final boolean showLibraryAvailable = viewMode == RecentActivity.VIEW_RECENT;
            ActionMenuHelper.setMenuItemVisible(menu, showLibraryAvailable, R.id.recent_showlibrary);
            ActionMenuHelper.setMenuItemVisible(menu, !showLibraryAvailable, R.id.recent_showrecent);
        } else {
            ActionMenuHelper.setMenuItemVisible(menu, false, R.id.recent_showlibrary);
            ActionMenuHelper.setMenuItemVisible(menu, false, R.id.recent_showrecent);
        }

        ActionMenuHelper.setMenuItemExtra(menu, R.id.recent_storage_all, "path", "/");
        ActionMenuHelper.setMenuItemExtra(menu, R.id.recent_storage_external, "path", BaseDroidApp.EXT_STORAGE.getAbsolutePath());

        final MenuItem storageMenu = menu.findItem(R.id.recent_storage_menu);
        if (storageMenu != null) {
            final SubMenu subMenu = storageMenu.getSubMenu();
            subMenu.removeGroup(R.id.actions_storageGroup);

            final Set<String> added = new HashSet<String>();
            added.add("/");
            added.add(FileUtils.getCanonicalPath(BaseDroidApp.EXT_STORAGE));

            if (ls.showScanningInMenu) {
                for (final String path : ls.autoScanDirs) {
                    final File file = new File(path);
                    final String mp = FileUtils.getCanonicalPath(file);
                    if (mp != null && added.add(mp)) {
                        addStorageMenuItem(subMenu, R.drawable.recent_menu_storage_scanned, file.getPath(), path);
                    }
                }
            }
            if (ls.showRemovableMediaInMenu) {
                for (final String path : MediaManager.getReadableMedia()) {
                    final File file = new File(path);
                    final String mp = FileUtils.getCanonicalPath(file);
                    if (mp != null && added.add(mp)) {
                        addStorageMenuItem(subMenu, R.drawable.recent_menu_storage_external, file.getName(), path);
                    }
                }
            }
        }
    }

    protected void addStorageMenuItem(final Menu menu, final int resId, final String name, final String path) {
        final MenuItem bmi = menu.add(R.id.actions_storageGroup, R.id.actions_storage, Menu.NONE, name);
        bmi.setIcon(resId);
        ActionMenuHelper.setMenuItemExtra(bmi, "path", path);
    }

    /**
     * {@inheritDoc}
     *
     * @see android.app.Activity#onCreateContextMenu(android.view.ContextMenu, android.view.View, android.view.ContextMenu.ContextMenuInfo)
     */
    @Override
    public void onCreateContextMenu(final ContextMenu menu, final View v, final ContextMenuInfo menuInfo) {
        final Object source = getContextMenuSource(v, menuInfo);

        if (source instanceof BookNode) {
            onCreateBookMenu(menu, (BookNode) source);
        } else if (source instanceof BookShelfAdapter) {
            onCreateShelfMenu(menu, (BookShelfAdapter) source);
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

    protected void onCreateBookMenu(final ContextMenu menu, final BookNode node) {
        final BookSettings bs = node.settings;
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.book_menu, menu);

        menu.setHeaderTitle(node.path);
        menu.findItem(R.id.bookmenu_recentgroup).setVisible(bs != null);

        final BookShelfAdapter bookShelf = getController().getBookShelf(node);
        final BookShelfAdapter current = bookcaseView != null ? getController().getBookShelf(
                bookcaseView.getCurrentList()) : null;
        menu.findItem(R.id.bookmenu_openbookshelf).setVisible(
                bookShelf != null && current != null && bookShelf != current);

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

    protected void onCreateShelfMenu(final ContextMenu menu, final BookShelfAdapter a) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.library_menu, menu);
        menu.setHeaderTitle(a.name);
    }

    /**
     * {@inheritDoc}
     *
     * @see android.app.Activity#onKeyDown(int, android.view.KeyEvent)
     */
    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            getController().getOrCreateAction(R.id.mainmenu_close).run();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    void changeLibraryView(final int view) {
        final ViewFlipper vf = getViewflipper();
        if (view == VIEW_LIBRARY) {
            vf.setDisplayedChild(VIEW_LIBRARY);
            if (libraryButton != null) {
                libraryButton.setImageResource(R.drawable.recent_actionbar_recent);
            }
        } else {
            vf.setDisplayedChild(VIEW_RECENT);
            if (libraryButton != null) {
                libraryButton.setImageResource(R.drawable.recent_actionbar_library);
            }
        }
    }

    int getViewMode() {
        final ViewFlipper vf = getViewflipper();
        return vf != null ? vf.getDisplayedChild() : VIEW_RECENT;
    }

    void showBookshelf(final int shelfIndex) {
        if (bookcaseView != null) {
            bookcaseView.setCurrentList(shelfIndex);
        }
    }

    void showNextBookshelf() {
        if (bookcaseView != null) {
            bookcaseView.nextList();
        }
    }

    void showPrevBookshelf() {
        if (bookcaseView != null) {
            bookcaseView.prevList();
        }
    }

    void showBookcase(final BooksAdapter bookshelfAdapter, final RecentAdapter recentAdapter) {
        final ViewFlipper vf = getViewflipper();
        vf.removeAllViews();
        if (bookcaseView == null) {
            bookcaseView = (BookcaseView) LayoutInflater.from(this).inflate(R.layout.bookcase_view, vf, false);
            bookcaseView.init(bookshelfAdapter, recentAdapter);
        }
        vf.addView(bookcaseView, 0);

        if (libraryButton != null) {
            libraryButton.setImageResource(R.drawable.recent_actionbar_library);
        }
    }

    void showLibrary(final LibraryAdapter libraryAdapter, final RecentAdapter recentAdapter) {
        if (recentBooksView == null) {
            recentBooksView = new RecentBooksView(getController(), recentAdapter);
            registerForContextMenu(recentBooksView);
        }
        if (libraryView == null) {
            libraryView = new LibraryView(getController(), libraryAdapter);
            registerForContextMenu(libraryView);
        }

        final ViewFlipper vf = getViewflipper();
        vf.removeAllViews();
        vf.addView(recentBooksView, VIEW_RECENT);
        vf.addView(libraryView, VIEW_LIBRARY);

        if (libraryButton != null) {
            libraryButton.setImageResource(R.drawable.recent_actionbar_library);
        }

        if (recentAdapter.getCount() == 0) {
            changeLibraryView(VIEW_LIBRARY);
        }
    }

    ViewFlipper getViewflipper() {
        if (viewflipper == null) {
            viewflipper = (ViewFlipper) findViewById(R.id.recentflip);
        }

        return viewflipper;
    }
}
