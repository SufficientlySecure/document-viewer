package org.ebookdroid.core.presentation;

import org.ebookdroid.R;
import org.ebookdroid.core.IBrowserActivity;
import org.ebookdroid.core.settings.SettingsManager;
import org.ebookdroid.core.settings.books.BookSettings;
import org.ebookdroid.core.utils.DirectoryFilter;
import org.ebookdroid.core.utils.FileExtensionFilter;
import org.ebookdroid.core.views.BookshelfView;
import org.ebookdroid.utils.LengthUtils;
import org.ebookdroid.utils.StringUtils;

import android.database.DataSetObserver;
import android.os.AsyncTask;
import android.os.FileObserver;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class BooksAdapter extends PagerAdapter {

    final IBrowserActivity base;
    final AtomicBoolean inScan = new AtomicBoolean();

    final TreeMap<Integer, BookShelfAdapter> data = new TreeMap<Integer, BookShelfAdapter>();

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
            BookShelfAdapter ra = createRecent();
            ra.nodes.clear();
            final int count = recent.getCount();
            for (int i = 0; i < count; i++) {
                final BookSettings item = recent.getItem(i);
                final File file = new File(item.fileName);
                ra.nodes.add(new BookNode(0, file.getName(), file.getAbsolutePath()));
            }
            ra.notifyDataSetChanged();
            BooksAdapter.this.notifyDataSetInvalidated();
        }
    };

    private final HashMap<String, FileObserver> _fileObservers = new HashMap<String, FileObserver>();

    private final RecentAdapter recent;

    public BooksAdapter(final IBrowserActivity base, final RecentAdapter adapter) {
        this.base = base;
        this.recent = adapter;
        this.recent.registerDataSetObserver(observer);
    }

    @Override
    public void destroyItem(View collection, int position, Object view) {
        ((ViewPager) collection).removeView((View) view);
        ((View) view).destroyDrawingCache();
    }

    @Override
    public void finishUpdate(View arg0) {
        // TODO Auto-generated method stub
    }

    @Override
    public int getCount() {
        return getListCount();
    }

    @Override
    public Object instantiateItem(View arg0, int arg1) {
        BookshelfView view = new BookshelfView(base, arg0, data.get(arg1));
        ((ViewPager) arg0).addView(view, 0);
        return view;
    }

    @Override
    public boolean isViewFromObject(View arg0, Object arg1) {
        return arg0.equals(arg1);
    }

    @Override
    public void restoreState(Parcelable arg0, ClassLoader arg1) {
        // TODO Auto-generated method stub
    }

    @Override
    public Parcelable saveState() {
        return null;
    }

    @Override
    public void startUpdate(View arg0) {
        // TODO Auto-generated method stub
    }

    public synchronized BookShelfAdapter getList(int id) {
        return data.get(id);
    }

    public synchronized int getListCount() {
        return data.size();
    }

    public String getListName(final int currentList) {
        createRecent();
        BookShelfAdapter list = getList(currentList);
        return list != null ? LengthUtils.safeString(list.name) : "";
    }

    public String getListPath(final int currentList) {
        createRecent();
        BookShelfAdapter list = getList(currentList);
        return list != null ? LengthUtils.safeString(list.path) : "";
    }

    public synchronized List<String> getListNames() {
        createRecent();

        if (data.isEmpty()) {
            return null;
        }

        List<String> result = new ArrayList<String>(data.size());
        for (BookShelfAdapter a : data.values()) {
            result.add(a.name);
        }
        return result;
    }

    public synchronized List<String> getListPaths() {
        createRecent();

        if (data.isEmpty()) {
            return null;
        }

        List<String> result = new ArrayList<String>(data.size());
        for (BookShelfAdapter a : data.values()) {
            result.add(a.path);
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

    public synchronized void clearData() {
        System.out.println("BS: clearData: old=" + getListPaths());

        final BookShelfAdapter oldRecent = data.get(0);
        data.clear();
        SEQ.set(1);

        if (oldRecent != null) {
            data.put(0, oldRecent);
        } else {
            createRecent();
        }

        notifyDataSetChanged();
    }

    private synchronized BookShelfAdapter createRecent() {
        BookShelfAdapter a = data.get(0);
        if (a == null) {
            a = new BookShelfAdapter(base, 0, base.getContext().getString(R.string.recent_title), "");
            data.put(0, a);
        }
        return a;
    }

    public void startScan() {
        if (inScan.compareAndSet(false, true)) {
            synchronized (_fileObservers) {
                for (FileObserver fo : _fileObservers.values()) {
                    fo.stopWatching();
                }
                _fileObservers.clear();
            }
            new ScanTask(SettingsManager.getAppSettings().getAllowedFileTypes()).execute("");
        }
    }

    public void stopScan() {
        inScan.set(false);
    }

    synchronized void addNode(final BookNode node) {
        if (node != null) {
            BookShelfAdapter a = getList(node.listNum);
            if (a == null) {
                File f = new File(node.path);
                File p = f.getParentFile();
                a = new BookShelfAdapter(base, node.listNum, p.getName(), p.getAbsolutePath());
                data.put(node.listNum, a);
            }
            a.nodes.add(node);
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

    class ScanTask extends AsyncTask<String, String, Void> {

        final FileExtensionFilter filter;

        final Queue<BookNode> currNodes = new ConcurrentLinkedQueue<BookNode>();

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
                for (BookNode p = currNodes.poll(); p != null && inScan.get(); p = currNodes.poll()) {
                    addNode(p);
                }
                notifyDataSetChanged();
            }
        }

        @Override
        protected void onPostExecute(Void v) {
            base.showProgress(false);
            notifyDataSetChanged();
            synchronized (_fileObservers) {
                for (FileObserver fo : _fileObservers.values()) {
                    fo.startWatching();
                }
            }
            inScan.set(false);
        }

        private void scanDir(final File dir) {
            // Checks if scan should be continued
            if (!inScan.get() || !dir.isDirectory() || dir.getAbsolutePath().startsWith("/sys")) {
                return;
            }

            final File[] list = dir.listFiles((FilenameFilter) filter);
            if (list != null && list.length > 0) {
                synchronized (_fileObservers) {
                    if (_fileObservers.get(dir.getAbsolutePath()) == null) {
                        _fileObservers.put(dir.getAbsolutePath(), new FileObserver(dir.getAbsolutePath(),
                                FileObserver.CREATE | FileObserver.DELETE) {

                            @Override
                            public void onEvent(int event, String path) {
                                base.getActivity().runOnUiThread(new Runnable() {

                                    @Override
                                    public void run() {
                                        BooksAdapter.this.startScan();
                                    }
                                });
                            }
                        });
                    }
                }
                Arrays.sort(list, StringUtils.getNaturalFileComparator());
                final int listNum = SEQ.getAndIncrement();
                for (final File f : list) {
                    currNodes.add(new BookNode(listNum, f.getName(), f.getAbsolutePath()));
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

    List<DataSetObserver> _dsoList = new ArrayList<DataSetObserver>();

    public void registerDataSetObserver(DataSetObserver dataSetObserver) {
        _dsoList.add(dataSetObserver);
    }

    private void notifyDataSetInvalidated() {

        for (DataSetObserver dso : _dsoList) {
            dso.onInvalidated();
        }
    }

    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();

        for (DataSetObserver dso : _dsoList) {
            dso.onChanged();
        }
    }

}
