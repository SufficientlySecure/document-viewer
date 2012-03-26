package org.emdev.utils.filesystem;

import static android.os.FileObserver.CREATE;
import static android.os.FileObserver.DELETE;
import static android.os.FileObserver.MOVED_FROM;
import static android.os.FileObserver.MOVED_TO;

import org.ebookdroid.ui.library.IBrowserActivity;

import android.os.AsyncTask;
import android.os.FileObserver;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.emdev.ui.actions.EventDispatcher;
import org.emdev.ui.actions.InvokationType;
import org.emdev.utils.LengthUtils;
import org.emdev.utils.StringUtils;

public class FileSystemScanner {

    private static final int EVENT_MASK = CREATE | MOVED_TO | DELETE | MOVED_FROM;

    final IBrowserActivity base;
    final EventDispatcher listeners;
    final AtomicBoolean inScan = new AtomicBoolean();
    final Map<File, FileObserver> observers = new HashMap<File, FileObserver>();

    private ScanTask m_scanTask;

    public FileSystemScanner(final IBrowserActivity base) {
        this.base = base;
        this.listeners = new EventDispatcher(base.getActivity(), InvokationType.AsyncUI, Listener.class);
    }

    public void startScan(final FileExtensionFilter filter, final String... paths) {
        if (inScan.compareAndSet(false, true)) {
            m_scanTask = new ScanTask(filter);
            m_scanTask.execute(paths);
        } else {
            m_scanTask.addPaths(paths);
        }
    }

    public void startScan(final FileExtensionFilter filter, final Collection<String> paths) {
        final String[] arr = paths.toArray(new String[paths.size()]);
        if (inScan.compareAndSet(false, true)) {
            m_scanTask = new ScanTask(filter);
            m_scanTask.execute(arr);
        } else {
            m_scanTask.addPaths(arr);
        }
    }

    public boolean isScan() {
        return inScan.get();
    }

    public void stopScan() {
        if (inScan.compareAndSet(true, false)) {
            m_scanTask = null;
        }
    }

    public FileObserver getObserver(final File dir) {
        //final String path = dir.getAbsolutePath();
        synchronized (observers) {
            //FileObserver fo = observers.get(path);
            FileObserver fo = observers.get(dir);
            if (fo == null) {
                fo = new FileObserverImpl(dir);
                observers.put(dir, fo);
            }
            return fo;
        }
    }

    public void removeObserver(final File dir) {
        synchronized (observers) {
            observers.remove(dir);
        }
    }

    public void stopObservers() {
        synchronized (observers) {
            for (final FileObserver o : observers.values()) {
                o.stopWatching();
            }
            observers.clear();
        }
    }

    public void addListener(final Listener listener) {
        listeners.addListener(listener);
    }

    public void removeListener(final Listener listener) {
        listeners.removeListener(listener);
    }

    class ScanTask extends AsyncTask<String, String, Void> {

        final FileExtensionFilter filter;

        final LinkedList<File> paths = new LinkedList<File>();

        public ScanTask(final FileExtensionFilter filter) {
            this.filter = filter;
        }

        @Override
        protected void onPreExecute() {
            base.showProgress(true);
        }

        @Override
        protected Void doInBackground(final String... paths) {
            addPaths(paths);

            for (File dir = getDir(); dir != null && inScan.get(); dir = getDir()) {
                scanDir(dir);
            }
            return null;
        }

        @Override
        protected void onPostExecute(final Void v) {
            base.showProgress(false);
            inScan.set(false);
        }

        void scanDir(final File dir) {
            // Checks if scan should be continued
            if (!inScan.get() || !dir.isDirectory() || dir.getAbsolutePath().startsWith("/sys")) {
                return;
            }

            // Retrieves file observer for scanning folder
            final FileObserver observer = getObserver(dir);
            // Stop watching
            observer.stopWatching();

            // Retrieves listener
            final Listener l = listeners.getListener();

            // Retrieves file list
            final File[] files = dir.listFiles((FilenameFilter) filter);
            // Sort file list
            if (LengthUtils.isNotEmpty(files)) {
                Arrays.sort(files, StringUtils.NFC);
            }
            // Call the file scan callback
            l.onFileScan(dir, files);

            // Retrieves files from current directory
            final File[] childDirs = dir.listFiles(DirectoryFilter.ALL);
            // Immediately starts folder watching
            getObserver(dir).startWatching();

            if (LengthUtils.isNotEmpty(childDirs)) {
                // Sort child dir list
                Arrays.sort(childDirs, StringUtils.NFC);
                // Add children for deep ordered scanning
                synchronized (this) {
                    for (int i = childDirs.length - 1; i >= 0; i--) {
                        this.paths.addFirst(childDirs[i]);
                    }
                }
            }
        }

        synchronized void addPaths(final String... paths) {
            for (final String path : paths) {
                final File dir = new File(path);
                if (dir.exists() && dir.isDirectory()) {
                    this.paths.add(dir);
                }
            }
        }

        synchronized File getDir() {
            return this.paths.isEmpty() ? null : this.paths.removeFirst();
        }
    }

    public class FileObserverImpl extends FileObserver {

        private final File folder;

        public FileObserverImpl(final File folder) {
            super(folder.getAbsolutePath(), EVENT_MASK);
            this.folder = folder;
        }

        @Override
        public void onEvent(final int event, final String path) {
            if (folder == null || path == null) {
                return;
            }

            final File f = new File(folder, path);
            final boolean isDirectory = f.isDirectory();
            final Listener l = listeners.getListener();

            switch (event) {
                case CREATE:
                case MOVED_TO:
                    if (isDirectory) {
                        l.onDirAdded(folder, f);
                        getObserver(f).startWatching();
                    } else {
                        l.onFileAdded(folder, f);
                    }
                    break;
                case DELETE:
                case MOVED_FROM:
                    if (isDirectory) {
                        l.onDirDeleted(folder, f);
                        removeObserver(f);
                    } else {
                        l.onFileDeleted(folder, f);
                    }
                    break;
                default:
                    break;
            }
        }

    }

    public static interface Listener {

        void onFileScan(File parent, File[] files);

        void onFileAdded(File parent, File f);

        void onFileDeleted(File parent, File f);

        void onDirAdded(File parent, File f);

        void onDirDeleted(File parent, File f);
    }
}
