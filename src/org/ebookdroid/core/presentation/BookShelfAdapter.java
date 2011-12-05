package org.ebookdroid.core.presentation;

import org.ebookdroid.R;
import org.ebookdroid.core.IBrowserActivity;
import org.ebookdroid.core.presentation.BooksAdapter.ViewHolder;
import org.ebookdroid.utils.StringUtils;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.List;

public class BookShelfAdapter extends BaseAdapter {

    private final IBrowserActivity base;

    final int id;
    final String name;
    final String path;

    final List<BookNode> nodes = new ArrayList<BookNode>();

    public BookShelfAdapter(IBrowserActivity base, int index, String name, String path) {
        this.base = base;
        this.id = index;
        this.name = name;
        this.path = path;
    }

    @Override
    public int getCount() {
        return nodes.size();
    }

    @Override
    public Object getItem(int position) {
        return nodes.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        final ViewHolder holder = BaseViewHolder.getOrCreateViewHolder(ViewHolder.class, R.layout.thumbnail, view,
                parent);

        BookNode node = nodes.get(position);

        holder.textView.setText(StringUtils.cleanupTitle(node.getName()));
        base.loadThumbnail(node.getPath(), holder.imageView, R.drawable.book);

        return holder.getView();
    }

    public String getPath() {
        return path;
    }
}