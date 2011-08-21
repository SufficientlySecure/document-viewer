package org.ebookdroid.core.presentation;

import org.ebookdroid.R;
import org.ebookdroid.core.IBrowserActivity;
import org.ebookdroid.utils.FileUtils;
import org.ebookdroid.utils.LengthUtils;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class BrowserAdapter extends BaseAdapter implements Comparator<File> {

    private static final List<File> EMPTY_LIST = Collections.<File> emptyList();

    private final IBrowserActivity base;
    private final FileFilter filter;

    private File currentDirectory;
    private List<File> files = EMPTY_LIST;

    public BrowserAdapter(final IBrowserActivity base, final FileFilter filter) {
        this.base = base;
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

    @Override
    public View getView(final int i, View view, final ViewGroup viewGroup) {

        if (view == null) {
            view = LayoutInflater.from(base.getContext()).inflate(R.layout.browseritem, viewGroup, false);
        }

        final File file = files.get(i);

        final TextView textView = (TextView) view.findViewById(R.id.browserItemText);
        textView.setText(file.getName());

        final ImageView imageView = (ImageView) view.findViewById(R.id.browserItemIcon);

        if (file.isDirectory()) {
            boolean watched = base.getSettings().getAppSettings().getAutoScanDirs().contains(file.getPath());
            imageView.setImageResource(watched ? R.drawable.folderwatched : R.drawable.folderopen);

            //long len = file.list().length;
            /*
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
            */
            final TextView info = (TextView) view.findViewById(R.id.browserItemInfo);
            //info.setText("Folders: " + folders + " Books: " + books);
            info.setText("");
        } else {
            imageView.setImageResource(R.drawable.book);
            final TextView info = (TextView) view.findViewById(R.id.browserItemInfo);
            info.setText(FileUtils.getFileDate(file.lastModified()));

            final TextView fileSize = (TextView) view.findViewById(R.id.browserItemfileSize);
            fileSize.setText(FileUtils.getFileSize(file.length()));
        }
        return view;
    }

    public void setCurrentDirectory(final File currentDirectory) {
        this.currentDirectory = currentDirectory;

        final File[] fileArray = currentDirectory.listFiles(filter);

        List<File> files = EMPTY_LIST;
        if (LengthUtils.isNotEmpty(fileArray)) {
            files = new ArrayList<File>(Arrays.asList(fileArray));
            this.currentDirectory = currentDirectory;
            Collections.sort(files, this);
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

    @Override
    public int compare(final File f1, final File f2) {
        if (f1.isDirectory() && f2.isFile()) {
            return -1;
        }
        if (f1.isFile() && f2.isDirectory()) {
            return 1;
        }
        return f1.getName().compareTo(f2.getName());
    }
}
