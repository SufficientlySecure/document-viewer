package org.emdev.utils.textmarkup.image;

import org.ebookdroid.droids.fb2.codec.FB2Page;

import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.RectF;

import java.io.IOException;
import java.io.InputStream;

import org.emdev.utils.LengthUtils;
import org.emdev.utils.base64.Base64;
import org.emdev.utils.base64.Base64InputStream;

public abstract class AbstractImageData implements IImageData {

    protected AbstractImageData() {
    }

    @Override
    public final RectF getImageRect(final boolean inline) {
        float w = 0, h = 0;
        final Options opts = getImageSize();
        if (opts != null) {
            final int origWidth = opts.outWidth;
            final int origHeight = opts.outHeight;

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
        }
        return new RectF(0f, 0f, w, h);
    }

    protected abstract Options getImageSize();

    protected Options getImageSize(final String encoded) {
        final Options opts = new Options();
        opts.inJustDecodeBounds = true;
        final Base64InputStream stream = new Base64InputStream(new AsciiCharInputStream(encoded), Base64.DEFAULT);
        BitmapFactory.decodeStream(stream, null, opts);
        try {
            stream.close();
        } catch (final IOException ex) {
        }
        return opts;
    }

    protected static class AsciiCharInputStream extends InputStream {

        private final String str;
        private int index;
        private final int length;

        public AsciiCharInputStream(final String str) {
            this.length = LengthUtils.length(str);
            this.str = str;
        }

        @Override
        public int read() throws IOException {
            return index >= length ? -1 : (0xFF & str.charAt(index++));
        }

        @Override
        public int available() throws IOException {
            return length - index;
        }

        @Override
        public int read(final byte[] buffer, final int offset, final int length) throws IOException {
            final int available = available();
            if (available <= 0) {
                return -1;
            }
            final int read = Math.min(available, length);
            for (int i = 0; i < read; i++) {
                final int c = (0xFF & str.charAt(index++));
                buffer[offset + i] = (byte) c;
            }
            return read;
        }
    }

}
