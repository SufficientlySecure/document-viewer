package org.ebookdroid.ui.opds.adapters;

import org.ebookdroid.R;
import org.ebookdroid.opds.exceptions.AuthorizationRequiredException;
import org.ebookdroid.opds.exceptions.OPDSException;
import org.ebookdroid.opds.model.Feed;

import android.app.ProgressDialog;
import android.content.DialogInterface.OnCancelListener;

import org.emdev.ui.progress.IProgressIndicator;
import org.emdev.ui.tasks.BaseAsyncTask;
import org.emdev.utils.LengthUtils;

final class LoadFeedTask extends BaseAsyncTask<Feed, FeedTaskResult> implements OnCancelListener,
        IProgressIndicator {

    private final OPDSAdapter adapter;

    LoadFeedTask(OPDSAdapter adapter) {
        super(adapter.context, R.string.opds_connecting, true);
        this.adapter = adapter;
    }

    @Override
    protected FeedTaskResult doInBackground(final Feed... params) {
        final Feed f = params[0];
        try {
            final Feed feed = adapter.client.loadFeed(f, this);
            adapter.executor.startLoadThumbnails(feed);
            return new FeedTaskResult(feed);
        } catch (final OPDSException ex) {
            return new FeedTaskResult(f, ex);
        }
    }

    @Override
    protected void onPostExecute(final FeedTaskResult result) {
        super.onPostExecute(result);

        if (result.error instanceof AuthorizationRequiredException) {
            adapter.showAuthDlg(result);
        } else if (result.error != null) {
            adapter.showErrorDlg(R.string.opdsrefreshfolder, R.id.opdsrefreshfolder, result, result.error);
        }

        final FeedListener l = adapter.listeners.getListener();
        l.feedLoaded(result.feed);
        adapter.notifyDataSetChanged();

    }

    @Override
    public void setProgressDialogMessage(final int resourceID, final Object... args) {
        publishProgress(context.getResources().getString(resourceID, args));
    }

    @Override
    protected void onProgressUpdate(final String... values) {
        final int length = LengthUtils.length(values);
        if (length == 0) {
            return;
        }
        final String last = values[length - 1];
        try {
            if (progressDialog == null || !progressDialog.isShowing()) {
                progressDialog = ProgressDialog.show(context, "", last, true);
                progressDialog.setCancelable(true);
                progressDialog.setCanceledOnTouchOutside(true);
                progressDialog.setOnCancelListener(this);
            } else {
                progressDialog.setMessage(last);
            }
        } catch (final Throwable th) {
        }
    }
}