package org.ebookdroid.core.multitouch;

import org.ebookdroid.core.models.ZoomModel;

import android.view.MotionEvent;

public class MultiTouchZoomImpl implements MultiTouchZoom {

    private final ZoomModel zoomModel;
    private boolean resetLastPointAfterZoom;
    private float lastZoomDistance;
    private boolean multiEventCatched;

    public MultiTouchZoomImpl(final ZoomModel zoomModel) {
        this.zoomModel = zoomModel;
    }

    @Override
    public boolean onTouchEvent(final MotionEvent ev) {
        if ((ev.getAction() & MotionEvent.ACTION_POINTER_DOWN) == MotionEvent.ACTION_POINTER_DOWN) {
            lastZoomDistance = getZoomDistance(ev);
            multiEventCatched = true;
            return true;
        }
        if ((ev.getAction() & MotionEvent.ACTION_POINTER_UP) == MotionEvent.ACTION_POINTER_UP) {
            lastZoomDistance = 0;
            zoomModel.commit();
            resetLastPointAfterZoom = true;
            multiEventCatched = true;
            return true;
        }
        if (ev.getAction() == MotionEvent.ACTION_MOVE && lastZoomDistance != 0) {
            final float zoomDistance = getZoomDistance(ev);
            zoomModel.setZoom(zoomModel.getZoom() * zoomDistance / lastZoomDistance);
            lastZoomDistance = zoomDistance;
            multiEventCatched = true;
            return true;
        }
        if (ev.getAction() == MotionEvent.ACTION_UP && multiEventCatched) {
            multiEventCatched = false;
            return true;
        }
        if (ev.getAction() == MotionEvent.ACTION_MOVE && multiEventCatched) {
            return true;
        }
        return false;
    }

    private float getZoomDistance(final MotionEvent ev) {
        // We do not need actual distance. Square also goes well.
        return (float) ((ev.getX(0) - ev.getX(1)) * (ev.getX(0) - ev.getX(1)) + (ev.getY(0) - ev.getY(1))
                * (ev.getY(0) - ev.getY(1)));
    }

    @Override
    public boolean isResetLastPointAfterZoom() {
        return resetLastPointAfterZoom;
    }

    @Override
    public void setResetLastPointAfterZoom(final boolean resetLastPointAfterZoom) {
        this.resetLastPointAfterZoom = resetLastPointAfterZoom;
    }
}
