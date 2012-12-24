package org.emdev.common.filesystem;

import static android.os.FileObserver.CLOSE_WRITE;
import static android.os.FileObserver.CREATE;
import static android.os.FileObserver.DELETE;
import static android.os.FileObserver.MOVED_FROM;
import static android.os.FileObserver.MOVED_TO;

import android.app.Activity;
import android.os.FileObserver;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import org.emdev.common.cache.CacheManager;
import org.emdev.common.log.LogContext;
import org.emdev.common.log.LogManager;
import org.emdev.ui.actions.EventDispatcher;
import org.emdev.ui.actions.InvokationType;
import org.emdev.ui.tasks.AsyncTask;
import org.emdev.utils.FileUtils;
import org.emdev.utils.LengthUtils;
import org.emdev.utils.StringUtils;

public class FileSystemScanner {

    private static final LogContext LCTX = LogManager.root().lctx("FileSystemScanner", false);

    private static final int EVENT_MASK = CREATE | CLOSE_WRITE | MOVED_TO | DELETE | MOVED_FROM;

    final EventDispatcher listeners;
    final AtomicBoolean inScan = new AtomicBoolean();
    final Map<File, FileObserver> observers = new HashMap<File, FileObserver>();

    private ScanTask m_scanTask;

    public FileSystemScanner(final Activity activity) {
        this.listeners = new EventDispatcher(activity, InvokationType.AsyncUI, Listener.class, ProgressListener.class);
    }

    public void shutdown() {
        stopScan();
        stopObservers();
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
        // final String path = dir.getAbsolutePath();
        synchronized (observers) {
            // FileObserver fo = observers.get(path);
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

    public void stopObservers(final String path) {
        final String mpath = FileUtils.invertMountPrefix(path);
        final String ap = path + "/";
        final String mp = mpath != null ? mpath + "/" : null;

        synchronized (observers) {
            final Iterator<Entry<File, FileObserver>> iter = observers.entrySet().iterator();
            while (iter.hasNext()) {
                final Entry<File, FileObserver> next = iter.next();
                final File file = next.getKey();
                final String filePath = file.getAbsolutePath();
                final boolean eq = filePath.startsWith(ap) || filePath.equals(path) || mpath != null
                        && (filePath.startsWith(mp) || filePath.equals(mpath));
                if (eq) {
                    next.getValue().stopWatching();
                    iter.remove();
                }
            }
        }
    }

    public void addListener(final Object listener) {
        listeners.addListener(listener);
    }

    public void removeListener(final Object listener) {
        listeners.removeListener(listener);
    }

    public static String toString(final int event) {
        switch (event) {
            case FileObserver.ACCESS:
                return "ACCESS";
            case FileObserver.MODIFY:
                return "MODIFY";
            case FileObserver.ATTRIB:
                return "ATTRIB";
            case FileObserver.CLOSE_WRITE:
                return "CLOSE_WRITE";
            case FileObserver.CLOSE_NOWRITE:
                return "CLOSE_NOWRITE";
            case FileObserver.OPEN:
                return "OPEN";
            case FileObserver.MOVED_FROM:
                return "MOVED_FROM";
            case FileObserver.MOVED_TO:
                return "MOVED_TO";
            case FileObserver.CREATE:
                return "CREATE";
            case FileObserver.DELETE:
                return "DELETE";
            case FileObserver.DELETE_SELF:
                return "DELETE_SELF";
            case FileObserver.MOVE_SELF:
                return "MOVE_SELF";
            default:
                return "0x" + Integer.toHexString(event);
        }
    }

    class ScanTask extends AsyncTask<String, String, Void> {

        final FileExtensionFilter filter;

        final LinkedList<File> paths = new LinkedList<File>();

        public ScanTask(final FileExtensionFilter filter) {
            this.filter = filter;
        }

        @Override
        protected void onPreExecute() {
            final ProgressListener pl = listeners.getListener();
            pl.showProgress(true);
        }

        @Override
        protected Void doInBackground(final String... paths) {
            addPaths(paths);

            try {
                for (File dir = getDir(); dir != null && inScan.get(); dir = getDir()) {
                    scanDir(dir);
                }
            } finally {
                inScan.set(false);
            }
            return null;
        }

        @Override
        protected void onPostExecute(final Void v) {
            final ProgressListener pl = listeners.getListener();
            pl.showProgress(false);
        }

        void scanDir(final File dir) {
            // Checks if scan should be continued
            if (!inScan.get()) {
                return;
            }

            if (dir == null || !dir.isDirectory()) {
                return;
            }

            if (dir.getAbsolutePath().startsWith("/sys")) {
                LCTX.d("Skip system dir: " + dir);
                return;
            }

            try {
                final File cd = CacheManager.getCacheDir();
                if (cd != null && dir.getCanonicalPath().equals(cd.getCanonicalPath())) {
                    LCTX.d("Skip file cache: " + dir);
                    return;
                }
            } catch (final IOException ex) {
                ex.printStackTrace();
            }

            if (LCTX.isDebugEnabled()) {
                LCTX.d("Scan dir: " + dir);
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

    class FileObserverImpl extends FileObserver {

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

            int actualEvent = event & ALL_EVENTS;
            LCTX.d("0x" + Integer.toHexString(event) + " " + FileSystemScanner.toString(actualEvent) + ": "
                    + f.getAbsolutePath());

            switch (actualEvent) {
                case CREATE:
                    if (isDirectory) {
                        l.onDirAdded(folder, f);
                        getObserver(f).startWatching();
                    } else {
                        // Ignore file creation, wait for data writing
                    }
                    break;
                case CLOSE_WRITE:
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

    public static interface ProgressListener {

        public void showProgress(boolean show);

    }
}
