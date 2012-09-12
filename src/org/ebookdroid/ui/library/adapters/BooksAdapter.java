package org.ebookdroid.ui.library.adapters;

import org.ebookdroid.R;
import org.ebookdroid.common.notifications.INotificationManager;
import org.ebookdroid.common.settings.LibSettings;
import org.ebookdroid.ui.library.IBrowserActivity;
import org.ebookdroid.ui.library.views.BookshelfView;

import android.database.DataSetObserver;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.emdev.common.filesystem.FileSystemScanner;
import org.emdev.ui.adapters.BaseViewHolder;
import org.emdev.ui.tasks.AsyncTask;
import org.emdev.utils.LengthUtils;
import org.emdev.utils.StringUtils;
import org.emdev.utils.collections.SparseArrayEx;

public class BooksAdapter extends PagerAdapter implements FileSystemScanner.Listener, Iterable<BookShelfAdapter> {

    public final static int SERVICE_SHELVES = 2;

    public static final int RECENT_INDEX = 0;

    public static final int SEARCH_INDEX = 1;

    private final static AtomicInteger SEQ = new AtomicInteger(SERVICE_SHELVES);

    final IBrowserActivity base;

    final AtomicBoolean inScan = new AtomicBoolean();

    final SparseArrayEx<BookShelfAdapter> data = new SparseArrayEx<BookShelfAdapter>();

    final TreeMap<String, BookShelfAdapter> folders = new TreeMap<String, BookShelfAdapter>();

    private final RecentUpdater updater = new RecentUpdater();

    private final RecentAdapter recent;

    private final FileSystemScanner scanner;

    private final List<DataSetObserver> _dsoList = new ArrayList<DataSetObserver>();

    private String searchQuery;

    public BooksAdapter(final IBrowserActivity base, final RecentAdapter adapter) {
        this.base = base;
        this.recent = adapter;
        this.recent.registerDataSetObserver(updater);

        this.scanner = new FileSystemScanner(base.getActivity());
        this.scanner.addListener(this);
        this.scanner.addListener(base);

        this.searchQuery = LibSettings.current().searchBookQuery;
    }

    @Override
    public void destroyItem(final View collection, final int position, final Object view) {
        ((ViewPager) collection).removeView((View) view);
        ((View) view).destroyDrawingCache();
    }

    @Override
    public void finishUpdate(final View arg0) {
        // TODO Auto-generated method stub
    }

    @Override
    public Iterator<BookShelfAdapter> iterator() {
        return data.iterator();
    }

    @Override
    public int getCount() {
        return getListCount();
    }

    @Override
    public Object instantiateItem(final View arg0, final int arg1) {
        final BookshelfView view = new BookshelfView(base, arg0, getList(arg1));
        ((ViewPager) arg0).addView(view, 0);
        return view;
    }

    @Override
    public boolean isViewFromObject(final View arg0, final Object arg1) {
        return arg0.equals(arg1);
    }

    @Override
    public void restoreState(final Parcelable arg0, final ClassLoader arg1) {
        // TODO Auto-generated method stub
    }

    @Override
    public Parcelable saveState() {
        return null;
    }

    @Override
    public void startUpdate(final View arg0) {
        // TODO Auto-generated method stub
    }

    public synchronized BookShelfAdapter getShelf(final String path) {
        return folders.get(path);
    }

    public synchronized int getShelfPosition(final BookShelfAdapter shelf) {
        checkServiceAdapters();
        return data.indexOfValue(shelf);
    }

    public synchronized BookShelfAdapter getList(final int index) {
        return data.valueAt(index);
    }

    public synchronized int getListCount() {
        return data.size();
    }

    public synchronized int getListCount(final int currentList) {
        checkServiceAdapters();
        if (0 <= currentList && currentList < data.size()) {
            return getList(currentList).nodes.size();
        }
        return 0;
    }

    public String getListName(final int currentList) {
        checkServiceAdapters();
        final BookShelfAdapter list = getList(currentList);
        return list != null ? LengthUtils.safeString(list.name) : "";
    }

    public String getListPath(final int currentList) {
        checkServiceAdapters();
        final BookShelfAdapter list = getList(currentList);
        return list != null ? LengthUtils.safeString(list.path) : "";
    }

    public synchronized List<String> getListNames() {
        checkServiceAdapters();

        final int size = data.size();

        if (size == 0) {
            return null;
        }

        final List<String> result = new ArrayList<String>(data.size());
        for (int index = 0; index < size; index++) {
            final BookShelfAdapter a = data.valueAt(index);
            result.add(a.name);
        }
        return result;
    }

    public synchronized List<String> getListPaths() {
        checkServiceAdapters();

        final int size = data.size();

        if (size == 0) {
            return null;
        }

        final List<String> result = new ArrayList<String>(data.size());
        for (int index = 0; index < size; index++) {
            final BookShelfAdapter a = data.valueAt(index);
            result.add(a.path);
        }
        return result;
    }

