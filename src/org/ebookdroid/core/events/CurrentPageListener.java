package org.ebookdroid.core.events;

public interface CurrentPageListener {

    void currentPageChanged(int pageIndex);

    public class CurrentPageChangedEvent extends SafeEvent<CurrentPageListener> {

        private final int pageIndex;

        public CurrentPageChangedEvent(final int pageIndex) {
            this.pageIndex = pageIndex;
        }

        @Override
        public void dispatchSafely(final CurrentPageListener listener) {
            listener.currentPageChanged(pageIndex);
        }
    }
}
