/*
 * Copyright (C) 2007-2011 Geometer Plus <contact@geometerplus.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.ebookdroid.core.curl;

import org.ebookdroid.common.bitmaps.BitmapManager;
import org.ebookdroid.common.bitmaps.BitmapRef;
import org.ebookdroid.common.settings.AppSettings;
import org.ebookdroid.core.EventDraw;
import org.ebookdroid.core.EventPool;
import org.ebookdroid.core.Page;
import org.ebookdroid.core.SinglePageController;
import org.ebookdroid.core.ViewState;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.FloatMath;

import org.emdev.utils.MatrixUtils;

/**
 * The Class SinglePageNaturalCurler.
 * 
 * Used code from FBReader.
 */
public class SinglePageNaturalCurler extends AbstractPageAnimator {

    final Path forePath = new Path();
    final Path edgePath = new Path();
    final Path quadPath = new Path();

    private final Paint backPaint = new Paint();
    private final Paint edgePaint = new Paint();

    public SinglePageNaturalCurler(final SinglePageController singlePageDocumentView) {
        super(PageAnimationType.CURLER_DYNAMIC, singlePageDocumentView);

        backPaint.setAntiAlias(false);
        backPaint.setAlpha(0x40);

        edgePaint.setAntiAlias(true);
        edgePaint.setStyle(Paint.Style.FILL);
        edgePaint.setShadowLayer(15, 0, 0, 0xC0000000);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.core.curl.AbstractPageAnimator#getInitialXForBackFlip(int)
     */
    @Override
    protected int getInitialXForBackFlip(final int width) {
        return width << 1;
    }

    /**
     * Do the page curl depending on the methods we are using
     */
    @Override
    protected void updateValues() {
        final int width = view.getWidth();
        final int height = view.getHeight();

        mA.x = width - mMovement.x + 0.1f;
        mA.y = height - mMovement.y + 0.1f;

    }

    @Override
    protected void drawInternal(final EventDraw event) {
        drawBackground(event);

        final Canvas canvas = event.canvas;

        final BitmapRef fgBitmap = BitmapManager.getBitmap("Foreground", canvas.getWidth(), canvas.getHeight(),
                Bitmap.Config.RGB_565);
        try {
            final Bitmap bmp = fgBitmap.getBitmap();
            bmp.eraseColor(Color.BLACK);
            drawForeground(EventPool.newEventDraw(event, new Canvas(bmp)));

            final int myWidth = canvas.getWidth();
            final int myHeight = canvas.getHeight();

            final int cornerX = myWidth;
            final int cornerY = myHeight;
            final int oppositeX = -cornerX;
            final int oppositeY = -cornerY;
            final int dX = Math.max(1, Math.abs((int) mA.x - cornerX));
            final int dY = Math.max(1, Math.abs((int) mA.y - cornerY));

            final int x1 = cornerX == 0 ? (dY * dY / dX + dX) / 2 : cornerX - (dY * dY / dX + dX) / 2;
            final int y1 = cornerY == 0 ? (dX * dX / dY + dY) / 2 : cornerY - (dX * dX / dY + dY) / 2;

            float sX, sY;
            {
                final float d1 = (int) mA.x - x1;
                final float d2 = (int) mA.y - cornerY;
                sX = FloatMath.sqrt(d1 * d1 + d2 * d2) / 2;
                if (cornerX == 0) {
                    sX = -sX;
                }
            }
            {
                final float d1 = (int) mA.x - cornerX;
                final float d2 = (int) mA.y - y1;
                sY = FloatMath.sqrt(d1 * d1 + d2 * d2) / 2;
                if (cornerY == 0) {
                    sY = -sY;
                }
            }

            forePath.rewind();
            forePath.moveTo((int) mA.x, (int) mA.y);
            forePath.lineTo(((int) mA.x + cornerX) / 2, ((int) mA.y + y1) / 2);
            forePath.quadTo(cornerX, y1, cornerX, y1 - sY);
            if (Math.abs(y1 - sY - cornerY) < myHeight) {
                forePath.lineTo(cornerX, oppositeY);
            }
            forePath.lineTo(oppositeX, oppositeY);
            if (Math.abs(x1 - sX - cornerX) < myWidth) {
                forePath.lineTo(oppositeX, cornerY);
            }
            forePath.lineTo(x1 - sX, cornerY);
            forePath.quadTo(x1, cornerY, ((int) mA.x + x1) / 2, ((int) mA.y + cornerY) / 2);

            quadPath.moveTo(x1 - sX, cornerY);
            quadPath.quadTo(x1, cornerY, ((int) mA.x + x1) / 2, ((int) mA.y + cornerY) / 2);
            canvas.drawPath(quadPath, edgePaint);
            quadPath.rewind();
            quadPath.moveTo(((int) mA.x + cornerX) / 2, ((int) mA.y + y1) / 2);
            quadPath.quadTo(cornerX, y1, cornerX, y1 - sY);
            canvas.drawPath(quadPath, edgePaint);
            quadPath.rewind();

            canvas.save();
            canvas.clipPath(forePath);
            canvas.drawBitmap(bmp, 0, 0, null);
            canvas.restore();

            edgePaint.setColor(getAverageColor(bmp));

            edgePath.rewind();
            edgePath.moveTo((int) mA.x, (int) mA.y);
            edgePath.lineTo(((int) mA.x + cornerX) / 2, ((int) mA.y + y1) / 2);
            edgePath.quadTo(((int) mA.x + 3 * cornerX) / 4, ((int) mA.y + 3 * y1) / 4, ((int) mA.x + 7 * cornerX) / 8,
                    ((int) mA.y + 7 * y1 - 2 * sY) / 8);
            edgePath.lineTo(((int) mA.x + 7 * x1 - 2 * sX) / 8, ((int) mA.y + 7 * cornerY) / 8);
            edgePath.quadTo(((int) mA.x + 3 * x1) / 4, ((int) mA.y + 3 * cornerY) / 4, ((int) mA.x + x1) / 2,
                    ((int) mA.y + cornerY) / 2);

            canvas.drawPath(edgePath, edgePaint);

            canvas.save();
            canvas.clipPath(edgePath);
            final Matrix m = MatrixUtils.get();
            m.postScale(1, -1);
            m.postTranslate((int) mA.x - cornerX, (int) mA.y + cornerY);
            final float angle;
            if (cornerY == 0) {
                angle = -180 / 3.1416f * (float) Math.atan2((int) mA.x - cornerX, (int) mA.y - y1);
            } else {
                angle = 180 - 180 / 3.1416f * (float) Math.atan2((int) mA.x - cornerX, (int) mA.y - y1);
            }
            m.postRotate(angle, (int) mA.x, (int) mA.y);
            canvas.drawBitmap(bmp, m, backPaint);
            canvas.restore();
        } finally {
            BitmapManager.release(fgBitmap);
        }

    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.core.curl.AbstractPageAnimator#getLeftBound()
     */
    @Override
    protected float getLeftBound() {
        return 1 - view.getWidth();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.core.curl.AbstractPageAnimator#resetClipEdge()
     */
    @Override
    protected void resetClipEdge() {
        mMovement.x = 0;
        mMovement.y = 0;
        mOldMovement.x = 0;
        mOldMovement.y = 0;

        // Now set the points
        mA = new Vector2D(0, 0);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.core.curl.AbstractPageAnimator#fixMovement(org.ebookdroid.core.curl.Vector2D, boolean)
     */
    @Override
    protected Vector2D fixMovement(final Vector2D point, final boolean bMaintainMoveDir) {
        return point;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.core.curl.AbstractPageAnimator#drawBackground(org.ebookdroid.core.EventDraw)
     */
    @Override
    protected void drawBackground(final EventDraw event) {
        final Canvas canvas = event.canvas;
        final ViewState viewState = event.viewState;

        Page page = event.viewState.model.getPageObject(backIndex);
        if (page == null) {
            page = event.viewState.model.getCurrentPageObject();
        }
        if (page != null) {
            canvas.save();
            canvas.clipRect(viewState.getBounds(page));
            event.process(page);
            canvas.restore();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.core.curl.AbstractPageAnimator#drawForeground(org.ebookdroid.core.EventDraw)
     */
    @Override
    protected void drawForeground(final EventDraw event) {
        final Canvas canvas = event.canvas;
        final ViewState viewState = event.viewState;

        Page page = event.viewState.model.getPageObject(foreIndex);
        if (page == null) {
            page = event.viewState.model.getCurrentPageObject();
        }
        if (page != null) {
            canvas.save();
            canvas.clipRect(viewState.getBounds(page));
            event.process(page);
            canvas.restore();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.core.curl.AbstractPageAnimator#drawExtraObjects(org.ebookdroid.core.EventDraw)
     */
    @Override
    protected void drawExtraObjects(final EventDraw event) {

        if (AppSettings.current().showAnimIcon) {
            final Canvas canvas = event.canvas;
            canvas.drawBitmap(arrowsBitmap, view.getWidth() - arrowsBitmap.getWidth(),
                    view.getHeight() - arrowsBitmap.getHeight(), PAINT);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.core.curl.AbstractPageAnimator#onFirstDrawEvent(android.graphics.Canvas,
     *      org.ebookdroid.core.ViewState)
     */
    @Override
    protected void onFirstDrawEvent(final Canvas canvas, final ViewState viewState) {
        resetClipEdge();

        lock.writeLock().lock();
        try {
            updateValues();
        } finally {
            lock.writeLock().unlock();
        }
    }

    private static int getAverageColor(final Bitmap bitmap) {
        final int w = Math.min(bitmap.getWidth(), 7);
        final int h = Math.min(bitmap.getHeight(), 7);
        long r = 0, g = 0, b = 0;
        for (int i = 0; i < w; ++i) {
            for (int j = 0; j < h; ++j) {
                final int color = bitmap.getPixel(i, j);
                r += color & 0xFF0000;
                g += color & 0xFF00;
                b += color & 0xFF;
            }
        }
        r /= w * h;
        g /= w * h;
        b /= w * h;
        r >>= 16;
        g >>= 8;
        return Color.rgb((int) (r & 0xFF), (int) (g & 0xFF), (int) (b & 0xFF));
    }

}
