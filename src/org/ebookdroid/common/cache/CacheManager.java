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
import org.emdev.utils.LengthUtils;
import org.emdev.utils.StringUtils;

public class CacheManager extends org.emdev.common.cache.CacheManager {

    private static final Map<String, SoftReference<ThumbnailFile>> thumbmails = new HashMap<String, SoftReference<ThumbnailFile>>();

    public static ThumbnailFile getThumbnailFile(final String path) {
        final String md5 = StringUtils.md5(path);

        final SoftReference<ThumbnailFile> ref = thumbmails.get(md5);
        ThumbnailFile file = ref != null ? ref.get() : null;
        if (file == null) {
            file = new ThumbnailFile(s_cacheDir, md5 + ".thumbnail");
            thumbmails.put(md5, new SoftReference<ThumbnailFile>(file));
        }

        return file;
    }

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

    public static PageCacheFile getPageFile(final String path) {
        final String md5 = StringUtils.md5(path);
        return new PageCacheFile(s_cacheDir, md5 + ".cache");
    }

    public static DocumentCacheFile getDocumentFile(final String path) {
        final String md5 = StringUtils.md5(path);
        return new DocumentCacheFile(s_cacheDir, md5 + ".dcache");
    }

    public static void clear() {
        thumbmails.clear();
        org.emdev.common.cache.CacheManager.clear();
    }

    public static void clear(final String path) {
        final String md5 = StringUtils.md5(path);
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
}
