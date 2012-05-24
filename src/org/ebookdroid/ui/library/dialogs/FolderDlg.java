package org.ebookdroid.ui.library.dialogs;

import org.ebookdroid.EBookDroidApp;
import org.ebookdroid.R;
import org.ebookdroid.ui.library.adapters.BrowserAdapter;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import java.io.File;
import java.io.FileFilter;

import org.emdev.ui.AbstractActionActivity;
import org.emdev.ui.actions.ActionEx;
import org.emdev.ui.actions.ActionMethod;
import org.emdev.ui.actions.ActionMethodDef;
import org.emdev.ui.actions.ActionTarget;
import org.emdev.utils.LayoutUtils;
import org.emdev.utils.android.AndroidVersion;
import org.emdev.utils.filesystem.DirectoryFilter;

@ActionTarget(
// action list
actions = {
        // start
        @ActionMethodDef(id = R.id.browserhome, method = "goHome"),
        @ActionMethodDef(id = R.id.browserupfolder, method = "goUp"),
// finish
})
public class FolderDlg extends AbstractActionActivity implements AdapterView.OnItemClickListener,
        AdapterView.OnItemLongClickListener {

    private static final String CURRENT_DIRECTORY = "currentDirectory";

    protected final FileFilter filter;
    private BrowserAdapter adapter;

    private ViewFlipper viewflipper;
    private TextView header;

    private ListView filesView;

    public FolderDlg() {
        this.filter = DirectoryFilter.NOT_HIDDEN;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.folder_dialog);

        LayoutUtils.maximizeWindow(getWindow());

        adapter = new BrowserAdapter(filter);
        header = (TextView) findViewById(R.id.browsertext);
        viewflipper = (ViewFlipper) findViewById(R.id.browserflip);
        filesView = new ListView(this);

        filesView.setAdapter(adapter);
        filesView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);
        filesView.setOnItemClickListener(this);
        filesView.setOnItemLongClickListener(this);

        viewflipper.addView(LayoutUtils.fillInParent(viewflipper, filesView));

        if (AndroidVersion.VERSION == 3) {
            setActionForView(R.id.browserhome);
            setActionForView(R.id.browserupfolder);
        }

        setResult(RESULT_CANCELED);
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(CURRENT_DIRECTORY, adapter.getCurrentDirectory().getAbsolutePath());
    }

    @Override
    protected void onPostCreate(final Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        goHome(null);

        if (savedInstanceState != null) {
            final String absolutePath = savedInstanceState.getString(CURRENT_DIRECTORY);
            if (absolutePath != null) {
                setCurrentDir(new File(absolutePath));
            }
        }
    }

    @ActionMethod(ids = R.id.browserhome)
    public void goHome(final ActionEx action) {
        if (EBookDroidApp.EXT_STORAGE.exists()) {
            setCurrentDir(EBookDroidApp.EXT_STORAGE);
        } else {
            setCurrentDir(new File("/"));
        }
    }

    @ActionMethod(ids = R.id.browserupfolder)
    public void goUp(final ActionEx action) {
        final File dir = adapter.getCurrentDirectory();
        final File parent = dir != null ? dir.getParentFile() : null;
        if (parent != null) {
            setCurrentDir(parent);
        }
    }

    public void setCurrentDir(final File newDir) {
        final ImageView view = (ImageView) findViewById(R.id.browserupfolder);
        final boolean hasParent = newDir.getParentFile() != null;
        view.setImageResource(hasParent ? R.drawable.arrowup_enabled : R.drawable.arrowup_disabled);

        header.setText(newDir.getAbsolutePath());
        adapter.setCurrentDirectory(newDir);
    }

    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            final File dir = adapter.getCurrentDirectory();
            final File parent = dir != null ? dir.getParentFile() : null;
            if (parent != null) {
                setCurrentDir(parent);
            } else {
                finish();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onItemClick(final AdapterView<?> adapterView, final View view, final int i, final long l) {
        File selected = adapter.getItem(i);
        if (selected.isDirectory()) {
            setCurrentDir(selected);
        }
    }

    @Override
    public boolean onItemLongClick(final AdapterView<?> adapterView, final View view, final int i, final long l) {
        File selected = adapter.getItem(i);

        if (selected.isDirectory()) {
            Intent startIntent = getIntent();
            Intent intent = new Intent(startIntent.getAction(), Uri.fromFile(selected));
            int actionId = startIntent.getIntExtra(ACTIVITY_RESULT_ACTION_ID, 0);
            intent.putExtra(ACTIVITY_RESULT_ACTION_ID, actionId);
            setResult(RESULT_OK, intent);
            finish();
        }
        return false;
    }

}
