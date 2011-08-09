package org.ebookdroid.core.events;

public interface CurrentPageListener {

    void currentPageChanged(int docPageIndex, int viewPageIndex);

    public class CurrentPageChangedEvent extends SafeEvent<CurrentPageListener> {

        private final int docPageIndex;

        private final int viewPageIndex;

        public CurrentPageChangedEvent(final int docPageIndex, final int viewPageIndex) {
            this.docPageIndex = docPageIndex;
            this.viewPageIndex = viewPageIndex;
        }

        @Override
        public void dispatchSafely(final CurrentPageListener listener) {
            listener.currentPageChanged(docPageIndex, viewPageIndex);
        }
    }
}
