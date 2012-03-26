package org.ebookdroid.ui.library;

import org.ebookdroid.R;
import org.ebookdroid.common.cache.CacheManager;
import org.ebookdroid.common.cache.ThumbnailFile;
import org.ebookdroid.common.log.LogContext;
import org.ebookdroid.common.settings.AppSettings;
import org.ebookdroid.common.settings.AppSettings.Diff;
import org.ebookdroid.common.settings.ISettingsChangeListener;
import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.common.settings.books.BookSettings;
import org.ebookdroid.ui.library.adapters.BooksAdapter;
import org.ebookdroid.ui.library.adapters.FileListAdapter;
import org.ebookdroid.ui.library.adapters.RecentAdapter;
import org.ebookdroid.ui.library.views.BookcaseView;
import org.ebookdroid.ui.library.views.LibraryView;
import org.ebookdroid.ui.library.views.RecentBooksView;
import org.ebookdroid.ui.settings.SettingsUI;
import org.ebookdroid.ui.viewer.ViewerActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ViewFlipper;

import java.io.File;
import java.util.List;

import org.emdev.ui.AbstractActionActivity;
import org.emdev.ui.actions.ActionDialogBuilder;
import org.emdev.ui.actions.ActionEx;
import org.emdev.ui.actions.ActionMethod;
import org.emdev.ui.actions.ActionMethodDef;
import org.emdev.ui.actions.ActionTarget;
import org.emdev.ui.actions.IActionController;
import org.emdev.ui.actions.params.EditableValue;
import org.emdev.utils.LengthUtils;
import org.emdev.utils.android.AndroidVersion;
import org.emdev.utils.filesystem.FileExtensionFilter;

@ActionTarget(
// actions
actions = {
        // start
        @ActionMethodDef(id = R.id.recentmenu_cleanrecent, method = "showClearRecentDialog"),
        @ActionMethodDef(id = R.id.recent_showbrowser, method = "goFileBrowser"),
        @ActionMethodDef(id = R.id.recent_showlibrary, method = "goLibrary"),
        @ActionMethodDef(id = R.id.mainmenu_settings, method = "showSettings"),
        @ActionMethodDef(id = R.id.mainmenu_about, method = "showAbout"),
        @ActionMethodDef(id = R.id.actions_clearRecent, method = "doClearRecent"),
        @ActionMethodDef(id = R.id.ShelfCaption, method = "showSelectShelfDlg"),
        @ActionMethodDef(id = R.id.actions_selectShelf, method = "selectShelf"),
        @ActionMethodDef(id = R.id.ShelfLeftButton, method = "selectPrevShelf"),
        @ActionMethodDef(id = R.id.ShelfRightButton, method = "selectNextShelf"),
        @ActionMethodDef(id = R.id.recentmenu_searchBook, method = "showSearchDlg"),
        @ActionMethodDef(id = R.id.actions_searchBook, method = "searchBook")
// finish
})
public class RecentActivity extends AbstractActionActivity implements IBrowserActivity, ISettingsChangeListener {

    public static final LogContext LCTX = LogContext.ROOT.lctx("Core");

    private static final int CLEAR_RECENT_LIST = 0;
    private static final int DELETE_BOOKMARKS = 1;
    private static final int DELETE_BOOK_SETTINGS = 2;
    private static final int ERASE_DISK_CACHE = 3;

    private static final int VIEW_RECENT = 0;
    private static final int VIEW_LIBRARY = 1;

    private RecentAdapter recentAdapter;
    private FileListAdapter libraryAdapter;
    private BooksAdapter bookshelfAdapter;

    private ViewFlipper viewflipper;
    private ImageView libraryButton;

    private BookcaseView bookcaseView;

