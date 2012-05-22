package org.ebookdroid.ui.opds.adapters;

import org.ebookdroid.EBookDroidApp;
import org.ebookdroid.R;
import org.ebookdroid.common.cache.CacheManager;
import org.ebookdroid.common.cache.ThumbnailFile;
import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.opds.Book;
import org.ebookdroid.opds.BookDownloadLink;
import org.ebookdroid.opds.Entry;
import org.ebookdroid.opds.ExtentedEntryBuilder;
import org.ebookdroid.opds.Feed;
import org.ebookdroid.opds.Link;
import org.ebookdroid.opds.OPDSClient;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.os.AsyncTask;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import org.emdev.ui.adapters.BaseViewHolder;
import org.emdev.ui.progress.IProgressIndicator;
import org.emdev.ui.widget.TextViewMultilineEllipse;
import org.emdev.utils.LengthUtils;
import org.emdev.utils.listeners.ListenerProxy;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class OPDSAdapter extends BaseExpandableListAdapter {

    private final Context context;
    private final OPDSClient client;
    private final List<Feed> rootFeeds;

    private volatile Feed currentFeed;

    private final ListenerProxy listeners = new ListenerProxy(FeedListener.class);

    public OPDSAdapter(final Context context) {
        this.context = context;
        this.client = new OPDSClient(new ExtentedEntryBuilder());

        this.rootFeeds = new ArrayList<Feed>();

        final JSONArray feeds = SettingsManager.getAppSettings().opdsCatalogs;
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
            addFeeds(new Feed("Flibusta", "http://flibusta.net/opds"), new Feed("Plough", "http://www.plough.com/ploughCatalog_opds.xml"));
        }

        this.currentFeed = null;
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
        final JSONArray catalogs = SettingsManager.getAppSettings().opdsCatalogs;
        for (final Feed feed : feeds) {
            rootFeeds.add(feed);
            try {
                final JSONObject newCatalog = new JSONObject();
                newCatalog.put("alias", feed.title);
                newCatalog.put("url", feed.link.uri);
                catalogs.put(newCatalog);
            } catch (final JSONException ex) {
                ex.printStackTrace();
            }
        }
        SettingsManager.changeOpdsCatalogs(catalogs);
        if (currentFeed == null) {
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
            final File file = client.loadFile(book.thumbnail);
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

    final class LoadFeedTask extends AsyncTask<Feed, String, Feed> implements OnCancelListener, IProgressIndicator {

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
        protected Feed doInBackground(final Feed... params) {
            final Feed feed = client.load(params[0], this);
            new LoadThumbnailTask().execute(feed);
            return feed;
        }

        @Override
        protected void onPostExecute(final Feed result) {
            if (progressDialog != null) {
                try {
                    progressDialog.dismiss();
                } catch (final Throwable th) {
                }
            }

            final FeedListener l = listeners.getListener();
            l.feedLoaded(result);

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
            final Feed feed = params[0];
            for (final Book book : feed.books) {
                if (currentFeed != book.parent) {
                    break;
                }
                loadBookThumbnail(book);
                publishProgress(book);
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

    public void downloadBook(final Book book, final int linkIndex) {
        if (book == null || linkIndex >= book.downloads.size()) {
            return;
        }
        final BookDownloadLink link = book.downloads.get(linkIndex);
        new DownloadBookTask().execute(book, link);
    }

    final class DownloadBookTask extends AsyncTask<Object, String, File> implements OnCancelListener,
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
        protected File doInBackground(final Object... params) {
            // final Book book = (Book) params[0];
            final BookDownloadLink link = (BookDownloadLink) params[1];
            final File file = client.download(link, this);
            return file;
        }

        @Override
        protected void onPostExecute(final File result) {
            if (progressDialog != null) {
                try {
                    progressDialog.dismiss();
                } catch (final Throwable th) {
                }
            }
            if (result != null) {
                Toast.makeText(EBookDroidApp.context, "Book download complete: " + result.getAbsolutePath(), 0).show();
            } else {
                Toast.makeText(EBookDroidApp.context, "Book download failed", 0).show();
            }
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

}
