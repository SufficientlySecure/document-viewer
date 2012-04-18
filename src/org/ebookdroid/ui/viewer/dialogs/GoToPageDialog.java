package org.ebookdroid.ui.viewer.dialogs;

import org.ebookdroid.R;
import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.common.settings.books.BookSettings;
import org.ebookdroid.common.settings.books.Bookmark;
import org.ebookdroid.core.Page;
import org.ebookdroid.core.PageIndex;
import org.ebookdroid.core.models.DocumentModel;
import org.ebookdroid.ui.viewer.IActivityController;

import android.app.Dialog;
import android.content.Context;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import org.emdev.ui.actions.ActionDialogBuilder;
import org.emdev.ui.actions.ActionEx;
import org.emdev.ui.actions.ActionMethod;
import org.emdev.ui.actions.ActionMethodDef;
import org.emdev.ui.actions.ActionTarget;
import org.emdev.ui.actions.DialogController;
import org.emdev.ui.actions.IActionController;
import org.emdev.ui.actions.params.Constant;
import org.emdev.ui.actions.params.EditableValue;
import org.emdev.utils.LayoutUtils;

@ActionTarget(
// action
actions = {
        // start
        @ActionMethodDef(id = R.id.actions_addBookmark, method = "addBookmark"),
        @ActionMethodDef(id = R.id.actions_deleteAllBookmarks, method = "deleteAllBookmarks"),
        @ActionMethodDef(id = R.id.actions_gotoPage, method = "goToPageAndDismiss"),
        @ActionMethodDef(id = R.id.actions_removeBookmark, method = "removeBookmark"),
        @ActionMethodDef(id = R.id.mainmenu_bookmark, method = "showAddBookmarkDlg"),
        @ActionMethodDef(id = R.id.actions_showDeleteAllBookmarksDlg, method = "showDeleteAllBookmarksDlg"),
        @ActionMethodDef(id = R.id.actions_showDeleteBookmarkDlg, method = "showDeleteBookmarkDlg"),
        @ActionMethodDef(id = R.id.actions_setBookmarkedPage, method = "updateControls")
// finish
})
public class GoToPageDialog extends Dialog {

    final IActivityController base;
    BookmarkAdapter adapter;
    Bookmark current;
    DialogController<GoToPageDialog> actions;

