package org.ebookdroid.core.curl;

import org.ebookdroid.core.Page;
import org.ebookdroid.core.ViewState;

import android.graphics.Canvas;
import android.view.MotionEvent;

import java.util.concurrent.atomic.AtomicReference;

public class PageAnimatorProxy implements PageAnimator {

    private final AtomicReference<PageAnimator> orig;

    public PageAnimatorProxy(PageAnimator pa) {
        orig = new AtomicReference<PageAnimator>(pa);
    }

    public void switchCurler(PageAnimator orig) {
        this.orig.set(orig);
    }

    @Override
    public void init() {
    }

    @Override
    public boolean enabled() {
        return orig.get().enabled();
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return orig.get().onTouchEvent(ev);
    }

    @Override
    public PageAnimationType getType() {
        return orig.get().getType();
    }

    @Override
    public void resetPageIndexes(int currentIndex) {
        orig.get().resetPageIndexes(currentIndex);
    }

    @Override
    public void draw(Canvas canvas, ViewState viewState) {
        orig.get().draw(canvas, viewState);
    }

    @Override
    public void setViewDrawn(boolean b) {
        orig.get().setViewDrawn(b);
    }

    @Override
    public void FlipAnimationStep() {
        orig.get().FlipAnimationStep();
    }

    @Override
    public boolean isPageVisible(Page page, ViewState viewState) {
        return orig.get().isPageVisible(page, viewState);
    }

    public void pageUpdated(int viewIndex) {
        orig.get().pageUpdated(viewIndex);
    }
}
