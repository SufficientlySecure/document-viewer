package org.ebookdroid.ui.viewer.views;

import org.ebookdroid.R;
import org.ebookdroid.common.bitmaps.BitmapManager;
import org.ebookdroid.core.events.ZoomListener;
import org.ebookdroid.core.models.ZoomModel;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Scroller;

public class ZoomRoll extends View implements ZoomListener {

    private static final float MULTIPLIER = 400.0f;
    private static final float MAX_VALUE = (1 + ZoomModel.MAX_ZOOM)* MULTIPLIER;

    private final Bitmap left;
    private final Bitmap right;
    private final Bitmap center;
    private final Bitmap serifs;
    private final Scroller scroller;
    private final ZoomModel zoomModel;

    private final GestureDetector gestureDetector;
    private boolean inTouch;

    public ZoomRoll(final Context context, final ZoomModel zoomModel) {
        super(context);
        this.zoomModel = zoomModel;
        this.zoomModel.addListener(this);

        left = BitmapManager.getResource(R.drawable.components_zoomroll_left);
        right = BitmapManager.getResource(R.drawable.components_zoomroll_right);
        center = BitmapManager.getResource(R.drawable.components_zoomroll_center);
        serifs = BitmapManager.getResource(R.drawable.components_zoomroll_serifs);

        scroller = new Scroller(context);
        gestureDetector = new GestureDetector(getContext(), new GestureListener());

        setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), Math.max(left.getHeight(), right.getHeight()));
    }

    @Override
    public void draw(final Canvas canvas) {
        super.draw(canvas);
        final Paint paint = new Paint();
        canvas.drawBitmap(center, new Rect(0, 0, center.getWidth(), center.getHeight()), new Rect(0, 0, getWidth(),
                getHeight()), paint);
        float currentOffset = -getCurrentValue() % 40;
        while (currentOffset < getWidth()) {
            canvas.drawBitmap(serifs, currentOffset, (getHeight() - serifs.getHeight()) / 2.0f, paint);
            currentOffset += serifs.getWidth();
        }
        canvas.drawBitmap(left, 0, 0, paint);
        canvas.drawBitmap(right, getWidth() - right.getWidth(), getHeight() - right.getHeight(), paint);

        Paint textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(24);
        textPaint.setAntiAlias(true);
        String zoomText = String.format("%.2f", zoomModel.getZoom());
        canvas.drawText(zoomText+"x", 6, getHeight() / 2 + 12, textPaint);
    }

    @Override
    public boolean onTouchEvent(final MotionEvent ev) {
        super.onTouchEvent(ev);

        try {
            Thread.sleep(16);
        } catch (final InterruptedException e) {
            Thread.interrupted();
        }

        if ((ev.getAction() & MotionEvent.ACTION_UP) == MotionEvent.ACTION_UP) {
            inTouch = false;
        }

        final boolean res = gestureDetector.onTouchEvent(ev);
        computeScroll();
        return res;
    }

    @Override
    public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            setCurrentValue(scroller.getCurrX());
            invalidate();
        } else if (!inTouch) {
            zoomModel.commit();
        }
    }

    public float getCurrentValue() {
        return (zoomModel.getZoom() - 1.0f) * MULTIPLIER;
    }

    public void setCurrentValue(float currentValue) {
        if (currentValue < 0.0) {
            currentValue = 0.0f;
        }
        if (currentValue > MAX_VALUE) {
            currentValue = MAX_VALUE;
        }
        final float zoom = 1.0f + currentValue / MULTIPLIER;
        zoomModel.setZoom(zoom);
    }

    @Override
    public void zoomChanged(float oldZoom, float newZoom, boolean committed) {
        postInvalidate();
    }

    protected class GestureListener extends SimpleOnGestureListener {

        @Override
        public boolean onDoubleTap(final MotionEvent e) {
            setCurrentValue(0.0f);
            zoomModel.commit();
            ZoomRoll.this.postInvalidate();
            return true;
        }

        @Override
        public boolean onDown(final MotionEvent e) {
            inTouch = true;
            if (!scroller.isFinished()) {
                scroller.abortAnimation();
                zoomModel.commit();
            }
            return true;
        }

        @Override
        public boolean onFling(final MotionEvent e1, final MotionEvent e2, final float vX, final float vY) {
            inTouch = false;
            scroller.fling((int) getCurrentValue(), 0, -(int)vX, 0, 0, (int)MAX_VALUE, 0, 0);
            ZoomRoll.this.postInvalidate();
            return true;
        }

        @Override
        public boolean onScroll(final MotionEvent e1, final MotionEvent e2, final float distanceX, final float distanceY) {
            setCurrentValue(getCurrentValue() + distanceX);
            scroller.computeScrollOffset();
            ZoomRoll.this.postInvalidate();
            return true;
        }

        @Override
        public boolean onSingleTapUp(final MotionEvent e) {
            scroller.computeScrollOffset();
            inTouch = false;
            zoomModel.commit();
            return true;
        }

    }

}
