package org.ebookdroid.core.presentation;

import org.ebookdroid.R;
import org.ebookdroid.core.IBrowserActivity;
import org.ebookdroid.core.settings.SettingsManager;
import org.ebookdroid.core.utils.DirectoryFilter;
import org.ebookdroid.core.utils.FileExtensionFilter;
import org.ebookdroid.utils.FileUtils;
import org.ebookdroid.utils.StringUtils;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class FileListAdapter extends BaseExpandableListAdapter {

    final IBrowserActivity base;

    final AtomicBoolean inScan = new AtomicBoolean();

    final ArrayList<Node> data = new ArrayList<Node>();

    public FileListAdapter(final IBrowserActivity base) {
        this.base = base;
    }

    @Override
    public File getChild(final int groupPosition, final int childPosition) {
        return new File(data.get(groupPosition).getPath(), data.get(groupPosition).getList()[childPosition]);
    }

    @Override
    public long getChildId(final int groupPosition, final int childPosition) {
        return childPosition;
    }

    @Override
    public int getChildrenCount(final int groupPosition) {
        return data.get(groupPosition).getCount();
    }

    @Override
    public View getChildView(final int groupPosition, final int childPosition, final boolean isLastChild,
            final View convertView, final ViewGroup parent) {

        final ViewHolder holder = BaseViewHolder.getOrCreateViewHolder(ViewHolder.class, R.layout.browseritem,
                convertView, parent);

        final File file = getChild(groupPosition, childPosition);
        final boolean wasRead = SettingsManager.getBookSettings(file.getAbsolutePath()) != null;

        holder.name.setText(file.getName());
        holder.image.setImageResource(wasRead ? R.drawable.bookwatched : R.drawable.book);
        holder.info.setText(FileUtils.getFileDate(file.lastModified()));
        holder.fileSize.setText(FileUtils.getFileSize(file.length()));

        return holder.getView();
    }

    @Override
    public View getGroupView(final int groupPosition, final boolean isExpanded, final View convertView,
            final ViewGroup parent) {

        final ViewHolder holder = BaseViewHolder.getOrCreateViewHolder(ViewHolder.class, R.layout.browseritem,
                convertView, parent);

        final Node curr = getGroup(groupPosition);

        holder.name.setText(curr.getName());
        holder.image.setImageResource(R.drawable.folderopen);
        holder.info.setText("Books: " + curr.getCount());
        holder.fileSize.setText("");

        return holder.getView();
    }

    @Override
    public Node getGroup(final int groupPosition) {
        return data.get(groupPosition);
    }

    @Override
    public int getGroupCount() {
        return data.size();
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

    public void addNode(final Node node) {
        if (node != null) {
            data.add(node);
        }
    }

    public void clearData() {
        data.clear();
        notifyDataSetInvalidated();
    }

    public void startScan(final FileExtensionFilter filter) {
        if (inScan.compareAndSet(false, true)) {
            base.showProgress(true);
            clearData();
            new Thread(new ScanTask(filter)).start();
        }
    }

    public void stopScan() {
        inScan.set(false);
    }

    static class ViewHolder extends BaseViewHolder {

        TextView name;
        ImageView image;
        TextView info;
        TextView fileSize;

        @Override
        public void init(final View convertView) {
            super.init(convertView);
            this.name = (TextView) convertView.findViewById(R.id.browserItemText);
            this.image = (ImageView) convertView.findViewById(R.id.browserItemIcon);
            this.info = (TextView) convertView.findViewById(R.id.browserItemInfo);
            this.fileSize = (TextView) convertView.findViewById(R.id.browserItemfileSize);
        }
    }

    static class Node {

        private final String name;
        private final String path;
        private final String[] list;

        Node(final String name, final String path, final String[] list) {
            this.name = name;
            this.path = path;
            this.list = list;
        }

        String getName() {
            return this.name;
        }

        String getPath() {
            return this.path;
        }

        String[] getList() {
            return this.list;
        }

        int getCount() {
            return this.list.length;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

    class ScanTask implements Runnable {

        final FileExtensionFilter filter;

        final Queue<Node> currNodes = new ConcurrentLinkedQueue<Node>();

        final AtomicBoolean inUI = new AtomicBoolean();

        public ScanTask(final FileExtensionFilter filter) {
            this.filter = filter;
        }

        @Override
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

            for (final String path : SettingsManager.getAppSettings().getAutoScanDirs()) {
                // Scan each valid folder
                final File dir = new File(path);
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

        private void scanDir(final File dir) {
            // Checks if scan should be continued
            if (!inScan.get() || !dir.isDirectory() || dir.getAbsolutePath().startsWith("/sys")) {
                return;
            }
            final String[] list = dir.list(filter);
            if (list != null && list.length > 0) {
                Arrays.sort(list, StringUtils.getNaturalComparator());
                currNodes.add(new Node(dir.getName(), dir.getAbsolutePath(), list));
                if (inUI.compareAndSet(false, true)) {
                    // Start UI task if required
                    base.getActivity().runOnUiThread(this);
                }
            }
            // Retrieves files from current directory
            final File[] listOfDirs = dir.listFiles(DirectoryFilter.ALL);
            if (listOfDirs != null && inScan.get()) {
                for (int i = 0; i < listOfDirs.length; i++) {
                    // Recursively processing found file
                    scanDir(listOfDirs[i]);
                }
            }
        }
    }

}