    public GoToPageDialog(final IActivityController base) {
        super(base.getContext());
        this.base = base;
        this.actions = new DialogController<GoToPageDialog>(this);

        setTitle(R.string.dialog_title_goto_page);
        setContentView(R.layout.gotopage);

        final Button button = (Button) findViewById(R.id.goToButton);
        final SeekBar seekbar = (SeekBar) findViewById(R.id.seekbar);
        final EditText editText = (EditText) findViewById(R.id.pageNumberTextEdit);

        actions.connectViewToActions(R.id.bookmarkHeader, R.id.mainmenu_bookmark, R.id.actions_showDeleteAllBookmarksDlg);
        actions.connectViewToAction(button, R.id.actions_gotoPage);
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
                if (fromUser) {
                    updateControls(progress, false);
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        LayoutUtils.maximizeWindow(getWindow());

        final DocumentModel dm = base.getDocumentModel();
        final Page lastPage = dm != null ? dm.getLastPageObject() : null;
        final int current = dm != null ? dm.getCurrentViewPageIndex() : 0;
        final int max = lastPage != null ? lastPage.index.viewIndex : 0;

        adapter = new BookmarkAdapter(lastPage, SettingsManager.getBookSettings());

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
    }

    @ActionMethod(ids = R.id.actions_gotoPage)
    public void goToPageAndDismiss(final ActionEx action) {
        navigateToPage();
        dismiss();
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
        final Bookmark bookmark = (Bookmark) view.getTag();
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

    @ActionMethod(ids = R.id.mainmenu_bookmark)
    public void showAddBookmarkDlg(final ActionEx action) {
        final Context context = getContext();

        final SeekBar seekbar = (SeekBar) findViewById(R.id.seekbar);
        final int viewIndex = seekbar.getProgress();

        final EditText input = new EditText(context);
        input.setText(context.getString(R.string.text_page) + " " + (viewIndex + 1));
        input.selectAll();

        final ActionDialogBuilder builder = new ActionDialogBuilder(getContext(), actions);
        builder.setTitle(R.string.menu_add_bookmark);
        builder.setMessage(R.string.add_bookmark_name);
        builder.setView(input);
        builder.setPositiveButton(R.id.actions_addBookmark, new EditableValue("input", input), new Constant(
                "viewIndex", viewIndex));
        builder.setNegativeButton().show();
    }

    @ActionMethod(ids = R.id.actions_addBookmark)
    public void addBookmark(final ActionEx action) {
        final Integer viewIndex = action.getParameter("viewIndex");
        final Editable value = action.getParameter("input");
        final Page page = base.getDocumentModel().getPageObject(viewIndex);
        adapter.add(new Bookmark(value.toString(), page.index, 0, 0));
        adapter.notifyDataSetChanged();
    }

    @ActionMethod(ids = R.id.actions_showDeleteAllBookmarksDlg)
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

        editText.setText("" + (viewIndex + 1));
        editText.selectAll();

        if (updateBar) {
            seekbar.setProgress(viewIndex);
        }

        current = null;
    }

    private void navigateToPage() {
        if (current != null) {
            final Page actualPage = current.page.getActualPage(base.getDocumentModel(), adapter.bookSettings);
            if (actualPage != null) {
                base.jumpToPage(actualPage.index.viewIndex, current.offsetX, current.offsetY, true);
            }
            return;
        }
        final EditText text = (EditText) findViewById(R.id.pageNumberTextEdit);
        int pageNumber = 1;
        try {
            pageNumber = Integer.parseInt(text.getText().toString());
        } catch (final Exception e) {
            pageNumber = 1;
        }
        final int pageCount = base.getDocumentModel().getPageCount();
        if (pageNumber < 1 || pageNumber > pageCount) {
            Toast.makeText(getContext(), base.getContext().getString(R.string.bookmark_invalid_page) + pageCount, 2000)
                    .show();
            return;
        }
        base.jumpToPage(pageNumber - 1, 0, 0, true);
    }

    private final class BookmarkAdapter extends BaseAdapter {

        final BookSettings bookSettings;

        final Bookmark start;
        final Bookmark end;

        public BookmarkAdapter(final Page lastPage, final BookSettings bookSettings) {
            this.bookSettings = bookSettings;
            this.start = new Bookmark(true, getContext().getString(R.string.bookmark_start), PageIndex.FIRST, 0, 0);
            this.end = new Bookmark(true, getContext().getString(R.string.bookmark_end),
                    lastPage != null ? lastPage.index : PageIndex.FIRST, 0, 0);
        }

        public void add(final Bookmark... bookmarks) {
            for (final Bookmark bookmark : bookmarks) {
                bookSettings.bookmarks.add(bookmark);
            }
            SettingsManager.storeBookSettings();
            notifyDataSetChanged();
        }

        public void remove(final Bookmark b) {
            if (!b.service) {
                bookSettings.bookmarks.remove(b);
                SettingsManager.storeBookSettings();
                notifyDataSetChanged();
            }
        }

        public void clear() {
            bookSettings.bookmarks.clear();
            SettingsManager.storeBookSettings();
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return 2 + bookSettings.bookmarks.size();
        }

        public boolean hasUserBookmarks() {
            return !bookSettings.bookmarks.isEmpty();
        }

        @Override
        public Object getItem(final int index) {
            return getBookmark(index);
        }

        public Bookmark getBookmark(final int index) {
            if (index == 0) {
                return start;
            }
            if (index - 1 < bookSettings.bookmarks.size()) {
                return bookSettings.bookmarks.get(index - 1);
            }
            return end;
        }

        @Override
        public long getItemId(final int index) {
            return index;
        }

        @Override
        public View getView(final int index, View itemView, final ViewGroup parent) {
            if (itemView == null) {
                itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.bookmark, parent, false);
                final ProgressBar bar = (ProgressBar) itemView.findViewById(R.id.bookmarkPage);
                bar.setProgressDrawable(base.getActivity().getResources().getDrawable(R.drawable.progress));
            }

            final Bookmark b = getBookmark(index);
            itemView.setTag(b);
            itemView.setOnClickListener(actions.getOrCreateAction(R.id.actions_setBookmarkedPage));
            itemView.setOnLongClickListener(actions.getOrCreateAction(R.id.actions_showDeleteBookmarkDlg));

            final TextView text = (TextView) itemView.findViewById(R.id.bookmarkName);
            final ProgressBar bar = (ProgressBar) itemView.findViewById(R.id.bookmarkPage);
            text.setText(b.name);

            bar.setMax(base.getDocumentModel().getPageCount() - 1);
            bar.setProgress(b.page.viewIndex);

            return itemView;
        }
    }
}
