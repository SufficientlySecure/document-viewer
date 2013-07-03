package org.ebookdroid.fontpack.ui;

import java.io.IOException;
import java.io.InputStream;

import org.ebookdroid.fontpack.FontpackApp;
import org.ebookdroid.fontpack.R;
import org.emdev.common.fonts.IFontProvider;
import org.emdev.common.fonts.data.FontPack;
import org.emdev.ui.AbstractActionActivity;
import org.emdev.ui.actions.ActionController;
import org.emdev.ui.actions.ActionDialogBuilder;
import org.emdev.ui.actions.ActionEx;
import org.emdev.ui.actions.ActionMethod;
import org.emdev.ui.tasks.BaseAsyncTask;

import android.os.Bundle;
import android.view.Menu;
import android.webkit.WebView;
import android.widget.CheckBox;

public class MainActivity extends AbstractActionActivity<MainActivity, ActionController<MainActivity>> {

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (FontpackApp.EBOOKDROID_VERSION == -1) {
            showErrorDlg(R.string.msg_no_ebookdroid);
            return;
        }

        if (0 < FontpackApp.EBOOKDROID_VERSION && FontpackApp.EBOOKDROID_VERSION < 1499) {
            showErrorDlg(R.string.msg_old_ebookdroid);
            return;
        }

        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing()) {
            System.exit(0);
        }
    }

    @Override
    protected ActionController<MainActivity> createController() {
        return new ActionController<MainActivity>(this);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @ActionMethod(ids = R.id.install)
    public void install(final ActionEx action) {
        new FontInstaller().execute(FontpackApp.afm);
    }

    @ActionMethod(ids = R.id.menu_about)
    public void about(final ActionEx action) {
        final ActionDialogBuilder b = new ActionDialogBuilder(this, getController());
        final WebView view = new WebView(this);

        final String content = getLicence();
        view.loadDataWithBaseURL("file:///fake/not_used", content, "text/html", "UTF-8", "");

        b.setTitle(R.string.menu_about);
        b.setView(view);
        b.setPositiveButton(android.R.string.ok, 0);
        b.show();
    }

    private String getLicence() {
        try {
            final InputStream input = getAssets().open("about.html");
            final int size = input.available();
            final byte[] buffer = new byte[size];
            input.read(buffer);
            input.close();
            final String text = new String(buffer, "UTF8");
            return text;
        } catch (final IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    @ActionMethod(ids = R.id.menu_close)
    public void close(final ActionEx action) {
        finish();
    }

    public void showErrorDlg(final int msgId, final Object... args) {
        final ActionDialogBuilder builder = new ActionDialogBuilder(this, getController());

        builder.setTitle(R.string.app_name);
        builder.setMessage(msgId, args);

        builder.setPositiveButton(R.string.menu_close, R.id.menu_close);
        builder.show();
    }

    public class FontInstaller extends BaseAsyncTask<IFontProvider, Boolean> {

        public FontInstaller() {
            super(MainActivity.this, R.string.msg_installing, false);
        }

        @Override
        protected Boolean doInBackground(final IFontProvider... params) {
            boolean res = true;
            for (final IFontProvider ifp : params) {
                for (final FontPack fp : ifp) {
                    publishProgress(getString(R.string.msg_installing_pack, fp.name));
                    res &= FontpackApp.esfm.install(fp);
                }
            }
            return res;
        }

        @Override
        protected void onPostExecute(final Boolean result) {
            super.onPostExecute(result);
            final CheckBox removeView = (CheckBox) findViewById(R.id.remove);
            if (result && removeView.isChecked()) {
                FontpackApp.uninstall();
                finish();
            }
        }
    }
}
