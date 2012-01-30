package org.ebookdroid.core.touch;

import org.ebookdroid.core.models.ZoomModel;

import android.view.MotionEvent;

public class MultiTouchZoomImpl implements IMultiTouchZoom {

    private final ZoomModel zoomModel;
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
            multiEventCatched = true;
            return true;
        }
        if (ev.getAction() == MotionEvent.ACTION_MOVE && lastZoomDistance != 0 && ev.getPointerCount() > 1) {
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
        if (ev.getPointerCount() > 1) {
            // We do not need actual distance. Square also goes well.
            float x0 = ev.getX(0);
            float x1 = ev.getX(1);
            float y0 = ev.getY(0);
            float y1 = ev.getY(1);
            return (float) ((x0 - x1) * (x0 - x1) + (y0 - y1) * (y0 - y1));
        }
        return lastZoomDistance;
    }

    @Override
    public boolean enabled() {
        return true;
    }
}
