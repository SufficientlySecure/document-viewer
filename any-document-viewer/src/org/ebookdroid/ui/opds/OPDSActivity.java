package org.ebookdroid.ui.opds;

import org.ebookdroid.R;
import org.ebookdroid.opds.model.Book;
import org.ebookdroid.opds.model.Entry;
import org.ebookdroid.opds.model.Feed;
import org.ebookdroid.opds.model.Link;

import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;

import org.emdev.common.android.AndroidVersion;
import org.emdev.ui.AbstractActionActivity;
import org.emdev.ui.actions.ActionMenuHelper;
import org.emdev.ui.actions.params.Constant;
import org.emdev.ui.uimanager.IUIManager;

public class OPDSActivity extends AbstractActionActivity<OPDSActivity, OPDSActivityController> {

    ExpandableListView list;

    public OPDSActivity() {
        super(false, ON_CREATE);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.emdev.ui.AbstractActionActivity#createController()
     */
    @Override
    protected OPDSActivityController createController() {
        return new OPDSActivityController(this);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.emdev.ui.AbstractActionActivity#onCreateImpl(android.os.Bundle)
     */
    @Override
    protected void onCreateImpl(final Bundle savedInstanceState) {

        setContentView(R.layout.opds);
        setActionForView(R.id.opdsaddfeed);

        final OPDSActivityController c = getController();

        list = (ExpandableListView) findViewById(R.id.opdslist);
        list.setGroupIndicator(null);
        list.setChildIndicator(null);
        list.setOnGroupClickListener(c);
        list.setOnChildClickListener(c);
        list.setAdapter(c.adapter);

        this.registerForContextMenu(list);
    }

    /**
     * {@inheritDoc}
     *
     * @see android.app.Activity#onKeyDown(int, android.view.KeyEvent)
     */
    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            final Feed current = getController().adapter.getCurrentFeed();
            if (current == null) {
                finish();
            } else {
                getController().setCurrentFeed(current.parent);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
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
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @see android.app.Activity#onCreateContextMenu(android.view.ContextMenu, android.view.View, android.view.ContextMenu.ContextMenuInfo)
     */
    @Override
    public void onCreateContextMenu(final ContextMenu menu, final View v, final ContextMenuInfo menuInfo) {
        if (menuInfo instanceof ExpandableListContextMenuInfo) {
            final ExpandableListContextMenuInfo cmi = (ExpandableListContextMenuInfo) menuInfo;
            final int type = ExpandableListView.getPackedPositionType(cmi.packedPosition);
            final int groupPosition = ExpandableListView.getPackedPositionGroup(cmi.packedPosition);
            final int childPosition = ExpandableListView.getPackedPositionChild(cmi.packedPosition);
            // System.out.println("OPDSActivity.onCreateContextMenu(): " + type + ", " + groupPosition + ", "
            // + childPosition);
            switch (type) {
                case ExpandableListView.PACKED_POSITION_TYPE_NULL:
                    onCreateContextMenu(menu);
                    return;
                case ExpandableListView.PACKED_POSITION_TYPE_GROUP:
                    final Entry entry = getController().adapter.getGroup(groupPosition);
                    if (entry instanceof Feed) {
                        onCreateFeedContextMenu(menu, (Feed) entry);
                    } else if (entry instanceof Book) {
                        onCreateBookContextMenu(menu, (Book) entry);
                    }
                    return;
                case ExpandableListView.PACKED_POSITION_TYPE_CHILD:
                    final Entry group = getController().adapter.getGroup(groupPosition);
                    final Object child = getController().adapter.getChild(groupPosition, childPosition);
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

    protected void onCreateContextMenu(final ContextMenu menu) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.opds_defmenu, menu);

        final Feed feed = getController().adapter.getCurrentFeed();
        menu.setHeaderTitle(getFeedTitle(feed));
        updateMenuItems(menu, feed);
    }

    protected void onCreateFeedContextMenu(final ContextMenu menu, final Feed feed) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.opds_feedmenu, menu);

        menu.setHeaderTitle(getFeedTitle(feed));
        updateMenuItems(menu, feed.parent);

        ActionMenuHelper.setMenuParameters(getController(), menu, new Constant("feed", feed));
    }

    protected void onCreateFacetContextMenu(final ContextMenu menu, final Feed feed, final Feed facet) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.opds_facetmenu, menu);

        menu.setHeaderTitle(getFeedTitle(facet));
        updateMenuItems(menu, feed.parent);

        ActionMenuHelper.setMenuParameters(getController(), menu, new Constant("feed", facet));
    }

    protected void onCreateBookContextMenu(final ContextMenu menu, final Book book) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.opds_bookmenu, menu);

        menu.setHeaderTitle(book.title);
        ActionMenuHelper.setMenuParameters(getController(), menu, new Constant("book", book));
    }

    protected void onCreateLinkContextMenu(final ContextMenu menu, final Book book, final Link link) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.opds_bookmenu, menu);

        menu.setHeaderTitle(book.title);
        ActionMenuHelper.setMenuParameters(getController(), menu, new Constant("book", book),
                new Constant("link", link));
    }

    /**
     * {@inheritDoc}
     *
     * @see org.emdev.ui.AbstractActionActivity#updateMenuItems(android.view.Menu)
     */
    @Override
    protected void updateMenuItems(final Menu menu) {
        updateMenuItems(menu, getController().adapter.getCurrentFeed());
    }

    protected void updateMenuItems(final Menu menu, final Feed feed) {
        final boolean canUp = feed != null;
        final boolean canNext = feed != null && feed.next != null;
        final boolean canPrev = feed != null && feed.prev != null;

        if (menu != null) {
            if (AndroidVersion.lessThan3x) {
                ActionMenuHelper.setMenuItemEnabled(menu, canUp, R.id.opdsupfolder,
                        R.drawable.opds_menu_nav_up_enabled, R.drawable.opds_menu_nav_up_disabled);
                ActionMenuHelper.setMenuItemEnabled(menu, canNext, R.id.opdsnextfolder,
                        R.drawable.opds_menu_nav_next_enabled, R.drawable.opds_menu_nav_next_disabled);
                ActionMenuHelper.setMenuItemEnabled(menu, canPrev, R.id.opdsprevfolder,
                        R.drawable.opds_menu_nav_prev_enabled, R.drawable.opds_menu_nav_prev_disabled);
            } else {
                ActionMenuHelper.setMenuItemEnabled(menu, canUp, R.id.opdsupfolder,
                        R.drawable.opds_actionbar_nav_up_enabled, R.drawable.opds_actionbar_nav_up_disabled);
                ActionMenuHelper.setMenuItemEnabled(menu, canNext, R.id.opdsnextfolder,
                        R.drawable.opds_actionbar_nav_next_enabled, R.drawable.opds_actionbar_nav_next_disabled);
                ActionMenuHelper.setMenuItemEnabled(menu, canPrev, R.id.opdsprevfolder,
                        R.drawable.opds_actionbar_nav_prev_enabled, R.drawable.opds_actionbar_nav_prev_disabled);
            }
        }
    }

    protected String getFeedTitle(final Feed feed) {
        return feed != null ? feed.title : getResources().getString(R.string.opds);
    }

    protected void onCurrrentFeedChanged(final Feed feed) {
        IUIManager.instance.invalidateOptionsMenu(this);
        setTitle(getFeedTitle(feed));
        findViewById(R.id.opdsaddfeed).setVisibility(feed != null ? View.GONE : View.VISIBLE);
    }

    protected void onFeedLoaded(final Feed feed) {
        IUIManager.instance.invalidateOptionsMenu(this);
    }
}
