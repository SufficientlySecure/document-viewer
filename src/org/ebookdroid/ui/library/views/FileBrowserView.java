package org.ebookdroid.ui.library.views;

import org.ebookdroid.R;
import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.ui.library.IBrowserActivity;
import org.ebookdroid.ui.library.adapters.BrowserAdapter;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.util.Set;

public class FileBrowserView extends ListView implements AdapterView.OnItemClickListener,
        AdapterView.OnItemLongClickListener, DialogInterface.OnClickListener {

    private final IBrowserActivity base;
    private final BrowserAdapter adapter;

    private File selected;
    private boolean scannedDir;

    public FileBrowserView(final IBrowserActivity base, final BrowserAdapter adapter) {
        super(base.getContext());
        this.base = base;
        this.adapter = adapter;

        this.setAdapter(adapter);
        this.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);
        this.setOnItemClickListener(this);
        this.setOnItemLongClickListener(this);
    }

    @Override
    public void onItemClick(final AdapterView<?> adapterView, final View view, final int i, final long l) {
        selected = adapter.getItem(i);
        if (selected.isDirectory()) {
            base.setCurrentDir(selected);
        } else {
            base.showDocument(Uri.fromFile(selected));
        }
    }

    @Override
    public boolean onItemLongClick(final AdapterView<?> adapterView, final View view, final int i, final long l) {
        selected = adapter.getItem(i);

        if (selected.isDirectory()) {

            final Set<String> dirs = SettingsManager.getAppSettings().autoScanDirs;
            scannedDir = dirs.contains(selected.getPath());

            final AlertDialog.Builder builder = new AlertDialog.Builder(base.getActivity());
            builder.setTitle(selected.getName());
            builder.setItems((scannedDir) ? R.array.list_filebrowser_del : R.array.list_filebrowser_add, this);

            final AlertDialog alert = builder.create();
            alert.show();
        } else {
            showDialog("Path: " + selected.getParent() + "\nFile: " + selected.getName());
        }
        return false;
    }

    @Override
    public void onClick(final DialogInterface dialog, final int item) {
        switch (item) {
            case 0:
                base.setCurrentDir(selected);
                break;
            case 1:
                SettingsManager.changeAutoScanDirs(selected.getPath(), !scannedDir);
                adapter.notifyDataSetInvalidated();
                Toast.makeText(base.getActivity().getApplicationContext(), "Done.", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void showDialog(final String msg) {
        final AlertDialog alertDialog = new AlertDialog.Builder(base.getContext()).create();
        alertDialog.setTitle("Info");
        alertDialog.setMessage(msg);
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK", (DialogInterface.OnClickListener) null);
        alertDialog.setIcon(R.drawable.icon);
        alertDialog.show();
    }

}
