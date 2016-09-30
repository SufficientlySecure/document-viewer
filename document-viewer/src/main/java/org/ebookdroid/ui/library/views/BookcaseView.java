package org.ebookdroid.ui.library.views;

import org.ebookdroid.ui.library.RecentActivity;
import org.ebookdroid.ui.library.RecentActivityController;
import org.emdev.ui.AbstractActionActivity;
import org.emdev.ui.AbstractActivityController;
import org.sufficientlysecure.viewer.R;
import org.ebookdroid.ui.library.adapters.BooksAdapter;
import org.ebookdroid.ui.library.adapters.RecentAdapter;

import android.content.Context;
import android.database.DataSetObserver;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class BookcaseView extends RelativeLayout {
    private ViewPager shelves;
    private BooksAdapter adapter;
    private RecentAdapter recents;

    public BookcaseView(final Context context) {
        super(context);
    }

    public BookcaseView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public BookcaseView(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
    }

    private void showSelectShelfDlg() {
        Context context = getContext();
        if (context instanceof RecentActivity) {
            RecentActivityController controller = ((RecentActivity) context).getController();
            controller.showSelectShelfDlg();
        }
    }

    public void init(final BooksAdapter adapter, final RecentAdapter recents) {
        this.adapter = adapter;
        this.recents = recents;
        this.shelves = (ViewPager) findViewById(R.id.Shelves);

        shelves.setAdapter(adapter);

        adapter.registerDataSetObserver(new DataSetObserver() {

            @Override
            public void onChanged() {
                onBookAdapterChanged();
            }
        });

        recents.registerDataSetObserver(new DataSetObserver() {

            @Override
            public void onChanged() {
                onRecentAdapterChanged();
            }
        });

        onBookAdapterChanged();

        TabLayout.OnTabSelectedListener l = new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                showSelectShelfDlg();
            }
        };

        final TabLayout tl = (TabLayout) findViewById(R.id.tabs);
        tl.setupWithViewPager(this.shelves);
        tl.addOnTabSelectedListener(l);
    }

    protected void onBookAdapterChanged() {
        final int selfCount = adapter.getCount();
        int currentItem = shelves.getCurrentItem();
        if (currentItem >= selfCount) {
            currentItem = selfCount - 1;
            setCurrentList(currentItem);
            return;
        }
        if (currentItem == BooksAdapter.RECENT_INDEX) {
            final int recentCount = adapter.getListCount(BooksAdapter.RECENT_INDEX);
            if (recentCount == 0 && selfCount > BooksAdapter.SERVICE_SHELVES) {
                setCurrentList(BooksAdapter.SERVICE_SHELVES);
                return;
            }
        }
    }

    protected void onRecentAdapterChanged() {
        final int count = BookcaseView.this.adapter.getCount();
        final int recentCount = recents.getCount();
        if (recentCount == 0 && count > BooksAdapter.SERVICE_SHELVES) {
            setCurrentList(BooksAdapter.SERVICE_SHELVES);
        }
    }

    public int getCurrentList() {
        return shelves.getCurrentItem();
    }

    public void setCurrentList(final int shelf) {
        shelves.setCurrentItem(shelf);
    }

    public void prevList() {
        setCurrentList(Math.max(0, shelves.getCurrentItem() - 1));
    }

    public void nextList() {
        setCurrentList(Math.min(shelves.getCurrentItem() + 1, adapter.getListCount() - 1));
    }

}