    private boolean firstResume = true;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.recent);

        recentAdapter = new RecentAdapter(this);
        bookshelfAdapter = new BooksAdapter(this, recentAdapter);
        libraryAdapter = new FileListAdapter(bookshelfAdapter);

        libraryButton = (ImageView) findViewById(R.id.recent_showlibrary);

        viewflipper = (ViewFlipper) findViewById(R.id.recentflip);

        SettingsManager.addListener(this);
        SettingsManager.applyAppSettingsChanges(null, SettingsManager.getAppSettings());

        if (AndroidVersion.VERSION == 3) {
            setActionForView(R.id.recent_showlibrary);
            setActionForView(R.id.recent_showbrowser);
            setActionForView(R.id.ShelfLeftButton);
            setActionForView(R.id.ShelfCaption);
            setActionForView(R.id.ShelfRightButton);
        }

        final boolean shouldLoad = SettingsManager.getAppSettings().loadRecent;
        final BookSettings recent = SettingsManager.getRecentBook();
        final File file = (recent != null && recent.fileName != null) ? new File(recent.fileName) : null;
        final boolean found = file != null ? file.exists()
                && SettingsManager.getAppSettings().allowedFileTypes.accept(file) : false;

        if (LCTX.isDebugEnabled()) {
            LCTX.d("Last book: " + (file != null ? file.getAbsolutePath() : "") + ", found: " + found
                    + ", should load: " + shouldLoad);
        }

        if (shouldLoad && found) {
            changeLibraryView(VIEW_RECENT);
            showDocument(Uri.fromFile(file));
        } else {
            changeLibraryView(recent != null ? VIEW_RECENT : VIEW_LIBRARY);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        final AppSettings appSettings = SettingsManager.getAppSettings();
        if (appSettings.getUseBookcase()) {
            if (firstResume) {
                bookshelfAdapter.startScan();
            }
            recentAdapter.setBooks(SettingsManager.getAllBooksSettings().values(), appSettings.allowedFileTypes);
        } else {
            if (viewflipper.getDisplayedChild() == VIEW_RECENT) {
                if (SettingsManager.getRecentBook() == null) {
                    changeLibraryView(VIEW_LIBRARY);
                } else {
                    recentAdapter.setBooks(SettingsManager.getAllBooksSettings().values(),
                            appSettings.allowedFileTypes);
                }

            }
        }

        firstResume = false;
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {

        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.recentmenu, menu);
        return true;
    }

    @ActionMethod(ids = R.id.recentmenu_cleanrecent)
    public void showClearRecentDialog(final ActionEx action) {
        final ActionDialogBuilder builder = new ActionDialogBuilder(getContext(), getController());

        builder.setTitle(R.string.clear_recent_title);
        builder.setMultiChoiceItems(R.array.list_clear_recent_mode, R.id.actions_clearRecent);
        builder.setPositiveButton(R.id.actions_clearRecent);
        builder.setNegativeButton();
        builder.show();
    }

    @ActionMethod(ids = R.id.actions_clearRecent)
    public void doClearRecent(final ActionEx action) {
        if (action.isDialogItemSelected(ERASE_DISK_CACHE)) {
            CacheManager.clear();
            recentAdapter.notifyDataSetInvalidated();
            libraryAdapter.notifyDataSetInvalidated();
        }

        if (action.isDialogItemSelected(DELETE_BOOK_SETTINGS)) {
            SettingsManager.deleteAllBookSettings();
            recentAdapter.clearBooks();
            libraryAdapter.notifyDataSetInvalidated();
        } else {
            if (action.isDialogItemSelected(CLEAR_RECENT_LIST)) {
                SettingsManager.clearAllRecentBookSettings();
                recentAdapter.clearBooks();
                libraryAdapter.notifyDataSetInvalidated();
            }
            if (action.isDialogItemSelected(DELETE_BOOKMARKS)) {
                SettingsManager.deleteAllBookmarks();
            }
        }

    }

    @ActionMethod(ids = R.id.recentmenu_searchBook)
    public void showSearchDlg(final ActionEx action) {
        final ActionDialogBuilder builder = new ActionDialogBuilder(getContext(), getController());

        final EditText input = new EditText(this);
        input.setSingleLine();
        input.setText(LengthUtils.safeString(bookshelfAdapter.getSearchQuery()));
        input.selectAll();

        builder.setTitle(R.string.search_book_dlg_title);
        builder.setView(input);
        builder.setPositiveButton(android.R.string.search_go, R.id.actions_searchBook,
                new EditableValue("input", input));
        builder.setNegativeButton();
        builder.show();
    }

    @ActionMethod(ids = R.id.actions_searchBook)
    public void searchBook(final ActionEx action) {
        final Editable value = action.getParameter("input");
        final String searchQuery = value.toString();
        if (bookshelfAdapter.startSearch(searchQuery)) {
            if (SettingsManager.getAppSettings().useBookcase) {
                this.bookcaseView.setCurrentList(BooksAdapter.SEARCH_INDEX);
            }
        }
    }

    @ActionMethod(ids = R.id.mainmenu_settings)
    public void showSettings(final ActionEx action) {
        libraryAdapter.stopScan();
        bookshelfAdapter.stopScan();
        SettingsUI.showAppSettings(this);
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
    public void setCurrentDir(final File newDir) {
    }

    @Override
    public void showDocument(final Uri uri) {
        libraryAdapter.stopScan();
        bookshelfAdapter.stopScan();
        final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setClass(this, ViewerActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            System.exit(0);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void changeLibraryView(final int view) {
        if (!SettingsManager.getAppSettings().getUseBookcase()) {
            final FileExtensionFilter filter = SettingsManager.getAppSettings().allowedFileTypes;

            if (view == VIEW_LIBRARY) {
                viewflipper.setDisplayedChild(VIEW_LIBRARY);
                libraryButton.setImageResource(R.drawable.actionbar_recent);
                libraryAdapter.startScan();
            } else {
                viewflipper.setDisplayedChild(VIEW_RECENT);
                libraryButton.setImageResource(R.drawable.actionbar_library);
                recentAdapter.setBooks(SettingsManager.getAllBooksSettings().values(), filter);
            }
        }
    }

    @ActionMethod(ids = R.id.ShelfCaption)
    public void showSelectShelfDlg(final ActionEx action) {
        final List<String> names = bookshelfAdapter.getListNames();

        if (LengthUtils.isNotEmpty(names)) {
            final ActionDialogBuilder builder = new ActionDialogBuilder(getContext(), getController());
            builder.setTitle(R.string.bookcase_shelves);
            builder.setItems(names.toArray(new String[names.size()]),
                    getController().getOrCreateAction(R.id.actions_selectShelf));
            builder.show();
        }
    }

    @ActionMethod(ids = R.id.actions_selectShelf)
    public void selectShelf(final ActionEx action) {
        final Integer item = action.getParameter(IActionController.DIALOG_ITEM_PROPERTY);
        if (bookcaseView != null) {
            bookcaseView.setCurrentList(item);
        }
    }

    @ActionMethod(ids = R.id.ShelfLeftButton)
    public void selectPrevShelf(final ActionEx action) {
        if (bookcaseView != null) {
            bookcaseView.prevList();
        }
    }

    @ActionMethod(ids = R.id.ShelfRightButton)
    public void selectNextShelf(final ActionEx action) {
        if (bookcaseView != null) {
            bookcaseView.nextList();
        }
    }

    @ActionMethod(ids = R.id.recent_showlibrary)
    public void goLibrary(final ActionEx action) {
        if (!SettingsManager.getAppSettings().getUseBookcase()) {
            if (viewflipper.getDisplayedChild() == VIEW_RECENT) {
                changeLibraryView(VIEW_LIBRARY);
            } else if (viewflipper.getDisplayedChild() == VIEW_LIBRARY) {
                changeLibraryView(VIEW_RECENT);
            }
        }
    }

    @ActionMethod(ids = R.id.recent_showbrowser)
    public void goFileBrowser(final ActionEx action) {
        final Intent myIntent = new Intent(RecentActivity.this, BrowserActivity.class);
        startActivity(myIntent);
    }

    @Override
    public void showProgress(final boolean show) {
        final ProgressBar progress = (ProgressBar) findViewById(R.id.recentprogress);
        if (show) {
            progress.setVisibility(View.VISIBLE);
        } else {
            progress.setVisibility(View.GONE);
        }
    }

    @Override
    public void loadThumbnail(final String path, final ImageView imageView, final int defaultResID) {
        final ThumbnailFile tf = CacheManager.getThumbnailFile(path);
        final Bitmap bmp = tf.getImage();
        if (bmp != null) {
            imageView.setImageBitmap(bmp);
        }
    }

    @Override
    public void onAppSettingsChanged(final AppSettings oldSettings, final AppSettings newSettings, final Diff diff) {
        final FileExtensionFilter filter = newSettings.allowedFileTypes;
        if (diff.isUseBookcaseChanged()) {
            viewflipper.removeAllViews();

            if (SettingsManager.getAppSettings().getUseBookcase()) {
                libraryButton.setImageResource(R.drawable.actionbar_shelf);

                bookcaseView = new BookcaseView(this, bookshelfAdapter);
                viewflipper.addView(bookcaseView, 0);

                recentAdapter.setBooks(SettingsManager.getAllBooksSettings().values(), filter);
            } else {
                libraryButton.setImageResource(R.drawable.actionbar_library);

                viewflipper.addView(new RecentBooksView(this, recentAdapter), VIEW_RECENT);
                viewflipper.addView(new LibraryView(this, libraryAdapter), VIEW_LIBRARY);
            }
            return;
        }

        if (diff.isAutoScanDirsChanged()) {
            if (newSettings.getUseBookcase()) {
                bookshelfAdapter.startScan();
            } else {
                libraryAdapter.startScan();
            }
            return;
        }
        if (diff.isAllowedFileTypesChanged()) {
            recentAdapter.setBooks(SettingsManager.getAllBooksSettings().values(), filter);
            if (newSettings.getUseBookcase()) {
                bookshelfAdapter.startScan();
            } else {
                libraryAdapter.startScan();
            }
        }
    }

    @Override
    public void onBookSettingsChanged(final BookSettings oldSettings, final BookSettings newSettings,
            final org.ebookdroid.common.settings.books.BookSettings.Diff diff, final Diff appDiff) {

    }
}
