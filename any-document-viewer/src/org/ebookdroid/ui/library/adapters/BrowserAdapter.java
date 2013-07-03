package org.ebookdroid.ui.library.adapters;

import org.ebookdroid.R;
import org.ebookdroid.common.settings.LibSettings;
import org.ebookdroid.common.settings.SettingsManager;

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
import java.util.Set;

import org.emdev.ui.adapters.BaseViewHolder;
import org.emdev.utils.FileUtils;
import org.emdev.utils.LengthUtils;

public class BrowserAdapter extends BaseAdapter implements Comparator<File> {

    private final FileFilter filter;

    private File currentDirectory;
    private List<File> files = Collections.emptyList();

    public BrowserAdapter(final FileFilter filter) {
        this.filter = filter;
    }

    @Override
    public int getCount() {
        return files.size();
    }

    @Override
    public File getItem(final int i) {
        return 0 <= i && i < files.size() ? files.get(i) : null;
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
        String ap = file.getAbsolutePath();

        holder.textView.setText(file.getName());

        if (file.isDirectory()) {
            Set<String> autoScanDirs = LibSettings.current().autoScanDirs;
            String mp = FileUtils.invertMountPrefix(ap);
            final boolean watched = autoScanDirs.contains(ap) || (mp != null && autoScanDirs.contains(mp));
            holder.imageView.setImageResource(watched ? R.drawable.browser_item_folder_watched : R.drawable.browser_item_folder_open);
            holder.info.setText("");
            holder.fileSize.setText("");
        } else {
            final boolean wasRead = SettingsManager.getBookSettings(ap) != null;
            holder.imageView.setImageResource(wasRead ? R.drawable.recent_item_book_watched : R.drawable.browser_item_book);
            holder.info.setText(FileUtils.getFileDate(file.lastModified()));
            holder.fileSize.setText(FileUtils.getFileSize(file.length()));
        }

        return holder.getView();
    }

    public void setCurrentDirectory(final File currentDirectory) {
        if (currentDirectory.getAbsolutePath().startsWith("/sys")) {
            return;
        }
        this.currentDirectory = currentDirectory;

        final File[] files = currentDirectory.listFiles(filter);

        if (LengthUtils.isNotEmpty(files)) {
            Arrays.sort(files, this);
        }

        setFiles(files);
    }

    private void setFiles(final File[] files) {
        final List<File> ff = LengthUtils.isNotEmpty(files) ? new ArrayList<File>(Arrays.asList(files)) : new ArrayList<File>();
        this.files = ff;
        notifyDataSetChanged();
    }

    public File getCurrentDirectory() {
        return currentDirectory;
    }

    public void remove(final File file) {
        if (files.remove(file)) {
            notifyDataSetChanged();
        }
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

    public static class ViewHolder extends BaseViewHolder {

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
}
