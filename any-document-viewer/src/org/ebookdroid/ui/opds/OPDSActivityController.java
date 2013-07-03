package org.ebookdroid.ui.opds;

import org.ebookdroid.R;
import org.ebookdroid.opds.model.Book;
import org.ebookdroid.opds.model.Entry;
import org.ebookdroid.opds.model.Feed;
import org.ebookdroid.opds.model.Link;
import org.ebookdroid.ui.opds.adapters.FeedListener;
import org.ebookdroid.ui.opds.adapters.OPDSAdapter;

import android.annotation.TargetApi;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import org.emdev.ui.AbstractActivityController;
import org.emdev.ui.actions.ActionDialogBuilder;
import org.emdev.ui.actions.ActionEx;
import org.emdev.ui.actions.ActionMethod;
import org.emdev.ui.actions.IActionController;
import org.emdev.ui.actions.params.CheckableValue;
import org.emdev.ui.actions.params.Constant;
import org.emdev.ui.actions.params.EditableValue;
import org.emdev.ui.actions.params.EditableValue.PasswordEditable;
import org.emdev.utils.LengthUtils;

@TargetApi(8)
public class OPDSActivityController extends AbstractActivityController<OPDSActivity> implements
        ExpandableListView.OnGroupClickListener, ExpandableListView.OnChildClickListener, FeedListener {

    OPDSAdapter adapter;
    Feed current;

    public OPDSActivityController(final OPDSActivity activity) {
        super(activity, BEFORE_CREATE, AFTER_CREATE, ON_DESTROY);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.emdev.ui.AbstractActivityController#beforeCreate(android.app.Activity)
     */
    @Override
    public void beforeCreate(final OPDSActivity activity) {
        adapter = new OPDSAdapter(activity, this);
        adapter.addListener(this);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.emdev.ui.AbstractActivityController#afterCreate(android.app.Activity, boolean)
     */
    @Override
    public void afterCreate(final OPDSActivity activity, final boolean recreated) {
        if (recreated) {
            activity.onCurrrentFeedChanged(adapter.getCurrentFeed());
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.emdev.ui.AbstractActivityController#onDestroy(boolean)
     */
    @Override
    public void onDestroy(final boolean finishing) {
        if (finishing) {
            adapter.close();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.opds.adapters.FeedListener#feedLoaded(org.ebookdroid.opds.model.Feed)
     */
    @Override
    public void feedLoaded(final Feed feed) {
        getManagedComponent().onFeedLoaded(feed);
    }

    /**
     * {@inheritDoc}
     *
     * @see android.widget.ExpandableListView.OnGroupClickListener#onGroupClick(android.widget.ExpandableListView,
     *      android.view.View, int, long)
     */
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

    /**
     * {@inheritDoc}
     *
     * @see android.widget.ExpandableListView.OnChildClickListener#onChildClick(android.widget.ExpandableListView,
     *      android.view.View, int, int, long)
     */
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

    public void setCurrentFeed(final Feed feed) {
        adapter.setCurrentFeed(feed);
        getManagedComponent().onCurrrentFeedChanged(feed);
    }

    @ActionMethod(ids = R.id.opdsclose)
    public void close(final ActionEx action) {
        getManagedComponent().finish();
    }

    @ActionMethod(ids = R.id.opdshome)
    public void goHome(final ActionEx action) {
        setCurrentFeed(null);
    }

    @ActionMethod(ids = R.id.opdsgoto)
    public void goTo(final ActionEx action) {
        final Feed feed = action.getParameter("feed");
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

    @ActionMethod(ids = { R.id.opdsaddfeed, R.id.opds_feed_add })
    public void showAddFeedDlg(final ActionEx action) {

        final View childView = LayoutInflater.from(getManagedComponent()).inflate(R.layout.alias_url, null);

        final ActionDialogBuilder builder = new ActionDialogBuilder(getManagedComponent(), this);
        builder.setTitle(R.string.opds_addfeed_title);
        builder.setMessage(R.string.opds_addfeed_msg);
        builder.setView(childView);

        final EditText aliasEdit = (EditText) childView.findViewById(R.id.editAlias);
        final EditText urlEdit = (EditText) childView.findViewById(R.id.editURL);
        final CheckBox authCheck = (CheckBox) childView.findViewById(R.id.checkAuth);
        final TextView loginText = (TextView) childView.findViewById(R.id.textUsername);
        final EditText loginEdit = (EditText) childView.findViewById(R.id.editUsername);
        final TextView passwordText = (TextView) childView.findViewById(R.id.textPassword);
        final EditText passwordEdit = (EditText) childView.findViewById(R.id.editPassword);

        authCheck.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                loginText.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                loginEdit.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                passwordText.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                passwordEdit.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
        });

        authCheck.setChecked(false);

        builder.setPositiveButton(R.string.opds_addfeed_ok, R.id.actions_addFeed,
                new EditableValue("alias", aliasEdit), new EditableValue("url", urlEdit), new CheckableValue("auth",
                        authCheck), new EditableValue("login", loginEdit), new EditableValue("password", passwordEdit));
        builder.setNegativeButton();
        builder.show();
    }

    @ActionMethod(ids = R.id.actions_addFeed)
    public void addFeed(final ActionEx action) {
        final String alias = LengthUtils.toString(action.getParameter("alias"));
        final String url = LengthUtils.toString(action.getParameter("url"));
        if (LengthUtils.isAnyEmpty(alias, url)) {
            return;
        }

        final Boolean auth = action.getParameter("auth");
        if (auth) {
            final String login = action.getParameter("login").toString();
            final String password = ((PasswordEditable) action.getParameter("password")).getPassword();
            adapter.addFeed(alias, url, login, password);
        } else {
            adapter.addFeed(alias, url);
        }
    }

    @ActionMethod(ids = { R.id.opds_feed_edit })
    public void showEditFeedDlg(final ActionEx action) {
        final Feed feed = action.getParameter("feed");

        final View childView = LayoutInflater.from(getManagedComponent()).inflate(R.layout.alias_url, null);

        final ActionDialogBuilder builder = new ActionDialogBuilder(getManagedComponent(), this);
        builder.setTitle(R.string.opds_editfeed_title);
        builder.setMessage(R.string.opds_editfeed_msg);
        builder.setView(childView);

        final EditText aliasEdit = (EditText) childView.findViewById(R.id.editAlias);
        final EditText urlEdit = (EditText) childView.findViewById(R.id.editURL);
        final CheckBox authCheck = (CheckBox) childView.findViewById(R.id.checkAuth);
        final TextView loginText = (TextView) childView.findViewById(R.id.textUsername);
        final EditText loginEdit = (EditText) childView.findViewById(R.id.editUsername);
        final TextView passwordText = (TextView) childView.findViewById(R.id.textPassword);
        final EditText passwordEdit = (EditText) childView.findViewById(R.id.editPassword);

        authCheck.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                loginText.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                loginEdit.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                passwordText.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                passwordEdit.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
        });

        authCheck.setChecked(feed.login == null);
        authCheck.setChecked(feed.login != null);

        aliasEdit.setText(feed.title);
        urlEdit.setText(feed.id);
        loginEdit.setText(feed.login);
        passwordEdit.setText(feed.password);

        builder.setPositiveButton(R.string.opds_editfeed_ok, R.id.actions_editFeed, new EditableValue("alias",
                aliasEdit), new EditableValue("url", urlEdit), new CheckableValue("auth", authCheck),
                new EditableValue("login", loginEdit), new EditableValue("password", passwordEdit), new Constant(
                        "feed", feed));
        builder.setNegativeButton();
        builder.show();
    }

    @ActionMethod(ids = R.id.actions_editFeed)
    public void editFeed(final ActionEx action) {
        final Feed feed = action.getParameter("feed");
        final String alias = LengthUtils.toString(action.getParameter("alias"));
        final String url = LengthUtils.toString(action.getParameter("url"));
        final Boolean auth = action.getParameter("auth");
        final String login = auth ? action.getParameter("login").toString() : null;
        final String password = auth ? ((PasswordEditable) action.getParameter("password")).getPassword() : null;

        if (LengthUtils.isAllNotEmpty(alias, url)) {
            adapter.editFeed(feed, alias, url, login, password);
        }
    }

    @ActionMethod(ids = { R.id.opds_feed_delete })
    public void showDeleteFeedDlg(final ActionEx action) {
        final Feed feed = action.getParameter("feed");
        final ActionDialogBuilder builder = new ActionDialogBuilder(getManagedComponent(), this);
        builder.setTitle(R.string.opds_deletefeed_title);
        builder.setMessage(R.string.opds_deletefeed_msg);
        builder.setPositiveButton(R.string.opds_deletefeed_ok, R.id.actions_deleteFeed, new Constant("feed", feed));
        builder.setNegativeButton();
        builder.show();
    }

    @ActionMethod(ids = R.id.actions_deleteFeed)
    public void deleteFeed(final ActionEx action) {
        final Feed feed = action.getParameter("feed");
        adapter.removeFeed(feed);
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

        final String rawType = getManagedComponent().getResources().getString(R.string.opds_downloading_dlg_raw_type);

        if (link != null || book.downloads.size() == 1) {
            final Link target = link != null ? link : book.downloads.get(0);
            final ActionDialogBuilder builder = new ActionDialogBuilder(getManagedComponent(), this);
            builder.setTitle(R.string.opds_downloading_dlg_title);
            builder.setMessage(LengthUtils.safeString(target.type, rawType));
            builder.setPositiveButton(R.id.actions_downloadBook, new Constant("book", book), new Constant(
                    IActionController.DIALOG_ITEM_PROPERTY, 0));
            builder.setNegativeButton();
            builder.show();
            return;
        }

        final List<String> itemList = new ArrayList<String>();
        for (final Link l : book.downloads) {
            itemList.add(LengthUtils.safeString(l.type, rawType));
        }
        final String[] items = itemList.toArray(new String[itemList.size()]);

        final ActionDialogBuilder builder = new ActionDialogBuilder(getManagedComponent(), this);
        builder.setTitle(R.string.opds_downloading_type_dlg_title);
        builder.setItems(items, getOrCreateAction(R.id.actions_downloadBook).putValue("book", book));
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

}
