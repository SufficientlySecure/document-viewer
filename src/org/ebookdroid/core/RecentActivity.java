package org.ebookdroid.core;

import org.ebookdroid.R;
import org.ebookdroid.core.log.EmergencyHandler;
import org.ebookdroid.core.presentation.FileListAdapter;
import org.ebookdroid.core.presentation.RecentAdapter;
import org.ebookdroid.core.settings.SettingsActivity;
import org.ebookdroid.core.settings.SettingsManager;
import org.ebookdroid.core.utils.DirectoryOrFileFilter;
import org.ebookdroid.core.views.LibraryView;
import org.ebookdroid.core.views.RecentBooksView;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ViewFlipper;

import java.io.File;

public class RecentActivity extends Activity implements IBrowserActivity {

    private static final int VIEW_RECENT = 0;
    private static final int VIEW_LIBRARY = 1;

    private RecentAdapter recentAdapter;
    private FileListAdapter libraryAdapter;

    private ViewFlipper viewflipper;
    private ImageView library;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EmergencyHandler.init(this);

        setContentView(R.layout.recent);

        recentAdapter = new RecentAdapter(this);
        libraryAdapter = new FileListAdapter(this);

        library = (ImageView) findViewById(R.id.recentlibrary);

        viewflipper = (ViewFlipper) findViewById(R.id.recentflip);
        viewflipper.addView(new RecentBooksView(this, recentAdapter), VIEW_RECENT);
        viewflipper.addView(new LibraryView(this, libraryAdapter), VIEW_LIBRARY);
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
        getSettings().deleteAllBookSettings();
        recentAdapter.clearBooks();
    }

    public void showSettings(final View view) {
        libraryAdapter.stopScan();
        final Intent i = new Intent(RecentActivity.this, SettingsActivity.class);
        startActivity(i);
    }

    @Override
    protected void onResume() {
        super.onResume();

        getSettings().clearCurrentBookSettings();

        final DirectoryOrFileFilter filter = new DirectoryOrFileFilter(getSettings().getAppSettings()
                .getAllowedFileTypes(Activities.getAllExtensions()));

        recentAdapter.setBooks(getSettings().getAllBooksSettings().values(), filter);

        libraryAdapter.startScan(filter);
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
    public SettingsManager getSettings() {
        return SettingsManager.getInstance(this);
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

    private void changeLibraryView() {
        if (viewflipper.getDisplayedChild() == VIEW_RECENT) {
            viewflipper.setDisplayedChild(VIEW_LIBRARY);
            library.setImageResource(R.drawable.actionbar_recent);
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
}
