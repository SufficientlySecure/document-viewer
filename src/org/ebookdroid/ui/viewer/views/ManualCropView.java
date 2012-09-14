package org.ebookdroid.ui.viewer.views;

import org.ebookdroid.R;
import org.ebookdroid.common.bitmaps.BitmapManager;
import org.ebookdroid.common.bitmaps.Bitmaps;
import org.ebookdroid.core.AbstractViewController;
import org.ebookdroid.core.EventPool;
import org.ebookdroid.core.Page;
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

public class ManualCropView extends View {

    private final IActivityController base;
    PointF topLeft = new PointF(0.1f, 0.1f);
    PointF bottomRight = new PointF(0.9f, 0.9f);

    PointF currentPoint = null;

    private boolean inTouch = false;

    private final GestureDetector gestureDetector;

    Paint PAINT = new Paint();

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
        final Page page = base.getDocumentModel().getCurrentPageObject();
        if (page != null) {

            base.getZoomModel().setZoom(1.0f, true);

            RectF oldCb = page.nodes.root.croppedBounds;
            if (oldCb != null) {

                final List<Bitmaps> bitmapsToRecycle = new ArrayList<Bitmaps>();
                page.nodes.recycleAll(bitmapsToRecycle, true);
                BitmapManager.release(bitmapsToRecycle);

                page.nodes.root.croppedBounds = page.type.getInitialRect();
                page.setAspectRatio(page.cpi);

                EventPool.newEventReset((AbstractViewController) base.getDocumentController(), InvalidateSizeReason.PAGE_LOADED, false).process();
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(0x7F000000);

        canvas.save();
        canvas.clipRect(topLeft.x * getWidth(), topLeft.y * getHeight(), bottomRight.x * getWidth(), bottomRight.y
                * getHeight());
        canvas.drawColor(0x00FFFFFF, Mode.CLEAR);
        canvas.restore();

        Drawable d = base.getContext().getResources().getDrawable(R.drawable.circle);
        d.setBounds((int) (topLeft.x * getWidth() - 25), (int) (topLeft.y * getHeight() - 25), (int) (topLeft.x
                * getWidth() + 25), (int) (topLeft.y * getHeight() + 25));
        d.draw(canvas);

        d.setBounds((int) (bottomRight.x * getWidth() - 25), (int) (bottomRight.y * getHeight() - 25),
                (int) (bottomRight.x * getWidth() + 25), (int) (bottomRight.y * getHeight() + 25));
        d.draw(canvas);

        canvas.drawLine(0, topLeft.y * getHeight(), getWidth(), topLeft.y * getHeight(), PAINT);
        canvas.drawLine(0, bottomRight.y * getHeight(), getWidth(), bottomRight.y * getHeight(), PAINT);
        canvas.drawLine(topLeft.x * getWidth(), 0, topLeft.x * getWidth(), getHeight(), PAINT);
        canvas.drawLine(bottomRight.x * getWidth(), 0, bottomRight.x * getWidth(), getHeight(), PAINT);

    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        try {
            Thread.sleep(16);
        } catch (final InterruptedException e) {
            Thread.interrupted();
        }

        if ((ev.getAction() & MotionEvent.ACTION_UP) == MotionEvent.ACTION_UP) {
            inTouch = false;
            currentPoint = null;
        }

        return gestureDetector.onTouchEvent(ev);
    }

    protected class GestureListener extends SimpleOnGestureListener {

        @Override
        public boolean onDown(MotionEvent e) {
            if ((Math.abs(e.getX() - ManualCropView.this.topLeft.x * ManualCropView.this.getWidth()) < 10)
                    && (Math.abs(e.getY() - ManualCropView.this.topLeft.y * ManualCropView.this.getHeight()) < 10)) {
                ManualCropView.this.currentPoint = topLeft;
            }
            if ((Math.abs(e.getX() - ManualCropView.this.bottomRight.x * ManualCropView.this.getWidth()) < 10)
                    && (Math.abs(e.getY() - ManualCropView.this.bottomRight.y * ManualCropView.this.getHeight()) < 10)) {
                ManualCropView.this.currentPoint = bottomRight;
            }
            inTouch = true;
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (ManualCropView.this.currentPoint != null) {
                ManualCropView.this.currentPoint.x = e2.getX() / ManualCropView.this.getWidth();
                ManualCropView.this.currentPoint.y = e2.getY() / ManualCropView.this.getHeight();
                ManualCropView.this.invalidate();
                return true;
            }
            return false;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            ManualCropView.this.applyCropping();
            return true;
        }
    }

    public void applyCropping() {
        System.out.println("ManualCropView.applyCropping()");
        ViewEffects.toggleControls(this);
    }
}
