package org.ebookdroid.core;

import org.ebookdroid.R;
import org.ebookdroid.core.actions.ActionEx;
import org.ebookdroid.core.actions.ActionMethod;
import org.ebookdroid.core.actions.ActionMethodDef;
import org.ebookdroid.core.actions.ActionTarget;
import org.ebookdroid.core.presentation.BrowserAdapter;
import org.ebookdroid.core.settings.SettingsManager;
import org.ebookdroid.core.settings.ui.SettingsUI;
import org.ebookdroid.core.utils.AndroidVersion;
import org.ebookdroid.core.utils.CompositeFilter;
import org.ebookdroid.core.utils.DirectoryFilter;
import org.ebookdroid.core.views.FileBrowserView;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import java.io.File;
import java.io.FileFilter;

@ActionTarget(
// action list
actions = {
        // start
        @ActionMethodDef(id = R.id.browserhome, method = "goHome"),
        @ActionMethodDef(id = R.id.browserupfolder, method = "goUp"),
        @ActionMethodDef(id = R.id.mainmenu_settings, method = "showSettings"),
        @ActionMethodDef(id = R.id.mainmenu_about, method = "showAbout"),
        @ActionMethodDef(id = R.id.browserrecent, method = "goRecent")
// finish
})
public class BrowserActivity extends AbstractActionActivity implements IBrowserActivity {

    private BrowserAdapter adapter;
    protected final FileFilter filter;
    private static final String CURRENT_DIRECTORY = "currentDirectory";

    private ViewFlipper viewflipper;
    private TextView header;

    public BrowserActivity() {
        this.filter = new CompositeFilter(false, DirectoryFilter.NOT_HIDDEN, SettingsManager.getAppSettings()
                .getAllowedFileTypes());
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.browser);

        adapter = new BrowserAdapter(filter);
        header = (TextView) findViewById(R.id.browsertext);
        viewflipper = (ViewFlipper) findViewById(R.id.browserflip);
        viewflipper.addView(new FileBrowserView(this, adapter));

        if (AndroidVersion.VERSION == 3) {
            setActionForView(R.id.browserhome);
            setActionForView(R.id.browserupfolder);
            setActionForView(R.id.browserrecent);
        }
    }

    @Override
    protected void onPostCreate(final Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        goHome(null);

        if (savedInstanceState != null) {
            final String absolutePath = savedInstanceState.getString(CURRENT_DIRECTORY);
            if (absolutePath != null) {
                setCurrentDir(new File(absolutePath));
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.browsermenu, menu);
        return true;
    }

    @ActionMethod(ids = R.id.browserhome)
    public void goHome(final ActionEx action) {
        final File sdcardPath = new File("/sdcard");
        if (sdcardPath.exists()) {
            setCurrentDir(sdcardPath);
        } else {
            setCurrentDir(new File("/"));
        }
    }

    @ActionMethod(ids = R.id.browserupfolder)
    public void goUp(final ActionEx action) {
        final File dir = adapter.getCurrentDirectory();
        final File parent = dir != null ? dir.getParentFile() : null;
        if (parent != null) {
            setCurrentDir(parent);
        }
    }

    @ActionMethod(ids = R.id.mainmenu_settings)
    public void showSettings(final ActionEx action) {
        SettingsUI.showAppSettings(this);
    }

    @ActionMethod(ids = R.id.browserrecent)
    public void goRecent(final ActionEx action) {
        final Intent myIntent = new Intent(BrowserActivity.this, RecentActivity.class);
        startActivity(myIntent);
        finish();
    }

    @Override
    public void showDocument(final Uri uri) {
        final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setClass(this, Activities.getByUri(uri));
        startActivity(intent);
    }

    @Override
    public void setCurrentDir(final File newDir) {
        final ImageView view = (ImageView) findViewById(R.id.browserupfolder);
        final boolean hasParent = newDir.getParentFile() != null;
        view.setImageResource(hasParent ? R.drawable.arrowup_enabled : R.drawable.arrowup_disabled);

        header.setText(newDir.getAbsolutePath());
        adapter.setCurrentDirectory(newDir);
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(CURRENT_DIRECTORY, adapter.getCurrentDirectory().getAbsolutePath());
    }

    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            final File dir = adapter.getCurrentDirectory();
            final File parent = dir != null ? dir.getParentFile() : null;
            if (parent != null) {
                setCurrentDir(parent);
            } else {
                finish();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public Activity getActivity() {
        return this;
    }

    @Override
    public void showProgress(final boolean show) {
    }

    @Override
    public void loadThumbnail(final String path, final ImageView imageView, final int defaultResID) {
        imageView.setImageResource(defaultResID);
    }
}
