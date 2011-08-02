package org.ebookdroid.core;

import org.ebookdroid.R;

import android.app.Dialog;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class GoToPageDialog extends Dialog {

    private final IViewerActivity base;

    public GoToPageDialog(final IViewerActivity base) {
        super(base.getContext());
        this.base = base;
        setTitle(R.string.dialog_title_goto_page);
        setContentView(R.layout.gotopage);
        final Button button = (Button) findViewById(R.id.goToButton);
        button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View view) {
                goToPageAndDismiss();
            }
        });
        final EditText editText = (EditText) findViewById(R.id.pageNumberTextEdit);
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
    }

    private void goToPageAndDismiss() {
        navigateToPage();
        dismiss();
    }

    private void navigateToPage() {
        final EditText text = (EditText) findViewById(R.id.pageNumberTextEdit);
        int pageNumber = 0;
        try {
            pageNumber = Integer.parseInt(text.getText().toString());
        } catch (final Exception e) {
            pageNumber = 0;
        }
        final int pageCount = base.getDocumentModel().getPageCount();
        if (pageNumber < 1 || pageNumber > pageCount) {
            Toast.makeText(getContext(), "Page number out of range. Valid range: 1-" + pageCount, 2000).show();
            return;
        }
        base.getDocumentController().goToPage(pageNumber - 1);
    }
}
