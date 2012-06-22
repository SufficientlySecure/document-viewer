package org.ebookdroid.droids.fb2.codec;

import org.ebookdroid.common.bitmaps.BitmapManager;
import org.ebookdroid.common.bitmaps.BitmapRef;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.core.codec.CodecPageInfo;
import org.ebookdroid.core.codec.PageLink;
import org.ebookdroid.core.codec.PageTextBox;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.emdev.utils.MatrixUtils;
import org.emdev.utils.textmarkup.FontStyle;
import org.emdev.utils.textmarkup.JustificationMode;
import org.emdev.utils.textmarkup.line.AbstractLineElement;
import org.emdev.utils.textmarkup.line.Line;
import org.emdev.utils.textmarkup.line.LineFixedWhiteSpace;
import org.emdev.utils.textmarkup.line.LineWhiteSpace;
import org.emdev.utils.textmarkup.line.TextElement;

public class FB2Page implements CodecPage {

    public static final int PAGE_WIDTH = 800;

    public static final int PAGE_HEIGHT = 1280;

    public static final CodecPageInfo CPI = new CodecPageInfo(PAGE_WIDTH, PAGE_HEIGHT);

    public static final int MARGIN_X = 20;

    public static final int MARGIN_Y = 20;

    static final RectF PAGE_RECT = new RectF(0, 0, PAGE_WIDTH, PAGE_HEIGHT);

    final ArrayList<Line> lines = new ArrayList<Line>(PAGE_HEIGHT / FontStyle.TEXT.getFontSize());
    final ArrayList<Line> noteLines = new ArrayList<Line>(PAGE_HEIGHT / FontStyle.FOOTNOTE.getFontSize());

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
    public List<PageLink> getPageLinks() {
        return Collections.emptyList();
    }

    @Override
    public List<PageTextBox> getPageText() {
        return Collections.emptyList();
    }

    @Override
    public List<? extends RectF> searchText(String pattern) {
        final List<RectF> rects = new ArrayList<RectF>();

        final char[] charArray = pattern.toCharArray();
        final float y = searchText(lines, charArray, rects, FB2Page.MARGIN_Y);

        searchText(noteLines, charArray, rects, y);

        return rects;
    }

    private float searchText(final ArrayList<Line> lines, final char[] pattern, final List<RectF> rects, float y) {
        for (int i = 0, n = lines.size(); i < n; i++) {
            final Line line = lines.get(i);
            final float bottom = y + line.getHeight();
            line.ensureJustification();
            float x = FB2Page.MARGIN_X;
            for (int i1 = 0, n1 = line.elements.size(); i1 < n1; i1++) {
                final AbstractLineElement e = line.elements.get(i1);
                final float w = e.width + (e instanceof LineWhiteSpace ? line.spaceWidth : 0);
                if (e instanceof TextElement) {
                    final TextElement textElement = (TextElement) e;
                    if (textElement.indexOf(pattern) != -1) {
                        Rect bounds = new Rect();
                        textElement.getTextBounds(bounds);


                        rects.add(new RectF((x - 3) / FB2Page.PAGE_WIDTH, (bottom + bounds.top - 3) / FB2Page.PAGE_HEIGHT, (x + w + 3)
                                / FB2Page.PAGE_WIDTH, (bottom + bounds.bottom + 3) / FB2Page.PAGE_HEIGHT));
                    }
                }
                x += w;
            }
            y = bottom;
        }
        return y;
    }

    @Override
    public void recycle() {
    }

    @Override
    public BitmapRef renderBitmap(final int width, final int height, final RectF pageSliceBounds) {
        final Matrix matrix = MatrixUtils.get();
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
        for (int i = 0, n = lines.size(); i < n; i++) {
            final Line line = lines.get(i);
            int top = y;
            int bottom = y + line.getHeight();
            if (bounds.top < bottom && top < bounds.bottom) {
                line.render(c, FB2Page.MARGIN_X, bottom, bounds.left, bounds.right);
            }
            y = bottom;
        }
        for (int i = 0, n = noteLines.size(); i < n; i++) {
            final Line line = noteLines.get(i);
            int top = y;
            int bottom = y + line.getHeight();
            if (bounds.top < bottom && top < bounds.bottom) {
                line.render(c, FB2Page.MARGIN_X, bottom, bounds.left, bounds.right);
            }
            y = bottom;
        }

        return bmp;
    }

    public void appendLine(final Line line) {
        if (committed || !line.appendable()) {
            return;
        }
        lines.add(line);
        contentHeight += line.getHeight();
    }

    public void appendNoteLine(final Line line) {
        noteLines.add(line);
        contentHeight += line.getHeight();
    }

    public void commit() {
        if (committed) {
            return;
        }
        final int h = FB2Page.PAGE_HEIGHT - contentHeight - 2 * FB2Page.MARGIN_Y;
        if (h > 0) {
            lines.add(new Line(0, JustificationMode.Center).append(new LineFixedWhiteSpace(0, h)));
            contentHeight += h;
        }
        for (final Line line : noteLines) {
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
