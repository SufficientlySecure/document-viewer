package org.ebookdroid.core.events;

public class BringUpZoomControlsEvent extends SafeEvent<BringUpZoomControlsListener> {

    @Override
    public void dispatchSafely(final BringUpZoomControlsListener listener) {
        listener.toggleZoomControls();
    }
}
