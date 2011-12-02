package org.ebookdroid.core.views;

import org.ebookdroid.core.IBrowserActivity;
import org.ebookdroid.core.presentation.BooksAdapter;
import org.ebookdroid.core.presentation.BooksAdapter.BookShelfAdapter;

import android.content.Context;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Scroller;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Bookshelves extends ViewGroup {

    public static interface OnShelfSwitchListener {

        void onScreenSwitched(int screen);
    }

    private static final int SNAP_VELOCITY = 1000;
    private static final int INVALID_SHELF = -1;

    private Scroller scroller;
    private VelocityTracker velocityTracker;

    private final static int STATE_REST = 0;
    private final static int STATE_SCROLLING = 1;

    private int state = STATE_REST;

    private float lastMotionX;
    private int touchSlop;
    private int maximumVelocity;
    private int currentShelf;
    private int nextShelf = INVALID_SHELF;

    private boolean firstLayout = true;

    private OnShelfSwitchListener shelfSwitchListener;

    private final Map<String, BookshelfView> views = new LinkedHashMap<String, BookshelfView>();

    public Bookshelves(final Context context) {
        super(context);
        init();
    }

    private void init() {
        scroller = new Scroller(getContext());

        final ViewConfiguration configuration = ViewConfiguration.get(getContext());
        touchSlop = configuration.getScaledTouchSlop();
        maximumVelocity = configuration.getScaledMaximumFlingVelocity();
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        final int width = MeasureSpec.getSize(widthMeasureSpec);

        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            getChildAt(i).measure(widthMeasureSpec, heightMeasureSpec);
        }

        if (firstLayout) {
            scrollTo(currentShelf * width, 0);
            firstLayout = false;
        }
    }

    @Override
    protected void onLayout(final boolean changed, final int l, final int t, final int r, final int b) {
        int childLeft = 0;

        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != View.GONE) {
                final int childWidth = child.getMeasuredWidth();
                child.layout(childLeft, 0, childLeft + childWidth, child.getMeasuredHeight());
                childLeft += childWidth;
            }
        }
    }

    @Override
    public boolean onTouchEvent(final MotionEvent ev) {
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        }
        velocityTracker.addMovement(ev);

        final int action = ev.getAction();
        final float x = ev.getRawX();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (!scroller.isFinished()) {
                    scroller.abortAnimation();
                }

                lastMotionX = x;

                state = scroller.isFinished() ? STATE_REST : STATE_SCROLLING;

                break;

            case MotionEvent.ACTION_MOVE:
                final int xDiff = (int) Math.abs(x - lastMotionX);

                final boolean xMoved = xDiff > touchSlop;

                if (xMoved) {
                    state = STATE_SCROLLING;
                }

                if (state == STATE_SCROLLING) {
                    final int deltaX = (int) (lastMotionX - x);
                    lastMotionX = x;

                    final int scrollX = getScrollX();
                    if (deltaX < 0) {
                        if (scrollX > 0) {
                            scrollBy(Math.max(-scrollX, deltaX), 0);
                        }
                    } else if (deltaX > 0) {
                        final int availableToScroll = getChildAt(getChildCount() - 1).getRight() - scrollX - getWidth();
                        if (availableToScroll > 0) {
                            scrollBy(Math.min(availableToScroll, deltaX), 0);
                        }
                    }
                }
                return state == STATE_SCROLLING;

            case MotionEvent.ACTION_UP:
                if (state == STATE_SCROLLING) {
                    final VelocityTracker vt = velocityTracker;
                    vt.computeCurrentVelocity(1000, maximumVelocity);
                    final int velocityX = (int) vt.getXVelocity();

                    if (velocityX > SNAP_VELOCITY && currentShelf > 0) {
                        // Fling hard enough to move left
                        snapToShelf(currentShelf - 1);
                    } else if (velocityX < -SNAP_VELOCITY && currentShelf < getChildCount() - 1) {
                        // Fling hard enough to move right
                        snapToShelf(currentShelf + 1);
                    } else {
                        snapToDestination();
                    }

                    if (velocityTracker != null) {
                        velocityTracker.recycle();
                        velocityTracker = null;
                    }
                    state = STATE_REST;
                    return true;
                }

                state = STATE_REST;

                break;

        }

        return false;
    }

    private void snapToDestination() {
        final int shelfWidth = getWidth();
        final int toShelf = (getScrollX() + (shelfWidth / 2)) / shelfWidth;

        snapToShelf(toShelf);
    }

    private void snapToShelf(int toShelf) {
        if (!scroller.isFinished()) {
            return;
        }

        toShelf = Math.max(0, Math.min(toShelf, getChildCount() - 1));

        nextShelf = toShelf;

        final int newX = toShelf * getWidth();
        final int delta = newX - getScrollX();
        scroller.startScroll(getScrollX(), 0, delta, 0, Math.abs(delta) * 2);
        invalidate();
    }

    @Override
    public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollTo(scroller.getCurrX(), scroller.getCurrY());
            postInvalidate();
        } else if (nextShelf != INVALID_SHELF) {
            currentShelf = Math.max(0, Math.min(nextShelf, getChildCount() - 1));

            if (shelfSwitchListener != null) {
                shelfSwitchListener.onScreenSwitched(currentShelf);
            }

            nextShelf = INVALID_SHELF;
        }
    }

    public int getCurrentShelf() {
        return currentShelf;
    }

    public void setCurrentShelf(final int shelf) {
        currentShelf = Math.max(0, Math.min(shelf, getChildCount() - 1));
        if (!scroller.isFinished()) {
            scroller.abortAnimation();
        }
        snapToShelf(currentShelf);
    }

    public void setOnShelfSwitchListener(final OnShelfSwitchListener onShelfSwitchListener) {
        shelfSwitchListener = onShelfSwitchListener;
    }

    public void updateShelves(final IBrowserActivity base, final BooksAdapter adapter) {
        final List<String> paths = adapter.getListPaths();
        final int v = paths.size();

        System.out.println("BS: recreate views: new=" + paths + ", old=" + views.keySet());

        final Set<String> keys = new HashSet<String>(views.keySet());
        keys.removeAll(paths);

        for (final String removed : keys) {
            final BookshelfView view = views.remove(removed);
            if (view != null) {
                System.out.println("BS: removed view:" + view.path);
                super.removeView(view);
            }
        }

        for (int i = 0; i < v; i++) {
            BookshelfView view = views.get(adapter.getListPath(i));
            if (view == null) {
                view = new BookshelfView(base, this, new BookShelfAdapter(adapter, i));
                views.put(view.path, view);
                System.out.println("BS: added view:" + view.path);
                super.addView(view);
            } else {
                ((BookShelfAdapter)view.getAdapter()).notifyDataSetChanged();
            }
        }
    }

}
