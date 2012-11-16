package org.ebookdroid.ui.viewer.dialogs;

import org.ebookdroid.R;
import org.ebookdroid.common.settings.AppSettings;
import org.ebookdroid.common.settings.books.BookSettings;
import org.ebookdroid.common.settings.books.Bookmark;
import org.ebookdroid.core.Page;
import org.ebookdroid.core.models.DocumentModel;
import org.ebookdroid.ui.viewer.IActivityController;
import org.ebookdroid.ui.viewer.adapters.BookmarkAdapter;

import android.app.Dialog;
import android.content.Context;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;

import org.emdev.ui.actions.ActionDialogBuilder;
import org.emdev.ui.actions.ActionEx;
import org.emdev.ui.actions.ActionMethod;
import org.emdev.ui.actions.DialogController;
import org.emdev.ui.actions.IActionController;
import org.emdev.ui.actions.params.Constant;
import org.emdev.ui.actions.params.EditableValue;
import org.emdev.ui.uimanager.IUIManager;
import org.emdev.ui.widget.IViewContainer;
import org.emdev.ui.widget.SeekBarIncrementHandler;
import org.emdev.utils.LayoutUtils;

public class GoToPageDialog extends Dialog {

    final IActivityController base;
    final SeekBarIncrementHandler handler;
    BookmarkAdapter adapter;
    Bookmark current;
    DialogController<GoToPageDialog> actions;
    int offset;

    public GoToPageDialog(final IActivityController base) {
        super(base.getContext());
        this.base = base;
        this.actions = new DialogController<GoToPageDialog>(this);
        this.handler = new SeekBarIncrementHandler();

        final BookSettings bs = base.getBookSettings();
        this.offset = bs != null ? bs.firstPageOffset : 1;

        setTitle(R.string.dialog_title_goto_page);
        setContentView(R.layout.gotopage);

        final SeekBar seekbar = (SeekBar) findViewById(R.id.seekbar);
        final EditText editText = (EditText) findViewById(R.id.pageNumberTextEdit);

        actions.connectViewToAction(R.id.bookmark_add);
        actions.connectViewToAction(R.id.bookmark_remove_all);
        actions.connectViewToAction(R.id.bookmark_remove);
        actions.connectViewToAction(R.id.goToButton);
        actions.connectEditorToAction(editText, R.id.actions_gotoPage);

        seekbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(final SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(final SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
                updateControls(progress, false);
            }
        });

