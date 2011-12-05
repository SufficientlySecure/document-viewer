package org.ebookdroid.core.views;

import org.ebookdroid.R;
import org.ebookdroid.core.IBrowserActivity;
import org.ebookdroid.core.presentation.BooksAdapter;

import android.database.DataSetObserver;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public class BookcaseView extends LinearLayout {

    private final TextView shelfCaption;
    private final BooksAdapter adapter;

    private ViewPager shelves;

    public BookcaseView(IBrowserActivity base, BooksAdapter adapter) {
        super(base.getContext());

        this.adapter = adapter;

        setOrientation(VERTICAL);

        LinearLayout ll = (LinearLayout) LayoutInflater.from(base.getContext()).inflate(R.layout.bookshelf_caption,
                null);
        addView(ll);

        shelfCaption = (TextView) ll.findViewById(R.id.ShelfCaption);
        shelves = new ViewPager(getContext()) {
        };

        shelves.setAdapter(adapter);
        shelves.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
                ViewGroup.LayoutParams.FILL_PARENT));
        addView(shelves);

        adapter.registerDataSetObserver(new DataSetObserver() {

            @Override
            public void onChanged() {
                super.onChanged();
                shelfCaption.setText(BookcaseView.this.adapter.getListName(shelves.getCurrentItem()));
            }
        });

        shelves.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {

            public void onPageSelected(int arg0) {
                shelfCaption.setText(BookcaseView.this.adapter.getListName(shelves.getCurrentItem()));
            }
        });
    }

    public void setCurrentList(Integer item) {
        shelves.setCurrentItem(item);
        shelfCaption.setText(BookcaseView.this.adapter.getListName(shelves.getCurrentItem()));
    }

    public void prevList() {
        int shelf = shelves.getCurrentItem() - 1;
        if (shelf < 0) {
            shelf = 0;
        }
        shelves.setCurrentItem(shelf);
        shelfCaption.setText(BookcaseView.this.adapter.getListName(shelves.getCurrentItem()));
    }

    public void nextList() {
        int shelf = shelves.getCurrentItem() + 1;
        if (shelf > adapter.getListCount() - 1) {
            shelf = adapter.getListCount() - 1;
        }
        shelves.setCurrentItem(shelf);
        shelfCaption.setText(BookcaseView.this.adapter.getListName(shelves.getCurrentItem()));
    }
}
