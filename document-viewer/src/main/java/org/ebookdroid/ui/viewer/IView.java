package org.ebookdroid.ui.viewer;

import org.ebookdroid.core.ViewState;

import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.view.View;

import org.emdev.common.log.LogContext;
import org.emdev.common.log.LogManager;

public interface IView {

    LogContext LCTX = LogManager.root().lctx("View");

    View getView();

    IActivityController getBase();

    void invalidateScroll();

    void invalidateScroll(final float newZoom, final float oldZoom, @Nullable PointF center);

    void startPageScroll(final int dx, final int dy);

    void startFling(final float vX, final float vY, final Rect limits);

    void continueScroll();

    /**
     * Forces the scrolling to finish
     *
     * @return true if this call forced the scrolling to finish, false if it was already finished
     */
    boolean forceFinishScroll();

    void scrollBy(int x, int y);

    void scrollTo(final int x, final int y);

    void _scrollTo(final int x, final int y);

    void onScrollChanged(final int curX, final int curY, final int oldX, final int oldY);

    RectF getViewRect();

    void changeLayoutLock(final boolean lock);

    boolean isLayoutLocked();

    @WorkerThread
    void waitForInitialization();

    void onDestroy();

    float getScrollScaleRatio();

    void stopScroller();

    void redrawView();

    void redrawView(final ViewState viewState);

    int getScrollX();

    int getScrollY();

    int getWidth();

    int getHeight();

    PointF getBase(RectF viewRect);

    void checkFullScreenMode();

    boolean post(Runnable r);

    boolean isScrollFinished();
}
