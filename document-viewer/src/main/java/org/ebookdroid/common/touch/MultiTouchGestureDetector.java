package org.ebookdroid.common.touch;


import android.annotation.TargetApi;
import android.graphics.PointF;
import android.util.FloatMath;
import android.view.MotionEvent;

import org.emdev.common.log.LogContext;
import org.emdev.common.log.LogManager;

@TargetApi(5)
public class MultiTouchGestureDetector implements IGestureDetector {

    protected static final LogContext LCTX = LogManager.root().lctx("Gesture", false);

    private final IMultiTouchListener listener;
    private float twoFingerDistance;

    private boolean multiEventCatched;
    private PointF multiCenter;

    private boolean twoFingerPress = false;
    private boolean twoFingerMove = false;

    public MultiTouchGestureDetector(final IMultiTouchListener listener) {
        this.listener = listener;
    }

    @Override
    public boolean enabled() {
        return true;
    }

    @Override
    public boolean onTouchEvent(final MotionEvent ev) {
        if ((ev.getAction() & MotionEvent.ACTION_POINTER_DOWN) == MotionEvent.ACTION_POINTER_DOWN) {

            if (LCTX.isDebugEnabled()) {
                LCTX.d("onTouchEvent(pointer down, " + ev.getPointerCount() + "): " + twoFingerPress + ", "
                        + twoFingerMove);
            }

            switch (ev.getPointerCount()) {
                case 2:
                    if (getTwoFingerDistance(ev) > 25){
                        twoFingerDistance = getTwoFingerDistance(ev);
                        twoFingerPress = true;
                        twoFingerMove = false;
                    }
                    break;
                default:
                    twoFingerPress = false;
                    twoFingerMove = false;
                    twoFingerDistance = 0;
            }

            multiCenter = calculateCenter(ev);
            multiEventCatched = true;
            return true;
        }

        if (ev.getAction() == MotionEvent.ACTION_MOVE) {

            if (LCTX.isDebugEnabled()) {
                LCTX.d("onTouchEvent(move, " + ev.getPointerCount() + "): " + twoFingerPress + ", "
                        + twoFingerMove);
            }

            if (twoFingerPress && ev.getPointerCount() == 2) {
                final PointF newCenter = calculateCenter(ev);
                // First time movement distance should be more than 10, next - without limitation
                if (distance(newCenter, multiCenter) > 10.0f || twoFingerMove) {
                    final float zoomDistance = getTwoFingerDistance(ev);
                    listener.onTwoFingerPinch(calculateCenterEvent(ev), twoFingerDistance, zoomDistance);
                    twoFingerDistance = zoomDistance;
                    twoFingerMove = true;
                }
                multiEventCatched = true;
            }
            return multiEventCatched;
        }

        if ((ev.getAction() & MotionEvent.ACTION_POINTER_UP) == MotionEvent.ACTION_POINTER_UP) {

            if (LCTX.isDebugEnabled()) {
                LCTX.d("onTouchEvent(pointer up, " + ev.getPointerCount() + "): " + twoFingerPress + ", "
                        + twoFingerMove);
            }

            if (twoFingerPress && ev.getPointerCount() == 2) {
                if (twoFingerMove) {
                    listener.onTwoFingerPinchEnd(calculateCenterEvent(ev));
                    twoFingerDistance = 0;
                    twoFingerMove = false;
                } else {
                    listener.onTwoFingerTap(calculateCenterEvent(ev));
                }
                twoFingerPress = false;
                multiEventCatched = true;
            }
            twoFingerPress = false;
            twoFingerMove = false;
            twoFingerDistance = 0;
            return true;
        }

        if (ev.getAction() == MotionEvent.ACTION_UP && multiEventCatched) {

            if (LCTX.isDebugEnabled()) {
                LCTX.d("onTouchEvent(up, " + ev.getPointerCount() + "): " + twoFingerPress + ", "
                        + twoFingerMove);
            }

            if (twoFingerPress && ev.getPointerCount() < 2) {
                // System.out.println("MultiTouchGestureDetector.onTouchEvent(): up pointer");
                if (twoFingerMove) {
                    listener.onTwoFingerPinchEnd(calculateCenterEvent(ev));
                } else {
                    listener.onTwoFingerTap(calculateCenterEvent(ev));
                }
            }
            multiEventCatched = false;
            twoFingerPress = false;
            twoFingerMove = false;
            twoFingerDistance = 0;
            return true;
        }

        return false;
    }

    private float distance(final PointF p0, final PointF p1) {
        return FloatMath.sqrt(((p0.x - p1.x) * (p0.x - p1.x) + (p0.y - p1.y) * (p0.y - p1.y)));
    }

    private PointF calculateCenter(final MotionEvent ev) {
        float x = 0, y = 0;
        for (int i = 0; i < ev.getPointerCount(); i++) {
            x += ev.getX(i);
            y += ev.getY(i);
        }
        return new PointF(x / ev.getPointerCount(), y / ev.getPointerCount());
    }

    private MotionEvent calculateCenterEvent(final MotionEvent ev) {
        final PointF center = calculateCenter(ev);
        final MotionEvent newEvent = MotionEvent.obtain(ev);
        newEvent.setLocation(center.x, center.y);
        return newEvent;

    }

    private float getTwoFingerDistance(final MotionEvent ev) {
        final float x0 = ev.getX(0);
        final float x1 = ev.getX(1);
        final float y0 = ev.getY(0);
        final float y1 = ev.getY(1);
        return FloatMath.sqrt(((x0 - x1) * (x0 - x1) + (y0 - y1) * (y0 - y1)));
    }
}
