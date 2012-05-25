package org.emdev.ui.tasks;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.AsyncTask;

import java.util.concurrent.atomic.AtomicBoolean;

import org.emdev.utils.LengthUtils;

public abstract class BaseAsyncTask<Params, Result> extends AsyncTask<Params, String, Result> implements
        OnCancelListener {

    protected final Context context;
    protected final int startProgressStringId;
    protected final boolean cancellable;
    protected final AtomicBoolean continueFlag = new AtomicBoolean(true);
    protected ProgressDialog progressDialog;

    public BaseAsyncTask(Context context, int startProgressStringId, boolean cancellable) {
        this.context = context;
        this.startProgressStringId = startProgressStringId;
        this.cancellable = cancellable;
    }

    @Override
    protected void onPreExecute() {
        onProgressUpdate(context.getResources().getString(startProgressStringId));
    }

    @Override
    public void onCancel(final DialogInterface dialog) {
        if (cancellable) {
            continueFlag.set(false);
            cancel(true);
        }
    }

    @Override
    protected void onPostExecute(final Result result) {
        closeProgressDialog();
    }

    protected void closeProgressDialog() {
        if (progressDialog != null) {
            try {
                progressDialog.dismiss();
            } catch (final Throwable th) {
            }
            progressDialog = null;
        }
    }

    @Override
    protected void onProgressUpdate(final String... values) {
        final int length = LengthUtils.length(values);
        if (length == 0) {
            return;
        }
        final String last = values[length - 1];
        if (progressDialog == null || !progressDialog.isShowing()) {
            progressDialog = ProgressDialog.show(context, "", last, true);
            if (cancellable) {
                progressDialog.setCancelable(true);
                progressDialog.setCanceledOnTouchOutside(true);
                progressDialog.setOnCancelListener(this);
            }
        } else {
            progressDialog.setMessage(last);
        }
    }
}
