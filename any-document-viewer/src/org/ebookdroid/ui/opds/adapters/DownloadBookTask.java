package org.ebookdroid.ui.opds.adapters;

import org.ebookdroid.R;
import org.ebookdroid.opds.exceptions.AuthorizationRequiredException;
import org.ebookdroid.opds.exceptions.OPDSException;
import org.ebookdroid.opds.model.Book;
import org.ebookdroid.opds.model.BookDownloadLink;

import android.content.DialogInterface.OnCancelListener;

import java.io.File;

import org.emdev.ui.progress.IProgressIndicator;
import org.emdev.ui.tasks.BaseFileAsyncTask;

final class DownloadBookTask extends BaseFileAsyncTask<Object, DownloadBookResult> implements
        OnCancelListener, IProgressIndicator {

    private final OPDSAdapter adapter;
    public DownloadBookTask(OPDSAdapter adapter) {
        super(adapter.context, R.string.opds_connecting, R.string.opds_download_complete,
                R.string.opds_download_error, true);
        this.adapter = adapter;
    }

    @Override
    protected DownloadBookResult doInBackground(final Object... params) {
        final Book book = (Book) params[0];
        final BookDownloadLink link = (BookDownloadLink) params[1];
        try {
            final File file = adapter.client.downloadBook(book, link, this);
            return new DownloadBookResult(book, link, file);
        } catch (final OPDSException ex) {
            return new DownloadBookResult(book, link, ex);
        }
    }

    @Override
    protected void onPostExecute(final DownloadBookResult result) {
        super.onPostExecute(result);
        if (result != null) {
            if (result.error instanceof AuthorizationRequiredException) {
                adapter.showAuthDlg(result);
            } else if (result.error instanceof OPDSException) {
                adapter.showErrorDlg(R.string.opds_retry_download, R.id.actions_retryDownloadBook, result,
                        (OPDSException) result.error);
            } else if (result.error != null) {
                adapter.showErrorDlg(R.string.opds_retry_download, R.id.actions_retryDownloadBook, result,
                        new OPDSException(result.error));
            }
        }
        adapter.startLoadThumbnails();
    }

    @Override
    protected void processError(final Throwable error) {
    }
}