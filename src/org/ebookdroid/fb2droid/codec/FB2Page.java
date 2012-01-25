package org.ebookdroid.fb2droid.codec;

import org.ebookdroid.core.bitmaps.BitmapManager;
import org.ebookdroid.core.bitmaps.BitmapRef;
import org.ebookdroid.core.codec.CodecPage;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;

import java.util.ArrayList;

public class FB2Page implements CodecPage {

    public static final int PAGE_WIDTH = 800;

    public static final int PAGE_HEIGHT = 1176;

    public static final int MARGIN_X = 20;

    public static final int MARGIN_Y = 20;

    static final RectF PAGE_RECT = new RectF(0, 0, PAGE_WIDTH, PAGE_HEIGHT);

    final ArrayList<FB2Line> lines = new ArrayList<FB2Line>(PAGE_HEIGHT / RenderingStyle.TEXT_SIZE);
    final ArrayList<FB2Line> noteLines = new ArrayList<FB2Line>(PAGE_HEIGHT / RenderingStyle.FOOTNOTE_SIZE);

    boolean committed = false;
    int contentHeight = 0;

    @Override
    public int getHeight() {
        return PAGE_HEIGHT;
    }

    @Override
    public int getWidth() {
        return PAGE_WIDTH;
    }

    @Override
    public void recycle() {
    }

    @Override
    public BitmapRef renderBitmap(final int width, final int height, final RectF pageSliceBounds) {
        final Matrix matrix = new Matrix();
        matrix.postScale((float) width / PAGE_WIDTH, (float) height / PAGE_HEIGHT);
        matrix.postTranslate(-pageSliceBounds.left * width, -pageSliceBounds.top * height);
        matrix.postScale(1 / pageSliceBounds.width(), 1 / pageSliceBounds.height());

        final BitmapRef bmp = BitmapManager.getBitmap("FB2 page", width, height, Bitmap.Config.RGB_565);

        final Canvas c = new Canvas(bmp.getBitmap());
        c.setMatrix(matrix);

        final Paint paint1 = new Paint();
        paint1.setColor(Color.WHITE);
        c.drawRect(PAGE_RECT, paint1);

        RectF bounds = new RectF(pageSliceBounds.left * PAGE_WIDTH, pageSliceBounds.top * PAGE_HEIGHT,
                pageSliceBounds.right * PAGE_WIDTH, pageSliceBounds.bottom * PAGE_HEIGHT);

        int y = MARGIN_Y;
        for (final FB2Line line : lines) {
            int top = y;
            int bottom = y + line.getHeight();
            if (bounds.top < bottom && top < bounds.bottom) {
                line.render(c, y, bounds.left, bounds.right);
            }
            y = bottom;
        }
        for (final FB2Line line : noteLines) {
            int top = y;
            int bottom = y + line.getHeight();
            if (bounds.top < bottom && top < bounds.bottom) {
                line.render(c, y, bounds.left, bounds.right);
            }
            y = bottom;
        }

        return bmp;
    }

    public void appendLine(final FB2Line line) {
        if (committed) {
            return;
        }
        lines.add(line);
        contentHeight += line.getHeight();
    }

    public static FB2Page getLastPage(final ArrayList<FB2Page> pages) {
        if (pages.size() == 0) {
            pages.add(new FB2Page());
        }
        FB2Page fb2Page = pages.get(pages.size() - 1);
        if (fb2Page.committed) {
            fb2Page = new FB2Page();
            pages.add(fb2Page);
        }
        return fb2Page;
    }

    public void appendNoteLine(final FB2Line line) {
        noteLines.add(line);
        contentHeight += line.getHeight();
    }

    public void commit() {
        if (committed) {
            return;
        }
        final int h = FB2Page.PAGE_HEIGHT - contentHeight - 2 * FB2Page.MARGIN_Y;
        if (h > 0) {
            lines.add(new FB2Line().append(new FB2LineFixedWhiteSpace(0, h)));
            contentHeight += h;
        }
        for (final FB2Line line : noteLines) {
            lines.add(line);
        }
        noteLines.clear();
        committed = true;
    }

    @Override
    public boolean isRecycled() {
        return false;
    }
}