    public synchronized BookNode getItem(final int currentList, final int position) {
        checkServiceAdapters();
        if (0 <= currentList && currentList < data.size()) {
            return getList(currentList).nodes.get(position);
        }
        throw new RuntimeException("Wrong list id: " + currentList + "/" + data.size());
    }

    public long getItemId(final int position) {
        return position;
    }

    public synchronized void clearData() {
        getService(SEARCH_INDEX).nodes.clear();

        final BookShelfAdapter[] service = new BookShelfAdapter[SERVICE_SHELVES];
        for (int i = 0; i < service.length; i++) {
            service[i] = data.get(i);
        }

        data.clear();
        folders.clear();
        SEQ.set(SERVICE_SHELVES);

        for (int i = 0; i < service.length; i++) {
            if (service[i] != null) {
                data.append(i, service[i]);
            } else {
                getService(i);
            }
        }

        notifyDataSetChanged();
    }

    public synchronized void clearSearch() {
        final BookShelfAdapter search = getService(SEARCH_INDEX);
        search.nodes.clear();
        search.notifyDataSetChanged();
    }

    protected synchronized void checkServiceAdapters() {
        for (int i = 0; i < SERVICE_SHELVES; i++) {
            getService(i);
        }
    }

    protected synchronized BookShelfAdapter getService(final int index) {
        BookShelfAdapter a = data.get(index);
        if (a == null) {
            switch (index) {
                case RECENT_INDEX:
                    a = new BookShelfAdapter(base, 0, base.getContext().getString(R.string.recent_title), "");
                    break;
                case SEARCH_INDEX:
                    a = new BookShelfAdapter(base, 0, base.getContext().getString(R.string.search_results_title), "");
                    break;
            }
            if (a != null) {
                data.append(index, a);
            }
        }
        return a;
    }

    public void startScan() {
        clearData();
        final LibSettings libSettings = LibSettings.current();
        scanner.startScan(libSettings.allowedFileTypes, libSettings.autoScanDirs);
    }

    public void stopScan() {
        scanner.stopScan();
    }

