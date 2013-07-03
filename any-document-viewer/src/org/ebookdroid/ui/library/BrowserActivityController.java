package org.ebookdroid.ui.library;

import org.ebookdroid.CodecType;
import org.ebookdroid.R;
import org.ebookdroid.common.cache.CacheManager;
import org.ebookdroid.common.settings.LibSettings;
import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.common.settings.books.BookSettings;
import org.ebookdroid.common.settings.books.Bookmark;
import org.ebookdroid.ui.library.adapters.BookNode;
import org.ebookdroid.ui.library.adapters.BrowserAdapter;
import org.ebookdroid.ui.library.dialogs.FolderDlg;
import org.ebookdroid.ui.library.tasks.CopyBookTask;
import org.ebookdroid.ui.library.tasks.MoveBookTask;
import org.ebookdroid.ui.library.tasks.RenameBookTask;
import org.ebookdroid.ui.opds.OPDSActivity;
import org.ebookdroid.ui.settings.SettingsUI;
import org.ebookdroid.ui.viewer.ViewerActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.view.KeyEvent;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;

import java.io.File;
import java.io.FileFilter;
import java.util.Set;

import org.emdev.BaseDroidApp;
import org.emdev.common.android.AndroidVersion;
import org.emdev.common.filesystem.CompositeFilter;
import org.emdev.common.filesystem.DirectoryFilter;
import org.emdev.common.filesystem.PathFromUri;
import org.emdev.ui.AbstractActivityController;
import org.emdev.ui.actions.ActionDialogBuilder;
import org.emdev.ui.actions.ActionEx;
import org.emdev.ui.actions.ActionMenuHelper;
import org.emdev.ui.actions.ActionMethod;
import org.emdev.ui.actions.params.Constant;
import org.emdev.ui.actions.params.EditableValue;
import org.emdev.utils.CompareUtils;
import org.emdev.utils.FileUtils;
import org.emdev.utils.LengthUtils;

public class BrowserActivityController extends AbstractActivityController<BrowserActivity> implements IBrowserActivity {

    private static final String CURRENT_DIRECTORY = "currentDirectory";

    FileFilter filter;
    BrowserAdapter adapter;

