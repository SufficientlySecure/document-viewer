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

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.os.AsyncTask;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.BaseExpandableListAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
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
import org.emdev.ui.progress.IProgressIndicator;
import org.emdev.ui.tasks.BaseFileAsyncTask;
import org.emdev.ui.tasks.BaseFileAsyncTask.FileTaskResult;
import org.emdev.ui.widget.TextViewMultilineEllipse;
import org.emdev.utils.LengthUtils;
import org.emdev.utils.listeners.ListenerProxy;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class OPDSAdapter extends BaseExpandableListAdapter {

    private final Context context;
    private final IActionController<?> actions;

    private final OPDSClient client;
    private final List<Feed> rootFeeds;

    private volatile Feed currentFeed;

    private final ListenerProxy listeners = new ListenerProxy(FeedListener.class);

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
                if (LengthUtils.isAllNotEmpty(alias, url)) {
                    rootFeeds.add(new Feed(alias, url));
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

    public void addFeeds(final Feed... feeds) {
        for (final Feed feed : feeds) {
            rootFeeds.add(feed);
        }
        store();
        if (currentFeed == null) {
            notifyDataSetChanged();
        }
    }

    public void editFeed(final Feed feed, final String alias, final String url) {
        if (feed.id.equals(url)) {
            feed.title = alias;
            store();
            if (currentFeed == null) {
                notifyDataSetInvalidated();
            }
            return;
        }
        final Feed newFeed = new Feed(alias, url);
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

        if (feed != null) {
            if (feed.loadedAt == 0) {
                new LoadFeedTask().execute(feed);
            } else {
                new LoadThumbnailTask().execute(feed);
            }
        }
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

    protected View getItemView(final Object item, final boolean child, final View view, final ViewGroup parent) {

        final ViewHolder holder = BaseViewHolder.getOrCreateViewHolder(ViewHolder.class, R.layout.opdsitem, view,
                parent);

        if (item instanceof Entry) {
            final Entry entry = (Entry) item;
            holder.textView.setText(entry.title);
            if (entry.content != null) {
                @SuppressWarnings("deprecation")
                final String decoded = URLDecoder.decode(entry.content.content);
                holder.info.setText(Html.fromHtml(decoded));
            } else {
                holder.info.setText("");
            }
            if (entry instanceof Feed) {
                holder.imageView.setImageResource(R.drawable.folderopen);
            } else if (entry instanceof Book) {
                final ThumbnailFile thumbnailFile = CacheManager.getThumbnailFile(entry.id);
                if (thumbnailFile.exists()) {
                    holder.imageView.setImageBitmap(thumbnailFile.getRawImage());
                } else {
                    holder.imageView.setImageResource(R.drawable.book);
                }
            }
        } else if (item instanceof Link) {
            final Link link = (Link) item;
            holder.textView.setText(link.type);
            holder.info.setText("");
            holder.imageView.setImageResource(R.drawable.book);
        }

        final MarginLayoutParams lp = (MarginLayoutParams) holder.imageView.getLayoutParams();
        lp.leftMargin = child ? 50 : 4;
        return holder.getView();
    }

    protected void loadBookThumbnail(final Book book) {
        if (book.thumbnail == null) {
            return;
        }
        final ThumbnailFile thumbnailFile = CacheManager.getThumbnailFile(book.id);
        if (thumbnailFile.exists()) {
            return;
        }

        try {
            final File file = client.loadFile(book.parent, book.thumbnail);
            if (file == null) {
                return;
            }

            final Options opts = new Options();
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            opts.inJustDecodeBounds = true;

            BitmapFactory.decodeStream(new FileInputStream(file), null, opts);

            opts.inSampleSize = getScale(opts, 200, 200);
            opts.inJustDecodeBounds = false;

            final Bitmap image = BitmapFactory.decodeStream(new FileInputStream(file), null, opts);
            if (image != null) {
                thumbnailFile.setImage(image);
                image.recycle();
            }
        } catch (final Throwable ex) {
            ex.printStackTrace();
        }
    }

    protected int getScale(final Options opts, final float requiredWidth, final float requiredHeight) {
        int scale = 1;
        int widthTmp = opts.outWidth;
        int heightTmp = opts.outHeight;
        while (true) {
            if (widthTmp / 2 < requiredWidth || heightTmp / 2 < requiredHeight) {
                break;
            }
            widthTmp /= 2;
            heightTmp /= 2;

            scale *= 2;
        }
        return scale;
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
        new DownloadBookTask().execute(book, link);
    }

    public void showErrorDlg(final int pbLabel, final int pbAction, final Object result, OPDSException error) {
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
        final String password = ((PasswordEditable)action.getParameter("password")).getPassword();

        final Object info = action.getParameter("info");
        if (info instanceof FeedTaskResult) {
            final AuthorizationRequiredException ex = (AuthorizationRequiredException) ((FeedTaskResult) info).error;
            try {
                client.setAuthorization(ex.host, username, password);
                actions.getOrCreateAction(R.id.opdsrefreshfolder).run();
            } catch (final OPDSException exx) {
                ex.printStackTrace();
            }
        } else if (info instanceof DownloadBookResult) {
            final DownloadBookResult result = (DownloadBookResult) info;
            final AuthorizationRequiredException ex = (AuthorizationRequiredException) result.error;
            try {
                client.setAuthorization(ex.host, username, password);
                new DownloadBookTask().execute(result.book, result.link);
            } catch (final OPDSException exx) {
                ex.printStackTrace();
            }
        }
    }

    @ActionMethod(ids = R.id.actions_retryDownloadBook)
    public void retryDownload(final ActionEx action) {
        DownloadBookResult info = action.getParameter("info");
        new DownloadBookTask().execute(info.book, info.link);
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

    public static class FeedTaskResult {

        public Feed feed;
        public OPDSException error;

        public FeedTaskResult(final Feed feed) {
            this.feed = feed;
        }

        public FeedTaskResult(final Feed feed, final OPDSException error) {
            this.feed = feed;
            this.error = error;
        }
    }

    final class LoadFeedTask extends AsyncTask<Feed, String, FeedTaskResult> implements OnCancelListener,
            IProgressIndicator {

        private ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            onProgressUpdate(context.getResources().getString(R.string.opds_connecting));
        }

        @Override
        public void onCancel(final DialogInterface dialog) {
            this.cancel(true);
        }

        @Override
        protected FeedTaskResult doInBackground(final Feed... params) {
            final Feed f = params[0];
            try {
                final Feed feed = client.loadFeed(f, this);
                new LoadThumbnailTask().execute(feed);
                return new FeedTaskResult(feed);
            } catch (final OPDSException ex) {
                return new FeedTaskResult(f, ex);
            }
        }

        @Override
        protected void onPostExecute(final FeedTaskResult result) {
            if (progressDialog != null) {
                try {
                    progressDialog.dismiss();
                } catch (final Throwable th) {
                }
            }

            if (result.error instanceof AuthorizationRequiredException) {
                showAuthDlg(result);
            } else if (result.error != null) {
                showErrorDlg(R.string.opdsrefreshfolder, R.id.opdsrefreshfolder, result, result.error);
            }

            final FeedListener l = listeners.getListener();
            l.feedLoaded(result.feed);
            notifyDataSetChanged();

        }

        @Override
        public void setProgressDialogMessage(final int resourceID, final Object... args) {
            publishProgress(context.getResources().getString(resourceID, args));
        }

        @Override
        protected void onProgressUpdate(final String... values) {
            final int length = LengthUtils.length(values);
            if (length == 0) {
                return;
            }
            final String last = values[length - 1];
            if (progressDialog == null || !progressDialog.isShowing()) {
                progressDialog = ProgressDialog.show(context, "", last, true);
                progressDialog.setCancelable(true);
                progressDialog.setCanceledOnTouchOutside(true);
                progressDialog.setOnCancelListener(this);
            } else {
                progressDialog.setMessage(last);
            }
        }
    }

    final class LoadThumbnailTask extends AsyncTask<Feed, Book, String> {

        @Override
        protected String doInBackground(final Feed... params) {
            if (LengthUtils.isEmpty(params)) {
                return null;
            }
            for (final Feed feed : params) {
                if (feed == null) {
                    continue;
                }
                for (final Book book : feed.books) {
                    if (currentFeed != book.parent) {
                        break;
                    }
                    loadBookThumbnail(book);
                    publishProgress(book);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(final String result) {
            notifyDataSetInvalidated();
        }

        @Override
        protected void onProgressUpdate(final Book... books) {
            boolean inCurrent = false;
            for (final Book book : books) {
                inCurrent |= book.parent == currentFeed;
            }
            if (inCurrent) {
                notifyDataSetInvalidated();
            }
        }
    }

    public static interface FeedListener {

        void feedLoaded(Feed feed);
    }

    protected static class DownloadBookResult extends FileTaskResult {

        public Book book;
        public BookDownloadLink link;

        public DownloadBookResult(final Book book, final BookDownloadLink link, final File target) {
            super(target);
            this.book = book;
            this.link = link;
        }

        public DownloadBookResult(final Book book, final BookDownloadLink link, final Throwable error) {
            super(error);
            this.book = book;
            this.link = link;
        }
    }

    final class DownloadBookTask extends BaseFileAsyncTask<Object, DownloadBookResult> implements OnCancelListener,
            IProgressIndicator {

        public DownloadBookTask() {
            super(OPDSAdapter.this.context, R.string.opds_connecting, R.string.opds_download_complete,
                    R.string.opds_download_error, true);
        }

        @Override
        protected DownloadBookResult doInBackground(final Object... params) {
            final Book book = (Book) params[0];
            final BookDownloadLink link = (BookDownloadLink) params[1];
            try {
                final File file = client.downloadBook(book, link, this);
                return new DownloadBookResult(book, link, file);
            } catch (final OPDSException ex) {
                return new DownloadBookResult(book, link, ex);
            }
        }

        @Override
        protected void onPostExecute(DownloadBookResult result) {
            super.onPostExecute(result);
            if (result != null) {
                if (result.error instanceof AuthorizationRequiredException) {
                    showAuthDlg(result);
                } else if (result.error instanceof OPDSException) {
                    showErrorDlg(R.string.opds_retry_download, R.id.actions_retryDownloadBook, result,
                            (OPDSException) result.error);
                } else if (result.error != null) {
                    showErrorDlg(R.string.opds_retry_download, R.id.actions_retryDownloadBook, result,
                            new OPDSException(result.error));
                }
            }
        }

        @Override
        protected void processError(final Throwable error) {
        }
    }

}
