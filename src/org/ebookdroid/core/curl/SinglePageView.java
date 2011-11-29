package org.ebookdroid.core.curl;

import org.ebookdroid.R;
import org.ebookdroid.core.Page;
import org.ebookdroid.core.SinglePageDocumentView;
import org.ebookdroid.core.ViewState;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.MotionEvent;

public class SinglePageView implements PageAnimator {

    protected static Bitmap dragBitmap;

    protected final PageAnimationType type;

    protected final SinglePageDocumentView view;

    protected boolean bViewDrawn;

    protected int foreIndex = -1;

    protected int backIndex = -1;

    public SinglePageView(final SinglePageDocumentView view) {
        this(PageAnimationType.NONE, view);
    }

    protected SinglePageView(PageAnimationType type, final SinglePageDocumentView view) {
        this.type = type;
        this.view = view;
    }

    @Override
    public void init() {
        if (dragBitmap == null) {
            dragBitmap = BitmapFactory.decodeResource(view.getBase().getContext().getResources(), R.drawable.drag);
        }
    }

    @Override
    public final PageAnimationType getType() {
        return type;
    }

    @Override
    public boolean enabled() {
        return false;
    }

    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        return false;
    }

    @Override
    public boolean isPageVisible(final Page page, final ViewState viewState) {
        final int pageIndex = page.index.viewIndex;
        return pageIndex == view.calculateCurrentPage(viewState);
    }

    @Override
    public void draw(final Canvas canvas, final ViewState viewState) {
        final Page page = view.getBase().getDocumentModel().getCurrentPageObject();
        if (page != null) {
            page.draw(canvas, viewState);

            Rect l = view.getScrollLimits();
            if (l.width() + l.height() > 0) {
                final Paint paint = new Paint();
                paint.setFilterBitmap(true);
                paint.setAntiAlias(true);
                paint.setDither(true);

                float x = viewState.view.getScrollX() + viewState.view.getWidth() - dragBitmap.getWidth() - 1;
                float y = viewState.view.getScrollY() + viewState.view.getHeight() - dragBitmap.getHeight() - 1;

                canvas.drawBitmap(dragBitmap, x, y, paint);
            }
        }
    }

    @Override
    public final void resetPageIndexes(final int currentIndex) {
        foreIndex = backIndex = currentIndex;
    }

    @Override
    public void FlipAnimationStep() {
    }

    @Override
    public final void setViewDrawn(final boolean bViewDrawn) {
        this.bViewDrawn = bViewDrawn;
    }

    public boolean isViewDrawn() {
        return bViewDrawn;
    }

    @Override
    public void pageUpdated(int viewIndex) {
    }
}
