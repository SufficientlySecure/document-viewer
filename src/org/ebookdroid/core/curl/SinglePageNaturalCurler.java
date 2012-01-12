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

import org.ebookdroid.core.Page;
import org.ebookdroid.core.SinglePageDocumentView;
import org.ebookdroid.core.ViewState;
import org.ebookdroid.core.settings.SettingsManager;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.FloatMath;

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

    public SinglePageNaturalCurler(final SinglePageDocumentView singlePageDocumentView) {
        super(PageAnimationType.CURLER_DYNAMIC, singlePageDocumentView);

        backPaint.setAntiAlias(false);
        backPaint.setAlpha(0x40);

        edgePaint.setAntiAlias(true);
        edgePaint.setStyle(Paint.Style.FILL);
        edgePaint.setShadowLayer(15, 0, 0, 0xC0000000);
    }

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

    protected void drawInternal(Canvas canvas, ViewState viewState) {
        drawBackground(canvas, viewState);

        final Bitmap fgBitmap = Bitmap.createBitmap(canvas.getWidth(), canvas.getHeight(), Bitmap.Config.RGB_565);
        drawForeground(new Canvas(fgBitmap), viewState);

        int myWidth = canvas.getWidth();
        int myHeight = canvas.getHeight();

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
            float d1 = (int) mA.x - x1;
            float d2 = (int) mA.y - cornerY;
            sX = FloatMath.sqrt(d1 * d1 + d2 * d2) / 2;
            if (cornerX == 0) {
                sX = -sX;
            }
        }
        {
            float d1 = (int) mA.x - cornerX;
            float d2 = (int) mA.y - y1;
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
        canvas.drawBitmap(fgBitmap, 0, 0, null);
        canvas.restore();

        edgePaint.setColor(getAverageColor(fgBitmap));

        edgePath.rewind();
        edgePath.moveTo((int) mA.x, (int) mA.y);
        edgePath.lineTo(((int) mA.x + cornerX) / 2, ((int) mA.y + y1) / 2);
        edgePath.quadTo(((int) mA.x + 3 * cornerX) / 4, ((int) mA.y + 3 * y1) / 4, ((int) mA.x + 7 * cornerX) / 8, ((int) mA.y + 7 * y1 - 2 * sY) / 8);
        edgePath.lineTo(((int) mA.x + 7 * x1 - 2 * sX) / 8, ((int) mA.y + 7 * cornerY) / 8);
        edgePath.quadTo(((int) mA.x + 3 * x1) / 4, ((int) mA.y + 3 * cornerY) / 4, ((int) mA.x + x1) / 2, ((int) mA.y + cornerY) / 2);

        canvas.drawPath(edgePath, edgePaint);
//        canvas.save();
//        canvas.clipPath(edgePath);
//        final Matrix m = new Matrix();
//        m.postScale(1, -1);
//        m.postTranslate((int) mA.x - cornerX, (int) mA.y + cornerY);
//        final float angle;
//        if (cornerY == 0) {
//            angle = -180 / 3.1416f * (float) Math.atan2((int) mA.x - cornerX, (int) mA.y - y1);
//        } else {
//            angle = 180 - 180 / 3.1416f * (float) Math.atan2((int) mA.x - cornerX, (int) mA.y - y1);
//        }
//        m.postRotate(angle, (int) mA.x, (int) mA.y);
//        canvas.drawBitmap(fgBitmap, m, backPaint);
//        canvas.restore();
    }

    @Override
    protected float getLeftBound() {
        return 1 - view.getWidth();
    }

    @Override
    protected void resetClipEdge() {
        mMovement.x = 0;
        mMovement.y = 0;
        mOldMovement.x = 0;
        mOldMovement.y = 0;

        // Now set the points
        mA = new Vector2D(0, 0);
    }

    @Override
    protected Vector2D fixMovement(Vector2D point, boolean bMaintainMoveDir) {
        return point;
    }

    @Override
    protected void drawBackground(Canvas canvas, ViewState viewState) {
        Page page = view.getBase().getDocumentModel().getPageObject(backIndex);
        if (page == null) {
            page = view.getBase().getDocumentModel().getCurrentPageObject();
        }
        if (page != null) {
            canvas.save();
            canvas.clipRect(viewState.getBounds(page));
            page.draw(canvas, viewState, true);
            canvas.restore();
        }
    }

    @Override
    protected void drawForeground(Canvas canvas, ViewState viewState) {
        Page page = view.getBase().getDocumentModel().getPageObject(foreIndex);
        if (page == null) {
            page = view.getBase().getDocumentModel().getCurrentPageObject();
        }
        if (page != null) {
            canvas.save();
            canvas.clipRect(viewState.getBounds(page));
            page.draw(canvas, viewState, true);
            canvas.restore();
        }
    }

    @Override
    protected void drawExtraObjects(Canvas canvas, ViewState viewState) {
        final Paint paint = new Paint();
        paint.setFilterBitmap(true);
        paint.setAntiAlias(true);
        paint.setDither(true);

        if (SettingsManager.getAppSettings().getShowAnimIcon()) {
            canvas.drawBitmap(arrowsBitmap, view.getWidth() - arrowsBitmap.getWidth(),
                    view.getHeight() - arrowsBitmap.getHeight(), paint);
        }
    }

    @Override
    protected void onFirstDrawEvent(Canvas canvas, ViewState viewState) {
        resetClipEdge();

        lock.writeLock().lock();
        try {
            updateValues();
        } finally {
            lock.writeLock().unlock();
        }
    }

    private static int getAverageColor(Bitmap bitmap) {
        final int w = Math.min(bitmap.getWidth(), 7);
        final int h = Math.min(bitmap.getHeight(), 7);
        long r = 0, g = 0, b = 0;
        for (int i = 0; i < w; ++i) {
            for (int j = 0; j < h; ++j) {
                int color = bitmap.getPixel(i, j);
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
