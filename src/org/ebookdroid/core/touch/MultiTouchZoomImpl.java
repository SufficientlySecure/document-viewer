package org.ebookdroid.core.touch;

import android.graphics.PointF;
import android.util.FloatMath;
import android.util.Pair;
import android.view.MotionEvent;

public class MultiTouchZoomImpl implements IMultiTouchZoom {

    private final IMultiTouchListener listener;
    private float twoFingerDistance;
    private boolean twoFingerPress = false;
    private boolean multiEventCatched;
    private PointF multiCenter;

    public MultiTouchZoomImpl(final IMultiTouchListener listener) {
        this.listener = listener;
    }

    @Override
    public boolean onTouchEvent(final MotionEvent ev) {
        if ((ev.getAction() & MotionEvent.ACTION_POINTER_DOWN) == MotionEvent.ACTION_POINTER_DOWN) {
            switch (ev.getPointerCount()) {
                case 2:
                    twoFingerDistance = getZoomDistance(ev);
                    twoFingerPress = true;
                    break;
                default:
                    twoFingerPress = false;
            }

            multiCenter = calculateCenter(ev);
            multiEventCatched = true;
            return true;
        }
        if ((ev.getAction() & MotionEvent.ACTION_POINTER_UP) == MotionEvent.ACTION_POINTER_UP) {
            if (ev.getPointerCount() < 2) {
                if (twoFingerDistance > 0) {
                    twoFingerDistance = 0;
                    listener.onTwoFingerPinchEnd();
                }
                if (twoFingerPress) {
                    listener.onTwoFingerTap();
                }
                twoFingerPress = false;
            }
            multiEventCatched = true;
            return true;
        }
        if (ev.getAction() == MotionEvent.ACTION_MOVE) {
            if (twoFingerDistance != 0 && ev.getPointerCount() == 2) {
                PointF newCenter = calculateCenter(ev);
                if (distance(newCenter, multiCenter) > 10.0f || !twoFingerPress) {
                    twoFingerPress = false; // We moved it is not a tap anymore!

                    final float zoomDistance = getZoomDistance(ev);

                    listener.onTwoFingerPinch(twoFingerDistance, zoomDistance);
                    twoFingerDistance = zoomDistance;
                }
                multiEventCatched = true;
            }
            return multiEventCatched;
        }
        if (ev.getAction() == MotionEvent.ACTION_UP && multiEventCatched) {
            multiEventCatched = false;
            return true;
        }
        return false;
    }

    private float distance(PointF p0, PointF p1) {
        return FloatMath.sqrt(((p0.x - p1.x) * (p0.x - p1.x) + (p0.y - p1.y) * (p0.y - p1.y)));
    }

    private PointF calculateCenter(MotionEvent ev) {
        float x = 0, y = 0;
        for (int i = 0; i < ev.getPointerCount(); i++) {
            x += ev.getX(i);
            y += ev.getY(i);
        }
        return new PointF(x / ev.getPointerCount(), y / ev.getPointerCount());
    }

    private float getZoomDistance(final MotionEvent ev) {
        if (ev.getPointerCount() == 2) {
            float x0 = ev.getX(0);
            float x1 = ev.getX(1);
            float y0 = ev.getY(0);
            float y1 = ev.getY(1);
            return FloatMath.sqrt(((x0 - x1) * (x0 - x1) + (y0 - y1) * (y0 - y1)));
        }
        return twoFingerDistance;
    }

    @Override
    public boolean enabled() {
        return true;
    }
}
