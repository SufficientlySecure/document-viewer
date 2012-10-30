package org.ebookdroid.ui.library.adapters;

import org.ebookdroid.R;
import org.ebookdroid.ui.library.IBrowserActivity;
import org.ebookdroid.ui.library.adapters.BooksAdapter.ViewHolder;

import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

import org.emdev.ui.adapters.BaseViewHolder;
import org.emdev.utils.FileUtils;
import org.emdev.utils.StringUtils;

public class BookShelfAdapter extends BaseAdapter {

    private final IBrowserActivity base;
    private final IdentityHashMap<DataSetObserver, DataSetObserver> observers = new IdentityHashMap<DataSetObserver, DataSetObserver>();

    public final int id;
    public final String name;
    public final String path;
    public final String mpath;

    final List<BookNode> nodes = new ArrayList<BookNode>();
    public boolean measuring = false;

    public BookShelfAdapter(final IBrowserActivity base, final int index, final String name, final String path) {
        this.base = base;
        this.id = index;
        this.name = name;
        this.path = path;
        this.mpath = FileUtils.invertMountPrefix(path);
    }

    @Override
    public int getCount() {
        return nodes.size();
    }

    @Override
    public Object getItem(final int position) {
        return nodes.get(position);
    }

    @Override
    public long getItemId(final int position) {
        return position;
    }

    @Override
    public View getView(final int position, final View view, final ViewGroup parent) {
        final ViewHolder holder = BaseViewHolder.getOrCreateViewHolder(ViewHolder.class, R.layout.thumbnail, view,
                parent);

        final BookNode node = nodes.get(position);

        if (!measuring) {
            holder.textView.setText(StringUtils.cleanupTitle(node.name));
            base.loadThumbnail(node.path, holder.imageView, R.drawable.recent_item_book);
        }

        return holder.getView();
    }

    public String getPath() {
        return path;
    }

    @Override
    public void registerDataSetObserver(final DataSetObserver observer) {
        if (!observers.containsKey(observer)) {
            super.registerDataSetObserver(observer);
            observers.put(observer, observer);
        }
    }

    @Override
    public void unregisterDataSetObserver(final DataSetObserver observer) {
        if (null != observers.remove(observer)) {
            super.unregisterDataSetObserver(observer);
        }
    }
}
