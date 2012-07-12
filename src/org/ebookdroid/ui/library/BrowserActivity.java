package org.ebookdroid.ui.library;

import org.ebookdroid.CodecType;
import org.ebookdroid.EBookDroidApp;
import org.ebookdroid.R;
import org.ebookdroid.common.cache.CacheManager;
import org.ebookdroid.common.settings.LibSettings;
import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.common.settings.books.BookSettings;
import org.ebookdroid.ui.library.adapters.BookNode;
import org.ebookdroid.ui.library.adapters.BrowserAdapter;
import org.ebookdroid.ui.library.dialogs.FolderDlg;
import org.ebookdroid.ui.library.tasks.CopyBookTask;
import org.ebookdroid.ui.library.tasks.MoveBookTask;
import org.ebookdroid.ui.library.tasks.RenameBookTask;
import org.ebookdroid.ui.library.views.FileBrowserView;
import org.ebookdroid.ui.opds.OPDSActivity;
import org.ebookdroid.ui.settings.SettingsUI;
import org.ebookdroid.ui.viewer.ViewerActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.EditText;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import java.io.File;
import java.io.FileFilter;

import org.emdev.ui.AbstractActionActivity;
import org.emdev.ui.actions.ActionController;
import org.emdev.ui.actions.ActionDialogBuilder;
import org.emdev.ui.actions.ActionEx;
import org.emdev.ui.actions.ActionMethod;
import org.emdev.ui.actions.ActionMethodDef;
import org.emdev.ui.actions.ActionTarget;
import org.emdev.ui.actions.params.Constant;
import org.emdev.ui.actions.params.EditableValue;
import org.emdev.utils.CompareUtils;
import org.emdev.utils.FileUtils;
import org.emdev.utils.LayoutUtils;
import org.emdev.utils.android.AndroidVersion;
import org.emdev.utils.filesystem.CompositeFilter;
import org.emdev.utils.filesystem.DirectoryFilter;
import org.emdev.utils.filesystem.PathFromUri;

