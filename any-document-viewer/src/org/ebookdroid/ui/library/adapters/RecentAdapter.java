package org.ebookdroid.ui.library.adapters;

import org.ebookdroid.R;
import org.ebookdroid.common.settings.books.BookSettings;
import org.ebookdroid.ui.library.IBrowserActivity;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.emdev.common.filesystem.FileExtensionFilter;
import org.emdev.ui.adapters.BaseViewHolder;
import org.emdev.utils.FileUtils;

public class RecentAdapter extends BaseAdapter {

    final IBrowserActivity base;

    private final List<BookNode> books = new ArrayList<BookNode>();
    private final Map<String, BookNode> nodes = new HashMap<String, BookNode>();

    public RecentAdapter(final IBrowserActivity base) {
        this.base = base;
    }

    @Override
    public int getCount() {
        return books.size();
    }

    @Override
    public BookNode getItem(final int i) {
        return books.get(i);
    }

    @Override
    public long getItemId(final int i) {
        return i;
    }

    @Override
    public View getView(final int i, final View view, final ViewGroup parent) {
        final ViewHolder holder = BaseViewHolder.getOrCreateViewHolder(ViewHolder.class, R.layout.recentitem, view,
                parent);

        final BookNode node = books.get(i);
        final File file = new File(node.path);

        holder.name.setText(file.getName());

        base.loadThumbnail(node.path, holder.imageView, R.drawable.recent_item_book);

        holder.info.setText(FileUtils.getFileDate(file.lastModified()));
        holder.fileSize.setText(FileUtils.getFileSize(file.length()));

        return holder.getView();
    }

    public void clearBooks() {
        this.books.clear();
        this.nodes.clear();
        notifyDataSetChanged();
    }

    public BookNode getNode(final String path) {
        final BookNode n = nodes.get(path);
        if (n != null) {
            return n;
        }
        final String mpath = FileUtils.invertMountPrefix(path);
        return mpath != null ? nodes.get(mpath) : null;
    }

    public void setBooks(final Collection<BookSettings> books, final FileExtensionFilter filter) {
        this.books.clear();
        this.nodes.clear();
        for (final BookSettings bs : books) {
            if (filter == null || filter.accept(bs.fileName)) {
                final BookNode node = new BookNode(bs);
                addNode(node);
            }
        }
        notifyDataSetChanged();
    }

    public void addNode(final BookNode node) {
        this.books.add(node);
        nodes.put(node.path, node);
        if (node.mpath != null) {
            nodes.put(node.mpath, node);
        }
    }

    public void replaceBook(final BookNode origin, final BookSettings target) {
        if (origin != null) {
            final BookNode oldNode1 = nodes.remove(origin.path);
            final BookNode oldNode2 = origin.mpath != null ? nodes.remove(origin.mpath) : null;
            if (oldNode1 != null) {
                books.remove(oldNode1);
            }
            if (oldNode2 != null) {
                books.remove(oldNode2);
            }
            books.remove(origin);
        }

        final BookNode newNode = new BookNode(target);
        final BookNode prevNode1 = nodes.put(newNode.path, newNode);
        final BookNode prevNode2 = newNode.mpath != null ? nodes.put(newNode.mpath, newNode) : null;

        if (prevNode1 != null) {
            books.remove(prevNode1);
        }
        if (prevNode2 != null) {
            books.remove(prevNode2);
        }

        books.add(0, newNode);
        notifyDataSetChanged();
    }

    public void removeBook(final BookNode book) {
        if (books.remove(book)) {
            nodes.remove(book.path);
            if (book.mpath != null) {
                nodes.remove(book.mpath);
            }
            notifyDataSetChanged();
        }
    }

    public static class ViewHolder extends BaseViewHolder {

        TextView name;
        ImageView imageView;
        TextView info;
        TextView fileSize;

        @Override
        public void init(final View convertView) {
            super.init(convertView);
            name = (TextView) convertView.findViewById(R.id.recentItemName);
            imageView = (ImageView) convertView.findViewById(R.id.recentItemIcon);
            info = (TextView) convertView.findViewById(R.id.recentItemInfo);
            fileSize = (TextView) convertView.findViewById(R.id.recentItemfileSize);
        }
    }

}