    public BrowserActivityController(final BrowserActivity activity) {
        super(activity, BEFORE_CREATE, ON_POST_CREATE);
        this.filter = new CompositeFilter(false, DirectoryFilter.NOT_HIDDEN, LibSettings.current().allowedFileTypes);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.emdev.ui.AbstractActivityController#beforeCreate(android.app.Activity)
     */
    @Override
    public void beforeCreate(final BrowserActivity activity) {
        adapter = new BrowserAdapter(filter);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.emdev.ui.AbstractActivityController#onPostCreate(android.os.Bundle, boolean)
     */
    @Override
    public void onPostCreate(final Bundle savedInstanceState, final boolean recreated) {
        if (recreated) {
            return;
        }

        goHome(null);

        final BrowserActivity activity = getManagedComponent();
        final Uri data = activity.getIntent().getData();
        if (data != null) {
            setCurrentDir(new File(PathFromUri.retrieve(activity.getContentResolver(), data)));
        } else if (savedInstanceState != null) {
            final String absolutePath = savedInstanceState.getString(CURRENT_DIRECTORY);
            if (absolutePath != null) {
                setCurrentDir(new File(absolutePath));
            }
        } else {
            final Set<String> dirs = LibSettings.current().autoScanDirs;
            if (LengthUtils.isNotEmpty(dirs)) {
                setCurrentDir(new File(dirs.iterator().next()));
            }
        }

        showProgress(false);
    }

    public boolean onKeyDown(final int keyCode, final KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            final File dir = adapter.getCurrentDirectory();
            final File parent = dir != null ? dir.getParentFile() : null;
            if (parent != null) {
                setCurrentDir(parent);
            } else {
                getManagedComponent().finish();
            }
            return true;
        }
        return false;
    }

    @ActionMethod(ids = R.id.browserhome)
    public void goHome(final ActionEx action) {
        if (BaseDroidApp.EXT_STORAGE.exists()) {
            setCurrentDir(BaseDroidApp.EXT_STORAGE);
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
        SettingsUI.showAppSettings(getManagedComponent(), null);
    }

    @ActionMethod(ids = R.id.browserrecent)
    public void goRecent(final ActionEx action) {
        final BrowserActivity activity = getManagedComponent();
        final Intent myIntent = new Intent(activity, RecentActivity.class);
        activity.startActivity(myIntent);
        activity.finish();
    }

    @ActionMethod(ids = R.id.mainmenu_opds)
    public void goOPDSBrowser(final ActionEx action) {
        final BrowserActivity activity = getManagedComponent();
        final Intent myIntent = new Intent(activity, OPDSActivity.class);
        activity.startActivity(myIntent);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.library.IBrowserActivity#showDocument(android.net.Uri, org.ebookdroid.common.settings.books.Bookmark)
     */
    @Override
    public void showDocument(final Uri uri, final Bookmark b) {
        final BrowserActivity activity = getManagedComponent();
        final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setClass(activity, ViewerActivity.class);
        if (b != null) {
            intent.putExtra("pageIndex", "" + b.page.viewIndex);
            intent.putExtra("offsetX", "" + b.offsetX);
            intent.putExtra("offsetY", "" + b.offsetY);
        }
        activity.startActivity(intent);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.library.IBrowserActivity#setCurrentDir(java.io.File)
     */
    @Override
    public void setCurrentDir(final File newDir) {
        adapter.setCurrentDirectory(newDir);
        getManagedComponent().setTitle(newDir);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.emdev.common.filesystem.FileSystemScanner.ProgressListener#showProgress(boolean)
     */
    @Override
    public void showProgress(final boolean show) {
        final BrowserActivity activity = getManagedComponent();

        if (!AndroidVersion.lessThan3x) {
            activity.runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    try {
                        activity.setProgressBarIndeterminateVisibility(show);
                        activity.getWindow().setFeatureInt(Window.FEATURE_INDETERMINATE_PROGRESS, !show ? 10000 : 1);
                    } catch (final Throwable e) {
                    }
                }
            });
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.library.IBrowserActivity#loadThumbnail(java.lang.String, android.widget.ImageView, int)
     */
    @Override
    public void loadThumbnail(final String path, final ImageView imageView, final int defaultResID) {
        imageView.setImageResource(defaultResID);
    }

    @ActionMethod(ids = R.id.actions_goToBookmark)
    public void openBook(final ActionEx action) {
        final File file = action.getParameter(ActionMenuHelper.MENU_ITEM_SOURCE);
        if (!file.isDirectory()) {
            final Bookmark b = action.getParameter("bookmark");
            showDocument(Uri.fromFile(file), b);
        }
    }

    @ActionMethod(ids = R.id.bookmenu_removefromrecent)
    public void removeBookFromRecents(final ActionEx action) {
        final File file = action.getParameter(ActionMenuHelper.MENU_ITEM_SOURCE);
        if (file != null) {
            SettingsManager.removeBookFromRecents(file.getAbsolutePath());
            adapter.notifyDataSetInvalidated();
        }
    }

    @ActionMethod(ids = R.id.bookmenu_cleardata)
    public void removeCachedBookFiles(final ActionEx action) {
        final File file = action.getParameter(ActionMenuHelper.MENU_ITEM_SOURCE);
        if (file != null) {
            CacheManager.clear(file.getAbsolutePath());
            adapter.notifyDataSetInvalidated();
        }
    }

    @ActionMethod(ids = R.id.bookmenu_deletesettings)
    public void removeBookSettings(final ActionEx action) {
        final File file = action.getParameter(ActionMenuHelper.MENU_ITEM_SOURCE);
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

        getOrCreateAction(id).putValue("source", file);

        final FolderDlg dlg = new FolderDlg(this);
        dlg.show(new File(file.getAbsolutePath()), titleId, id);
    }

    @ActionMethod(ids = R.id.actions_doCopyBook)
    public void doCopyBook(final ActionEx action) {
        final File targetFolder = action.getParameter(FolderDlg.SELECTED_FOLDER);
        final File book = action.getParameter("source");
        final BookNode node = new BookNode(book, SettingsManager.getBookSettings(book.getAbsolutePath()));

        new CopyBookTask(this.getManagedComponent(), null, targetFolder).execute(node);
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
        final EditText input = new EditText(getManagedComponent());
        input.setSingleLine();
        input.setText(path.name);
        input.selectAll();

        final ActionDialogBuilder builder = new ActionDialogBuilder(this.getManagedComponent(), this);
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

        final ActionDialogBuilder builder = new ActionDialogBuilder(getContext(), this);
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
