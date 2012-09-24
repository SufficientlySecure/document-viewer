package org.ebookdroid.core;

import java.util.Queue;

import org.emdev.utils.LengthUtils;

public class EventScrollUp extends AbstractEventScroll<EventScrollUp> {

    EventScrollUp(final Queue<EventScrollUp> eventQueue) {
        super(eventQueue);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.AbstractEvent#calculatePageVisibility(org.ebookdroid.core.ViewState)
     */
    @Override
    protected ViewState calculatePageVisibility(final ViewState initial) {
        final Page[] pages = ctrl.model.getPages();
        if (LengthUtils.isEmpty(pages)) {
            return initial;
        }

        final int firstVisiblePage = initial.pages.firstVisible;
        final int lastVisiblePage = initial.pages.lastVisible;

        if (firstVisiblePage == -1) {
            return super.calculatePageVisibility(initial);
        }

        if (ctrl.isPageVisible(pages[firstVisiblePage], initial)) {
            return findLastVisiblePage(initial, pages, firstVisiblePage, true);
        }

        if (firstVisiblePage != lastVisiblePage && ctrl.isPageVisible(pages[lastVisiblePage], initial)) {
            return findFirstVisiblePage(initial, pages, lastVisiblePage, true);
        }

        final int midIndex = firstVisiblePage + 1;
        int delta = 0;
        int run = 2;
        while (run > 0) {
            run = 0;
            final int left = midIndex - delta;
            final int right = midIndex + delta;
            if (left >= 0) {
                run++;
                if (ctrl.isPageVisible(pages[left], initial)) {
                    return findFirstVisiblePage(initial, pages, left, false);
                }
            }
            if (right < pages.length - 1) {
                run++;
                if (ctrl.isPageVisible(pages[right], initial)) {
                    return findLastVisiblePage(initial, pages, right, false);
                }
            }
            delta++;
        }

        return new ViewState(initial, -1, -1);
    }

    protected ViewState findLastVisiblePage(final ViewState initial, final Page[] pages, final int first,
            final boolean updateFirst) {
        int firstVisiblePage = first;
        // If firstVisiblePage is still visible, try to find lastVisiblePage
        int lastVisiblePage = firstVisiblePage;
        while (lastVisiblePage < pages.length - 1) {
            final int index = lastVisiblePage + 1;
            if (!ctrl.isPageVisible(pages[index], initial)) {
                break;
            }
            lastVisiblePage = index;
        }
        if (updateFirst) {
            // Then try to find real firstVisiblePage
            for (int index = firstVisiblePage - 1; index >= 0; index--) {
                if (!ctrl.isPageVisible(pages[index], initial)) {
                    break;
                }
                firstVisiblePage = index;
            }
        }
        return new ViewState(initial, firstVisiblePage, lastVisiblePage);
    }

    protected ViewState findFirstVisiblePage(final ViewState initial, final Page[] pages, final int last,
            final boolean updateLast) {
        int lastVisiblePage = last;
        // If lastVisiblePage is still visible, try to find firstVisiblePage
        int firstVisiblePage = lastVisiblePage;
        for (int index = lastVisiblePage - 1; index >= 0; index--) {
            if (!ctrl.isPageVisible(pages[index], initial)) {
                break;
            }
            firstVisiblePage = index;
        }
        if (updateLast) {
            // Then try to find real lastVisiblePage
            for (int index = lastVisiblePage + 1; index < pages.length; index++) {
                if (!ctrl.isPageVisible(pages[index], initial)) {
                    break;
                }
                lastVisiblePage = index;
            }
        }
        return new ViewState(initial, firstVisiblePage, lastVisiblePage);
    }
}
