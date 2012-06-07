package org.ebookdroid.ui.library;

import org.ebookdroid.R;
import org.ebookdroid.common.log.LogContext;
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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.ImageView;
import android.widget.ViewFlipper;

import java.util.concurrent.atomic.AtomicLong;

import org.emdev.ui.AbstractActionActivity;
import org.emdev.ui.actions.ActionMethodDef;
import org.emdev.ui.actions.ActionTarget;
import org.emdev.utils.android.AndroidVersion;

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
    private ImageView libraryButton;
    private BookcaseView bookcaseView;
    private RecentBooksView recentBooksView;
    private LibraryView libraryView;

    public RecentActivity() {
        super();
        LCTX = LogContext.ROOT.lctx(this.getClass().getSimpleName(), true).lctx("" + SEQ.getAndIncrement(), true);
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

        setContentView(R.layout.recent);
        libraryButton = (ImageView) findViewById(R.id.recent_showlibrary);
        viewflipper = (ViewFlipper) findViewById(R.id.recentflip);

        RecentActivityController c = restoreController();
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
        if (LCTX.isDebugEnabled()) {
            LCTX.d("onDestroy(): " + isFinishing());
        }
        super.onDestroy();
        getController().onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.recentmenu, menu);
        return true;
    }

    @Override
    public void onCreateContextMenu(final ContextMenu menu, final View v, final ContextMenuInfo menuInfo) {
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
                source = ((ExpandableListAdapter) adapter).getChild(group, child);
            } else {
                source = ((ExpandableListAdapter) adapter).getGroup(group);
            }
        }

        if (source instanceof BookNode) {
            final BookNode node = (BookNode) source;

            final MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.book_menu, menu);

            menu.setHeaderTitle(node.path);
            menu.findItem(R.id.bookmenu_recentgroup).setVisible(node.settings != null);

        } else if (source instanceof BookShelfAdapter) {
            BookShelfAdapter a = (BookShelfAdapter) source;

            final MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.library_menu, menu);

            menu.setHeaderTitle(a.name);
        }

        setMenuSource(menu, source);
    }

    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            System.exit(0);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    void changeLibraryView(final int view) {
        if (view == VIEW_LIBRARY) {
            viewflipper.setDisplayedChild(VIEW_LIBRARY);
            libraryButton.setImageResource(R.drawable.actionbar_recent);
        } else {
            viewflipper.setDisplayedChild(VIEW_RECENT);
            libraryButton.setImageResource(R.drawable.actionbar_library);
        }
    }

    int getViewMode() {
        return viewflipper.getDisplayedChild();
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
        if (bookcaseView == null) {
            bookcaseView = new BookcaseView(getController(), bookshelfAdapter);
        }
        viewflipper.removeAllViews();
        viewflipper.addView(bookcaseView, 0);

        libraryButton.setImageResource(R.drawable.actionbar_shelf);
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

        viewflipper.removeAllViews();
        viewflipper.addView(recentBooksView, VIEW_RECENT);
        viewflipper.addView(libraryView, VIEW_LIBRARY);

        libraryButton.setImageResource(R.drawable.actionbar_library);
    }
}
