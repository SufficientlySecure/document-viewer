package org.emdev.utils.textmarkup.line;


import org.ebookdroid.droids.fb2.codec.FB2Page;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import java.io.IOException;
import java.io.InputStream;

import org.emdev.utils.base64.Base64;
import org.emdev.utils.base64.Base64InputStream;

public class Image extends AbstractLineElement {

    private final String encoded;
    private byte[] data;
    private final Paint paint;

    public Image(final String encoded, final boolean inline) {
        super(calculateImageRect(encoded, inline));
        this.encoded = encoded;
        paint = new Paint();
        paint.setFilterBitmap(true);
    }

    private static RectF calculateImageRect(final String encoded, final boolean inline) {
        final Options opts = getImageSize(encoded);
        final int origWidth = opts.outWidth;
        final int origHeight = opts.outHeight;

        float w = 0, h = 0;

        if (origWidth <= FB2Page.PAGE_WIDTH - 2 * FB2Page.MARGIN_X
                && origHeight <= FB2Page.PAGE_HEIGHT - 2 * FB2Page.MARGIN_Y && inline) {
            w = origWidth;
            h = origHeight;
        } else {
            if (origWidth > FB2Page.PAGE_WIDTH - 2 * FB2Page.MARGIN_X || !inline) {
                w = FB2Page.PAGE_WIDTH - 2 * FB2Page.MARGIN_X;
                h = origHeight * w / origWidth;
            } else {
                w = origWidth;
                h = origHeight;
            }
            if (h > FB2Page.PAGE_HEIGHT - 2 * FB2Page.MARGIN_X) {
                w = w * (FB2Page.PAGE_HEIGHT - 2 * FB2Page.MARGIN_Y) / h;
                h = FB2Page.PAGE_HEIGHT - 2 * FB2Page.MARGIN_X;
            }
        }
        return new RectF(0f, 0f, w, h);
    }

    @Override
    public float render(final Canvas c, final int y, final int x, final float additionalWidth, float left, float right) {
        if (left < x + width && x < right) {
            final byte[] data = getData();
            final Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
            if (bmp != null) {
                c.drawBitmap(bmp, null, new Rect(x, y - height, (int) (x + width), y), paint);
                bmp.recycle();
            } else {
                c.drawRect(new Rect(x, y - height, (int) (x + width), y), paint);
            }
        }
        return width;
    }

    public byte[] getData() {
        if (data == null) {
            data = Base64.decode(encoded, Base64.DEFAULT);
        }
        return data;
    }

    private static Options getImageSize(final String encoded) {
        final Options opts = new Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory
                .decodeStream(new Base64InputStream(new AsciiCharInputStream(encoded), Base64.DEFAULT), null, opts);
        return opts;
    }

    private static class AsciiCharInputStream extends InputStream {

        private final String str;
        private int index;

        public AsciiCharInputStream(final String str) {
            this.str = str;
        }

        @Override
        public int read() throws IOException {
            return index >= str.length() ? -1 : (0xFF & str.charAt(index++));
        }
    }
}
