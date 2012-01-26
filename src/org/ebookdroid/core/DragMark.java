package org.ebookdroid.core;

import org.ebookdroid.EBookDroidApp;
import org.ebookdroid.R;
import org.ebookdroid.core.ViewState;

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

        Rect l = viewState.ctrl.getScrollLimits();
        if (l.width() + l.height() > 0) {
            final Paint paint = new Paint();
            paint.setFilterBitmap(true);
            paint.setAntiAlias(true);
            paint.setDither(true);

            float x = viewState.view.getScrollX() + viewState.view.getWidth() - dragBitmap.getWidth() - 1;
            float y = viewState.view.getScrollY() + viewState.view.getHeight() - dragBitmap.getHeight() - 1;

            canvas.drawBitmap(dragBitmap, x, y, paint);
        }
    }
}
