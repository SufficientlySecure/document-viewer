package org.ebookdroid.core.presentation;

import org.ebookdroid.R;
import org.ebookdroid.core.settings.SettingsManager;
import org.ebookdroid.utils.FileUtils;
import org.ebookdroid.utils.LengthUtils;

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

    private final FileFilter filter;

    private File currentDirectory;
    private File[] files = null;

    public BrowserAdapter(final FileFilter filter) {
        this.filter = filter;
    }

    @Override
    public int getCount() {
        if (LengthUtils.isNotEmpty(files)) {
            return files.length;
        }
        return 0;
    }

    @Override
    public File getItem(final int i) {
        if (LengthUtils.isNotEmpty(files)) {
            return files[i];
        }
        return null;
    }

    @Override
    public long getItemId(final int i) {
        return i;
    }

    @Override
    public View getView(final int i, final View view, final ViewGroup parent) {

        final ViewHolder holder = BaseViewHolder.getOrCreateViewHolder(ViewHolder.class, R.layout.browseritem, view,
                parent);

        final File file = getItem(i);

        holder.textView.setText(file.getName());

        if (file.isDirectory()) {
            final boolean watched = SettingsManager.getAppSettings().getAutoScanDirs().contains(file.getPath());
            holder.imageView.setImageResource(watched ? R.drawable.folderwatched : R.drawable.folderopen);
            holder.info.setText("");
            holder.fileSize.setText("");
        } else {
            holder.imageView.setImageResource(R.drawable.book);
            holder.info.setText(FileUtils.getFileDate(file.lastModified()));
            holder.fileSize.setText(FileUtils.getFileSize(file.length()));
        }

        return holder.getView();
    }

    static class ViewHolder extends BaseViewHolder {

        TextView textView;
        ImageView imageView;
        TextView info;
        TextView fileSize;

        @Override
        public void init(final View convertView) {
            super.init(convertView);
            textView = (TextView) convertView.findViewById(R.id.browserItemText);
            imageView = (ImageView) convertView.findViewById(R.id.browserItemIcon);
            info = (TextView) convertView.findViewById(R.id.browserItemInfo);
            fileSize = (TextView) convertView.findViewById(R.id.browserItemfileSize);
        }
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
