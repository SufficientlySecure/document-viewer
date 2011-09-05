package org.ebookdroid.core.presentation;

import org.ebookdroid.R;
import org.ebookdroid.core.IBrowserActivity;
import org.ebookdroid.core.settings.SettingsManager;
import org.ebookdroid.core.utils.FileExtensionFilter;
import org.ebookdroid.utils.StringUtils;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class FileListGridAdapter extends BaseAdapter implements ListAdapter {

    final IBrowserActivity base;
    final AtomicBoolean inScan = new AtomicBoolean();

    public class Node {

        private final String name;
        private final String path;

        Node(String name, String path) {
            this.name = name;
            this.path = path;
        }

        String getName() {
            return this.name;
        }

        public String getPath() {
            return this.path;
        }

        public String toString() {
            return this.name;
        }
    }

    final ArrayList<Node> data = new ArrayList<Node>();

    public FileListGridAdapter(final IBrowserActivity base) {
        this.base = base;
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;
        TextView textView;
        if (convertView == null) { // if it's not recycled, initialize some attributes
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.thumbnail, parent, false);
        }

        final File cacheDir = base.getContext().getFilesDir();

        final String md5 = StringUtils.md5(data.get(position).getPath());
        final File thumbnailFile = new File(cacheDir, md5 + ".thumbnail");

        imageView = (ImageView) convertView.findViewById(R.id.thumbnailImage);
        textView = (TextView) convertView.findViewById(R.id.thumbnailText);
        if (thumbnailFile.exists()) {
            imageView.setImageURI(Uri.fromFile(thumbnailFile));
        } else {
            imageView.setImageResource(R.drawable.book);
        }
        textView.setText(data.get(position).getName());
        return convertView;
    }

    public void clearData() {
        data.clear();
        notifyDataSetInvalidated();
    }

    public void startScan(FileExtensionFilter filter) {
        if (inScan.compareAndSet(false, true)) {
            base.showProgress(true);
            clearData();
            new Thread(new ScanTask(filter)).start();
        }
    }

    public void stopScan() {
        inScan.set(false);
    }

    public void addNode(Node node) {
        if (node != null)
            data.add(node);
    }

    private class ScanTask implements Runnable {

        final FileExtensionFilter filter;

        final Queue<Node> currNodes = new ConcurrentLinkedQueue<Node>();

        final AtomicBoolean inUI = new AtomicBoolean();

        public ScanTask(FileExtensionFilter filter) {
            this.filter = filter;
        }

        public void run() {
            // Checks if we started to update adapter data
            if (!currNodes.isEmpty() && inUI.get()) {
                // Add files from queue to adapter
                for (Node n = currNodes.poll(); n != null && inScan.get(); n = currNodes.poll()) {
                    addNode(n);
                }
                notifyDataSetChanged();
                // Clear flag
                inUI.set(false);
                // Finish UI part
                return;
            }

            for (String path : SettingsManager.getAppSettings().getAutoScanDirs()) {
                // Scan each valid folder
                File dir = new File(path);
                if (dir.isDirectory()) {
                    scanDir(dir);
                }
            }

            // Check if queued files are available and no UI task started
            if (!currNodes.isEmpty() && inScan.get() && inUI.compareAndSet(false, true)) {
                // Start final UI task
                base.getActivity().runOnUiThread(this);
            }
            base.getActivity().runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    base.showProgress(false);
                }
            });

        }

        private class DirectoryFilter implements FileFilter {

            @Override
            public boolean accept(File file) {
                return file.isDirectory();
            }
        }

        private void scanDir(final File dir) {
            // Checks if scan should be continued
            if (!inScan.get() || !dir.isDirectory()) {
                return;
            }
            final File[] list = dir.listFiles((FilenameFilter) filter);
            if (list != null && list.length > 0) {
                Arrays.sort(list);
                for (File f : list) {
                    currNodes.add(new Node(f.getName(), f.getAbsolutePath()));
                    if (inUI.compareAndSet(false, true)) {
                        // Start UI task if required
                        base.getActivity().runOnUiThread(this);
                    }
                }
            }
            // Retrieves files from current directory
            final File[] listOfDirs = dir.listFiles(new DirectoryFilter());
            if (listOfDirs != null && inScan.get()) {
                for (int i = 0; i < listOfDirs.length; i++) {
                    // Recursively processing found file
                    scanDir(listOfDirs[i]);
                }
            }
        }
    }

}
