package org.ebookdroid.core.views;

import org.ebookdroid.core.IBrowserActivity;
import org.ebookdroid.core.presentation.FileListGridAdapter;
import org.ebookdroid.core.presentation.FileListGridAdapter.Node;

import android.net.Uri;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.AdapterView.OnItemClickListener;

import java.io.File;

public class LibraryGridView extends GridView implements OnItemClickListener {

    private IBrowserActivity base;
    private FileListGridAdapter adapter;

    public LibraryGridView(IBrowserActivity base, FileListGridAdapter adapter) {
        super(base.getContext());
        this.base = base;
        this.adapter = adapter;

        setNumColumns(AUTO_FIT);
        setStretchMode(STRETCH_SPACING);
        setGravity(0x11);
        setColumnWidth(205);
        setVerticalSpacing(10);
        setHorizontalSpacing(10);
        setAdapter(adapter);

        setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final FileListGridAdapter.Node node = (Node) adapter.getItem(position);
        File file = new File(node.getPath());
        if (!file.isDirectory()) {
            base.showDocument(Uri.fromFile(file));
        }
    }

}
