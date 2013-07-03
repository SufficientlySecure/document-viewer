package org.ebookdroid.common.touch;

import org.ebookdroid.R;
import org.ebookdroid.common.touch.TouchManager.Region;
import org.ebookdroid.common.touch.TouchManager.TouchProfile;
import org.ebookdroid.ui.viewer.IActivityController;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.FloatMath;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;

import java.util.ListIterator;

import org.emdev.common.log.LogContext;
import org.emdev.common.log.LogManager;
import org.emdev.ui.actions.ActionController;
import org.emdev.utils.MathUtils;

public class TouchManagerView extends View {

    private static final LogContext LCTX = LogManager.root().lctx("TouchManagerView");

    private static final float GRID_X = 10;
    private static final float GRID_Y = 10;

    private static final int[] COLORS = { Color.BLUE, Color.GRAY, Color.RED, Color.YELLOW };

    private final IActivityController base;

    private final Paint bgPaint;
    private final Paint gridPaint;
    private final ActionController<TouchManagerView> actions;
    private final DefaultGestureDetector detector;

    private TouchProfile profile;

    private final Paint rPaint = new Paint();

    public TouchManagerView(final IActivityController base) {
        super(base.getContext());
        this.base = base;
        this.actions = new ActionController<TouchManagerView>(base, this);
        this.detector = new DefaultGestureDetector(getContext(), new GestureListener());

        super.setVisibility(View.GONE);

        setFocusable(true);
        setFocusableInTouchMode(true);

        bgPaint = new Paint();
        bgPaint.setColor(Color.BLACK);
        bgPaint.setStyle(Paint.Style.FILL);
        bgPaint.setAlpha(0x40);

        gridPaint = new Paint();
        gridPaint.setColor(Color.GREEN);
    }

    @Override
    public void setVisibility(final int visibility) {
        if (visibility == View.VISIBLE) {
            profile = TouchManager.topProfile();
        } else {
            profile = null;
        }
        super.setVisibility(visibility);
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        super.onDraw(canvas);
        if (profile == null) {
            return;
        }

        final int width = getWidth();
        final int height = getHeight();

        canvas.drawRect(0, 0, width, height, bgPaint);

        final float xStep = width / GRID_X;
        final float yStep = height / GRID_Y;

        int cIndex = 0;
        ListIterator<Region> regions = profile.regions(false);
        while (regions.hasPrevious()) {
            final Region region = regions.previous();
            final RectF r = region.getActualRect(width, height);

            rPaint.setColor(COLORS[cIndex]);
            rPaint.setAlpha(0x80);
            canvas.drawRect(r, rPaint);

            cIndex = (cIndex + 1) % COLORS.length;
        }

        drawGrid(canvas, xStep, yStep);

        cIndex = 0;
        regions = profile.regions(false);
        while (regions.hasPrevious()) {
            final Region region = regions.previous();
            final RectF r = region.getActualRect(width, height);
            rPaint.setColor(COLORS[cIndex]);
            drawBounds(canvas, r, rPaint);

            cIndex = (cIndex + 1) % COLORS.length;
        }

        if (current != null) {
            rPaint.setColor(Color.WHITE);
            rPaint.setAlpha(0x80);
            final RectF r = current.getActualRect(width, height);
            canvas.drawRect(r, rPaint);
            rPaint.setColor(Color.WHITE);
            drawBounds(canvas, r, rPaint);
        }
    }

    protected void drawGrid(final Canvas canvas, final float xStep, final float yStep) {
        for (float x = xStep; x < getWidth(); x += xStep) {
            canvas.drawLine(x, 0, x, getHeight(), gridPaint);
        }

        for (float y = yStep; y < getHeight(); y += yStep) {
            canvas.drawLine(0, y, getWidth(), y, gridPaint);
        }
    }

    protected void drawBounds(final Canvas canvas, final RectF r, final Paint p) {
        canvas.drawLine(r.left, r.top, r.right - 1, r.top, p);
        canvas.drawLine(r.left, r.bottom - 1, r.right - 1, r.bottom - 1, p);
        canvas.drawLine(r.left, r.top, r.left, r.bottom - 1, p);
        canvas.drawLine(r.right - 1, r.top, r.right - 1, r.bottom - 1, p);
    }

