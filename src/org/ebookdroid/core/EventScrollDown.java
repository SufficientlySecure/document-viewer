package org.ebookdroid.core;

import android.graphics.RectF;

import java.util.Queue;

import org.emdev.utils.LengthUtils;

public class EventScrollDown extends AbstractEventScroll<EventScrollDown> {

    public EventScrollDown(final Queue<EventScrollDown> eventQueue) {
        super(eventQueue);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.AbstractEvent#calculatePageVisibility(org.ebookdroid.core.ViewState)
     */
    @Override
    protected void calculatePageVisibility() {
        final Page[] pages = ctrl.model.getPages();
        if (LengthUtils.isEmpty(pages)) {
            return;
        }

        final int firstVisiblePage = viewState.pages.firstVisible;
        final int lastVisiblePage = viewState.pages.lastVisible;

        if (firstVisiblePage == -1) {
            super.calculatePageVisibility();
            return;
        }

        final RectF bounds = new RectF();

        if (ctrl.isPageVisible(pages[firstVisiblePage], viewState, bounds)) {
            findLastVisiblePage(pages, firstVisiblePage, true, bounds);
            return;
        }

        if (firstVisiblePage != lastVisiblePage && ctrl.isPageVisible(pages[lastVisiblePage], viewState, bounds)) {
            findFirstVisiblePage(pages, lastVisiblePage, true, bounds);
            return;
        }

        final int midIndex = firstVisiblePage;
        int delta = 0;
        int run = 2;
        while (run > 0) {
            run = 0;
            final int left = midIndex - delta;
            final int right = midIndex + delta;
            if (left >= 0) {
                run++;
                if (ctrl.isPageVisible(pages[left], viewState, bounds)) {
                    findFirstVisiblePage(pages, left, false, bounds);
                    return;
                }
            }
            if (right < pages.length - 1) {
                run++;
                if (ctrl.isPageVisible(pages[right], viewState, bounds)) {
                    findLastVisiblePage(pages, right, false, bounds);
                    return;
                }
            }
            delta++;
        }

        viewState.update(-1, -1);
    }

    protected void findLastVisiblePage(final Page[] pages, final int first, final boolean updateFirst,
            final RectF bounds) {
        int firstVisiblePage = first;
        // If firstVisiblePage is still visible, try to find lastVisiblePage
        int lastVisiblePage = firstVisiblePage;
        while (lastVisiblePage < pages.length - 1) {
            final int index = lastVisiblePage + 1;
            if (!ctrl.isPageVisible(pages[index], viewState, bounds)) {
                break;
            }
            lastVisiblePage = index;
        }
        if (updateFirst) {
            // Then try to find real firstVisiblePage
            for (int index = firstVisiblePage - 1; index >= 0; index--) {
                if (!ctrl.isPageVisible(pages[index], viewState, bounds)) {
                    break;
                }
                firstVisiblePage = index;
            }
        }
        viewState.update(firstVisiblePage, lastVisiblePage);
    }

    protected void findFirstVisiblePage(final Page[] pages, final int last, final boolean updateLast, final RectF bounds) {
        int lastVisiblePage = last;
        // If lastVisiblePage is still visible, try to find firstVisiblePage
        int firstVisiblePage = lastVisiblePage;
        for (int index = lastVisiblePage - 1; index >= 0; index--) {
            if (!ctrl.isPageVisible(pages[index], viewState, bounds)) {
                break;
            }
            firstVisiblePage = index;
        }
        if (updateLast) {
            // Then try to find real lastVisiblePage
            for (int index = lastVisiblePage + 1; index < pages.length; index++) {
                if (!ctrl.isPageVisible(pages[index], viewState, bounds)) {
                    break;
                }
                lastVisiblePage = index;
            }
        }
        viewState.update(firstVisiblePage, lastVisiblePage);
    }
}
