package org.ebookdroid.core.views;

import org.ebookdroid.R;
import org.ebookdroid.core.IBrowserActivity;
import org.ebookdroid.core.presentation.BooksAdapter;

import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

public class BookcaseView extends LinearLayout {

    private final TextView shelfCaption;
    private final BookshelfView shelf;
    private final BooksAdapter adapter;

    public BookcaseView(IBrowserActivity base, BooksAdapter adapter) {
        super(base.getContext());
        this.adapter = adapter;

        setOrientation(VERTICAL);

        LinearLayout ll = (LinearLayout) LayoutInflater.from(base.getContext()).inflate(R.layout.bookshelf_caption,
                null);
        addView(ll);

        shelfCaption = (TextView) ll.findViewById(R.id.ShelfCaption);

        shelf = new BookshelfView(base, adapter);
        addView(shelf);

        adapter.registerDataSetObserver(new DataSetObserver() {

            @Override
            public void onChanged() {
                super.onChanged();
                shelfCaption.setText(BookcaseView.this.adapter.getListName());
            }
        });
    }
}
