package org.ebookdroid.core.presentation;

import org.ebookdroid.R;
import org.ebookdroid.core.IBrowserActivity;
import org.ebookdroid.core.settings.SettingsManager;
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
import java.util.Arrays;
import java.util.Comparator;

public class BrowserAdapter extends BaseAdapter implements Comparator<File> {

    private final IBrowserActivity base;
    private final FileFilter filter;

    private File currentDirectory;
    private File[] files = null;

    public BrowserAdapter(final IBrowserActivity base, final FileFilter filter) {
        this.base = base;
        this.filter = filter;
    }

    @Override
    public int getCount() {
        if(LengthUtils.isNotEmpty(files))
            return files.length;
        return 0;
    }

    @Override
    public File getItem(final int i) {
        if(LengthUtils.isNotEmpty(files))
            return files[i];
        return null;
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

        final File file = getItem(i);

        final TextView textView = (TextView) view.findViewById(R.id.browserItemText);
        textView.setText(file.getName());

        final ImageView imageView = (ImageView) view.findViewById(R.id.browserItemIcon);

        if (file.isDirectory()) {
            boolean watched = SettingsManager.getAppSettings().getAutoScanDirs().contains(file.getPath());
            imageView.setImageResource(watched ? R.drawable.folderwatched : R.drawable.folderopen);

            final TextView info = (TextView) view.findViewById(R.id.browserItemInfo);
            //info.setText("Folders: " + folders + " Books: " + books);
            info.setText("");
            final TextView fileSize = (TextView) view.findViewById(R.id.browserItemfileSize);
            fileSize.setText("");
        } else {
            //final boolean wasRead = SettingsManager.getBookSettings(file.getAbsolutePath()) != null;
            //imageView.setImageResource(wasRead ? R.drawable.bookwatched : R.drawable.book);
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

        final File[] files = currentDirectory.listFiles(filter);

        if (LengthUtils.isNotEmpty(files)) {
            Arrays.sort(files, this);
        }
        setFiles(files);
    }

    private void setFiles(final File[] files) {
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
