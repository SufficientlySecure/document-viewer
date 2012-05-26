package org.ebookdroid.ui.opds;

import org.ebookdroid.R;
import org.ebookdroid.opds.Book;
import org.ebookdroid.opds.Entry;
import org.ebookdroid.opds.Feed;
import org.ebookdroid.opds.Link;
import org.ebookdroid.ui.opds.adapters.OPDSAdapter;

import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;

import java.util.ArrayList;
import java.util.List;

import org.emdev.ui.AbstractActionActivity;
import org.emdev.ui.actions.ActionController;
import org.emdev.ui.actions.ActionDialogBuilder;
import org.emdev.ui.actions.ActionEx;
import org.emdev.ui.actions.ActionMethod;
import org.emdev.ui.actions.IActionController;
import org.emdev.ui.actions.params.Constant;
import org.emdev.ui.actions.params.EditableValue;
import org.emdev.utils.LengthUtils;

public class OPDSActivity extends AbstractActionActivity<OPDSActivity, ActionController<OPDSActivity>> implements
        ExpandableListView.OnGroupClickListener, ExpandableListView.OnChildClickListener, OPDSAdapter.FeedListener {

    private OPDSAdapter adapter;

    private ExpandableListView list;

    private Menu optionsMenu;

    public OPDSActivity() {
    }

    @Override
    protected ActionController<OPDSActivity> createController() {
        return new ActionController<OPDSActivity>(this);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.opds);
        setActionForView(R.id.opdsaddfeed);

        list = (ExpandableListView) findViewById(R.id.opdslist);
        list.setOnGroupClickListener(this);
        list.setOnChildClickListener(this);

        list.setGroupIndicator(null);
        list.setChildIndicator(null);

        adapter = new OPDSAdapter(this);
        adapter.addListener(this);
        list.setAdapter(adapter);

        this.registerForContextMenu(list);
    }

    @Override
    protected void onPostCreate(final Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        goHome(null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        adapter.close();
    }

    /**
     * {@inheritDoc}
     * 
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.opdsmenu, menu);

        this.optionsMenu = menu;
        updateNavigation(optionsMenu, adapter.getCurrentFeed());
        return true;
    }

    @Override
    public boolean onMenuOpened(final int featureId, final Menu menu) {
        this.optionsMenu = menu;
        updateNavigation(optionsMenu, adapter.getCurrentFeed());
        return super.onMenuOpened(featureId, menu);
    }

    @Override
    public void onCreateContextMenu(final ContextMenu menu, final View v, final ContextMenuInfo menuInfo) {
        if (menuInfo instanceof ExpandableListContextMenuInfo) {
            final ExpandableListContextMenuInfo cmi = (ExpandableListContextMenuInfo) menuInfo;
            final int type = ExpandableListView.getPackedPositionType(cmi.packedPosition);
            final int groupPosition = ExpandableListView.getPackedPositionGroup(cmi.packedPosition);
            final int childPosition = ExpandableListView.getPackedPositionChild(cmi.packedPosition);
            System.out.println("OPDSActivity.onCreateContextMenu(): " + type + ", " + groupPosition + ", "
                    + childPosition);
            switch (type) {
                case ExpandableListView.PACKED_POSITION_TYPE_NULL:
                    onCreateContextMenu(menu);
                    return;
                case ExpandableListView.PACKED_POSITION_TYPE_GROUP:
                    final Entry entry = adapter.getGroup(groupPosition);
                    if (entry instanceof Feed) {
                        onCreateFeedContextMenu(menu, (Feed) entry);
                    } else if (entry instanceof Book) {
                        onCreateBookContextMenu(menu, (Book) entry);
                    }
                    return;
                case ExpandableListView.PACKED_POSITION_TYPE_CHILD:
                    final Entry group = adapter.getGroup(groupPosition);
                    final Object child = adapter.getChild(groupPosition, childPosition);
                    if (child instanceof Link) {
                        onCreateLinkContextMenu(menu, (Book) group, (Link) child);
                    } else if (child instanceof Feed) {
                        onCreateFacetContextMenu(menu, (Feed) group, (Feed) child);
                    }
                    return;
            }
        }
        onCreateContextMenu(menu);
    }

    private void onCreateContextMenu(final ContextMenu menu) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.opds_defmenu, menu);

        Feed feed = adapter.getCurrentFeed();
        menu.setHeaderTitle(getFeedTitle(feed));
        updateNavigation(menu, feed);
    }

    private void onCreateFeedContextMenu(final ContextMenu menu, final Feed feed) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.opds_feedmenu, menu);

        menu.setHeaderTitle(getFeedTitle(feed));
        updateNavigation(menu, feed.parent);

        getController().getOrCreateAction(R.id.opdsgoto).putValue("feed", feed);
    }

    private void onCreateFacetContextMenu(final ContextMenu menu, final Feed feed, final Feed facet) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.opds_facetmenu, menu);

        menu.setHeaderTitle(getFeedTitle(facet));
        updateNavigation(menu, feed.parent);

        getController().getOrCreateAction(R.id.opdsgoto).putValue("feed", facet);
    }

    private void onCreateBookContextMenu(final ContextMenu menu, final Book book) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.opds_bookmenu, menu);

        menu.setHeaderTitle(book.title);
        getController().getOrCreateAction(R.id.opds_book_download).putValue("book", book).putValue("link", null);
    }

    private void onCreateLinkContextMenu(final ContextMenu menu, final Book book, final Link link) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.opds_bookmenu, menu);

        menu.setHeaderTitle(book.title);
        getController().getOrCreateAction(R.id.opds_book_download).putValue("book", book).putValue("link", null);
    }

    public void setCurrentFeed(final Feed feed) {
        updateNavigation(optionsMenu, feed);

        setTitle(getFeedTitle(feed));
        findViewById(R.id.opdsaddfeed).setVisibility(feed != null ? View.GONE : View.VISIBLE);
        adapter.setCurrentFeed(feed);
    }

    private String getFeedTitle(final Feed feed) {
        return feed != null ? feed.title : getResources().getString(R.string.opds);
    }

    @ActionMethod(ids = { R.id.opdsaddfeed, R.id.opds_feed_add })
    public void showAddFeedDlg(final ActionEx action) {

        final View childView = LayoutInflater.from(this).inflate(R.layout.alias_url, null);

        final ActionDialogBuilder builder = new ActionDialogBuilder(this, getController());
        builder.setTitle(R.string.opds_addfeed_title);
        builder.setMessage(R.string.opds_addfeed_msg);
        builder.setView(childView);

        final EditText aliasEdit = (EditText) childView.findViewById(R.id.editAlias);
        final EditText urlEdit = (EditText) childView.findViewById(R.id.editURL);

        builder.setPositiveButton(R.string.opds_addfeed_ok, R.id.actions_addFeed,
                new EditableValue("alias", aliasEdit), new EditableValue("url", urlEdit));
        builder.setNegativeButton();
        builder.show();
    }

    @ActionMethod(ids = R.id.actions_addFeed)
    public void addFeed(final ActionEx action) {
        final String alias = LengthUtils.toString(action.getParameter("alias"));
        final String url = LengthUtils.toString(action.getParameter("url"));

        if (LengthUtils.isAllNotEmpty(alias, url)) {
            adapter.addFeed(alias, url);
        }
    }

    @ActionMethod(ids = R.id.opdsclose)
    public void close(final ActionEx action) {
        finish();
    }

    @ActionMethod(ids = R.id.opdshome)
    public void goHome(final ActionEx action) {
        setCurrentFeed(null);
    }

    @ActionMethod(ids = R.id.opdsgoto)
    public void goTo(final ActionEx action) {
        Feed feed = action.getParameter("feed");
        setCurrentFeed(feed);
    }

    @ActionMethod(ids = R.id.opdsupfolder)
    public void goUp(final ActionEx action) {
        final Feed dir = adapter.getCurrentFeed();
        final Feed parent = dir != null ? dir.parent : null;
        setCurrentFeed(parent);
    }

    @ActionMethod(ids = R.id.opdsnextfolder)
    public void goNext(final ActionEx action) {
        final Feed dir = adapter.getCurrentFeed();
        final Feed next = dir != null ? dir.next : null;
        if (next != null) {
            setCurrentFeed(next);
        }
    }

    @ActionMethod(ids = R.id.opdsprevfolder)
    public void goPrev(final ActionEx action) {
        final Feed dir = adapter.getCurrentFeed();
        final Feed prev = dir != null ? dir.prev : null;
        if (prev != null) {
            setCurrentFeed(prev);
        }
    }

    @ActionMethod(ids = R.id.opdsrefreshfolder)
    public void refresh(final ActionEx action) {
        final Feed dir = adapter.getCurrentFeed();
        if (dir != null) {
            setCurrentFeed(dir);
        }
    }

    @Override
    public void feedLoaded(final Feed feed) {
        updateNavigation(optionsMenu, feed);
    }

    @Override
    public boolean onGroupClick(final ExpandableListView parent, final View v, final int groupPosition, final long id) {
        if (adapter.getChildrenCount(groupPosition) > 0) {
            return false;
        }
        final Entry group = adapter.getGroup(groupPosition);
        if (group instanceof Feed) {
            setCurrentFeed((Feed) group);
            return true;
        } else if (group instanceof Book) {
            showDownloadDlg((Book) group, null);
            return true;
        }
        return false;
    }

    @Override
    public boolean onChildClick(final ExpandableListView parent, final View v, final int groupPosition,
            final int childPosition, final long id) {
        final Entry group = adapter.getGroup(groupPosition);
        final Object child = adapter.getChild(groupPosition, childPosition);
        if (child instanceof Feed) {
            setCurrentFeed((Feed) child);
        } else if (child instanceof Link) {
            showDownloadDlg((Book) group, (Link) child);
        }

        return true;
    }

    @ActionMethod(ids = R.id.opds_book_download)
    public void showDownloadDlg(final ActionEx action) {
        final Book book = action.getParameter("book");
        final Link link = action.getParameter("link");
        showDownloadDlg(book, link);
    }

    protected void showDownloadDlg(final Book book, final Link link) {
        if (LengthUtils.isEmpty(book.downloads)) {
            return;
        }

        if (link != null || book.downloads.size() == 1) {
            final Link target = link != null ? link : book.downloads.get(0);
            final ActionDialogBuilder builder = new ActionDialogBuilder(this, getController());
            builder.setTitle("Downloading book as");
            builder.setMessage(LengthUtils.safeString(target.type, "Raw type"));
            builder.setPositiveButton(R.id.actions_downloadBook, new Constant("book", book), new Constant(
                    IActionController.DIALOG_ITEM_PROPERTY, 0));
            builder.setNegativeButton();
            builder.show();
            return;
        }

        final List<String> itemList = new ArrayList<String>();
        for (final Link l : book.downloads) {
            itemList.add(LengthUtils.safeString(l.type, "Raw type"));
        }
        final String[] items = itemList.toArray(new String[itemList.size()]);

        final ActionDialogBuilder builder = new ActionDialogBuilder(this, getController());
        builder.setTitle("Select type of book to download");
        builder.setItems(items, this.getController().getOrCreateAction(R.id.actions_downloadBook)
                .putValue("book", book));
        builder.show();
    }

    @ActionMethod(ids = R.id.actions_downloadBook)
    public void doDownload(final ActionEx action) {
        final Book book = action.getParameter("book");
        final Integer index = action.getParameter(IActionController.DIALOG_ITEM_PROPERTY);
        if (book != null && index != null) {
            adapter.downloadBook(book, index.intValue());
        }
    }

    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            final Feed current = adapter.getCurrentFeed();
            if (current == null) {
                finish();
            } else {
                setCurrentFeed(current.parent);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    protected void updateNavigation(final Menu menu, final Feed feed) {
        final boolean canUp = feed != null;
        final boolean canNext = feed != null && feed.next != null;
        final boolean canPrev = feed != null && feed.prev != null;

        if (menu != null) {
            updateItem(menu, canUp, R.id.opdsupfolder, R.drawable.arrowup_enabled, R.drawable.arrowup_disabled);
            updateItem(menu, canNext, R.id.opdsnextfolder, R.drawable.arrowright_enabled,
                    R.drawable.arrowright_disabled);
            updateItem(menu, canPrev, R.id.opdsprevfolder, R.drawable.arrowleft_enabled, R.drawable.arrowleft_disabled);
        }
    }

    protected void updateItem(final Menu menu, final boolean enabled, final int viewId, final int enabledResId,
            final int disabledResId) {
        final MenuItem v = menu.findItem(viewId);
        if (v != null) {
            v.setIcon(enabled ? enabledResId : disabledResId);
            v.setEnabled(enabled);
        }
    }

}
