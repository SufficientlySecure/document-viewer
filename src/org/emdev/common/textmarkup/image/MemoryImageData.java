package org.emdev.common.textmarkup.image;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;

import org.emdev.utils.base64.Base64;

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

    @Override
    public void recycle() {
        encoded = null;
        data = null;
    }

    protected byte[] getData() {
        if (data == null) {
            data = Base64.decode(encoded, Base64.DEFAULT);
            encoded = null;
        }
        return data;
    }

    protected Options getImageSize() {
        return getImageSize(encoded);
    }
}
