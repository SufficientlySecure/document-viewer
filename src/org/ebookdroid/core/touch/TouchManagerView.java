package org.ebookdroid.core.touch;

import org.ebookdroid.R;
import org.ebookdroid.core.IViewerActivity;
import org.ebookdroid.core.actions.ActionController;
import org.ebookdroid.core.actions.ActionEx;
import org.ebookdroid.core.log.LogContext;
import org.ebookdroid.core.touch.TouchManager.Region;
import org.ebookdroid.core.touch.TouchManager.Touch;
import org.ebookdroid.core.touch.TouchManager.TouchProfile;
import org.ebookdroid.utils.MathUtils;

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

public class TouchManagerView extends View {

    private static final LogContext LCTX = LogContext.ROOT.lctx("TouchManagerView");

    private static final String TMV_PROFILE = "TouchManagerView.Default";

    private static final float GRID_X = 10;
    private static final float GRID_Y = 10;

    private static final int[] COLORS = { Color.BLUE, Color.CYAN, Color.GRAY, Color.MAGENTA, Color.RED, Color.YELLOW };

    private final IViewerActivity base;

    private final Paint bgPaint;
    private final Paint gridPaint;
    private final ActionController<TouchManagerView> actions;
    private final DefaultGestureDetector detector;

    private TouchProfile profile;

    public TouchManagerView(final IViewerActivity base) {
        super(base.getContext());
        this.base = base;
        this.actions = new ActionController<TouchManagerView>(base.getActivity(), base.getActionController(), this);
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

        final TouchProfile tp = TouchManager.addProfile(TMV_PROFILE);
        {
            final Region r = tp.addRegion(0, 0, 100, 100);
            r.setAction(Touch.DoubleTap, true, R.id.actions_toggleTouchManagerView);
        }
    }

    @Override
    public void setVisibility(int visibility) {
        if (visibility == View.VISIBLE) {
            profile = TouchManager.pushProfile(TMV_PROFILE);
        } else if (profile != null) {
            TouchManager.popProfile();
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

        final Paint rPaint = new Paint();

        int cIndex = 0;
        final ListIterator<Region> regions = profile.regions();
        while (regions.hasNext()) {
            regions.next();
        }
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
        while (regions.hasNext()) {
            regions.next();
        }
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

    @Override
    public final boolean onTouchEvent(final MotionEvent ev) {
        try {
            Thread.sleep(16);
        } catch (final InterruptedException e) {
            Thread.interrupted();
        }

        if ((ev.getAction() & MotionEvent.ACTION_UP) == MotionEvent.ACTION_UP) {
            endPoint = new PointF(ev.getX(), ev.getY());
            current = getRegion(startPoint, endPoint);

            processRegion(current);

            startPoint = null;
            endPoint = null;
            current = null;
            invalidate();
        }

        return detector.onTouchEvent(ev);
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

    protected void processRegion(final Region current) {
        LCTX.d("processRegion(" + current + ")");
    }

    protected boolean processTap(final TouchManager.Touch type, final MotionEvent e) {
        final Integer actionId = TouchManager.getAction(type, e.getX(), e.getY(), getWidth(), getHeight());
        final ActionEx action = actionId != null ? actions.getOrCreateAction(actionId) : null;
        if (action != null) {
            LCTX.d("Touch action: " + action.name + ", " + action.getMethod().toString());
            action.run();
            return true;
        } else {
            LCTX.d("Touch action not found");
        }
        return false;
    }

    protected class GestureListener extends SimpleOnGestureListener {

        @Override
        public boolean onDoubleTap(final MotionEvent e) {
            return processTap(TouchManager.Touch.DoubleTap, e);
        }

        @Override
        public boolean onDown(final MotionEvent e) {
            LCTX.d("onDown(" + e + ")");
            startPoint = new PointF(e.getX(), e.getY());
            return true;
        }

        @Override
        public boolean onScroll(final MotionEvent e1, final MotionEvent e2, final float distanceX, final float distanceY) {
            final float x = e2.getX(), y = e2.getY();

            endPoint = new PointF(x, y);
            current = getRegion(startPoint, endPoint);
            LCTX.d("onScroll(" + x + ", " + y + "): " + current);
            invalidate();
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(final MotionEvent e) {
            return processTap(TouchManager.Touch.SingleTap, e);
        }

        @Override
        public void onLongPress(final MotionEvent e) {
            processTap(TouchManager.Touch.LongTap, e);
        }
    }

}
