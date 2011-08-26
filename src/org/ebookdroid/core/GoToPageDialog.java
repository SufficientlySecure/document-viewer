package org.ebookdroid.core;

import org.ebookdroid.R;

import android.app.Dialog;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class GoToPageDialog extends Dialog {

    private final IViewerActivity base;
    private BookmarkAdapter adapter;

    public GoToPageDialog(final IViewerActivity base) {
        super(base.getContext());
        this.base = base;
        setTitle(R.string.dialog_title_goto_page);
        setContentView(R.layout.gotopage);

        final Button button = (Button) findViewById(R.id.goToButton);
        final SeekBar seekbar = (SeekBar) findViewById(R.id.seekbar);
        final EditText editText = (EditText) findViewById(R.id.pageNumberTextEdit);

        final ListView bookmarks = (ListView) findViewById(R.id.bookmarks);

        adapter = new BookmarkAdapter();

        bookmarks.setAdapter(adapter);

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

    private void goToPageAndDismiss() {
        navigateToPage();
        dismiss();
    }

    @Override
    protected void onStart() {
        super.onStart();
        final SeekBar seekbar = (SeekBar) findViewById(R.id.seekbar);
        final EditText editText = (EditText) findViewById(R.id.pageNumberTextEdit);

        seekbar.setMax(base.getDocumentModel().getPageCount() - 1);
        seekbar.setProgress(base.getDocumentModel().getCurrentViewPageIndex());
        editText.setText("" + (base.getDocumentModel().getCurrentViewPageIndex() + 1));

        if (adapter != null) {
            adapter.clear();
            adapter.add(new Bookmark(0, base.getContext().getString(R.string.bookmark_start)));
            adapter.add(base.getDocumentModel().getBookmarks());
            adapter.add(new Bookmark(base.getDocumentModel().getPageCount() - 1, base.getContext().getString(R.string.bookmark_end)));

            final ListView bookmarks = (ListView) findViewById(R.id.bookmarks);
            bookmarks.setEnabled(true);
        }
    }

    public void updateControls(final Bookmark bookmark) {
        final SeekBar seekbar = (SeekBar) findViewById(R.id.seekbar);
        final EditText editText = (EditText) findViewById(R.id.pageNumberTextEdit);
        editText.setText("" + (bookmark.getPage() + 1));
        seekbar.setProgress(bookmark.getPage());
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
            Toast.makeText(getContext(), base.getContext().getString(R.string.bookmark_invalid_page) + pageCount, 2000).show();
            return;
        }
        base.getDocumentController().goToPage(pageNumber - 1);
    }

    private final class BookmarkAdapter extends BaseAdapter {

        private final List<Bookmark> list = new ArrayList<Bookmark>();

        public void add(final Bookmark... bookmarks) {
            for (final Bookmark bookmark : bookmarks) {
                list.add(bookmark);
            }
            notifyDataSetChanged();
        }

        public void add(final Collection<Bookmark> bookmarks) {
            list.addAll(bookmarks);
            notifyDataSetChanged();
        }

        public void clear() {
            list.clear();
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(final int index) {
            return list.get(index);
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

            itemView.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(final View v) {
                    final Bookmark bookmark = list.get(index);
                    updateControls(bookmark);
                }
            });

            final Bookmark b = list.get(index);
            final TextView text = (TextView) itemView.findViewById(R.id.bookmarkName);
            text.setText(b.toString());

            final ProgressBar bar = (ProgressBar) itemView.findViewById(R.id.bookmarkPage);
            bar.setMax(base.getDocumentModel().getPageCount() - 1);
            bar.setProgress(b.getPage());

            return itemView;
        }
    }

}
