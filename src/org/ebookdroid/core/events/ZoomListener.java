package org.ebookdroid.core.events;

public interface ZoomListener {

    void zoomChanged(float newZoom, float oldZoom);

    void commitZoom();

    public class CommitZoomEvent extends SafeEvent<ZoomListener> {

        @Override
        public void dispatchSafely(final ZoomListener listener) {
            listener.commitZoom();
        }
    }
}
