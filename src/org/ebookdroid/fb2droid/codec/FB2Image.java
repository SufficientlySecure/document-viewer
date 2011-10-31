package org.ebookdroid.fb2droid.codec;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Base64;
import android.util.Base64InputStream;

import java.io.IOException;
import java.io.InputStream;

public class FB2Image extends AbstractFB2LineElement {

    private final String encoded;
    private byte[] data;

    public FB2Image(final String encoded) {
        super(calculateImageRect(encoded));
        this.encoded = encoded;
    }

    private static RectF calculateImageRect(final String encoded) {
        final Options opts = getImageSize(encoded);
        final int origWidth = opts.outWidth;
        final int origHeight = opts.outHeight;

        float w = 0, h = 0;

        if (origWidth <= FB2Page.PAGE_WIDTH && origHeight <= FB2Page.PAGE_HEIGHT) {
            w = origWidth;
            h = origHeight;
        } else {
            if (origWidth > FB2Page.PAGE_WIDTH) {
                w = FB2Page.PAGE_WIDTH;
                h = origHeight * w / origWidth;
            } else {
                w = origWidth;
                h = origHeight;
            }
            if (h > FB2Page.PAGE_HEIGHT) {
                w = w * FB2Page.PAGE_HEIGHT / h;
                h = FB2Page.PAGE_HEIGHT;
            }
        }
        return new RectF(0f, 0f, w, h);
    }

    @Override
    public void adjustWidth(final float w) {
    }

    @Override
    public void render(final Canvas c, final int y, final int x) {
        final byte[] data = getData();
        final Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
        c.drawBitmap(bmp, null, new Rect(x, y - height, (int) (x + width), y), null);
        bmp.recycle();
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
