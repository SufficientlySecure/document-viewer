package org.ebookdroid.core.presentation;

import org.ebookdroid.R;

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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class RecentAdapter extends BaseAdapter {

    private final Context context;
    private File currentDirectory;
    private List<File> files = Collections.emptyList();
    private final FileFilter filter;

    public RecentAdapter(final Context context, final FileFilter filter) {
        this.context = context;
        this.filter = filter;
    }

    @Override
    public int getCount() {
        return files.size();
    }

    @Override
    public File getItem(final int i) {
        return files.get(i);
    }

    @Override
    public long getItemId(final int i) {
        return i;
    }

    private String getFileSize(final long size) {

        if (size > 1073741824) {
            return String.format("%.2f", size / 1073741824.0) + " GB";
        } else if (size > 1048576) {
            return String.format("%.2f", size / 1048576.0) + " MB";
        } else if (size > 1024) {
            return String.format("%.2f", size / 1024.0) + " KB";
        } else {
            return size + " B";
        }

    }

    @Override
    public View getView(final int i, final View view, final ViewGroup viewGroup) {
        final View browserItem = LayoutInflater.from(context).inflate(R.layout.recentitem, viewGroup, false);
        
       
        final ImageView imageView = (ImageView) browserItem.findViewById(R.id.recentItemImage);
        final File file = files.get(i);
        final TextView textView = (TextView) browserItem.findViewById(R.id.recentItemText);
        textView.setText(file.getName());

        imageView.setImageResource(R.drawable.book);

        return browserItem;
    }

    public void setCurrentDirectory(final File currentDirectory) {
        final File[] fileArray = currentDirectory.listFiles(filter);
        final ArrayList<File> files = new ArrayList<File>(fileArray != null ? Arrays.asList(fileArray)
                : Collections.<File> emptyList());
        this.currentDirectory = currentDirectory;
        Collections.sort(files, new Comparator<File>() {

            @Override
            public int compare(final File o1, final File o2) {
                if (o1.isDirectory() && o2.isFile()) {
                    return -1;
                }
                if (o1.isFile() && o2.isDirectory()) {
                    return 1;
                }
                return o1.getName().compareTo(o2.getName());
            }
        });
        if (currentDirectory.getParentFile() != null) {
            files.add(0, currentDirectory.getParentFile());
        }
        setFiles(files);
    }

    public void setFiles(final List<File> files) {
        this.files = files;
        notifyDataSetInvalidated();
    }

    public File getCurrentDirectory() {
        return currentDirectory;
    }
}
