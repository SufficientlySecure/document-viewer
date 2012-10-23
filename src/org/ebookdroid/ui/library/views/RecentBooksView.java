package org.ebookdroid.ui.library.views;

import org.ebookdroid.ui.library.IBrowserActivity;
import org.ebookdroid.ui.library.adapters.BookNode;
import org.ebookdroid.ui.library.adapters.RecentAdapter;

import android.net.Uri;
import android.view.View;
import android.widget.AdapterView;

import java.io.File;

public class RecentBooksView extends android.widget.ListView implements AdapterView.OnItemClickListener {

    protected final IBrowserActivity base;

    protected final RecentAdapter adapter;

    public RecentBooksView(final IBrowserActivity base, final RecentAdapter adapter) {
        super(base.getContext());

        this.base = base;
        this.adapter = adapter;

        setAdapter(adapter);
        setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);
        setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(final AdapterView<?> adapterView, final View view, final int i, final long l) {
        final BookNode bs = adapter.getItem(i);
        base.showDocument(Uri.fromFile(new File(bs.path)), null);
    }
}
