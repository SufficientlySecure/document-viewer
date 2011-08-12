package org.ebookdroid.core;

import org.ebookdroid.R;
import org.ebookdroid.cbdroid.CbrViewerActivity;
import org.ebookdroid.cbdroid.CbzViewerActivity;
import org.ebookdroid.core.presentation.FileListAdapter;
import org.ebookdroid.core.presentation.RecentAdapter;
import org.ebookdroid.core.settings.SettingsActivity;
import org.ebookdroid.core.settings.SettingsManager;
import org.ebookdroid.core.utils.DirectoryOrFileFilter;
import org.ebookdroid.core.views.LibraryView;
import org.ebookdroid.core.views.RecentBooksView;
import org.ebookdroid.djvudroid.DjvuViewerActivity;
import org.ebookdroid.pdfdroid.PdfViewerActivity;
import org.ebookdroid.xpsdroid.XpsViewerActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ViewFlipper;

import java.util.HashMap;

public class RecentActivity extends Activity implements IBrowserActivity {

    private int VIEW_RECENT = 0;
    private int VIEW_LIBRARY = 1;

    private RecentAdapter recentAdapter;
    private FileListAdapter libraryAdapter;

    private ViewFlipper viewflipper;
    private ImageView library;

    private final static HashMap<String, Class<? extends Activity>> extensionToActivity = new HashMap<String, Class<? extends Activity>>();

    static {
        extensionToActivity.put("pdf", PdfViewerActivity.class);
        extensionToActivity.put("djvu", DjvuViewerActivity.class);
        extensionToActivity.put("djv", DjvuViewerActivity.class);
        extensionToActivity.put("xps", XpsViewerActivity.class);
        extensionToActivity.put("cbz", CbzViewerActivity.class);
        extensionToActivity.put("cbr", CbrViewerActivity.class);
    }

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
                getSettings().deleteAllBookSettings();
                recentAdapter.clearBooks();
                return true;
            case R.id.recentmenu_settings:
                libraryAdapter.stopScan();
                final Intent i = new Intent(RecentActivity.this, SettingsActivity.class);
                startActivity(i);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        getSettings().clearCurrentBookSettings();

        DirectoryOrFileFilter filter = new DirectoryOrFileFilter(getSettings().getAppSettings().getAllowedFileTypes(
                extensionToActivity.keySet()));

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
    public void showDocument(final Uri uri) {
        final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        final String uriString = uri.toString();
        final String extension = uriString.substring(uriString.lastIndexOf('.') + 1);
        intent.setClass(this, extensionToActivity.get(extension.toLowerCase()));
        startActivity(intent);
    }

    private void changeLibraryView() {
        Log.i("www", "Curr view " + viewflipper.getDisplayedChild());
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
        // TODO: change
        final Intent myIntent = new Intent(RecentActivity.this, BrowserActivity.class);
        startActivity(myIntent);
    }
}
