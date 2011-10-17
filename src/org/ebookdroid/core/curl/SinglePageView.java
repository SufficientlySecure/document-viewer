package org.ebookdroid.core.curl;

import org.ebookdroid.core.Page;
import org.ebookdroid.core.SinglePageDocumentView;
import org.ebookdroid.core.ViewState;

import android.graphics.Canvas;
import android.view.MotionEvent;

public class SinglePageView implements PageAnimator {

    protected final SinglePageDocumentView view;

    public SinglePageView(final SinglePageDocumentView view) {
        this.view = view;
    }

    @Override
    public PageAnimationType getType() {
        return PageAnimationType.NONE;
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
        }
    }

    @Override
    public void init() {
    }

    @Override
    public void resetPageIndexes(final int currentIndex) {
    }

    @Override
    public void setViewDrawn(final boolean b) {
    }

    @Override
    public void FlipAnimationStep() {
    }
}
