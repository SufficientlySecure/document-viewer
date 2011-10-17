package org.ebookdroid.core.events;

public interface ZoomListener {

    void zoomChanged(float newZoom, float oldZoom);

    void commitZoom();

}
