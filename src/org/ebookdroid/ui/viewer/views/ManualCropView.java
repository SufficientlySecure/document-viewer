package org.ebookdroid.ui.viewer.views;

import org.ebookdroid.R;
import org.ebookdroid.common.settings.types.PageAlign;
import org.ebookdroid.core.EventCrop;
import org.ebookdroid.core.Page;
import org.ebookdroid.core.ViewState;
import org.ebookdroid.ui.viewer.IActivityController;

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

import org.emdev.ui.actions.ActionController;
import org.emdev.ui.actions.ActionDialogBuilder;
import org.emdev.ui.actions.ActionEx;
import org.emdev.ui.actions.ActionMethod;
import org.emdev.ui.actions.IActionController;
import org.emdev.utils.MathUtils;

public class ManualCropView extends View {

    private static final Paint PAINT = new Paint();

    private final IActivityController base;

    private final GestureDetector gestureDetector;

    private final ActionController<ManualCropView> controller = new ActionController<ManualCropView>(this);

    private final PointF topLeft = new PointF(0.1f, 0.1f);
    private final PointF bottomRight = new PointF(0.9f, 0.9f);
    private PointF currentPoint = null;

    private Page page;

    private RectF result;

    public ManualCropView(final IActivityController base) {
        super(base.getContext());
        this.base = base;

        super.setVisibility(View.GONE);
        PAINT.setColor(Color.CYAN);

        setLayoutParams(new ViewGroup.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
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

        if (page.shouldCrop() && oldCb != null) {
            new EventCrop(base.getDocumentController()).add(page).process().release();
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
    }

    public void applyCropping() {
        if (page == null) {
            ViewEffects.toggleControls(this);
            return;
        }

        result = new RectF(Math.min(topLeft.x, bottomRight.x), Math.min(topLeft.y, bottomRight.y), Math.max(topLeft.x,
                bottomRight.x), Math.max(topLeft.y, bottomRight.y));

        final ActionDialogBuilder builder = new ActionDialogBuilder(getContext(), controller);
        builder.setTitle(R.string.manual_cropping_title);
        builder.setItems(R.array.list_crop_actions, controller.getOrCreateAction(R.id.actions_applyCrop));
        builder.setNegativeButton(R.string.manual_cropping_back);
        builder.show();
    }

    @ActionMethod(ids = R.id.actions_applyCrop)
    public void onApply(final ActionEx action) {
        final Integer index = action.getParameter(IActionController.DIALOG_ITEM_PROPERTY);
        if (index == null) {
            return;
        }

        ViewEffects.toggleControls(this);

        EventCrop event = null;

        switch (index.intValue()) {
            case 0:
                // Apply to current only
                event = new EventCrop(base.getDocumentController(), result, true);
                event.add(page).process().release();
                return;

            case 1:
                // Apply to even(odd)
                event = new EventCrop(base.getDocumentController(), result, true);
                event.addEvenOdd(page, true).process().release();
                return;

            case 2:
                // Apply to even(odd) symmetrically
                event = new EventCrop(base.getDocumentController(), result, true);
                event.addEvenOdd(page, true).process().release();

                final RectF symm = new RectF();
                symm.left = 1 - result.right;
                symm.top = result.top;
                symm.right = 1 - result.left;
                symm.bottom = result.bottom;

                event = new EventCrop(base.getDocumentController(), symm, true);
                event.addEvenOdd(page, false).process().release();

                return;

            case 3:
                // Apply to all
                event = new EventCrop(base.getDocumentController(), result, true);
                event.addAll().process().release();
                return;
            case 4:
                // Remove manual cropping
                event = new EventCrop(base.getDocumentController(), null, true);
                event.add(page).process().release();
                return;
            case 5:
                // Remove all manual cropping
                event = new EventCrop(base.getDocumentController(), null, true);
                event.addAll().process().release();
                return;
        }
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
        final ViewState viewState = ViewState.get(base.getDocumentController());
        final RectF pageBounds = viewState.getBounds(page);
        pageBounds.offset(-viewState.viewBase.x, -viewState.viewBase.y);
        viewState.release();
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
