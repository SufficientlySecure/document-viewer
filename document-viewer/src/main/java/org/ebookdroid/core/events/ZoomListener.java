package org.ebookdroid.core.events;

import android.graphics.PointF;
import android.support.annotation.Nullable;

public interface ZoomListener {

    void zoomChanged(float oldZoom, float newZoom, boolean committed, @Nullable PointF center);
}
