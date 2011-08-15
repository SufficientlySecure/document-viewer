package org.ebookdroid.core.presentation;

import org.ebookdroid.R;
import org.ebookdroid.core.settings.BookSettings;
import org.ebookdroid.utils.FileUtils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.FileFilter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class RecentAdapter extends BaseAdapter {

    private final Context context;
    private List<BookSettings> books = Collections.emptyList();

    public RecentAdapter(final Context context) {
        this.context = context;
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
    public View getView(final int i, View view, final ViewGroup viewGroup) {
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.recentitem, viewGroup, false);
        }
        final ImageView imageView = (ImageView) view.findViewById(R.id.recentItemIcon);

        final BookSettings bs = books.get(i);
        final File file = new File(bs.getFileName());

        final TextView name = (TextView) view.findViewById(R.id.recentItemName);
        name.setText(file.getName());

        imageView.setImageResource(R.drawable.book);
        final TextView info = (TextView) view.findViewById(R.id.recentItemInfo);
        info.setText(new SimpleDateFormat("dd MMM yyyy").format(file.lastModified()));

        final TextView fileSize = (TextView) view.findViewById(R.id.recentItemfileSize);
        fileSize.setText(FileUtils.getFileSize(file.length()));
        return view;
    }

    public void clearBooks() {
        this.books = Collections.emptyList();
    }

    public void setBooks(final Collection<BookSettings> books, final FileFilter filter) {
        if (filter != null) {
            this.books = new ArrayList<BookSettings>(books.size());
            for (final BookSettings bs : books) {
                final File f = new File(bs.getFileName());
                if (filter.accept(f)) {
                    this.books.add(bs);
                }
            }
        } else {
            this.books = new ArrayList<BookSettings>(books);
        }
        notifyDataSetInvalidated();
    }
}
