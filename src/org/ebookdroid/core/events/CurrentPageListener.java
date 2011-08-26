package org.ebookdroid.core.events;

import org.ebookdroid.core.PageIndex;

public interface CurrentPageListener {

    void currentPageChanged(PageIndex oldIndex, PageIndex newIndex);

    public class CurrentPageChangedEvent extends SafeEvent<CurrentPageListener> {

        private final PageIndex oldIndex;

        private final PageIndex newIndex;

        public CurrentPageChangedEvent(final PageIndex oldIndex, final PageIndex newIndex) {
            this.oldIndex = oldIndex;
            this.newIndex = newIndex;
        }

        @Override
        public void dispatchSafely(final CurrentPageListener listener) {
            listener.currentPageChanged(oldIndex, newIndex);
        }
    }
}
