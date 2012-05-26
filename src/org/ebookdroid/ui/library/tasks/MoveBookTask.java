package org.ebookdroid.ui.library.tasks;

import org.ebookdroid.R;
import org.ebookdroid.common.cache.CacheManager;
import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.common.settings.books.BookSettings;
import org.ebookdroid.ui.library.adapters.BookNode;
import org.ebookdroid.ui.library.adapters.RecentAdapter;

import android.content.Context;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.emdev.ui.progress.UIFileCopying;
import org.emdev.ui.tasks.BaseFileAsyncTask;

public class MoveBookTask extends BaseFileAsyncTask<BookNode> {

    private final RecentAdapter recentAdapter;
    private final File targetFolder;

    private BookNode book;
    private File origin;

    public MoveBookTask(final Context context, final RecentAdapter recentAdapter, final File targetFolder) {
        super(context, R.string.book_move_start, R.string.book_move_complete, R.string.book_move_error, false);
        this.recentAdapter = recentAdapter;
        this.targetFolder = targetFolder;
    }

    @Override
    protected FileTaskResult doInBackground(final BookNode... params) {
        book = params[0];
        origin = new File(book.path);
        try {
            final File target = move(new File(targetFolder, origin.getName()));
            if (target != null) {
                CacheManager.copy(book.path, target.getAbsolutePath(), true);
            }
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
        try {
            if (book.settings != null) {
                final BookSettings bs = SettingsManager.copyBookSettings(target, book.settings);
                if (recentAdapter != null) {
                    recentAdapter.replaceBook(book, bs);
                }
                SettingsManager.deleteBookSettings(book.settings);
            }
            if (origin.exists()) {
                origin.delete();
            }
        } catch (final Throwable th) {
            // TODO Auto-generated catch block
            th.printStackTrace();
        }
        super.processTargetFile(target);
    }

    protected File move(final File target) throws FileNotFoundException, IOException {
        if (origin.renameTo(target)) {
            return target;
        }

        final UIFileCopying worker = new UIFileCopying(R.string.opds_loading_book, 256 * 1024, this);
        final BufferedInputStream in = new BufferedInputStream(new FileInputStream(origin), 256 * 1024);
        final BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(target), 256 * 1024);

        worker.copy(origin.length(), in, out);

        return target;
    }
}
