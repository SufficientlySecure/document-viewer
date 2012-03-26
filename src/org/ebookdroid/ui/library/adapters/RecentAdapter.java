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
import java.util.Collections;
import java.util.List;

import org.emdev.ui.adapters.BaseViewHolder;
import org.emdev.utils.FileUtils;
import org.emdev.utils.filesystem.FileExtensionFilter;

public class RecentAdapter extends BaseAdapter {

    final IBrowserActivity base;

    private List<BookSettings> books = Collections.emptyList();

    public RecentAdapter(IBrowserActivity base) {
        this.base = base;
    }

    @Override
    public int getCount() {
        return books.size();
    }

    @Override
    public BookSettings getItem(final int i) {
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

        final BookSettings bs = books.get(i);
        final File file = new File(bs.fileName);

        holder.name.setText(file.getName());

        // holder.imageView.setScaleType(ImageView.ScaleType.FIT_XY);
        base.loadThumbnail(file.getPath(), holder.imageView, R.drawable.book);

        holder.info.setText(FileUtils.getFileDate(file.lastModified()));
        holder.fileSize.setText(FileUtils.getFileSize(file.length()));

        return holder.getView();
    }

    public void clearBooks() {
        this.books = Collections.emptyList();
        notifyDataSetInvalidated();
    }

    public void setBooks(final Collection<BookSettings> books, final FileExtensionFilter filter) {
        if (filter != null) {
            this.books = new ArrayList<BookSettings>(books.size());
            for (final BookSettings bs : books) {
                if (filter.accept(bs.fileName)) {
                    this.books.add(bs);
                }
            }
        } else {
            this.books = new ArrayList<BookSettings>(books);
        }
        notifyDataSetInvalidated();
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
