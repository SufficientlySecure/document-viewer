package org.ebookdroid.core;

import org.ebookdroid.R;

import android.app.Dialog;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class GoToPageDialog extends Dialog {

    private final IViewerActivity base;
    private ArrayAdapter<Bookmark> adapter;

    public GoToPageDialog(final IViewerActivity base) {
        super(base.getContext());
        this.base = base;
        setTitle(R.string.dialog_title_goto_page);
        setContentView(R.layout.gotopage);

        final Button button = (Button) findViewById(R.id.goToButton);
        final SeekBar seekbar = (SeekBar) findViewById(R.id.seekbar);
        final EditText editText = (EditText) findViewById(R.id.pageNumberTextEdit);

        final Spinner bookmarks = (Spinner) findViewById(R.id.bookmarks);

        adapter = new ArrayAdapter<Bookmark>(base.getContext(), android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

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
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    editText.setText("" + (progress + 1));
                }
            }
        });

        bookmarks.setOnItemSelectedListener(new OnItemSelectedListener() {
            private boolean firstEvent = true;
            @Override
            public void onItemSelected(AdapterView<?> adapter, View view, int i, long lng) {
                bookmarks.setSelection(0);
                if (i == 0) {
                    return;
                }
                if (firstEvent) {
                    firstEvent = false;
                    return;
                }
                Object item = adapter.getItemAtPosition(i);
                updateControls(seekbar, editText, item);
                goToPageAndDismiss();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapter) {
                return;
            }

            private void updateControls(final SeekBar seekbar, final EditText editText, Object item) {
                if (item instanceof Bookmark) {
                    Bookmark bookmark = (Bookmark) item;
                    editText.setText("" + (bookmark.getPage() + 1));
                    seekbar.setProgress(bookmark.getPage());
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
            adapter.add(new Bookmark(-1, "Bookmarks"));
            for(Bookmark b : base.getDocumentModel().getBookmarks()) {
                adapter.add(b);
            }
            final Spinner bookmarks = (Spinner) findViewById(R.id.bookmarks);
            bookmarks.setEnabled(base.getDocumentModel().getBookmarks().size() != 0);
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
            Toast.makeText(getContext(), "Page number out of range. Valid range: 1-" + pageCount, 2000).show();
            return;
        }
        base.getDocumentController().goToPage(pageNumber - 1);
    }
}
