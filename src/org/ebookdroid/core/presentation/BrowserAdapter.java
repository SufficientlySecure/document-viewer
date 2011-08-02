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

public class BrowserAdapter extends BaseAdapter {

    private final Context context;
    private File currentDirectory;
    private List<File> files = Collections.emptyList();
    private final FileFilter filter;

    public BrowserAdapter(final Context context, final FileFilter filter) {
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
        final View browserItem = LayoutInflater.from(context).inflate(R.layout.browseritem, viewGroup, false);
        final ImageView imageView = (ImageView) browserItem.findViewById(R.id.browserItemIcon);
        final File file = files.get(i);
        final TextView textView = (TextView) browserItem.findViewById(R.id.browserItemText);
        textView.setText(file.getName());

        if (currentDirectory != null && file.equals(currentDirectory.getParentFile())) {
            imageView.setImageResource(R.drawable.arrowup);
            textView.setText(file.getAbsolutePath());
        } else if (file.isDirectory()) {
            imageView.setImageResource(R.drawable.folderopen);
            final File[] listOfFiles = file.listFiles(filter);
            int folders = 0;
            int books = 0;
            if (listOfFiles != null) {
                for (int i1 = 0; i1 < listOfFiles.length; i1++) {
                    if (listOfFiles[i1].isDirectory()) {
                        folders++;
                    } else {
                        books++;
                    }
                }
            }

            final TextView info = (TextView) browserItem.findViewById(R.id.browserItemInfo);
            info.setText("Folders: " + folders + " Books: " + books);
        } else {
            imageView.setImageResource(R.drawable.book);
            final TextView info = (TextView) browserItem.findViewById(R.id.browserItemInfo);
            info.setText(new SimpleDateFormat("dd MMM yyyy").format(file.lastModified()));

            final TextView fileSize = (TextView) browserItem.findViewById(R.id.browserItemfileSize);
            fileSize.setText(getFileSize(file.length()));
        }
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
