package org.ebookdroid.ui.opds.adapters;

import org.ebookdroid.R;
import org.ebookdroid.common.cache.CacheManager;
import org.ebookdroid.common.cache.ThumbnailFile;
import org.ebookdroid.common.settings.OpdsSettings;
import org.ebookdroid.opds.ExtentedEntryBuilder;
import org.ebookdroid.opds.OPDSClient;
import org.ebookdroid.opds.exceptions.AuthorizationRequiredException;
import org.ebookdroid.opds.exceptions.OPDSException;
import org.ebookdroid.opds.model.Book;
import org.ebookdroid.opds.model.BookDownloadLink;
import org.ebookdroid.opds.model.Entry;
import org.ebookdroid.opds.model.Feed;
import org.ebookdroid.opds.model.Link;

import android.annotation.TargetApi;
import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.BaseExpandableListAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import org.emdev.ui.actions.ActionController;
import org.emdev.ui.actions.ActionDialogBuilder;
import org.emdev.ui.actions.ActionEx;
import org.emdev.ui.actions.ActionMethod;
import org.emdev.ui.actions.IActionController;
import org.emdev.ui.actions.params.Constant;
import org.emdev.ui.actions.params.EditableValue;
import org.emdev.ui.actions.params.EditableValue.PasswordEditable;
import org.emdev.ui.adapters.BaseViewHolder;
import org.emdev.ui.widget.TextViewMultilineEllipse;
import org.emdev.utils.LengthUtils;
import org.emdev.utils.listeners.ListenerProxy;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@TargetApi(8)
public class OPDSAdapter extends BaseExpandableListAdapter {

    final Context context;
    final IActionController<?> actions;

    final OPDSClient client;
    final List<Feed> rootFeeds;

    volatile Feed currentFeed;

    final ListenerProxy listeners = new ListenerProxy(FeedListener.class);

    final OPDSTaskExecutor executor = new OPDSTaskExecutor(this);

    public OPDSAdapter(final Context context, final IActionController<?> actions) {
        this.context = context;
        this.actions = new ActionController<OPDSAdapter>(actions, this);
        this.client = new OPDSClient(new ExtentedEntryBuilder());

        this.rootFeeds = new ArrayList<Feed>();

        final JSONArray feeds = OpdsSettings.current().opdsCatalogs;
        for (int i = 0, n = feeds.length(); i < n; i++) {
            try {
                final JSONObject obj = feeds.getJSONObject(i);
                final String alias = obj.getString("alias");
                final String url = obj.getString("url");
                final String login = obj.optString("login");
                final String password = obj.optString("password");
                if (LengthUtils.isAllNotEmpty(alias, url)) {
                    rootFeeds.add(new Feed(alias, url, login, password));
                }
            } catch (final JSONException ex) {
                ex.printStackTrace();
            }
        }

        // TODO remove in release
        if (rootFeeds.isEmpty()) {
            addFeeds(new Feed("Flibusta", "http://flibusta.net/opds"), new Feed("Plough",
                    "http://www.plough.com/ploughCatalog_opds.xml"));
        }

        this.currentFeed = null;
    }

    protected void store() {
        final JSONArray catalogs = new JSONArray();
        for (final Feed feed : rootFeeds) {
            try {
                final JSONObject newCatalog = new JSONObject();
                newCatalog.put("alias", feed.title);
                newCatalog.put("url", feed.link.uri);
                if (feed.login != null) {
                    newCatalog.put("login", feed.login);
                    newCatalog.put("password", feed.password);
                }
                catalogs.put(newCatalog);
            } catch (final JSONException ex) {
                ex.printStackTrace();
            }
        }
        OpdsSettings.changeOpdsCatalogs(catalogs);
    }

    @Override
    protected void finalize() {
        close();
    }

    public void close() {
        client.close();
    }

    public void addFeed(final String alias, final String url) {
        addFeeds(new Feed(alias, url));
    }

    public void addFeed(final String alias, final String url, final String login, final String password) {
        addFeeds(new Feed(alias, url, login, password));
    }

    public void addFeeds(final Feed... feeds) {
        for (final Feed feed : feeds) {
            rootFeeds.add(feed);
        }
        store();
        if (currentFeed == null) {
            notifyDataSetChanged();
        }
    }

