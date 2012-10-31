package org.ebookdroid.ui.library.views;

import org.ebookdroid.R;
import org.ebookdroid.ui.library.adapters.BooksAdapter;

import android.content.Context;
import android.database.DataSetObserver;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class BookcaseView extends RelativeLayout {

    private TextView shelfCaption;
    private ViewPager shelves;
    private View prevShelf;
    private View nextShelf;
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
        this.prevShelf = findViewById(R.id.ShelfLeftButton);
        this.nextShelf = findViewById(R.id.ShelfRightButton);

        shelves.setAdapter(adapter);

        adapter.registerDataSetObserver(new DataSetObserver() {

            @Override
            public void onChanged() {
                super.onChanged();
                final int count = BookcaseView.this.adapter.getCount();
                int currentItem = shelves.getCurrentItem();
                if (currentItem >= count) {
                    currentItem = count - 1;
                    shelves.setCurrentItem(currentItem);
                    return;
                }
                final String listName = BookcaseView.this.adapter.getListName(currentItem);
                shelfCaption.setText(listName);
            }
        });

        shelves.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {

            @Override
            public void onPageSelected(final int shelf) {
                updateShelfCaption(shelf);
            }
        });

        updateShelfCaption(0);
    }

    public int getCurrentList() {
        return shelves.getCurrentItem();
    }

    public void setCurrentList(final int shelf) {
        shelves.setCurrentItem(shelf);
    }

    public void updateShelfCaption(final int shelf) {
        shelfCaption.setText(BookcaseView.this.adapter.getListName(shelves.getCurrentItem()));
        prevShelf.setVisibility(shelf == 0 ? View.GONE : View.VISIBLE);
        nextShelf.setVisibility(shelf >= adapter.getCount() - 1 ? View.GONE : View.VISIBLE);
    }

    public void prevList() {
        setCurrentList(Math.max(0, shelves.getCurrentItem() - 1));
    }

    public void nextList() {
        setCurrentList(Math.min(shelves.getCurrentItem() + 1, adapter.getListCount() - 1));
    }

}