    private PointF startPoint;
    private PointF endPoint;
    private Region current;

    protected void processRegion() {
        if (profile != null) {
            if (startPoint != null && endPoint != null) {
                current = getOrCreareRegion(startPoint, endPoint);
            }
            if (LCTX.isDebugEnabled()) {
                LCTX.d("processRegion(): " + current);
            }
            if (current != null) {
                final TouchConfigDialog dlg = new TouchConfigDialog(base, this, profile, current);
                dlg.show();
            }
        }

        startPoint = null;
        endPoint = null;
        current = null;
    }

    protected Region getOrCreareRegion(final PointF startPoint, final PointF endPoint) {
        final Region selected = getRegion(startPoint, endPoint);
        for (final Region r : profile.regions) {
            if (r.getRect().equals(selected.getRect())) {
                return r;
            }
        }
        profile.addRegion(selected);
        return selected;
    }

    @Override
    public final boolean onTouchEvent(final MotionEvent ev) {
        try {
            Thread.sleep(16);
        } catch (final InterruptedException e) {
            Thread.interrupted();
        }

        if (LCTX.isDebugEnabled()) {
            LCTX.d("onTouchEvent(): " + ev);
        }
        boolean res = detector.onTouchEvent(ev);

        int action = ev.getAction();
        if (!res && (action == MotionEvent.ACTION_UP)) {
            if (startPoint != null) {
                endPoint = new PointF(ev.getX(), ev.getY());
                current = getRegion(startPoint, endPoint);
                processRegion();
            }
            invalidate();
            return true;
        }
        return res;
    }

    protected Region getRegion(final PointF startPoint, final PointF endPoint) {
        final float width = getWidth();
        final float height = getHeight();
        final float xStep = width / GRID_X;
        final float yStep = height / GRID_Y;

        final float cellWidth = 100 / GRID_X;
        final float cellHeight = 100 / GRID_X;

        float left = MathUtils.fmin(startPoint.x, endPoint.x);
        float right = MathUtils.fmax(startPoint.x, endPoint.x);
        float top = MathUtils.fmin(startPoint.y, endPoint.y);
        float bottom = MathUtils.fmax(startPoint.y, endPoint.y);

        left = cellWidth * FloatMath.floor(left / xStep);
        right = cellWidth * FloatMath.floor(right / xStep) + cellWidth;
        top = cellHeight * FloatMath.floor(top / yStep);
        bottom = cellHeight * FloatMath.floor(bottom / yStep) + cellHeight;

        return new Region(MathUtils.rect(left, top, right, bottom));
    }

    protected class GestureListener extends SimpleOnGestureListener {

        @Override
        public boolean onDoubleTap(final MotionEvent e) {
            actions.getOrCreateAction(R.id.actions_toggleTouchManagerView).run();
            return true;
        }

        @Override
        public boolean onDown(final MotionEvent e) {
            startPoint = new PointF(e.getX(), e.getY());
            endPoint = startPoint;
            current = getRegion(startPoint, endPoint);
            if (LCTX.isDebugEnabled()) {
                LCTX.d("onDown(): " + current);
            }
            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            if (LCTX.isDebugEnabled()) {
                LCTX.d("onSingleTapUp(): " + current);
            }
            endPoint = null;
            return true;
        }

        @Override
        public boolean onScroll(final MotionEvent e1, final MotionEvent e2, final float distanceX, final float distanceY) {
            final float x = e2.getX(), y = e2.getY();
            endPoint = new PointF(x, y);
            current = getRegion(startPoint, endPoint);
            if (LCTX.isDebugEnabled()) {
                LCTX.d("onScroll(): " + current);
            }
            invalidate();
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(final MotionEvent e) {
            if (profile != null) {
                current = profile.getRegion(e.getX(), e.getY(), getWidth(), getHeight());
                if (LCTX.isDebugEnabled()) {
                    LCTX.d("onSingleTapConfirmed(): " + current);
                }
                processRegion();
            }
            return true;
        }

        @Override
        public void onLongPress(final MotionEvent e) {
        }
    }

}
