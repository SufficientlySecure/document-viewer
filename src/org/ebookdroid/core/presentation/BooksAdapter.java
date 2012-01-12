package org.ebookdroid.core.presentation;

import org.ebookdroid.R;
import org.ebookdroid.core.IBrowserActivity;
import org.ebookdroid.core.settings.SettingsManager;
import org.ebookdroid.core.settings.books.BookSettings;
import org.ebookdroid.core.views.BookshelfView;
import org.ebookdroid.utils.LengthUtils;

import _android.util.SparseArrayEx;

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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class BooksAdapter extends PagerAdapter implements FileSystemScanner.Listener, Iterable<BookShelfAdapter> {

    final IBrowserActivity base;

    final AtomicBoolean inScan = new AtomicBoolean();

    final SparseArrayEx<BookShelfAdapter> data = new SparseArrayEx<BookShelfAdapter>();

    final TreeMap<String, BookShelfAdapter> folders = new TreeMap<String, BookShelfAdapter>();

    private final static AtomicInteger SEQ = new AtomicInteger(1);

    private final RecentUpdater updater = new RecentUpdater();

    private final RecentAdapter recent;

    private final FileSystemScanner scanner;

    private final List<DataSetObserver> _dsoList = new ArrayList<DataSetObserver>();

    public BooksAdapter(final IBrowserActivity base, final RecentAdapter adapter) {
        this.base = base;
        this.recent = adapter;
        this.recent.registerDataSetObserver(updater);
        this.scanner = new FileSystemScanner(base);
        this.scanner.listeners.addListener(this);
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

    public synchronized BookShelfAdapter getList(final int index) {
        return data.valueAt(index);
    }

    public synchronized int getListCount() {
        return data.size();
    }

    public String getListName(final int currentList) {
        createRecent();
        final BookShelfAdapter list = getList(currentList);
        return list != null ? LengthUtils.safeString(list.name) : "";
    }

    public String getListPath(final int currentList) {
        createRecent();
        final BookShelfAdapter list = getList(currentList);
        return list != null ? LengthUtils.safeString(list.path) : "";
    }

    public synchronized List<String> getListNames() {
        createRecent();

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
        createRecent();

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

    public BookNode getItem(final int currentList, final int position) {
        createRecent();
        if (0 <= currentList && currentList < data.size()) {
            return getList(currentList).nodes.get(position);
        }
        throw new RuntimeException("Wrong list id: " + currentList + "/" + data.size());
    }

    public long getItemId(final int position) {
        return position;
    }

    public synchronized void clearData() {
        final BookShelfAdapter oldRecent = data.get(0);
        data.clear();
        folders.clear();
        SEQ.set(1);

        if (oldRecent != null) {
            data.append(0, oldRecent);
        } else {
            createRecent();
        }

        notifyDataSetChanged();
    }

    private synchronized BookShelfAdapter createRecent() {
        BookShelfAdapter a = data.get(0);
        if (a == null) {
            a = new BookShelfAdapter(base, 0, base.getContext().getString(R.string.recent_title), "");
            data.append(0, a);
        }
        return a;
    }

    public void startScan() {
        clearData();
        scanner.startScan(SettingsManager.getAppSettings().getAutoScanDirs());
    }

    public void stopScan() {
        scanner.stopScan();
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
            if (a == null) {
                a = new BookShelfAdapter(base, SEQ.getAndIncrement(), parent.getName(), dir);
                data.append(a.id, a);
                folders.put(dir, a);
                for (final File f : files) {
                    a.nodes.add(new BookNode(f));
                }
                notifyDataSetChanged();
            } else {
                for (final File f : files) {
                    a.nodes.add(new BookNode(f));
                }
                a.notifyDataSetChanged();
            }
        }
    }

    @Override
    public synchronized void onFileAdded(final File parent, final File f) {
        if (f != null) {
            final String dir = parent.getAbsolutePath();
            BookShelfAdapter a = folders.get(dir);
            if (a == null) {
                a = new BookShelfAdapter(base, SEQ.getAndIncrement(), parent.getName(), dir);
                data.append(a.id, a);
                folders.put(dir, a);
                a.nodes.add(new BookNode(f));
                Collections.sort(a.nodes);
                notifyDataSetChanged();
            } else {
                a.nodes.add(new BookNode(f));
                Collections.sort(a.nodes);
                a.notifyDataSetChanged();
            }
        }
    }

    @Override
    public synchronized void onFileDeleted(final File parent, final File f) {
        final String dir = parent.getAbsolutePath();
        final BookShelfAdapter a = folders.get(dir);
        if (a != null) {
            final String path = f.getAbsolutePath();
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
                    return;
                }
            }
        }
    }

    @Override
    public void onDirAdded(final File parent, final File f) {
        scanner.startScan(f.getAbsolutePath());
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

    public void registerDataSetObserver(final DataSetObserver dataSetObserver) {
        _dsoList.add(dataSetObserver);
    }

    private void notifyDataSetInvalidated() {
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
            final BookShelfAdapter ra = createRecent();
            ra.nodes.clear();
            final int count = recent.getCount();
            for (int i = 0; i < count; i++) {
                final BookSettings item = recent.getItem(i);
                final File file = new File(item.fileName);
                final BookNode book = new BookNode(file);
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
}
