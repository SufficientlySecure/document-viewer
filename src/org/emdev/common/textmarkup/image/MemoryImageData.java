package org.emdev.common.textmarkup.image;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;

import java.util.concurrent.atomic.AtomicLong;

import org.emdev.utils.base64.Base64;

public class MemoryImageData extends AbstractImageData {

    String encoded;
    byte[] data;
    int dataLength;
    char[] chars;
    int start;
    int length;

    public MemoryImageData(final String encoded) {
        this.encoded = encoded;
    }

    public MemoryImageData(char[] ch, int start, int length) {
        this.encoded = null;
        this.chars = ch;
        this.start = start;
        this.length = length;
    }

    @Override
    public Bitmap getBitmap() {
        final byte[] data = getData();
        final Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, dataLength);
        return bmp;
    }

    @Override
    public void recycle() {
        encoded = null;
        data = null;
    }

    protected byte[] getData() {
        if (data == null) {
            if (encoded != null) {
                data = Base64.decode(encoded, Base64.DEFAULT);
                dataLength = data.length;
                encoded = null;
            } else {
                AtomicLong outSize = new AtomicLong();
                data = Base64.decode(chars, start, length, Base64.DEFAULT, outSize);
                dataLength = (int) outSize.get();
            }

        }
        return data;
    }

    protected Options getImageSize() {
        if (encoded != null) {
            return getImageSize(encoded);
        } else {
            return getImageSize(chars, start, length);
        }
    }
}
