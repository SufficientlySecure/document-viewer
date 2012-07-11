package org.ebookdroid.ui.library.views;

import org.ebookdroid.R;
import org.ebookdroid.ui.library.adapters.BooksAdapter;

import android.content.Context;
import android.database.DataSetObserver;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class BookcaseView extends RelativeLayout {

    private TextView shelfCaption;
    private ViewPager shelves;
    private BooksAdapter adapter;

    public BookcaseView(final Context context) {
        super(context);
    }

    public BookcaseView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public BookcaseView(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
    }

    public void init(final BooksAdapter adapter) {
        this.adapter = adapter;
        this.shelfCaption = (TextView) findViewById(R.id.ShelfCaption);
        this.shelves = (ViewPager) findViewById(R.id.Shelves);

        shelves.setAdapter(adapter);

        adapter.registerDataSetObserver(new DataSetObserver() {

            @Override
            public void onChanged() {
                super.onChanged();
                shelfCaption.setText(BookcaseView.this.adapter.getListName(shelves.getCurrentItem()));
            }
        });

        shelves.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {

            @Override
            public void onPageSelected(final int arg0) {
                shelfCaption.setText(BookcaseView.this.adapter.getListName(shelves.getCurrentItem()));
            }
        });

        shelfCaption.setText(BookcaseView.this.adapter.getListName(0));
    }

    public int getCurrentList() {
        return shelves.getCurrentItem();
    }

    public void setCurrentList(final int item) {
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