        handler.init(new IViewContainer.DialogBridge(this), seekbar, R.id.seekbar_minus, R.id.seekbar_plus);
    }

    @Override
    protected void onStart() {
        super.onStart();

        LayoutUtils.maximizeWindow(getWindow());

        final DocumentModel dm = base.getDocumentModel();
        final Page lastPage = dm != null ? dm.getLastPageObject() : null;
        final int current = dm != null ? dm.getCurrentViewPageIndex() : 0;
        final int max = lastPage != null ? lastPage.index.viewIndex : 0;

        adapter = new BookmarkAdapter(this.getContext(), actions, lastPage, base.getBookSettings());

        final ListView bookmarks = (ListView) findViewById(R.id.bookmarks);
        bookmarks.setAdapter(adapter);

        final SeekBar seekbar = (SeekBar) findViewById(R.id.seekbar);
        seekbar.setMax(max);

        updateControls(current, true);
    }

    @Override
    protected void onStop() {
        final ListView bookmarks = (ListView) findViewById(R.id.bookmarks);
        bookmarks.setAdapter(null);
        adapter = null;
        IUIManager.instance.invalidateOptionsMenu(base.getManagedComponent());
    }

    @ActionMethod(ids = R.id.goToButton)
    public void goToPageAndDismiss(final ActionEx action) {
        if (navigateToPage()) {
            dismiss();
        }
    }

    @ActionMethod(ids = R.id.actions_setBookmarkedPage)
    public void updateControls(final ActionEx action) {
        final View view = action.getParameter(IActionController.VIEW_PROPERTY);
        final Bookmark bookmark = (Bookmark) view.getTag();
        final Page actualPage = bookmark.page.getActualPage(base.getDocumentModel(), adapter.bookSettings);
        if (actualPage != null) {
            updateControls(actualPage.index.viewIndex, true);
        }
        current = bookmark;
    }

    @ActionMethod(ids = R.id.actions_showDeleteBookmarkDlg)
    public void showDeleteBookmarkDlg(final ActionEx action) {
        final View view = action.getParameter(IActionController.VIEW_PROPERTY);
        final Bookmark bookmark = view != null ? (Bookmark) view.getTag() : null;
        if (bookmark.service) {
            return;
        }

        final ActionDialogBuilder builder = new ActionDialogBuilder(getContext(), actions);
        builder.setTitle(R.string.del_bookmark_title);
        builder.setMessage(R.string.del_bookmark_text);
        builder.setPositiveButton(R.id.actions_removeBookmark, new Constant("bookmark", bookmark));
        builder.setNegativeButton().show();
    }

    @ActionMethod(ids = R.id.actions_removeBookmark)
    public void removeBookmark(final ActionEx action) {
        final Bookmark bookmark = action.getParameter("bookmark");
        adapter.remove(bookmark);
    }

    @ActionMethod(ids = R.id.bookmark_add)
    public void showAddBookmarkDlg(final ActionEx action) {
        final Context context = getContext();
        final View view = action.getParameter(IActionController.VIEW_PROPERTY);
        final Bookmark bookmark = (Bookmark) view.getTag();

        final EditText input = (EditText) LayoutInflater.from(getContext()).inflate(R.layout.bookmark_edit, null);
        final ActionDialogBuilder builder = new ActionDialogBuilder(getContext(), actions);
        builder.setMessage(R.string.add_bookmark_name);
        builder.setView(input);

        if (bookmark == null) {
            builder.setTitle(R.string.menu_add_bookmark);

            final SeekBar seekbar = (SeekBar) findViewById(R.id.seekbar);
            final int viewIndex = seekbar.getProgress();

            input.setText(context.getString(R.string.text_page) + " " + (viewIndex + offset));
            input.selectAll();

            builder.setPositiveButton(R.id.actions_addBookmark, new EditableValue("input", input), new Constant(
                    "viewIndex", viewIndex));
        } else {
            builder.setTitle(R.string.menu_edit_bookmark);

            input.setText(bookmark.name);
            input.selectAll();

            builder.setPositiveButton(R.id.actions_addBookmark, new EditableValue("input", input), new Constant(
                    "bookmark", bookmark));
        }

        builder.setNegativeButton().show();
    }

    @ActionMethod(ids = R.id.actions_addBookmark)
    public void addBookmark(final ActionEx action) {
        final Editable value = action.getParameter("input");
        final Bookmark bookmark = action.getParameter("bookmark");
        if (bookmark != null) {
            bookmark.name = value.toString();
            adapter.update(bookmark);
        } else {
            final Integer viewIndex = action.getParameter("viewIndex");
            final Page page = base.getDocumentModel().getPageObject(viewIndex);
            adapter.add(new Bookmark(value.toString(), page.index, 0, 0));
            adapter.notifyDataSetChanged();
        }
    }

    @ActionMethod(ids = { R.id.bookmark_remove_all, R.id.actions_showDeleteAllBookmarksDlg })
    public void showDeleteAllBookmarksDlg(final ActionEx action) {
        if (!adapter.hasUserBookmarks()) {
            return;
        }

        final ActionDialogBuilder builder = new ActionDialogBuilder(getContext(), actions);
        builder.setTitle(R.string.clear_bookmarks_title);
        builder.setMessage(R.string.clear_bookmarks_text);
        builder.setPositiveButton(R.id.actions_deleteAllBookmarks);
        builder.setNegativeButton().show();
    }

    @ActionMethod(ids = R.id.actions_deleteAllBookmarks)
    public void deleteAllBookmarks(final ActionEx action) {
        adapter.clear();
    }

    private void updateControls(final int viewIndex, final boolean updateBar) {
        final SeekBar seekbar = (SeekBar) findViewById(R.id.seekbar);
        final EditText editText = (EditText) findViewById(R.id.pageNumberTextEdit);

        editText.setText("" + (viewIndex + offset));
        editText.selectAll();

        if (updateBar) {
            seekbar.setProgress(viewIndex);
        }

        current = null;
    }

    private boolean navigateToPage() {
        if (current != null) {
            final Page actualPage = current.page.getActualPage(base.getDocumentModel(), adapter.bookSettings);
            if (actualPage != null) {
                base.jumpToPage(actualPage.index.viewIndex, current.offsetX, current.offsetY,
                        AppSettings.current().storeGotoHistory);
                return true;
            }
            return false;
        }
        final EditText text = (EditText) findViewById(R.id.pageNumberTextEdit);
        final int pageNumber = getEnteredPageIndex(text);
        final int pageCount = base.getDocumentModel().getPageCount();
        if (pageNumber < 0 || pageNumber >= pageCount) {
            final String msg = base.getContext().getString(R.string.bookmark_invalid_page, offset, pageCount - 1 + offset);
            Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
            return false;
        }
        base.jumpToPage(pageNumber, 0, 0, AppSettings.current().storeGotoHistory);
        return true;
    }

    private int getEnteredPageIndex(final EditText text) {
        try {
            return Integer.parseInt(text.getText().toString()) - offset;
        } catch (final Exception e) {
        }
        return -1;
    }
}
