package org.ebookdroid.ui.viewer.views;

import org.ebookdroid.R;
import org.ebookdroid.common.touch.DefaultGestureDetector;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.widget.TextView;

import org.emdev.common.log.LogContext;
import org.emdev.common.log.LogManager;
import org.emdev.ui.actions.IActionController;

public class BookmarkView extends TextView {

    protected final LogContext LCTX = LogManager.root().lctx("BookmarkView", true);

    protected IActionController<?> actions;

    protected DefaultGestureDetector detector;

    public BookmarkView(final Context context) {
        super(context);
        init(context);
    }

    public BookmarkView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public BookmarkView(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    protected void init(final Context context) {
        detector = new DefaultGestureDetector(context, new GestureListener());
    }

    public IActionController<?> getActions() {
        return actions;
    }

    public void setActions(final IActionController<?> actions) {
        this.actions = actions;
    }

    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        if (LCTX.isDebugEnabled()) {
            LCTX.d("onTouch(" + event + ")");
        }
        super.onTouchEvent(event);
        return detector.onTouchEvent(event);
    }

    protected class GestureListener extends SimpleOnGestureListener {

        /**
         * {@inheritDoc}
         *
         * @see android.view.GestureDetector.SimpleOnGestureListener#onSingleTapConfirmed(android.view.MotionEvent)
         */
        @Override
        public boolean onSingleTapConfirmed(final MotionEvent e) {
            if (LCTX.isDebugEnabled()) {
                LCTX.d("onSingleTapConfirmed(" + e + ")");
            }
            if (actions != null) {
                actions.getOrCreateAction(R.id.actions_setBookmarkedPage).onClick(BookmarkView.this);
            }
            return true; // processTap(TouchManager.Touch.SingleTap, e);
        }

        /**
         * {@inheritDoc}
         *
         * @see android.view.GestureDetector.SimpleOnGestureListener#onDoubleTap(android.view.MotionEvent)
         */
        @Override
        public boolean onDoubleTap(final MotionEvent e) {
            if (LCTX.isDebugEnabled()) {
                LCTX.d("onDoubleTap(" + e + ")");
            }
            if (actions != null) {
                actions.getOrCreateAction(R.id.bookmark_add).onClick(BookmarkView.this);
            }
            return true; // processTap(TouchManager.Touch.DoubleTap, e);
        }

        /**
         * {@inheritDoc}
         *
         * @see android.view.GestureDetector.SimpleOnGestureListener#onLongPress(android.view.MotionEvent)
         */
        @Override
        public void onLongPress(final MotionEvent e) {
            if (LCTX.isDebugEnabled()) {
                LCTX.d("onLongPress(" + e + ")");
            }
            final MotionEvent cancel = MotionEvent.obtain(e);
            cancel.setAction(MotionEvent.ACTION_CANCEL);
            detector.onTouchEvent(cancel);
        }

        /**
         * {@inheritDoc}
         *
         * @see android.view.GestureDetector.SimpleOnGestureListener#onDown(android.view.MotionEvent)
         */
        @Override
        public boolean onDown(final MotionEvent e) {
            return true;
        }

        /**
         * {@inheritDoc}
         *
         * @see android.view.GestureDetector.SimpleOnGestureListener#onSingleTapUp(android.view.MotionEvent)
         */
        @Override
        public boolean onSingleTapUp(final MotionEvent e) {
            return true;
        }

        /**
         * {@inheritDoc}
         *
         * @see android.view.GestureDetector.SimpleOnGestureListener#onFling(android.view.MotionEvent,
         *      android.view.MotionEvent, float, float)
         */
        @Override
        public boolean onFling(final MotionEvent e1, final MotionEvent e2, final float vX, final float vY) {
            return true;
        }

        /**
         * {@inheritDoc}
         *
         * @see android.view.GestureDetector.SimpleOnGestureListener#onScroll(android.view.MotionEvent,
         *      android.view.MotionEvent, float, float)
         */
        @Override
        public boolean onScroll(final MotionEvent e1, final MotionEvent e2, final float distanceX, final float distanceY) {
            return true;
        }
    }

}
