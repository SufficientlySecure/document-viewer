package org.ebookdroid.ui.viewer.views;

import org.ebookdroid.EBookDroidApp;
import org.ebookdroid.R;
import org.ebookdroid.core.ViewState;
import org.ebookdroid.ui.viewer.IView;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import org.emdev.ui.gl.BitmapTexture;
import org.emdev.ui.gl.GLCanvas;

public class DragMark {

    private static final Paint PAINT = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);

    private static Bitmap dragBitmap;

    private static BitmapTexture dragBitmapTx;

    public synchronized static void draw(final Canvas canvas, final ViewState viewState) {
        if (dragBitmap == null) {
            dragBitmap = BitmapFactory.decodeResource(EBookDroidApp.context.getResources(), R.drawable.components_curler_drag);
        }

        final Rect l = viewState.ctrl.getScrollLimits();
        if (l.width() + l.height() > 0) {
            final IView view = viewState.ctrl.getView();
            final float x = view.getScrollX() - viewState.viewBase.x + view.getWidth() - dragBitmap.getWidth() - 1;
            final float y = view.getScrollY() - viewState.viewBase.y + view.getHeight() - dragBitmap.getHeight() - 1;

            canvas.drawBitmap(dragBitmap, x, y, PAINT);
        }
    }

    public synchronized static void draw(final GLCanvas canvas, final ViewState viewState) {
        if (dragBitmap == null || dragBitmap.isRecycled()) {
            dragBitmap = BitmapFactory.decodeResource(EBookDroidApp.context.getResources(), R.drawable.components_curler_drag);
            if (dragBitmapTx != null) {
                dragBitmapTx.recycle();
                dragBitmapTx = null;
            }
        }
        if (dragBitmapTx == null) {
            dragBitmapTx = new BitmapTexture(dragBitmap);
        }

        final Rect l = viewState.ctrl.getScrollLimits();
        if (l.width() + l.height() > 0) {
            final IView view = viewState.ctrl.getView();
            final float x = view.getScrollX() - viewState.viewBase.x + view.getWidth() - dragBitmap.getWidth() - 1;
            final float y = view.getScrollY() - viewState.viewBase.y + view.getHeight() - dragBitmap.getHeight() - 1;

            canvas.drawTexture(dragBitmapTx, (int)x, (int)y, dragBitmapTx.getWidth(), dragBitmapTx.getHeight());
        }
    }

}