    public void editFeed(final Feed feed, final String alias, final String url, final String login,
            final String password) {
        if (feed.id.equals(url)) {
            feed.title = alias;
            feed.login = login;
            feed.password = password;
            store();
            if (currentFeed == null) {
                notifyDataSetInvalidated();
            }
            return;
        }
        final Feed newFeed = new Feed(alias, url, login, password);
        final int index = rootFeeds.indexOf(feed);
        if (index == -1) {
            rootFeeds.add(newFeed);
        } else {
            rootFeeds.set(index, newFeed);
        }
        store();
        if (currentFeed == null) {
            if (index == -1) {
                notifyDataSetChanged();
            } else {
                notifyDataSetInvalidated();
            }
        }
    }

    public void removeFeed(final Feed feed) {
        if (rootFeeds.remove(feed)) {
            store();
            notifyDataSetChanged();
        }
    }

    public void setCurrentFeed(final Feed feed) {
        if (feed != null && feed == this.currentFeed) {
            feed.books.clear();
            feed.children.clear();
            feed.loadedAt = 0;
        }
        this.currentFeed = feed;

        notifyDataSetInvalidated();

        executor.startLoadFeed(feed);
    }

    public Feed getCurrentFeed() {
        return currentFeed;
    }

    @Override
    public int getGroupCount() {
        if (currentFeed == null) {
            return rootFeeds.size();
        }
        return currentFeed.children.size() + currentFeed.books.size();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Entry getGroup(final int groupPosition) {
        if (currentFeed == null) {
            return rootFeeds.get(groupPosition);
        }
        return getItem(groupPosition, currentFeed.children, currentFeed.books);
    }

    @Override
    public long getGroupId(final int groupPosition) {
        return groupPosition;
    }

    @Override
    public int getChildrenCount(final int groupPosition) {
        final Entry group = getGroup(groupPosition);
        if (group instanceof Feed) {
            return ((Feed) group).facets.size();
        } else if (group instanceof Book) {
            final int size = ((Book) group).downloads.size();
            return size > 1 ? size : 0;
        }
        return 0;
    }

    @Override
    public Object getChild(final int groupPosition, final int childPosition) {
        final Entry group = getGroup(groupPosition);
        if (group instanceof Feed) {
            return ((Feed) group).facets.get(childPosition);
        } else if (group instanceof Book) {
            return ((Book) group).downloads.get(childPosition);
        }
        return null;
    }

    @Override
    public long getChildId(final int groupPosition, final int childPosition) {
        return childPosition;
    }

    @Override
    public boolean isChildSelectable(final int groupPosition, final int childPosition) {
        return true;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(final int groupPosition, final boolean isExpanded, final View convertView,
            final ViewGroup parent) {

        return getItemView(getGroup(groupPosition), false, convertView, parent);
    }

    @Override
    public View getChildView(final int groupPosition, final int childPosition, final boolean isLastChild,
            final View convertView, final ViewGroup parent) {

        return getItemView(getChild(groupPosition, childPosition), true, convertView, parent);
    }

    protected Entry getItem(final int i, final List<? extends Entry>... lists) {
        int index = i;
        for (final List<? extends Entry> l : lists) {
            if (index < 0) {
                return null;
            }
            final int size = l.size();
            if (index < size) {
                return l.get(index);
            }
            index -= size;
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    protected View getItemView(final Object item, final boolean child, final View view, final ViewGroup parent) {

        final ViewHolder holder = BaseViewHolder.getOrCreateViewHolder(ViewHolder.class, R.layout.opdsitem, view,
                parent);

        if (item instanceof Entry) {
            final Entry entry = (Entry) item;
            holder.textView.setText(entry.title);
            if (entry.content != null) {
                String decoded = entry.content.content;
                try {
                    decoded = URLDecoder.decode(entry.content.content);
                } catch (Exception ex) {
                }
                holder.info.setText(Html.fromHtml(decoded));
            } else {
                holder.info.setText("");
            }
            if (entry instanceof Feed) {
                holder.imageView.setImageResource(R.drawable.opds_item_feed);
            } else if (entry instanceof Book) {
                final ThumbnailFile thumbnailFile = CacheManager.getThumbnailFile(entry.id);
                if (thumbnailFile.exists()) {
                    holder.imageView.setImageBitmap(thumbnailFile.getRawImage());
                } else {
                    holder.imageView.setImageResource(R.drawable.opds_item_book);
                }
            }
        } else if (item instanceof Link) {
            final Link link = (Link) item;
            holder.textView.setText(link.type);
            holder.info.setText("");
            holder.imageView.setImageResource(R.drawable.opds_item_book);
        }

        final MarginLayoutParams lp = (MarginLayoutParams) holder.imageView.getLayoutParams();
        lp.leftMargin = child ? 50 : 4;
        return holder.getView();
    }

    public void addListener(final FeedListener listener) {
        listeners.addListener(listener);
    }

    public void removeListener(final FeedListener listener) {
        listeners.removeListener(listener);
    }

    public void downloadBook(final Book book, final int linkIndex) {
        if (book == null || linkIndex >= book.downloads.size()) {
            return;
        }
        final BookDownloadLink link = book.downloads.get(linkIndex);
        executor.startBookDownload(book, link);
    }

    public void showErrorDlg(final int pbLabel, final int pbAction, final Object result, final OPDSException error) {
        final String msg = error.getErrorMessage();

        final ActionDialogBuilder b = new ActionDialogBuilder(context, actions);
        b.setTitle(R.string.opds_error_title);
        b.setMessage(R.string.opds_error_msg, msg);
        if (pbAction != R.id.actions_no_action) {
            b.setPositiveButton(pbLabel, pbAction, new Constant("info", result));
            b.setNegativeButton();
        } else {
            b.setPositiveButton(pbLabel, pbAction);
        }
        b.show();
    }

    public void showAuthDlg(final Object info) {
        final ActionDialogBuilder b = new ActionDialogBuilder(context, actions);

        final View view = LayoutInflater.from(context).inflate(R.layout.opds_auth_dlg, null);
        final EditText editUsername = (EditText) view.findViewById(R.id.opds_auth_editUsername);
        final EditText editPassword = (EditText) view.findViewById(R.id.opds_auth_editPassword);

        b.setTitle(R.string.opds_authfeed_title);
        b.setMessage(R.string.opds_authfeed_msg);
        b.setView(view);
        b.setPositiveButton(R.string.opds_authfeed_ok, R.id.actions_setFeedAuth, new EditableValue("username",
                editUsername), new EditableValue("password", editPassword), new Constant("info", info));

        b.setNegativeButton();

        b.show();
    }

    @ActionMethod(ids = R.id.actions_setFeedAuth)
    public void setFeedAuth(final ActionEx action) {
        final String username = action.getParameter("username").toString();
        final String password = ((PasswordEditable) action.getParameter("password")).getPassword();

        final Object info = action.getParameter("info");
        if (info instanceof FeedTaskResult) {
            final AuthorizationRequiredException ex = (AuthorizationRequiredException) ((FeedTaskResult) info).error;
            client.setAuthorization(ex.host, username, password);
            actions.getOrCreateAction(R.id.opdsrefreshfolder).run();
        } else if (info instanceof DownloadBookResult) {
            final DownloadBookResult result = (DownloadBookResult) info;
            final AuthorizationRequiredException ex = (AuthorizationRequiredException) result.error;
            client.setAuthorization(ex.host, username, password);
            executor.startBookDownload(result.book, result.link);
        }

        store();
    }

    @ActionMethod(ids = R.id.actions_retryDownloadBook)
    public void retryDownload(final ActionEx action) {
        final DownloadBookResult info = action.getParameter("info");
        executor.startBookDownload(info.book, info.link);
    }

    public void startLoadThumbnails() {
        executor.startLoadThumbnails(currentFeed);
    }

    public static class ViewHolder extends BaseViewHolder {

        TextView textView;
        ImageView imageView;
        TextViewMultilineEllipse info;

        @Override
        public void init(final View convertView) {
            super.init(convertView);
            textView = (TextView) convertView.findViewById(R.id.opdsItemText);
            imageView = (ImageView) convertView.findViewById(R.id.opdsItemIcon);
            info = (TextViewMultilineEllipse) convertView.findViewById(R.id.opdsDescription);
        }
    }

}
