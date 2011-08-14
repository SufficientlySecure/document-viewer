package org.ebookdroid.core;

import org.ebookdroid.R;
import org.ebookdroid.core.presentation.BrowserAdapter;
import org.ebookdroid.core.settings.SettingsActivity;
import org.ebookdroid.core.settings.SettingsManager;
import org.ebookdroid.core.utils.DirectoryOrFileFilter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import java.io.File;
import java.io.FileFilter;


public class BrowserActivity extends Activity implements IBrowserActivity {

    private BrowserAdapter adapter;
    protected final FileFilter filter;
    private static final String CURRENT_DIRECTORY = "currentDirectory";

    private ViewFlipper viewflipper;
    private TextView header;

    private final AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {

        @Override
        @SuppressWarnings({ "unchecked" })
        public void onItemClick(final AdapterView<?> adapterView, final View view, final int i, final long l) {
            final File file = ((AdapterView<BrowserAdapter>) adapterView).getAdapter().getItem(i);
            if (file.isDirectory()) {
                setCurrentDir(file);
            } else {
                showDocument(file);
            }
        }
    };

    public BrowserActivity() {
        this.filter = createFileFilter();

    }

    protected FileFilter createFileFilter() {
        return new DirectoryOrFileFilter(new FileFilter() {

            @Override
            public boolean accept(final File pathname) {
                for (final String s : Activities.getAllExtensions()) {
                    if (pathname.getName().toLowerCase().endsWith("." + s)
                            && getSettings().getAppSettings().isFileTypeAllowed(s)) {
                        return true;
                    }
                }
                return false;
            }
        });
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.browser);
        final ListView browseList = initBrowserListView();

        header = (TextView) findViewById(R.id.browsertext);
        viewflipper = (ViewFlipper) findViewById(R.id.browserflip);
        viewflipper.addView(browseList);
    }

    @Override
    protected void onPostCreate(final Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        final File sdcardPath = new File("/sdcard");
        if (sdcardPath.exists()) {
            setCurrentDir(sdcardPath);
        } else {
            setCurrentDir(new File("/"));
        }
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

    public void showSettings(final View view) {
        final Intent i = new Intent(BrowserActivity.this, SettingsActivity.class);
        startActivity(i);
    }

    private void showDialog(final String msg) {
        final AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle("Info");
        alertDialog.setMessage(msg);
        alertDialog.setButton("OK", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(final DialogInterface dialog, final int which) {

                // here you can add functions
            }
        });
        alertDialog.setIcon(R.drawable.icon);
        alertDialog.show();
    }

    private ListView initListView(final BrowserAdapter adapter) {
        final ListView listView = new ListView(this);
        listView.setAdapter(adapter);
        listView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);
        listView.setOnItemClickListener(onItemClickListener);

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            @SuppressWarnings({ "unchecked" })
            public boolean onItemLongClick(final AdapterView<?> adapterView, final View view, final int i, final long l) {
                final File file = ((AdapterView<BrowserAdapter>) adapterView).getAdapter().getItem(i);

                if(file.isDirectory())
                {
                    //TODO: change app settings.
                    final CharSequence[] items = {"Set as scan directory"};

                    final AlertDialog.Builder builder = new AlertDialog.Builder(BrowserActivity.this);
                    builder.setTitle(file.getName());
                    builder.setItems(items, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(final DialogInterface dialog, final int item) {
                            Toast.makeText(getApplicationContext(), items[item], Toast.LENGTH_SHORT).show();
                        }
                    });
                    final AlertDialog alert = builder.create();
                    alert.show();
                }
                else
                    showDialog("Path: " + file.getParent() + "\nFile: " + file.getName());
                return false;
            }
        });

        listView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
                ViewGroup.LayoutParams.FILL_PARENT));
        return listView;
    }

    private ListView initBrowserListView() {
        adapter = new BrowserAdapter(this, filter);
        return initListView(adapter);
    }


    private void showDocument(final File file) {
        showDocument(Uri.fromFile(file));
    }

    @Override
    public void showDocument(final Uri uri) {
        final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
         intent.setClass(this, Activities.getByUri(uri));
        startActivity(intent);
    }

    private void setCurrentDir(final File newDir) {
        adapter.setCurrentDirectory(newDir);
        header.setText(newDir.getAbsolutePath());
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(CURRENT_DIRECTORY, adapter.getCurrentDirectory().getAbsolutePath());
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            final File parent = adapter.getCurrentDirectory().getParentFile();
            if (parent != null) {
                adapter.setCurrentDirectory(parent);
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
}
