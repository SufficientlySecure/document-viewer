package org.ebookdroid.core.presentation;

import org.ebookdroid.R;
import org.ebookdroid.core.IBrowserActivity;
import org.ebookdroid.core.settings.SettingsManager;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.FileFilter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class FileListAdapter extends BaseExpandableListAdapter implements Comparator<File> {

    final IBrowserActivity base;
    final Context context;
    final AtomicBoolean inScan = new AtomicBoolean();

    final List<File> dirsdata = new ArrayList<File>();
    final List<ArrayList<File>> data = new ArrayList<ArrayList<File>>();

    public FileListAdapter(final IBrowserActivity base) {
        this.base = base;
        this.context = base.getContext();
    }

    @Override
    public File getChild(final int groupPosition, final int childPosition) {
        return data.get(groupPosition).get(childPosition);
    }

    @Override
    public long getChildId(final int groupPosition, final int childPosition) {
        return childPosition;
    }

    @Override
    public int getChildrenCount(final int groupPosition) {
        return data.get(groupPosition).size();
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
    public View getChildView(final int groupPosition, final int childPosition, final boolean isLastChild,
            View convertView, final ViewGroup parent) {

        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.browseritem, parent, false);
        }
        final ImageView imageView = (ImageView) convertView.findViewById(R.id.browserItemIcon);
        final File file = getChild(groupPosition, childPosition);
        final TextView textView = (TextView) convertView.findViewById(R.id.browserItemText);
        textView.setText(file.getName());

        imageView.setImageResource(R.drawable.book);
        final TextView info = (TextView) convertView.findViewById(R.id.browserItemInfo);
        info.setText(new SimpleDateFormat("dd MMM yyyy").format(file.lastModified()));

        final TextView fileSize = (TextView) convertView.findViewById(R.id.browserItemfileSize);
        fileSize.setText(getFileSize(file.length()));
        return convertView;
    }

    @Override
    public View getGroupView(final int groupPosition, final boolean isExpanded, View convertView, final ViewGroup parent) {

        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.browseritem, parent, false);
        }
        final ImageView imageView = (ImageView) convertView.findViewById(R.id.browserItemIcon);
        final File file = getGroup(groupPosition);
        final TextView textView = (TextView) convertView.findViewById(R.id.browserItemText);
        textView.setText(file.getName());

        imageView.setImageResource(R.drawable.folderopen);

        int books = 0;
        int index = dirsdata.indexOf(file);
        if (index >= 0) {
            ArrayList<File> files = data.get(index);
            if (files != null) {
                books = files.size();
            }
        }

        final TextView info = (TextView) convertView.findViewById(R.id.browserItemInfo);
        info.setText("Books: " + books);

        return convertView;
    }

    @Override
    public File getGroup(final int groupPosition) {
        return dirsdata.get(groupPosition);
    }

    @Override
    public int getGroupCount() {
        return dirsdata.size();
    }

    @Override
    public long getGroupId(final int groupPosition) {
        return groupPosition;
    }

    @Override
    public boolean isChildSelectable(final int groupPosition, final int childPosition) {
        return true;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    public void addFile(final File file) {
        if (file.isDirectory()) {
            return;
        }
        final File parent = file.getParentFile();
        if (!dirsdata.contains(parent)) {
            dirsdata.add(parent);
        }
        final int pos = dirsdata.indexOf(parent);
        if (data.size() <= pos) {
            final ArrayList<File> a = new ArrayList<File>();
            data.add(pos, a);
        }
        data.get(pos).add(file);
        Collections.sort(data.get(pos), this);
    }

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

    public void clearData() {
        dirsdata.clear();
        data.clear();
        notifyDataSetInvalidated();
    }

    public void startScan(FileFilter filter) {
        if (inScan.compareAndSet(false, true)) {
            clearData();
            new Thread(new ScanTask(filter)).start();
        }
    }

    public void stopScan() {
        inScan.set(false);
    }

    private class ScanTask implements Runnable {

        private final FileFilter filter;

        private final Queue<File> currFiles = new ConcurrentLinkedQueue<File>();

        private final AtomicBoolean inUI = new AtomicBoolean();

        public ScanTask(FileFilter filter) {
            this.filter = filter;
        }

        public void run() {
            // Checks if we started to update adapter data
            if (!currFiles.isEmpty()) {
                // Add files from queue to adapter
                for (File f = currFiles.poll(); f != null && inScan.get(); f = currFiles.poll()) {
                    addFile(f);
                }
                // Clear flag
                inUI.set(false);
                // Finish UI part
                return;
            }

            // Retrieves paths to scan
            String[] paths = SettingsManager.getInstance(context).getAppSettings().getAutoScanDirs();
            for (String path : paths) {
                // Scan each valid folder
                File dir = new File(path);
                if (dir.isDirectory()) {
                    scanDir(dir);
                }
            }

            // Check if queued files are available and no UI task started
            if (!currFiles.isEmpty() && inScan.get() && inUI.compareAndSet(false, true)) {
                // Start final UI task
                base.getActivity().runOnUiThread(this);
            }
        }

        private void scanDir(final File file) {
            // Checks if scan should be continued
            if (!inScan.get()) {
                return;
            }
            // Checks parameter type
            if (file.isFile()) {
                // Add file to queue
                currFiles.add(file);
                if (currFiles.size() > 10 && inUI.compareAndSet(false, true)) {
                    // Start UI task if required
                    base.getActivity().runOnUiThread(this);
                }
            } else if (file.isDirectory()) {
                // Retrieves files from current directory
                final File[] listOfFiles = file.listFiles(filter);
                if (listOfFiles != null && inScan.get()) {
                    for (int i = 0; i < listOfFiles.length; i++) {
                        // Recursively processing found file
                        scanDir(listOfFiles[i]);
                    }
                }
            }
        }
    }

}
