package org.ebookdroid.core;

import org.ebookdroid.R;
import org.ebookdroid.core.presentation.BrowserAdapter;
import org.ebookdroid.core.settings.SettingsActivity;
import org.ebookdroid.core.settings.SettingsManager;
import org.ebookdroid.core.utils.DirectoryOrFileFilter;
import org.ebookdroid.core.views.FileBrowserView;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import java.io.File;
import java.io.FileFilter;

public class BrowserActivity extends Activity implements IBrowserActivity {

    private BrowserAdapter adapter;
    protected final FileFilter filter;
    private static final String CURRENT_DIRECTORY = "currentDirectory";

    private ViewFlipper viewflipper;
    private TextView header;

    public BrowserActivity() {
        this.filter = new DirectoryOrFileFilter(SettingsManager.getAppSettings().getAllowedFileTypes(
                Activities.getAllExtensions()));
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.browser);

        adapter = new BrowserAdapter(this, filter);
        header = (TextView) findViewById(R.id.browsertext);
        viewflipper = (ViewFlipper) findViewById(R.id.browserflip);
        viewflipper.addView(new FileBrowserView(this, adapter));

        View.OnClickListener handler = new View.OnClickListener() {

            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.browserhome:
                        goHome(v);
                        break;
                    case R.id.browserrecent:
                        goRecent(v);
                        break;
                    case R.id.browserupfolder:
                        goUp(v);
                        break;
                }
            }
        };

        findViewById(R.id.browserhome).setOnClickListener(handler);
        findViewById(R.id.browserrecent).setOnClickListener(handler);
        findViewById(R.id.browserupfolder).setOnClickListener(handler);
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

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.browsermenu_settings:
                showSettings(null);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void goHome(View view) {
        final File sdcardPath = new File("/sdcard");
        if (sdcardPath.exists()) {
            setCurrentDir(sdcardPath);
        } else {
            setCurrentDir(new File("/"));
        }
    }

    public void goUp(final View view) {
        final File dir = adapter.getCurrentDirectory();
        final File parent = dir != null ? dir.getParentFile() : null;
        if (parent != null) {
            setCurrentDir(parent);
        }
    }

    public void showSettings(final View view) {
        final Intent i = new Intent(BrowserActivity.this, SettingsActivity.class);
        startActivity(i);
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

    public void goRecent(final View view) {
        final Intent myIntent = new Intent(BrowserActivity.this, RecentActivity.class);
        startActivity(myIntent);
        finish();
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
    public void showProgress(final boolean show)
    {

    }
}