@ActionTarget(
// action list
actions = {
        // start
        @ActionMethodDef(id = R.id.browserhome, method = "goHome"),
        @ActionMethodDef(id = R.id.browserupfolder, method = "goUp"),
        @ActionMethodDef(id = R.id.mainmenu_settings, method = "showSettings"),
        @ActionMethodDef(id = R.id.mainmenu_about, method = "showAbout"),
        @ActionMethodDef(id = R.id.browserrecent, method = "goRecent"),
        @ActionMethodDef(id = R.id.mainmenu_opds, method = "goOPDSBrowser"),
        @ActionMethodDef(id = R.id.bookmenu_open, method = "openBook"),
        @ActionMethodDef(id = R.id.bookmenu_removefromrecent, method = "removeBookFromRecents"),
        @ActionMethodDef(id = R.id.bookmenu_cleardata, method = "removeCachedBookFiles"),
        @ActionMethodDef(id = R.id.bookmenu_deletesettings, method = "removeBookSettings"),
        @ActionMethodDef(id = R.id.bookmenu_copy, method = "copyBook"),
        @ActionMethodDef(id = R.id.bookmenu_move, method = "copyBook"),
        @ActionMethodDef(id = R.id.actions_doCopyBook, method = "doCopyBook"),
        @ActionMethodDef(id = R.id.actions_doMoveBook, method = "doMoveBook"),
        @ActionMethodDef(id = R.id.bookmenu_rename, method = "renameBook"),
        @ActionMethodDef(id = R.id.actions_doRenameBook, method = "doRenameBook"),
        @ActionMethodDef(id = R.id.bookmenu_delete, method = "deleteBook"),
        @ActionMethodDef(id = R.id.actions_doDeleteBook, method = "doDeleteBook")

// finish
})
public class BrowserActivity extends AbstractActionActivity<BrowserActivity, ActionController<BrowserActivity>>
        implements IBrowserActivity {

    private BrowserAdapter adapter;
    protected final FileFilter filter;
    private static final String CURRENT_DIRECTORY = "currentDirectory";

    private ViewFlipper viewflipper;
    private TextView header;

    public BrowserActivity() {
        this.filter = new CompositeFilter(false, DirectoryFilter.NOT_HIDDEN, LibSettings.current().allowedFileTypes);
    }

    @Override
    protected ActionController<BrowserActivity> createController() {
        return new ActionController<BrowserActivity>(this);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.browser);

        adapter = new BrowserAdapter(filter);
        header = (TextView) findViewById(R.id.browsertext);
        viewflipper = (ViewFlipper) findViewById(R.id.browserflip);
        viewflipper.addView(LayoutUtils.fillInParent(viewflipper, new FileBrowserView(this, adapter)));

        if (AndroidVersion.VERSION == 3) {
            setActionForView(R.id.browserhome);
            setActionForView(R.id.browserupfolder);
            setActionForView(R.id.browserrecent);
        }
    }

    @Override
    protected void onPostCreate(final Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        goHome(null);

        Uri data = getIntent().getData();
        if (data != null) {
            setCurrentDir(new File(PathFromUri.retrieve(getContentResolver(), data)));
        } else if (savedInstanceState != null) {
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

    @ActionMethod(ids = R.id.mainmenu_settings)
    public void showSettings(final ActionEx action) {
        SettingsUI.showAppSettings(this);
    }

    @ActionMethod(ids = R.id.browserrecent)
    public void goRecent(final ActionEx action) {
        final Intent myIntent = new Intent(BrowserActivity.this, RecentActivity.class);
        startActivity(myIntent);
        finish();
    }

    @ActionMethod(ids = R.id.mainmenu_opds)
    public void goOPDSBrowser(final ActionEx action) {
        final Intent myIntent = new Intent(BrowserActivity.this, OPDSActivity.class);
        startActivity(myIntent);
    }

    @Override
    public void showDocument(final Uri uri) {
        final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setClass(this, ViewerActivity.class);
        startActivity(intent);
    }

    @Override
    public void setCurrentDir(final File newDir) {
        final ImageView view = (ImageView) findViewById(R.id.browserupfolder);
        final boolean hasParent = newDir.getParentFile() != null;
        view.setImageResource(hasParent ? R.drawable.arrowup_enabled : R.drawable.arrowup_disabled);

        header.setText(newDir.getAbsolutePath());
        adapter.setCurrentDirectory(newDir);
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(CURRENT_DIRECTORY, adapter.getCurrentDirectory().getAbsolutePath());
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
    public Context getContext() {
        return this;
    }

    @Override
    public Activity getActivity() {
        return this;
    }

    @Override
    public void showProgress(final boolean show) {
    }

    @Override
    public void loadThumbnail(final String path, final ImageView imageView, final int defaultResID) {
        imageView.setImageResource(defaultResID);
    }

    @Override
    public void onCreateContextMenu(final ContextMenu menu, final View v, final ContextMenuInfo menuInfo) {
        Object source = null;

        if (menuInfo instanceof AdapterContextMenuInfo) {
            final AbsListView list = (AbsListView) v;
            final AdapterContextMenuInfo mi = (AdapterContextMenuInfo) menuInfo;
            source = list.getAdapter().getItem(mi.position);
        } else if (menuInfo instanceof ExpandableListContextMenuInfo) {
            final ExpandableListView list = (ExpandableListView) v;
            final ExpandableListAdapter adapter = list.getExpandableListAdapter();
            final ExpandableListContextMenuInfo mi = (ExpandableListContextMenuInfo) menuInfo;
            final long pp = mi.packedPosition;
            final int group = ExpandableListView.getPackedPositionGroup(pp);
            final int child = ExpandableListView.getPackedPositionChild(pp);
            if (child >= 0) {
                source = adapter.getChild(group, child);
            } else {
                source = adapter.getGroup(group);
            }
        }

        if (source instanceof File) {
            final MenuInflater inflater = getMenuInflater();
            final File node = (File) source;
            final String path = node.getAbsolutePath();

            if (node.isDirectory()) {
                inflater.inflate(R.menu.library_menu, menu);
                menu.setHeaderTitle(path);
            } else {
                final BookSettings bs = SettingsManager.getBookSettings(path);
                inflater.inflate(R.menu.book_menu, menu);
                menu.setHeaderTitle(path);
                menu.findItem(R.id.bookmenu_recentgroup).setVisible(bs != null);
                menu.findItem(R.id.bookmenu_openbookshelf).setVisible(false);
                menu.findItem(R.id.bookmenu_openbookfolder).setVisible(false);
            }
        }

        setMenuSource(menu, source);
    }

    @ActionMethod(ids = R.id.bookmenu_open)
    public void openBook(final ActionEx action) {
        final File file = action.getParameter(AbstractActionActivity.MENU_ITEM_SOURCE);
        if (!file.isDirectory()) {
            showDocument(Uri.fromFile(file));
        }
    }

    @ActionMethod(ids = R.id.bookmenu_removefromrecent)
    public void removeBookFromRecents(final ActionEx action) {
        final File file = action.getParameter(AbstractActionActivity.MENU_ITEM_SOURCE);
        if (file != null) {
            SettingsManager.removeBookFromRecents(file.getAbsolutePath());
            adapter.notifyDataSetInvalidated();
        }
    }

    @ActionMethod(ids = R.id.bookmenu_cleardata)
    public void removeCachedBookFiles(final ActionEx action) {
        final File file = action.getParameter(AbstractActionActivity.MENU_ITEM_SOURCE);
        if (file != null) {
            CacheManager.clear(file.getAbsolutePath());
            adapter.notifyDataSetInvalidated();
        }
    }

    @ActionMethod(ids = R.id.bookmenu_deletesettings)
    public void removeBookSettings(final ActionEx action) {
        final File file = action.getParameter(AbstractActionActivity.MENU_ITEM_SOURCE);
        if (file != null) {
            final BookSettings bs = SettingsManager.getBookSettings(file.getAbsolutePath());
            if (bs != null) {
                SettingsManager.deleteBookSettings(bs);
                adapter.notifyDataSetInvalidated();
            }
        }
    }

    @ActionMethod(ids = { R.id.bookmenu_copy, R.id.bookmenu_move })
    public void copyBook(final ActionEx action) {
        final File file = action.getParameter("source");
        if (file == null) {
            return;
        }
        final boolean isCopy = action.id == R.id.bookmenu_copy;
        final int titleId = isCopy ? R.string.copy_book_to_dlg_title : R.string.move_book_to_dlg_title;
        final int id = isCopy ? R.id.actions_doCopyBook : R.id.actions_doMoveBook;

        getController().getOrCreateAction(id).putValue("source", file);

        final FolderDlg dlg = new FolderDlg(getController());
        dlg.show(new File(file.getAbsolutePath()), titleId, id);
    }

    @ActionMethod(ids = R.id.actions_doCopyBook)
    public void doCopyBook(final ActionEx action) {
        final File targetFolder = action.getParameter(FolderDlg.SELECTED_FOLDER);
        final File book = action.getParameter("source");
        final BookNode node = new BookNode(book, SettingsManager.getBookSettings(book.getAbsolutePath()));

        new CopyBookTask(this.getContext(), null, targetFolder).execute(node);
    }

    @ActionMethod(ids = R.id.actions_doMoveBook)
    public void doMoveBook(final ActionEx action) {
        final File targetFolder = action.getParameter(FolderDlg.SELECTED_FOLDER);
        final File book = action.getParameter("source");
        final BookNode node = new BookNode(book, SettingsManager.getBookSettings(book.getAbsolutePath()));

        new MoveBookTask(this.getContext(), null, targetFolder) {

            @Override
            protected void processTargetFile(final File target) {
                super.processTargetFile(target);
                adapter.remove(origin);
            }
        }.execute(node);
    }

    @ActionMethod(ids = R.id.bookmenu_rename)
    public void renameBook(final ActionEx action) {
        final File file = action.getParameter("source");
        if (file == null) {
            return;
        }

        final FileUtils.FilePath path = FileUtils.parseFilePath(file.getAbsolutePath(), CodecType.getAllExtensions());
        final EditText input = new EditText(this);
        input.setSingleLine();
        input.setText(path.name);
        input.selectAll();

        final ActionDialogBuilder builder = new ActionDialogBuilder(this, this.getController());
        builder.setTitle(R.string.book_rename_title);
        builder.setMessage(R.string.book_rename_msg);
        builder.setView(input);
        builder.setPositiveButton(R.id.actions_doRenameBook, new Constant("source", file), new Constant("file", path),
                new EditableValue("input", input));
        builder.setNegativeButton().show();
    }

    @ActionMethod(ids = R.id.actions_doRenameBook)
    public void doRenameBook(final ActionEx action) {
        final File book = action.getParameter("source");
        final BookNode node = new BookNode(book, SettingsManager.getBookSettings(book.getAbsolutePath()));
        final FileUtils.FilePath path = action.getParameter("file");
        final Editable value = action.getParameter("input");
        final String newName = value.toString();
        if (!CompareUtils.equals(path.name, newName)) {
            path.name = newName;
            new RenameBookTask(this.getContext(), null, path) {

                @Override
                protected void processTargetFile(final File target) {
                    super.processTargetFile(target);
                    adapter.remove(origin);
                }
            }.execute(node);
        }
    }

    @ActionMethod(ids = R.id.bookmenu_delete)
    public void deleteBook(final ActionEx action) {
        final File file = action.getParameter("source");
        if (file == null) {
            return;
        }

        final ActionDialogBuilder builder = new ActionDialogBuilder(getContext(), getController());
        builder.setTitle(R.string.book_delete_title);
        builder.setMessage(R.string.book_delete_msg);
        builder.setPositiveButton(R.id.actions_doDeleteBook, new Constant("source", file));
        builder.setNegativeButton().show();
    }

    @ActionMethod(ids = R.id.actions_doDeleteBook)
    public void doDeleteBook(final ActionEx action) {
        final File file = action.getParameter("source");
        if (file == null) {
            return;
        }

        if (file.delete()) {
            CacheManager.clear(file.getAbsolutePath());
            adapter.remove(file);
        }
    }
}