    public boolean startSearch(final String searchQuery) {
        this.searchQuery = LengthUtils.safeString(searchQuery).trim();
        LibSettings.updateSearchBookQuery(this.searchQuery);

        clearSearch();

        if (LengthUtils.isEmpty(this.searchQuery)) {
            return false;
        }

        if (!scanner.isScan()) {
            new SearchTask().execute("");
        }

        return true;
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    protected synchronized void onNodesFound(final List<BookNode> nodes) {
        final BookShelfAdapter search = getService(SEARCH_INDEX);
        search.nodes.addAll(nodes);
        Collections.sort(search.nodes);
        search.notifyDataSetChanged();
    }

    @Override
    public synchronized void onFileScan(final File parent, final File[] files) {
        final String dir = parent.getAbsolutePath();
        BookShelfAdapter a = folders.get(dir);
        if (LengthUtils.isEmpty(files)) {
            if (a != null) {
                onDirDeleted(parent.getParentFile(), parent);
            }
        } else {
            final BookShelfAdapter search = getService(SEARCH_INDEX);
            if (a == null) {
                a = new BookShelfAdapter(base, SEQ.getAndIncrement(), parent.getName(), dir);
                data.append(a.id, a);
                folders.put(dir, a);
                boolean found = false;
                for (final File f : files) {
                    BookNode node = recent.getNode(f.getAbsolutePath());
                    if (node == null) {
                        node = new BookNode(f, null);
                    }
                    a.nodes.add(node);
                    if (acceptSearch(node)) {
                        found = true;
                        search.nodes.add(node);
                    }
                }
                if (found) {
                    Collections.sort(search.nodes);
                }
                notifyDataSetChanged();
            } else {
                boolean found = false;
                for (final File f : files) {
                    BookNode node = recent.getNode(f.getAbsolutePath());
                    if (node == null) {
                        node = new BookNode(f, null);
                    }
                    a.nodes.add(node);
                    if (acceptSearch(node)) {
                        found = true;
                        search.nodes.add(node);
                    }
                }
                a.notifyDataSetChanged();
                if (found) {
                    Collections.sort(search.nodes);
                    search.notifyDataSetChanged();
                }
            }
        }
    }

    @Override
    public synchronized void onFileAdded(final File parent, final File f) {
        if (f != null && LibSettings.current().allowedFileTypes.accept(f)) {
            final String dir = parent.getAbsolutePath();
            BookShelfAdapter a = folders.get(dir);
            final BookShelfAdapter search = getService(SEARCH_INDEX);
            if (a == null) {
                a = new BookShelfAdapter(base, SEQ.getAndIncrement(), parent.getName(), dir);
                data.append(a.id, a);
                folders.put(dir, a);
                BookNode node = recent.getNode(f.getAbsolutePath());
                if (node == null) {
                    node = new BookNode(f, null);
                }
                a.nodes.add(node);
                Collections.sort(a.nodes);
                if (acceptSearch(node)) {
                    search.nodes.add(node);
                    Collections.sort(search.nodes);
                }
                notifyDataSetChanged();
            } else {
                BookNode node = recent.getNode(f.getAbsolutePath());
                if (node == null) {
                    node = new BookNode(f, null);
                }
                a.nodes.add(node);
                Collections.sort(a.nodes);
                a.notifyDataSetChanged();
                if (acceptSearch(node)) {
                    search.nodes.add(node);
                    Collections.sort(search.nodes);
                    search.notifyDataSetChanged();
                }
            }
            INotificationManager.instance.notify(R.string.notification_file_add, f.getAbsolutePath(), null);
        }
    }

    @Override
    public synchronized void onFileDeleted(final File parent, final File f) {
        if (f != null && LibSettings.current().allowedFileTypes.accept(f)) {
            final String dir = parent.getAbsolutePath();
            final BookShelfAdapter a = folders.get(dir);
            if (a != null) {
                final String path = f.getAbsolutePath();
                final BookShelfAdapter search = getService(SEARCH_INDEX);
                for (final Iterator<BookNode> i = a.nodes.iterator(); i.hasNext();) {
                    final BookNode node = i.next();
                    if (path.equals(node.path)) {
                        i.remove();
                        if (a.nodes.isEmpty()) {
                            data.remove(a.id);
                            folders.remove(a.path);
                            this.notifyDataSetChanged();
                        } else {
                            a.notifyDataSetChanged();
                        }
                        if (search.nodes.remove(node)) {
                            search.notifyDataSetChanged();
                        }
                        INotificationManager.instance.notify(R.string.notification_file_delete, f.getAbsolutePath(), null);
                        return;
                    }
                }
            }
        }
    }

    @Override
    public void onDirAdded(final File parent, final File f) {
        final LibSettings libSettings = LibSettings.current();
        scanner.startScan(libSettings.allowedFileTypes, f.getAbsolutePath());
    }

    @Override
    public synchronized void onDirDeleted(final File parent, final File f) {
        final String dir = f.getAbsolutePath();
        final BookShelfAdapter a = folders.get(dir);
        if (a != null) {
            data.remove(a.id);
            folders.remove(a.path);
            this.notifyDataSetChanged();
        }
    }

    protected boolean acceptSearch(final BookNode node) {
        if (LengthUtils.isEmpty(searchQuery)) {
            return false;
        }
        final String bookTitle = StringUtils.cleanupTitle(node.name).toLowerCase();
        final int pos = bookTitle.indexOf(searchQuery);
        return pos >= 0;
    }

    public void registerDataSetObserver(final DataSetObserver dataSetObserver) {
        _dsoList.add(dataSetObserver);
    }

    protected void notifyDataSetInvalidated() {
        for (final DataSetObserver dso : _dsoList) {
            dso.onInvalidated();
        }
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        for (final DataSetObserver dso : _dsoList) {
            dso.onChanged();
        }
    }

    public static class ViewHolder extends BaseViewHolder {

        ImageView imageView;
        TextView textView;

        @Override
        public void init(final View convertView) {
            super.init(convertView);
            this.imageView = (ImageView) convertView.findViewById(R.id.thumbnailImage);
            this.textView = (TextView) convertView.findViewById(R.id.thumbnailText);
        }
    }

    private final class RecentUpdater extends DataSetObserver {

        @Override
        public void onChanged() {
            updateRecentBooks();
        }

        @Override
        public void onInvalidated() {
            updateRecentBooks();
        }

        private void updateRecentBooks() {
            final BookShelfAdapter ra = getService(RECENT_INDEX);
            ra.nodes.clear();
            final int count = recent.getCount();
            for (int i = 0; i < count; i++) {
                final BookNode book = recent.getItem(i);
                ra.nodes.add(book);
                final BookShelfAdapter a = folders.get(new File(book.path).getParent());
                if (a != null) {
                    a.notifyDataSetInvalidated();
                }
            }
            ra.notifyDataSetChanged();
            BooksAdapter.this.notifyDataSetInvalidated();
        }
    }

    class SearchTask extends AsyncTask<String, String, Void> {

        private final BlockingQueue<BookNode> queue = new ArrayBlockingQueue<BookNode>(160, true);

        @Override
        protected void onPreExecute() {
            base.showProgress(true);
        }

        @Override
        protected Void doInBackground(final String... paths) {
            int aIndex = SERVICE_SHELVES;
            while (aIndex < getListCount()) {
                int nIndex = 0;
                while (nIndex < getListCount(aIndex)) {
                    final BookNode node = getItem(aIndex, nIndex);
                    if (acceptSearch(node)) {
                        queue.offer(node);
                        publishProgress("");
                    }
                    nIndex++;
                }
                aIndex++;
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(final String... values) {
            final ArrayList<BookNode> nodes = new ArrayList<BookNode>();
            while (!queue.isEmpty()) {
                nodes.add(queue.poll());
            }
            if (!nodes.isEmpty()) {
                onNodesFound(nodes);
            }
        }

        @Override
        protected void onPostExecute(final Void v) {
            onProgressUpdate("");
            base.showProgress(false);
        }
    }
}
