package org.ebookdroid.core;

import android.graphics.RectF;

import java.util.Queue;

import org.emdev.utils.LengthUtils;

public class EventScrollTo extends AbstractEventScroll<EventScrollTo> {

    public int viewIndex;

    public EventScrollTo(final Queue<EventScrollTo> eventQueue) {
        super(eventQueue);
    }

    final void init(final AbstractViewController ctrl, final int viewIndex) {
        super.init(ctrl);
        this.viewIndex = viewIndex;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.AbstractEvent#calculatePageVisibility(org.ebookdroid.core.ViewState)
     */
    @Override
    protected void calculatePageVisibility() {
        int firstVisiblePage = viewIndex;
        int lastVisiblePage = viewIndex;

        final Page[] pages = ctrl.model.getPages();
        if (LengthUtils.isEmpty(pages)) {
            return;
        }

        final RectF bounds = new RectF();

        while (firstVisiblePage > 0) {
            final int index = firstVisiblePage - 1;
            if (!ctrl.isPageVisible(pages[index], viewState, bounds)) {
                break;
            }
            firstVisiblePage = index;
        }
        while (lastVisiblePage < pages.length - 1) {
            final int index = lastVisiblePage + 1;
            if (!ctrl.isPageVisible(pages[index], viewState, bounds)) {
                break;
            }
            lastVisiblePage = index;
        }
        viewState.update(firstVisiblePage, lastVisiblePage);
    }

}
