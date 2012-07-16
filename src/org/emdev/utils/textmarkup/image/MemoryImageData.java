package org.emdev.utils.textmarkup.image;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;

import java.io.IOException;

import org.emdev.utils.base64.Base64;
import org.emdev.utils.base64.Base64InputStream;

public class MemoryImageData extends AbstractImageData {

    String encoded;
    byte[] data;

    public MemoryImageData(final String encoded) {
        this.encoded = encoded;
    }

    @Override
    public Bitmap getBitmap() {
        final byte[] data = getData();
        final Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
        return bmp;
    }

    protected byte[] getData() {
        if (data == null) {
            data = Base64.decode(encoded, Base64.DEFAULT);
            encoded = null;
        }
        return data;
    }

    protected Options getImageSize() {
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
}
