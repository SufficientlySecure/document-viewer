package org.ebookdroid.core.views;

import org.ebookdroid.R;
import org.ebookdroid.core.IBrowserActivity;
import org.ebookdroid.core.presentation.BooksAdapter;

import android.database.DataSetObserver;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class BookcaseView extends LinearLayout {

    private final TextView shelfCaption;
    private final BookshelfView shelf;
    private final BooksAdapter adapter;

    private final int REL_SWIPE_MIN_DISTANCE;
    private final int REL_SWIPE_MAX_OFF_PATH;
    private final int REL_SWIPE_THRESHOLD_VELOCITY;

    public BookcaseView(IBrowserActivity base, BooksAdapter adapter) {
        super(base.getContext());
        DisplayMetrics dm = getResources().getDisplayMetrics();
        REL_SWIPE_MIN_DISTANCE = (int)(120.0f * dm.densityDpi / 160.0f + 0.5);
        REL_SWIPE_MAX_OFF_PATH = (int)(250.0f * dm.densityDpi / 160.0f + 0.5);
        REL_SWIPE_THRESHOLD_VELOCITY = (int)(200.0f * dm.densityDpi / 160.0f + 0.5);

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


        final GestureDetector gestureDetector = new GestureDetector(new SwipeGestureDetector());
        View.OnTouchListener gestureListener = new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
            }};
        shelf.setOnTouchListener(gestureListener);
    }

    class SwipeGestureDetector extends SimpleOnGestureListener{

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            int pos = shelf.pointToPosition((int)e.getX(), (int)e.getY());
            shelf.onItemClick(pos);
            return false;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (Math.abs(e1.getY() - e2.getY()) > REL_SWIPE_MAX_OFF_PATH)
                return false;
            if(e1.getX() - e2.getX() > REL_SWIPE_MIN_DISTANCE &&
                Math.abs(velocityX) > REL_SWIPE_THRESHOLD_VELOCITY) {
                adapter.prevList();
            }  else if (e2.getX() - e1.getX() > REL_SWIPE_MIN_DISTANCE &&
                Math.abs(velocityX) > REL_SWIPE_THRESHOLD_VELOCITY) {
                adapter.nextList();
            }
            return false;
        }

    }
}
