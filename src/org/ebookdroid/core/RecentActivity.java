package org.ebookdroid.core;

import org.ebookdroid.R;
import org.ebookdroid.core.actions.ActionDialogBuilder;
import org.ebookdroid.core.actions.ActionEx;
import org.ebookdroid.core.actions.ActionMethod;
import org.ebookdroid.core.actions.ActionMethodDef;
import org.ebookdroid.core.actions.ActionTarget;
import org.ebookdroid.core.actions.IActionController;
import org.ebookdroid.core.cache.CacheManager;
import org.ebookdroid.core.log.LogContext;
import org.ebookdroid.core.presentation.BooksAdapter;
import org.ebookdroid.core.presentation.FileListAdapter;
import org.ebookdroid.core.presentation.RecentAdapter;
import org.ebookdroid.core.settings.AppSettings;
import org.ebookdroid.core.settings.AppSettings.Diff;
import org.ebookdroid.core.settings.ISettingsChangeListener;
import org.ebookdroid.core.settings.SettingsManager;
import org.ebookdroid.core.settings.books.BookSettings;
import org.ebookdroid.core.settings.ui.SettingsActivity;
import org.ebookdroid.core.utils.FileExtensionFilter;
import org.ebookdroid.core.views.BookcaseView;
import org.ebookdroid.core.views.LibraryView;
import org.ebookdroid.core.views.RecentBooksView;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ViewFlipper;

import java.io.File;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;

