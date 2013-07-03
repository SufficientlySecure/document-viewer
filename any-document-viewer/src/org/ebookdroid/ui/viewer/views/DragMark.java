package org.ebookdroid.ui.viewer.views;

import org.ebookdroid.R;
import org.ebookdroid.core.ViewState;
import org.ebookdroid.ui.viewer.IView;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import org.emdev.BaseDroidApp;
import org.emdev.ui.gl.BitmapTexture;
import org.emdev.ui.gl.GLCanvas;

public enum DragMark {

    DRAG(R.drawable.components_curler_drag, false),

    CURLER(R.drawable.components_curler_arrows, true);

    private static final Paint PAINT = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);

    private final int resId;

    private final boolean showAlways;

    private Bitmap dragBitmap;

    private BitmapTexture dragBitmapTx;

    private DragMark(final int resId, final boolean showAlways) {
        this.resId = resId;
        this.showAlways = showAlways;
    }

    public synchronized void draw(final Canvas canvas, final ViewState viewState) {
        if (dragBitmap == null || dragBitmap.isRecycled()) {
            dragBitmap = BitmapFactory.decodeResource(BaseDroidApp.context.getResources(), resId);
        }

        final Rect l = viewState.ctrl.getScrollLimits();
        if (showAlways || (l.width() + l.height() > 0)) {
            final IView view = viewState.ctrl.getView();
            final float x = view.getScrollX() - viewState.viewBase.x + view.getWidth() - dragBitmap.getWidth() - 1;
            final float y = view.getScrollY() - viewState.viewBase.y + view.getHeight() - dragBitmap.getHeight() - 1;

            canvas.drawBitmap(dragBitmap, x, y, PAINT);
        }
    }

    public synchronized void draw(final GLCanvas canvas, final ViewState viewState) {
        if (dragBitmap == null || dragBitmap.isRecycled()) {
            dragBitmap = BitmapFactory.decodeResource(BaseDroidApp.context.getResources(), resId);
            if (dragBitmapTx != null) {
                dragBitmapTx.recycle();
                dragBitmapTx = null;
            }
        }
        if (dragBitmapTx == null) {
            dragBitmapTx = new BitmapTexture(dragBitmap);
            dragBitmapTx.setOpaque(false);
        }

        final Rect l = viewState.ctrl.getScrollLimits();
        if (showAlways || (l.width() + l.height() > 0)) {
            final IView view = viewState.ctrl.getView();

            final int w = dragBitmap.getWidth();
            final int h = dragBitmap.getHeight();

            final float x = view.getScrollX() - viewState.viewBase.x + view.getWidth() - w - 1;
            final float y = view.getScrollY() - viewState.viewBase.y + view.getHeight() - h - 1;

            canvas.setClipRect(x, y, w, h);
            canvas.drawTexture(dragBitmapTx, (int) x, (int) y, dragBitmapTx.getWidth(), dragBitmapTx.getHeight());
            canvas.clearClipRect();
        }
    }

}
