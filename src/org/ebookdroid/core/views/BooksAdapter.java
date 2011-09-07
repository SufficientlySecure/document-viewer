package org.ebookdroid.core.views;

import org.ebookdroid.R;
import org.ebookdroid.core.IBrowserActivity;
import org.ebookdroid.core.settings.SettingsManager;
import org.ebookdroid.core.utils.FileExtensionFilter;
import org.ebookdroid.utils.LengthUtils;
import org.ebookdroid.utils.StringUtils;

import android.graphics.Color;
import android.net.Uri;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.FileFilter;
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
    
    public String getListName() {
        return LengthUtils.safeString(names.get(currentList));
    }
    
    @Override
    public int getCount() {
        return getList(currentList).size();
    }

    @Override
    public Object getItem(int position) {
        return getList(currentList).get(position);
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

        final String md5 = StringUtils.md5(getList(currentList).get(position).getPath());
        final File thumbnailFile = new File(cacheDir, md5 + ".thumbnail");

        imageView = (ImageView) convertView.findViewById(R.id.thumbnailImage);
        textView = (TextView) convertView.findViewById(R.id.thumbnailText);
        textView.setTextColor(Color.BLACK);
        if (thumbnailFile.exists()) {
            imageView.setImageURI(Uri.fromFile(thumbnailFile));
        } else {
            imageView.setImageResource(R.drawable.book);
        }
        textView.setText(StringUtils.cleanupTitle(getList(currentList).get(position).getName()));

        return convertView;
    }

    public void clearData() {
        data.clear();
        names.clear();
        SEQ.set(0);
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

    synchronized void addNode(Pair<Integer, Node> pair) {
        if (pair != null) {
            ArrayList<Node> list = getList(pair.first);
            list.add(pair.second);
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

    private class ScanTask implements Runnable {

        final FileExtensionFilter filter;

        final Queue<Pair<Integer, Node>> currNodes = new ConcurrentLinkedQueue<Pair<Integer, Node>>();

        final AtomicBoolean inUI = new AtomicBoolean();

        public ScanTask(FileExtensionFilter filter) {
            this.filter = filter;
        }

        public void run() {
            // Checks if we started to update adapter data
            if (!currNodes.isEmpty() && inUI.get()) {
                // Add files from queue to adapter
                for (Pair<Integer, Node> p = currNodes.poll(); p != null && inScan.get(); p = currNodes.poll()) {
                    addNode(p);
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
                int listNum = SEQ.getAndIncrement();
                names.put(listNum, dir.getName());
                for (File f : list) {
                    currNodes.add(new Pair<Integer, Node>(listNum, new Node(f.getName(), f.getAbsolutePath())));
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

    public String[] getListNames() {
        if (names.isEmpty()) {
            return null;
        }
        return names.values().toArray(new String[names.values().size()]);
    }

    public void setCurrentList(int index) {
        if (index >=0 && index < SEQ.get()) {
            currentList = index;
        }
        notifyDataSetChanged();
    }

}
