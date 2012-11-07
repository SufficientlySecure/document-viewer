package org.ebookdroid.common.cache;

import org.ebookdroid.R;
import org.ebookdroid.common.settings.types.CacheLocation;

import android.content.Context;

import java.io.File;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;

import org.emdev.BaseDroidApp;
import org.emdev.common.filesystem.FilePrefixFilter;
import org.emdev.ui.progress.IProgressIndicator;
import org.emdev.ui.tasks.BaseAsyncTask;
import org.emdev.utils.FileUtils;
import org.emdev.utils.LengthUtils;
import org.emdev.utils.StringUtils;
import org.emdev.utils.listeners.ListenerProxy;

public class CacheManager extends org.emdev.common.cache.CacheManager {

    private static final Map<String, SoftReference<ThumbnailFile>> thumbmails = new HashMap<String, SoftReference<ThumbnailFile>>();

    public static final ListenerProxy listeners = new ListenerProxy(ICacheListener.class);

    public static void setCacheLocation(final CacheLocation cacheLocation, final boolean moveFiles) {
        File cacheDir = s_context.getFilesDir();
        if (cacheLocation == CacheLocation.Custom) {
            if (!BaseDroidApp.APP_STORAGE.equals(cacheDir)) {
                cacheDir = new File(BaseDroidApp.APP_STORAGE, "files");
            }
        }
        if (setCacheDir(cacheDir, moveFiles, null)) {
            thumbmails.clear();
        }
    }

    public static void moveCacheLocation(final Context context, final CacheLocation cacheLocation) {
        File cacheDir = s_context.getFilesDir();
        if (cacheLocation == CacheLocation.Custom) {
            if (!BaseDroidApp.APP_STORAGE.equals(cacheDir)) {
                cacheDir = new File(BaseDroidApp.APP_STORAGE, "files");
            }
        }

        new MoveLocationTask(context).execute(cacheDir);
    }

    public static ThumbnailFile getThumbnailFile(final String path) {
        final String amd5 = StringUtils.md5(path);
        final String mpath = FileUtils.invertMountPrefix(path);
        final String mmd5 = mpath != null ? StringUtils.md5(mpath) : null;

        SoftReference<ThumbnailFile> ref = thumbmails.get(amd5);
        ThumbnailFile file = ref != null ? ref.get() : null;
        if (file != null) {
            return file;
        }

        ref = thumbmails.get(mmd5);
        file = ref != null ? ref.get() : null;
        if (file != null) {
            return file;
        }

        file = new ThumbnailFile(path, s_cacheDir, amd5 + ".thumbnail");
        if (!file.exists()) {
            final ThumbnailFile f = new ThumbnailFile(path, s_cacheDir, mmd5 + ".thumbnail");
            file = f.exists() ? f : file;
        }

        thumbmails.put(amd5, new SoftReference<ThumbnailFile>(file));
        if (mmd5 != null) {
            thumbmails.put(mmd5, new SoftReference<ThumbnailFile>(file));
        }
        return file;
    }

    public static PageCacheFile getPageFile(final String path) {
        final String amd5 = StringUtils.md5(path);
        final PageCacheFile apcf = new PageCacheFile(s_cacheDir, amd5 + ".cache");
        if (apcf.exists()) {
            return apcf;
        }

        final String mpath = FileUtils.invertMountPrefix(path);
        final String mmd5 = mpath != null ? StringUtils.md5(mpath) : null;
        final PageCacheFile mpcf = new PageCacheFile(s_cacheDir, mmd5 + ".cache");
        if (mpcf.exists()) {
            return mpcf;
        }

        return apcf;
    }

    public static DocumentCacheFile getDocumentFile(final String path) {
        final String amd5 = StringUtils.md5(path);
        final DocumentCacheFile adcf = new DocumentCacheFile(s_cacheDir, amd5 + ".dcache");
        if (adcf.exists()) {
            return adcf;
        }

        final String mpath = FileUtils.invertMountPrefix(path);
        final String mmd5 = mpath != null ? StringUtils.md5(mpath) : null;
        final DocumentCacheFile mdcf = new DocumentCacheFile(s_cacheDir, mmd5 + ".dcache");
        if (mdcf.exists()) {
            return mdcf;
        }
        return adcf;
    }

    public static void clear() {
        thumbmails.clear();
        org.emdev.common.cache.CacheManager.clear();
    }

    public static void clear(final String path) {
        if (LengthUtils.isEmpty(path)) {
            return;
        }

        final String amd5 = StringUtils.md5(path);
        final String mpath = FileUtils.invertMountPrefix(path);
        final String mmd5 = mpath != null ? StringUtils.md5(mpath) : null;

        clearImpl(amd5);
        if (mmd5 != null) {
            clearImpl(mmd5);
        }
    }

    private static void clearImpl(final String md5) {
        thumbmails.remove(md5);
        final String[] files = s_cacheDir != null ? s_cacheDir.list(new FilePrefixFilter(md5 + ".")) : null;
        if (LengthUtils.isNotEmpty(files)) {
            for (final String file : files) {
                new File(s_cacheDir, file).delete();
            }
        }
    }

    private static final class MoveLocationTask extends BaseAsyncTask<File, Boolean> implements IProgressIndicator {

        public MoveLocationTask(final Context context) {
            super(context, R.string.cache_moving_text, false);
        }

        @Override
        protected Boolean doInBackground(final File... params) {
            return setCacheDir(params[0], true, this);
        }

        @Override
        protected void onPostExecute(final Boolean result) {
            if (result) {
                thumbmails.clear();
            }
            super.onPostExecute(result);
        }

        @Override
        public void setProgressDialogMessage(final int resourceID, final Object... args) {
            final String text = context.getResources().getString(R.string.cache_moving_progress, args);
            publishProgress(text);
        }
    }

    public static interface ICacheListener {

        void onThumbnailChanged(ThumbnailFile tf);

    }
}
