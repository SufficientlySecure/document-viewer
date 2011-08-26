package org.ebookdroid.core;

import org.ebookdroid.R;
import org.ebookdroid.core.log.LogContext;
import org.ebookdroid.core.presentation.FileListAdapter;
import org.ebookdroid.core.presentation.RecentAdapter;
import org.ebookdroid.core.settings.BookSettings;
import org.ebookdroid.core.settings.SettingsActivity;
import org.ebookdroid.core.settings.SettingsManager;
import org.ebookdroid.core.utils.FileExtensionFilter;
import org.ebookdroid.core.views.LibraryView;
import org.ebookdroid.core.views.RecentBooksView;

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
import android.widget.ProgressBar;
import android.widget.ViewFlipper;

import java.io.File;

public class RecentActivity extends Activity implements IBrowserActivity {

    public static final LogContext LCTX = LogContext.ROOT.lctx("Core");

    private static final int VIEW_RECENT = 0;
    private static final int VIEW_LIBRARY = 1;

    private RecentAdapter recentAdapter;
    private FileListAdapter libraryAdapter;

    private ViewFlipper viewflipper;
    private ImageView library;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.recent);

        recentAdapter = new RecentAdapter(this);
        libraryAdapter = new FileListAdapter(this);

        library = (ImageView) findViewById(R.id.recentlibrary);

        viewflipper = (ViewFlipper) findViewById(R.id.recentflip);
        viewflipper.addView(new RecentBooksView(this, recentAdapter), VIEW_RECENT);
        viewflipper.addView(new LibraryView(this, libraryAdapter), VIEW_LIBRARY);

        View.OnClickListener handler = new View.OnClickListener() {

            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.recentlibrary:
                        goLibrary(v);
                        break;
                    case R.id.recentbrowser:
                        goFileBrowser(v);
                        break;
                }
            }
        };

        findViewById(R.id.recentlibrary).setOnClickListener(handler);
        View recentBrowser = findViewById(R.id.recentbrowser);
        if (recentBrowser != null) {
            recentBrowser.setOnClickListener(handler);
        }

        boolean shouldLoad = SettingsManager.getAppSettings().isLoadRecentBook();
        BookSettings recent = SettingsManager.getRecentBook();
        File file = recent != null ? new File(recent.getFileName()) : null;
        boolean found = file != null ? file.exists() : false;

        if (LCTX.isDebugEnabled()) {
            LCTX.d("Last book: " + (file != null ? file.getAbsolutePath() : "") + ", found: " + found
                    + ", should load: " + shouldLoad);
        }

        if (shouldLoad && found) {
            showDocument(Uri.fromFile(file));
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        SettingsManager.clearCurrentBookSettings();
        SettingsManager.onSettingsChanged();

        viewflipper.setDisplayedChild(VIEW_RECENT);
        library.setImageResource(R.drawable.actionbar_library);

        recentAdapter.setBooks(SettingsManager.getAllBooksSettings().values(), SettingsManager.getAppSettings()
                .getAllowedFileTypes(Activities.getAllExtensions()));
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {

        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.recentmenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.recentmenu_cleanrecent:
                clearRecent(null);
                return true;
            case R.id.recentmenu_settings:
                showSettings(null);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void clearRecent(final View view) {
        SettingsManager.deleteAllBookSettings();
        recentAdapter.clearBooks();
    }

    public void showSettings(final View view) {
        libraryAdapter.stopScan();
        final Intent i = new Intent(RecentActivity.this, SettingsActivity.class);
        startActivity(i);
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
    public void setCurrentDir(final File newDir) {
    }

    @Override
    public void showDocument(final Uri uri) {
        final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setClass(this, Activities.getByUri(uri));
        startActivity(intent);
    }

    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            System.exit(0);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void changeLibraryView() {
        if (viewflipper.getDisplayedChild() == VIEW_RECENT) {
            viewflipper.setDisplayedChild(VIEW_LIBRARY);
            library.setImageResource(R.drawable.actionbar_recent);
            final FileExtensionFilter filter = SettingsManager.getAppSettings().getAllowedFileTypes(
                    Activities.getAllExtensions());
            libraryAdapter.startScan(filter);

        } else {
            viewflipper.setDisplayedChild(VIEW_RECENT);
            library.setImageResource(R.drawable.actionbar_library);
        }
    }

    public void goLibrary(final View view) {
        changeLibraryView();
    }

    public void goFileBrowser(final View view) {
        final Intent myIntent = new Intent(RecentActivity.this, BrowserActivity.class);
        startActivity(myIntent);
    }
    
    @Override
    public void showProgress(final boolean show)
    {
        final ProgressBar progress = (ProgressBar) findViewById(R.id.recentprogress);
        if (show) {
            progress.setVisibility(View.VISIBLE);
        } else {
            progress.setVisibility(View.GONE);
        }
    
    }
}
