package org.ebookdroid.core;

import org.ebookdroid.EBookDroidApp;
import org.ebookdroid.R;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

public class DragMark {

    private static Bitmap dragBitmap;

    public synchronized static void draw(final Canvas canvas, final ViewState viewState) {
        if (dragBitmap == null) {
            dragBitmap = BitmapFactory.decodeResource(EBookDroidApp.context.getResources(), R.drawable.drag);
        }

        final Rect l = viewState.ctrl.getScrollLimits();
        if (l.width() + l.height() > 0) {
            final Paint paint = new Paint();
            paint.setFilterBitmap(true);
            paint.setAntiAlias(true);
            paint.setDither(true);

            final IDocumentView view = viewState.view;
            final float x = view.getScrollX() + view.getWidth() - dragBitmap.getWidth() - 1;
            final float y = view.getScrollY() + view.getHeight() - dragBitmap.getHeight() - 1;

            canvas.drawBitmap(dragBitmap, x, y, paint);
        }
    }
}
