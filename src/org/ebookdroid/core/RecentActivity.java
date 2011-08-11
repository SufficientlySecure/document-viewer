package org.ebookdroid.core;


import org.ebookdroid.R;
import org.ebookdroid.cbdroid.CbrViewerActivity;
import org.ebookdroid.cbdroid.CbzViewerActivity;
import org.ebookdroid.core.presentation.RecentAdapter;
import org.ebookdroid.core.settings.BookSettings;
import org.ebookdroid.core.settings.SettingsManager;
import org.ebookdroid.djvudroid.DjvuViewerActivity;
import org.ebookdroid.pdfdroid.PdfViewerActivity;
import org.ebookdroid.xpsdroid.XpsViewerActivity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.ViewFlipper;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



public class RecentActivity extends Activity {

    private RecentAdapter recentAdapter;
    private ViewFlipper viewflipper;
    
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
            final BookSettings bs = ((AdapterView<RecentAdapter>) adapterView).getAdapter().getItem(i);
            showDocument(new File(bs.getFileName()));
        }
    };
    
    
    
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recent);      
        final ListView recentListView = initRecentListView();
        
        viewflipper=(ViewFlipper)findViewById(R.id.recentflip);
        viewflipper.addView(recentListView);

    }
    
    private ListView initListView(final RecentAdapter adapter) {
        final ListView listView = new ListView(this);
        listView.setAdapter(adapter);
        listView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);
        listView.setOnItemClickListener(onItemClickListener);

        listView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
                ViewGroup.LayoutParams.FILL_PARENT));
        return listView;
    }
    
    private ListView initRecentListView() {
        recentAdapter = new RecentAdapter(this);
        return initListView(recentAdapter);
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
    
    
    @Override
    protected void onResume() {
        super.onResume();

        SettingsManager.getInstance(this).clearCurrentBookSettings();

        Map<String, BookSettings> all = SettingsManager.getInstance(this).getAllBooksSettings();
        List<BookSettings> books = new ArrayList<BookSettings>(all.size());
        for(BookSettings bs : all.values()) {
            books.add(bs);
        }
        recentAdapter.setBooks(books);
    }
    public void goLibrary(View view)
    {
        //TODO: change 
        Intent myIntent = new Intent(RecentActivity.this, MainBrowserActivity.class);
        startActivity(myIntent);
    }
    
    public void goFileBrowser(View view)
    {
        //TODO: change 
        Intent myIntent = new Intent(RecentActivity.this, MainBrowserActivity.class);
        startActivity(myIntent);
    }

}
