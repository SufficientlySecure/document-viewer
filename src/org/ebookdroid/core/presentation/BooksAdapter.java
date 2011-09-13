package org.ebookdroid.core.presentation;

import org.ebookdroid.R;
import org.ebookdroid.core.IBrowserActivity;
import org.ebookdroid.core.settings.SettingsManager;
import org.ebookdroid.core.utils.DirectoryFilter;
import org.ebookdroid.core.utils.FileExtensionFilter;
import org.ebookdroid.utils.LengthUtils;
import org.ebookdroid.utils.StringUtils;

import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class BooksAdapter extends BaseAdapter {

    final IBrowserActivity base;
    final AtomicBoolean inScan = new AtomicBoolean();

    final TreeMap<Integer, ArrayList<Node>> data = new TreeMap<Integer, ArrayList<Node>>();
    final TreeMap<Integer, String> names = new TreeMap<Integer, String>();

    private final static AtomicInteger SEQ = new AtomicInteger(0);

    private int currentList = 0;

    public BooksAdapter(final IBrowserActivity base) {
        this.base = base;
    }

    public int getListCount() {
        return SEQ.get();
    }

    public synchronized void nextList() {
        if (currentList < SEQ.get() - 1) {
            currentList++;
        } else {
            currentList = 0;
        }
        notifyDataSetChanged();
    }

    public synchronized void prevList() {
        if (currentList > 0) {
            currentList--;
        } else {
            currentList = SEQ.get() - 1;
        }
        notifyDataSetChanged();
    }

    public synchronized void setCurrentList(final int index) {
        if (index >= 0 && index < SEQ.get()) {
            currentList = index;
        }
        notifyDataSetChanged();
    }

    public String getListName() {
        return LengthUtils.safeString(names.get(currentList));
    }

    public String[] getListNames() {
        if (names.isEmpty()) {
            return null;
        }
        return names.values().toArray(new String[names.values().size()]);
    }

    @Override
    public int getCount() {
        return getList(currentList).size();
    }

    @Override
    public Object getItem(final int position) {
        return getList(currentList).get(position);
    }

    @Override
    public long getItemId(final int position) {
        return position;
    }

    @Override
    public View getView(final int position, final View view, final ViewGroup parent) {

        final ViewHolder holder = BaseViewHolder.getOrCreateViewHolder(ViewHolder.class, R.layout.thumbnail, view,
                parent);

        holder.textView.setTextColor(Color.BLACK);
        holder.textView.setText(StringUtils.cleanupTitle(getList(currentList).get(position).getName()));

        base.loadThumbnail(getList(currentList).get(position).getPath(), holder.imageView, R.drawable.book);

        return holder.getView();
    }

    public void clearData() {
        data.clear();
        names.clear();
        SEQ.set(0);
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

    synchronized void addNode(final Node node) {
        if (node != null) {
            final ArrayList<Node> list = getList(node.listNum);
            list.add(node);
        }
    }

    private ArrayList<Node> getList(final int number) {
        ArrayList<Node> list = data.get(number);
        if (list == null) {
            list = new ArrayList<Node>();
            data.put(number, list);
        }
        return list;
    }

    static class ViewHolder extends BaseViewHolder {

        ImageView imageView;
        TextView textView;

        @Override
        public void init(final View convertView) {
            super.init(convertView);
            this.imageView = (ImageView) convertView.findViewById(R.id.thumbnailImage);
            this.textView = (TextView) convertView.findViewById(R.id.thumbnailText);
        }
    }

    public static class Node {

        final int listNum;
        final String name;
        final String path;

        Node(final int listNum, final String name, final String path) {
            this.listNum = listNum;
            this.name = name;
            this.path = path;
        }

        String getName() {
            return this.name;
        }

        public String getPath() {
            return this.path;
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
                for (Node p = currNodes.poll(); p != null && inScan.get(); p = currNodes.poll()) {
                    addNode(p);
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
            if (!inScan.get() || !dir.isDirectory()) {
                return;
            }
            final File[] list = dir.listFiles((FilenameFilter) filter);
            if (list != null && list.length > 0) {
                Arrays.sort(list, StringUtils.getNaturalFileComparator());
                final int listNum = SEQ.getAndIncrement();
                names.put(listNum, dir.getName());
                for (final File f : list) {
                    currNodes.add(new Node(listNum, f.getName(), f.getAbsolutePath()));
                    if (inUI.compareAndSet(false, true)) {
                        // Start UI task if required
                        base.getActivity().runOnUiThread(this);
                    }
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
