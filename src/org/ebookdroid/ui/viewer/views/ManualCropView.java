package org.ebookdroid.ui.viewer.views;

import org.ebookdroid.R;
import org.ebookdroid.common.bitmaps.BitmapManager;
import org.ebookdroid.common.bitmaps.Bitmaps;
import org.ebookdroid.common.settings.types.PageAlign;
import org.ebookdroid.core.AbstractViewController;
import org.ebookdroid.core.EventPool;
import org.ebookdroid.core.Page;
import org.ebookdroid.core.ViewState;
import org.ebookdroid.ui.viewer.IActivityController;
import org.ebookdroid.ui.viewer.IViewController.InvalidateSizeReason;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff.Mode;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;

import java.util.ArrayList;
import java.util.List;

import org.emdev.utils.MathUtils;

public class ManualCropView extends View {

    private static final Paint PAINT = new Paint();

    private final IActivityController base;

    private final GestureDetector gestureDetector;

    private PointF topLeft = new PointF(0.1f, 0.1f);
    private PointF bottomRight = new PointF(0.9f, 0.9f);
    private PointF currentPoint = null;

    private Page page;

    public ManualCropView(final IActivityController base) {
        super(base.getContext());
        this.base = base;

        super.setVisibility(View.GONE);
        PAINT.setColor(Color.CYAN);

        setLayoutParams(new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        setFocusable(true);
        setFocusableInTouchMode(true);

        gestureDetector = new GestureDetector(getContext(), new GestureListener());
    }

    public void initControls() {
        page = base.getDocumentModel().getCurrentPageObject();
        if (page == null) {
            return;
        }

        base.getZoomModel().setZoom(1.0f, true);

        base.getBookSettings().pageAlign = PageAlign.AUTO;

        final RectF oldCb = page.nodes.root.getCropping();

        // System.out.println("ManualCropView.initControls(): " + oldCb);

        if (base.getBookSettings().cropPages && oldCb != null) {

            final List<Bitmaps> bitmapsToRecycle = new ArrayList<Bitmaps>();
            page.nodes.recycleAll(bitmapsToRecycle, true);
            BitmapManager.release(bitmapsToRecycle);

            page.nodes.root.setManualCropping(page.type.getInitialRect(), false);
            page.setAspectRatio(page.cpi);

            EventPool.newEventReset((AbstractViewController) base.getDocumentController(),
                    InvalidateSizeReason.PAGE_LOADED, false).process();
        }

        if (oldCb == null) {
            topLeft.set(0.1f, 0.1f);
            bottomRight.set(0.9f, 0.9f);
        } else {
            final RectF ir = new RectF(page.type.getInitialRect());
            final float irw = ir.width();

            final float left = (oldCb.left - ir.left) / irw;
            final float top = (oldCb.top);
            final float right = (oldCb.right - ir.left) / irw;
            final float bottom = (oldCb.bottom);

            topLeft.set(left, top);
            bottomRight.set(right, bottom);
        }

        // System.out.println("ManualCropView.initControls(): " + str(topLeft) + " " + str(bottomRight));
    }

    public void applyCropping() {
        if (page == null) {
            ViewEffects.toggleControls(this);
            return;
        }

        final RectF r = new RectF(Math.min(topLeft.x, bottomRight.x), Math.min(topLeft.y, bottomRight.y), Math.max(
                topLeft.x, bottomRight.x), Math.max(topLeft.y, bottomRight.y));

        // System.out.println("ManualCropView.applyCropping(): vpc = " + r);

        final RectF cb = new RectF(page.type.getInitialRect());
        final float irw = cb.width();
        cb.left += r.left * irw;
        cb.right -= (1 - r.right) * irw;
        cb.top += r.top;
        cb.bottom -= (1 - r.bottom);

        // System.out.println("ManualCropView.applyCropping(): dpc = " + cb);

        final List<Bitmaps> bitmapsToRecycle = new ArrayList<Bitmaps>();
        page.nodes.recycleAll(bitmapsToRecycle, true);
        BitmapManager.release(bitmapsToRecycle);

        page.nodes.root.setManualCropping(cb, true);

        EventPool.newEventReset((AbstractViewController) base.getDocumentController(),
                InvalidateSizeReason.PAGE_LOADED, false).process();

        page = null;
        ViewEffects.toggleControls(this);
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        if (page == null) {
            return;
        }

        final RectF r = getActualRect();

        canvas.drawColor(0x7F000000);

        canvas.save();

        canvas.clipRect(r.left, r.top, r.right, r.bottom);
        canvas.drawColor(0x00FFFFFF, Mode.CLEAR);
        canvas.restore();

        final Drawable d = base.getContext().getResources().getDrawable(R.drawable.components_cropper_circle);
        d.setBounds((int) (r.left - 25), (int) (r.top - 25), (int) (r.left + 25), (int) (r.top + 25));
        d.draw(canvas);

        d.setBounds((int) (r.right - 25), (int) (r.bottom - 25), (int) (r.right + 25), (int) (r.bottom + 25));
        d.draw(canvas);

        canvas.drawLine(0, r.top, getWidth(), r.top, PAINT);
        canvas.drawLine(0, r.bottom, getWidth(), r.bottom, PAINT);
        canvas.drawLine(r.left, 0, r.left, getHeight(), PAINT);
        canvas.drawLine(r.right, 0, r.right, getHeight(), PAINT);

    }

    private RectF getActualRect() {
        final RectF pageBounds = getPageRect();
        final float pageWidth = pageBounds.width();
        final float pageHeight = pageBounds.height();

        final float left = pageBounds.left + topLeft.x * pageWidth;
        final float top = pageBounds.top + topLeft.y * pageHeight;
        final float right = pageBounds.left + bottomRight.x * pageWidth;
        final float bottom = pageBounds.top + bottomRight.y * pageHeight;

        return new RectF(Math.min(left, right), Math.min(top, bottom), Math.max(right, left), Math.max(bottom, top));
    }

    private RectF getPageRect() {
        final ViewState viewState = new ViewState(base.getDocumentController());
        final RectF pageBounds = viewState.getBounds(page);
        pageBounds.offset(-viewState.viewBase.x, -viewState.viewBase.y);
        return pageBounds;
    }

    static String str(final PointF p) {
        return "(" + p.x + ", " + p.y + ")";
    }

    @Override
    public boolean onTouchEvent(final MotionEvent ev) {
        try {
            Thread.sleep(16);
        } catch (final InterruptedException e) {
            Thread.interrupted();
        }

        if ((ev.getAction() & MotionEvent.ACTION_UP) == MotionEvent.ACTION_UP) {
            currentPoint = null;
        }

        return gestureDetector.onTouchEvent(ev);
    }

    protected class GestureListener extends SimpleOnGestureListener {

        private static final int SPOT_SIZE = 25;

        @Override
        public boolean onDown(final MotionEvent e) {
            if (page == null) {
                return true;
            }

            final float x = e.getX();
            final float y = e.getY();
            final RectF r = getActualRect();

            if ((Math.abs(x - r.left) < SPOT_SIZE) && (Math.abs(y - r.top) < SPOT_SIZE)) {
                currentPoint = topLeft;
                return true;
            }

            if ((Math.abs(x - r.right) < SPOT_SIZE) && (Math.abs(y - r.bottom) < SPOT_SIZE)) {
                currentPoint = bottomRight;
                return true;
            }

            currentPoint = null;
            return true;
        }

        @Override
        public boolean onScroll(final MotionEvent e1, final MotionEvent e2, final float distanceX, final float distanceY) {
            if (page == null || currentPoint == null) {
                return true;
            }

            final float x = e2.getX();
            final float y = e2.getY();
            final RectF r = getPageRect();

            currentPoint.x = MathUtils.adjust((x - r.left) / r.width(), 0f, 1f);
            currentPoint.y = MathUtils.adjust((y - r.top) / r.height(), 0f, 1f);

            invalidate();

            return true;
        }

        @Override
        public boolean onDoubleTap(final MotionEvent e) {
            applyCropping();
            return true;
        }
    }
}
