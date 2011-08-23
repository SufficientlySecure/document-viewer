package org.ebookdroid.core.views;

import org.ebookdroid.core.IBrowserActivity;
import org.ebookdroid.core.presentation.RecentAdapter;
import org.ebookdroid.core.settings.BookSettings;
import org.ebookdroid.core.settings.BookSettingsActivity;

import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;

import java.io.File;

public class RecentBooksView extends android.widget.ListView implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {

    protected final IBrowserActivity base;

    protected final RecentAdapter adapter;

    public RecentBooksView(final IBrowserActivity base, final RecentAdapter adapter) {
        super(base.getContext());

        this.base = base;
        this.adapter = adapter;

        setAdapter(adapter);
        setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);
        setOnItemClickListener(this);
        setOnItemLongClickListener(this);
        setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
                ViewGroup.LayoutParams.FILL_PARENT));
    }

    @Override
    public void onItemClick(final AdapterView<?> adapterView, final View view, final int i, final long l) {
        final BookSettings bs = adapter.getItem(i);
        base.showDocument(Uri.fromFile(new File(bs.getFileName())));
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View arg1, int i, long l) {
        final BookSettings bs = adapter.getItem(i);
        final Intent intent = new Intent(base.getActivity(), BookSettingsActivity.class);

        intent.setData(Uri.fromFile(new File(bs.getFileName())));

        base.getActivity().startActivity(intent);

        return true;
    }
}
