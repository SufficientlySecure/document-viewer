package org.ebookdroid.core.models;

import org.ebookdroid.core.events.ListenerProxy;
import org.ebookdroid.core.events.ZoomListener;

public class ZoomModel extends ListenerProxy {

    private float zoom = 1.0f;
    private static final float INCREMENT_DELTA = 0.05f;
    private boolean horizontalScrollEnabled;
    private boolean isCommited;

    public ZoomModel() {
        super(ZoomListener.class);
    }

    public void setZoom(float zoom) {
        zoom = Math.max(zoom, 1.0f);
        if (this.zoom != zoom) {
            final float oldZoom = this.zoom;
            this.zoom = zoom;
            isCommited = false;

            this.<ZoomListener> getListener().zoomChanged(zoom, oldZoom);
        }
    }

    public float getZoom() {
        return zoom;
    }

    public void increaseZoom() {
        setZoom(getZoom() + INCREMENT_DELTA);
    }

    public void decreaseZoom() {
        setZoom(getZoom() - INCREMENT_DELTA);
    }

    public void setHorizontalScrollEnabled(final boolean horizontalScrollEnabled) {
        this.horizontalScrollEnabled = horizontalScrollEnabled;
    }

    public boolean isHorizontalScrollEnabled() {
        return horizontalScrollEnabled;
    }

    public boolean canDecrement() {
        return zoom > 1.0f;
    }

    public void commit() {
        if (!isCommited) {
            isCommited = true;
            this.<ZoomListener> getListener().commitZoom();
        }
    }
}
