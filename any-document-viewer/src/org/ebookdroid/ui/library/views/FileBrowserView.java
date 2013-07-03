package org.ebookdroid.ui.library.views;

import org.ebookdroid.ui.library.IBrowserActivity;
import org.ebookdroid.ui.library.adapters.BrowserAdapter;

import android.net.Uri;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.io.File;

public class FileBrowserView extends ListView implements AdapterView.OnItemClickListener {

    private final IBrowserActivity base;
    private final BrowserAdapter adapter;

    private File selected;

    public FileBrowserView(final IBrowserActivity base, final BrowserAdapter adapter) {
        super(base.getContext());
        this.base = base;
        this.adapter = adapter;

        this.setAdapter(adapter);
        this.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);
        this.setOnItemClickListener(this);

        base.getActivity().registerForContextMenu(this);
    }

    @Override
    public void onItemClick(final AdapterView<?> adapterView, final View view, final int i, final long l) {
        selected = adapter.getItem(i);
        if (selected.isDirectory()) {
            base.setCurrentDir(selected);
        } else {
            base.showDocument(Uri.fromFile(selected), null);
        }
    }
}