@ActionTarget(
// actions
actions = {
        // start
        @ActionMethodDef(id = R.id.recentmenu_cleanrecent, method = "showClearRecentDialog"),
        @ActionMethodDef(id = R.id.recent_showbrowser, method = "goFileBrowser"),
        @ActionMethodDef(id = R.id.recent_showlibrary, method = "goLibrary"),
        @ActionMethodDef(id = R.id.recentmenu_settings, method = "showSettings"),
        @ActionMethodDef(id = R.id.actions_clearRecent, method = "doClearRecent"),
        @ActionMethodDef(id = R.id.ShelfCaption, method = "showSelectShelfDlg"),
        @ActionMethodDef(id = R.id.actions_selectShelf, method = "selectShelf"),
        @ActionMethodDef(id = R.id.ShelfLeftButton, method = "selectPrevShelf"),
        @ActionMethodDef(id = R.id.ShelfRightButton, method = "selectNextShelf")
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

    private final Map<String, SoftReference<Bitmap>> thumbnails = new HashMap<String, SoftReference<Bitmap>>();

    private Bitmap cornerThmbBitmap;

    private Bitmap leftThmbBitmap;

    private Bitmap topThmbBitmap;

    private BookcaseView bookcaseView;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.recent);

        cornerThmbBitmap = BitmapFactory.decodeResource(this.getResources(), R.drawable.bt_corner);
        leftThmbBitmap = BitmapFactory.decodeResource(this.getResources(), R.drawable.bt_left);
        topThmbBitmap = BitmapFactory.decodeResource(this.getResources(), R.drawable.bt_top);

        recentAdapter = new RecentAdapter(this);
        libraryAdapter = new FileListAdapter(this);
        bookshelfAdapter = new BooksAdapter(this, recentAdapter);

        libraryButton = (ImageView) findViewById(R.id.recent_showlibrary);

        viewflipper = (ViewFlipper) findViewById(R.id.recentflip);

        SettingsManager.addListener(this);
        SettingsManager.applyAppSettingsChanges(null, SettingsManager.getAppSettings());

        final boolean shouldLoad = SettingsManager.getAppSettings().isLoadRecentBook();
        final BookSettings recent = SettingsManager.getRecentBook();
        final File file = recent != null ? new File(recent.fileName) : null;
        final boolean found = file != null ? file.exists()
                && SettingsManager.getAppSettings().getAllowedFileTypes().accept(file) : false;

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

        thumbnails.clear();
        if (SettingsManager.getAppSettings().getUseBookcase()) {
            bookshelfAdapter.startScan(SettingsManager.getAppSettings().getAllowedFileTypes());
            recentAdapter.setBooks(SettingsManager.getAllBooksSettings().values(), SettingsManager.getAppSettings()
                    .getAllowedFileTypes());
        } else {
            if (viewflipper.getDisplayedChild() == VIEW_RECENT) {
                if (SettingsManager.getRecentBook() == null) {
                    changeLibraryView(VIEW_LIBRARY);
                } else {
                    recentAdapter.setBooks(SettingsManager.getAllBooksSettings().values(), SettingsManager
                            .getAppSettings().getAllowedFileTypes());
                }

            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {

        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.recentmenu, menu);
        return true;
    }

    @ActionMethod(ids = R.id.recentmenu_cleanrecent)
    public void showClearRecentDialog(final ActionEx action) {
        final ActionDialogBuilder builder = new ActionDialogBuilder(actions);

        builder.setTitle(R.string.clear_recent_title);
        builder.setMultiChoiceItems(R.array.list_clear_recent_mode, R.id.actions_clearRecent);
        builder.setPositiveButton(R.id.actions_clearRecent);
        builder.setNegativeButton();
        builder.show();
    }

    @ActionMethod(ids = R.id.actions_clearRecent)
    public void doClearRecent(ActionEx action) {
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

    @ActionMethod(ids = R.id.recentmenu_settings)
    public void showSettings(ActionEx action) {
        libraryAdapter.stopScan();
        bookshelfAdapter.stopScan();
        final Intent i = new Intent(RecentActivity.this, SettingsActivity.class);
        startActivity(i);
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
        final Class<? extends Activity> activity = Activities.getByUri(uri);
        if (activity != null) {
            intent.setClass(this, activity);
            startActivity(intent);
        }
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
            final FileExtensionFilter filter = SettingsManager.getAppSettings().getAllowedFileTypes();

            if (view == VIEW_LIBRARY) {
                viewflipper.setDisplayedChild(VIEW_LIBRARY);
                libraryButton.setImageResource(R.drawable.actionbar_recent);
                libraryAdapter.startScan(filter);
            } else {
                viewflipper.setDisplayedChild(VIEW_RECENT);
                libraryButton.setImageResource(R.drawable.actionbar_library);
                recentAdapter.setBooks(SettingsManager.getAllBooksSettings().values(), filter);
            }
        }
    }

    @ActionMethod(ids = R.id.ShelfCaption)
    public void showSelectShelfDlg(ActionEx action) {
        final String[] names = bookshelfAdapter.getListNames();

        if ((names != null) && (names.length > 0)) {
            final ActionDialogBuilder builder = new ActionDialogBuilder(actions);
            builder.setTitle(R.string.bookcase_shelves);
            builder.setItems(names, actions.getOrCreateAction(R.id.actions_selectShelf));
            builder.show();
        }
    }

    @ActionMethod(ids = R.id.actions_selectShelf)
    public void selectShelf(ActionEx action) {
        Integer item = action.getParameter(IActionController.DIALOG_ITEM_PROPERTY);
        if (bookcaseView != null) {
            bookcaseView.setCurrentList(item);
        }
    }

    @ActionMethod(ids = R.id.ShelfLeftButton)
    public void selectPrevShelf(ActionEx action) {
        if (bookcaseView != null) {
            bookcaseView.prevList();
        }
    }

    @ActionMethod(ids = R.id.ShelfRightButton)
    public void selectNextShelf(ActionEx action) {
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
        final SoftReference<Bitmap> ref = thumbnails.get(path);
        Bitmap bmp = ref != null ? ref.get() : null;
        if (bmp == null) {
            final File thumbnailFile = CacheManager.getThumbnailFile(path);
            Bitmap tmpbmp = null;
            boolean store = true;
            if (thumbnailFile.exists()) {

                tmpbmp = BitmapFactory.decodeFile(thumbnailFile.getPath());
                if (tmpbmp == null) {
                    thumbnailFile.delete();
                    tmpbmp = Bitmap.createBitmap(160, 200, Bitmap.Config.ARGB_8888);
                    tmpbmp.eraseColor(Color.WHITE);
                    store = false;
                }
            } else {
                tmpbmp = Bitmap.createBitmap(160, 200, Bitmap.Config.ARGB_8888);
                tmpbmp.eraseColor(Color.WHITE);
                store = false;
            }
            int left = 15;
            int top = 10;
            final int width = tmpbmp.getWidth() + left;
            final int height = tmpbmp.getHeight() + top;
            bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

            bmp.eraseColor(Color.TRANSPARENT);

            final Canvas c = new Canvas(bmp);

            c.drawBitmap(cornerThmbBitmap, null, new Rect(0, 0, left, top), null);
            c.drawBitmap(topThmbBitmap, null, new Rect(left, 0, width, top), null);
            c.drawBitmap(leftThmbBitmap, null, new Rect(0, top, left, height), null);
            c.drawBitmap(tmpbmp, null, new Rect(left, top, width, height), null);

            if (store) {
                thumbnails.put(path, new SoftReference<Bitmap>(bmp));
            }
        }

        if (bmp != null) {
            imageView.setImageBitmap(bmp);
        }
    }

    @Override
    public void onAppSettingsChanged(final AppSettings oldSettings, final AppSettings newSettings, final Diff diff) {
        if (diff.isUseBookcaseChanged()) {
            viewflipper.removeAllViews();

            if (SettingsManager.getAppSettings().getUseBookcase()) {
                libraryButton.setImageResource(R.drawable.actionbar_shelf);

                bookcaseView = new BookcaseView(this, bookshelfAdapter);
                viewflipper.addView(bookcaseView, 0);

                recentAdapter.setBooks(SettingsManager.getAllBooksSettings().values(), SettingsManager.getAppSettings()
                        .getAllowedFileTypes());
            } else {
                libraryButton.setImageResource(R.drawable.actionbar_library);

                viewflipper.addView(new RecentBooksView(this, recentAdapter), VIEW_RECENT);
                viewflipper.addView(new LibraryView(this, libraryAdapter), VIEW_LIBRARY);
            }
        }
    }

    @Override
    public void onBookSettingsChanged(final BookSettings oldSettings, final BookSettings newSettings,
            final org.ebookdroid.core.settings.books.BookSettings.Diff diff, final Diff appDiff) {

    }
}
