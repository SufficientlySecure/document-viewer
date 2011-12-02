package org.ebookdroid.core.presentation;

import org.ebookdroid.R;
import org.ebookdroid.core.IBrowserActivity;
import org.ebookdroid.core.settings.SettingsManager;
import org.ebookdroid.core.settings.books.BookSettings;
import org.ebookdroid.core.utils.DirectoryFilter;
import org.ebookdroid.core.utils.FileExtensionFilter;
import org.ebookdroid.utils.LengthUtils;
import org.ebookdroid.utils.StringUtils;

import android.database.DataSetObserver;
import android.os.AsyncTask;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class BooksAdapter {

    final IBrowserActivity base;
    final AtomicBoolean inScan = new AtomicBoolean();

    final TreeMap<Integer, NodeList> data = new TreeMap<Integer, NodeList>();

    private final static AtomicInteger SEQ = new AtomicInteger(1);

    private final DataSetObserver observer = new DataSetObserver() {

        @Override
        public void onChanged() {
            updateRecentBooks();
        }

        @Override
        public void onInvalidated() {
            updateRecentBooks();
        };

        private void updateRecentBooks() {
            NodeList arrayList = createRecent();
            arrayList.nodes.clear();
            final int count = recent.getCount();
            for (int i = 0; i < count; i++) {
                final BookSettings item = recent.getItem(i);
                final File file = new File(item.fileName);
                arrayList.nodes.add(new Node(0, file.getName(), file.getAbsolutePath()));
            }
            BooksAdapter.this.notifyDataSetInvalidated();
        }
    };

    private final RecentAdapter recent;

    public BooksAdapter(final IBrowserActivity base, final RecentAdapter adapter) {
        this.base = base;
        this.recent = adapter;
        this.recent.registerDataSetObserver(observer);
    }

    public synchronized NodeList getList(int id) {
        return data.get(id);
    }

    public synchronized int getListCount() {
        return data.size();
    }

    public String getListName(final int currentList) {
        createRecent();
        NodeList list = getList(currentList);
        return list != null ? LengthUtils.safeString(list.name) : "";
    }

    public String getListPath(final int currentList) {
        createRecent();
        NodeList list = getList(currentList);
        return list != null ? LengthUtils.safeString(list.path) : "";
    }

    public synchronized List<String> getListNames() {
        createRecent();

        if (data.isEmpty()) {
            return null;
        }

        List<String> result = new ArrayList<String>(data.size());
        for (NodeList list : data.values()) {
            result.add(list.name);
        }
        return result;
    }

    public synchronized List<String> getListPaths() {
        createRecent();

        if (data.isEmpty()) {
            return null;
        }

        List<String> result = new ArrayList<String>(data.size());
        for (NodeList list : data.values()) {
            result.add(list.path);
        }
        return result;
    }

    public synchronized int getCount(final int currentList) {
        createRecent();
        if (0 <= currentList && currentList < data.size()) {
            return getList(currentList).nodes.size();
        }
        throw new RuntimeException("Wrong list id: " + currentList + ", " + data.keySet());
    }

    public Object getItem(final int currentList, final int position) {
        createRecent();
        if (0 <= currentList && currentList < data.size()) {
            return getList(currentList).nodes.get(position);
        }
        throw new RuntimeException("Wrong list id: " + currentList + ", " + data.keySet());
    }

    public long getItemId(final int position) {
        return position;
    }

    public View getView(final int currentList, final int position, final View view, final ViewGroup parent) {

        final ViewHolder holder = BaseViewHolder.getOrCreateViewHolder(ViewHolder.class, R.layout.thumbnail, view,
                parent);

        NodeList list = getList(currentList);
        Node node = list.nodes.get(position);

        holder.textView.setText(StringUtils.cleanupTitle(node.getName()));
        base.loadThumbnail(node.getPath(), holder.imageView, R.drawable.book);

        return holder.getView();
    }

    public synchronized void clearData() {
        System.out.println("BS: clearData: old=" + getListPaths());

        final NodeList oldRecent = data.get(0);
        data.clear();
        SEQ.set(1);

        if (oldRecent != null) {
            data.put(0, oldRecent);
        } else {
            createRecent();
        }

        notifyDataSetChanged();
    }

    private synchronized NodeList createRecent() {
        NodeList recentList = data.get(0);
        if (recentList == null) {
            recentList = new NodeList(0, base.getContext().getString(R.string.recent_title), "");
            data.put(0, recentList);
        }
        return recentList;
    }

    public void startScan(final FileExtensionFilter filter) {
        if (inScan.compareAndSet(false, true)) {
            new ScanTask(filter).execute("");
        }
    }

    public void stopScan() {
        inScan.set(false);
    }

    synchronized void addNode(final Node node) {
        if (node != null) {
            NodeList list = getList(node.listNum);
            if (list == null) {
                File f = new File(node.path);
                File p = f.getParentFile();
                list = new NodeList(node.listNum, p.getName(), p.getAbsolutePath());
                data.put(node.listNum, list);
            }
            list.nodes.add(node);
        }
    }

    static class ViewHolder extends BaseViewHolder {

        ImageView imageView;
        TextView textView;

        @Override
        public void init(final View convertView) {
            super.init(convertView);
            this.imageView = (ImageView) convertView.findViewById(R.id.thumbnailImage);
            this.textView = (TextView) convertView.findViewById(R.id.thumbnailText);
        }
    }

    public static class NodeList {

        final int id;
        final String name;
        final String path;

        final List<Node> nodes = new ArrayList<Node>();

        public NodeList(int id, String name, String path) {
            this.id = id;
            this.name = name;
            this.path = path;
        }
    }

    public static class Node {

        final int listNum;
        final String name;
        final String path;

        Node(final int listNum, final String name, final String path) {
            this.listNum = listNum;
            this.name = name;
            this.path = path;
        }

        String getName() {
            return this.name;
        }

        public String getPath() {
            return this.path;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

    class ScanTask extends AsyncTask<String, String, Void> {

        final FileExtensionFilter filter;

        final Queue<Node> currNodes = new ConcurrentLinkedQueue<Node>();

        public ScanTask(final FileExtensionFilter filter) {
            this.filter = filter;
        }

        @Override
        protected void onPreExecute() {
            base.showProgress(true);
            clearData();
        }

        @Override
        protected Void doInBackground(String... params) {
            for (final String path : SettingsManager.getAppSettings().getAutoScanDirs()) {
                // Scan each valid folder
                final File dir = new File(path);
                if (dir.isDirectory()) {
                    scanDir(dir);
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            if (!currNodes.isEmpty()) {
                // Add files from queue to adapter
                for (Node p = currNodes.poll(); p != null && inScan.get(); p = currNodes.poll()) {
                    addNode(p);
                }
                notifyDataSetChanged();
            }
        }

        @Override
        protected void onPostExecute(Void v) {
            base.showProgress(false);
            notifyDataSetChanged();
        }

        private void scanDir(final File dir) {
            // Checks if scan should be continued
            if (!inScan.get() || !dir.isDirectory() || dir.getAbsolutePath().startsWith("/sys")) {
                return;
            }
            final File[] list = dir.listFiles((FilenameFilter) filter);
            if (list != null && list.length > 0) {
                Arrays.sort(list, StringUtils.getNaturalFileComparator());
                final int listNum = SEQ.getAndIncrement();
                for (final File f : list) {
                    currNodes.add(new Node(listNum, f.getName(), f.getAbsolutePath()));
                    publishProgress("");
                }
            }
            // Retrieves files from current directory
            final File[] listOfDirs = dir.listFiles(DirectoryFilter.ALL);
            if (LengthUtils.isNotEmpty(listOfDirs)) {
                Arrays.sort(listOfDirs, StringUtils.getNaturalFileComparator());
                // if (inScan.get()) {
                for (int i = 0; i < listOfDirs.length; i++) {
                    // Recursively processing found file
                    scanDir(listOfDirs[i]);
                }
                // }
            }
        }
    }

    public static class BookShelfAdapter extends BaseAdapter {

        private int index;
        private BooksAdapter adapter;

        public BookShelfAdapter(BooksAdapter adapter, int index) {
            this.adapter = adapter;
            this.index = index;
        }

        @Override
        public int getCount() {
            return adapter.getCount(index);
        }

        @Override
        public Object getItem(int position) {
            return adapter.getItem(index, position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return adapter.getView(index, position, convertView, parent);
        }

        public String getPath() {
            return adapter.getListPath(index);
        }
    }

    List<DataSetObserver> _dsoList = new ArrayList<DataSetObserver>();

    public void registerDataSetObserver(DataSetObserver dataSetObserver) {
        _dsoList.add(dataSetObserver);
    }

    private void notifyDataSetInvalidated() {
        for (DataSetObserver dso : _dsoList) {
            dso.onInvalidated();
        }
    }

    private void notifyDataSetChanged() {
        for (DataSetObserver dso : _dsoList) {
            dso.onChanged();
        }
    }
}
