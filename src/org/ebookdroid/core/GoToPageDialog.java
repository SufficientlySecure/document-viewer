package org.ebookdroid.core;

import org.ebookdroid.R;
import org.ebookdroid.core.models.DocumentModel;
import org.ebookdroid.core.settings.BookSettings;
import org.ebookdroid.core.settings.Bookmark;
import org.ebookdroid.core.settings.SettingsManager;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Editable;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class GoToPageDialog extends Dialog {

    final IViewerActivity base;
    BookmarkAdapter adapter;

    public GoToPageDialog(final IViewerActivity base) {
        super(base.getContext());
        this.base = base;
        setTitle(R.string.dialog_title_goto_page);
        setContentView(R.layout.gotopage);

        final Button button = (Button) findViewById(R.id.goToButton);
        final SeekBar seekbar = (SeekBar) findViewById(R.id.seekbar);
        final EditText editText = (EditText) findViewById(R.id.pageNumberTextEdit);

        final View bookmarksHeader = findViewById(R.id.bookmarkHeader);
        final BookmarkHeaderListener listener = new BookmarkHeaderListener(base);
        bookmarksHeader.setOnClickListener(listener);
        bookmarksHeader.setOnLongClickListener(listener);

        button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View view) {
                goToPageAndDismiss();
            }
        });

        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(final TextView textView, final int actionId, final KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_NULL || actionId == EditorInfo.IME_ACTION_DONE) {
                    goToPageAndDismiss();
                    return true;
                }

                return false;
            }
        });

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
                    editText.setText("" + (progress + 1));
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        adapter = new BookmarkAdapter(base.getDocumentModel().getLastPageObject(), SettingsManager.getBookSettings());
        final ListView bookmarks = (ListView) findViewById(R.id.bookmarks);
        bookmarks.setAdapter(adapter);

        final SeekBar seekbar = (SeekBar) findViewById(R.id.seekbar);
        final EditText editText = (EditText) findViewById(R.id.pageNumberTextEdit);

        final DocumentModel dm = base.getDocumentModel();

        seekbar.setMax(dm.getPageCount() - 1);
        seekbar.setProgress(dm.getCurrentViewPageIndex());
        editText.setText("" + (dm.getCurrentViewPageIndex() + 1));
    }

    @Override
    protected void onStop() {
        final ListView bookmarks = (ListView) findViewById(R.id.bookmarks);
        bookmarks.setAdapter(null);
        adapter = null;
    }

    private void goToPageAndDismiss() {
        navigateToPage();
        dismiss();
    }

    public void updateControls(final Bookmark bookmark) {
        final SeekBar seekbar = (SeekBar) findViewById(R.id.seekbar);
        final EditText editText = (EditText) findViewById(R.id.pageNumberTextEdit);

        Page actualPage = bookmark.getPage().getActualPage(base.getDocumentModel(), adapter.bookSettings);
        if (actualPage != null) {
            editText.setText("" + (actualPage.index.viewIndex + 1));
            seekbar.setProgress(actualPage.index.viewIndex);
        }
    }

    private void navigateToPage() {
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
        base.getDocumentController().goToPage(pageNumber - 1);
    }

    private final class BookmarkAdapter extends BaseAdapter {

        final BookSettings bookSettings;

        final Bookmark start;
        final Bookmark end;

        public BookmarkAdapter(final Page lastPage, BookSettings bookSettings) {
            this.bookSettings = bookSettings;
            this.start = new Bookmark(PageIndex.FIRST, getContext().getString(R.string.bookmark_start), true);
            this.end = new Bookmark(lastPage.index, getContext().getString(R.string.bookmark_end), true);
        }

        public void add(final Bookmark... bookmarks) {
            List<Bookmark> list = bookSettings.getBookmarks();
            for (final Bookmark bookmark : bookmarks) {
                list.add(bookmark);
            }
            SettingsManager.edit(bookSettings).commit();
            notifyDataSetChanged();
        }

        public void remove(final Bookmark b) {
            if (!b.isService()) {
                bookSettings.getBookmarks().remove(b);
                SettingsManager.edit(bookSettings).commit();
                notifyDataSetChanged();
            }
        }

        public void clear() {
            bookSettings.getBookmarks().clear();
            SettingsManager.edit(bookSettings).commit();
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return 2 + bookSettings.getBookmarks().size();
        }

        public boolean hasUserBookmarks() {
            return !bookSettings.getBookmarks().isEmpty();
        }

        @Override
        public Object getItem(final int index) {
            return getBookmark(index);
        }

        public Bookmark getBookmark(final int index) {
            if (index == 0) {
                return start;
            }
            List<Bookmark> list = bookSettings.getBookmarks();
            if (index - 1 < list.size()) {
                return list.get(index - 1);
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
            }

            final Bookmark b = getBookmark(index);
            final BookmarkListener listener = new BookmarkListener(b);
            itemView.setOnClickListener(listener);
            itemView.setOnLongClickListener(listener);

            final TextView text = (TextView) itemView.findViewById(R.id.bookmarkName);
            text.setText(b.getName());

            final ProgressBar bar = (ProgressBar) itemView.findViewById(R.id.bookmarkPage);
            bar.setMax(base.getDocumentModel().getPageCount() - 1);
            bar.setProgress(b.getPage().viewIndex);

            return itemView;
        }
    }

    private final class BookmarkListener implements View.OnClickListener, View.OnLongClickListener {

        private final Bookmark bookmark;

        private BookmarkListener(final Bookmark bookmark) {
            this.bookmark = bookmark;
        }

        @Override
        public void onClick(final View v) {
            updateControls(bookmark);
        }

        @Override
        public boolean onLongClick(final View v) {
            if (bookmark.isService()) {
                return false;
            }
            final Context context = getContext();

            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(R.string.del_bookmark_title);
            builder.setMessage(R.string.del_bookmark_text);
            builder.setPositiveButton(R.string.password_ok, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(final DialogInterface dialog, final int whichButton) {
                    adapter.remove(bookmark);
                }
            });
            builder.setNegativeButton(R.string.password_cancel, new EmptyDialogButtonListener()).show();

            return false;
        }
    }

    private final class BookmarkHeaderListener implements View.OnClickListener, View.OnLongClickListener {

        private final IViewerActivity m_base;

        private BookmarkHeaderListener(final IViewerActivity base) {
            m_base = base;
        }

        @Override
        public void onClick(final View v) {
            final Context context = getContext();

            final SeekBar seekbar = (SeekBar) findViewById(R.id.seekbar);
            final int viewIndex = seekbar.getProgress();

            final EditText input = new EditText(context);
            input.setText(context.getString(R.string.text_page) + " " + (viewIndex + 1));

            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(R.string.menu_add_bookmark);
            builder.setMessage(R.string.add_bookmark_name);
            builder.setView(input);
            builder.setPositiveButton(R.string.password_ok, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(final DialogInterface dialog, final int whichButton) {
                    final Editable value = input.getText();
                    Page page = m_base.getDocumentModel().getPageObject(viewIndex);
                    adapter.add(new Bookmark(page.index, value.toString()));
                    adapter.notifyDataSetChanged();
                }
            });
            builder.setNegativeButton(R.string.password_cancel, new EmptyDialogButtonListener()).show();
        }

        @Override
        public boolean onLongClick(final View v) {
            if (!adapter.hasUserBookmarks()) {
                return false;
            }

            final Context context = getContext();

            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(R.string.clear_bookmarks_title);
            builder.setMessage(R.string.clear_bookmarks_text);
            builder.setPositiveButton(R.string.password_ok, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(final DialogInterface dialog, final int whichButton) {
                    adapter.clear();
                }
            });
            builder.setNegativeButton(R.string.password_cancel, new EmptyDialogButtonListener()).show();

            return false;
        }

    }

    private final class EmptyDialogButtonListener implements DialogInterface.OnClickListener {

        @Override
        public void onClick(final DialogInterface dialog, final int whichButton) {
        }
    }

}
