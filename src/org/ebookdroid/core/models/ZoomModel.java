package org.ebookdroid.core.models;

import org.ebookdroid.core.events.ZoomListener;

import org.emdev.utils.MathUtils;
import org.emdev.utils.listeners.ListenerProxy;

public class ZoomModel extends ListenerProxy {

    public static final float MIN_ZOOM = 1.0f;
    public static final float MAX_ZOOM = 32.0f;

    private static final float ZOOM_ROUND_FACTOR = 32.0f;

    private float initialZoom = MIN_ZOOM;
    private float currentZoom = MIN_ZOOM;

    private boolean isCommited;

    public ZoomModel() {
        super(ZoomListener.class);
    }

    public void initZoom(final float zoom) {
        this.initialZoom = this.currentZoom = MathUtils.adjust(MathUtils.round(zoom, ZOOM_ROUND_FACTOR), MIN_ZOOM,
                MAX_ZOOM);
        isCommited = true;
    }

    public void setZoom(final float zoom) {
        final float newZoom = MathUtils.adjust(MathUtils.round(zoom, ZOOM_ROUND_FACTOR), MIN_ZOOM, MAX_ZOOM);
        final float oldZoom = this.currentZoom;
        if (newZoom != oldZoom) {
            isCommited = false;
            this.currentZoom = newZoom;
            this.<ZoomListener> getListener().zoomChanged(oldZoom, newZoom, false);
        }
    }

    public void scaleZoom(final float factor) {
        setZoom(currentZoom * factor);
    }

    public float getZoom() {
        return currentZoom;
    }

    public void commit() {
        if (!isCommited) {
            isCommited = true;
            this.<ZoomListener> getListener().zoomChanged(initialZoom, currentZoom, true);
            initialZoom = currentZoom;
        }
    }
}
