package org.ebookdroid.core;

import org.ebookdroid.R;
import org.ebookdroid.cbdroid.CbrViewerActivity;
import org.ebookdroid.cbdroid.CbzViewerActivity;
import org.ebookdroid.core.presentation.BrowserAdapter;
import org.ebookdroid.core.presentation.FileListAdapter;
import org.ebookdroid.core.settings.BookSettings;
import org.ebookdroid.core.settings.SettingsActivity;
import org.ebookdroid.core.settings.SettingsManager;
import org.ebookdroid.djvudroid.DjvuViewerActivity;
import org.ebookdroid.pdfdroid.PdfViewerActivity;
import org.ebookdroid.xpsdroid.XpsViewerActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainBrowserActivity extends Activity {

    private BrowserAdapter adapter;
    private BrowserAdapter recentAdapter;
    private FileListAdapter libraryAdapter;
    protected final FileFilter filter;
    private TabHost tabHost;
    private static final String CURRENT_DIRECTORY = "currentDirectory";

    private ArrayList<File> currFiles = null;

    private boolean scan = true;

    private final static HashMap<String, Class<? extends Activity>> extensionToActivity = new HashMap<String, Class<? extends Activity>>();

    static {
        extensionToActivity.put("pdf", PdfViewerActivity.class);
        extensionToActivity.put("djvu", DjvuViewerActivity.class);
        extensionToActivity.put("djv", DjvuViewerActivity.class);
        extensionToActivity.put("xps", XpsViewerActivity.class);
        extensionToActivity.put("cbz", CbzViewerActivity.class);
        extensionToActivity.put("cbr", CbrViewerActivity.class);
    }

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

    private final ExpandableListView.OnChildClickListener onChildClickListener = new ExpandableListView.OnChildClickListener() {

        @Override
        public boolean onChildClick(final ExpandableListView parent, final View v, final int groupPosition,
                final int childPosition, final long id) {
            // TODO: review this
            final File file = libraryAdapter.getChild(groupPosition, childPosition);
            if (file.isDirectory()) {
                setCurrentDir(file);
            } else {
                showDocument(file);
            }
            return false;
        }

    };

    public MainBrowserActivity() {
        this.filter = createFileFilter();

    }

    protected FileFilter createFileFilter() {
        return new FileFilter() {

            @Override
            public boolean accept(final File pathname) {
                for (final String s : extensionToActivity.keySet()) {
                    if (pathname.getName().toLowerCase().endsWith("." + s)
                            && PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getBoolean(
                                    "brfiletype" + s, true)) {
                        return true;
                    }
                }
                return pathname.isDirectory();
            }
        };
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.browser);
        final ListView browseList = initBrowserListView();
        final ListView recentListView = initRecentListView();
        tabHost = (TabHost) findViewById(R.id.browserTabHost);
        tabHost.setup();
        tabHost.addTab(tabHost.newTabSpec("Recent").setIndicator(getString(R.string.tab_recent))
                .setContent(new TabHost.TabContentFactory() {

                    @Override
                    public View createTabContent(final String s) {
                        return recentListView;
                    }
                }));
        tabHost.addTab(tabHost.newTabSpec("Browse").setIndicator(getString(R.string.tab_browse))
                .setContent(new TabHost.TabContentFactory() {

                    @Override
                    public View createTabContent(final String s) {
                        return browseList;
                    }
                }));

        final ExpandableListView libraryListView = initLibraryListView();

        tabHost.addTab(tabHost.newTabSpec("Library").setIndicator(getString(R.string.tab_files))
                .setContent(new TabHost.TabContentFactory() {

                    @Override
                    public View createTabContent(final String s) {
                        return libraryListView;
                    }
                }));

        tabHost.setOnTabChangedListener(new OnTabChangeListener() {

            @Override
            public void onTabChanged(final String tabId) {
                if (tabId.equals("Recent")) {
                    getWindow().setTitle(getString(R.string.tab_recent));
                } else if (tabId.equals("Browse")) {
                    getWindow().setTitle(adapter.getCurrentDirectory().getAbsolutePath());
                } else if (tabId.equals("Library")) {
                    getWindow().setTitle(getString(R.string.tab_files));
                    libraryAdapter.clearData();
                    final Thread thread = new Thread(null, loadListItems);
                    thread.start();
                }
            }
        });

    }

    // Runnable to load the items
    private final Runnable loadListItems = new Runnable() {

        @Override
        public void run() {
            currFiles = new ArrayList<File>();
            scanDir(new File(PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getString("brautoscandir",
                    "/sdcard")));
        }
    };

    private final Runnable returnRes = new Runnable() {

        @Override
        public void run() {
            synchronized (MainBrowserActivity.this) {
                if (currFiles != null && currFiles.size() > 0) {
                    for (int i = 0; i < currFiles.size(); i++) {
                        libraryAdapter.addFile(currFiles.get(i));
                        currFiles.remove(i);
                    }
                    libraryAdapter.notifyDataSetChanged();
                }
            }
        }
    };

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
            case R.id.browsermenu_cleanrecent:
                SettingsManager.getInstance(this).clearAllBookSettings();
                recentAdapter.setFiles(Collections.<File>emptyList());
                return true;
            case R.id.browsermenu_settings:
                scan = false;
                final Intent i = new Intent(MainBrowserActivity.this, SettingsActivity.class);
                startActivity(i);
                return true;
        }
        return super.onOptionsItemSelected(item);
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

    private ListView initRecentListView() {
        recentAdapter = new BrowserAdapter(this, filter);
        return initListView(recentAdapter);
    }

    private ExpandableListView initLibraryListView() {
        final ExpandableListView libraryListView = new ExpandableListView(this);
        libraryAdapter = new FileListAdapter(this, filter);
        libraryListView.setAdapter(libraryAdapter);
        // TODO: create correct group indicator
        // libraryListView.setGroupIndicator(getResources().getDrawable(R.drawable.group_indicator));
        libraryListView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);
        libraryListView.setOnChildClickListener(onChildClickListener);
        return libraryListView;
    }

    private void showDocument(final File file) {
        showDocument(Uri.fromFile(file));
    }

    protected void showDocument(final Uri uri) {
        final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        final String uriString = uri.toString();
        final String extension = uriString.substring(uriString.lastIndexOf('.') + 1);
        intent.setClass(this, extensionToActivity.get(extension.toLowerCase()));
        startActivity(intent);
    }

    private void setCurrentDir(final File newDir) {
        adapter.setCurrentDirectory(newDir);
        getWindow().setTitle(newDir.getAbsolutePath());
    }

    private void scanDir(final File file) {
        if (file.isFile()) {
            synchronized (MainBrowserActivity.this) {
                currFiles.add(file);
            }
            runOnUiThread(returnRes);
            /*
             * try {
             * Thread.sleep(1000); // Added for test. Remove in release
             * } catch (InterruptedException e) {
             * e.printStackTrace();
             * }
             */
        } else if (file.isDirectory()) {
            final File[] listOfFiles = file.listFiles(filter);
            if (listOfFiles != null) {
                for (int i = 0; i < listOfFiles.length; i++) {
                    scanDir(listOfFiles[i]);
                }
            }
        }
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(CURRENT_DIRECTORY, adapter.getCurrentDirectory().getAbsolutePath());
    }

    @Override
    protected void onResume() {
        super.onResume();
        Map<String, BookSettings> all = SettingsManager.getInstance(this).getAllBooksSettings();
        List<File> files = new ArrayList<File>(all.size());
        for(BookSettings bs : all.values()) {
            files.add(new File(bs.getFileName()));
        }
        recentAdapter.setFiles(files);

        if (scan == false) {
            scan = true;
            libraryAdapter.clearData();
            final Thread thread = new Thread(null, loadListItems);
            thread.start();
        }
    }

    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0
                && tabHost.getCurrentTabTag().equals("Browse")) {
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

}
