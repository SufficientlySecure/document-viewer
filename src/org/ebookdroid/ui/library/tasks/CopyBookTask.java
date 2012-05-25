package org.ebookdroid.ui.library.tasks;

import org.ebookdroid.R;
import org.ebookdroid.common.cache.CacheManager;
import org.ebookdroid.common.cache.ThumbnailFile;
import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.common.settings.books.BookSettings;
import org.ebookdroid.ui.library.adapters.BookNode;
import org.ebookdroid.ui.library.adapters.RecentAdapter;

import android.content.Context;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.emdev.ui.progress.UIFileCopying;
import org.emdev.ui.tasks.BaseFileAsyncTask;
import org.emdev.utils.FileUtils;

public class CopyBookTask extends BaseFileAsyncTask<BookNode> {

    private final RecentAdapter recentAdapter;
    private final File targetFolder;

    private BookNode book;
    private File origin;

    public CopyBookTask(final Context context, final RecentAdapter recentAdapter, final File targetFolder) {
        super(context, R.string.book_copy_start, R.string.book_copy_complete, R.string.book_copy_error, false);
        this.recentAdapter = recentAdapter;
        this.targetFolder = targetFolder;
    }

    @Override
    protected FileTaskResult doInBackground(final BookNode... params) {
        book = params[0];
        origin = new File(book.path);
        final File target = new File(targetFolder, origin.getName());
        try {

            final UIFileCopying worker = new UIFileCopying(R.string.book_copy_progress, 256 * 1024, this);
            final BufferedInputStream in = new BufferedInputStream(new FileInputStream(origin), 256 * 1024);
            final BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(target), 256 * 1024);

            worker.copy(origin.length(), in, out);

            copyThumbnail(target);
            copyPageCache(target);

            return new FileTaskResult(target);
        } catch (final IOException ex) {
            return new FileTaskResult(ex);
        } catch (final Throwable th) {
            th.printStackTrace();
        }

        return null;
    }

    @Override
    protected void processTargetFile(final File target) {
        if (book.settings != null) {
            try {
                final BookSettings bs = SettingsManager.copyBookSettings(target, book.settings);
                if (recentAdapter != null) {
                    recentAdapter.replaceBook(null, bs);
                }
            } catch (final Throwable th) {
                th.printStackTrace();
            }
        }
        super.processTargetFile(target);
    }

    protected void copyThumbnail(final File target) {
        final ThumbnailFile ot = CacheManager.getThumbnailFile(book.path);
        if (ot.exists()) {
            try {
                final ThumbnailFile tt = CacheManager.getThumbnailFile(target.getAbsolutePath());
                final BufferedInputStream in = new BufferedInputStream(new FileInputStream(origin), 256 * 1024);
                final BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(tt), 256 * 1024);
                FileUtils.copy(in, out);
            } catch (IOException ex) {
                // TODO Auto-generated catch block
                ex.printStackTrace();
            }
        }
    }

    protected void copyPageCache(final File target) {
        final File ot = CacheManager.getPageFile(book.path);
        if (ot.exists()) {
            try {
                final File tt = CacheManager.getPageFile(target.getAbsolutePath());
                final BufferedInputStream in = new BufferedInputStream(new FileInputStream(origin), 256 * 1024);
                final BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(tt), 256 * 1024);
                FileUtils.copy(in, out);
            } catch (IOException ex) {
                // TODO Auto-generated catch block
                ex.printStackTrace();
            }
        }
    }

}
