package org.ebookdroid.ui.library.dialogs;

import org.ebookdroid.EBookDroidApp;
import org.ebookdroid.R;
import org.ebookdroid.ui.library.adapters.BrowserAdapter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.io.FileFilter;

import org.emdev.common.filesystem.DirectoryFilter;
import org.emdev.ui.actions.ActionController;
import org.emdev.ui.actions.ActionDialogBuilder;
import org.emdev.ui.actions.ActionEx;
import org.emdev.ui.actions.ActionMethod;
import org.emdev.ui.actions.IActionController;
import org.emdev.ui.actions.params.AbstractActionParameter;
import org.emdev.utils.LayoutUtils;

public class FolderDlg implements AdapterView.OnItemClickListener {

    public static final String SELECTED_FOLDER = "selected";

    protected final FileFilter filter;
    private BrowserAdapter adapter;

    private TextView header;
    private ListView filesView;

    private final IActionController<FolderDlg> controller;
    private final Context context;

    private File selected;
    private ImageView upButton;
    private ImageView homeButton;

    public FolderDlg(final IActionController<? extends Activity> controller) {
        this.filter = DirectoryFilter.NOT_HIDDEN;
        this.context = controller.getManagedComponent();
        this.controller = new ActionController<FolderDlg>(controller, this);
    }

    public void show(final File file, int titleId, final int okActionId) {
        final View view = LayoutInflater.from(context).inflate(R.layout.folder_dialog, null);

        adapter = new BrowserAdapter(filter);

        header = (TextView) view.findViewById(R.id.browsertext);
        filesView = (ListView) view.findViewById(R.id.browserview);
        upButton = (ImageView) view.findViewById(R.id.browserupfolder);
        homeButton = (ImageView) view.findViewById(R.id.browserhome);

        upButton.setOnClickListener(controller.getOrCreateAction(R.id.browserupfolder));
        homeButton.setOnClickListener(controller.getOrCreateAction(R.id.browserhome));

        filesView.setAdapter(adapter);
        filesView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);
        filesView.setOnItemClickListener(this);

        final ActionDialogBuilder builder = new ActionDialogBuilder(context, controller);

        builder.setTitle(titleId);
        builder.setView(view);
        builder.setPositiveButton(android.R.string.ok, okActionId, new SelectedFolder());
        builder.setNegativeButton();

        goHome(null);

        AlertDialog dlg = builder.show();
        LayoutUtils.maximizeWindow(dlg.getWindow());
    }

    public void show(final File file, int titleId, final int okActionId, final int cancelActionId) {
        final View view = LayoutInflater.from(context).inflate(R.layout.folder_dialog, null);

        adapter = new BrowserAdapter(filter);

        header = (TextView) view.findViewById(R.id.browsertext);
        filesView = (ListView) view.findViewById(R.id.browserview);
        upButton = (ImageView) view.findViewById(R.id.browserupfolder);
        homeButton = (ImageView) view.findViewById(R.id.browserhome);

        upButton.setOnClickListener(controller.getOrCreateAction(R.id.browserupfolder));
        homeButton.setOnClickListener(controller.getOrCreateAction(R.id.browserhome));

        filesView.setAdapter(adapter);
        filesView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);
        filesView.setOnItemClickListener(this);

        final ActionDialogBuilder builder = new ActionDialogBuilder(context, controller);

        builder.setTitle(titleId);
        builder.setView(view);
        builder.setPositiveButton(android.R.string.ok, okActionId, new SelectedFolder());
        builder.setNegativeButton(android.R.string.cancel, cancelActionId);

        goHome(null);

        AlertDialog dlg = builder.show();
        LayoutUtils.maximizeWindow(dlg.getWindow());
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
        final boolean hasParent = newDir.getParentFile() != null;
        upButton.setImageResource(hasParent ? R.drawable.browser_actionbar_nav_up_enabled : R.drawable.browser_actionbar_nav_up_disabled);

        selected = newDir;
        header.setText(newDir.getAbsolutePath());
        adapter.setCurrentDirectory(newDir);
    }

    @Override
    public void onItemClick(final AdapterView<?> adapterView, final View view, final int i, final long l) {
        final File selected = adapter.getItem(i);
        if (selected.isDirectory()) {
            setCurrentDir(selected);
        }
    }

    private class SelectedFolder extends AbstractActionParameter {

        public SelectedFolder() {
            super(SELECTED_FOLDER);
        }

        @Override
        public Object getValue() {
            return selected;
        }

    }
}
