package org.ebookdroid.ui.library;

import org.ebookdroid.EBookDroidApp;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.emdev.common.android.AndroidVersion;
import org.emdev.common.log.LogContext;
import org.emdev.common.log.LogManager;
import org.emdev.ui.AbstractActionActivity;
import org.emdev.ui.actions.ActionMethodDef;
import org.emdev.ui.actions.ActionTarget;
import org.emdev.ui.uimanager.IUIManager;
import org.emdev.utils.LengthUtils;

@ActionTarget(
// actions
actions = {
// start
@ActionMethodDef(id = R.id.mainmenu_about, method = "showAbout")
// finish
})
public class RecentActivity extends AbstractActionActivity<RecentActivity, RecentActivityController> {

    public final LogContext LCTX;

    private static final AtomicLong SEQ = new AtomicLong();

    public static final int VIEW_RECENT = 0;
    public static final int VIEW_LIBRARY = 1;

    private ViewFlipper viewflipper;

    ImageView libraryButton;
    BookcaseView bookcaseView;
    RecentBooksView recentBooksView;
    LibraryView libraryView;

    public RecentActivity() {
        super();
        LCTX = LogManager.root().lctx(this.getClass().getSimpleName(), true).lctx("" + SEQ.getAndIncrement(), true);
    }

    @Override
    protected RecentActivityController createController() {
        return new RecentActivityController(this);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        if (LCTX.isDebugEnabled()) {
            LCTX.d("onCreate()");
        }
        super.onCreate(savedInstanceState);
        if (!isTaskRoot()) {
            // Workaround for Android 2.1-
            if (LCTX.isDebugEnabled()) {
                LCTX.d("onCreate(): close duplicated activity");
            }
            finish();
            return;
        }

        IUIManager.instance.setTitleVisible(this, !AndroidVersion.lessThan3x, true);

        setContentView(R.layout.recent);

        if (AndroidVersion.lessThan3x) {
            // Old layout with custom title bar
            libraryButton = (ImageView) findViewById(R.id.recent_showlibrary);
        }

        final RecentActivityController c = restoreController();
        if (c != null) {
            c.onRestore(this);
        } else {
            getController().onCreate();
        }

        if (AndroidVersion.VERSION == 3) {
            setActionForView(R.id.recent_showlibrary);
            setActionForView(R.id.recent_showbrowser);
            setActionForView(R.id.ShelfLeftButton);
            setActionForView(R.id.ShelfCaption);
            setActionForView(R.id.ShelfRightButton);
        }
    }

    @Override
    protected void onResume() {
        if (LCTX.isDebugEnabled()) {
            LCTX.d("onResume()");
        }
        super.onResume();
        IUIManager.instance.invalidateOptionsMenu(this);
        getController().onResume();
    }

    @Override
    protected void onPause() {
        if (LCTX.isDebugEnabled()) {
            LCTX.d("onPause(): " + isFinishing());
        }
        super.onPause();
        getController().onPause();
    }

    @Override
    protected void onDestroy() {
        final boolean finishing = isFinishing();
        if (LCTX.isDebugEnabled()) {
            LCTX.d("onDestroy(): " + finishing);
        }
        super.onDestroy();
        if (!isTaskRoot()) {
            LCTX.d("onDestroy(): close duplicated activity");
            return;
        }
        getController().onDestroy(finishing);

        EBookDroidApp.onActivityClose(finishing);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.recentmenu, menu);
        return true;
    }

    @Override
    protected void updateMenuItems(final Menu menu) {
        if (!LibSettings.current().getUseBookcase()) {
            final int viewMode = getViewMode();
            final boolean showLibraryAvailable = viewMode == RecentActivity.VIEW_RECENT;
            setMenuItemVisible(menu, showLibraryAvailable, R.id.recent_showlibrary);
            setMenuItemVisible(menu, !showLibraryAvailable, R.id.recent_showrecent);
        } else {
            setMenuItemVisible(menu, false, R.id.recent_showlibrary);
            setMenuItemVisible(menu, false, R.id.recent_showrecent);
        }
    }

    @Override
    public void onCreateContextMenu(final ContextMenu menu, final View v, final ContextMenuInfo menuInfo) {
        final Object source = getContextMenuSource(v, menuInfo);

        if (source instanceof BookNode) {
            onCreateBookMenu(menu, (BookNode) source);
        } else if (source instanceof BookShelfAdapter) {
            onCreateShelfMenu(menu, (BookShelfAdapter) source);
        }

        setMenuSource(menu, source);
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
        setMenuItemExtra(bmi, "bookmark", b);
    }

    protected void onCreateShelfMenu(final ContextMenu menu, final BookShelfAdapter a) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.library_menu, menu);
        menu.setHeaderTitle(a.name);
    }

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
            bookcaseView.init(bookshelfAdapter);
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
    }

    ViewFlipper getViewflipper() {
        if (viewflipper == null) {
            viewflipper = (ViewFlipper) findViewById(R.id.recentflip);
        }

        return viewflipper;
    }
}
